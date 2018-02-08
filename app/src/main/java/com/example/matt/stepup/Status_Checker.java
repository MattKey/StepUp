package com.example.matt.stepup;

import android.content.Intent;
import android.media.Rating;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RatingBar;

public class Status_Checker extends AppCompatActivity {

    RatingBar mBar;
    Button btn_sbt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_status__checker);

        mBar = findViewById(R.id.ratingBar);
        btn_sbt = findViewById(R.id.btn_submit);

        submit();
    }

    private void submit(){
        btn_sbt.setOnClickListener(
                new View.OnClickListener(){
                    @Override
                    public void onClick(View v){
                        int rating = mBar.getNumStars();
                        Log.d("ILON", String.valueOf(rating));
                        startActivity(new Intent(Status_Checker.this, MainActivity.class));
                    }
                }
        );
    }
}
