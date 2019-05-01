package org.sofwerx.ogc.sos;

import android.util.Log;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.StringReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

public abstract class AbstractSosOperation {
    private static SosSensor defaultSensor;

    public static AbstractSosOperation newFromXmlString(String text) {
        if (text != null) {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            try {
                DocumentBuilder builder = null;
                builder = factory.newDocumentBuilder();
                Document doc = builder.parse(new InputSource(new StringReader(text)));
                return newFromXML(doc);
            } catch (ParserConfigurationException | IOException | SAXException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    private static Element removeSoap(Document doc) {
        if (doc == null)
            return null;
        Element element = doc.getDocumentElement();
        Element soapBody;
        try {
            Element soapEnvelope;
            String localName = element.getTagName();
            if ((localName != null) && localName.contains("soap:Envelope"))
                soapEnvelope = element;
            else
                soapEnvelope = (Element) element.getElementsByTagName("soap:Envelope").item(0);
            if (soapEnvelope == null)
                return element;
            soapBody = (Element) soapEnvelope.getElementsByTagName("soap:Body").item(0);
        } catch (Exception ignore) {
            return element;
        }
        if (soapBody == null)
            return element;
        NodeList children = soapBody.getChildNodes();
        if ((children != null) && (children.getLength() > 0)) {
            for (int i=0;i<children.getLength();i++) {
                if (children.item(i) instanceof Element)
                    return (Element) children.item(i);
            }
        }
        return element;
    }

    public static AbstractSosOperation newFromXML(Document doc) {
        Node node = removeSoap(doc);
        AbstractSosOperation operation = null;
        if (doc != null) {
            if (node != null) {
                String tagName = node.getNodeName();
                if (tagName != null) {
                    if (tagName.contains(OperationInsertSensorResponse.NAMESPACE))
                        operation = new OperationInsertSensorResponse();
                    else if (tagName.contains(OperationInsertSensor.NAMESPACE))
                        operation = new OperationInsertSensor();
                    else if (tagName.contains(OperationInsertResultTemplateResponse.NAMESPACE))
                        operation = new OperationInsertResultTemplateResponse();
                    else if (tagName.contains(OperationInsertResultTemplate.NAMESPACE))
                        operation = new OperationInsertResultTemplate();
                    else if (tagName.contains(OperationInsertResultResponse.NAMESPACE))
                        operation = new OperationInsertResultResponse();
                    else if (tagName.contains(OperationInsertResult.NAMESPACE))
                        operation = new OperationInsertResult(defaultSensor);
                    else if (tagName.contains(OperationGetCapabilitiesResponse.NAMESPACE)) {
                        //TODO differentiate between GetCapabilities and the response to GetCapabilities
                        operation = new OperationGetCapabilitiesResponse();
                    }
                    if (operation != null)
                        operation.parse((Element)node);
                }
            }
        }
        return operation;
    }

    /**
     * Used so that templates requiring some context can have that context available
     * @return
     */
    public static SosSensor getDefaultSensor() {
        return defaultSensor;
    }

    /**
     * Used so that templates requiring some context can have that context available
     * @param defaultSensor
     */
    public static void setDefaultSensor(SosSensor defaultSensor) {
        AbstractSosOperation.defaultSensor = defaultSensor;
        Log.d(SosIpcTransceiver.TAG,"Default sensor is now: "+((defaultSensor==null)?"null":defaultSensor.getUniqueId()));
    }

    /**
     * Does this operation contain all the information it needs
     * @return
     */
    public abstract boolean isValid();

    protected abstract void parse(Element element);
    public Document toXML() throws ParserConfigurationException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder parser = factory.newDocumentBuilder();
        return parser.newDocument();
    }
}
