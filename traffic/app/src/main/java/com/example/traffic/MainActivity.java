package com.example.traffic;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.Set;

public class MainActivity extends AppCompatActivity {
    //获取蓝牙实例;
    private BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    //已配对的设备适配器
    private ArrayAdapter<String> mPairedDevicesArrayAdapter;
    //未配对的设备适配器
    private ArrayAdapter<String> mUnPairedDevicesArrayAdapter;

    private Button buttonView;

    @SuppressLint("MissingPermission")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        //如果打不开蓝牙提示信息，结束程序
        if (mBluetoothAdapter == null) {
            Toast.makeText(getApplicationContext(), "无法打开手机蓝牙，请确认手机是否有蓝牙功能！", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }


        buttonView = (Button) findViewById(R.id.discovery);
        buttonView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                doDiscovery();
            }
        });


        mPairedDevicesArrayAdapter = new ArrayAdapter<String>(this, R.layout.device_name);
        mUnPairedDevicesArrayAdapter = new ArrayAdapter<String>(this, R.layout.device_name);


        ListView pairedListView = (ListView) findViewById(R.id.pairedListView);
        pairedListView.setAdapter(mPairedDevicesArrayAdapter);

        pairedListView.setOnItemClickListener( mDeviceClickListener);

        ListView unPairedListView = (ListView) findViewById(R.id.unPairedListView);
        unPairedListView.setAdapter(mUnPairedDevicesArrayAdapter);

        unPairedListView.setOnItemClickListener( mDeviceClickListener);
        // 注册接收查找到设备action接收器
        this.registerReceiver(mReceiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));
        // 注册查找结束action接收器
        this.registerReceiver(mReceiver, new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED));
        this.registerReceiver(mReceiver, new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_STARTED));

        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();

        //添加已配对设备到列表并显示
        if (pairedDevices.size() > 0) {
            findViewById(R.id.pairedListView).setVisibility(View.VISIBLE);
            for (BluetoothDevice device : pairedDevices) {
                mPairedDevicesArrayAdapter.add(device.getName() + "\n" + device.getAddress());
            }
        } else {
            String noDevices = "没有找到已配对的设备。";
            mPairedDevicesArrayAdapter.add(noDevices);
        }

    }


    // 查找到设备和搜索完成action监听器
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            // 查找到设备action
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // 得到蓝牙设备
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // 如果是已配对的则略过，已得到显示，其余的在添加到列表中进行显示
                if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
                    mUnPairedDevicesArrayAdapter.add(device.getName() + "\n" + device.getAddress());
                } else {  //添加到已配对设备列表
                    mPairedDevicesArrayAdapter.add(device.getName() + "\n" + device.getAddress());
                }
                // 搜索完成action
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                setTitle("选择要连接的设备");
                if (mUnPairedDevicesArrayAdapter.getCount() == 0) {
                    String noDevices = "没有找到新设备";
                    mUnPairedDevicesArrayAdapter.add(noDevices);
                }
            }
        }
    };

    @SuppressLint("MissingPermission")
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 关闭服务查找
        if (mBluetoothAdapter != null) {
            mBluetoothAdapter.cancelDiscovery();
        }
        // 注销action接收器
        this.unregisterReceiver(mReceiver);
    }


    /**
     * 开始服务和设备查找
     */
    private void doDiscovery() {

        // 在窗口显示查找中信息
        setTitle("查找设备中...");

        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){//未开启定位权限
            //开启定位权限,200是标识码
            ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.ACCESS_FINE_LOCATION},200);
        }else{
            // 显示其它设备（未配对设备）列表
            findViewById(R.id.textView).setVisibility(View.VISIBLE);
            // 关闭再进行的服务查找
            if (mBluetoothAdapter.isDiscovering()) {
                mBluetoothAdapter.cancelDiscovery();
            }

            mBluetoothAdapter.startDiscovery();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){
            case 200:
                if(grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    doDiscovery();
                }else{
                    finish();
                }
                break;
            default:
        }
    }


    // 选择设备响应函数
    private AdapterView.OnItemClickListener mDeviceClickListener = new AdapterView.OnItemClickListener() {
        @SuppressLint("MissingPermission")
        public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3) {
            // 准备连接设备，关闭服务查找
            mBluetoothAdapter.cancelDiscovery();

            // 得到mac地址
            String info = ((TextView) v).getText().toString();
            String address = info.substring(info.length() - 17);

            // 设置返回数据
            Intent signalIntent = new Intent(getApplicationContext(), SignalActivity.class); //跳转活动
            signalIntent.putExtra("macAddress", address);
            startActivity(signalIntent ); //设置返回宏定义

        }
    };
}