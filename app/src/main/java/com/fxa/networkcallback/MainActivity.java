package com.fxa.networkcallback;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorListener;
import android.hardware.SensorManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.telephony.TelephonyManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    String tag = "fxa";

    private boolean isVpnConnected(ConnectivityManager connectivityManager,Network network){
        NetworkCapabilities networkCapabilities = connectivityManager.getNetworkCapabilities(network);
        return networkCapabilities!= null && networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN);
    }


    Set<String> net_types = new HashSet<>();

    Map<String,String> net_cache = new HashMap<>();

    private boolean isConnected(){
        return net_types.size() > 0;
    }
    private String getConnectedType(){
        if(net_types.contains("wifi")){
            return "wifi";
        }
        if(net_types.contains("mobile")){
            return "mobile";
        }
        if(net_types.contains("eth")){
            return "eth";
        }
        return "none";
    }

    private final Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(@NonNull Message msg) {
            if (msg.what == 0) {
                String mobileType = "";
                if(connectivityManager!=null){
                    NetworkInfo  info = connectivityManager.getActiveNetworkInfo();
                    if(info != null){
                        mobileType = covertNetType(info.getSubtype());
                    }
                }
                String type = getConnectedType();
                if("mobile".equals(type)){
                    type = type+"-"+mobileType;
                }
                Log.e(tag,"net connected->"+(isConnected()? "connected": "disconnected")+" type:"+type);

            }else if(msg.what == 1){
                Network activeNetwork = connectivityManager.getActiveNetwork();
                if(activeNetwork == null){
                    net_types.clear();
                    net_cache.clear();
                    Log.e(tag,"onLost 断网了");
                }else{
                    Log.e(tag,"onLost 还连着的网络->"+ net_cache.get(activeNetwork.toString()));
                }
                handler.removeMessages(0);
                handler.sendEmptyMessageDelayed(0,200);
            }

            return false;
        }
    });

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // 判断当前横竖屏状态
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            // 当前为横屏
            Log.e("fxa","横屏");

        } else if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            // 当前为竖屏
            Log.e("fxa","竖屏");
        }
    }

    private SensorManager sensorManager = null;

    ConnectivityManager connectivityManager;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        Sensor sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        //需要系统旋转开关打开，锁定方向则无效
