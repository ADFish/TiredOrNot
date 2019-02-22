package com.megvii.landmarklib;

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
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import com.megvii.landmark.R;
import com.sina.weibo.sdk.api.TextObject;
import com.sina.weibo.sdk.api.WeiboMultiMessage;
import com.sina.weibo.sdk.api.share.BaseResponse;
import com.sina.weibo.sdk.api.share.IWeiboShareAPI;
import com.sina.weibo.sdk.api.share.SendMultiMessageToWeiboRequest;
import com.sina.weibo.sdk.api.share.WeiboShareSDK;
import com.sina.weibo.sdk.constant.WBConstants;
import java.text.DecimalFormat;
import java.util.ArrayList;

import static com.sina.weibo.sdk.api.share.IWeiboHandler.Response;

public class WatingActivity extends Activity implements Response {
  private String fileName;
  private double fatigueValue;
  private int detectResult;
  private boolean detected = false;
  //控件
  private ImageButton btnBack;
  private ProgressBar barWaiting;
  private ArrayList<String> dataLists;
  private float[] fd;
  private double chance = 0f;

  /**
   * 当前 应用的 APP_KEY
   */
  public static final String APP_KEY = "2789978139";
  private IWeiboShareAPI mWeiboShareAPI = null;
  private String shareText;
  private AlertDialog dialog;
  private Handler handler_tip = new Handler() {
    @Override public void handleMessage(Message msg) {
      super.handleMessage(msg);
      //3s后执行代码
    }
  };
  private Handler handler_check = new Handler() {
    @Override public void handleMessage(Message msg) {
      super.handleMessage(msg);
      //3s后执行代码
    }
  };

  private Runnable timeRun_tip = new Runnable() {
    @Override public void run() {
      if (detected) {
        fd = new float[dataLists.size()];
        for (int i = 0; i < dataLists.size(); i++) {
          fd[i] = Float.valueOf(dataLists.get(i));
        }
        //有小到大排序,fd是有序的
        for (int i = 0; i < fd.length - 1; i++) {//外层循环控制排序趟数
          for (int j = 0; j < fd.length - 1 - i; j++) {//内层循环控制每一趟排序多少次
            if (fd[j] > fd[j + 1]) {
              float temp = fd[j];
              fd[j] = fd[j + 1];
              fd[j + 1] = temp;
            }
          }
        }
        float percentValue = 0.2f * fd[fd.length - 1];
        int n1 = 0;
        for (int i = 0; i < fd.length; i++) {
          //统计<0.2max{n}的个n1
          if (fd[i] < percentValue) {
            n1 = n1 + 1;
          }
        }
        chance = (Double.valueOf(n1) / Double.valueOf(fd.length)) * 100;
        //停止旋转
        barWaiting.setVisibility(View.GONE);
        //显示结果
        showValueDialog();
      }
    }
  };
  private Runnable timeRun_check = new Runnable() {
    @Override public void run() {
      detectResult = GetValue(fileName);
    }
  };

  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
    setContentView(R.layout.activity_waiting_face);
    getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.f1mytitlebar);
    //新页面接收数据
    //    Bundle bundle = this.getIntent().getExtras();
    //接收name值
    //    fileName = bundle.getString("filename");
    //Log.i("获取到的name值为",fileName);

    Intent intent = getIntent();
    //获取n=faces[c].points[3].y-faces[c].points[4].y
    dataLists = intent.getStringArrayListExtra("face_point");
    btnBack = (ImageButton) findViewById(R.id.backimage);
    btnBack.setOnClickListener(new Button.OnClickListener() {
      public void onClick(View v) {
        WatingActivity.this.finish();
      }
    });
    barWaiting = (ProgressBar) findViewById(R.id.waitingFaceProgressBar);
    detectResult = GetValue(fileName);
    //handler_check.postDelayed(timeRun_check, 0);
    handler_tip.postDelayed(timeRun_tip, 4000);
    mWeiboShareAPI = WeiboShareSDK.createWeiboAPI(this, APP_KEY);
    mWeiboShareAPI.registerApp();
  }

  //返回值意味着检测是否成功，0表示不成功，1表示成功
  private int GetValue(String fileName) {
    int detectPeople = 0;
        /*try {
            Thread.currentThread().sleep(5000);//阻断2秒
        } catch (InterruptedException e) {a
            e.printStackTrace();
        }*/
    detected = true;
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

    TextView tv_value = (TextView) window.findViewById(R.id.tv_value);

    DecimalFormat decimalFormat = new DecimalFormat(".00");
    if (chance >= 15) {
      tv_value.setText("疲劳\n" + "疲劳指数（百分制）=" + decimalFormat.format(chance));
    } else {
      tv_value.setText("不疲劳\n" + "疲劳指数（百分制）=" + decimalFormat.format(chance));
    }
    shareText = tv_value.getText().toString();
    home.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View v) {

        Intent intent = new Intent();
        intent.setClass(WatingActivity.this, F1MainAct.class);
        startActivity(intent);
        dialog.cancel();
      }
    });
    share.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View v) {
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
    dialog = new AlertDialog.Builder(WatingActivity.this).create();
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
      @Override public void onClick(View v) {
        // TODO Auto-generated method stub
        dialog.cancel();
      }
    });
    wechat.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View v) {
        // TODO Auto-generated method stub
        dialog.cancel();
      }
    });
    moment.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View v) {
        // TODO Auto-generated method stub
        dialog.cancel();
      }
    });
    qq.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View v) {
        // TODO Auto-generated method stub
        dialog.cancel();
      }
    });
    weibo.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View v) {
        // TODO Auto-generated method stub
        sendMultiMessage();
        //dialog.cancel();
      }
    });
  }

  /**
   * 第三方应用发送请求消息到微博，唤起微博分享界面。
   */
  private void sendMultiMessage() {
    // 1. 初始化微博的分享消息
    WeiboMultiMessage weiboMessage = new WeiboMultiMessage();
    TextObject textObject = new TextObject();
    textObject.text = shareText;
    weiboMessage.textObject = textObject;
    // 2. 初始化从第三方到微博的消息请求
    SendMultiMessageToWeiboRequest request = new SendMultiMessageToWeiboRequest();
    // 用transaction唯一标识一个请求
    request.transaction = String.valueOf(System.currentTimeMillis());
    request.multiMessage = weiboMessage;
    // 3. 发送请求消息到微博，唤起微博分享界面
    mWeiboShareAPI.sendRequest(WatingActivity.this, request);
  }

  @Override public void onResponse(BaseResponse baseResp) {
    if (baseResp != null) {
      switch (baseResp.errCode) {
        case WBConstants.ErrorCode.ERR_OK:
          Toast.makeText(this, "分享成功", Toast.LENGTH_LONG).show();
          break;
        case WBConstants.ErrorCode.ERR_CANCEL:
          Toast.makeText(this, "取消分享", Toast.LENGTH_LONG).show();
          break;
        case WBConstants.ErrorCode.ERR_FAIL:
          Toast.makeText(this, "分享失败" + "Error Message: " + baseResp.errMsg, Toast.LENGTH_LONG)
              .show();
          break;
      }
    }
  }
}

