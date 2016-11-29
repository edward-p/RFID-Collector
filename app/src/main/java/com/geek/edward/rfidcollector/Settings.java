package com.geek.edward.rfidcollector;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.nfc.NfcAdapter;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;

public class Settings extends AppCompatActivity {
//设置界面仅一键恢复功能
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);


        Button button=(Button) findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean isexisted=false;
                File f=new File(Environment.getRootDirectory().getPath()+"/etc/libnfc-nxp.conf.bak");
                if(f.exists())
                    isexisted=true;
                if(isexisted){
                    //挂载system为rw
                    Process p=null;
                    try {
                        p=Runtime.getRuntime().exec(new String[]{"su","-c","mount -o rw,remount "+Environment.getRootDirectory().getPath()});
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    try {
                        p.waitFor();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    try {
                        p=Runtime.getRuntime().exec(new String[]{"su","-c","mv "+Environment.getRootDirectory().getPath()+"/etc/libnfc-nxp.conf.bak "+Environment.getRootDirectory().getPath()+"/etc/libnfc-nxp.conf"});
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    try {
                        p.waitFor();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    //挂载system为ro
                    try {
                        p=Runtime.getRuntime().exec(new String[]{"su","-c","mount -o ro,remount "+Environment.getRootDirectory().getPath()});
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    try {
                        p.waitFor();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    //重启NFC服务
                    try {
                        p=Runtime.getRuntime().exec(new String[]{"su","-c","svc nfc disable"});
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    try {
                        p.waitFor();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    try {
                        p=Runtime.getRuntime().exec(new String[]{"su","-c","svc nfc enable"});
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    try {
                        p.waitFor();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    Toast suc=Toast.makeText(getApplicationContext(), "Already restore to default!", Toast.LENGTH_SHORT);
                    suc.show();
                    SharedPreferences spcu = getSharedPreferences("current", MODE_PRIVATE);
                    SharedPreferences.Editor ecu = spcu.edit();
                    ecu.putString("current","N/A");
                    ecu.apply();
                }
                else{
                    Toast suc=Toast.makeText(getApplicationContext(), "No backup file found!", Toast.LENGTH_SHORT);
                    suc.show();
                }
            }
        });
        Button add=(Button) findViewById(R.id.button2);
        add.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EditText uid= (EditText) findViewById(R.id.editText);
                String UID= uid.getText().toString();
                Intent in=new Intent();
                in.setClass(Settings.this,MainActivity.class);
                in.putExtra("UID",UID);
                setResult(2,in);
                finish();
            }
        });
    }
}
