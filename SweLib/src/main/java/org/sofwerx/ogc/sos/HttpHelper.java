package org.sofwerx.ogc.sos;

import android.util.Base64;
import android.util.Log;
import android.util.Pair;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

import javax.net.ssl.HttpsURLConnection;

public class HttpHelper {
    public static String post(String serverURL, String username, String password, String body) throws IOException {
        return post(serverURL, username, password, body,false);
    }

    public static String post(String serverURL, String username, String password, String body, boolean soapWrapper) throws IOException {
        if (serverURL == null)
            throw new IOException("Cannot connect to a null server URL");
        if (body == null)
            throw new IOException("Cannot send an empty body");

        StringWriter payload = new StringWriter();
        //payload.append(SOAP_HEADER);
        payload.append(body);
        //payload.append(SOAP_FOOTER);

        URL url;
        String response = "";
        url = new URL(serverURL);

        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setReadTimeout(15000);
        conn.setConnectTimeout(15000);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type","application/soap+xml");
        conn.setDoInput(true);
        if ((username != null) && (password != null)) {
            try {
                byte[] data = (username + ":" + password).getBytes("UTF-8");
                String encoded = new String(Base64.encode(data, Base64.DEFAULT));
                conn.setRequestProperty("Authorization", "Basic " + encoded);
            } catch (UnsupportedEncodingException ignore) {
            }
        }
        //conn.setDoOutput(true);
        conn.setInstanceFollowRedirects(false);

        OutputStream os = conn.getOutputStream();
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
        String payloadText = payload.toString();
        if (soapWrapper) {
            if (payloadText.startsWith("<?xml version")) {
                payloadText = payloadText.substring(payloadText.indexOf('>')+1); //remove the xml header
                payloadText = SOAP_HEADER+payloadText+SOAP_FOOTER;
            }
        }
        writer.write(payloadText);

        writer.flush();
        writer.close();
        os.close();
        int responseCode=conn.getResponseCode();

        if (responseCode == HttpsURLConnection.HTTP_OK) {
            String line;
            BufferedReader br=new BufferedReader(new InputStreamReader(conn.getInputStream()));
            while ((line=br.readLine()) != null) {
                response+=line;
            }
        } else {
            Log.e(SosIpcTransceiver.TAG,"Http connection attempt failed: "+responseCode);
            response = null;
        }

        conn.disconnect();
        Log.d(SosIpcTransceiver.TAG,"HttpURLConnection disconnected");

        if ((response != null) && (response.length() < 1))
            response = null;
        return response;
    }

    public static String get(String serverURL, ArrayList<Pair<String,String>> keyValues) throws IOException {
        if ((serverURL == null) || (keyValues == null) || keyValues.isEmpty()) {
            Log.w(SosIpcTransceiver.TAG,"Unable to post with null data or server URL");
            return null;
        }
        URL url;
        String response = "";
        StringWriter out = new StringWriter();
        out.append(serverURL);
        boolean first = true;

        for (Pair<String,String> pair:keyValues) {
            if (first) {
                first = false;
                out.append('?');
            } else
                out.append('&');
            out.append(pair.first);
            out.append('=');
            out.append(pair.second);
        }
        String urlString = out.toString();
        url = new URL(urlString);

        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setReadTimeout(15000);
        conn.setConnectTimeout(15000);
        conn.setRequestMethod("GET");
        conn.setInstanceFollowRedirects(false);
        //conn.setDoOutput(true);

        int responseCode=conn.getResponseCode();

        if (responseCode == HttpsURLConnection.HTTP_OK) {
            String line;
            BufferedReader br=new BufferedReader(new InputStreamReader(conn.getInputStream()));
            while ((line=br.readLine()) != null) {
                response+=line;
            }
        } else {
            Log.e(SosIpcTransceiver.TAG,"Http connection attempt failed: "+responseCode);
            response = null;
        }

        conn.disconnect();
        Log.d(SosIpcTransceiver.TAG,"HttpURLConnection disconnected");

        if ((response != null) && (response.length() < 1))
            response = null;
        return response;
    }

    //Not pretty, but efficient
    private final static String SOAP_HEADER = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><env:Envelope xmlns:env=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://www.w3.org/2003/05/soap-envelope http://www.w3.org/2003/05/soap-envelope/soap-envelope.xsd\"> <env:Body>\r\n";
    private final static String SOAP_FOOTER = "\r\n</env:Body></env:Envelope>";
}
