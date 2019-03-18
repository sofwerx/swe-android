package org.sofwerx.ogc.sos;

import android.util.Log;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.util.ArrayList;

/**
 * This is used for templates that involve text rather than quantity
 */
public class SensorTextResultTemplateField  extends SensorResultTemplateField {
    protected SensorTextResultTemplateField() {}

    /**
     * Constructor for a sensor field
     * @param name name of the field
     * @param definition definition for this type of name
     */
    public SensorTextResultTemplateField(String name, String definition) {
        setName(name);
        setQuantityDefinition(definition);
    }

    @Override
    public void addToElement(Document doc, Element element) {
        if ((doc == null) || (element == null)) {
            Log.e(SosIpcTransceiver.TAG,"Neither doc nor element can be null in SensorResultTemplateField.addToElement()");
            return;
        }
        Element field = doc.createElement(TAG_NAME_FIELD);
        field.setAttribute(NAME_NAME,getName());
        element.appendChild(field);
        Element text = doc.createElement(TAG_NAME_TEXT);
        text.setAttribute(NAME_DEFINITION,getQuantityDefinition());
        field.appendChild(text);
    }

    @Override
    public void parse(Element field) {
        if (field == null)
            return;
        setName(field.getAttribute(NAME_NAME));
    }
}