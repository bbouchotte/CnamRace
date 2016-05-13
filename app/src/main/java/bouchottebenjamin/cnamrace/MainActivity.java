package bouchottebenjamin.cnamrace;

import android.graphics.Color;
import android.graphics.Matrix;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Text;

public class MainActivity extends AppCompatActivity implements SensorEventListener  {

    private Button init;
    private Button validLimitGravity;
    private TextView gravityTV;
    private TextView tiltTV;
    private EditText gravityLimitTV;

    private int limitGravity;
    private float x;
    private float y;
    private float z;
    private float offset_x;
    private float offset_y;
    private float offset_z;
    float[] rotationMatrix;
    float[] geomagneticMatrix;
    float[] orientation;

    private SensorManager sensorManager;
    private Sensor accelerometer;
    private Sensor geomagnetic;

    boolean geomagneticOK = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        init = (Button) findViewById(R.id.init);
        validLimitGravity = (Button) findViewById(R.id.validLimitGravity);
        gravityTV = (TextView) findViewById(R.id.gravity);
        tiltTV = (TextView) findViewById(R.id.tiltTV);
        gravityLimitTV = (EditText) findViewById(R.id.gravityLimit);

        limitGravity = Integer.parseInt(String.valueOf(gravityLimitTV.getText()));
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        geomagnetic = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        if (accelerometer != null) {
            Toast.makeText(this, "Un accéléromètre est disponible: " + accelerometer.getName(), Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "Aucun accéléromètre n'est disponible.", Toast.LENGTH_LONG).show();
        }

        if (geomagnetic != null) {
            Toast.makeText(this, "Un magnétomètre est disponible: " + geomagnetic.getName(), Toast.LENGTH_LONG).show();
            geomagneticOK = true;
        } else {
            Toast.makeText(this, "Aucun magnétomètre n'est disponible.", Toast.LENGTH_LONG).show();
            tiltTV.setText("Aucun magnétomètre n'est disponible.");

        }

        init.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                init();
            }
        });

        validLimitGravity.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setLimitGravity();
            }
        });

    }

    private void setLimitGravity() { limitGravity = Integer.parseInt(String.valueOf(gravityLimitTV.getText())); }

    private void init() {
        offset_x = x;
        offset_y = y;
        offset_z = z;
    }

    private String getDirectionFromDegrees(float degrees) {
        if (degrees >= -22.5 && degrees < 22.5) { return "N"; }
        if (degrees >= 22.5 && degrees < 67.5) { return "NE"; }
        if (degrees >= 67.5 && degrees < 112.5) { return "E"; }
        if (degrees >= 112.5 && degrees < 157.5) { return "SE"; }
        if (degrees >= 157.5 || degrees < -157.5) { return "S"; }
        if (degrees >= -157.5 && degrees < -112.5) { return "SW"; }
        if (degrees >= -112.5 && degrees < -67.5) { return "W"; }
        if (degrees >= -67.5 && degrees < -22.5) { return "NW"; }
        return null;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        switch (event.sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER:
                float gravity = 0;
                x = event.values[0];
                y = event.values[1];
                z = event.values[2];
                float xO = (float) event.values[0] - offset_x;
                float yO = (float) event.values[1] - offset_y;
                float zO = (float) event.values[2] - offset_z;
                gravity = (float) Math.floor(Math.sqrt(Math.pow(xO, 2) + Math.pow(yO, 2) + Math.pow(zO, 2)) * 100 * 9.80665) / 100;

                gravityTV.setText(String.valueOf((int) gravity));
                if (gravity > limitGravity) {
                    gravityTV.setBackgroundColor(Color.parseColor("#ff0000"));
                } else {
                    gravityTV.setBackgroundColor(Color.parseColor("#00ff00"));
                }

                    // Inclinaison
                double tilt;
                if(geomagneticMatrix != null) {
                    SensorManager.getRotationMatrix(new float[16], rotationMatrix, event.values, geomagneticMatrix);
                    SensorManager.getOrientation(rotationMatrix, orientation);

                    tilt = orientation[1] * 180 / Math.PI;  // conversion en degrés
                    String direction = getDirectionFromDegrees(orientation[2]);


                } else {
                    tilt = Math.atan(x / (Math.sqrt(Math.pow(y, 2) + Math.pow(z, 2)))); // inclinaison axe x
                    tilt = tilt * 180 / Math.PI;  // conversion rad en deg
                }
                tiltTV.setText((int) tilt);
                if (tilt > 5 || tilt < -5) {
                    tiltTV.setText("Moins vite dans les virages");
                }
                if (y < -8) {
                    Toast.makeText(this, "Attention! Vous êtes à l'envers.", Toast.LENGTH_SHORT).show();
                }
                break;
            case Sensor.TYPE_MAGNETIC_FIELD:
                geomagneticMatrix = event.values.clone();
                break;
            default:
                break;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
        sensorManager.registerListener(this, geomagnetic, SensorManager.SENSOR_DELAY_FASTEST);
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this, geomagnetic);
        sensorManager.unregisterListener(this, accelerometer);
    }
}