//        OrientationDetector mOrientationDetector = new OrientationDetector(this);
//        mOrientationDetector.enable();
        sensorManager.registerListener(new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
                    // 获取旋转矩阵
                    float[] rotationMatrix = new float[9];
                    SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values);

                    // 获取旋转角度
                    float[] orientation = new float[3];
                    SensorManager.getOrientation(rotationMatrix, orientation);

                    // 计算x轴的旋转角度（单位：弧度）

                    float zRotationRadians = orientation[0];

                    float xRotationRadians = orientation[1];

                    float yRotationRadians = orientation[2];



                    // 将弧度转换为角度（单位：度）
                    float xRotationDegrees = (float) Math.toDegrees(xRotationRadians);
                    float yRotationDegrees = (float) Math.toDegrees(yRotationRadians);
                    float zRotationDegrees = (float) Math.toDegrees(zRotationRadians);
                    // 打印x轴的旋转角度
                    Log.d("MainActivity", "x轴旋转角度：" + xRotationDegrees+" y->"+yRotationDegrees+" z->"+zRotationDegrees);

                    if((yRotationDegrees < - 45 && yRotationDegrees >-90) &&(xRotationDegrees < 45 && xRotationDegrees > -45)){
                        Log.e("MainActivity","左");
                        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                    }else if ((yRotationDegrees > 45 && yRotationDegrees < 90) && (xRotationDegrees < 45 && xRotationDegrees > -45) ){
                        Log.e("MainActivity","右");
                        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
                    } else if(xRotationDegrees < -45 && xRotationDegrees >-90 ){
                        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                    }else if(xRotationDegrees > 45 && xRotationDegrees < 90){
                        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT);
                    }

                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {

            }
        }, sensor, SensorManager.SENSOR_DELAY_NORMAL);










        connectivityManager  = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        connectivityManager.registerDefaultNetworkCallback(new ConnectivityManager.NetworkCallback(){


            @Override
            public void onAvailable(@NonNull Network network) {
                super.onAvailable(network);
                Log.e(tag,"onAvailable->"+network);
                NetworkCapabilities networkCapabilities = connectivityManager.getNetworkCapabilities(network);
                if(networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)){
                    Log.e(tag,"onAvailable wifi");
                    net_types.add("wifi");
                    net_cache.put(network.toString(),"wifi");
                }
                else if(networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)){
                    Log.e(tag,"onAvailable mobile");
                    net_types.add("mobile");
                    net_cache.put(network.toString(),"mobile");
                }
                else if(networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)){
                    Log.e(tag,"onAvailable eth");
                    net_types.add("eth");
                    net_cache.put(network.toString(),"eth");
                }
                handler.removeMessages(0);
                handler.sendEmptyMessageDelayed(0,200);
            }


            @Override
            public void onCapabilitiesChanged(@NonNull Network network, @NonNull NetworkCapabilities networkCapabilities) {
                super.onCapabilitiesChanged(network, networkCapabilities);
                if(networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)){
                    if(networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)){
                        Log.e(tag,"onCapabilitiesChanged wifi");
                        net_types.add("wifi");
                        net_cache.put(network.toString(),"wifi");
                    }
                    else if(networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)){
                        Log.e(tag,"onCapabilitiesChanged mobile");
                        net_types.add("mobile");
                        net_cache.put(network.toString(),"mobile");
                    }
                    else if(networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)){
                        Log.e(tag,"onCapabilitiesChanged eth");
                        net_types.add("eth");
                        net_cache.put(network.toString(),"eth");
                    }
                    else{
                        Log.e(tag,"onCapabilitiesChanged other");
                        boolean vpn = isVpnConnected(connectivityManager,network);
                        if(vpn){
                            Log.e(tag,"onCapabilitiesChanged vpn connected");
                            net_types.clear();
                        }
                    }
                    handler.removeMessages(0);
                    handler.sendEmptyMessageDelayed(0,200);
                }
            }

            @Override
            public void onLost(@NonNull Network network) {
                super.onLost(network);
                String type = net_cache.get(network.toString());
                net_types.remove(type);
                Log.e(tag,"onLost->"+network+" "+type);
                //wifi 断开的时候 一定要检查移动数据的状态，因为wifi开启状态下，切换移动数据不会有任何回调
                handler.removeMessages(1);
                handler.sendEmptyMessageDelayed(1,500);

            }
        });
    }

    private String covertNetType(int networkType) {
        switch (networkType) {
            case TelephonyManager.NETWORK_TYPE_GPRS:
            case TelephonyManager.NETWORK_TYPE_EDGE:
            case TelephonyManager.NETWORK_TYPE_CDMA:
            case TelephonyManager.NETWORK_TYPE_IDEN:
            case TelephonyManager.NETWORK_TYPE_1xRTT:
                return "2g";

            case TelephonyManager.NETWORK_TYPE_UMTS:
            case TelephonyManager.NETWORK_TYPE_EVDO_0:
            case TelephonyManager.NETWORK_TYPE_EVDO_A:
            case TelephonyManager.NETWORK_TYPE_EVDO_B:
            case TelephonyManager.NETWORK_TYPE_HSDPA:
            case TelephonyManager.NETWORK_TYPE_HSUPA:
            case TelephonyManager.NETWORK_TYPE_HSPA:
            case TelephonyManager.NETWORK_TYPE_HSPAP:
            case TelephonyManager.NETWORK_TYPE_EHRPD:
                Log.e(tag,"3g");
                return "3g";

            case TelephonyManager.NETWORK_TYPE_LTE:
            case 19: // NETWORK_TYPE_LTE_CA
                Log.e(tag,"4g");
                return "4g";
            case 20: // NETWORK_TYPE_NR
                Log.e(tag,"5g");
                return "5g";
            case TelephonyManager.NETWORK_TYPE_UNKNOWN:
            default:
                Log.e(tag,"unknown");
                return "unknown";
        }
    }


}