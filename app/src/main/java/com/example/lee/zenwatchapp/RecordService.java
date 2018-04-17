package com.example.lee.zenwatchapp;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Service;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.Date;

import static java.lang.Math.sqrt;

public class RecordService extends Service{
    private static final String TAG = "RecordService";
    private float GyroscopeTimestamp, AccelerometerTimestamp;
    private SQLiteManager SQLiteManag;
    private final static int SQLiteVersion = 1;
    private SensorManager mSensorManager;
    public boolean InsertGyroscopeToken,InsertAccelerometerToken;
    private PowerManager.WakeLock wakeLock;

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
        InsertGyroscopeToken = false;
        InsertAccelerometerToken = false;
        new ExportDBFile().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR);
        wakeLock.release();
        SQLiteManag.close();
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void Init(){

        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);

        assert powerManager != null;
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MyWakelockTag");

        wakeLock.acquire();

        SQLiteManag = new SQLiteManager(this, WatchMainActivity.App_ID() + "-" + GetTime() + ".db", null, SQLiteVersion);

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

        mSensorManager.registerListener(mSensorListener, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_FASTEST);
        mSensorManager.registerListener(mSensorListener, mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE), SensorManager.SENSOR_DELAY_FASTEST);

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
                new InsertAccelerometer().execute(String.valueOf(linear_acceleration[0]),String.valueOf(linear_acceleration[1]),String.valueOf(linear_acceleration[2]));
            }
            AccelerometerTimestamp = event.timestamp;
        }
    }

    @SuppressLint("StaticFieldLeak")
    private class InsertGyroscope extends AsyncTask<String,String,String> {
        @Override
        protected String doInBackground(String... params) {
            ContentValues content = new ContentValues();
            content.put("axisX", params[0]);
            content.put("axisY", params[1]);
            content.put("axisZ", params[2]);
            content.put("StoreDate", GetTime());
            content.put("UnixTimeStamp", GetUnixTime());
            SQLiteManag.getWritableDatabase().insert("Gyroscope", null, content);
            return null;
        }
        @Override
        protected void onPostExecute(String value){
        }
        @Override
        protected void onPreExecute() {
        }
    }

    @SuppressLint("StaticFieldLeak")
    private class InsertAccelerometer extends AsyncTask<String,String,String> {
        @Override
        protected String doInBackground(String... params) {
            ContentValues content = new ContentValues();
            content.put("axisX", params[0]);
            content.put("axisY", params[1]);
            content.put("axisZ", params[2]);
            content.put("StoreDate", GetTime());
            content.put("UnixTimeStamp", GetUnixTime());
            SQLiteManag.getWritableDatabase().insert("Accelerometer", null, content);
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

    private long GetUnixTime(){
        return System.currentTimeMillis();
    }

    @SuppressLint("StaticFieldLeak")
    private class ExportDBFile extends AsyncTask<String,Void,String> {
        @Override
        protected String doInBackground(String... params) {
            int i = 0;
            String APPDataPath = Environment.getDataDirectory() + "/data/" + "com.example.lee.zenwatchapp" + "/databases/";
            String ExportPath = Environment.getExternalStorageDirectory() + "/ZenWatchAPPDataFile/";
            LOGD("Files", "APPDataPath: " + APPDataPath);
            LOGD("Files", "ExportPath: " + ExportPath);
            File APPDirectory = new File(APPDataPath);
            File ExportDirectory = new File(ExportPath);
            if (!ExportDirectory.exists()){
                ExportDirectory.mkdir();
            }
            File[] APPDataFiles = APPDirectory.listFiles();
            File[] ExportFiles = ExportDirectory.listFiles();
            LOGD("Files", "APPDataFilesSizes: " + APPDataFiles.length);
            LOGD("Files", "ExportFilesSizes: " + ExportFiles.length);
            while (i < APPDataFiles.length - 1) {
                if (!APPDataFiles[i].getName().contains("journal")) {
                    LOGD("Files", "FileExport: " + APPDataFiles[i].getName());
                    File ACCFile = new File(ExportDirectory, APPDataFiles[i].getName() + "-Accelerometer" + ".csv");
                    File GyrFile = new File(ExportDirectory, APPDataFiles[i].getName() + "-Gyroscope" + ".csv");
                    try{
                        ACCFile.createNewFile();
                        CSVWriter csvWriter = new CSVWriter(new FileWriter(ACCFile));
                        SQLiteManager SQLiteMag = new SQLiteManager(RecordService.this, APPDataFiles[i].getName(), null, SQLiteVersion);
                        SQLiteDatabase db = SQLiteMag.getReadableDatabase();
                        Cursor curCSV = db.rawQuery("SELECT * FROM Accelerometer", null);
                        csvWriter.writeNext(curCSV.getColumnNames());
                        while (curCSV.moveToNext()){
                            String arrStr[] = {curCSV.getString(0),curCSV.getString(1), curCSV.getString(2), curCSV.getString(3), curCSV.getString(4), curCSV.getString(5)};
                            csvWriter.writeNext(arrStr);
                        }
                        db.close();
                        csvWriter.close();
                        curCSV.close();
                    }catch (Exception sqlEX){
                        LOGD(TAG,sqlEX.getMessage());
                    }
                    try{
                        GyrFile.createNewFile();
                        CSVWriter csvWriter = new CSVWriter(new FileWriter(GyrFile));
                        SQLiteManager SQLiteMag = new SQLiteManager(RecordService.this, APPDataFiles[i].getName(), null, SQLiteVersion);
                        SQLiteDatabase db = SQLiteMag.getReadableDatabase();
                        Cursor curCSV = db.rawQuery("SELECT * FROM Gyroscope", null);
                        csvWriter.writeNext(curCSV.getColumnNames());
                        while (curCSV.moveToNext()){
                            String arrStr[] = {curCSV.getString(0),curCSV.getString(1), curCSV.getString(2), curCSV.getString(3), curCSV.getString(4), curCSV.getString(5)};
                            csvWriter.writeNext(arrStr);
                        }
                        db.close();
                        csvWriter.close();
                        curCSV.close();
                    }catch (Exception sqlEX){
                        LOGD(TAG,sqlEX.getMessage());
                    }
                    boolean deleted = APPDataFiles[i].delete();
                    if (deleted){
                        LOGD("Deleted", APPDataFiles[i].getName());
                    }
                }
                boolean deleted = APPDataFiles[i].delete();
                if (deleted){
                    LOGD("Deleted", APPDataFiles[i].getName());
                }
                i++;
            }
            return null;
        }
        @Override
        protected void onPostExecute(String value){
            WatchMainActivity.ChangeRecordStatusView();
            WatchMainActivity.export_status_view.setText("CSV File Exported");
        }
        @Override
        protected void onPreExecute() {
            WatchMainActivity.export_status_view.setText("DB File Transforming");
        }
    }

    private void LOGD(String TAG, String Message){
        Log.d(TAG,Message);
    }

}
