package com.example.android.bluetoothlegatt;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import org.greenrobot.eventbus.EventBus;

/**
 * 用于接收蓝牙状态的广播
 * Created by sunlipeng on 2019/1/17.
 */
public class BluetoothStateReceiver extends BroadcastReceiver {

    private static final String TAG = BluetoothStateReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_OFF);
        Log.i(TAG, "当前蓝牙状态值：" + state);
        EventBus.getDefault().post(new BluetoothStatus(state));
    }
}
