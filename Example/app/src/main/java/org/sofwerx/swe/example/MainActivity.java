package org.sofwerx.swe.example;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;

import com.google.android.material.textfield.TextInputEditText;

import org.sofwerx.ogc.sos.AbstractSosOperation;
import org.sofwerx.ogc.sos.SensorMeasurement;
import org.sofwerx.ogc.sos.SensorMeasurementTime;
import org.sofwerx.ogc.sos.SensorResultTemplateField;
import org.sofwerx.ogc.sos.SosSensor;
import org.sofwerx.ogc.sos.SosService;
import org.sofwerx.ogc.sos.SosMessageListener;

import java.io.StringWriter;
import java.util.Random;

public class MainActivity extends AppCompatActivity implements SosMessageListener {
    private SosService sosService;
    private SosSensor sosSensor;
    private SensorMeasurementTime sensorMeasurementTime;
    private SensorMeasurement sensorMeasurementSample;

    private final static String SAMPLE_SHORT_NAME = "Swe Example";
    private final static String SAMPLE_LONG_NAME = "Sample readings from the Swe Example app";
    private final static String SAMPLE_SENSOR_NAME = "Sample Reader";
    private final static String SAMPLE_SENSOR_DEFINITION = "http://www.sofwerx.org/sample.owl#Sample";
    private final static String SAMPLE_SENSOR_UOM = "widgets";

    private void updateSensorSettings() {
        buildSensor();
        sosSensor.setId(editDeviceName.getText().toString());
        sosSensor.setUniqueId(textSensorId.getText().toString());
    }

    private void buildSensor() {
        if (sosSensor == null) {
            //This is where our sample sensor is built
            sosSensor = new SosSensor(null, null, SAMPLE_SHORT_NAME, SAMPLE_LONG_NAME);
            sensorMeasurementTime = new SensorMeasurementTime();
            sensorMeasurementSample = new SensorMeasurement(new SensorResultTemplateField(SAMPLE_SENSOR_NAME,SAMPLE_SENSOR_DEFINITION,SAMPLE_SENSOR_UOM));
            sosSensor.addMeasurement(sensorMeasurementTime);
            sosSensor.addMeasurement(sensorMeasurementSample);
            sensorMeasurementTime.setValue(System.currentTimeMillis());
            sensorMeasurementSample.setValue(seekValue.getProgress());
        }
    }

    private void sendData() {
        int value = seekValue.getProgress();
        if (sensorMeasurementTime != null)
            sensorMeasurementTime.setValue(System.currentTimeMillis());
        if (sensorMeasurementSample != null)
            sensorMeasurementSample.setValue(value);
        if (sosService == null) {
            String url;
            if (checkSendNet.isChecked())
                url = editSosServerUrl.getText().toString();
            else
                url = null;
            String username = editSosUsername.getText().toString();
            String sosPassword = editSosPassword.getText().toString();
            log("Creating sensor...");
            sosService = new SosService(MainActivity.this, sosSensor, url, username, sosPassword, true, checkSendIpc.isChecked());
            log("Sending reading "+value);
        } else {
            log("Sending reading "+value);
            sosService.broadcastSensorReadings();
        }
    }


    @Override
    public void onDestroy() {
        if (sosService != null)
            sosService.shutdown(); //for proper memory management, remember to shutdown the SOS service so it can do its cleanup as well
        saveValuesToPreferences();
        super.onDestroy();
    }

    /**
     * Below is all the GUI and support coded needed to display and alter the sensor info
     */

