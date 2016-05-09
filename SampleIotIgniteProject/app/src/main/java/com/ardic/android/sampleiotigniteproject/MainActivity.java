package com.ardic.android.sampleiotigniteproject;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.net.wifi.WifiManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.os.Handler;

import java.io.IOException;
import java.sql.Time;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;


public class MainActivity extends AppCompatActivity implements NsdManager.DiscoveryListener{

    private static final String TAG ="TestApp";
    // Ignite Variables //
    private NsdManager mNsdManager;
    private static final String SERVICE_TYPE="_esp8266._tcp.";
    private static List<String> espServiceList = new ArrayList<String>();
    private static List<ESP8266NodeHandler> espInstances = new ArrayList<ESP8266NodeHandler>();
    private static final long ESP_HANDLER_PERIOD = 30000L;
    private static final long ESP_HANDLER_DELAY = 40000L;

    private BroadcastReceiver wifiReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            ConnectivityManager conMan = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo netInfo = conMan.getActiveNetworkInfo();
            if (netInfo != null && netInfo.getType() == ConnectivityManager.TYPE_WIFI) {
                Log.i(TAG, "Wifi Active.");
                // handle nsd if wifi changes...
            }
            else {
                Log.i(TAG, "Don't have Wifi Connection");
            }
        }
    };

    private IntentFilter mIntentFilter = new IntentFilter();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.i(TAG,"Application started...");
        mNsdManager = (NsdManager) getSystemService(Context.NSD_SERVICE);
        mNsdManager.discoverServices(
                SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, this);


        mIntentFilter.addAction("android.net.wifi.WIFI_STATE_CHANGED");
        mIntentFilter.addAction("android.net.wifi.STATE_CHANGE");


        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {

                for(ESP8266NodeHandler esp : espInstances){
                   if(!esp.isRunning()){
                       esp.start();
                       Log.i(TAG,"ESP : " + esp.getIpAndPort() + " is started again.");
                   }
                }
            }
        },ESP_HANDLER_DELAY,ESP_HANDLER_PERIOD);


        registerReceiver(wifiReceiver,mIntentFilter);
    }



    @Override
    protected void onDestroy() {
        super.onDestroy();
        mNsdManager.stopServiceDiscovery(this);


        for(ESP8266NodeHandler esp : espInstances){
            esp.stop();
        }
        unregisterReceiver(wifiReceiver);

    }


    @Override
    public void onStartDiscoveryFailed(String serviceType, int errorCode) {

    }

    @Override
    public void onStopDiscoveryFailed(String serviceType, int errorCode) {

    }

    @Override
    public void onDiscoveryStarted(String serviceType) {

        Log.i(TAG,"Service discovery started...");
    }

    @Override
    public void onDiscoveryStopped(String serviceType) {
        Log.i(TAG,"Service discovery stopped...");
    }

    @Override
    public void onServiceFound(NsdServiceInfo serviceInfo) {
        Log.i(TAG,"New service found!");


        mNsdManager.resolveService(serviceInfo, new NsdManager.ResolveListener() {
            @Override
            public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
                Log.i(TAG,"New service resolve failed!");
            }

            @Override
            public void onServiceResolved(NsdServiceInfo serviceInfo) {

                Log.i(TAG,"New service resolved!");
                Log.i(TAG,"Raw  :  " + serviceInfo.toString());
                String espIP = serviceInfo.getHost().toString().substring(1,serviceInfo.getHost().toString().length());
                int port = serviceInfo.getPort();

                Log.i(TAG,"IP&PORT  :  " + espIP + ":"+port);
                if(!espServiceList.contains(espIP)) {
                    final ESP8266NodeHandler mEspHandler = new ESP8266NodeHandler(espIP, port, getApplicationContext());
                    espInstances.add(mEspHandler);
                    espServiceList.add(espIP);
                    Log.i(TAG,"Total founded esp8266 : " +espServiceList.size() );
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {

                                Log.i(TAG,"Instance Size : " + espInstances.size());
                                new Handler().postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        mEspHandler.start();
                                        Log.i(TAG,"STARTING ESP " + mEspHandler.getIpAndPort());
                                    }
                                },(5000*espInstances.indexOf(mEspHandler)*2));


                        }
                    },1000);
                }else{
                    int loc = 0;

                    for(ESP8266NodeHandler e : espInstances){
                        if(e.getIpAndPort().equals(espIP+" : "+port)){
                            loc = espInstances.indexOf(e);
                            break;
                        }
                    }
                    final ESP8266NodeHandler esp = espInstances.get(loc);

                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {

                            Log.i(TAG,"Instance Size : " + espInstances.size());
                            new Handler().postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    esp.start();
                                    Log.i(TAG,"STARTING ESP " + esp.getIpAndPort());
                                }
                            },(5000*espInstances.indexOf(esp)*2));


                        }
                    },1000);
                }
            }
        });
    }

    @Override
    public void onServiceLost(NsdServiceInfo serviceInfo) {

    }

}
