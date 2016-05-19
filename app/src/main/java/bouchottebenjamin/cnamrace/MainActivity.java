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
import android.view.WindowManager;
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
    private TextView compassTV;
    private TextView lightTV;
    private EditText gravityLimitTV;

    private int limitGravity;
    private float x;
    private float y;
    private float z;
    private float offset_x;
    private float offset_y;
    private float offset_z;
    float gravity = 0;
    float[] rotationMatrix;
    float[] geomagneticMatrix;
    float[] orientation;
    float[] orientationGyr;
    float initialLight;

    private SensorManager sensorManager;
    private Sensor accelerometer;
    private Sensor geomagnetic;
    private Sensor gyroscope;
    private Sensor light;

    boolean geomagneticOK = false;
    boolean gyroscopeOK = false;
    boolean lightOK = false;

    // Pour ne pas déclencher les toasts 30 millions de fois...
    Toast reverseToast;
    Toast gToast;
    Toast tiltToast;

    WindowManager.LayoutParams lp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        init = (Button) findViewById(R.id.init);
        validLimitGravity = (Button) findViewById(R.id.validLimitGravity);
        gravityTV = (TextView) findViewById(R.id.gravity);
        tiltTV = (TextView) findViewById(R.id.tiltTV);
        compassTV = (TextView) findViewById(R.id.compassTV);
        lightTV = (TextView) findViewById(R.id.lightTV);
        gravityLimitTV = (EditText) findViewById(R.id.gravityLimit);

        limitGravity = Integer.parseInt(String.valueOf(gravityLimitTV.getText()));
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        geomagnetic = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        light = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);

        lp = getWindow().getAttributes();
        initialLight = lp.screenBrightness;

        reverseToast = Toast.makeText(getApplicationContext(), "Attention! Vous êtes à l'envers.", Toast.LENGTH_SHORT);
        gToast = Toast.makeText(getApplicationContext(), gravity + "G: diminution des gaz", Toast.LENGTH_SHORT);
        tiltToast = Toast.makeText(getApplicationContext(), "Moins vite dans les virages", Toast.LENGTH_SHORT);

        String infoCapteurs = "";
        if (accelerometer != null) {
            //Toast.makeText(this, "Un accéléromètre est disponible: " + accelerometer.getName(), Toast.LENGTH_SHORT).show();
            infoCapteurs += "accéléromètre OK ";
        } else {
            //Toast.makeText(this, "Aucun accéléromètre n'est disponible.", Toast.LENGTH_SHORT).show();
            infoCapteurs += "accéléromètre NOK";
        }

        if (geomagnetic != null) {
            //Toast.makeText(this, "Un magnétomètre est disponible: " + geomagnetic.getName(), Toast.LENGTH_LONG).show();
            geomagneticOK = true;
            infoCapteurs += "magnétomètre OK ";
        } else {
            //Toast.makeText(this, "Aucun magnétomètre n'est disponible.", Toast.LENGTH_SHORT).show();
            tiltTV.setText("Aucun magnétomètre n'est disponible.");
            infoCapteurs += "magnétomètre NOK ";
        }

        if (gyroscope != null) {
            //Toast.makeText(this, "Un gyroscope est disponible: " + gyroscope.getName(), Toast.LENGTH_LONG).show();
            gyroscopeOK = true;
            infoCapteurs += "gyroscope OK ";
        } else {
            //Toast.makeText(this, "Aucun gyroscope n'est disponible.", Toast.LENGTH_SHORT).show();
            tiltTV.setText("Aucun magnétomètre n'est disponible.");
            infoCapteurs += "gyroscope NOK";
        }

        if (light != null) {
            //Toast.makeText(this, "Un capteur de lumière est disponible: " + gyroscope.getName(), Toast.LENGTH_LONG).show();
            lightOK = true;
            infoCapteurs += "light OK ";
            Log.i("light - max", String.valueOf(light.getMaximumRange()));
            Log.i("light - power", String.valueOf(light.getPower()));
            Log.i("light - vendor", String.valueOf(light.getVendor()));
            Log.i("light - version", String.valueOf(light.getVersion()));
        } else {
            //Toast.makeText(this, "Aucun capteur de lumière n'est disponible.", Toast.LENGTH_LONG).show();
            tiltTV.setText("Aucun capteur de lumière n'est disponible.");
            infoCapteurs += " light NOK";
        }

        Log.i("infocapteur", infoCapteurs);
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
                x = event.values[0];
                y = event.values[1];
                z = event.values[2];
                float xO = (float) event.values[0] - offset_x;
                float yO = (float) event.values[1] - offset_y;
                float zO = (float) event.values[2] - offset_z;
                gravity = (float) Math.round(Math.sqrt(Math.pow(xO, 2) + Math.pow(yO, 2) + Math.pow(zO, 2)) / 9.80665);

                gravityTV.setText(String.valueOf((int) gravity));
                if (gravity > limitGravity && Math.abs(x) >= 1) {   // si > 6G et virage
                    gravityTV.setBackgroundColor(Color.parseColor("#ff0000"));
                    if (!gToast.getView().isShown()) {
                        gToast.show();
                    }
                } else {
                    gravityTV.setBackgroundColor(Color.parseColor("#00ff00"));
                }

                    // Inclinaison
                double tilt = 0;
                if (geomagneticMatrix != null) {
                    SensorManager.getRotationMatrix(new float[16], rotationMatrix, event.values, geomagneticMatrix);
                    SensorManager.getOrientation(rotationMatrix, orientation);

                    tilt = orientation[1] * 180 / Math.PI;  // conversion en degrés
                    compassTV.setText(getDirectionFromDegrees(orientation[2]));
                } else {
                    tilt = Math.atan(x / (Math.sqrt(Math.pow(y, 2) + Math.pow(z, 2)))); // inclinaison axe x
                    tilt = Math.round(tilt * 180 / Math.PI);  // conversion rad en deg

                    //autre solution
                    double norm_Of_g = Math.sqrt(x * x + y * y + z * z);
                    tilt = (int) Math.round( Math.toDegrees(Math.acos( x / norm_Of_g ))) - 90;
                }
                tiltTV.setText( "" + tilt);
                if (tilt > 5 || tilt < -5) {
                    if (!tiltToast.getView().isShown()) {
                        tiltToast.show();
                        lp.screenBrightness = 1.0f;
                        getWindow().setAttributes(lp);
                    }
                } else {
                    lp.screenBrightness = initialLight;
                    getWindow().setAttributes(lp);
                }
                if (y < -8) {
                    if (!reverseToast.getView().isShown()) {
                        reverseToast.show();
                    }
                }
                break;
            case Sensor.TYPE_MAGNETIC_FIELD:
                geomagneticMatrix = event.values.clone();
                break;
            case Sensor.TYPE_GYROSCOPE:
                break;
            case Sensor.TYPE_LIGHT:
                lightTV.setText((int) event.values[0]);
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
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, geomagnetic, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, light, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this, geomagnetic);
        sensorManager.unregisterListener(this, accelerometer);
        sensorManager.unregisterListener(this, gyroscope);
        sensorManager.unregisterListener(this, light);
    }
}