    private TextInputEditText editDeviceName, editSosServerUrl, editSosUsername, editSosPassword;
    private CheckBox checkSendIpc, checkSendNet;
    private TextView textCurrentValue, textCannotSendWarning, textSensorId, textResult, textAssigned;
    private SeekBar seekValue;
    private Button sendButton;
    private View viewMeasurements, viewAssigned;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        editDeviceName = findViewById(R.id.editName);
        editSosServerUrl = findViewById(R.id.editUrl);
        editSosUsername = findViewById(R.id.editUsername);
        editSosPassword = findViewById(R.id.editPassword);
        checkSendIpc = findViewById(R.id.checkSendIpc);
        checkSendNet = findViewById(R.id.checkSendNet);
        textCurrentValue = findViewById(R.id.textCurrentValue);
        textCannotSendWarning = findViewById(R.id.textSendWarning);
        textSensorId = findViewById(R.id.textSensorId);
        textResult = findViewById(R.id.result);
        textAssigned = findViewById(R.id.textAssigned);
        seekValue = findViewById(R.id.seekBar);
        sendButton = findViewById(R.id.send);
        viewMeasurements = findViewById(R.id.viewMeasurements);
        viewAssigned = findViewById(R.id.viewAssigned);
        ImageButton eraseAssignedButton = findViewById(R.id.buttonErase);
        sendButton.setOnClickListener(v -> sendData());
        eraseAssignedButton.setOnClickListener(v -> {
            if (sosSensor != null) {
                sosSensor = null;
                if (sosService != null) {
                    sosService.shutdown();
                    sosService = null;
                }
                log("Sensor assignments from server removed");
                updateVisibility();
            }
        });
        seekValue.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                textCurrentValue.setText("Current value: "+progress);
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) { }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) { }
        });
        checkSendIpc.setOnCheckedChangeListener((buttonView, isChecked) -> {
            updateVisibility();
            if (sosService != null)
                sosService.setIpcBroadcast(isChecked);
        });
        checkSendNet.setOnCheckedChangeListener((buttonView, isChecked) -> {
            updateVisibility();
            if (sosService != null)
                sosService.setSosServerUrl(isChecked?editSosServerUrl.getText().toString():null);
        });
        editDeviceName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { }

            @Override
            public void afterTextChanged(Editable s) {
                if (sosSensor != null)
                    sosSensor.setId(s.toString());
                updateSensorId();
                updateVisibility();
            }
        });
        textResult.setMovementMethod(new ScrollingMovementMethod());
        loadValuesFromPreferences();
    }

    private final static String PREFS_SEND_IPC = "ipc";
    private final static String PREFS_SEND_NET = "net";
    private final static String PREFS_NAME = "name";
    private final static String PREFS_SOS_URL = "url";
    private final static String PREFS_SOS_USERNAME = "usr";
    private final static String PREFS_SOS_PASSWORD = "pwd";
    private final static String PREFS_SENSOR_ASSIGNED_PROCEDURE = "snrpro";
    private final static String PREFS_SENSOR_ASSIGNED_OFFERING = "snroff";
    private final static String PREFS_SENSOR_ASSIGNED_TEMPLATE = "snrtmp";

    private void loadValuesFromPreferences() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String name = prefs.getString(PREFS_NAME,null);
        if ((name == null) || (name.length() == 0)) {
            Random random = new Random();
            name = "SweLib Sample "+random.nextInt(10)+random.nextInt(10);
        }
        editDeviceName.setText(name);
        editSosServerUrl.setText(prefs.getString(PREFS_SOS_URL,null));
        editSosUsername.setText(prefs.getString(PREFS_SOS_USERNAME,null));
        editSosPassword.setText(prefs.getString(PREFS_SOS_PASSWORD,null));
        checkSendIpc.setChecked(prefs.getBoolean(PREFS_SEND_IPC,true));
        checkSendNet.setChecked(prefs.getBoolean(PREFS_SEND_NET,false));
        buildSensor();
        sosSensor.setAssignedProcedure(prefs.getString(PREFS_SENSOR_ASSIGNED_PROCEDURE,null));
        sosSensor.setAssignedOffering(prefs.getString(PREFS_SENSOR_ASSIGNED_OFFERING,null));
        sosSensor.setAssignedTemplate(prefs.getString(PREFS_SENSOR_ASSIGNED_TEMPLATE,null));
    }

    private void saveValuesToPreferences() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor edit = prefs.edit();
        String id = editDeviceName.getText().toString();
        if ((id != null) && (id.length() > 0))
            edit.putString(PREFS_NAME,id);
        else
            edit.remove(PREFS_NAME);
        String url = editSosServerUrl.getText().toString();
        if ((url != null) && (url.length() > 0))
            edit.putString(PREFS_SOS_URL,url);
        else
            edit.remove(PREFS_SOS_URL);
        String usr = editSosUsername.getText().toString();
        if ((usr != null) && (usr.length() > 0))
            edit.putString(PREFS_SOS_USERNAME,usr);
        else
            edit.remove(PREFS_SOS_USERNAME);
        String pwd = editSosPassword.getText().toString();
        if ((pwd != null) && (pwd.length() > 0))
            edit.putString(PREFS_SOS_PASSWORD,pwd);
        else
            edit.remove(PREFS_SOS_PASSWORD);
        edit.putBoolean(PREFS_SEND_IPC,checkSendIpc.isChecked());
        edit.putBoolean(PREFS_SEND_NET,checkSendNet.isChecked());
        String offering = null;
        String procedure = null;
        String template = null;
        if (sosSensor != null) {
            offering = sosSensor.getAssignedOffering();
            procedure = sosSensor.getAssignedProcedure();
            template = sosSensor.getAssignedTemplate();
        }
        if ((offering != null) && (offering.length() > 0))
            edit.putString(PREFS_SENSOR_ASSIGNED_OFFERING,offering);
        else
            edit.remove(PREFS_SENSOR_ASSIGNED_OFFERING);
        if ((procedure != null) && (procedure.length() > 0))
            edit.putString(PREFS_SENSOR_ASSIGNED_PROCEDURE,procedure);
        else
            edit.remove(PREFS_SENSOR_ASSIGNED_PROCEDURE);
        if ((template != null) && (template.length() > 0))
            edit.putString(PREFS_SENSOR_ASSIGNED_TEMPLATE,template);
        else
            edit.remove(PREFS_SENSOR_ASSIGNED_TEMPLATE);
        edit.apply();
    }

    private void updateVisibility() {
        if (checkSendNet.isChecked()) {
            editSosServerUrl.setVisibility(View.VISIBLE);
            editSosUsername.setVisibility(View.VISIBLE);
            editSosPassword.setVisibility(View.VISIBLE);
        } else {
            editSosServerUrl.setVisibility(View.GONE);
            editSosUsername.setVisibility(View.GONE);
            editSosPassword.setVisibility(View.GONE);
        }
        if (sosSensor == null)
            viewAssigned.setVisibility(View.GONE);
        else {
            StringWriter out = null;
            if (sosSensor.getAssignedOffering() != null) {
                out = new StringWriter();
                out.append("Offering: ");
                out.append(sosSensor.getAssignedOffering());
            }
            if (sosSensor.getAssignedProcedure() != null) {
                if (out == null)
                    out = new StringWriter();
                else
                    out.append(LINE_SEP);
                out.append("Procedure: ");
                out.append(sosSensor.getAssignedProcedure());
            }
            if (sosSensor.getAssignedTemplate() != null) {
                if (out == null)
                    out = new StringWriter();
                else
                    out.append(LINE_SEP);
                out.append("Template: ");
                out.append(sosSensor.getAssignedTemplate());
            }

            if (out == null)
                viewAssigned.setVisibility(View.GONE);
            else {
                textAssigned.setText(out.toString());
                viewAssigned.setVisibility(View.VISIBLE);
            }
        }
        checkSendingPrereqs();
    }

    @SuppressLint("SetTextI18n")
    private void checkSendingPrereqs() {
        boolean isEnabled = checkSendIpc.isChecked() || checkSendNet.isChecked();
        if (isEnabled) {
            String failures = getMissingPrereqs();

            if (failures == null) {
                viewMeasurements.setVisibility(View.VISIBLE);
                sendButton.setEnabled(true);
                textCannotSendWarning.setVisibility(View.GONE);
            } else {
                viewMeasurements.setVisibility(View.GONE);
                sendButton.setEnabled(true);
                textCannotSendWarning.setText(failures);
                textCannotSendWarning.setVisibility(View.VISIBLE);
            }
        } else {
            sendButton.setEnabled(false);
            textCannotSendWarning.setText("Send via IPC or Internet must be checked");
            textCannotSendWarning.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (sosSensor != null) {
            editDeviceName.setText(sosSensor.getId());
            textSensorId.setText(sosSensor.getUniqueId());
        }
        updateVisibility();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    private final static String LINE_SEP = System.getProperty("line.separator");
    private boolean firstLogLine = true;
    private void log(String text) {
        if (text == null)
            return;
        if (textResult.getText().length() > 2000)
            firstLogLine = true;
        if (firstLogLine) {
            firstLogLine = false;
            textResult.setText(text);
        } else {
            textResult.append(LINE_SEP);
            textResult.append(text);
        }
    }

    /**
     * Gets a message describing anything this sensor is missing that would be required
     * to register with an SOS-T server
     * @return null == all prereqs are complete
     */
    private String getMissingPrereqs() {
        StringWriter out = null;
        String callsign = editDeviceName.getText().toString();
        String sensorId = textSensorId.toString();

        if ((callsign == null) || (callsign.length() == 0)) {
            out = new StringWriter();
            out.append("Missing Identification Name");
        } else if (callsign.length() < SosSensor.MIN_ID_LENGTH) {
            out = new StringWriter();
            out.append("Identification Name is too short");
        }

        if ((sensorId == null) || (sensorId.length() == 0)) {
            if (out == null)
                out = new StringWriter();
            else
                out.append(", ");
            out.append("Missing Unique ID");
        }

        if (checkSendNet.isChecked()) {
            String url = editSosServerUrl.getText().toString();
            if ((url == null) || (url.length() == 0)) {
                if (out == null)
                    out = new StringWriter();
                else
                    out.append(", ");
                out.append("Missing Server URL");
            }
        }

        updateSensorSettings();

        if (out == null)
            return null;
        return out.toString();
    }


    private final static String UNIQUE_ID_SAMPLE_PREFIX = "http://www.sofwerx.org/sample/";
    /**
     * This is a convenience method to set the sensor ID (a machine readable unique reference
     * to this device) based on the id (a human readable unique reference to this device -
     * like a label for this device on a map). You can set the sensorId seperately
     */
    private void updateSensorId() {
        String callsign = editDeviceName.getText().toString();
        String sensorId;
        if (callsign == null)
            sensorId = null;
        else {
            String safe = callsign.toLowerCase().replace(" ","");
            if (safe.length() < 1)
                sensorId = null;
            else
                sensorId = UNIQUE_ID_SAMPLE_PREFIX+safe;
        }
        textSensorId.setText(sensorId);
        if (sosSensor != null)
            sosSensor.setUniqueId(sensorId);
    }
    @Override
    public void onSosOperationReceived(final AbstractSosOperation operation) {
        runOnUiThread(() -> {
            if (operation != null) {
                log(operation.getClass().getSimpleName() + " received");
                updateVisibility();
            }
        });
    }

    @Override
    public void onSosError(final String message) {
        runOnUiThread(() -> {
            log(message);
        });
    }

    @Override
    public void onSosConfigurationSuccess() {
        runOnUiThread(() -> {
            log("SOS configuration was successful");
        });
    }
}
