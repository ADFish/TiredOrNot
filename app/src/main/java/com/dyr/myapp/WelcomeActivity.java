package com.dyr.myapp;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import java.util.Timer;
import java.util.TimerTask;


/**
 * Created by sony on 2016/7/6.
 */
public class WelcomeActivity extends Activity{
    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.welcome);
        Timer timer=new Timer();
        TimerTask task=new TimerTask(){
            public void run(){
                Intent intent=new Intent(WelcomeActivity.this,MainActivity.class); //通过Intent实现跳转
                startActivity(intent);
                finish();
            }
        };
        timer.schedule(task,1000*2);
    }
}
