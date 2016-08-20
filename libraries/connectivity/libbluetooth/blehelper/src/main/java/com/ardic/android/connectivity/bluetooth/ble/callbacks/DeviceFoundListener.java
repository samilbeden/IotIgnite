package com.ardic.android.connectivity.bluetooth.ble.callbacks;

import android.bluetooth.BluetoothDevice;

/**
 * Created by yavuz.erzurumlu.
 */
public interface DeviceFoundListener {
    void onDeviceFound(BluetoothDevice device, int rssi, byte[] scanRecord);
}
