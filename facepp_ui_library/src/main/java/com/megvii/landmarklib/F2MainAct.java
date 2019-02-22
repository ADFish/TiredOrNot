package com.megvii.landmarklib;

import android.app.Activity;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.Size;
import android.media.MediaPlayer;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLSurfaceView.Renderer;
import android.opengl.Matrix;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.megvii.facepp.sdk.Facepp;
import com.megvii.facepp.sdk.Facepp.Face;
import com.megvii.facepp.sdk.Facepp.FaceppConfig;
import com.megvii.landmark.R;
import com.megvii.landmarklib.util.CameraMatrix;
import com.megvii.landmarklib.util.ConUtil;
import com.megvii.landmarklib.util.DialogUtil;
import com.megvii.landmarklib.util.ICamera;
import com.megvii.landmarklib.util.MediaRecorderUtil;
import com.megvii.landmarklib.util.OpenGLDrawRect;
import com.megvii.landmarklib.util.OpenGLUtil;
import com.megvii.landmarklib.util.PointsMatrix;
import com.megvii.landmarklib.util.Screen;
import com.megvii.landmarklib.util.SensorEventUtil;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import static android.content.ContentValues.TAG;

public class F2MainAct extends Activity
    implements PreviewCallback, Renderer, SurfaceTexture.OnFrameAvailableListener {

  private boolean isDebug, isBackCamera, isTiming, isROIDetect, isFaceProperty;
  private int printTime = 33;
  private GLSurfaceView mGlSurfaceView;
  private ICamera mICamera;
  private Camera mCamera;
  private DialogUtil mDialogUtil;
  private TextView debugInfoText, debugPrinttext;
  HandlerThread mHandlerThread = new HandlerThread("hhh");
  Handler mHandler;
  private Facepp facepp;
  private MediaRecorderUtil mediaRecorderUtil;
  private boolean isStartRecorder = false;
  private int min_face_size = 200;
  private int detection_interval = 25;
  private HashMap<String, Integer> resolutionMap;
  private SensorEventUtil sensorUtil;
  private float roi_ratio = 0.8f;

  private Handler handler;
  private int second = 0, minute = 30;
  private boolean isdetect = false;

  private String timespawn = "";
  private String message = "";
  private double chance = 0f;
  private ArrayList<Float> dataLists = new ArrayList<Float>();
  //报警播放器
  public MediaPlayer mMediaPlayer = new MediaPlayer();
  private Runnable timeRun = new Runnable() {

    @Override public void run() {

      if (second == 60) {
        minute++;
        second = 0;
      }

      if (minute == 30 && isdetect == false) {

        minute = 0;
        second = 0;
        isdetect = true;
        startdetct();
      }
      if (isdetect && second == 20) {
        isdetect = false;
        stopdetct();
      }
      second++;
      handler.postDelayed(timeRun, 1000);
    }
  };

  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    Screen.initialize(this);
    requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
    setContentView(R.layout.f2main);
    getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.titlebar2);

    init();

    handler = new Handler();
    handler.post(timeRun);

    final ImageButton backbutton = (ImageButton) findViewById(R.id.backimage2);
    backbutton.setOnClickListener(new Button.OnClickListener() {
      public void onClick(View v) {
        onPause();
      }
    });

    final ImageButton rotatebutton = (ImageButton) findViewById(R.id.RotateButton2);
    rotatebutton.setOnClickListener(new Button.OnClickListener() {
      public void onClick(View v) {
        rotateCamera();
      }
    });
  }

  private void init() {
    if (android.os.Build.MODEL.equals("PLK-AL10")) printTime = 50;

    isDebug = getIntent().getBooleanExtra("isdebug", false);
    isFaceProperty = getIntent().getBooleanExtra("isFaceProperty", false);
    isBackCamera = getIntent().getBooleanExtra("isBackCamera", false);
    isStartRecorder = getIntent().getBooleanExtra("isStartRecorder", false);
    isROIDetect = getIntent().getBooleanExtra("ROIDetect", false);
    isTiming = getIntent().getBooleanExtra("isTiming", false);
    isTiming = true;
    min_face_size = getIntent().getIntExtra("faceSize", min_face_size);
    detection_interval = getIntent().getIntExtra("interval", detection_interval);
    resolutionMap = (HashMap<String, Integer>) getIntent().getSerializableExtra("resolution");

    facepp = new Facepp();
    sensorUtil = new SensorEventUtil(this);

    mHandlerThread.start();
    mHandler = new Handler(mHandlerThread.getLooper());

    mGlSurfaceView = (GLSurfaceView) findViewById(R.id.opengl_layout_surfaceview);
    mGlSurfaceView.setEGLContextClientVersion(2);// 创建一个OpenGL ES 2.0
    // context
    mGlSurfaceView.setRenderer(this);// 设置渲染器进入gl
    // RENDERMODE_CONTINUOUSLY不停渲染
    // RENDERMODE_WHEN_DIRTY懒惰渲染，需要手动调用 glSurfaceView.requestRender() 才会进行更新
    mGlSurfaceView.setRenderMode(mGlSurfaceView.RENDERMODE_WHEN_DIRTY);// 设置渲染器模式
    mGlSurfaceView.setOnClickListener(new OnClickListener() {
      @Override public void onClick(View v) {
        autoFocus();
      }
    });

    mICamera = new ICamera();
    mDialogUtil = new DialogUtil(this);
    debugInfoText = (TextView) findViewById(R.id.opengl_layout_debugInfotext);
    debugPrinttext = (TextView) findViewById(R.id.opengl_layout_debugPrinttext);
    if (isDebug) {
      debugInfoText.setVisibility(View.VISIBLE);
    } else {
      debugInfoText.setVisibility(View.INVISIBLE);
    }
    AssetManager assetManager = getAssets();
    try {
      AssetFileDescriptor fileDescriptor = assetManager.openFd("7872.mp3");
      try {
        mMediaPlayer.reset();
        mMediaPlayer.setDataSource(fileDescriptor.getFileDescriptor(),
            fileDescriptor.getStartOffset(), fileDescriptor.getLength());
        mMediaPlayer.prepare();
      } catch (IOException e) {
        e.printStackTrace();
        Log.e("MP3playService", "播放音频异常");
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private void autoFocus() {
    if (mCamera != null && isBackCamera) {
      mCamera.cancelAutoFocus();
      Parameters parameters = mCamera.getParameters();
      parameters.setFocusMode(Parameters.FOCUS_MODE_AUTO);
      mCamera.setParameters(parameters);
      mCamera.autoFocus(null);
    }
  }

  private void startdetct() {
    isBackCamera = !isBackCamera;
    if (dataLists != null) {
      dataLists.clear();
    }
    rotateCamera();
  }

  /**
   * 停止录制的操作
   */
  private void stopdetct() {
    //  mICamera.startPreview(mSurface);
    mICamera.closeCamera();
    //  mSurface.release();
    //有小到大排序,fd是有序的
    for (int i = 0; i < dataLists.size() - 1; i++) {//外层循环控制排序趟数
      for (int j = 0; j < dataLists.size() - 1 - i; j++) {//内层循环控制每一趟排序多少次
        if (dataLists.get(j) > dataLists.get(j + 1)) {
          float temp = dataLists.get(j);
          dataLists.set(j, dataLists.get(j + 1));
          dataLists.set(j + 1, temp);
        }
      }
    }
    float percentValue = 0.2f * dataLists.get(dataLists.size() - 1);
    int n1 = 0;
    for (int i = 0; i < dataLists.size(); i++) {
      //统计<0.2max{n}的个n1
      if (dataLists.get(i) < percentValue) {
        n1 = n1 + 1;
      }
    }
    chance = (Double.valueOf(n1) / Double.valueOf(dataLists.size())) * 100;
    if (chance >= 15) {
      playOrPause();
    }
  }

  private void sendinterface() {
  }

  private int Angle;

  private void rotateCamera() {

    mICamera.closeCamera();
    isBackCamera = !isBackCamera;
    mCamera = mICamera.openCamera(isBackCamera, this, resolutionMap);

    if (mCamera != null) {
      Angle = 360 - mICamera.Angle;
      if (isBackCamera) Angle = 360 - mICamera.Angle - 180;

      CameraInfo cameraInfo = new CameraInfo();
      Camera.getCameraInfo(mICamera.cameraId, cameraInfo);
      RelativeLayout.LayoutParams layout_params = mICamera.getLayoutParam();
      mGlSurfaceView.setLayoutParams(layout_params);

      int width = mICamera.cameraWidth;
      int height = mICamera.cameraHeight;

      int left = 0;
      int top = 0;
      int right = width;
      int bottom = height;

      String errorCode =
          facepp.init(this, ConUtil.getFileContent(this, R.raw.megviifacepp_0_2_0_model));
      Log.w("ceshi", "errorCode====" + errorCode);
      FaceppConfig faceppConfig = facepp.getFaceppConfig();
      faceppConfig.rotation = Angle;
      faceppConfig.interval = detection_interval;
      faceppConfig.minFaceSize = min_face_size;
      faceppConfig.roi_left = left;
      faceppConfig.roi_top = top;
      faceppConfig.roi_right = right;
      faceppConfig.roi_bottom = bottom;
      faceppConfig.detectionMode = FaceppConfig.DETECTION_MODE_TRACKING;
      facepp.setFaceppConfig(faceppConfig);
    } else {
      mDialogUtil.showDialog("打开相机失败");
    }
    mICamera.startPreview(mSurface);// 设置预览容器
    mICamera.actionDetect(this);
  }

  @Override protected void onResume() {
    super.onResume();
    ConUtil.acquireWakeLock(this);
    startTime = System.currentTimeMillis();
    mCamera = mICamera.openCamera(isBackCamera, this, resolutionMap);
    if (mCamera != null) {
      Angle = 360 - mICamera.Angle;
      if (isBackCamera) Angle = 360 - mICamera.Angle - 180;
      CameraInfo cameraInfo = new CameraInfo();
      Camera.getCameraInfo(mICamera.cameraId, cameraInfo);
      RelativeLayout.LayoutParams layout_params = mICamera.getLayoutParam();
      mGlSurfaceView.setLayoutParams(layout_params);

      int width = mICamera.cameraWidth;
      int height = mICamera.cameraHeight;

      int left = 0;
      int top = 0;
      int right = width;
      int bottom = height;
      if (isROIDetect) {
        float line = height * roi_ratio;
        left = (int) ((width - line) / 2.0f);
        top = (int) ((height - line) / 2.0f);
        right = width - left;
        bottom = height - top;
      }

      String errorCode =
          facepp.init(this, ConUtil.getFileContent(this, R.raw.megviifacepp_0_2_0_model));
      Log.w("ceshi", "errorCode====" + errorCode);
      FaceppConfig faceppConfig = facepp.getFaceppConfig();
      faceppConfig.rotation = Angle;
      faceppConfig.interval = detection_interval;
      faceppConfig.minFaceSize = min_face_size;
      faceppConfig.roi_left = left;
      faceppConfig.roi_top = top;
      faceppConfig.roi_right = right;
      faceppConfig.roi_bottom = bottom;
      faceppConfig.detectionMode = FaceppConfig.DETECTION_MODE_TRACKING;
      facepp.setFaceppConfig(faceppConfig);
    } else {
      mDialogUtil.showDialog("打开相机失败");
    }
  }

  public Bitmap decodeToBitMap(byte[] data, Camera _camera) {
    Size size = _camera.getParameters().getPreviewSize();
    try {
      YuvImage image = new YuvImage(data, ImageFormat.NV21, size.width, size.height, null);
      if (image != null) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        image.compressToJpeg(new Rect(0, 0, size.width, size.height), 80, stream);
        Bitmap bmp = BitmapFactory.decodeByteArray(stream.toByteArray(), 0, stream.size());
        stream.close();
        return bmp;
      }
    } catch (Exception ex) {
    }
    return null;
  }

  public void saveImage(Bitmap bmp) {
    File appDir = new File(Environment.getExternalStorageDirectory(), "Myapp-dataF2");
    // File appDir = new File("Myapp-data2");
    if (!appDir.exists()) {
      appDir.mkdir();
    }
    //  String fileName = System.currentTimeMillis() + ".jpg";
    String fileName = timespawn + ".jpg";
    File file = new File(appDir, fileName);
    try {
      FileOutputStream fos = new FileOutputStream(file);
      bmp.compress(Bitmap.CompressFormat.JPEG, 100, fos);
      fos.flush();
      fos.close();
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public void writeFileSdcard(String message) {
    File appDir = new File(Environment.getExternalStorageDirectory(), "Myapp-dataF2");
    // File appDir = new File("Myapp-data");
    if (!appDir.exists()) {
      appDir.mkdir();
    }
    try {

      //FileOutputStream fout = openFileOutput(fileName, MODE_PRIVATE);

      //   FileOutputStream fout = new FileOutputStream("Myapp-dataF1/"+fileName+".txt");
      String fileName = timespawn + ".txt";
      File file = new File(appDir, fileName);
      FileOutputStream fout = new FileOutputStream(file);
      //  if (fout)

      byte[] bytes = message.getBytes();

      fout.write(bytes);

      fout.close();
    } catch (Exception e) {

      e.printStackTrace();
    }
  }

  /**
   * 画绿色框
   */
  private void drawShowRect() {
    mPointsMatrix.vertexBuffers =
        OpenGLDrawRect.drawCenterShowRect(isBackCamera, mICamera.cameraWidth, mICamera.cameraHeight,
            roi_ratio);
  }

  public void getSensor(float x, float y, float z) {
    // if (mPointsMatrix != null) {
    // mPointsMatrix.bottomVertexBuffer =
    // OpenGLDrawRect.drawBottomShowRect(0.15f, 0, 0f, x, y, z);
    // }
  }

  boolean isSuccess = false;
  float confidence;
  float pitch, yaw, roll;
  long startTime;
  long get3DPosefaceTime_end = 0;

  @Override public void onPreviewFrame(final byte[] imgData, final Camera camera) {
    if (isSuccess) return;
    isSuccess = true;
    message = "";
    mHandler.post(new Runnable() {
      @Override public void run() {
        int width = mICamera.cameraWidth;
        int height = mICamera.cameraHeight;

        long faceDetectTime_action = System.currentTimeMillis();
        Face[] faces = facepp.detect(imgData, width, height, Facepp.IMAGEMODE_NV21);

        if (faces != null) {
          final long algorithmTime = System.currentTimeMillis() - faceDetectTime_action;

          long actionMaticsTime = System.currentTimeMillis();

          float ling = width > height ? width : height;
          float widthValue_point = 0.3755f;
          float hightValue = 0.5f;

          ArrayList<ArrayList> pointsOpengl = new ArrayList<ArrayList>();
          ArrayList<FloatBuffer> vertexBuffersOpengl = new ArrayList<FloatBuffer>();
          confidence = 0.0f;

          if (faces.length >= 0) {
            for (int c = 0; c < faces.length; c++) {
              facepp.getLandMark(faces, c, Facepp.facePoints);
              long get3DPosefaceTime_action = System.currentTimeMillis();
              facepp.get3DPose(faces, c);
              get3DPosefaceTime_end = System.currentTimeMillis() - get3DPosefaceTime_action;
              RectF rectF = new RectF();

              float _x_offset = 0, _y_offset = 0;
              float max_len = height;
              if (width > height) {
                max_len = width;
                _x_offset = (width - height) / 2;
              } else {
                _y_offset = (height - width) / 2;
              }

              pitch = faces[c].pitch;
              yaw = faces[c].yaw;
              roll = faces[c].roll;
              confidence = faces[c].confidence;
              float point3 = 0;
              float point4 = 0;
              ArrayList<FloatBuffer> triangleVBList = new ArrayList<FloatBuffer>();
              for (int i = 0; i < 18; i++) {
                if (i == 2) {
                  point3 = (faces[c].points[i].y);
                }
                if (i == 3) {
                  point4 = (faces[c].points[i].y);
                }
                message += Float.toString(faces[c].points[i].x) + " " + Float.toString(
                    faces[c].points[i].y) + " ";

                float x = ((faces[c].points[i].x + _x_offset) / max_len) * 2 - 1;
                if (isBackCamera) x = -x;
                float y = 1 - (((faces[c].points[i].y + _y_offset) + _y_offset) / max_len) * 2;
                float[] pointf = { x, y, 0.0f };
                FloatBuffer fb = mCameraMatrix.floatBufferUtil(pointf);
                triangleVBList.add(fb);
              }
              dataLists.add(point3 - point4);
              pointsOpengl.add(triangleVBList); //draw landmarks

              //check for https://console.faceplusplus.com.cn/documents/5671270
              //faces[c].points[i] i 0-8 for right eyes,9-17 for left eyes.

              timespawn = System.currentTimeMillis() + "";
              writeFileSdcard(message);//save points data
              saveImage(decodeToBitMap(imgData, camera));

              sendinterface();//make an interface here to send landmarks and imgData
            }
          } else {
            pitch = 0.0f;
            yaw = 0.0f;
            roll = 0.0f;
          }
          if (isFaceProperty) {
            mPointsMatrix.bottomVertexBuffer =
                OpenGLDrawRect.drawBottomShowRect(0.15f, 0, -0.7f, pitch, -yaw, roll, Angle);
          }
          synchronized (mPointsMatrix) {
            mPointsMatrix.points = pointsOpengl;
            // mPointsMatrix.vertexBuffers = vertexBuffersOpengl;
          }

          final long matrixTime = System.currentTimeMillis() - actionMaticsTime;
          runOnUiThread(new Runnable() {
            @Override public void run() {
              DecimalFormat decimalFormat = new DecimalFormat(".00");// 构造方法的字符格式这里如果小数不足2位,会以0补足.
              String pitch_str = decimalFormat.format(pitch);// format
              // 返回的是字符串
              String yaw_str = decimalFormat.format(yaw);// format
              // 返回的是字符串
              String roll_str = decimalFormat.format(roll);// format
              // 返回的是字符串
              // image.setImageBitmap(decodeToBitMap(imgData,
              // camera));
              debugInfoText.setText("cameraWidth: " + mICamera.cameraWidth + "\ncameraHeight: "
                  + mICamera.cameraHeight + "\nalgorithmTime: " + algorithmTime + "ms"
                  // + "\nlandmarkTime: " + landmarkTime +
                  // "ms"
                  + "\nmatrixTime: " + matrixTime + ", \nconfidence:" + confidence + "\n3Dpose: "
                  + pitch_str + ", " + yaw_str + ", " + roll_str + "\n3DPoseTime:"
                  + get3DPosefaceTime_end);
            }
          });
        }
        isSuccess = false;
        if (!isTiming) {
          timeHandle.sendEmptyMessage(1);
        }
      }
    });
  }

  @Override protected void onPause() {
    super.onPause();
    ConUtil.releaseWakeLock();
    if (mediaRecorderUtil != null) {
      mediaRecorderUtil.releaseMediaRecorder();
    }
    mICamera.closeCamera();
    mCamera = null;

    timeHandle.removeMessages(0);

    finish();
  }

  @Override protected void onDestroy() {
    super.onDestroy();
    facepp.release();
    if (mMediaPlayer != null) {
      stop();
      mMediaPlayer.release();
    }
  }

  private int mTextureID = -1;
  private SurfaceTexture mSurface;
  private CameraMatrix mCameraMatrix;
  private PointsMatrix mPointsMatrix;

  @Override public void onFrameAvailable(SurfaceTexture surfaceTexture) {
    // TODO Auto-generated method stub

  }

  @Override public void onSurfaceCreated(GL10 gl, EGLConfig config) {
    // 黑色背景
    GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);

    mTextureID = OpenGLUtil.createTextureID();
    mSurface = new SurfaceTexture(mTextureID);
    // 这个接口就干了这么一件事，当有数据上来后会进到onFrameAvailable方法
    mSurface.setOnFrameAvailableListener(this);// 设置照相机有数据时进入
    mCameraMatrix = new CameraMatrix(mTextureID);
    mPointsMatrix = new PointsMatrix();
    mICamera.startPreview(mSurface);// 设置预览容器
    // mICamera.actionDetect(this);
    if (isTiming) {
      timeHandle.sendEmptyMessageDelayed(0, printTime);
    }
    if (isROIDetect) drawShowRect();
  }

  @Override public void onSurfaceChanged(GL10 gl, int width, int height) {
    // 设置画面的大小
    GLES20.glViewport(0, 0, width, height);

    float ratio = (float) width / height;

    // this projection matrix is applied to object coordinates
    // in the onDrawFrame() method
    Matrix.frustumM(mProjMatrix, 0, -ratio, ratio, -1, 1, 3, 7);
    // Matrix.perspectiveM(mProjMatrix, 0, 0.382f, ratio, 3, 700);
  }

  private final float[] mMVPMatrix = new float[16];
  private final float[] mProjMatrix = new float[16];
  private final float[] mVMatrix = new float[16];
  private final float[] mRotationMatrix = new float[16];

  @Override public void onDrawFrame(GL10 gl) {
    final long actionTime = System.currentTimeMillis();
    // Log.w("ceshi", "onDrawFrame===");
    GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);// 清除屏幕和深度缓存
    float[] mtx = new float[16];
    mSurface.getTransformMatrix(mtx);
    mCameraMatrix.draw(mtx);
    // Set the camera position (View matrix)
    Matrix.setLookAtM(mVMatrix, 0, 0, 0, -3, 0f, 0f, 0f, 0f, 1f, 0f);

    // Calculate the projection and view transformation
    Matrix.multiplyMM(mMVPMatrix, 0, mProjMatrix, 0, mVMatrix, 0);

    mPointsMatrix.draw(mMVPMatrix);

    if (isDebug) {
      runOnUiThread(new Runnable() {
        @Override public void run() {
          final long endTime = System.currentTimeMillis() - actionTime;
          debugPrinttext.setText("printTime: " + endTime);
        }
      });
    }
    Log.d(TAG, "onDrawFrame");
    if (isdetect == false) {
      GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
      GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
      Log.d(TAG, "onDrawFrame2");
    } else {
      mSurface.updateTexImage();// 更新image，会调用onFrameAvailable方法
    }
  }

  Handler timeHandle = new Handler() {
    @Override public void handleMessage(Message msg) {
      switch (msg.what) {
        case 0:
          mGlSurfaceView.requestRender();// 发送去绘制照相机不断去回调
          timeHandle.sendEmptyMessageDelayed(0, printTime);
          break;
        case 1:
          mGlSurfaceView.requestRender();// 发送去绘制照相机不断去回调
          break;
      }
    }
  };

  /**
   * 播放或暂停
   */
  public void playOrPause() {
    if (mMediaPlayer.isPlaying()) {
      mMediaPlayer.pause();
    } else {
      mMediaPlayer.start();
    }
  }

  /**
   * 停止播放
   */
  public void stop() {
    if (mMediaPlayer != null) {
      mMediaPlayer.stop();
      //停止播放后从新初始化mediaplay的位置
      try {
        mMediaPlayer.prepare();
      } catch (IOException e) {
        e.printStackTrace();
      }
      mMediaPlayer.seekTo(0);
    }
  }
}