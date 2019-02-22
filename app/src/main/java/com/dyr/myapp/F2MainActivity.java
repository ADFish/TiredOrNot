package com.dyr.myapp;

/**
 * Created by sony on 2016/7/13.
 */

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.hardware.Camera;
import android.media.AudioManager;
import android.media.CamcorderProfile;
import android.media.Image;
import android.media.MediaRecorder;
import android.media.SoundPool;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.VideoView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

public class F2MainActivity extends Activity
{
    private Camera mCamera;
    private CameraPreview mPreview;
    private MediaRecorder mMediaRecorder;
    private boolean isRecording = false;
    public static final int MEDIA_TYPE_IMAGE = 1;
    public static final int MEDIA_TYPE_VIDEO = 2;
    public Timer timer = new Timer();
    public int plus = 5;
    //  public SoundPool soundPool =new SoundPool(1, AudioManager.STREAM_ALARM,0);
    public static Camera getCameraInstance()
    {
        Camera c = null;
        try
        {
            c = Camera.open(0); // 试图获取Camera实例
        } catch (Exception e){
            Log.d("MyCamera", "摄像头不可用");
        }
        return c; // 不可用则返回null
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
        setContentView(R.layout.activity_main2);
        getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE,R.layout.titlebar2);
        // 创建Camera实例
        mCamera = getCameraInstance();
        // 创建Preview view并将其设为activity中的内容
        mPreview = new CameraPreview(this, mCamera);
        FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
        preview.addView(mPreview);
        final ImageButton imagebutton = (ImageButton) findViewById(R.id.imageButton3);
        final ImageButton backbutton=(ImageButton)findViewById(R.id.backimage2);
        backbutton.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setClass(F2MainActivity.this, MainActivity.class);
                startActivity(intent);
            }
        });
        final ImageButton rotate=(ImageButton) findViewById(R.id.RotateButton2);
        final ImageView imageView =(ImageView) findViewById(R.id.imageView2);
        rotate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Log.d("MyCamerarotate", "onClick: rotate");
            }
        });
        imagebutton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                imagebutton.setVisibility(View.INVISIBLE);
                imageView.setVisibility(1);
                //   soundPool.play(0,1,1,0,3,1);
                timer.schedule(new TimerTask()
                {
                    @Override
                    public void run()
                    {
                        Log.d("MyCamera", "录制中 ");
                        if (isRecording) {
                            // 停止录像并释放camera
                            mMediaRecorder.stop(); // 停止录像
                            mMediaRecorder.reset();
                            mMediaRecorder.release();// 释放MediaRecorder对象
                            mMediaRecorder = null;
                            mCamera.lock();         // 将控制权从MediaRecorder 交回camera
                            Log.d("MyCamera", "录制结束 ");
                            isRecording = false;
                        }
                        else
                        {
                            if (plus>=5)
                            {
                                // 初始化视频camera
                                if (prepareVideoRecorder()) {
                                    mMediaRecorder.start();
                                    isRecording = true;
                                    plus=0;
                                    Log.d("MyCamera", "onClick:begin ");
                                } else {
                                    Log.d("MyCamera", "mediarecorder准备未完成 ");
                                    releaseMediaRecorder();
                                }
                            }
                            else plus++;
                        }
                    };
                },0,5000);
            }
        });
    }

    private boolean prepareVideoRecorder()
    {
        mMediaRecorder = new MediaRecorder();
        // 第1步：解锁并将摄像头指向MediaRecorder
        mCamera.unlock();
        mMediaRecorder.setCamera(mCamera);
        // 第2步：指定源
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
        // 第3步：指定CamcorderProfile（需要API Level 8以上版本）
        mMediaRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH));
        // 第4步：指定输出文件
        // mMediaRecorder.setOutputFile("/sdcard/mvideo.3gp");
        //   mMediaRecorder.setOutputFile("content://media/external/video/media/video.3gp");
        mMediaRecorder.setOutputFile(getOutputMediaFile(MEDIA_TYPE_VIDEO).toString());
        //  Log.d("MyCamera",getOutputMediaFile(MEDIA_TYPE_VIDEO).toString());
        // mMediaRecorder.setOutputFile(getOutputMediaFileUri(MEDIA_TYPE_VIDEO).toString());
        //  mMediaRecorder.setOutputFile(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString());
        // 第5步：指定预览输出
        mMediaRecorder.setPreviewDisplay(mPreview.getHolder().getSurface());
        // 第6步：根据以上配置准备MediaRecorder
        try
        {
            mMediaRecorder.prepare();
        } catch (IllegalStateException e) {
            Log.d("MyCamera", "IllegalStateException preparing MediaRecorder: " + e.getMessage());
            releaseMediaRecorder();
            return false;
        } catch (IOException e) {
            Log.d("MyCamera", "IOException preparing MediaRecorder: " + e.getMessage());
            releaseMediaRecorder();
            return false;
        }
        return true;
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        timer.cancel();
        releaseMediaRecorder(); // 如果正在使用MediaRecorder，首先需要释放它。
        releaseCamera();         // 在暂停事件中立即释放摄像头
    }
    private void releaseMediaRecorder()
    {
        if (mMediaRecorder != null) {
            mMediaRecorder.stop();
            mMediaRecorder.reset(); // 清除recorder配置
            mMediaRecorder.release(); // 释放recorder对象
            mMediaRecorder = null;
            mCamera.lock();           // 为后续使用锁定摄像头
        }
    }
    private void releaseCamera()
    {
        if (mCamera != null) {
            mCamera.release();        // 为其它应用释放摄像头
            mCamera = null;
        }
    }

    /** 为保存图片或视频创建文件Uri */
  /*  private static Uri getOutputMediaFileUri(int type)
    {
        return Uri.fromFile(getOutputMediaFile(type));
    }*/
    /** 为保存图片或视频创建File */
    private static File getOutputMediaFile(int type)
    {
        // 安全起见，在使用前应该
        // 用Environment.getExternalStorageState()检查SD卡是否已装入
        if (Environment.getExternalStorageState()==null) {
            Log.d("MyCamera","noSDcard ");
            return null;
        }
        else {
            Log.d("MyCamera","gotSDcard ");
        }
        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "MyApp2016");
// 如果期望图片在应用程序卸载后还存在、且能被其它应用程序共享，
// 则此保存位置最合适
        // 如果不存在的话，则创建存储目录
        if (! mediaStorageDir.exists()) {
            if (! mediaStorageDir.mkdirs()) {
                Log.d("MyCamera","failed to create directory");
                return null;
            }
        }
        // 创建媒体文件名
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile;
        if (type == MEDIA_TYPE_IMAGE) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "mIMG_"+ timeStamp + ".jpg");
        }
        else if(type == MEDIA_TYPE_VIDEO) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "mVID_"+ timeStamp + ".mp4");
        }
        else {
            return null;
        }
        return mediaFile;
    }

}
