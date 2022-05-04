package com.example.traffic;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class SignalActivity extends AppCompatActivity implements View.OnClickListener {
    //SPP服务UUID号
    private final static String MY_UUID = "00001101-0000-1000-8000-00805F9B34FB";
    //蓝牙设备
    BluetoothDevice mBluetoothDevice = null;
    //蓝牙通信Socket
    BluetoothSocket mBluetoothSocket = null;
    //获取蓝牙实例
    private BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    //运行状态
    boolean bRun = true;
    //读取线程状态
    boolean bThread = false;
    //输入流，用来接收蓝牙数据
    private InputStream is;
    //显示用数据缓存
    private String smsg = "";

    private  TextView lightState =null ;
    @SuppressLint("MissingPermission")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.signal_main);


        Intent intent =getIntent();
        /*取出Intent中附加的数据*/
        String macAddress = intent.getStringExtra("macAddress");

        // 得到蓝牙设备句柄
        mBluetoothDevice = mBluetoothAdapter.getRemoteDevice(macAddress);


        // 用服务号得到socket
        try {
            mBluetoothSocket = mBluetoothDevice.createRfcommSocketToServiceRecord(UUID.fromString(MY_UUID));
        } catch (IOException e) {
            Toast.makeText(this, "连接失败！", Toast.LENGTH_SHORT).show();
        }


        Button  redButton = findViewById(R.id.red);
        Button  greenButton = findViewById(R.id.green);
        Button  yellowButton = findViewById(R.id.yellow);

        redButton.setBackgroundColor(Color.parseColor("#FF0000"));
        greenButton.setBackgroundColor(Color.parseColor("#00FF00"));
        yellowButton.setBackgroundColor(Color.parseColor("#FFFF00"));

        redButton.setOnClickListener(this);
        greenButton.setOnClickListener(this);
        yellowButton.setOnClickListener(this);
        //连接socket
        TextView connectView = findViewById(R.id.connectState);
          lightState= findViewById(R.id.lightState);
        try {
            mBluetoothSocket.connect();
            Toast.makeText(this, "连接" + mBluetoothDevice.getName() + "成功！", Toast.LENGTH_SHORT).show();
            connectView.setText("已连接");
        } catch (IOException e) {
            try {
                Toast.makeText(this, "连接失败！", Toast.LENGTH_SHORT).show();
                mBluetoothSocket.close();
                mBluetoothSocket = null;
            } catch (IOException ee) {
                Toast.makeText(this, "连接失败！", Toast.LENGTH_SHORT).show();
            }

            return;
        }

        //打开接收线程
        try {
            is = mBluetoothSocket.getInputStream();   //得到蓝牙数据输入流
        } catch (IOException e) {
            Toast.makeText(this, "接收数据失败！", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!bThread) {
            readThread.start();
            bThread = true;
        } else {
            bRun = true;
        }

    }
    @Override
    public void onClick(View view) {

        switch (view.getId()){
            case R.id.red:
                sendMsg("1");
                break;
            case R.id.green:
                sendMsg("2");
                break;
            case R.id.yellow:
                sendMsg("3");
                break;
            default:
                break;
        }
    }

    //发送消息到蓝牙
    private void  sendMsg(String msg){
        try {
            int n = 0;
            //蓝牙连接输出流
            OutputStream os = mBluetoothSocket.getOutputStream();
            byte[] bos = msg.getBytes();
            for (byte bo : bos) {
                if (bo == 0x0a) n++;
            }
            byte[] bos_new = new byte[bos.length + n];
            n = 0;
            for (byte bo : bos) {
                //手机中换行为0a,将其改为0d 0a后再发送, 0x0D + 0x0A  回车换行
                if (bo == 0x0a) {
                    bos_new[n] = 0x0d;
                    n++;
                    bos_new[n] = 0x0a;
                } else {
                    bos_new[n] = bo;
                }
                n++;
            }

            os.write(bos_new);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //接收数据线程
    Thread readThread=new Thread(){
        public void run(){
            int num ;
            byte[] buffer = new byte[1024];
            byte[] buffer_new = new byte[1024];
            int i  ;
            int n ;
            bRun = true;
            //接收线程
            while(true){
                try{
                    while(is.available()==0){
                        while(!bRun){}
                    }
                    int getCount=0;
                   String smsg="";
                    while(true) {

                        if (!bThread)//跳出循环
                            return;

                        num = is.read(buffer);         //读入数据
                        System.out.println(num);
                        n = 0;

                        for (i = 0; i < num; i++) {
                            //替换换行
                            if ((buffer[i] == 0x0d) && (buffer[i + 1] == 0x0a)) {
                                buffer_new[n] = 0x0a;
                                i++;
                            } else {
                                buffer_new[n] = buffer[i];
                            }
                            n++;
                        }
                        smsg =  new String(buffer_new, 0, n);
                        //由于网络通信会有延时，该处不是最佳方案跳出方案，退出应该和发送端约定

                        if(is.available() == 0 )break;
                    }
                    Message msg=  handler.obtainMessage();
                    msg.obj=smsg;

                    handler.sendMessage(msg);
                }catch(IOException e){
                    e.printStackTrace();
                }
            }
        }
    };



    Handler handler = new Handler(Looper.getMainLooper()){
        @Override
        public void handleMessage(  Message msg) {

                switch (msg.obj.toString()){
                    case "1":
                        lightState.setText("红灯亮");
                        break;
                    case "2":
                        lightState.setText("绿灯亮");
                        break;
                    case "3":
                        lightState.setText("黄灯亮");
                        break;
                    default:
                        break;
                }



        }
    };


}
