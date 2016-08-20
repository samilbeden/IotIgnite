
package com.ardic.android.libnsd.nsdhelper;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import android.annotation.TargetApi;
import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.net.nsd.NsdManager.DiscoveryListener;
import android.os.Build;
import android.util.Log;
import com.ardic.android.libnsd.nsdhelper.NetworkServiceDiscovery;
import com.ardic.android.libnsd.nsdhelper.ServiceDiscoveryResolverListener;

/**
 * Using Android's NSD API implement listener service for bonjour devices. Lots of popular
 * microcontrollers (Arduino Yun,Edison) are using this service for broadcast their local IP on
 * network. It provides getting these devices IP by automatically. For more info :
 * http://developer.android.com/reference/android/net/nsd/NsdManager.html Nsd requires min API level
 * 16.
 * 
 * @author yavuz.erzurumlu
 */
@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
public class NetworkServiceDiscovery {

    private static final String TAG = NetworkServiceDiscovery.class.getSimpleName();
    private static final boolean DEBUG = false;
    private DiscoveryListener mDiscoveryListener;
    private NsdManager mNsdManager;
    private ServiceDiscoveryResolverListener mServiceResolverListener;
    private Context appContext;
    private List<NSDService> mNSDServiceList = new ArrayList<NSDService>();

