package org.sofwerx.ogc.sos;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

/**
 * Currently a single class intended to handle all IPC with other SOS capable processes.
 * This may be refactored into multiple classes for a bit cleaner look. It is a single
 * class for now to facilitate ease of review by several parties while the concepts are
 * being fleshed-out.
 */
public class SosIpcTransceiver extends BroadcastReceiver {
    public final static String TAG = "SosIpc";
    public final static String SOFWERX_LINK_PLACEHOLDER = "http://www.sofwerx.org/placeholder"; //this is used as a placeholder where a URL should be provided for a new standard or feature
    private final static SimpleDateFormat dateFormatISO8601 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.US);
    private final static SimpleDateFormat dateFormatBackup = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.US);
    public static final String ACTION_SOS = "org.sofwerx.ogc.ACTION_SOS";
    private static final String EXTRA_PAYLOAD = "SOS";
    private static final String EXTRA_ORIGIN = "src";
    public static final String ACTION_SQAN_BROADCAST = "org.sofwerx.sqan.pkt";
    private final static String SQAN_PACKET_BYTES = "bytes";
    private final static String SQAN_PACKET_CHANNEL = "channel";
    final static long DEFAULT_OUTGOING_THROTTLE_RATE = 1000l * 5l;
    final static long DEFAULT_INCOMING_THROTTLE_RATE = 1000l * 10l;
    private static boolean enableSqAN = true;
    private static String channel = SosService.DEFAULT_SWE_CHANNEL;
    private SosMessageListener listener;

    //Since XML is expensive to marshall/unmarshall, include throttling to ignore messages that come in or go out too fast
    private static long throttleRate = -1l;
    private static long nextAvailableIntake = Long.MIN_VALUE;

    public SosIpcTransceiver(SosMessageListener listener) {
        this.listener = listener;
    }

    public static void setChannel(String channel) { SosIpcTransceiver.channel = channel; }
    public static void setEnableSqAN(boolean enable) { SosIpcTransceiver.enableSqAN = enable; }

    /**
     * Sets a throttle rate (i.e. a min amo8nt of time between messages in ms); all messages
     * received in excess of the throttle rate will be dropped (done to prevent bogging
     * down the processor in XML marshallig/unmarshalling operations).
     * @param rate rate in ms (or -1l if no throttling is needed)
     */
    public static void setThrottleRate(long rate) {
        if (throttleRate != rate) {
            throttleRate = rate;
            if (rate > 0l)
                Log.d(TAG, "Setting throttle interval to " + Long.toString(rate) + "ms");
            else {
                Log.d(TAG, "Removing throttle");
            }
        }
    }

    public static void clearThrottle() { setThrottleRate(-1l); }

    @Override
    public void onReceive(Context context, Intent intent) {
        if ((context != null) && (intent != null)) {
            String action = intent.getAction();
            if (ACTION_SOS.equalsIgnoreCase(action)) {
                String origin = intent.getStringExtra(EXTRA_ORIGIN);
                if (!context.getPackageName().equalsIgnoreCase(origin))
                    onMessageReceived(context, origin, intent.getStringExtra(EXTRA_PAYLOAD));
            } else if (ACTION_SQAN_BROADCAST.equalsIgnoreCase(action)) { //forward traffic from SqAN
                String origin = intent.getStringExtra(EXTRA_ORIGIN);
                if (!context.getPackageName().equalsIgnoreCase(origin)) {
                    String channel = intent.getStringExtra(SQAN_PACKET_CHANNEL);
                    if ((SosIpcTransceiver.channel != null) && SosIpcTransceiver.channel.equalsIgnoreCase(channel)) { //only handle SOS-T channel broadcasts
                        try {
                            byte[] bytes = intent.getByteArrayExtra(SQAN_PACKET_BYTES);
                            if (bytes != null) {
                                String payload = new String(bytes,"UTF-8");
                                onMessageReceived(context,"sqan."+SQAN_PACKET_CHANNEL,payload);
                            }
                        } catch (UnsupportedEncodingException ignore) {
                        }
                    }
                }
            } else
                Log.e(TAG, "Unexpected action message received: " + action);
        }
    }

    /**
     * Handles the message received from the SOS Broadcast and pass the result to the listener
     * @param source the application that sent the request; mostly to prevent circular reporting
     * @param input the SOS operation received
     */
    public void onMessageReceived(final Context context,final String source, final String input) {
        if (source == null) {
            Log.e(TAG,"SOS broadcasts from anonymous senders is not supported");
            return;
        }
        if (input == null) {
            Log.e(TAG, "Null operation received from SOS broadcast IPC");
            return;
        }
        if ((throttleRate <= 0l) || (System.currentTimeMillis() > nextAvailableIntake)) {
            if (throttleRate > 0l)
                nextAvailableIntake = System.currentTimeMillis() + throttleRate;
            new Thread(() -> {
                try {
                    DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
                    DocumentBuilder docBuilder = builderFactory.newDocumentBuilder();
                    Document doc = docBuilder.parse(new InputSource(new ByteArrayInputStream(input.getBytes("utf-8"))));
                    if (doc != null) {
                        AbstractSosOperation operation = AbstractSosOperation.newFromXML(doc);
                        if (operation != null) {
                            if (listener != null)
                                listener.onSosOperationReceived(operation);
                        }
                    }
                } catch (ParserConfigurationException | IOException | SAXException e) {
                    Log.e(TAG, "SOS IPC broadcast was not XML: " + input);
                }
            }).start();
        } else
            Log.d(TAG,"Dropping message from "+source+" due to flooding: "+input);
    }

    /**
     * Broadcasts this SOS operation via IPC
     * @param context
     * @param operation
     */
    public void broadcast(final Context context, final AbstractSosOperation operation) throws SosException {
        if ((throttleRate <= 0l) || (System.currentTimeMillis() > nextAvailableIntake)) {
            if (throttleRate > 0l)
                nextAvailableIntake = System.currentTimeMillis() + throttleRate;
            if (operation != null) {
                if (!operation.isValid()) {
                    throw new SosException(operation.getClass().getSimpleName() + " does not have all required information");
                }
                new Thread(() -> {
                    Document doc = null;
                    try {
                        doc = operation.toXML();
                    } catch (ParserConfigurationException e) {
                        //throw new SosException("Unable to create document: " + e.getMessage());
                    }
                    try {
                        if (doc != null)
                            broadcast(context, toString(doc));
                    } catch (Exception ex) {
                        //throw new SosException("Unable to convert XML document to string: " + ex.getMessage());
                    }
                }).start();
            }
        } else
            Log.d(TAG,operation.getClass().getSimpleName()+" operation received but ignored since the current throttle rate of "+Long.toString(throttleRate)+"ms is being exceeded");
    }

    public final static String toString(Document doc) throws TransformerException {
        if (doc == null)
            return null;
        StringWriter writer = new StringWriter();
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
        transformer.setOutputProperty(OutputKeys.METHOD, "xml");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.transform(new DOMSource(doc), new StreamResult(writer));
        return writer.toString();
    }

    public void setListener(SosMessageListener listener) {
        this.listener = listener;
    }

    /**
     * Broadcast an SOS Operation
     * @param context
     * @param sosOperation
     */
    private void broadcast(Context context, String sosOperation) {
        if (context == null) {
            Log.d(TAG,"Context needed to broadcast an SOS Operation");
            return;
        }
        if (sosOperation == null) {
            Log.e(TAG, "Cannot broadcast an empty SOS Operation");
            return;
        }
        Intent intent = new Intent(ACTION_SOS);
        //intent.putExtra(EXTRA_ORIGIN, BuildConfig.APPLICATION_ID);
        intent.putExtra(EXTRA_ORIGIN, context.getPackageName());
        intent.putExtra(EXTRA_PAYLOAD,sosOperation);
        context.sendBroadcast(intent);

        if (enableSqAN) {
			Log.d(TAG,"Broadcasting over SqAN as well");
            try {
                byte[] bytes = sosOperation.getBytes("UTF-8");
                Intent sqanIntent = new Intent(ACTION_SQAN_BROADCAST);
                //sqanIntent.putExtra(EXTRA_ORIGIN, BuildConfig.APPLICATION_ID);
                sqanIntent.putExtra(EXTRA_ORIGIN, context.getPackageName());
                sqanIntent.putExtra(SQAN_PACKET_BYTES, bytes);
                sqanIntent.putExtra(SQAN_PACKET_CHANNEL, channel);
                context.sendBroadcast(sqanIntent);
            } catch (UnsupportedEncodingException ignore) {
            }
        }

        Log.d(TAG,"Broadcast: "+sosOperation);
    }

    /**
     * Consumes ISO 8601 formatted text and translates into UNIX time
     * @param time unix time (or Long.MIN_VALUE if could not be parsed)
     * @return
     */
    public static long parseTime(String time) {
        if (time != null) {
            try {
                Date date = dateFormatISO8601.parse(time);
                return date.getTime();
            } catch (ParseException e) {
                try {
                    if (time.endsWith("Z"))
                        time = time.substring(0,time.indexOf('Z'));
                    Date date = dateFormatBackup.parse(time);
                    return date.getTime();
                } catch (ParseException e1) {
                    e.printStackTrace();
                }
            }
        }
        return Long.MIN_VALUE;
    }

    public static String formatTime(long time) {
        return dateFormatISO8601.format(time);
    }
}