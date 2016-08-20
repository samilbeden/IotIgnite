package com.ardic.android.connectivity.bluetooth.ble;

import android.annotation.TargetApi;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import com.ardic.android.connectivity.bluetooth.ble.callbacks.DeviceFoundListener;
import com.ardic.android.connectivity.bluetooth.ble.callbacks.ScanResultCallback;
import com.ardic.android.connectivity.bluetooth.ble.exceptions.BLENotSupportedException;

import java.util.List;

/**
 * Created by yavuz.erzurumlu.
 */
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class BLEScanHelper {

    private ScanResultCallback mScanResultCallback;
    private static final String TAG =BLEScanHelper.class.getSimpleName() ;
    private Context appContext;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothManager mBluetoothManager;
    private static final int REQUEST_ENABLE_BT = 1;


    private BLEScanHelper(Context mContext){
        this.appContext = mContext;
        this.mScanResultCallback = new ScanResultCallback(null);
    }

    // Use this check to determine whether BLE is supported on the device.
    private boolean isBLESupported(){

        if (!appContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            return false;
        }else {
            return true;
        }

    }

    private void init(){
        mBluetoothManager = (BluetoothManager) appContext.getSystemService(Context.BLUETOOTH_SERVICE);
        if(mBluetoothManager!=null) {
            mBluetoothAdapter = mBluetoothManager.getAdapter();
        }
    }

    public boolean isBluetoothEnabled(){
        return mBluetoothAdapter.isEnabled();
    }

    /**
     *
     * In app you must override onActivityResult for receive users choice.The code below checks user's choice. If user does not open
     * bluetooth than closes activity.Example code below :
     *
     *
     *            <p>@Override<br>
     *        protected void onActivityResult(int requestCode, int resultCode, Intent data) {<br>
     *              // User choose not to enable Bluetooth.<br>
     *              if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {<br>
     *                 finish();<br>
     *                 return;<br>
     *              }<br>
     *          super.onActivityResult(requestCode, resultCode, data);<br>
     *        }</p>
     *
     *
     *  
     * @param mActivity
     */
    public void openBluetooth(Activity mActivity){

        if(!isBluetoothEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            mActivity.startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
    }


    public void scanBluetoothLEDevices(DeviceFoundListener deviceListener){

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mBluetoothAdapter.getBluetoothLeScanner().startScan(mScanResultCallback.getScanCallback());
            mScanResultCallback.setOnDeviceFoundListener(deviceListener);
        }else{
            mBluetoothAdapter.startLeScan(mScanResultCallback.getBleScanCallback());
        }
    }


    public void scanBluetoothLEDevicesWithSpecialSettings(List<ScanFilter> filter, ScanSettings settings,DeviceFoundListener deviceListener){

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mBluetoothAdapter.getBluetoothLeScanner().startScan(filter, settings, mScanResultCallback.getScanCallback());
            mScanResultCallback.setOnDeviceFoundListener(deviceListener);
        }

    }

    public void stopScanningBLEDevices(){

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mBluetoothAdapter.getBluetoothLeScanner().stopScan(mScanResultCallback.getScanCallback());
        }else {

            mBluetoothAdapter.stopLeScan(mScanResultCallback.getBleScanCallback());
        }
    }






    public static class BLEHelperFactory{

        private static BLEScanHelper instance;

        public static BLEScanHelper create(Context mContext) throws NullPointerException,BLENotSupportedException{

            if(mContext!=null) {
                instance = new BLEScanHelper(mContext);

                if(instance.isBLESupported()){
                    instance.init();
                    return  instance;
                }else{
                    throw  new BLENotSupportedException();
                }
            }else{
                throw new NullPointerException("Context is NULL");
            }
        }
    }
}
