
package com.ardic.android.libbluetoothconnection;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.ardic.android.libbluetoothconnection.interfaces.MessagingInterface;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Message;
import android.util.Log;

public class BluetoothScanThread extends Thread {

    public enum BLUETOOTH_STATES {
        BLUETOOTH_OPEN_MODE, SCAN_MODE, WAITING_FOR_FINISHED, SCAN_DONE
    };

    public static final int RESULT_CODE_SCAN_SUCCESS = -200;
    public static final int RESULT_CODE_SCAN_FAILED = -201;

    private static final String TAG = BluetoothScanThread.class.getSimpleName();
    private static final long INTERVAL_SCAN_THREAD_RUN = 500;
    private static final long WAIT_AFTER_BLUETOOTH_STATE_SET = 5000;
    private Context context;
    private MessagingInterface messagingInterface;
    private BLUETOOTH_STATES state = BLUETOOTH_STATES.BLUETOOTH_OPEN_MODE;
    private List<BluetoothDevice> btDeviceList;

    public BluetoothScanThread(Context context, MessagingInterface messagingInterface) {
        this.context = context;
        this.messagingInterface = messagingInterface;
        btDeviceList = new ArrayList<BluetoothDevice>();
    }

    @Override
    public void run() {
        super.run();

        if (!BluetoothUtil.isSupported() || context == null) {
            if (messagingInterface != null) {
                messagingInterface.onMessageReceived(Message.obtain(null, RESULT_CODE_SCAN_FAILED));
            }
            return;
        }

        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);

        try {
            context.registerReceiver(actionFoundReceiver, filter);
        } catch (IllegalArgumentException e) {
            // already registered
            Log.d(TAG, "Error: " + e);
        }

        while (!isInterrupted()) {

            switch (state) {
                case BLUETOOTH_OPEN_MODE:
                    if (BluetoothUtil.isEnabled()) {
                        setState(BLUETOOTH_STATES.SCAN_MODE);
                    } else {
                        BluetoothUtil.setEnable(true);
                        try {
                            Thread.sleep(WAIT_AFTER_BLUETOOTH_STATE_SET);
                        } catch (InterruptedException e) {
                            break;
                        }
                    }
                    break;
                case SCAN_MODE:
                    boolean start = BluetoothUtil.startScan();
                    setState(BLUETOOTH_STATES.WAITING_FOR_FINISHED);
                    if (!start) {
                        if (messagingInterface != null) {
                            messagingInterface.onMessageReceived(Message.obtain(null,
                                    RESULT_CODE_SCAN_FAILED));
                        }
                    }
                    break;
                case WAITING_FOR_FINISHED:
                    break;
                case SCAN_DONE:
                    /*
                     * Don't remove sleep code below. If you remove, discovery
                     * started never called at next step if started again
                     * immediately
                     */
                    try {
                        sleep(2000);
                    } catch (InterruptedException e) {
                        break;
                    }
                    if (messagingInterface != null) {
                        messagingInterface.onMessageReceived(Message.obtain(null,
                                RESULT_CODE_SCAN_SUCCESS));
                    }
                    break;
                default:
                    Log.d(TAG, "Warning default state");
                    break;
            }

            try {
                sleep(INTERVAL_SCAN_THREAD_RUN);
            } catch (InterruptedException e) {
                break;
            }
        }

        try {
            context.unregisterReceiver(actionFoundReceiver);
        } catch (IllegalArgumentException e) {
            // already unregistered
        }
        if (BluetoothUtil.isScanning()) {
            BluetoothUtil.stopScan();
        }
    }

    public List<BluetoothDevice> getBluetoothDeviceList() {
        return btDeviceList;
    }

    private void setState(BLUETOOTH_STATES state) {
        this.state = state;
        Log.d(TAG, "State: " + state);
    }

    private final BroadcastReceiver actionFoundReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                Log.d(TAG, "Device: " + device.getName() + ", " + device);
                btDeviceList.add(device);
            } else if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
                Log.d(TAG, "Discovery started");
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                // out.append("\nDiscovery Finished");
                Iterator<BluetoothDevice> itr = btDeviceList.iterator();
                while (itr.hasNext()) {
                    // Get Services for paired devices
                    BluetoothDevice device = itr.next();
                    Log.d(TAG, "Getting Services for " + device.getName() + ", " + device);
                    if (!device.fetchUuidsWithSdp()) {
                        Log.d(TAG, "SDP Failed for " + device.getName());
                    }
                }
                setState(BLUETOOTH_STATES.SCAN_DONE);
            }
        }
    };
}
