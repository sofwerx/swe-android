package org.sofwerx.ogc.sos;

import android.util.Log;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.ParserConfigurationException;

public class OperationGetCapabilities extends AbstractSosOperation {
    public final static String NAMESPACE = "sos:GetCapabilities";

    public OperationGetCapabilities() {
        super();
    }

    @Override
    public boolean isValid() {
        return true;
    }

    private final static String TAG_ACCEPT_VERSIONS = "ows:AcceptVersions";
    private final static String TAG_VERSION = "ows:Version";
    private final static String TAG_SECTIONS = "ows:Sections";
    private final static String TAG_SECTION = "ows:Section";

    private final static String TAG_CONTENTS = "sos:contents";

    @Override
    public void parse(Element getCapabilities) {
        if ((getCapabilities == null) || (getCapabilities.getChildNodes() == null))
            return;
        NodeList items = getCapabilities.getChildNodes();
        //TODO
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
        Element elementAcceptVersions = doc.createElement(TAG_ACCEPT_VERSIONS);
        Element elementVersion = doc.createElement(TAG_VERSION);
        elementVersion.setTextContent("2.0.0");
        elementAcceptVersions.appendChild(elementVersion);
        getCap.appendChild(elementAcceptVersions);
        Element elementSections = doc.createElement(TAG_SECTIONS);
        Element elementSectionOM = doc.createElement(TAG_SECTION);
        elementSectionOM.setTextContent("OperationsMetadata");
        elementSections.appendChild(elementSectionOM);
        Element elementSectionSI = doc.createElement(TAG_SECTION);
        elementSectionSI.setTextContent("ServiceIdentification");
        elementSections.appendChild(elementSectionSI);
        Element elementSectionSP = doc.createElement(TAG_SECTION);
        elementSectionSP.setTextContent("ServiceProvider");
        elementSections.appendChild(elementSectionSP);
        Element elementSectionFC = doc.createElement(TAG_SECTION);
        elementSectionFC.setTextContent("FilterCapabilities");
        elementSections.appendChild(elementSectionFC);
        Element elementSectionCon = doc.createElement(TAG_SECTION);
        elementSectionCon.setTextContent("Contents");
        elementSections.appendChild(elementSectionCon);

        getCap.appendChild(elementSections);
        return doc;
    }
}