    public NetworkServiceDiscovery(Context context) {

        appContext = context;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            mNsdManager = (NsdManager) appContext.getSystemService(Context.NSD_SERVICE);
        }

    }

    /**
     * initialize discovery listener for services.
     */

    private void initializeDiscoveryListener() {

        mDiscoveryListener = new DiscoveryListener() {

            @Override
            public void onStopDiscoveryFailed(String serviceType, int errorCode) {

                if (DEBUG) {

                    Log.i(TAG, "Stop Discovery Failed. Error Code : " + errorCode
                            + "\n Service Type : " + serviceType);
                }

            }

            @Override
            public void onStartDiscoveryFailed(String serviceType, int errorCode) {

                if (DEBUG) {

                    Log.i(TAG, "Start Discovery Failed. Error Code : " + errorCode
                            + "\n Service Type : " + serviceType);
                }

            }

            @Override
            public void onServiceLost(NsdServiceInfo serviceInfo) {
                // TODO Auto-generated method stub
                if (DEBUG) {
                    Log.i(TAG, "Service Lost!");
                    Log.i(TAG, "Service Name : " + serviceInfo.getServiceName());
                    Log.i(TAG, "Service Type : " + serviceInfo.getServiceType());
                    Log.i(TAG, "Host : " + serviceInfo.getHost());
                    Log.i(TAG, "Port : " + serviceInfo.getPort());
                }
                // Service is gone. Remove it from list.
                for (NSDService mService : mNSDServiceList) {

                    if (mService.host.equals(serviceInfo.getHost())) {

                        mNSDServiceList.remove(mService);

                        if (DEBUG) {
                            Log.i(TAG, "Service is removed from list.");
                        }

                    }
                }

            }

            @Override
            public void onServiceFound(NsdServiceInfo serviceInfo) {

                if (DEBUG) {
                    Log.i(TAG, "New Service Found!");
                    Log.i(TAG, "Service Name : " + serviceInfo.getServiceName());
                    Log.i(TAG, "Service Type : " + serviceInfo.getServiceType());
                    Log.i(TAG, "Host : " + serviceInfo.getHost());
                    Log.i(TAG, "Port : " + serviceInfo.getPort());
                }
                if (mNsdManager != null) {

                    mNsdManager.resolveService(serviceInfo, new NsdManager.ResolveListener() {

                        @Override
                        public void onServiceResolved(NsdServiceInfo serviceInfo) {

                            if (DEBUG) {
                                Log.i(TAG, "Service Successfully Resolved!");
                                Log.i(TAG, "Service Name : " + serviceInfo.getServiceName());
                                Log.i(TAG, "Service Type : " + serviceInfo.getServiceType());
                                Log.i(TAG, "Host : " + serviceInfo.getHost());
                                Log.i(TAG, "Port : " + serviceInfo.getPort());
                            }
                            // New service arrived. Add it to list.
                            NSDService mService = new NSDService(serviceInfo.getServiceName(),
                                    serviceInfo.getServiceType(), serviceInfo.getHost(),
                                    serviceInfo.getPort());

                            if (!mNSDServiceList.contains(mService)) {

                                if (DEBUG) {
                                    Log.i(TAG, "Service is new , adding to list.");
                                }
                                mNSDServiceList.add(mService);
                                mServiceResolverListener.onNewServiceResolved(mService);
                            } else {

                                if (DEBUG) {
                                    Log.i(TAG, "Service is already in list");
                                }
                            }

                        }

                        @Override
                        public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {

                            if (DEBUG) {
                                Log.i(TAG, "Service Resolve Failed!");
                                Log.i(TAG, "Service Name : " + serviceInfo.getServiceName());
                                Log.i(TAG, "Service Type : " + serviceInfo.getServiceType());
                                Log.i(TAG, "Host : " + serviceInfo.getHost());
                                Log.i(TAG, "Port : " + serviceInfo.getPort());
                            }
                            // Service resolve failed. Remove it from list.
                            for (NSDService mService : mNSDServiceList) {

                                if (mService.host.equals(serviceInfo.getHost())) {

                                    mNSDServiceList.remove(mService);

                                    if (DEBUG) {
                                        Log.i(TAG, "Service is removed from list.");
                                    }

                                }
                            }

                        }
                    });
                }

            }

            @Override
            public void onDiscoveryStopped(String serviceType) {

                if (DEBUG) {
                    Log.i(TAG, "Service Discovery Stopped! \n Service Type :" + serviceType);
                }
            }

            @Override
            public void onDiscoveryStarted(String serviceType) {

                if (DEBUG) {
                    Log.i(TAG, "Service Discovery Started! \n Service Type :" + serviceType);
                }

            }
        };
    }

    /**
     * starts discover services in given service type. Example service types:
     * EDISON_SERVICE="_xdk-app-daemon._tcp"; WORKSTATION_SERVICE="_workstation._tcp";
     * 
     * @param serviceType
     */

    public void discoverServices(String serviceType) {

        if (serviceType != null) {

            if (DEBUG) {
                Log.i(TAG, "Starting Discovery ! Service Type  : " + serviceType);
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {

                mNsdManager.discoverServices(serviceType, NsdManager.PROTOCOL_DNS_SD,
                        mDiscoveryListener);
            }

        }
    }

    public class NSDService {

        private String serviceName;
        private String serviceType;
        private InetAddress host;
        private int port;

        public NSDService(String serviceName, String serviceType, InetAddress host, int port) {

            this.serviceName = serviceName;
            this.serviceType = serviceType;
            this.host = host;
            this.port = port;
        }

        public NSDService() {

            this.serviceName = null;
            this.serviceType = null;
            this.host = null;
            this.port = 0;
        }

        public void setServiceName(String serviceName) {
            this.serviceName = serviceName;
        }

        public String getServiceName() {
            return this.serviceName;
        }

        public void setServiceType(String serviceType) {
            this.serviceType = serviceType;
        }

        public String getServiceType() {
            return this.serviceType;
        }

        public void setHost(InetAddress address) {
            this.host = address;
        }

        public InetAddress getHost() {
            return this.host;
        }

        public void setPort(int port) {
            this.port = port;
        }

        public int getPort() {
            return this.port;
        }

    }

    /**
     * returns service list. if there is no service found returns null.
     * 
     * @return
     */
    public List<NSDService> getServiceList() {

        if (mNSDServiceList.size() > 0) {
            if (DEBUG) {
                Log.i(TAG, "ServiceList > 0 \nList size :" + mNSDServiceList.size());
            }
            return mNSDServiceList;
        }
        if (DEBUG) {
            Log.i(TAG,
                    "ServiceList = 0 \n Returning NULL value. \nList Size : "
                            + mNSDServiceList.size());
        }
        return null;
    }

    /**
     * set service discovery resolver listener
     * 
     * @param listener
     */
    public void setServiceResolverListener(ServiceDiscoveryResolverListener listener) {

        if (DEBUG) {
            Log.i(TAG, "Setting service discovery resolver listener..");
        }
        mServiceResolverListener = listener;
    }

    /**
     * stop discovering services and unregister discovery listener.
     */

    public void stopServiceDiscovery() {

        if (mNsdManager != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            if (DEBUG) {
                Log.i(TAG, "Stopping service discovery..");
            }
            mNsdManager.stopServiceDiscovery(mDiscoveryListener);
        }
    }

    /**
     * initializes discovery listener.
     */
    public boolean initializeNsd() {

        if (mNsdManager != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            initializeDiscoveryListener();

            return true;
        }

        if (DEBUG) {
            Log.i(TAG, "Device API < 16 !!!!! To use this API SDK_INT must be >= 16");
        }
        return false;
    }

}
