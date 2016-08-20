package com.ardic.android.iotignitedemoapp;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.ardic.android.iotignite.callbacks.ConnectionCallback;
import com.ardic.android.iotignite.enumerations.NodeType;
import com.ardic.android.iotignite.enumerations.ThingCategory;
import com.ardic.android.iotignite.enumerations.ThingDataType;
import com.ardic.android.iotignite.exceptions.AuthenticationException;
import com.ardic.android.iotignite.listeners.ThingListener;
import com.ardic.android.iotignite.nodes.IotIgniteManager;
import com.ardic.android.iotignite.nodes.Node;
import com.ardic.android.iotignite.things.Thing;
import com.ardic.android.iotignite.things.ThingActionData;
import com.ardic.android.iotignite.things.ThingConfiguration;
import com.ardic.android.iotignite.things.ThingData;
import com.ardic.android.iotignite.things.ThingType;

import java.util.Hashtable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class VirtualDemoNodeHandler implements ConnectionCallback {

    private static final String TAG = "IoTIgniteDemoApp";

    //Set your App Key
    private final String DEMO_APP_KEY = "a051502e2a6942d0a78ab64e5e5533d5";

    private static final int NUMBER_OF_THREADS_IN_EXECUTOR = 2;
    private static final long EXECUTOR_START_DELAY = 100L;
    private static volatile ScheduledExecutorService mExecutor;
    private Hashtable<String, ScheduledFuture<?>> tasks = new Hashtable<String, ScheduledFuture<?>>();

    private IotIgniteManager mIotIgniteManager;
    private Node myNode;
    private Thing mTemperatureThing, mHumidityThing, mLampThing;
    private ThingType mTempThingType, mHumThingType, mLampThingType;
    private ThingData mTempData, mHumData;
    private ThingDataHandler mThingDataHandler;

    private String NODE = "VirtualDemoNode";
    public final String TEMP_THING = "Temperature";
    public final String HUM_THING = "Humidity";
    public final String LAMP_THING = "Lamp";
    private boolean igniteConnected = false;

    // Temperature Thing Listener
    // Receives configuration and action
    private ThingListener tempThingListener = new ThingListener() {
        @Override
        public void onConfigurationReceived(Thing thing) {
            Log.i(TAG, "Config arrived for " + thing.getThingID());
            applyConfiguration(thing);
        }

        @Override
        public void onActionReceived(String s, String s1, ThingActionData thingActionData) {

        }
    };

    // Humidity Thing Listener
    // Receives configuration and action
    private ThingListener humThingListener = new ThingListener() {
        @Override
        public void onConfigurationReceived(Thing thing) {
            Log.i(TAG, "Config arrived for " + thing.getThingID());
            applyConfiguration(thing);
        }

        @Override
        public void onActionReceived(String s, String s1, ThingActionData thingActionData) {

        }
    };

    // Lamp Thing Listener
    // Receives configuration and action
    private ThingListener lampThingListener = new ThingListener() {
        @Override
        public void onConfigurationReceived(Thing thing) {
            Log.i(TAG, "Config arrived for " + thing.getThingID());
            applyConfiguration(thing);
        }

        @Override
        public void onActionReceived(String s, String s1, final ThingActionData thingActionData) {
            Log.i(TAG, "Action arrived for " + s);
            // Toggle lamp in sensor activity
            if(sensorsActivity != null) {
                sensorsActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ToggleButton toggleLamp = (ToggleButton) sensorsActivity.findViewById(R.id.toggleLamp);
                        Log.i(TAG, thingActionData.getMessage());
                        int message = 0;
                        try {
                            message = Integer.parseInt(thingActionData.getMessage() == null ? "0" : thingActionData.getMessage().replace("\"", ""));
                        } catch (NumberFormatException e) {
                            Log.i(TAG, "Message Invalid");
                        }
                        if (message == 1) {
                            toggleLamp.setChecked(true);
                        } else {
                            toggleLamp.setChecked(false);
                        }
                    }
                });
            }
        }
    };

    private static final long IGNITE_TIMER_PERIOD = 5000L;

    private Timer igniteTimer = new Timer();

    private Context applicatonContext;

    private Activity sensorsActivity;

    private IgniteWatchDog igniteWatchDog = new IgniteWatchDog();

    // Handle ignite connection with timer task
    private class IgniteWatchDog extends TimerTask {
        @Override
        public void run() {
            if(!igniteConnected) {
                Log.i(TAG, "Rebuild Ignite...");
                start();
            }
        }
    }

    public VirtualDemoNodeHandler(Activity activity, Context appContext) {
        this.applicatonContext = appContext;
        this.sensorsActivity = activity;
    }

    public void start() {
        // Build Ignite Manager
        mIotIgniteManager = new IotIgniteManager.Builder()
                .setContext(applicatonContext)
                .setAppKey(DEMO_APP_KEY)
                .setConnectionListener(this)
                .build();
        cancelAndScheduleIgniteTimer();
    }

    public void stop() {
        if (igniteConnected) {
            mHumidityThing.setConnected(false, "Application Destroyed");
            mTemperatureThing.setConnected(false, "Application Destroyed");
            myNode.setConnected(false, "ApplicationDestroyed");
        }
        if(mExecutor != null) {
            mExecutor.shutdown();
        }
    }

    @Override
    public void onConnected() {

        Log.i(TAG, "Ignite Connected!");
        igniteConnected = true;
        updateConnectionStatus(true);

        // IoT Ignite connected, register node and things
        initIgniteVariables();
        cancelAndScheduleIgniteTimer();
    }

    @Override
    public void onDisconnected() {

        Log.i(TAG, "Ignite Disconnected!");
        igniteConnected = false;
        updateConnectionStatus(false);
        cancelAndScheduleIgniteTimer();

    }

    // Change connection status views in sensor activity
    private void updateConnectionStatus(final boolean connected) {
        if(sensorsActivity != null) {
            sensorsActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    ImageView imageViewConnection = (ImageView) sensorsActivity.findViewById(R.id.imageViewConnection);
                    TextView textViewConnection = (TextView) sensorsActivity.findViewById(R.id.textConnection);
                    if (connected) {
                        imageViewConnection.setImageDrawable(sensorsActivity.getResources().getDrawable(R.drawable.connected));
                        textViewConnection.setText("Connected");
                    } else {
                        imageViewConnection.setImageDrawable(sensorsActivity.getResources().getDrawable(R.drawable.disconnected));
                        textViewConnection.setText("Disconnected");
                    }
                }
            });
        }
    }

    // Create node and things and register them
    private void initIgniteVariables() {
        mTempThingType = new ThingType("Temperature", "IoT Ignite Devzone", ThingDataType.FLOAT);
        mHumThingType = new ThingType("Humidity", "IoT Ignite Devzone", ThingDataType.FLOAT);
        mLampThingType = new ThingType("Lamp", "IoT Ignite Devzone", ThingDataType.INTEGER);

        myNode = IotIgniteManager.NodeFactory.createNode(NODE, NODE, NodeType.GENERIC);

        // Register node if not registered and set connection.
        if (!myNode.isRegistered() && myNode.register()) {
            myNode.setConnected(true, NODE + " is online");
            Log.i(TAG, myNode.getNodeID() + " is successfully registered!");
        } else {
            myNode.setConnected(true, NODE + " is online");
            Log.i(TAG, myNode.getNodeID() + " is already registered!");
        }
        if (myNode.isRegistered()) {
            mTemperatureThing = myNode.createThing(TEMP_THING, mTempThingType, ThingCategory.EXTERNAL, false, tempThingListener);
            mHumidityThing = myNode.createThing(HUM_THING, mHumThingType, ThingCategory.EXTERNAL, false, humThingListener);
            mLampThing = myNode.createThing(LAMP_THING, mLampThingType, ThingCategory.EXTERNAL, true, lampThingListener);
            registerThingIfNotRegistered(mTemperatureThing);
            registerThingIfNotRegistered(mHumidityThing);
            registerThingIfNotRegistered(mLampThing);
        }
    }

    private void registerThingIfNotRegistered(Thing t) {
        if (!t.isRegistered() && t.register()) {
            t.setConnected(true, t.getThingID() + " connected");
            Log.i(TAG, t.getThingID() + " is successfully registered!");
        } else {
            t.setConnected(true, t.getThingID() + " connected");
            Log.i(TAG, t.getThingID() + " is already registered!");
        }
        applyConfiguration(t);
    }

    // Get thing values from sensor activity
    // Then send these values to IotIgnite
    private class ThingDataHandler implements Runnable {

        Thing mThing;

        ThingDataHandler(Thing thing) {
            mThing = thing;
        }
        @Override
        public void run() {
            ThingData mThingData = new ThingData();
            if(mThing.equals(mTemperatureThing)) {
                SeekBar seekBarTemperature = (SeekBar) sensorsActivity.findViewById(R.id.seekBarTemperature);
                mThingData.addData(seekBarTemperature.getProgress());
            } else if(mThing.equals(mHumidityThing)) {
                SeekBar seekBarHumidity = (SeekBar) sensorsActivity.findViewById(R.id.seekBarHumidity);
                mThingData.addData(seekBarHumidity.getProgress());
            } else if(mThing.equals(mLampThing)) {
                ToggleButton toggleLamp = (ToggleButton) sensorsActivity.findViewById(R.id.toggleLamp);
                mThingData.addData(toggleLamp.isChecked() ? 1 : 0);
            }

            if(mThing.sendData(mThingData)){
                Log.i(TAG, "DATA SENT SUCCESSFULLY : " + mThingData);
            }else{
                Log.i(TAG, "DATA SENT FAILURE");
            }
        }
    }

    // Schedule data readers for things
    private void applyConfiguration(Thing thing) {
        String key = thing.getNodeID() + "|" + thing.getThingID();
        stopReadDataTask(key);
        if (thing.getThingConfiguration().getDataReadingFrequency() > 0) {
            mThingDataHandler = new ThingDataHandler(thing);

            mExecutor = Executors.newScheduledThreadPool(NUMBER_OF_THREADS_IN_EXECUTOR);

            ScheduledFuture<?> sf = mExecutor.scheduleAtFixedRate(mThingDataHandler, EXECUTOR_START_DELAY, thing.getThingConfiguration().getDataReadingFrequency(), TimeUnit.MILLISECONDS);
            tasks.put(key, sf);
        }
    }

    // Stop task which reads data from thing
    public void stopReadDataTask(String key) {
        if (tasks.containsKey(key)) {
            try {
                tasks.get(key).cancel(true);
                tasks.remove(key);
            } catch (Exception e) {
                Log.d(TAG, "Could not stop schedule send data task" + e);
            }
        }
    }

    private void reconnect() {
        cancelAndScheduleIgniteTimer();
    }

    private void cancelAndScheduleIgniteTimer() {
        igniteTimer.cancel();
        igniteWatchDog.cancel();
        igniteWatchDog = new IgniteWatchDog();
        igniteTimer = new Timer();
        igniteTimer.schedule(igniteWatchDog, IGNITE_TIMER_PERIOD);
    }

    // Sends data to IotIgnite
    // If reading frequency is READING_WHEN_ARRIVE in thing configuration
    public void sendData(String thingId, int value) {
        if(igniteConnected) {
            try {
                Thing mThing = mIotIgniteManager.getNodeByID(NODE).getThingByID(thingId);
                if(mThing.getThingConfiguration().getDataReadingFrequency() == ThingConfiguration.READING_WHEN_ARRIVE) {
                    ThingData mthingData = new ThingData();
                    mthingData.addData(value);
                    if (mThing.sendData(mthingData)) {
                        Log.i(TAG, "DATA SENT SUCCESSFULLY : " + mthingData);
                    } else {
                        Log.i(TAG, "DATA SENT FAILURE");
                    }
                }
            } catch (AuthenticationException e) {
                Log.i(TAG, "AuthenticationException!");
            }
        } else {
            Log.i(TAG, "Ignite Disconnected!");
        }
    }
}