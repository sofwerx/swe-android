package org.sofwerx.ogc.sos;

import android.util.Pair;

import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.util.ArrayList;

import javax.xml.parsers.ParserConfigurationException;

public class OperationGetResults extends AbstractSosOperation {
    private SosSensor sensor;

    public OperationGetResults() {
        super();
    }

    public OperationGetResults(SosSensor sensor) {
        super();
        this.sensor = sensor;
    }

    @Override
    public boolean isValid() {
        return (sensor != null) && (sensor.getAssignedOffering() != null) && (sensor.getAssignedOffering() != null);
    }

    public SosSensor getSensor() { return sensor; }
    public void setSensor(SosSensor sensor) { this.sensor = sensor; }

    public ArrayList<Pair<String,String>> getPairs() {
        ArrayList<Pair<String,String>> pairs = new ArrayList<>();
        pairs.add(new Pair("service","SOS"));
        pairs.add(new Pair("version","2.0"));
        pairs.add(new Pair("request","GetResult"));
        pairs.add(new Pair("offering",sensor.getAssignedOffering()));
        pairs.add(new Pair("observedProperty",sensor.getFirstObservableProperty()));
        pairs.add(new Pair("responseFormat","application/json"));
        return pairs;
    }

    public JSONObject toJSON() {
        ArrayList<Pair<String,String>> pairs = getPairs();
        JSONObject obj = new JSONObject();

        try {
            for (Pair<String,String> pair:pairs) {
                obj.putOpt(pair.first,pair.second);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return obj;
    }

    public void parse(JSONObject obj) {
        if ((sensor != null) && (obj != null))
            sensor.parseSensors(obj);
    }

    @Override
    public void parse(Element getCapabilities) {
        //ignore, GetResults is handled as JSON only for now
    }

    @Override
    public Document toXML() throws ParserConfigurationException {
        //ignored, GetResults is handled as JSON only for now
        return null;
    }
}