package step_timer.uwaterloo.ca.step_timer;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
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
    Button record;
    Spinner stepSpinner;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        LinearLayout lin = (LinearLayout) findViewById(R.id.linear);
        lin.setOrientation(LinearLayout.VERTICAL);

        accelR=new TextView(getApplicationContext());
        record = (Button) findViewById(R.id.button);
        stepSpinner = (Spinner) findViewById(R.id.spinner1);

        accelR.setTextColor(Color.parseColor("#000000"));

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.step_options, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource (android.R.layout.simple_spinner_dropdown_item);
        stepSpinner.setAdapter(adapter);

        SensorManager sensorManager = (SensorManager) getSystemService (SENSOR_SERVICE);
        Sensor accelSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);

        AccelerometerSensorEventListener a = new AccelerometerSensorEventListener(accelR, record, stepSpinner, getApplicationContext());

        sensorManager.registerListener(a, accelSensor, SensorManager.SENSOR_DELAY_FASTEST);
        verifyStoragePermissions(this);
        lin.addView(accelR);
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
    Spinner stepSpinner;
    boolean pressed = false;
    int stepTimer = 10;
    int recorded =0;
    float[] smoothedAccel = new float[3];
    List<Float> stepValuesX = new ArrayList<Float>();
    List<Float> stepValuesY = new ArrayList<Float>();
    List<Float> stepValuesZ = new ArrayList<Float>();
    FileOutputStream os;
    PrintWriter out;

    public final Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            record.setText("Start");
        }
    };

    public AccelerometerSensorEventListener(TextView outputView, final Button recordStep, Spinner stepTypeSpinner, Context context)
    {
        output = outputView;
        record = recordStep;
        this.context=context;
        stepSpinner = stepTypeSpinner;
        record.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if (record.getText().equals("Start"))
                {
                    pressed = true;
                    record.setText("Recording");
                }
            }
        });
        TimerTask timerTask = new TimerTask()
        {
            @Override
            public void run()
            {
                if (pressed)
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
        recorded++;
        stepValuesX.add(smoothedAccel[0]);
        stepValuesY.add(smoothedAccel[1]);
        stepValuesZ.add(smoothedAccel[2]);
        if (stepSpinner.getSelectedItem().equals("Slow Step") && recorded == 71)
        {
            recorded = 0;
            mHandler.obtainMessage(1).sendToTarget();
            pressed = false;
            stopRecording();
        }
        if (stepSpinner.getSelectedItem().equals("Normal Step") && recorded == 56)
        {
            recorded = 0;
            mHandler.obtainMessage(1).sendToTarget();
            pressed = false;
            scalingStep ("Normal Step");
            stopRecording();
        }
        if (stepSpinner.getSelectedItem().equals("Fast Step") && recorded == 41)
        {
            recorded = 0;
            mHandler.obtainMessage(1).sendToTarget();
            pressed = false;
            scalingStep ("Fast Step");
            stopRecording();
        }
    }

    //Once stop is pressed write the list values. We want exactly 70 points of accelerometer readings.
    public void stopRecording()
    {

        File sdCard = Environment.getExternalStorageDirectory();
        File dir = new File (sdCard.getAbsolutePath()+"/Download");
        dir.mkdirs();
        File file = new File(dir, "stepvalues.txt");

        try
        {
            os = new FileOutputStream(file, true);
            out = new PrintWriter(os);
            if (stepSpinner.getSelectedItem().equals("Not Step"))
            {
                out.println("0");
            }
            else
            {
                out.println ("1");
            }
            for (int x = 0; x < 70; x++)
            {
                out.print(stepValuesX.get(x) + " ");
            }
            for (int y = 0; y < 70; y++)
            {
                out.print(stepValuesY.get(y) + " ");
            }
            for (int z = 0; z < 70; z++)
            {
                out.print(stepValuesZ.get(z) + " ");
            }
            out.println ();

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
        stepValuesX.clear();
        stepValuesY.clear();
        stepValuesZ.clear();
    }

    public void scalingStep (String scalingType)
    {
        if (scalingType.equals("Normal Step"))
        {
            for (int x =1; x< 16 ; x++)
            {
                stepValuesX.add(55-x*3, stepValuesX.get(55 - x*3 - 1) +(stepValuesX.get(55 - x*3)-stepValuesX.get(55 - x*3 - 1))/2);
                stepValuesY.add(55-x*3, stepValuesY.get(55 - x*3 - 1) +(stepValuesY.get(55 - x*3)-stepValuesY.get(55 - x*3 - 1))/2);
                stepValuesZ.add(55-x*3, stepValuesZ.get(55 - x*3 - 1) +(stepValuesZ.get(55 - x*3)-stepValuesZ.get(55 - x*3 - 1))/2);
            }
        }
        else
        {
            for (int x =1; x< 31 ; x++)
            {
                stepValuesX.add(40 - x, stepValuesX.get(40 - x - 1) +(stepValuesX.get(40 - x)-stepValuesX.get(40 - x - 1))/2);
                stepValuesY.add(40 - x, stepValuesY.get(40 - x - 1) +(stepValuesY.get(40 - x)-stepValuesY.get(40 - x - 1))/2);
                stepValuesZ.add(40 - x, stepValuesZ.get(40 - x - 1) +(stepValuesZ.get(40 - x)-stepValuesZ.get(40 - x - 1))/2);
            }
        }
    }
}


