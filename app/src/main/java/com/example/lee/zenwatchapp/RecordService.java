package com.example.lee.zenwatchapp;

import android.app.Service;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.IBinder;
import android.support.annotation.Nullable;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.Date;

import static java.lang.Math.sqrt;

public class RecordService extends Service{
    private static final String TAG = "RecordService";
    private float GyroscopeTimestamp, AccelerometerTimestamp;
    private SQLiteManager SQLiteManag;
    private final static int SQLiteVersion = 2;
    private SensorManager mSensorManager;
    public boolean InsertGyroscopeToken,InsertAccelerometerToken;

    @Override
    public void onCreate(){
        super.onCreate();
        WatchMainActivity.updateLog(TAG, "----> onCreate()");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId){
        WatchMainActivity.updateLog(TAG, "----> onStartCommand()");
        Init();
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy(){
        WatchMainActivity.updateLog(TAG, "----> onDestroy()");
        //mSensorManager.unregisterListener();
        InsertGyroscopeToken = false;
        InsertAccelerometerToken = false;
        new ExportDBFile().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR);
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void Init(){

        SQLiteManag = new SQLiteManager(this, WatchMainActivity.App_ID() + ".db", null, SQLiteVersion);

        if (InsertGyroscopeToken || InsertAccelerometerToken){
            InsertGyroscopeToken = false;
            InsertAccelerometerToken = false;
        }

        InsertGyroscopeToken = true;
        InsertAccelerometerToken = true;

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        SensorEventListener mSensorListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                Sensor sensor = event.sensor;
                if (sensor.getType() == Sensor.TYPE_GYROSCOPE) {
                    Get_Gyroscope_Data(event);
                } else if (sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                    Get_Accelerometer_Data(event);
                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {

            }
        };

        mSensorManager.registerListener(mSensorListener, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(mSensorListener, mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE), SensorManager.SENSOR_DELAY_NORMAL);

    }

    public void Get_Gyroscope_Data(SensorEvent event){
        if ((event.timestamp - GyroscopeTimestamp) > 200000000) {
            float axisX = event.values[0];
            float axisY = event.values[1];
            float axisZ = event.values[2];

            float omegaMagnitude = (float) sqrt(axisX * axisX + axisY * axisY + axisZ * axisZ);

            if (omegaMagnitude > 1) {
                axisX /= omegaMagnitude;
                axisY /= omegaMagnitude;
                axisZ /= omegaMagnitude;
            }
            if (InsertGyroscopeToken) {
                //WatchMainActivity.updateLog("InsertGyroscope",String.valueOf(axisX)+","+String.valueOf(axisY)+","+String.valueOf(axisZ));
                new InsertGyroscope().execute(String.valueOf(axisX), String.valueOf(axisY), String.valueOf(axisZ));
            }
            GyroscopeTimestamp = event.timestamp;
        }
    }

    public void Get_Accelerometer_Data(SensorEvent event){
        if ((event.timestamp - AccelerometerTimestamp) > 200000000){
            final double alpha = 0.8;
            double[] gravity = new double[3];
            gravity[0] = alpha * gravity[0] + (1 - alpha) * event.values[0];
            gravity[1] = alpha * gravity[1] + (1 - alpha) * event.values[1];
            gravity[2] = alpha * gravity[2] + (1 - alpha) * event.values[2];

            double[] linear_acceleration = new double[3];
            linear_acceleration[0] = event.values[0] - gravity[0];
            linear_acceleration[1] = event.values[1] - gravity[1];
            linear_acceleration[2] = event.values[2] - gravity[2];

            if(InsertAccelerometerToken){
                //WatchMainActivity.updateLog("InsertAccelerometer",String.valueOf(linear_acceleration[0])+","+String.valueOf(linear_acceleration[1])+","+String.valueOf(linear_acceleration[2]));
                new InsertAccelerometer().execute(String.valueOf(linear_acceleration[0]),String.valueOf(linear_acceleration[1]),String.valueOf(linear_acceleration[2]));
            }
            AccelerometerTimestamp = event.timestamp;
        }
    }

    private class InsertGyroscope extends AsyncTask<String,String,String> {
        @Override
        protected String doInBackground(String... params) {
            ContentValues content = new ContentValues();
            content.put("axisX", params[0]);
            content.put("axisY", params[1]);
            content.put("axisZ", params[2]);
            content.put("StoreDate", GetTime());
            SQLiteManag.getWritableDatabase().insert("Gyroscope", null, content);
            WatchMainActivity.updateLog("Gyroscope", "Insert");
            return null;
        }
        @Override
        protected void onPostExecute(String value){
        }
        @Override
        protected void onPreExecute() {
        }
    }

    private class InsertAccelerometer extends AsyncTask<String,String,String> {
        @Override
        protected String doInBackground(String... params) {
            ContentValues content = new ContentValues();
            content.put("axisX", params[0]);
            content.put("axisY", params[1]);
            content.put("axisZ", params[2]);
            content.put("StoreDate", GetTime());
            SQLiteManag.getWritableDatabase().insert("Accelerometer", null, content);
            WatchMainActivity.updateLog("Accelerometer", "Insert");
            return null;
        }
        @Override
        protected void onPostExecute(String value){
        }
        @Override
        protected void onPreExecute() {
        }
    }

    private String GetTime(){
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss:SSS");
        Date time = new Date(System.currentTimeMillis());
        return format.format(time);
    }

    private class ExportDBFile extends AsyncTask<String,Void,String> {
        @Override
        protected String doInBackground(String... params) {
            File sd = Environment.getExternalStorageDirectory();
            File Data = Environment.getDataDirectory();
            FileChannel source;
            FileChannel destination;
            String currenDBPath = "/data/" + "com.example.lee.zenwatchapp" + "/databases/" + WatchMainActivity.App_ID() + ".db";
            String backupDBPath = WatchMainActivity.App_ID() + ".db";
            File currenceDB = new File(Data, currenDBPath);
            File backupDB = new File(sd, backupDBPath);
            try{
                source = new FileInputStream(currenceDB).getChannel();
                destination = new FileOutputStream(backupDB).getChannel();
                destination.transferFrom(source, 0, source.size());
                source.close();
                destination.close();
                WatchMainActivity.updateLog("File Export", "File Export!");
            }catch (IOException e){
                e.printStackTrace();
            }
            return null;
        }
        @Override
        protected void onPostExecute(String value){
            WatchMainActivity.ChangeRecordStatusView();
        }
        @Override
        protected void onPreExecute() {
        }
    }
}
