package com.example.lee.zenwatchapp;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.wearable.view.WatchViewStub;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Properties;

public class WatchMainActivity extends Activity{

    private TextView idtextview;
    public static TextView export_status_view;
    private static TextView recording_status_view;
    private Socket ExportSocket;
    private static Button record_btn;
    private static String MyID;
    private boolean Recording = false;
    private Intent ServiceIntent;

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
        stopService(ServiceIntent);
    }

    private void Init(){

        requestPermission();

        ServiceIntent = new Intent(WatchMainActivity.this, RecordService.class);

        record_btn = (Button) findViewById(R.id.record_btn);
        Button export_btn = (Button) findViewById(R.id.export_btn);

        recording_status_view = (TextView) findViewById(R.id.recording_status_view);
        export_status_view = (TextView) findViewById(R.id.export_status_view);
        idtextview = (TextView) findViewById(R.id.idtextview);

        record_btn.setOnClickListener(ButtonListener);
        export_btn.setOnClickListener(ButtonListener);

        File file = new File(this.getFilesDir().getPath() + "/Config.properties");
        if (!file.exists()){
            CreatePropertiesFile(this);
        }

        Get_App_ID();
    }

    private void requestPermission() {
        int ReadExternalPermission = checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE);
        int WriteExternalPermission = checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (ReadExternalPermission != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
        }
        if (WriteExternalPermission != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        }
    }


    private View.OnClickListener ButtonListener = new View.OnClickListener(){

        @Override
        public void onClick(View v) {
            switch (v.getId()){
                case R.id.record_btn:
                    if (Recording){
                        stopService(ServiceIntent);
                        Recording = false;
                        Toast.makeText(WatchMainActivity.this, "Stop Record", Toast.LENGTH_SHORT).show();
                    }
                    else {
                        startService(ServiceIntent);
                        recording_status_view.setText("ON");
                        Recording = true;
                        record_btn.setText("Record OFF");
                        export_status_view.setText("Waiting");
                        Toast.makeText(WatchMainActivity.this, "Start Record", Toast.LENGTH_SHORT).show();
                    }
                    break;
            }
        }
    };

    public static void ChangeRecordStatusView(){
        record_btn.setText("Record ON");
        recording_status_view.setText("OFF");
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

    public void Get_App_ID(){
        Properties prop = new Properties();
        String propertiesPath = this.getFilesDir().getPath() + "/Config.properties";
        try {
            FileInputStream inputStream = new FileInputStream(propertiesPath);
            prop.load(inputStream);
            MyID = prop.getProperty("APP_ID");
            idtextview.setText(MyID);
        } catch (IOException e) {
            System.err.println("Failed to open app.properties file");
            e.printStackTrace();
        }
    }

    public static String App_ID(){
        return MyID;
    }

    public static void updateLog(String TAG, String Msg){
        Log.d(TAG, Msg);
    }
}
