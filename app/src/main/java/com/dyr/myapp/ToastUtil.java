package com.dyr.myapp;

/**
 * Created by sony on 2016/7/13.
 */
import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

public class ToastUtil {
    private static Toast toast = null;
    public static int LENGTH_LONG = Toast.LENGTH_LONG;
    public static int LENGTH_SHORT = Toast.LENGTH_SHORT;

    public static void OKTextToast(Context context,/*CharSequence text,*/int duration){
        toast = new Toast(context);
        LayoutInflater inflate = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View v = inflate.inflate(R.layout.myoktoast, null);
        // 在这里初始化一下里面的文字啊什么的
        toast.setView(v);
        //创建一个Toast提示消息
        //toast = Toast.makeText(context, text, duration);
        //设置Toast提示消息在屏幕上的位置
        toast.setGravity(Gravity.CENTER, 0, 0);
        toast.setDuration(duration);
        //显示消息
        toast.show();
    }

    public static void TextToast(Context context,CharSequence text,int duration){
        //创建一个Toast提示消息
        toast = Toast.makeText(context, text, duration);
        //设置Toast提示消息在屏幕上的位置
        toast.setGravity(Gravity.CENTER, 0, 0);
        toast.setDuration(duration);
        //显示消息
        toast.show();
    }

    public static void ImageToast(Context context,int ImageResourceId,CharSequence text,int duration){
        //创建一个Toast提示消息
        toast = Toast.makeText(context, text, Toast.LENGTH_LONG);
        //设置Toast提示消息在屏幕上的位置
        toast.setGravity(Gravity.CENTER, 0, 0);
        //获取Toast提示消息里原有的View.
        View toastView = toast.getView();
        //创建一个ImageView
        ImageView img = new ImageView(context);
        img.setImageResource(ImageResourceId);
        //创建一个LineLayout容器
        LinearLayout ll = new LinearLayout(context);
        //向LinearLayout中添加ImageView和Toast原有的View
        ll.addView(img);
        ll.addView(toastView);
        toast.show();
    }

}
