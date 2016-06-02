package com.ardic.android.connectivity.libwirelessconnection;

import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;

import java.util.List;

/**
 * Created by Yavuz Erzurumlu.
 */
public class WifiHelper {

    private Context appContext;
    private WifiManager mWifiManager;

    private WifiHelper(Context appContext){
        mWifiManager = (WifiManager) appContext.getSystemService(Context.WIFI_SERVICE);
   }

    public boolean isWifiEnabled(){
        return mWifiManager.isWifiEnabled();
    }

    public boolean setWifiEnabled(boolean enabled){
        return mWifiManager.setWifiEnabled(enabled);
    }

    public boolean connectToWEPNetwork(String ssid, String password, boolean isHidden){

        WifiConfiguration mWifiConf = new WifiConfiguration();
        mWifiConf.hiddenSSID = isHidden;
        mWifiConf.SSID = ssid;
        mWifiConf.wepKeys[0] = "\"" + password + "\"";
        mWifiConf.wepTxKeyIndex = 0;
        mWifiConf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
        mWifiConf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);

        mWifiManager.addNetwork(mWifiConf);
        return  connectToConfiguredSSID(ssid);
    }

    public boolean connectToWPANetwork(String ssid,String password, boolean isHidden){
        WifiConfiguration mWifiConf = new WifiConfiguration();
        mWifiConf.hiddenSSID = isHidden;
        mWifiConf.SSID = ssid;
        mWifiConf.preSharedKey = "\""+ password +"\"";

        mWifiManager.addNetwork(mWifiConf);
        return connectToConfiguredSSID(ssid);
    }

    public boolean connectToOpenNetwork(String ssid,String password, boolean isHidden){
        WifiConfiguration mWifiConf = new WifiConfiguration();
        mWifiConf.hiddenSSID = isHidden;
        mWifiConf.SSID = ssid;
        mWifiConf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
        mWifiManager.addNetwork(mWifiConf);
        return connectToConfiguredSSID(ssid);
    }

    private boolean connectToConfiguredSSID(String ssid){
        List<WifiConfiguration> list = mWifiManager.getConfiguredNetworks();
        for( WifiConfiguration i : list ) {
            if(i.SSID != null && i.SSID.equals("\"" + ssid + "\"")) {
                mWifiManager.disconnect();
                mWifiManager.enableNetwork(i.networkId, true);
                return mWifiManager.reconnect();
            }
        }
        return false;
    }

    public boolean disconnect(){
       return  mWifiManager.disconnect();
    }
    /**
     * Factory class for create instance for WifiHelper
     */
    public static class WifiHelperFactory {

        public static WifiHelper create(Context appContext) throws IllegalArgumentException{
            if(appContext!=null) {
                return  new WifiHelper(appContext);
            }else{
                throw new IllegalArgumentException("Application Context is NULL.");
            }
        };
    }
}
