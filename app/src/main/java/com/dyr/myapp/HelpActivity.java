package com.dyr.myapp;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageButton;


/**
 * Created by sony on 2016/7/7.
 */
public class HelpActivity extends Activity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
        setContentView(R.layout.help);
        getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE,R.layout.mytitlebar);
        ImageButton backbutton=(ImageButton)findViewById(R.id.backimage);
        backbutton.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setClass(HelpActivity.this, MainActivity.class);
                startActivity(intent);
            }
        });
    }
}
