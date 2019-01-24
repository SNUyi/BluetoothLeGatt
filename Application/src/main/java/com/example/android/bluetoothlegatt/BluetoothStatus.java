package com.example.android.bluetoothlegatt;

/**
 * EventBus的蓝牙开关状态
 * Created by sunlipeng on 2019/1/17.
 */
public class BluetoothStatus {

    //蓝牙状态值
    private int bluetoothStatus;

    public BluetoothStatus(int bluetoothStatus) {
        this.bluetoothStatus = bluetoothStatus;
    }

    public int getBluetoothStatus() {
        return bluetoothStatus;
    }

    public void setBluetoothStatus(int bluetoothStatus) {
        this.bluetoothStatus = bluetoothStatus;
    }
}
