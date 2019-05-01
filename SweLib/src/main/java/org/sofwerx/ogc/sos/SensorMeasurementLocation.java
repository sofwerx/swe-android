package org.sofwerx.ogc.sos;

import org.json.JSONObject;

import java.io.StringWriter;

/**
 * Holds a measurement location
 */
public class SensorMeasurementLocation extends SensorMeasurement {
    public final static String NAME = "location";
    public final static int FIELD_LATITUDE = 0;
    public final static int FIELD_LONGITUDE = 1;
    public final static int FIELD_ALTITUDE = 2;
    private double lat,lng,alt = Double.NaN;

    public SensorMeasurementLocation() {
        super(new SensorLocationResultTemplateField(null,null,null));
    }

    @Override
    public String getName() { return NAME; }

    /**
     * Gets the latitude (WGS-84)
     * @return
     */
    public double getLatitude() { return lat; }

    /**
     * Sets the latitude (WGS-84)
     * @param lat
     */
    public void setLatitude(double lat) { this.lat = lat; }

    /**
     * Gets the longitude (WGS-84)
     * @return
     */
    public double getLongitude() { return lng; }

    /**
     * Sets the longitude (WGS-84)
     * @param lng
     */
    public void setLng(double lng) { this.lng = lng; }

    /**
     * Gets the altitude
     * @return m HAE
     */
    public double getAltitude() { return alt; }

    /**
     * Gets the altitude
     * @param alt m HAE
     */
    public void setAltitude(double alt) { this.alt = alt; }


    public double[] getValues() {
        double[] values = new double[3];
        values[FIELD_LATITUDE] = lat;
        values[FIELD_LONGITUDE] = lng;
        values[FIELD_ALTITUDE] = alt;
        return values;
    }

    private void clear() {
        this.lat = Double.NaN;
        this.lng = Double.NaN;
        this.alt = Double.NaN;
    }

    /**
     * Sets the location
     * @param latitude WGS-84
     * @param longitude WGS-84
     * @param altitude m HAE
     */
    public void setLocation(double latitude, double longitude, double altitude) {
        this.lat = latitude;
        this.lng = longitude;
        this.alt = altitude;
    }

    @Override
    public void setValue(Object value) {
        if (value != null) {
            if (value instanceof JSONObject)
                parse((JSONObject) value);
            else
                clear();
        }
    }

    private void parse(JSONObject obj) {
        clear();
        if (obj == null)
            return;
        if (obj.has("lat"))
            lat = obj.optDouble("lat",Double.NaN);
        else if (obj.has("Lat"))
            lat = obj.optDouble("Lat",Double.NaN);
        else if (obj.has("Latitude"))
            lat = obj.optDouble("Latitude",Double.NaN);

        if (obj.has("lon"))
            lng = obj.optDouble("lon",Double.NaN);
        else if (obj.has("lng"))
            lng = obj.optDouble("lng",Double.NaN);
        else if (obj.has("Lon"))
            lng = obj.optDouble("Lon",Double.NaN);
        else if (obj.has("Longitude"))
            lng = obj.optDouble("Longitude",Double.NaN);

        if (obj.has("alt"))
            alt = obj.optDouble("alt",Double.NaN);
        else if (obj.has("Alt"))
            alt = obj.optDouble("Alt",Double.NaN);
        else if (obj.has("Altitude"))
            alt = obj.optDouble("Altitude",Double.NaN);
    }

    @Override
    public boolean isSame(SensorMeasurement other) {
        return ((other != null) && (other instanceof SensorMeasurementLocation));
    }

    @Override
    public void update(SensorMeasurement other) {
        if (other instanceof SensorMeasurementLocation) {
            SensorMeasurementLocation otherL = (SensorMeasurementLocation)other;
            lat = otherL.lat;
            lng = otherL.lng;
            alt = otherL.alt;
        }
    }

    @Override
    public String toString() {
        if (Double.isNaN(lat) || Double.isNaN(lng))
            return "invalid location";
        StringWriter out = new StringWriter();
        out.append(Double.toString(lat));
        out.append(',');
        out.append(Double.toString(lng));
        if (Double.isNaN(alt)) {
            out.append(',');
            out.append(Double.toString(alt));
        }
        return out.toString();
    }
}
