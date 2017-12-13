package com.example.lee.zenwatchapp;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.wearable.view.WatchViewStub;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static java.lang.Math.sqrt;

public class MainActivity extends Activity  {

    private TextView connect_status_view;
    private Socket ClientSocket;
    private Button connect_btn;
    private String MyID;
    private boolean connect_status = false;
    private SensorManager mSensorManager;
    private SensorEventListener mSensorListener;
    private static final float NS2S = 1.0f / 1000000000.0f;
    private float timestamp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {
                Init();
            }
        });
    }

    protected void onDestroy() {
        super.onDestroy();
        new DisConnection().execute("");
    }

    private void Init(){

        connect_btn = (Button) findViewById(R.id.connect_btn);

        connect_status_view = (TextView) findViewById(R.id.connect_status_view);

        connect_btn.setOnClickListener(ButtonListener);

        ClientSocket = new Socket();

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mSensorListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                Sensor sensor = event.sensor;
                if (sensor.getType() == Sensor.TYPE_GYROSCOPE) {
                    Get_Gyroscope_Data(event);
                    //Log.d("TYPE_GYROSCOPE","TYPE_GYROSCOPE");
                }
                else if (sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                    Get_Accelerometer_Data(event);
                    //Log.d("TYPE_ACCELEROMETER","TYPE_ACCELEROMETER");
                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {

            }
        };

        mSensorManager.registerListener(mSensorListener, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), 500000);
        mSensorManager.registerListener(mSensorListener, mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE), 200000);

        File file = new File(this.getFilesDir().getPath() + "/Config.properties");
        if (!file.exists()){
            CreatePropertiesFile(this);
        }

        Get_App_ID();

    }

    private View.OnClickListener ButtonListener = new View.OnClickListener(){

        @Override
        public void onClick(View v) {
            switch (v.getId()){
                case R.id.connect_btn:
                    if (!connect_status) {
                        new Connection().execute("");
                    }
                    else {
                        new DisConnection().execute("");
                    }
                    break;
            }
        }
    };

    private class Connection extends AsyncTask<String,Void,String> {
        @Override
        protected String doInBackground(String... params) {
            try{
                String HOST_IP = "140.127.196.75";
                int PORT = 6666;
                ClientSocket = new Socket();
                ClientSocket.connect(new InetSocketAddress(HOST_IP, PORT), 5000);
            }
            catch (Exception e){
                Log.d("ConnectedException",e.getMessage());
            }
            return null;
        }
        @Override
        protected void onPostExecute(String value){
            if (ClientSocket.isConnected()){
                new Send_ID().execute("");
                connect_btn.setText("Disconnect");
                connect_status_view.setText("Connected");
                connect_status = true;
            }
        }
        @Override
        protected void onPreExecute() {
        }
    }

    private class DisConnection extends AsyncTask<String,Void,String>{
        @Override
        protected String doInBackground(String... params) {
            try{
                if (ClientSocket.isConnected())
                    ClientSocket.close();
                    connect_status = false;
            }
            catch (Exception e){
                Log.d("ConnectedException",e.getMessage());
            }
            return null;
        }
        @Override
        protected void onPostExecute(String value){
            connect_btn.setText("Connect");
            connect_status_view.setText("Disconnect");
        }
        @Override
        protected void onPreExecute() {
        }
    }

    private class Send_ID extends AsyncTask<String,String,String>{

        @Override
        protected String doInBackground(String... params) {
            try{
                if (ClientSocket.isConnected()){
                    if (!params[0].equals("null")) {
                        byte[] Data = ("DeviceID:" + MyID + ";").getBytes("UTF-8");
                        OutputStream Os = ClientSocket.getOutputStream();
                        Os.write(Data);
                    }
                }
            }
            catch (Exception e){
            }
            return null;
        }
    }

    private class Send_Gyroscope_Data extends AsyncTask<String,Void,String>{

        @Override
        protected String doInBackground(String... params) {
            try{
                if (ClientSocket.isConnected()) {
                    if (!params[0].equals("null")) {
                        byte[] Data = ("DeviceGyroscope:" + params[0] + "," + GetTime() + ";").getBytes("UTF-8");
                        OutputStream os = ClientSocket.getOutputStream();
                        os.write(Data);
                    }
                }
            }
            catch (Exception e){
                //Log.d("SendException",e.getMessage());
            }
            return null;
        }
    }

    private class Send_Accelerometer_Data extends AsyncTask<String,Void,String>{

        @Override
        protected String doInBackground(String... params) {
            try{
                if (ClientSocket.isConnected()) {
                    if (!params[0].equals("null")) {
                        byte[] Data = ("DeviceAccelerometer:" + params[0] + "," + GetTime() + ";").getBytes("UTF-8");
                        OutputStream os = ClientSocket.getOutputStream();
                        os.write(Data);
                    }
                }
            }
            catch (Exception e){
                //Log.d("SendException",e.getMessage());
            }
            return null;
        }
    }

    public void CreatePropertiesFile(Context context) {
        Properties prop = new Properties();
        String propertiesPath = context.getFilesDir().getPath() + "/Config.properties";
        try {
            FileOutputStream out = new FileOutputStream(propertiesPath);
            prop.setProperty("APP_ID", Create_app_id());
            prop.store(out, "store");
            out.close();
        } catch (IOException e) {
            System.err.println("Failed to open app.properties file");
            e.printStackTrace();
        }
    }

    private String Create_app_id(){
        String[] ID_Data = new String[2];
        String ID;
        ID_Data[0]= " ";
        ID_Data[1]= " ";
        for (int i = 0; i < 4; i++){

            int Chose = (int)(Math.random()*2);

            if (Chose==0){
                int word_chose =  (int)(Math.random()* 9);
                ID_Data[0] += String.valueOf(word_chose);
            }

            if (Chose==1){
                int word_chose =  (int)(Math.random() * (90 - 65 + 1) + 65);
                char word = (char) word_chose;
                ID_Data[0] += String.valueOf(word);
            }

        }

        for (int i = 0; i < 4; i++){

            int Chose = (int)(Math.random()*2);

            if (Chose==0){
                int word_chose =  (int)(Math.random()* 9);
                ID_Data[1] += String.valueOf(word_chose);
            }

            if (Chose==1){
                int word_chose =  (int)(Math.random() * (90 - 65 + 1) + 65);
                char word = (char) word_chose;
                ID_Data[1] += String.valueOf(word);
            }

        }
        ID = ID_Data[0].trim() + "-" + ID_Data[1].trim();
        return ID;
    }

    private void Get_App_ID(){
        Properties prop = new Properties();
        String propertiesPath = this.getFilesDir().getPath() + "/Config.properties";
        try {
            FileInputStream inputStream = new FileInputStream(propertiesPath);
            prop.load(inputStream);
            Log.d("ID_Get",prop.getProperty("APP_ID"));
            MyID = prop.getProperty("APP_ID");
        } catch (IOException e) {
            System.err.println("Failed to open app.properties file");
            e.printStackTrace();
        }
    }

    public void Get_Gyroscope_Data(SensorEvent event){
        if (timestamp != 0) {
            final float dT = (event.timestamp - timestamp) * NS2S;
            float axisX = event.values[0];
            float axisY = event.values[1];
            float axisZ = event.values[2];

            float omegaMagnitude = (float) sqrt(axisX*axisX + axisY*axisY + axisZ*axisZ);

            if (omegaMagnitude > 1) {
                axisX /= omegaMagnitude;
                axisY /= omegaMagnitude;
                axisZ /= omegaMagnitude;
            }

            if (connect_status) {
                new Send_Gyroscope_Data().execute(String.valueOf(axisX) + "@" + String.valueOf(axisY) + "#" + String.valueOf(axisZ));
            }
        }
        timestamp = event.timestamp;
    }

    public void Get_Accelerometer_Data(SensorEvent event){

        final double alpha = 0.8;
        double[] gravity = new double[3];
        gravity[0] = alpha * gravity[0] + (1 - alpha) * event.values[0];
        gravity[1] = alpha * gravity[1] + (1 - alpha) * event.values[1];
        gravity[2] = alpha * gravity[2] + (1 - alpha) * event.values[2];

        double[] linear_acceleration = new double[3];
        linear_acceleration[0] = event.values[0] - gravity[0];
        linear_acceleration[1] = event.values[1] - gravity[1];
        linear_acceleration[2] = event.values[2] - gravity[2];

        if (connect_status) {
            new Send_Accelerometer_Data().execute(String.valueOf(linear_acceleration[0]) + "@" + String.valueOf(linear_acceleration[1]) + "#" + String.valueOf(linear_acceleration[2]));
        }
    }

    private String GetTime(){
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss:sss");
        Date time = new Date(System.currentTimeMillis());
        return format.format(time);
    }
}
