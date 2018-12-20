package com.dyr.myapp;

/**
 * Created by sony on 2016/7/13.
 */
import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.File;
import java.io.IOException;

public class F1MainActivity extends Activity implements SurfaceHolder.Callback {

    private File myRecVideoFile;
    private SurfaceView mSurfaceView;
    private SurfaceHolder mSurfaceHolder;
    private TextView tvTime;
    private TextView tvSize;
    private ImageButton btnStart;
    private ImageButton btnRotate;
    private ImageButton btnBack;
    private ProgressBar videoBar;
    private MediaRecorder recorder;
    private Handler handler;
    private Camera camera;
    private boolean recording; // 记录是否正在录像,fasle为未录像, true 为正在录像
    private int minute = 0;
    private int second = 0;
    private String time="";
    private String size="";
    private String fileName;
    private String name="";
    private int CammeraIndex=0;

    /**
     * 录制过程中,时间变化,大小变化
     */
    private Runnable timeRun = new Runnable() {

        @Override
        public void run() {
            /*long fileLength=myRecVideoFile.length();
            if(fileLength<1024 && fileLength>0){
                size=String.format("%dB/10M", fileLength);
            }else if(fileLength>=1024 && fileLength<(1024*1024)){
                fileLength=fileLength/1024;
                size=String.format("%dK/10M", fileLength);
            }else if(fileLength>(1024*1024*1024)){
                fileLength=(fileLength/1024)/1024;
                size=String.format("%dM/10M", fileLength);
            }*/
            if(second==21)
            {
                if(recorder!=null){
                    releaseMediaRecorder();
                    minute = 0;
                    second = 0;
                    handler.removeCallbacks(timeRun);
                    recording = false;
                }
                ToastUtil.OKTextToast(getApplicationContext(), ToastUtil.LENGTH_SHORT);
                videoBar.setProgress(0);
                videoBar.setVisibility(View.GONE);

                //转到待检测页
                //新建一个显式意图，第一个参数为当前Activity类对象，第二个参数为你要打开的Activity类
                Intent intent = new Intent();
                intent.setClass(F1MainActivity.this,WatingActivity.class);
                //用Bundle携带数据
                Bundle bundle=new Bundle();
                bundle.putString("filename", fileName);
                intent.putExtras(bundle);
                startActivity(intent);

                /*btnStart.setVisibility(View.VISIBLE);
                btnRotate.setVisibility(View.VISIBLE);
                time = String.format("%02d:%02d", 0, 0);
                size = String.format("%02dS/20S", 0);
                MainActivity.this.finish();*/
            }

            if(recording)
            {
                videoBar.setProgress(second);
                time = String.format("%02d:%02d", minute, second);
                size = String.format("%02dS/20S", second);
                tvSize.setText(size);
                tvTime.setText(time);
                handler.postDelayed(timeRun, 1000);
                second++;
                if (second == 60) {
                    minute++;
                    second = 0;
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
        setContentView(R.layout.activity_main);
        getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE,R.layout.mychecktitlebar);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        mSurfaceView = (SurfaceView) findViewById(R.id.videoView);
        mSurfaceHolder = mSurfaceView.getHolder();
        mSurfaceHolder.addCallback(this);
        mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        mSurfaceHolder.setKeepScreenOn(true);
        handler = new Handler();
        tvTime = (TextView) findViewById(R.id.tv_video_time);
        tvSize=(TextView)findViewById(R.id.tv_video_size);
        //btnStop = (Button) findViewById(R.id.btn_video_stop);
        btnStart = (ImageButton) findViewById(R.id.btn_video_start);
        btnRotate =(ImageButton)findViewById(R.id.RotateButton);
        btnBack =(ImageButton)findViewById(R.id.backimage);
        videoBar=(ProgressBar)findViewById(R.id.video_Bar);
        videoBar.setMax(20);
        videoBar.setVisibility(View.GONE);
        btnStart.setOnClickListener(listener);
        //btnStop.setOnClickListener(listener);
        btnRotate.setOnClickListener(listener);
        btnBack.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setClass(F1MainActivity.this, MainActivity.class);
                startActivity(intent);
            }
        });
        // 设置sdcard的路径
        fileName = Environment.getExternalStorageDirectory().getAbsolutePath();
        name="video_" +System.currentTimeMillis() + ".mp4";
        fileName += File.separator + File.separator+"Fatigue_value_check"+File.separator+name;
    }

    @Override
    protected void onResume() {
        super.onResume();
        btnStart.setVisibility(View.VISIBLE);
        btnRotate.setVisibility(View.VISIBLE);
        time = String.format("%02d:%02d", 0, 0);
        size = String.format("%02dS/20S", 0);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        // 开启相机
        if (camera == null) {
            int CammeraIndex=FindBackCamera();
            if(CammeraIndex==-1){
                ToastUtil.TextToast(getApplicationContext(), "您的手机不支持后置摄像头", ToastUtil.LENGTH_SHORT);
                CammeraIndex=FindFrontCamera();
            }
            camera = Camera.open(CammeraIndex);
            try {
                camera.setPreviewDisplay(mSurfaceHolder);
                camera.setDisplayOrientation(90);
            } catch (IOException e) {
                e.printStackTrace();
                camera.release();
            }
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        // 开始预览
        camera.startPreview();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        // 关闭预览并释放资源
        if (camera != null) {
            camera.stopPreview();
            camera.release();
            camera = null;
        }
    }

    private View.OnClickListener listener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.btn_video_start:
                    if(recorder!=null){
                        releaseMediaRecorder();
                        minute = 0;
                        second = 0;
                        handler.removeCallbacks(timeRun);
                        recording = false;
                    }
                    recorder();
                    videoBar.setProgress(0);
                    videoBar.setVisibility(View.VISIBLE);
                    btnStart.setVisibility(View.INVISIBLE);
                    btnRotate.setVisibility(View.INVISIBLE);
                    //btnStart.setEnabled(false);
                    break;
                case R.id.RotateButton:
                    rotateCamera();
                    break;
                case R.id.backimage:
                    releaseMediaRecorder();
                    handler.removeCallbacks(timeRun);
                    minute=0;
                    second=0;
                    recording = false;
                    videoBar.setProgress(0);
                    F1MainActivity.this.finish();
                    break;
            }
        }
    };
    //判断前置摄像头是否存在
    private int FindFrontCamera(){
        int cameraCount = 0;
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        cameraCount = Camera.getNumberOfCameras(); // get cameras number

        for ( int camIdx = 0; camIdx < cameraCount;camIdx++ ) {
            Camera.getCameraInfo( camIdx, cameraInfo ); // get camerainfo
            if ( cameraInfo.facing ==Camera.CameraInfo.CAMERA_FACING_FRONT ) {
                // 代表摄像头的方位，目前有定义值两个分别为CAMERA_FACING_FRONT前置和CAMERA_FACING_BACK后置
                return camIdx;
            }
        }
        return -1;
    }
    //判断后置摄像头是否存在
    private int FindBackCamera(){
        int cameraCount = 0;
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        cameraCount = Camera.getNumberOfCameras(); // get cameras number

        for ( int camIdx = 0; camIdx < cameraCount;camIdx++ ) {
            Camera.getCameraInfo( camIdx, cameraInfo ); // get camerainfo
            if ( cameraInfo.facing ==Camera.CameraInfo.CAMERA_FACING_BACK ) {
                // 代表摄像头的方位，目前有定义值两个分别为CAMERA_FACING_FRONT前置和CAMERA_FACING_BACK后置
                return camIdx;
            }
        }
        return -1;
    }

    //转换镜头
    private void rotateCamera(){
        if (camera != null) {
            camera.stopPreview();
            camera.release();
            camera = null;
            if(CammeraIndex==0)//后置转前置
            {
                int find=FindFrontCamera();
                if(find==-1){
                    ToastUtil.TextToast(getApplicationContext(), "您的手机不支持前置摄像头", ToastUtil.LENGTH_SHORT);
                }
                else
                    CammeraIndex=1;
            }
            else if(CammeraIndex==1)//前置转后置
            {
                int find=FindBackCamera();
                if(find==-1){
                    ToastUtil.TextToast(getApplicationContext(), "您的手机不支持后置摄像头", ToastUtil.LENGTH_SHORT);
                }
                else
                    CammeraIndex=0;
            }
            camera = Camera.open(CammeraIndex);
            try {
                camera.setPreviewDisplay(mSurfaceHolder);
                camera.setDisplayOrientation(90);
            } catch (IOException e) {
                e.printStackTrace();
                camera.release();
            }
            camera.startPreview();
        }
    }

    //释放recorder资源
    private void releaseMediaRecorder(){
        if (recorder != null) {
            recorder.stop();
            recorder.release();
            recorder = null;
        }
    }
    //开始录像
    public void recorder() {
        if (!recording) {
            try {
                // 关闭预览并释放资源
                if(camera!=null){
                    camera.stopPreview();
                    camera.release();
                    camera = null;
                }
                recorder = new MediaRecorder();
                // 声明视频文件对象
                myRecVideoFile = new File(fileName);
                if(!myRecVideoFile.exists()){
                    myRecVideoFile.getParentFile().mkdirs();
                    myRecVideoFile.createNewFile();
                }

                recorder.reset();
                // 0:前置摄像头，1:后置摄像头

                camera = Camera.open(CammeraIndex);
                // 设置摄像头预览顺时针旋转90度，才能使预览图像显示为正确的，而不是逆时针旋转90度的。
                camera.setDisplayOrientation(90);
                camera.unlock();
                recorder.setCamera(camera); //设置摄像头为相机
                recorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);//视频源
                recorder.setAudioSource(MediaRecorder.AudioSource.MIC); // 录音源为麦克风
                //recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4); // 输出格式为mp4
                            /* 引用android.util.DisplayMetrics 获取分辨率 */
                //DisplayMetrics dm = new DisplayMetrics();
                //getWindowManager().getDefaultDisplay().getMetrics(dm);
                //recorder.setVideoSize(480, 480); // 视频尺寸
                recorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH)); //设置视频和声音的编码为系统自带的格式
                recorder.setOutputFile(myRecVideoFile.getAbsolutePath());
                recorder.setPreviewDisplay(mSurfaceHolder.getSurface()); // 预览
                //recorder.setMaxFileSize(10*1024*1024); //设置视频文件的最大值为10M,单位B
                recorder.setMaxDuration(20*1000);//设置视频的最大时长，单位毫秒
                //recorder.setOrientationHint(90);//视频旋转90度，没有用
                recorder.prepare(); // 准备录像
                recorder.start(); // 开始录像
                handler.post(timeRun); // 调用Runable
                recording = true; // 改变录制状态为正在录制
            } catch (IOException e1) {
                releaseMediaRecorder();
                handler.removeCallbacks(timeRun);
                minute = 0;
                second = 0;
                recording = false;
                videoBar.setProgress(0);
                videoBar.setVisibility(View.GONE);
                btnStart.setVisibility(View.VISIBLE);
                btnRotate.setVisibility(View.VISIBLE);
            } catch (IllegalStateException e) {
                releaseMediaRecorder();
                handler.removeCallbacks(timeRun);
                minute = 0;
                second = 0;
                recording = false;
                videoBar.setProgress(0);
                videoBar.setVisibility(View.GONE);
                btnStart.setVisibility(View.VISIBLE);
                btnRotate.setVisibility(View.VISIBLE);
            }
        } else
            ToastUtil.TextToast(getApplicationContext(), "视频录制中...", ToastUtil.LENGTH_SHORT);
    }
}

