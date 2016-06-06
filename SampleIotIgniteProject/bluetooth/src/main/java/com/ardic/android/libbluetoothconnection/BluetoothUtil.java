
package com.ardic.android.libbluetoothconnection;

import java.util.ArrayList;
import java.util.List;

import android.annotation.TargetApi;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

public final class BluetoothUtil {

    protected static final String TAG = BluetoothUtil.class.getSimpleName();
    private static boolean autoPairing = false;

    private BluetoothUtil() {
    }

    public static boolean isSupported() {
        return BluetoothAdapter.getDefaultAdapter() != null;
    }

    public static boolean setEnable(boolean enable) {

        if (isSupported()) {
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            boolean isEnabled = bluetoothAdapter.isEnabled();
            if (enable && !isEnabled) {
                return bluetoothAdapter.enable();
            } else if (!enable && isEnabled) {
                return bluetoothAdapter.disable();
            }

            // No need to change bluetooth state
            return true;
        }

        return false;
    }

    public static boolean isEnabled() {

        if (isSupported()) {
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            return bluetoothAdapter.isEnabled();
        }

        return false;
    }

    public static boolean setName(String name) {

        if (isSupported() && !TextUtils.isEmpty(name)) {
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            return bluetoothAdapter.setName(name);
        }

        return false;
    }

    public static String getName() {

        if (isSupported()) {
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            return bluetoothAdapter.getName();
        }

        return "";
    }

    public static boolean isAutoPairingEnabled() {
        return isSupported() && autoPairing;
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    public static boolean setAutoPairingEnable(boolean enable, Context context) {

        if (isSupported() && context != null) {
            if (enable && !autoPairing) {
                IntentFilter intentBluetooth = new IntentFilter();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    intentBluetooth.addAction(BluetoothDevice.ACTION_PAIRING_REQUEST);
                }
                intentBluetooth.addAction(BluetoothDevice.ACTION_FOUND);
                try {
                    context.registerReceiver(bluetoothBroadcastReceiver, intentBluetooth);
                    autoPairing = true;
                } catch (IllegalStateException e) {
                    // Already registered
                }
            } else if (!enable && autoPairing) {
                try {
                    context.unregisterReceiver(bluetoothBroadcastReceiver);
                    autoPairing = false;
                } catch (IllegalStateException e) {
                    // Already unregistered
                }
            }

            return true;
        }

        return false;
    }

    private final static BroadcastReceiver bluetoothBroadcastReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            // get device bluetooth mac id
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                if (action.equals(BluetoothDevice.ACTION_PAIRING_REQUEST)) {
                    // confirm pairing request dialog
                    setConfirmationDialog(device);
                } else if (action.equals(BluetoothDevice.ACTION_FOUND)) {
                    startPairing(device);
                }
            }
            Log.d(TAG, "action: " + action);
        }
    };

    @TargetApi(Build.VERSION_CODES.KITKAT)
    private static void setConfirmationDialog(BluetoothDevice device) {
        boolean result = device.setPairingConfirmation(true);
        Log.d(TAG, "setPairingConfirmation: " + result);
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    private static void startPairing(BluetoothDevice device) {
        boolean result = device.createBond();
        Log.d(TAG, "startPairing: " + result);
    }

    public static String getAddress() {

        if (isSupported()) {
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            return bluetoothAdapter.getAddress();
        }

        return "";
    }

    public static boolean makeDiscoverable(Activity activity, int requestCode) {

        if (isSupported() && activity != null) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            enableBtIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 0);
            activity.startActivityForResult(enableBtIntent, requestCode);
        }

        return false;
    }

    public static boolean makeDiscoverable(Context context) {

        if (isSupported() && context != null) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            enableBtIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 0);
            enableBtIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(enableBtIntent);
        }

        return false;
    }

    public static boolean isDiscoverable() {

        if (isSupported()) {
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            return bluetoothAdapter.getScanMode() == BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE;
        }

        return false;
    }

    public static boolean startScan() {

        if (isSupported()) {
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            return bluetoothAdapter.startDiscovery();
        }

        return false;
    }

    public static boolean stopScan() {

        if (isSupported()) {
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            return bluetoothAdapter.startDiscovery();
        }

        return false;
    }

    public static boolean isScanning() {

        if (isSupported()) {
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            return bluetoothAdapter.isDiscovering();
        }

        return false;
    }

    public static List<String> getOnlyNameFromScanResults(List<BluetoothDevice> scanResults) {

        List<String> ssids = new ArrayList<String>();

        if (scanResults != null) {
            for (BluetoothDevice result : scanResults) {
                ssids.add(result.getName());
            }
        }

        return ssids;
    }
}
