package com.ardic.android.connectivity.bluetooth.ble.exceptions;

/**
 * Created by root on 09.06.2016.
 */
public class BLENotSupportedException extends Exception{


    public BLENotSupportedException(String msg, Throwable cause){
            super(msg,cause);
    }

    public BLENotSupportedException(){
        super();
    }
}
