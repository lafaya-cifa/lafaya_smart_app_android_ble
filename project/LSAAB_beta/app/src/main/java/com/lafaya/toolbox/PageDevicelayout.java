package com.lafaya.toolbox;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.github.rahatarmanahmed.cpv.CircularProgressView;

import java.util.ArrayList;
import java.util.List;

import me.aflak.bluetooth.Bluetooth;
import me.aflak.bluetooth.BluetoothCallback;
import me.aflak.bluetooth.DiscoveryCallback;

/**
 * Created by JeffYoung on 2016/11/3.
 **/
public class PageDevicelayout extends Activity{
    private final int REQUEST_ENABLE_BT = 0xa01;
    private final int PERMISSION_REQUEST_COARSE_LOCATION = 0xb01;
    private Bluetooth bluetooth;
    private BluetoothAdapter bluetoothAdapter;

    public List<BluetoothDevice> allpaireddevice = new ArrayList<>();
    public List<BluetoothDevice> onlinedevice = new ArrayList<>();
    public List<BluetoothDevice> paireddevice = new ArrayList<>();
    public List<BluetoothDevice> unpaireddevice = new ArrayList<>();
    private List<String> pairenames = new ArrayList<>();
    private List<String> unpairenames = new ArrayList<>();

    private boolean registered = false;

    private AutoCountListView list_devive_paired,list_devive_unpaired;

