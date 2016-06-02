package com.ardic.android.connectivity.libwirelessconnection;


import org.junit.Test;

/**
 * Created by Yavuz on 2.06.2016.
 */
public class WifiHelperUnitTests{


    @Test(expected = IllegalArgumentException.class)
    public void WifiHelperInstanceCreatorTest(){
        WifiHelper mWifiHelper = WifiHelper.WifiHelperFactory.create(null);
    }
}
