package edu.wisc.ece.pinpoint;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;

public class pinList extends AppCompatActivity {
    ImageButton button1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pin_list);

        if(Build.VERSION.SDK_INT > 25){
            button1 = findViewById(R.id.imageButton);
            button1.setTooltipText("PIN 1 with details");
            button1.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    openPinView();
                }
            });

        }
    }

    public void openPinView(){
        Intent intent = new Intent(this, PinViewActivity.class);
        startActivity(intent);

    }
}