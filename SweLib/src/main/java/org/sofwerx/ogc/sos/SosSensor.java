package org.sofwerx.ogc.sos;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * This is the overall device/sensor that will be reporting to the SOS. This sensor can make report
 * multiple measurements.
 */
public class SosSensor {
    public final static int MIN_ID_LENGTH = 6;
    private String assignedOffering;
    private String assignedProcedure;
    private String assignedTemplate;
    private String id;
    private String uniqueId;
    private String longName;
    private String shortName;
    private ArrayList<SensorMeasurement> measurements;
    private ArrayList<String> observableProperties;

    public SosSensor() {
        assignedOffering = null;
        assignedProcedure = null;
        assignedTemplate = null;
    }

    /**
     * Constructor for a Sensor
     * @param id the display ID on most systems (i.e. "TORGI WISKEY 35")
     * @param uniqueId the unique ID for this sensor (i.e. "http://www.sofwerx.org/torgi/wiskey35")
     * @param shortName the short name for what this sensor is (i.e. "TORGI")
     * @param longName the long name for this sensor (i.e. "Tactical Observation of RF GNSS Interference sensor")
     */
    public SosSensor(String id, String uniqueId, String shortName, String longName) {
        this();
        this.id = id;
        this.uniqueId = uniqueId;
        this.shortName = shortName;
        this.longName = longName;
    }

    public ArrayList<SensorMeasurement> getSensorMeasurements() { return measurements; }

    /**
     * Adds a measurement to the sensor
     * @param measurement
     */
    public void addMeasurement(SensorMeasurement measurement) {
        if (measurement == null)
            return;
        if ((measurement.getFormat() == null) && (measurement.toString() != null)) {
            Log.e(SosIpcTransceiver.TAG, "Cannot add a measurement to the sensor without specifying the measurement format");
            return;
        }
        if (measurements == null)
            measurements = new ArrayList<>();
        measurements.add(measurement);
    }

    /**
     * Removes any assignments (Assigned Offering, Assigned Procedure, Assigned Template) that
     * may have been made by an SOS Server. This is primarily used when switching to a new
     * server.
     */
    public void clearAssignments() {
        assignedOffering = null;
        assignedProcedure = null;
        assignedTemplate = null;
    }

    /**
     * Gets the assigned procedure ID provided by the SOS
     * (i.e. "http://www.sofwerx.org/torgi/wiskey35")
     * @return
     */
    public String getAssignedProcedure() { return assignedProcedure; }

    /**
     * Sets the assigned procedure ID provided by the SOS
     * (i.e. "http://www.sofwerx.org/torgi/wiskey35")
     * @param assignedProcedure
     */
    public void setAssignedProcedure(String assignedProcedure) { this.assignedProcedure = assignedProcedure; }

    /**
     * Gets the assigned offering ID provided by the SOS
     * (i.e. "http://www.sofwerx.org/torgi/wiskey35-sos")
     * @return
     */
    public String getAssignedOffering() { return assignedOffering; }

    /**
     * Sets the assigned offering ID provided by the SOS
     * (i.e. "http://www.sofwerx.org/torgi/wiskey35-sos")
     * @param assignedOffering
     */
    public void setAssignedOffering(String assignedOffering) { this.assignedOffering = assignedOffering; }

    /**
     * Gets the person-readable ID for this sensor (i.e. "TORGI Wiskey 35")
     * @return
     */
    public String getId() { return id; }

    /**
     * Sets the person-readable ID for this sensor (i.e. "TORGI Wiskey 35")
     * @param id
     */
    public void setId(String id) { this.id = id; }

    /**
     * Gets the unique ID for SOS purposes (i.e. "http://www.sofwerx.org/torgi/wiskey35")
     * @return
     */
    public String getUniqueId() { return uniqueId; }

    /**
     * Sets the unique ID for SOS purposes (i.e. "http://www.sofwerx.org/torgi/wiskey35")
     * @param uniqueId
     */
    public void setUniqueId(String uniqueId) { this.uniqueId = uniqueId; }

    /**
     * Gets the long name for this type of sensor
     * (i.e. "Tactical Observation of RF and GNSS Interference sensor")
     * @return
     */
    public String getLongName() { return longName; }

    /**
     * Gets the long name for this type of sensor
     * (i.e. "Tactical Observation of RF and GNSS Interference sensor")
     * @param longName
     */
    public void setLongName(String longName) { this.longName = longName; }

    /**
     * Gets the short name for this type of sensor (i.e. "TORGI")
     * @return
     */
    public String getShortName() { return shortName; }

    /**
     * Sets the short name for this type of sensor (i.e. "TORGI")
     * @param shortName
     */
    public void setShortName(String shortName) { this.shortName = shortName; }

    /**
     * Gets the template ID assigned by the SOS. This is received in response
     * to an InsertResultTemplate operation and required for use in an
     * InsertResult operation. This assigned template looks like
     * "http://www.sofwerx.org/torgi/wiskey35#output0"
     * @return
     */
    public String getAssignedTemplate() { return assignedTemplate; }