    Button button_device_cancle,button_device_scan;
    LinearLayout progress_scandevice;

//    private static final String devicefileName = "doorlist";//定义保存设备清单的文件名称
 //   SharedPreferences devicelist = null;
  //  private String device_name_now = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_devicelayout);
        //处理处理安卓不同版本兼容性问题
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_COARSE_LOCATION);
            }
        }

        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(mReceiver,filter);
        registered = true;

        bluetooth = new Bluetooth(this);
        bluetooth.onStart();
        if(!bluetooth.isEnabled()){
            bluetooth.enable();
        }

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        list_devive_paired = findViewById(R.id.list_devive_paired);
        list_devive_unpaired = findViewById(R.id.list_devive_unpaired);
        button_device_cancle = findViewById(R.id.button_device_cancle);
        button_device_scan = findViewById(R.id.button_device_scan);
        progress_scandevice = findViewById(R.id.progress_scandevice);
        //

        listenBluetoothState();
        listenBluetoothDiscovery();

        //在线已配对设备选择
        list_devive_paired.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if(!paireddevice.isEmpty()) {
                    BluetoothDevice d = paireddevice.get(position);

                    int pos = 0;
                    for (BluetoothDevice d1 : allpaireddevice) {
                        if (d.equals(d1)) {
                            //返回选择的位置
                            setResult(RESULT_OK, (new Intent()).putExtra("pos", pos));
                            break;
                        }
                        pos++;
                    }

                    if (registered) {
                        unregisterReceiver(mReceiver);
                        registered = false;
                    }
                    bluetooth.removeDiscoveryCallback();
                    finish();
                }
            }
        });
        //在线未配对设备选择
        list_devive_unpaired.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                /*返回选择的位置*/
                /*配对设备*/
                Message msg = new Message();
                msg.what = 6;
                Bundle bundle = new Bundle();
                bundle.putString("pos",Integer.toString(position));
                msg.setData(bundle);
                bthandler.sendMessage(msg);
            }
        });

        //扫描设备
        button_device_scan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                deviceScan();
            }
        });

        //取消扫描
        button_device_cancle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(registered) {
                    unregisterReceiver(mReceiver);
                    registered=false;
                }
                bluetoothAdapter.cancelDiscovery();
                bluetooth.removeDiscoveryCallback();
                finish();
            }
        });

        //启动扫描
        cleardevice();
        updateDeviceList();
        //deviceScan();
        addPariedDeviceList(allpaireddevice,list_devive_paired);

    }


    public void onDestroy() {
        super.onDestroy();
        if(registered) {
            unregisterReceiver(mReceiver);
            registered=false;
        }
    }

    @Override
    protected void onStart(){
        super.onStart();
        bluetooth.onStart();
    }
    @Override
    protected void onStop(){
        super.onStop();
        bluetooth.onStop();
    }


    // scan bluetooth devices
    private void deviceScan(){
        cleardevice();
        progress_scandevice.setVisibility(View.VISIBLE);
        button_device_scan.setEnabled(false);
        button_device_scan.setTextColor(this.getColor(R.color.colorSensor));
        //start to scan devices
        bluetoothAdapter.startDiscovery();
        bluetooth.startScanning();

    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // TODO Auto-generated method stub
        if(keyCode == KeyEvent.KEYCODE_BACK)
        {
            finish();
        }
        return false;
    }


    /*显示在线设备*/
    private void addDeviceList(){
        if(onlinedevice.size() > 0) {
            BluetoothDevice de = onlinedevice.get(onlinedevice.size() - 1);

            //check paired device
            for (BluetoothDevice de1 : allpaireddevice) {
                if (de.equals(de1)) {
                    pairenames.add(de.getName());
                    paireddevice.add(de);
                    showList(pairenames, list_devive_paired);
                    return;
                }
            }
            //unpaired device
            if (de.getName() != null) {
                unpairenames.add(de.getName());
                unpaireddevice.add(de);
                showList(unpairenames, list_devive_unpaired);
            }
        }
    }

    private void updateDeviceList(){
        Boolean matchlflag = false;
        if(onlinedevice.size() > 0){
            pairenames = new ArrayList<>();
            paireddevice = new ArrayList<>();
            unpairenames = new ArrayList<>();
            unpaireddevice = new ArrayList<>();
            allpaireddevice = bluetooth.getPairedDevices();

            for(BluetoothDevice de : onlinedevice){
                //update paired device
                for(BluetoothDevice de1 : allpaireddevice){
                    if (de.equals(de1)) {
                        pairenames.add(de.getName());
                        paireddevice.add(de);
                        matchlflag = true;
                        break;
                    }
                }
                //update unpaired device
                if(!matchlflag){
                    if(de.getName() != null) {
                        unpairenames.add(de.getName());
                        unpaireddevice.add(de);
                    }
                }
                matchlflag = false;
            }
            showList(pairenames,list_devive_paired);
            showList(unpairenames,list_devive_unpaired);
        }
    }

    private void showList( List<String> names,AutoCountListView showlist){
        if(names.size() > 0){
            int size = names.size();
            String[] namesarray = names.toArray(new String[size]);
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.drawer_list, R.id.text_drawerlist, namesarray);
            showlist.setAdapter(adapter);
            // 设置显示高度
            if (adapter.getCount() > 0) {
                AutoCountListView.setListViewHeightBasedOnChildren(showlist);
            }
        }
    }

    private void addPariedDeviceList(List<BluetoothDevice> delist,AutoCountListView showlist ){

        paireddevice = delist;
        if(delist.size()> 0) {
            List<String> names = new ArrayList<>();
            for (BluetoothDevice de : delist) {
                names.add(de.getName());
            }
            int size = names.size();
            String[] unpairearrayarray = names.toArray(new String[size]);
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.drawer_list, R.id.text_drawerlist, unpairearrayarray);
            showlist.setAdapter(adapter);
            // 设置显示高度
            if (adapter.getCount() > 0) {
                AutoCountListView.setListViewHeightBasedOnChildren(showlist);
            }
        }
    }


