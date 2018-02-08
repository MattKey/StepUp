package com.example.matt.stepup;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInstaller;
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
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.LogPrinter;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.os.Vibrator;
import android.database.Cursor;
import android.support.v7.app.AlertDialog;
import android.os.VibrationEffect;
import android.support.annotation.FloatRange;
import android.support.v7.app.AlertDialog;
import android.view.WindowManager;
import android.widget.EditText;

//google location & places api libraries
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import java.io.Console;
import java.io.File;
import java.io.IOException;
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
    //sqlite variables
    DatabaseHelper myDb;
    String currDT;
    //Button btn_str;


    //pedometer variables
    TextView tv_steps, tv_dateTime;
    SensorManager sensorManager;
    SharedPreferences sp;
    boolean running;
    float resumeStepsValue, currSteps, displaySteps;
    Button btn_res, btn_vw;

    //gps variables
    private static final String TAG = MainActivity.class.getSimpleName();
    //private static final String KEY_LOCATION = "location";
    //private GeoDataClient mGeoDataClient;
    private static final String KEY_LOCATION = "location";
    private GeoDataClient mGeoDataClient;
    private boolean mLocationPermissionGranted;
    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;
    private FusedLocationProviderClient mFusedLocationProviderClient;
    private Location mLastKnownLocation;
    double mLat, mLon;
    TextView tv_LatLon;

    //email variables
    PackageInstaller.Session session = null;
    ProgressDialog pdialog = null;
    Context ctx = null;
    String rec, subject, textMessage;

    //misc variables
    Thread t;
    Thread q;
    Button btn_start, btn_stop, btn_pic;
    boolean dialogShown = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //sqlite onCreate
        myDb = new DatabaseHelper(this);
        btn_vw = findViewById(R.id.btn_view);
        viewAll();
        //btn_str = findViewById(R.id.btn_store);
        btn_vw = findViewById(R.id.btn_view);
        //AddData();
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
        getLocationPermission();
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        btn_start = findViewById(R.id.btn_start);
        btn_stop = findViewById(R.id.btn_stop);
        btn_pic = findViewById(R.id.btn_pic);
        startRun();
        startListening();
        takePic();

        setThreadPrime();
        setQuestionThread();

        //TODO: running sensor: set a timer to ask if exercising? maybe talk to dr. li & dong about not automatically sensing
        //TODO: may not need reset button anymore, since resets at end of walk
        //TODO: set things behind a "finalized" UI (simplify it)
        //TODO: add question intervals & store results in separate table
        //TODO: add ability to save photos
        //TODO: bug when switching screens, asks again if exercising when switching back to main Activity
        //TODO:             -somehow bool running is switching to false at activity switch
    }

    private void setThreadPrime(){
        t = new Thread(){
            @Override
            public void run(){
                while(running){
                    try{
                        Thread.sleep(5000);

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                AutoAddData();
                            }
                        });

                    }catch (InterruptedException e){
                        e.printStackTrace();
                    }
                }
            }
        };
    }

    private void setQuestionThread() {
        q = new Thread() {
            @Override
            public void run() {
                while (running) {
                    try {
                        Thread.sleep(10000);

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                AutoQuestion();
                            }
                        });

                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
    }
        /*if (savedInstanceState != null) {
            mLastKnownLocation = savedInstanceState.getParcelable(KEY_LOCATION);
            getLocationPermission();
            getDeviceLocation();

            Toast.makeText(MainActivity.this, "Reached!", Toast.LENGTH_LONG).show();
        }*/
        //mLastKnownLocation = savedInstanceState.getParcelable(KEY_LOCATION);
        //getLocationPermission();
        //getDeviceLocation();
        //mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

    private void viewAll(){
        btn_vw.setOnClickListener(
                new View.OnClickListener(){
                    @Override
                    public void onClick(View v){
                        updateSP(resumeStepsValue);
                        startActivity(new Intent(MainActivity.this, ViewData.class));
                    }
                }
        );
    }

    private void startRun(){
        btn_start.setOnClickListener(
                new View.OnClickListener(){
                    @Override
                    public void onClick(View v){
                        resetSteps(v);
                        setThreadPrime();
                        t.start();
                        //q.start();
                        running = true;
                        Log.d("ILON", "startRun reached!");
                        //startListening();
                    }
                }
        );
    }

    private void takePic(){
        btn_pic.setOnClickListener(
                new View.OnClickListener(){
                    @Override
                    public void onClick(View v){
                        dispatchTakePictureIntent();
                    }
                }
        );
    }

    private void startListening(){
        Sensor countSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        if (countSensor != null) {
            sensorManager.registerListener(this, countSensor, SensorManager.SENSOR_DELAY_UI);
        } else {
            Toast.makeText(this, "Sensor not found!", Toast.LENGTH_SHORT).show();
        }
    }

    //used to manually add data
    /*private void AddData() {
        //ensure updated information
        currDT = DateFormat.getDateTimeInstance().format(new Date());
        getDeviceLocation();
        displaySteps = currSteps - resumeStepsValue;

    private void AddData() {
        currDT = DateFormat.getDateTimeInstance().format(new Date());
        getDeviceLocation();
        btn_str.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        boolean isInserted;
                        String stringLat = String.valueOf(mLat);
                        String stringLon = String.valueOf(mLon);
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
    }*/

    private void AutoAddData(){
        currDT = DateFormat.getDateTimeInstance().format(new Date());
        getDeviceLocation();
        displaySteps = currSteps - resumeStepsValue;
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

    private void AutoQuestion(){
        Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        v.vibrate(500);
        v.vibrate(500);
        startActivity(new Intent(MainActivity.this, Status_Checker.class));
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
        //startListening();
        if(running) {
            //startListening();

            sp = getSharedPreferences("steps", Context.MODE_PRIVATE);
            resumeStepsValue = sp.getFloat("rSV", 0.0f);
            displaySteps = currSteps - resumeStepsValue;
        }

        //displaySteps = currSteps - resumeStepsValue;
        tv_steps.setText(Float.toString(displaySteps));
    }

    @Override
    protected void onPause() {
        super.onPause();
        //running = false;
        //if you unregister the hardware will stop detecting steps
        //sensorManager.unregisterListener(this);
    }

    protected void calcSteps(){
        if (resumeStepsValue == 0) {
            resumeStepsValue = currSteps;
            // save resumeStepsValue to shared preferences
            updateSP(resumeStepsValue);
        }

        displaySteps = currSteps - resumeStepsValue;
    }


    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        Log.d("ILON", "sensor change detected, running is " + running);

        currSteps = sensorEvent.values[0];
        if(!running) {
            //ask user if exercising, prompt to begin exercise
            DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    switch (which) {
                        case DialogInterface.BUTTON_POSITIVE:
                            //Yes button clicked
                            calcSteps();
                            btn_start.performClick();
                            dialogShown = false;
                            break;

                        case DialogInterface.BUTTON_NEGATIVE:
                            //No button clicked
                            dialogShown = false;
                            break;
                    }
                }
            };

            //prevent multiple dialogs from popping up
            if (!dialogShown) {
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setMessage("Begin exercise?").setPositiveButton("Yes", dialogClickListener)
                        .setNegativeButton("No", dialogClickListener).show();
                dialogShown = true;
            }
        }else {
            calcSteps();
        }

        /*if (running) {
            calcSteps();
        } else {
            //ask user if exercising, prompt to begin exercise
            DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    switch (which) {
                        case DialogInterface.BUTTON_POSITIVE:
                            //Yes button clicked
                            calcSteps();
                            btn_start.performClick();
                            dialogShown = false;
                            break;

                        case DialogInterface.BUTTON_NEGATIVE:
                            //No button clicked
                            dialogShown = false;
                            break;
                    }
                }
            };

            //prevent multiple dialogs from popping up
            if (!dialogShown) {
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setMessage("Begin exercise?").setPositiveButton("Yes", dialogClickListener)
                        .setNegativeButton("No", dialogClickListener).show();
                dialogShown = true;
            }
        }*/
        tv_steps.setText(Float.toString(displaySteps));
        currDT = DateFormat.getDateTimeInstance().format(new Date());
        tv_dateTime.setText(currDT);

        getDeviceLocation();
    }

    /*@Override
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
    }*/


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
        try {
            t.join();
            //q.join();
            Log.d("ILON", "thread stopped successfully");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    //set steps to 0
    public void resetSteps(View view){
        resumeStepsValue = currSteps;
        float displaySteps = currSteps - resumeStepsValue;
        updateSP(resumeStepsValue);

        tv_steps.setText(Float.toString(displaySteps));

        getDeviceLocation();
    }

    public void stopRun(View view){
        Log.d("ILON", "stopRun Reached!");
        running = false;
        resetSteps(view);
        sendEmail();
        //sensorManager.unregisterListener(this);
    }


    //redirects to email client
    protected void sendEmail(){
        Log.i("Send email", "");

        StringBuilder emailText = new StringBuilder();
        DatabaseHelper myDb = new DatabaseHelper(this);
        Cursor res = myDb.getAllData();
        if (res.getCount() == 0){
            emailText.insert(0, "No data to show");
            return;
        }
        else{
            while(res.moveToNext()){
                //insert data backwards to show most recent data at top
                emailText.insert(0, "\n");
                emailText.insert(0, "Longitude: " + res.getDouble(4) + "\n");
                emailText.insert(0, "Latitude: " + res.getDouble(3) + "\n");
                emailText.insert(0, "Steps: " + res.getString(2) + "\n");
                emailText.insert(0, "Date & Time:" + res.getString(1) + "\n");
            }

            emailText.insert(0, "Please do not edit the following text:\n\n\n\n\n\n");
        }

        String[] TO = {"keynode@mail.fresnostate.edu"};
        Intent emailIntent = new Intent(Intent.ACTION_SEND);
        emailIntent.setData(Uri.parse("mailto:"));
        emailIntent.setType("text/plain");


        emailIntent.putExtra(Intent.EXTRA_EMAIL, TO);
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "CSU Fresno Research Data");
        emailIntent.putExtra(Intent.EXTRA_TEXT, emailText.toString());

        try {
            startActivity(Intent.createChooser(emailIntent, "Send mail..."));
            finish();
            Log.i("ILON", "Finished sending email...");
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(MainActivity.this,
                    "There is no email client installed.", Toast.LENGTH_SHORT).show();
        }
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

    static final int REQUEST_IMAGE_CAPTURE = 1;
    private void dispatchTakePictureIntent(){
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "com.example.android.fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        }
    }

    String mCurrentPhotoPath;
    private File createImageFile() throws IOException {
        //Create an image file name
        //String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "Photos_from_Run";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = image.getAbsolutePath();
        return image;
    }
}
