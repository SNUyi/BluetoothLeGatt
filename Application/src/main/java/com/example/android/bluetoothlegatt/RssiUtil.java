package com.example.android.bluetoothlegatt;

import android.util.Log;

/**
 * 功能：根据rssi计算距离
 * Created by sunlipeng on 2019/1/22.
 */
public class RssiUtil {

    private static final String TAG = "RssiUtil";

    //A和n的值，需要根据实际环境进行检测得出
    private static final double A_Value = 72;/* A - 发射端和接收端相隔1米时的信号强度*/
    private static final double n_Value = 2.5;/* n - 环境衰减因子*/

    /**
     * 根据Rssi获得返回的距离,返回数据单位为m
     *
     * @param rssi
     * @return
     */
    public static double getDistance(int rssi) {
        int iRssi = Math.abs(rssi);
        double power = (iRssi - A_Value) / (10 * n_Value);
        Log.d(TAG, "power的值：" + power);
        return Math.pow(10, power);
    }
}
