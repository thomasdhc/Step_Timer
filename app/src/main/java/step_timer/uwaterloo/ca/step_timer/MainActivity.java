package step_timer.uwaterloo.ca.step_timer;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;

import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity
{
    TextView accelR;
    TextView highAccel;
    Button record;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        LinearLayout lin = (LinearLayout) findViewById(R.id.linear);
        lin.setOrientation(LinearLayout.VERTICAL);

        accelR=new TextView(getApplicationContext());
        highAccel = new TextView (getApplicationContext());
        record = (Button) findViewById(R.id.button);

        accelR.setTextColor(Color.parseColor("#000000"));
        highAccel.setTextColor(Color.parseColor("#000000"));

        SensorManager sensorManager = (SensorManager) getSystemService (SENSOR_SERVICE);
        Sensor accelSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);

        highAccel.setText("Max Accelerometer (x,y,z): 0,0,0");
        AccelerometerSensorEventListener a = new AccelerometerSensorEventListener(accelR, record, getApplicationContext());

        sensorManager.registerListener(a, accelSensor, SensorManager.SENSOR_DELAY_FASTEST);
        verifyStoragePermissions(this);
        lin.addView(accelR);
        lin.addView(highAccel);
    }

    // Storage Permissions
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    // For app to write a text file to external storage, it must ask user for permission
    public static void verifyStoragePermissions(Activity activity)
    {
        // See if app has write permission
        int permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED)
        {
            // If app does not have write permission ask for it
            ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
        }
    }

}

// Class listens for accelerometer event change
class AccelerometerSensorEventListener implements SensorEventListener
{
    Context context;
    TextView output;
    Button record;
    boolean pressed = false;
    int stepTimer = 10;
    long startTime = 0;
    long endTime = 0;
    float[] smoothedAccel = new float[3];
    List<Float> stepValues = new ArrayList<Float>();
    FileOutputStream os;
    PrintWriter out;

    public AccelerometerSensorEventListener(TextView outputView, Button recordStep, Context context)
    {
        output = outputView;
        record = recordStep;
        this.context=context;
        record.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if (record.getText().equals("Start"))
                {
                    pressed = true;
                    record.setText("Stop");
                    startTime = System.currentTimeMillis();
                    stepValues.clear();
                }
                else
                {
                    stopRecording();
                    pressed = false;
                    record.setText("Start");
                }
            }
        });
        TimerTask timerTask = new TimerTask()
        {
            @Override
            public void run()
            {
                if (pressed=true)
                {
                    startRecording();
                }
            }
        };
        Timer timer = new Timer();
        timer.schedule(timerTask, stepTimer, stepTimer);
    }

    public void onAccuracyChanged(Sensor s, int i) {
    }

    public void onSensorChanged(SensorEvent se)
    {
        if (se.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION)
        {

            smoothedAccel[0] += (se.values[0] - smoothedAccel[0]) / 10;
            smoothedAccel[1] += (se.values[1] - smoothedAccel[1]) / 10;
            smoothedAccel[2] += (se.values[2] - smoothedAccel[2]) / 10;

            String s1 = "Accelerometer Reading (x,y,z): " + String.format("%f,%f,%f", smoothedAccel[0], smoothedAccel[1], smoothedAccel[2]);
            output.setText(s1);
        }
    }

    //Once start is pressed add values to list
    public void startRecording()
    {
       stepValues.add(smoothedAccel[2]);
    }

    //Once stop is pressed write the list values. We want exactly 70 points of accelerometer readings.
    public void stopRecording()
    {
        endTime = System.currentTimeMillis();
        if (stepValues.size() > 70 )
        {
            int total = stepValues.size();
            int discard = stepValues.size() - 70;
            int divsion= stepValues.size()/discard;
            //Log.i("Finsihed recording", stepValues.size() + " " + discard + " " + divsion);
            for (int x =0 ; x< discard ; x++)
            {
                stepValues.remove (total - (divsion*x+1));
            }
        }
        File sdCard = Environment.getExternalStorageDirectory();
        File dir = new File (sdCard.getAbsolutePath()+"/Download");
        dir.mkdirs();
        File file = new File(dir, "stepvalues.txt");
        
        try
        {
            os = new FileOutputStream(file, true);
            out = new PrintWriter(os);

            for (int x =0 ; x< stepValues.size() ; x++)
            {
                Log.i ("Record:", stepValues.get(x)+" ");
                out.print(stepValues.get(x) + " ");
            }
            out.println (endTime-startTime);
            out.close();
            os.close();
        }
        catch (FileNotFoundException f)
        {
            Log.e("FileNotFoundException", "File was not found");
        }
        catch (IOException e)
        {
            Log.e("IOException", "IOException occured");
        }
    }

}


