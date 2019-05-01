package org.sofwerx.ogc.sos;

import android.util.Log;
import android.util.Pair;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.ArrayList;

import javax.xml.parsers.ParserConfigurationException;

public class OperationGetCapabilitiesResponse extends AbstractSosOperation {
    public final static String NAMESPACE = "sos:Capabilities";
    private ArrayList<SosSensor> sensors;

    public OperationGetCapabilitiesResponse() {
        super();
    }

    @Override
    public boolean isValid() {
        return true;
    }

    public ArrayList<SosSensor> getSensors() { return sensors; }

    @Override
    public void parse(Element getCapabilities) {
        if ((getCapabilities == null) || (getCapabilities.getChildNodes() == null))
            return;
        NodeList items = getCapabilities.getChildNodes();
        for (int a=0; a<items.getLength();a++) {
            Node outerContents = items.item(a);
            String name = outerContents.getNodeName();
            if ((name != null) && name.contains("sos:contents") && outerContents.hasChildNodes()) {
                NodeList inners = outerContents.getChildNodes();
                for (int b=0;b<inners.getLength();b++) {
                    Node innerContents = inners.item(b);
                    String innerName = innerContents.getNodeName();
                    if ((innerName != null) && innerName.contains("sos:Contents") && innerContents.hasChildNodes()) {
                        NodeList innerInners = innerContents.getChildNodes();
                        for (int c=0;c<innerInners.getLength();c++) {
                            Node innerInner = innerInners.item(c);
                            String innerInnerName = innerInner.getNodeName();
                            if ((innerInnerName != null) && innerInnerName.contains("swes:offering") && innerInner.hasChildNodes()) {
                                NodeList offerings = innerInner.getChildNodes();
                                for (int d=0;d<offerings.getLength();d++) {
                                    Node offering = offerings.item(d);
                                    String offeringName = offering.getNodeName();
                                    if ((offeringName != null) && offeringName.contains("sos:ObservationOffering"))
                                        parseOffering(offering);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private void parseOffering(Node offering) {
        if ((offering == null) || !offering.hasChildNodes()) {
            Log.w(SosIpcTransceiver.TAG,"Unable to parse \"sos:ObservationOffering\" node in GetCapabilities response as the node is empty");
            return;
        }
        NodeList children = offering.getChildNodes();
        if (sensors == null)
            sensors = new ArrayList<>();
        SosSensor sensor = new SosSensor();
        Node node;
        String name;
        String text;
        for (int i=0;i<children.getLength();i++) {
            node = children.item(i);
            name = node.getNodeName();
            if (name == null)
                continue;
            text = node.getTextContent();
            if (name.contains("swes:description"))
                sensor.setLongName(text);
            else if (name.contains("swes:name"))
                sensor.setId(text);
            else if (name.contains("swes:procedure")) {
                if (sensor.getUniqueId() == null)
                    sensor.setUniqueId(text);
                sensor.setAssignedProcedure(text);
            } else if (name.contains("swes:identifier"))
                sensor.setAssignedOffering(text);
            else if (name.contains("observableProperty"))
                sensor.addObservableProperty(text);
        }
        sensors.add(sensor);
        Log.d(SosIpcTransceiver.TAG,"Sensor "+sensor.getId()+" found in GetCapabilities");
    }

    @Override
    public Document toXML() throws ParserConfigurationException {
        Document doc = super.toXML();
        Element getCap = doc.createElement(NAMESPACE);
        getCap.setAttribute("xmlns:swe","http://www.opengis.net/swe/2.0");
        getCap.setAttribute("xmlns:ows","http://www.opengis.net/ows/1.1");
        getCap.setAttribute("xmlns:sos","http://www.opengis.net/sos/2.0");
        getCap.setAttribute("service","SOS");
        getCap.setAttribute("xsi:schemaLocation","http://www.opengis.net/sos/2.0 http://schemas.opengis.net/sos/2.0/sosGetCapabilities.xsd");
        doc.appendChild(getCap);

        //TODO

        return doc;
    }
}