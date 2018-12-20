package com.dyr.myapp;

/**
 * Created by sony on 2016/7/13.
 */
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ProgressBar;

public class WatingActivity extends Activity {
    private String fileName;
    private double  fatigueValue;
    private int detectResult;
    private boolean detected=false;
    //控件
    private ImageButton btnBack;
    private ProgressBar barWaiting;

    private Handler handler_tip = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            //3s后执行代码
        }
    };
    private Handler handler_check = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            //3s后执行代码
        }
    };

    private Runnable timeRun_tip = new Runnable(){
        @Override
        public void run(){
            if(detected)
            {
                //停止旋转
                barWaiting.setIndeterminateDrawable(getResources().getDrawable(
                        R.drawable.loading_01));
                barWaiting.setProgressDrawable(getResources().getDrawable(
                        R.drawable.loading_01));

                //显示结果
                showValueDialog();
            }
        }
    };
    private Runnable timeRun_check = new Runnable(){
        @Override
        public void run(){
            detectResult=GetValue(fileName);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
        setContentView(R.layout.activity_waiting);
        getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE,R.layout.f1mytitlebar);
        //新页面接收数据
        Bundle bundle = this.getIntent().getExtras();
        //接收name值
        fileName = bundle.getString("filename");
        //Log.i("获取到的name值为",fileName);

        btnBack =(ImageButton)findViewById(R.id.backimage);
        btnBack.setOnClickListener(listener);
        barWaiting=(ProgressBar)findViewById(R.id.waitingProgressBar);

        detectResult=GetValue(fileName);
        //handler_check.postDelayed(timeRun_check, 0);
        handler_tip.postDelayed(timeRun_tip, 5000);
    }

    private View.OnClickListener listener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.backimage:
                    WatingActivity.this.finish();
                    break;
            }
        }
    };

    //返回值意味着检测是否成功，0表示不成功，1表示成功
    private int GetValue(String fileName){
        int detectPeople=0;
        /*try {
            Thread.currentThread().sleep(5000);//阻断2秒
        } catch (InterruptedException e) {
            e.printStackTrace();
        }*/
        detected=true;
        return 0;
    }

    private void showValueDialog() {
        final AlertDialog dialog = new AlertDialog.Builder(WatingActivity.this).create();
        dialog.show();
        Window window = dialog.getWindow();
        dialog.setCanceledOnTouchOutside(false);
        window.setGravity(Gravity.BOTTOM);
        // 设置布局
        window.setContentView(R.layout.value_alertdialog);
        // 设置宽高
        window.setLayout(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
        // 设置弹出的动画效果
        window.setWindowAnimations(R.style.AnimBottom);
        // 设置监听
        ImageButton home = (ImageButton) window.findViewById(R.id.btn_home);
        ImageButton share = (ImageButton) window.findViewById(R.id.btn_cross);
        home.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                    Intent intent = new Intent();
                    intent.setClass(WatingActivity.this, MainActivity.class);
                    startActivity(intent);
                dialog.cancel();
            }
        });
        share.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                dialog.cancel();
                showShareDialog();
            }
        });
        // 因为我们用的是windows的方法，所以不管ok活cancel都要加上“dialog.cancel()”这句话，
        // 不然有程序崩溃的可能，仅仅是一种可能，但我们还是要排除这一点，对吧？
        // 用AlertDialog的两个Button，即使监听里什么也不写，点击后也是会吧dialog关掉的，不信的同学可以去试下
    }

    private void showShareDialog() {
        final AlertDialog dialog = new AlertDialog.Builder(WatingActivity.this).create();
        dialog.show();
        Window window = dialog.getWindow();
        dialog.setCanceledOnTouchOutside(false);
        window.setGravity(Gravity.BOTTOM);
        // 设置布局
        window.setContentView(R.layout.share_alertdialog);
        // 设置宽高
        window.setLayout(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
        // 设置弹出的动画效果
        window.setWindowAnimations(R.style.AnimBottom);
        // 设置监听
        ImageButton cross = (ImageButton) window.findViewById(R.id.btn_cross);
        ImageButton wechat = (ImageButton) window.findViewById(R.id.share_wechat);
        ImageButton moment = (ImageButton) window.findViewById(R.id.share_moment);
        ImageButton qq = (ImageButton) window.findViewById(R.id.share_qq);
        ImageButton weibo = (ImageButton) window.findViewById(R.id.share_weibo);
        cross.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                dialog.cancel();
            }
        });
        wechat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                dialog.cancel();
            }
        });
        moment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                dialog.cancel();
            }
        });
        qq.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                dialog.cancel();
            }
        });
        weibo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                dialog.cancel();
            }
        });
    }
}

