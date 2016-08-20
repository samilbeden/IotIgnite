package com.ardic.android.connectivity.bluetooth.ble.callbacks;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.os.Build;

import java.util.List;

/**
 * Created by yavuz.erzurumlu.
 */
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class ScanResultCallback {

    private  DeviceFoundListener listener;

    private BluetoothAdapter.LeScanCallback mBleScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
            if(listener!=null) {
                listener.onDeviceFound(device,rssi,scanRecord);
            }
        }
    };

    private ScanCallback mScanCallback = new ScanCallback() {
        /**
         * Callback when a BLE advertisement has been found.
         *
         * @param callbackType Determines how this callback was triggered. Could be one of
         *                     {@link ScanSettings#CALLBACK_TYPE_ALL_MATCHES},
         *                     {@link ScanSettings#CALLBACK_TYPE_FIRST_MATCH} or
         *                     {@link ScanSettings#CALLBACK_TYPE_MATCH_LOST}
         * @param result       A Bluetooth LE scan result.
         */
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            if(listener!=null) {
                listener.onDeviceFound(result.getDevice(), result.getRssi() , result.getScanRecord().getBytes());
            }
        }

        /**
         * Callback when batch results are delivered.
         *
         * @param results List of scan results that are previously scanned.
         */
        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);

            if(listener!=null) {
                for (ScanResult r : results) {

                    listener.onDeviceFound(r.getDevice(),r.getRssi(), r.getScanRecord().getBytes());

                }
            }
        }

        /**
         * Callback when scan could not be started.
         *
         * @param errorCode Error code (one of SCAN_FAILED_*) for scan failure.
         */
        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
        }
    };

    public ScanResultCallback(DeviceFoundListener listener){
    this.listener = listener;
    }

    public ScanCallback getScanCallback(){
        return this.mScanCallback;
    }

    public BluetoothAdapter.LeScanCallback getBleScanCallback(){
        return this.mBleScanCallback;
    }

    public void setOnDeviceFoundListener(DeviceFoundListener listener){
        this.listener = listener;
    }
}
