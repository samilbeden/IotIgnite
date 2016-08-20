package com.ardic.android.genuino.ble.ignite;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.content.Intent;
import android.os.Handler;
import android.support.v4.text.TextUtilsCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;

import com.ardic.android.connectivity.bluetooth.ble.BLEScanHelper;
import com.ardic.android.connectivity.bluetooth.ble.callbacks.DeviceFoundListener;
import com.ardic.android.connectivity.bluetooth.ble.exceptions.BLENotSupportedException;
import com.ardic.android.iotignite.callbacks.ConnectionCallback;
import com.ardic.android.iotignite.enumerations.NodeType;
import com.ardic.android.iotignite.enumerations.ThingCategory;
import com.ardic.android.iotignite.enumerations.ThingDataType;
import com.ardic.android.iotignite.listeners.ThingListener;
import com.ardic.android.iotignite.nodes.IotIgniteManager;
import com.ardic.android.iotignite.nodes.Node;
import com.ardic.android.iotignite.things.Thing;
import com.ardic.android.iotignite.things.ThingActionData;
import com.ardic.android.iotignite.things.ThingData;
import com.ardic.android.iotignite.things.ThingType;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName() ;
    private static final String LED="LED";
    private static final String APP_KEY ="YOUR_APP_KEY_HERE";
    private static final String LED_GATT_CHARACTERISTIC = "19B10001-E8F2-537E-4F6C-D104768A1214";
    private static final String LED_GATT_SERVICE ="19B10000-E8F2-537E-4F6C-D104768A1214";
    private BLEScanHelper helper;
    private BluetoothDevice mDevice;
    private BluetoothGatt ledGatt;
    private BluetoothGattService ledService;
    private BluetoothGattCharacteristic ledCharacteristic;
    private BluetoothGattCallback mCallBack = new BluetoothGattCallback() {
        /**
         * Callback indicating when GATT client has connected/disconnected to/from a remote
         * GATT server.
         *
         * @param gatt     GATT client
         * @param status   Status of the connect or disconnect operation.
         *                 {@link BluetoothGatt#GATT_SUCCESS} if the operation succeeds.
         * @param newState Returns the new connection state. Can be one of
         *                 {@link //BluetoothProfile#STATE_DISCONNECTED} or
         *                 {@link //BluetoothProfile#STATE_CONNECTED}
         */
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            if(status == BluetoothGatt.GATT_SUCCESS){
                Log.i(TAG,"GATT Connected");
                ledGatt = gatt;
                ledGatt.discoverServices();
            }
        }

        /**
         * Callback invoked when the list of remote services, characteristics and descriptors
         * for the remote device have been updated, ie new services have been discovered.
         *
         * @param gatt   GATT client invoked {@link BluetoothGatt#discoverServices}
         * @param status {@link BluetoothGatt#GATT_SUCCESS} if the remote device
         */
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);

            ledService = gatt.getService(UUID.fromString(LED_GATT_SERVICE));

            ledCharacteristic = ledService.getCharacteristic(UUID.fromString(LED_GATT_CHARACTERISTIC));

            Log.i(TAG,"Led Characteristic setted.");

        }

        /**
         * Callback reporting the result of a characteristic read operation.
         *
         * @param gatt           GATT client invoked {@link BluetoothGatt#readCharacteristic}
         * @param characteristic Characteristic that was read from the associated
         *                       remote device.
         * @param status         {@link BluetoothGatt#GATT_SUCCESS} if the read operation
         */
        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
            
        }

        /**
         * Callback indicating the result of a characteristic write operation.
         * <p/>
         * <p>If this callback is invoked while a reliable write transaction is
         * in progress, the value of the characteristic represents the value
         * reported by the remote device. An application should compare this
         * value to the desired value to be written. If the values don't match,
         * the application must abort the reliable write transaction.
         *
         * @param gatt           GATT client invoked {@link BluetoothGatt#writeCharacteristic}
         * @param characteristic Characteristic that was written to the associated
         *                       remote device.
         * @param status         The result of the write operation
         *                       {@link BluetoothGatt#GATT_SUCCESS} if the operation succeeds.
         */
        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
        }

        /**
         * Callback triggered as a result of a remote characteristic notification.
         *
         * @param gatt           GATT client the characteristic is associated with
         * @param characteristic Characteristic that has been updated as a result
         */
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
        }

        /**
         * Callback reporting the result of a descriptor read operation.
         *
         * @param gatt       GATT client invoked {@link BluetoothGatt#readDescriptor}
         * @param descriptor Descriptor that was read from the associated
         *                   remote device.
         * @param status     {@link BluetoothGatt#GATT_SUCCESS} if the read operation
         */
        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorRead(gatt, descriptor, status);
        }

        /**
         * Callback invoked when a reliable write transaction has been completed.
         *
         * @param gatt   GATT client invoked {@link BluetoothGatt#executeReliableWrite}
         * @param status {@link BluetoothGatt#GATT_SUCCESS} if the reliable write
         */
        @Override
        public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {
            super.onReliableWriteCompleted(gatt, status);
        }

        /**
         * Callback indicating the result of a descriptor write operation.
         *
         * @param gatt       GATT client invoked {@link BluetoothGatt#writeDescriptor}
         * @param descriptor Descriptor that was writte to the associated
         *                   remote device.
         * @param status     The result of the write operation
         *                   {@link BluetoothGatt#GATT_SUCCESS} if the operation succeeds.
         */
        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorWrite(gatt, descriptor, status);
        }

        /**
         * Callback reporting the RSSI for a remote device connection.
         * <p/>
         * This callback is triggered in response to the
         * {@link BluetoothGatt#readRemoteRssi} function.
         *
         * @param gatt   GATT client invoked {@link BluetoothGatt#readRemoteRssi}
         * @param rssi   The RSSI value for the remote device
         * @param status {@link BluetoothGatt#GATT_SUCCESS} if the RSSI was read successfully
         */
        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            super.onReadRemoteRssi(gatt, rssi, status);
        }

        /**
         * Callback indicating the MTU for a given device connection has changed.
         * <p/>
         * This callback is triggered in response to the
         * {@link BluetoothGatt#requestMtu} function, or in response to a connection
         * event.
         *
         * @param gatt   GATT client invoked {@link BluetoothGatt#requestMtu}
         * @param mtu    The new MTU size
         * @param status {@link BluetoothGatt#GATT_SUCCESS} if the MTU has been changed successfully
         */
        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            super.onMtuChanged(gatt, mtu, status);
        }
    };
    private DeviceFoundListener deviceListener = new DeviceFoundListener() {
        @Override
        public void onDeviceFound(BluetoothDevice device, int rssi, byte[] scanRecord) {

            if(mDevice==null) {

                Log.i(TAG, "New device added! : " + device.getName() + " address : " + device.getAddress());
                if (LED.equals(device.getName())) {
                    mDevice = device;
                    Log.i(TAG,"LED Bluetooth Device is Ready!");
                }
            }
        }
    };

    private Switch ledSwitch;

    private boolean state = false;

    private Handler mHandler = new Handler();
    // scan until ble device found.
    private Runnable scannerRunnable = new Runnable() {
        @Override
        public void run() {

            if(mDevice==null){
                helper.stopScanningBLEDevices();
                helper.scanBluetoothLEDevices(deviceListener);
                mHandler.removeCallbacks(scannerRunnable);
                mHandler.postDelayed(this,5000);
                Log.i(TAG,"Scanning restarted...");
            }else{
                helper.stopScanningBLEDevices();
                Log.i(TAG,"Scanning stopped...");
                mHandler.removeCallbacks(scannerRunnable);
                if(igniteConnected) {
                    Log.i(TAG,"Ignite ready registering..");
                   registerNodeAndThings(mDevice.getAddress());
                }else{
                    Log.i(TAG,"Starting ignite...");
                    initIgnite();
                    mHandler.removeCallbacks(scannerRunnable);
                    mHandler.postDelayed(this,5000);
                }

            }
        }
    };

    // ignite variables

    private IotIgniteManager mIotIgniteManager;
    private Node genuinoNode;
    private Thing ledThing;
    private ThingType ledThingType = new ThingType("Genuino101-BuiltinLED","Geniuno101", ThingDataType.STRING);
    private boolean igniteConnected = false;


    private ThingListener mThingListener = new ThingListener() {
        @Override
        public void onConfigurationReceived(Thing thing) {
            Log.i(TAG,"Thing Config Arrived.");
        }

        @Override
        public void onActionReceived(String s, String s1, ThingActionData thingActionData) {
            Log.i(TAG,"Thing Action Received.");
            // Genuino Builtin LED.
            if(!TextUtils.isEmpty(thingActionData.getMessage()) && thingActionData.getMessage().equals("LED_13_ON")){
                openLED();
            }else if(!TextUtils.isEmpty(thingActionData.getMessage()) && thingActionData.getMessage().equals("LED_13_OFF")){
                closeLED();
            }
        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ledSwitch = (Switch) findViewById(R.id.led_switch);

        ledSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                if(isChecked){
                    openLED();
                }else{
                    closeLED();
                }
            }
        });
        try {
            helper = BLEScanHelper.BLEHelperFactory.create(getApplicationContext());
        } catch (BLENotSupportedException e) {
            Log.i(TAG,"BLE Not Supported on this device.");
            finish();
        }

        if(helper.isBluetoothEnabled()){
            Log.i(TAG,"Bluetooth enabled!");
            helper.scanBluetoothLEDevices(deviceListener);
        }else {
            Log.i(TAG,"Bluetooth is disabled!");
            helper.openBluetooth(this);
        }

        mHandler.postDelayed(scannerRunnable,5000);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // User choose not to enable Bluetooth.
        if (requestCode == 1 && resultCode == Activity.RESULT_CANCELED) {
            finish();
            return;
        }
    }

    private void registerNodeAndThings(String bleMacID){
        if(igniteConnected){
            genuinoNode = IotIgniteManager.NodeFactory.createNode("Genuino101 -" + bleMacID ,"Genuino101 -" + bleMacID, NodeType.GENERIC );

                if (genuinoNode != null && (genuinoNode.isRegistered() || genuinoNode.register())) {
                    genuinoNode.setConnected(true,"Genuino is Ready");
                    ledThing = genuinoNode.createThing("GenuinoBuiltinLED",ledThingType, ThingCategory.EXTERNAL, true, mThingListener);
                    if(ledThing != null && (ledThing.isRegistered() || ledThing.register())){
                        ledThing.setConnected(true,"Builtin LED Thing is ready");
                    }
                }

            Log.i(TAG,"Connecting GATT...");
            ledGatt = mDevice.connectGatt(getApplicationContext(),true,mCallBack);

            if(ledGatt.connect()){
                ledGatt.discoverServices();
                Log.i(TAG,"Discovering GATT Services.....");
            }
        }
    }

    private void initIgnite(){

        mIotIgniteManager = new IotIgniteManager.Builder()
                .setAppKey(APP_KEY)
                .setContext(getApplicationContext())
                .setConnectionListener(new ConnectionCallback() {
                    @Override
                    public void onConnected() {
                        Log.i(TAG,"Ignite connected");
                        igniteConnected = true;
                    }

                    @Override
                    public void onDisconnected() {
                        Log.i(TAG,"Ignite disconnected.");
                        igniteConnected = false;
                    }
                }).build();
    }

    private void openLED(){
        ThingData data = new ThingData();
        if(ledCharacteristic!=null){
            ledCharacteristic.setValue("1".getBytes());
            ledGatt.writeCharacteristic(ledCharacteristic);
            data.addData("LED_13_ON");
            if(igniteConnected && ledThing!=null && ledThing.isRegistered()) {
                ledThing.sendData(data);
            }
        }
    }

    private void closeLED(){

        ThingData data = new ThingData();
        if(ledCharacteristic!=null){
            ledCharacteristic.setValue("0".getBytes());
            ledGatt.writeCharacteristic(ledCharacteristic);
            data.addData("LED_13_OFF");
            if(igniteConnected && ledThing!=null && ledThing.isRegistered()) {
                ledThing.sendData(data);
            }
        }
    }
}