    /**
     * Sets the template ID assigned by the SOS. This is received in response
     * to an InsertResultTemplate operation and required for use in an
     * InsertResult operation. This assigned template looks like
     * "http://www.sofwerx.org/torgi/wiskey35#output0"
     * @param assignedTemplate
     */
    public void setAssignedTemplate(String assignedTemplate) { this.assignedTemplate = assignedTemplate; }

    /**
     * Does the sensor have measurements and do all of the measurements have the required template
     * @return
     */
    public boolean isMeasurmentsFieldsValid() {
        if ((measurements == null) || measurements.isEmpty())
            return false;
        boolean passed = true;
        for(SensorMeasurement measurement:measurements) {
            if ((measurement.getFormat() == null) || !measurement.getFormat().isValid())
                return false;
        }
        return passed;
    }

    /**
     * Does this sensor have enough information to begin the registration process with the SOS server
     * @return
     */
    public boolean isReadyToRegisterSensor() {
        return ((id != null) && (uniqueId != null) && (longName != null) && (shortName != null));
    }

    /**
     * Does this sensor have enough information to register a result template
     * @return
     */
    public boolean isReadyToRegisterResultTemplate() {
        return ((assignedProcedure != null) && (assignedOffering != null)
                && (measurements != null) && !measurements.isEmpty());
    }

    /**
     * Does this sensor have the registration information to begin sending results
     * @return
     */
    public boolean isReadyToSendResults() {
        return (assignedTemplate != null);
    }


    public boolean isSame(SosSensor other) {
        if (other != null) {
            if ((uniqueId != null) && (other.uniqueId != null)) {
                return uniqueId.equalsIgnoreCase(other.uniqueId);
            }
            if ((assignedOffering != null) && (other.assignedOffering != null)) {
                return assignedOffering.equalsIgnoreCase(other.assignedOffering);
            }
            if ((assignedTemplate != null) && (other.assignedTemplate != null)) {
                return assignedTemplate.equalsIgnoreCase(other.assignedTemplate);
            }
        }
        return false;
    }

    public void update(SosSensor other) {
        if (other == null)
            return;
        if (other.assignedOffering != null)
            assignedOffering = other.assignedOffering;
        if (other.assignedProcedure != null)
            assignedProcedure = other.assignedProcedure;
        if (other.assignedTemplate != null)
            assignedTemplate = other.assignedTemplate;
        if (other.id != null)
            id = other.id;
        if (other.uniqueId != null)
            uniqueId = other.uniqueId;
        if (other.longName != null)
            longName = other.longName;
        if (other.shortName != null)
            shortName = other.shortName;
        if (other.measurements != null)
            measurements = other.measurements;
    }

    public void addObservableProperty(String observableProperty) {
        if (observableProperty != null) {
            if (observableProperties == null) {
                observableProperties = new ArrayList<>();
                observableProperties.add(observableProperty);
                return;
            }
            for (String current:observableProperties) {
                if (observableProperty.equalsIgnoreCase(current))
                    return;
            }
            observableProperties.add(observableProperty);
        }
    }

    public void setObservableProperty() {
        if (uniqueId != null)
            addObservableProperty(uniqueId+"_1");
    }

    /**
     * Gets the first observable property
     * @return first observableProperty or null if none exists
     */
    public String getFirstObservableProperty() {
        if ((observableProperties == null) || observableProperties.isEmpty())
            return null;
        return observableProperties.get(0);
    }

    public ArrayList<String> getObservableProperties() {
        return observableProperties;
    }

    /**
     * Intakes a JSON version of GetResult response and parse it into SensorMeasurements
     * @param obj
     */
    public void parseSensors(JSONObject obj) {
        if (obj == null)
            return;
        Iterator<String> iter = obj.keys();
        while (iter.hasNext()) {
            String key = iter.next();
            try {
                Object value = obj.get(key);
                SensorMeasurement measurement = getSensorMeasurement(key);
                if (measurement == null) {
                    measurement = SensorMeasurement.newFromObject(key,value);
                    if (measurement != null) {
                        if (measurements == null)
                            measurements = new ArrayList<>();
                        measurements.add(measurement);
                    }
                } else
                    measurement.setValue(value);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    private SensorMeasurement getSensorMeasurement(String key) {
        if ((key == null) || (measurements == null) || measurements.isEmpty())
            return null;
        for (SensorMeasurement measurement:measurements) {
            if (key.equalsIgnoreCase(measurement.getName()))
                return measurement;
        }
        return null;
    }

    @Override
    public String toString() {
        StringWriter out = new StringWriter();
        if (id != null)
            out.append(id);
        out.append(": ");
        if ((measurements != null) && !measurements.isEmpty()) {
            boolean first = true;
            for (SensorMeasurement measurement:measurements) {
                if (first)
                    first = false;
                else
                    out.append(", ");
                out.append(measurement.toString());
            }
        } else
            out.append("no measurements");
        return out.toString();
    }
}
