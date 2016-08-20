package com.ardic.android.sampleiotigniteprojectwithdynamicconfigurations;

import android.net.NetworkInfo;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.os.Handler;

import com.ardic.android.connectivity.libwirelessconnection.WifiHelper;
import com.ardic.android.connectivity.libwirelessconnection.listeners.SimpleWifiConnectionListener;
import com.ardic.android.libnsd.nsdhelper.NetworkServiceDiscovery;
import com.ardic.android.libnsd.nsdhelper.ServiceDiscoveryResolverListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;


public class MainActivity extends AppCompatActivity{

    private static final String TAG ="TestApp";
    // Ignite Variables //
    private static final String SERVICE_TYPE="_esp8266._tcp.";
    private static List<String> espServiceList = new ArrayList<String>();
    private static List<ESP8266NodeHandler> espInstances = new ArrayList<ESP8266NodeHandler>();
    private static final long ESP_HANDLER_PERIOD = 30000L;
    private static final long ESP_HANDLER_DELAY = 40000L;

    private WifiHelper mWifiHelper;

    private NetworkServiceDiscovery discoveryHelper;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.i(TAG,"Application started...");


        discoveryHelper = new NetworkServiceDiscovery(getApplicationContext());

        discoveryHelper.initializeNsd();
        discoveryHelper.discoverServices(SERVICE_TYPE);
        discoveryHelper.setServiceResolverListener(new ServiceDiscoveryResolverListener() {
            @Override
            public void onNewServiceResolved(NetworkServiceDiscovery.NSDService serviceInfo) {

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

        mWifiHelper = WifiHelper.WifiHelperFactory.create(getApplicationContext(), new SimpleWifiConnectionListener() {
            @Override
            public void onConnecting(NetworkInfo info) {
                Log.i(TAG,"Connecting...");
            }

            @Override
            public void onConnected(NetworkInfo info) {
                Log.i(TAG,"Connected.");
            }

            @Override
            public void onDisconnected(NetworkInfo info) {
                Log.i(TAG,"Disconnected");
            }

            @Override
            public void onDisconnecting(NetworkInfo info) {
                Log.i(TAG,"disconnecting...");
            }

            @Override
            public void onSuspended(NetworkInfo info) {
                Log.i(TAG,"Suspended...");
            }

            @Override
            public void onUnknown(NetworkInfo info) {
                Log.i(TAG,"Unknown...");
            }
        });

        Log.i(TAG ,"Wifi Enabled :"  + mWifiHelper.isWifiEnabled());



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

    }



    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(discoveryHelper!=null) {
            discoveryHelper.stopServiceDiscovery();
        }

        for(ESP8266NodeHandler esp : espInstances){
            esp.stop();
        }

    }

}
