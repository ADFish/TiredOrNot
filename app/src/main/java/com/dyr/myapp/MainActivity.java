package com.dyr.myapp;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.megvii.facepp.sdk.Facepp;
import com.megvii.landmarklib.F2MainAct;
import com.megvii.landmarklib.F1MainAct;
import com.megvii.landmarklib.util.ConUtil;
import com.megvii.landmarklib.util.Util;
import com.megvii.licencemanage.sdk.LicenseManager;

import org.apache.http.Header;


/**
 * Created by sony on 2016/7/6.
 */
public class MainActivity extends Activity {
    private LicenseManager licenseManager;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        licenseManager = new LicenseManager(this);
        network();
        ImageButton imagebutton = (ImageButton) findViewById(R.id.imagebutton);
        /**myImageButton.setImageResource(R.drawable.help);*/
        imagebutton.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setClass(MainActivity.this, HelpActivity.class);
                startActivity(intent);
            }
        });

        Button button1 = (Button) findViewById(R.id.button1);
        button1.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                Intent intent = new Intent();
                //  intent.setClass(MainActivity.this, F1MainActivity.class);
                intent.setClass(MainActivity.this, F1MainAct.class);
                startActivity(intent);
            }
        });

        Button button2 = (Button) findViewById(R.id.button2);
        button2.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setClass(MainActivity.this, F2MainAct.class);
                //   intent.setClass(MainActivity.this, F2MainActivity.class);
                startActivity(intent);
            }
        });
    }

    private void network()  {
//        contentRel.setVisibility(View.GONE);
//        barLinear.setVisibility(View.VISIBLE);
//        againWarrantyBtn.setVisibility(View.GONE);
//        WarrantyText.setText("正在联网授权中...");
//        WarrantyBar.setVisibility(View.VISIBLE);
        licenseManager.setAuthTime(Facepp.getApiExpication(this) * 1000);
        // licenseManager.setAgainRequestTime(againRequestTime);

        String uuid = ConUtil.getUUIDString(MainActivity.this);
        long[] apiName = { Facepp.getApiName() };
        String content = licenseManager.getContent(uuid, LicenseManager.DURATION_30DAYS, apiName);

        String errorStr = licenseManager.getLastError();
        Log.w("ceshi", "getContent++++errorStr===" + errorStr);

        boolean isAuthSuccess = licenseManager.authTime();
        Log.w("ceshi", "isAuthSuccess===" + isAuthSuccess);
        if (isAuthSuccess) {
            authState(true);
        } else {
            AsyncHttpClient mAsyncHttpclient = new AsyncHttpClient();
            String url = "https://api.megvii.com/megviicloud/v1/sdk/auth";
            RequestParams params = new RequestParams();
            params.put("api_key", Util.API_KEY);
            params.put("api_secret", Util.API_SECRET);
            params.put("auth_msg", content);
            Log.w("ceshi", "content:" + content);
            mAsyncHttpclient.post(url, params, new AsyncHttpResponseHandler() {
                @Override
                public void onSuccess(int statusCode, Header[] headers, byte[] responseByte) {
                    String successStr = new String(responseByte);
                    boolean isSuccess = licenseManager.setLicense(successStr);
                    if (isSuccess)
                        authState(true);
                    else
                        authState(false);

                    String errorStr = licenseManager.getLastError();
                    Log.w("ceshi", "setLicense++++errorStr===" + errorStr);
                }

                @Override
                public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                    error.printStackTrace();
                    authState(false);
                }
            });
        }

    }

    private void authState(boolean isSuccess) {
        //       versionText.setText(Facepp.getVersion() + " ; " + ConUtil.getFormatterDate(Facepp.getApiExpication(this) * 1000));

        if (isSuccess) {
//            barLinear.setVisibility(View.GONE);
//            WarrantyBar.setVisibility(View.GONE);
//            againWarrantyBtn.setVisibility(View.GONE);
//            contentRel.setVisibility(View.VISIBLE);
        } else {
//            barLinear.setVisibility(View.VISIBLE);
//            WarrantyBar.setVisibility(View.GONE);
//            againWarrantyBtn.setVisibility(View.VISIBLE);
//            contentRel.setVisibility(View.GONE);
//            WarrantyText.setText("联网授权失败！请检查网络或找服务商");
        }

    }

}
