package com.example.user.fast;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.SeekBar;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Method;

public class MainActivity extends AppCompatActivity implements ActivityCompat.OnRequestPermissionsResultCallback {

    private Context mContext;
    public final static int REQUEST_READ_PHONE_STATE = 1;
    private int batteryLevel;
    private int batteryScale;
    private TextView textView;
    private TextView dataText;
    private TextView wifiText;
    private TextView otgText;

    //3.18
    private SeekBar seekBar;
    private TextView ring_sound;
    private AudioManager mAudioManager;
    private int maxVolume,currentVolume;
    private WifiManager mWifiManager;
    private String otgPath= "/proc/otg_power";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        requestWindowFeature(Window.FEATURE_NO_TITLE);// 隐藏标题
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);// 设置全屏
        mContext = this;
        setContentView(R.layout.activity_main);

        wifiText = findViewById(R.id.wifiText);
        dataText = findViewById(R.id.dataText);
        textView = findViewById(R.id.textView);
        //otgText = findViewById(R.id.otgText);
        seekBar = findViewById(R.id.seekBar);
        ring_sound = findViewById(R.id.ring_sound);
        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        maxVolume = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_RING);
        currentVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_RING);
        mWifiManager = (WifiManager)getSystemService(WIFI_SERVICE);
        seekBar.setMax(maxVolume);
        setView();
        setWifi();

        try {
            boolean dataflag =getDataEnabled(0, mContext);
            if (dataflag){
                dataText.setText("4G");
            }else {
                dataText.setText("No net");
            }
        }catch (Exception e) {
            e.printStackTrace();
        }

        //seekbar设置拖动监听
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener()
        {
            public void onProgressChanged(SeekBar arg0,int progress,boolean fromUser) {
                //设置媒体音量为当前seekbar进度
                mAudioManager.setStreamVolume(AudioManager.STREAM_RING, progress, 0);
                currentVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_RING);
                setView();
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {
                // TODO Auto-generated method stub
            }

            @Override public void onStopTrackingTouch(SeekBar seekBar) {
                // TODO Auto-generated method stub
            }
        });

        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        //注册接收器以获取电量信息
        registerReceiver(broadcastReceiver, intentFilter);
    }

    private void setWifi(){
        WifiInfo info = mWifiManager.getConnectionInfo();
        String wifiId = info != null ? info.getSSID() : null;
        if (mWifiManager.isWifiEnabled()) {
            wifiText.setText(wifiId);
        }else{
            wifiText.setText("Wifi is Close");
        }
    }

    private void setView(){
        //ring_sound.setText(currentVolume+"");
        seekBar.setProgress(currentVolume);
    }

    public static String readFile(String sys_path) {
        String prop = "waiting";// 默认值
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(sys_path));
            prop = reader.readLine();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if(reader != null){
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return prop;
    }

    private void writeSysFile(String path, String value) {
        try {
            BufferedWriter bufWriter = null;
            bufWriter = new BufferedWriter(new FileWriter(path));
            bufWriter.write(value + " ");
            bufWriter.flush();
            bufWriter.close();
            Log.d("qopqop", "write otg_power: " + value);
        } catch (IOException e) {
            Log.e("qopqop","error= "+ Log.getStackTraceString(e));
        }
    }

    /*public static void writeSysFile(String sys_path,boolean flag){
        Process p = null;
        DataOutputStream os = null;
        try {
            p = Runtime.getRuntime().exec("sh");
            os = new DataOutputStream(p.getOutputStream());
            if (flag) {
                os.writeBytes("echo 1 > "+sys_path + "\n");
            }else{
                os.writeBytes("echo 0 > "+sys_path + "\n");
            }
            os.writeBytes("exit\n");
            os.flush();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if(p != null)  { p.destroy(); }
            if(os != null){
                try {
                    os.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }*/

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                if(--currentVolume>=0) {
                    setView();
                }else {
                    currentVolume = 0;
                }
                return true;
            case KeyEvent.KEYCODE_VOLUME_UP:
                if(++currentVolume<=maxVolume){
                    setView();
                }else{
                    currentVolume = maxVolume;
                }
                return true;
            case KeyEvent.KEYCODE_VOLUME_MUTE:
                setView();
                return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    public void wifiClick(View view) {
        startActivity(new Intent(mContext, WifiActivity.class));//hold
    }

    public void dataClick(View view) {
        try {
            boolean data =getDataEnabled(0, mContext);
            Log.e("qopqop","data="+data);
            setDataEnabled(0,!data,mContext);
            if (!data){
                dataText.setText("4G");
            }else {
                dataText.setText("No net");
            }
        }catch (Exception e)
        {
            e.printStackTrace();
        }

    }

    public void batteryClick(View view) {
        //textView.setText((batteryLevel * 100 / batteryScale) + " % ");
    }

    public void cameraClick(View view) {
        startActivity(new Intent(mContext, CameraActivity.class));//hold
        //Intent intent = new Intent();
        //intent.setClassName("com.mediatek.camera", "com.mediatek.camera.CameraActivity");
        //startActivity(intent);
    }

    public void otgClick1(View view) {
        //String path = readFile(otgPath);
        //Log.e("qopqop","------path="+path);
        //otgText.setText(path);
        writeSysFile(otgPath,"0");
    }

    public void otgClick2(View view) {
        //String path = readFile(otgPath);
        //Log.e("qopqop","------path="+path);
        //otgText.setText(path);
        writeSysFile(otgPath,"1");
    }


    public void setDataEnabled(int slotIdx, boolean enable,Context context) throws Exception
    {
        //Log.e("qopqop","enable="+enable);
        //add 3.15 动态权限申请
        /*int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE);

        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_PHONE_STATE}, REQUEST_READ_PHONE_STATE);
        } else {
            //TODO
        }*/
        try {
            //int subid = SubscriptionManager.from(context).getActiveSubscriptionInfoForSimSlotIndex(slotIdx).getSubscriptionId();
            TelephonyManager telephonyService = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            //Method setDataEnabled = telephonyService.getClass().getDeclaredMethod("setDataEnabled", int.class, boolean.class);
            Method setDataEnabled = telephonyService.getClass().getDeclaredMethod("setDataEnabled", boolean.class);
            if (null != setDataEnabled) {
                //setDataEnabled.invoke(telephonyService, subid, enable);
                setDataEnabled.invoke(telephonyService, enable);
            }
        }catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public boolean getDataEnabled(int slotIdx,Context context) throws Exception
    {
        boolean enabled = false;
        //add 3.15 动态权限申请
        int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE);

        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            Log.e("qopqop","1111111111111");
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_PHONE_STATE}, REQUEST_READ_PHONE_STATE);
        } else {
            //TODO
            try {
                Log.e("qopqop","2222222222222");
                //int subid = SubscriptionManager.from(context).getActiveSubscriptionInfoForSimSlotIndex(slotIdx).getSubscriptionId();
                Log.e("qopqop","3333333333333");
                TelephonyManager telephonyService = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
                Log.e("qopqop","44444444444");
                //Method getDataEnabled = telephonyService.getClass().getDeclaredMethod("getDataEnabled", int.class);
                Method getDataEnabled = telephonyService.getClass().getDeclaredMethod("getDataEnabled");
                Log.e("qopqop","55555555555");
                Log.e("qopqop","getDataEnabled="+getDataEnabled);
                if (null != getDataEnabled) {
                    //enabled =  (Boolean) getDataEnabled.invoke(telephonyService, subid);
                    enabled =  (Boolean) getDataEnabled.invoke(telephonyService);
                    Log.e("qopqop","getDataEnabled----enabled="+enabled);
                }
            }catch (Exception e)
            {
                e.printStackTrace();
            }
        }
        /*try {
            int subid = SubscriptionManager.from(context).getActiveSubscriptionInfoForSimSlotIndex(slotIdx).getSubscriptionId();
            TelephonyManager telephonyService = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            Method getDataEnabled = telephonyService.getClass().getDeclaredMethod("getDataEnabled", int.class);
            if (null != getDataEnabled) {
                enabled =  (Boolean) getDataEnabled.invoke(telephonyService, subid);
                Log.e("qopqop","getDataEnabled----enabled="+enabled);
            }
        }catch (Exception e)
        {
            e.printStackTrace();
        }*/

        return enabled;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case REQUEST_READ_PHONE_STATE:
                if ((grantResults.length > 0) && (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    //TODO
                    Log.e("qopqop","Perssion is GRANTED");
                }
                break;

            default:
                break;
        }
    }
    @Override
    public void onResume() {
        super.onResume();
        setWifi();
    }

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent) {
//获取当前电量，如未获取具体数值，则默认为0
        batteryLevel = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
//获取最大电量，如未获取到具体数值，则默认为100
        batteryScale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
        Log.e("qopqop","batteryLevel="+batteryLevel);
//显示电量
        textView.setText((batteryLevel * 100 / batteryScale) + " % ");
         }
    };
}
