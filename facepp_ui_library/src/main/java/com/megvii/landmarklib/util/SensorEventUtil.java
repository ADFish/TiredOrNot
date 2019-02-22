package com.megvii.landmarklib.util;
//package com.dyr.myapp;

import com.megvii.landmarklib.F2MainAct;
import com.megvii.landmarklib.OpenglActivity;
import com.megvii.landmarklib.F1MainAct;
import android.app.Activity;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

public class SensorEventUtil implements SensorEventListener {
	private SensorManager mSensorManager;
	private Sensor mSensor;
	private OpenglActivity opActivity;
    private F1MainAct f1MainAct;
    private F2MainAct f2MainAct;

    public SensorEventUtil(F1MainAct opActivity) {
        this.f1MainAct = opActivity;
        mSensorManager = (SensorManager) opActivity.getSystemService(opActivity.SENSOR_SERVICE);
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);// TYPE_GRAVITY
        // 参数三，检测的精准度
        mSensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_NORMAL);// SENSOR_DELAY_GAME
    }

    public SensorEventUtil(F2MainAct opActivity) {
        this.f2MainAct = opActivity;
        mSensorManager = (SensorManager) opActivity.getSystemService(opActivity.SENSOR_SERVICE);
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);// TYPE_GRAVITY
        // 参数三，检测的精准度
        mSensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_NORMAL);// SENSOR_DELAY_GAME
    }

    public SensorEventUtil(OpenglActivity opActivity) {
        this.opActivity = opActivity;
        mSensorManager = (SensorManager) opActivity.getSystemService(opActivity.SENSOR_SERVICE);
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);// TYPE_GRAVITY
        // 参数三，检测的精准度
        mSensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_NORMAL);// SENSOR_DELAY_GAME
    }

    @Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {

	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		if (event.sensor == null) {
			return;
		}

		if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
			float x = event.values[0] / 10.0f;
			float y = event.values[1] / 10.0f;
			float z = event.values[2] / 10.0f;
			if (f2MainAct==null)
            {f1MainAct.getSensor(x,y,z);}
            else
                f2MainAct.getSensor(x, y, z);
		}
	}
}