//清除所有状态
    private void cleardevice(){
        progress_scandevice.setVisibility(View.GONE);
        pairenames = new ArrayList<>();
        unpairenames = new ArrayList<>();
        paireddevice = new ArrayList<>();
        unpaireddevice = new ArrayList<>();
        onlinedevice = new ArrayList<>();
        allpaireddevice = bluetooth.getPairedDevices();
    }

    //Listen on bluetooth state changes
    private void listenBluetoothState(){
        bluetooth.setBluetoothCallback(new BluetoothCallback() {
            @Override
            public void onBluetoothTurningOn() {
                Toast.makeText(PageDevicelayout.this, "正在打开蓝牙", Toast.LENGTH_LONG).show();
            }

            @Override
            public void onBluetoothOn() {
                Toast.makeText(PageDevicelayout.this, "蓝牙已打开", Toast.LENGTH_LONG).show();
            }

            @Override
            public void onBluetoothTurningOff() {
                Toast.makeText(PageDevicelayout.this, "正在关闭蓝牙", Toast.LENGTH_LONG).show();
            }

            @Override
            public void onBluetoothOff() {
                Toast.makeText(PageDevicelayout.this, "蓝牙已关闭", Toast.LENGTH_LONG).show();
            }

            @Override
            public void onUserDeniedActivation() {

            }
        });
    }

    //Listen on discovery and pairing
    public void listenBluetoothDiscovery(){

        bluetooth.setDiscoveryCallback(new DiscoveryCallback() {
            @Override
            public void onFinish() {
                //scan finished
                Message msg = new Message();
                msg.what = 1;
                bthandler.sendMessage(msg);
            }

            @Override
            public void onDevice(BluetoothDevice device) {
                //device found
                onlinedevice.add(device);
                Message msg = new Message();
                msg.what = 2;
                bthandler.sendMessage(msg);
            }

            @Override
            public void onPair(BluetoothDevice device) {
                //device paired
                Message msg = new Message();
                msg.what = 3;
                bthandler.sendMessage(msg);

            }

            @Override
            public void onUnpair(BluetoothDevice device) {
                //device unpaired
                Message msg = new Message();
                msg.what = 4;
                bthandler.sendMessage(msg);

            }

            @Override
            public void onError(String message) {
                //error occurred
                Message msg = new Message();
                msg.what = 5;
                bthandler.sendMessage(msg);

            }
        });
    }

    @SuppressLint("HandlerLeak")
    private Handler bthandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1://扫描完成
                    progress_scandevice.setVisibility(View.GONE);
                    button_device_scan.setEnabled(true);
                    button_device_scan.setTextColor(PageDevicelayout.this.getColor(R.color.colorPrimary));
                    msg.what = 0;
                    break;
                case 2://扫描到设备
                    addDeviceList();
                    msg.what = 0;
                    break;
                case 3://配对成功
                    updateDeviceList();
                    Toast.makeText(PageDevicelayout.this, "配对成功", Toast.LENGTH_SHORT).show();
                    msg.what = 0;
                    break;
                case 4://配对失败
                    updateDeviceList();
                    Toast.makeText(PageDevicelayout.this, "配对失败", Toast.LENGTH_SHORT).show();
                    msg.what = 0;
                    break;
                case 5://出错
                    updateDeviceList();
                    msg.what = 0;
                    break;
                case 6:
                    Toast.makeText(PageDevicelayout.this, "正在配对设备", Toast.LENGTH_SHORT).show();
                    bluetooth.pair(unpaireddevice.get(Integer.parseInt(msg.getData().getString("pos"))));
                    msg.what = 0;
                    break;
                default:
                    break;
            }
        }
    };
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);

                switch (state) {
                    case BluetoothAdapter.STATE_OFF:
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {

                            }
                        });
                        Toast.makeText(PageDevicelayout.this, "蓝牙已关闭", Toast.LENGTH_LONG).show();
                        break;
                    case BluetoothAdapter.STATE_ON:
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                cleardevice();
                                Message msg = new Message();
                                msg.what = 5;
                                bthandler.sendMessage(msg);
                            }
                        });
                        Toast.makeText(PageDevicelayout.this, "蓝牙已打开", Toast.LENGTH_LONG).show();
                        break;
                }
            }
        }
    };

}
