package com.example.matt.stepup;

import android.content.Intent;
import android.database.Cursor;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Vector;

public class ViewData extends AppCompatActivity {
    DatabaseHelper myDb;
    Button btnBack, btnClear;
    TextView tv_data;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_data);
        myDb = new DatabaseHelper(this);

        btnBack = findViewById(R.id.button);
        btnClear = findViewById(R.id.del);
        tv_data = findViewById(R.id.tv_data);
        viewAll();
        viewBack();
        DeleteData();
    }

    public void viewAll() {
        Cursor res = myDb.getAllData();
        if (res.getCount() == 0){
            showMessage("Error", "Nothing found");
            return;
        }

        StringBuilder buffer = new StringBuilder();
        while(res.moveToNext()){
            //insert data backwards to show most recent data at top
            buffer.insert(0, "---------------------------------------------------------");
            buffer.insert(0, "Longitude: " + res.getDouble(4) + "\n");
            buffer.insert(0, "Latitude: " + res.getDouble(3) + "\n");
            buffer.insert(0, "Steps: " + res.getString(2) + "\n");
            buffer.insert(0, "Date & Time:" + res.getString(1) + "\n");
        }

        tv_data.setText(buffer.toString());
    }

    private void showMessage(String title, String Message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(true);
        builder.setTitle(title);
        builder.setMessage(Message);
        builder.show();
    }

    private void viewBack(){
        btnBack.setOnClickListener(
                new View.OnClickListener(){
                    @Override
                    public void onClick(View v){
                        startActivity(new Intent(ViewData.this, MainActivity.class));
                    }
                }
        );
    }

    public void DeleteData() {
        btnClear.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        myDb.clearData();
                        tv_data.setText("");
                    }
                }
        );
    }
}
