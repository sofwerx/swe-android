package org.sofwerx.ogc.sos;

import android.util.Log;

/**
 * Holds a measurement time
 */
public class SensorMeasurementTime extends SensorMeasurement {
    public final static String NAME = "time";

    public SensorMeasurementTime() {
        super(new SensorTimeResultTemplateField());
    }

    @Override
    public String getName() { return NAME; }

    @Override
    public String toString() {
        if (value instanceof Long)
            return SosIpcTransceiver.formatTime((Long)value);
        Log.e(SosIpcTransceiver.TAG,"Value for this time measurement is not of type Long");
        return null;
    }

    @Override
    public void parseLong(String in) throws NumberFormatException {
        if (in != null)
            value = SosIpcTransceiver.parseTime(in); //consume ISO 8601 format
        if (!(value instanceof Long) || ((Long)value == Long.MIN_VALUE))
            throw new NumberFormatException("String was not ISO 8601 formatted time");
    }

    @Override
    public void setValue(Object value) {
        if (value == null)
            return;
        if (value instanceof String) {
            try {
                parseLong((String) value);
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        } else
            this.setValue(value);
    }


    @Override
    public boolean isSame(SensorMeasurement other) {
        return ((other != null) && (other instanceof SensorMeasurementTime));
    }
}
