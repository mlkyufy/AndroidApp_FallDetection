// For data export
// C:\Users\Oskars\Downloads\adb>adb -d shell "run-as com.example.oskars.xyzregister_v2 cat /data/data/com.example.oskars.xyzregister_v2/files/accData.txt" > accData.txt


package io.github.introml.activityrecognition;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import static android.hardware.Sensor.TYPE_ACCELEROMETER;

public class MainActivity extends AppCompatActivity implements SensorEventListener, TextToSpeech.OnInitListener {
    private float[] results;
    private static final int N_SAMPLES = 200;
    private static List<Float> xc, yc, zc,xg,yg,zg;
    //private static List<Long> timestamps;
    private static String filename;
    private static String[] labels = {"Fall", "Normal"};
    private TextView FallTextView, NormalTextView;
    private TextView firstTextView, secondTextView;
    private TextView logTextView;
    private TextToSpeech textToSpeech;
    private TensorFlowClassifier classifier;
    //private boolean setAudio=false;
    //private Button ToggleAudio;
    //private Switch ClassifierSwitch;
    //private Switch SoundSwitch;



    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        xc = new ArrayList<>();
        yc = new ArrayList<>();
        zc = new ArrayList<>();
        xg = new ArrayList<>();
        yg = new ArrayList<>();
        zg = new ArrayList<>();

        FallTextView = (TextView) findViewById(R.id.Fall_prob);
        NormalTextView = (TextView) findViewById(R.id.Normal_prob);



        /*ToggleAudio = (Button) findViewById(R.id.audioButton);
        ToggleAudio.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View V) {
                if (setAudio == true)
                    setAudio = false;
                else
                    setAudio = true;
            }
        });*/

        classifier = new TensorFlowClassifier(getApplicationContext());

        textToSpeech = new TextToSpeech(this, this);
        textToSpeech.setLanguage(Locale.US);
    }

    @Override
    public void onInit(int status) {
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (results == null || results.length == 0) {
                    return;
                }
                float max = -1;
                int idx = -1;
                for (int i = 0; i < results.length; i++) {
                    if (results[i] > max) {
                        idx = i;
                        max = results[i];
                    }
                }

                textToSpeech.speak(labels[idx], TextToSpeech.QUEUE_ADD, null, Integer.toString(new Random().nextInt()));
            }
        }, 2000, 5000);
    }
    protected void onPause() {
        getSensorManager().unregisterListener(this);
        super.onPause();
    }

    protected void onResume() {
        super.onResume();

        getSensorManager().registerListener(this, getSensorManager().getDefaultSensor(Sensor.TYPE_ACCELEROMETER), 20000);
        getSensorManager().registerListener(this, getSensorManager().getDefaultSensor(Sensor.TYPE_GYROSCOPE), 20000);
    }





    @Override
    public void onSensorChanged(SensorEvent event) {
        activityPrediction();
        //Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        xc.add(event.values[0]);
        yc.add(event.values[1]);
        zc.add(event.values[2]);
        xg.add(event.values[0]);
        yg.add(event.values[1]);
        zg.add(event.values[2]);
    }
    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }
    private void activityPrediction() {
        if (xc.size() == N_SAMPLES && yc.size() == N_SAMPLES && zc.size() == N_SAMPLES && xg.size() == N_SAMPLES && yg.size() == N_SAMPLES && zg.size() == N_SAMPLES) {
            List<Float> data = new ArrayList<>();
            data.addAll(xc);
            data.addAll(yc);
            data.addAll(zc);
            data.addAll(xg);
            data.addAll(yg);
            data.addAll(zg);

            results = classifier.predictProbabilities(toFloatArray(data));

            FallTextView.setText(Float.toString(round(results[0], 2)));
            NormalTextView.setText(Float.toString(round(results[1], 2)));

            xc.clear();
            yc.clear();
            zc.clear();
            xg.clear();
            yg.clear();
            zg.clear();
        }

    }
    private float[] toFloatArray(List<Float> list) {
        int i = 0;
        float[] array = new float[list.size()];

        for (Float f : list) {
            array[i++] = (f != null ? f : Float.NaN);
        }
        return array;
    }

    private static float round(float d, int decimalPlace) {
        BigDecimal bd = new BigDecimal(Float.toString(d));
        bd = bd.setScale(decimalPlace, BigDecimal.ROUND_HALF_UP);
        return bd.floatValue();
    }

    private SensorManager getSensorManager() {
        return (SensorManager) getSystemService(SENSOR_SERVICE);
    }

}
