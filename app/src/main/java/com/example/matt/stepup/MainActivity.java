package com.example.matt.stepup;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.os.Vibrator;
import android.database.Cursor;
import android.os.VibrationEffect;
import android.support.annotation.FloatRange;
import android.support.v7.app.AlertDialog;
import android.view.WindowManager;
import android.widget.EditText;

//google location & places api libraries
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.GeoDataClient;
import com.google.android.gms.location.places.PlaceDetectionClient;
import com.google.android.gms.location.places.PlaceLikelihood;
import com.google.android.gms.location.places.PlaceLikelihoodBufferResponse;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import org.w3c.dom.Text;

import java.text.DateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    // TODO see line 123

    //sqlite variables
    DatabaseHelper myDb;
    String currDT;
    Button btn_str;

    //pedometer variables
    TextView tv_steps, tv_dateTime;
    SensorManager sensorManager;
    SharedPreferences sp;
    boolean running = false;
    float resumeStepsValue, currSteps, displaySteps;
    Button btn_res, btn_vw;

    //gps variables
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final String KEY_LOCATION = "location";
    private GeoDataClient mGeoDataClient;
    private boolean mLocationPermissionGranted;
    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;
    private FusedLocationProviderClient mFusedLocationProviderClient;
    private Location mLastKnownLocation;
    double mLat, mLon;
    TextView tv_LatLon;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //sqlite onCreate
        myDb = new DatabaseHelper(this);
        btn_str = findViewById(R.id.btn_store);
        btn_vw = findViewById(R.id.btn_view);
        AddData();
        viewAll();
        //UpdateData();
        //DeleteData();

        //pedometer onCreate
        tv_steps = findViewById(R.id.tv_steps);
        tv_dateTime = findViewById(R.id.tv_dateTime);
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        btn_res = findViewById(R.id.btn_reset);
        // set to 0 as initial value
        // resumeStepsValue = 0;
        sp = getSharedPreferences("steps", Context.MODE_PRIVATE);
        resumeStepsValue = sp.getFloat("rSV", 0.0f);

        //gps onCreate
        tv_LatLon = findViewById(R.id.tv_latLon);
        /*if (savedInstanceState != null) {
            mLastKnownLocation = savedInstanceState.getParcelable(KEY_LOCATION);
            getLocationPermission();
            getDeviceLocation();

            Toast.makeText(MainActivity.this, "Reached!", Toast.LENGTH_LONG).show();
        }*/
        //mLastKnownLocation = savedInstanceState.getParcelable(KEY_LOCATION);
        getLocationPermission();
        //getDeviceLocation();
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        //TODO: Location updating semi-successfully, keep at it
    }

    private void viewAll(){
        btn_vw.setOnClickListener(
                new View.OnClickListener(){
                    @Override
                    public void onClick(View v){
                        startActivity(new Intent(MainActivity.this, ViewData.class));
                    }
                }
        );
    }

    private void AddData() {
        currDT = DateFormat.getDateTimeInstance().format(new Date());
        getDeviceLocation();
        btn_str.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        boolean isInserted;
                        if(currDT != null) {
                            isInserted = myDb.insertData(currDT, Math.round(displaySteps), mLat, mLon);
                        }
                        else
                            isInserted = myDb.insertData("dateTime unavailable", Math.round(displaySteps), mLat, mLon);
                        if (isInserted) {
                            Toast.makeText(MainActivity.this, "Data Inserted", Toast.LENGTH_LONG).show();
                        } else
                            Toast.makeText(MainActivity.this, "Data not Inserted", Toast.LENGTH_LONG).show();
                    }
                }
        );
    }

    /*private void UpdateData() {
        final String currDT = DateFormat.getDateTimeInstance().format(new Date());
        btnviewUpdate.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        boolean isUpdate = myDb.insertData(currDT,
                                Math.round(displaySteps));
                        if (isUpdate)
                            Toast.makeText(MainActivity.this, "Data Update", Toast.LENGTH_LONG).show();
                        else
                            Toast.makeText(MainActivity.this, "Data not Updated", Toast.LENGTH_LONG).show();
                    }
                }
        );
    }*/

    /*public void showMessage(String title, String Message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(true);
        builder.setTitle(title);
        builder.setMessage(Message);
        builder.show();
    }*/

    @Override
    protected void onResume(){
        super.onResume();
        running = true;
        Sensor countSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        if(countSensor != null){

            sensorManager.registerListener(this, countSensor, SensorManager.SENSOR_DELAY_UI);
        }
        else {
            Toast.makeText(this, "Sensor not found!", Toast.LENGTH_SHORT).show();
        }

        sp = getSharedPreferences("steps", Context.MODE_PRIVATE);
        resumeStepsValue = sp.getFloat("rSV", 0.0f);

        displaySteps = currSteps - resumeStepsValue;
        tv_steps.setText(Float.toString(displaySteps));
    }

    @Override
    protected void onPause() {
        super.onPause();
        //running = false;
        //if you unregister the hardware will stop detecting steps
        //sensorManager.unregisterListener(this);
    }


    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if(running){
            currSteps = sensorEvent.values[0];

            if(resumeStepsValue == 0)
            {
                resumeStepsValue = currSteps;
                // save resumeStepsValue to shared preferences
                updateSP(resumeStepsValue);
            }

            displaySteps = currSteps - resumeStepsValue;

            tv_steps.setText(Float.toString(displaySteps));
            currDT = DateFormat.getDateTimeInstance().format(new Date());
            tv_dateTime.setText(currDT);

            getDeviceLocation();

        }
    }


    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    /*@Override             //MAY NOT NEED
    public void onStop(){
        super.onStop();
    }*/

    //vibrate when sensor stops tracking steps & location
    @Override
    public void onDestroy(){
        super.onDestroy();
        sensorManager.unregisterListener(this);
        Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        v.vibrate(1000);
    }

    //set steps to 0
    public void resetSteps(View view){
        resumeStepsValue = currSteps;
        float displaySteps = currSteps - resumeStepsValue;
        updateSP(resumeStepsValue);

        tv_steps.setText(Float.toString(displaySteps));

        getDeviceLocation();
    }

    //update sharedPreferences
    public void updateSP(float storeSteps){
        sp = getSharedPreferences("steps", Context.MODE_PRIVATE);
        SharedPreferences.Editor e = sp.edit();
        e.putFloat("rSV", storeSteps);
        e.commit();
    }

    //asks user for permission to get location
    private void getLocationPermission() {
        if(ContextCompat.checkSelfPermission(this.getApplicationContext(),
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mLocationPermissionGranted = true;
        }
        else{
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }
    }

    private void getDeviceLocation() {
        try{
            if (mLocationPermissionGranted) {
                Task<Location> locationResult = mFusedLocationProviderClient.getLastLocation();
                locationResult.addOnCompleteListener(this, new OnCompleteListener<Location>() {
                    @Override
                    public void onComplete(@NonNull Task<Location> task) {
                        if(task.isSuccessful()) {
                            mLastKnownLocation = task.getResult();

                            mLat = mLastKnownLocation.getLatitude();
                            mLon = mLastKnownLocation.getLongitude();

                            tv_LatLon.setText("Lat: " + mLat + "\nLon: " + mLon);
                        }
                        else {
                            Log.d(TAG, "Current location is null. Using defaults.");
                            Log.e(TAG, "Exception: %s", task.getException());

                            /*mLat = 0.0;
                            mLon = 0.0;*/
                        }
                    }
                });
            }
        } catch (SecurityException e) {
            Log.e("Exception: %s", e.getMessage());
        }
    }
}
