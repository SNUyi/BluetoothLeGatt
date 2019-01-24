package com.example.android.bluetoothlegatt;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.ParcelUuid;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.UUID;

public class ScanDevicesActivity extends Activity implements View.OnClickListener {

    private static final String TAG = ScanDevicesActivity.class.getSimpleName();

    private static final int REQUEST_CODE_LOCATION_SETTINGS = 2;
    private static final int REQUEST_CODE_ACCESS_COARSE_LOCATION = 1;

    private BluetoothAdapter mBluetoothAdapter;
    private boolean mScanning;
    private Handler mHandler = new Handler();

    private static final int REQUEST_ENABLE_BT = 1;
    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 10000;

    private Button mBtnBluetooth;
    private Button mStartActivity;
    private Button mBtnStartBroadcast;
    private Button mBtnStopBroadcast;
    private RecyclerView mRvList;

    private ListAdapter mListAdapter;

    private BluetoothStateReceiver mBluetoothStateReceiver;

    private BluetoothLeAdvertiser mBluetoothLeAdvertiser;

    private Runnable mRunnable = new Runnable() {
        @Override
        public void run() {

            scanLeDevice(true);

            mHandler.postDelayed(this, 1000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan_devices);

        mBluetoothStateReceiver = new BluetoothStateReceiver();

        registerReceiver(mBluetoothStateReceiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
        EventBus.getDefault().register(this);

        // Use this check to determine whether BLE is supported on the device.  Then you can
        // selectively disable BLE-related features.
        // 检查当前手机是否支持ble 蓝牙,如果不支持退出程序
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }

        // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
        // 初始化 Bluetooth adapter, 通过蓝牙管理器得到一个参考蓝牙适配器(API必须在以上android4.3或以上和版本)
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        // Checks if Bluetooth is supported on the device.
        // 检查设备上是否支持蓝牙
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        //TODO
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {//如果 API level 是大于等于 23(Android 6.0) 时
            //判断是否具有权限
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                //判断是否需要向用户解释为什么需要申请该权限
                if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.ACCESS_COARSE_LOCATION)) {
                    Toast.makeText(this, "自Android 6.0开始需要打开位置权限才可以搜索到Ble设备", Toast.LENGTH_SHORT).show();
                }
                //请求权限
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                        REQUEST_CODE_ACCESS_COARSE_LOCATION);
            }
        }

        Boolean haha = isLocationEnable(this);
        if (!haha) {
//            setLocationService();
        }

        initView();
    }

    private void initView() {
        mBtnBluetooth = (Button) findViewById(R.id.btn_bluetooth);
        mStartActivity = (Button) findViewById(R.id.start_activity);
        mBtnStartBroadcast = (Button) findViewById(R.id.btn_start_broadcast);
        mBtnStopBroadcast = (Button) findViewById(R.id.btn_stop_broadcast);
        mRvList = (RecyclerView) findViewById(R.id.rv_list);

        mBtnBluetooth.setOnClickListener(this);
        mStartActivity.setOnClickListener(this);
        mBtnStartBroadcast.setOnClickListener(this);
        mBtnStopBroadcast.setOnClickListener(this);

        mRvList.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        //添加Android自带的分割线
        mRvList.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));

        mListAdapter = new ListAdapter(this, new ListAdapter.OnClickListener() {
            @Override
            public void onItemClick(int position) {
                final BluetoothDevice device = mListAdapter.getDevice(position);
                if (device == null) return;
                final Intent intent = new Intent(ScanDevicesActivity.this, DeviceControlActivity.class);
                intent.putExtra(DeviceControlActivity.EXTRAS_DEVICE_NAME, device.getName());
                intent.putExtra(DeviceControlActivity.EXTRAS_DEVICE_ADDRESS, device.getAddress());
                if (mScanning) {
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                    mScanning = false;
                }
                startActivity(intent);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Ensures Bluetooth is enabled on the device.  If Bluetooth is not currently enabled,
        // fire an intent to display a dialog asking the user to grant permission to enable it.
        if (!mBluetoothAdapter.isEnabled()) {
            if (!mBluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
        }
        // Initializes list view adapter.
        mRvList.setAdapter(mListAdapter);

        //定时刷新
        mHandler.postDelayed(mRunnable, 1000);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // User chose not to enable Bluetooth.
        if (requestCode == REQUEST_CODE_LOCATION_SETTINGS) {
            if (isLocationEnable(this)) {
                //定位已打开的处理
                Toast.makeText(this, "定位已经打开", Toast.LENGTH_SHORT).show();

            } else {
                //定位依然没有打开的处理
                Toast.makeText(this, "定位没有打开", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
            finish();
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onPause() {
        super.onPause();
        scanLeDevice(false);
        mHandler.removeCallbacks(mRunnable);
        mListAdapter.clear();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
        unregisterReceiver(mBluetoothStateReceiver);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_bluetooth:
                if (mBluetoothAdapter != null) {
                    if ("打开蓝牙".equals(mBtnBluetooth.getText())) {
                        mBluetoothAdapter.enable();
                    } else if ("关闭蓝牙".equals(mBtnBluetooth.getText())) {
                        mBluetoothAdapter.disable();
                    }
                }
                break;
            case R.id.start_activity:
                startActivity(new Intent(this, DeviceScanActivity.class));
                break;
            case R.id.btn_start_broadcast:
                startAction(v);
                break;
            case R.id.btn_stop_broadcast:
                stopAction(v);
                break;
        }
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(BluetoothStatus bluetoothStatus) {
        int EXTRA_STATE = bluetoothStatus.getBluetoothStatus();
        Log.i(TAG, "当前蓝牙状态值：" + EXTRA_STATE);
        if (BluetoothAdapter.STATE_ON == EXTRA_STATE) {
            Log.d(TAG, "蓝牙已经打开了");
            mBtnBluetooth.setText("关闭蓝牙");
            mBtnBluetooth.setBackgroundColor(this.getResources().getColor(R.color.blue));
        } else if (BluetoothAdapter.STATE_OFF == EXTRA_STATE) {
            Log.d(TAG, "蓝牙关闭了");
            mBtnBluetooth.setText("打开蓝牙");
            mBtnBluetooth.setBackgroundColor(this.getResources().getColor(R.color.gray));
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        if (!mScanning) {
            menu.findItem(R.id.menu_stop).setVisible(false);
            menu.findItem(R.id.menu_scan).setVisible(true);
            menu.findItem(R.id.menu_refresh).setActionView(null);
        } else {
            menu.findItem(R.id.menu_stop).setVisible(true);
            menu.findItem(R.id.menu_scan).setVisible(false);
            menu.findItem(R.id.menu_refresh).setActionView(
                    R.layout.actionbar_indeterminate_progress);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_scan:
//                mListAdapter.clear();
//                scanLeDevice(true);
                //定时刷新
                mHandler.postDelayed(mRunnable, 1000);
                break;
            case R.id.menu_stop:
                mHandler.removeCallbacks(mRunnable);
                scanLeDevice(false);
                break;
        }
        return true;
    }

    //TODO 执行完上面的请求权限后，系统会弹出提示框让用户选择是否允许改权限。选择的结果可以在回到接口中得知：
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CODE_ACCESS_COARSE_LOCATION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                //用户允许改权限，0表示允许，-1表示拒绝 PERMISSION_GRANTED = 0， PERMISSION_DENIED = -1
                //permission was granted, yay! Do the contacts-related task you need to do.
                //这里进行授权被允许的处理
            } else {
                //permission denied, boo! Disable the functionality that depends on this permission.
                //这里进行权限被拒绝的处理
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    //判断定位
    public static final boolean isLocationEnable(Context context) {
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        boolean networkProvider = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        boolean gpsProvider = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        if (networkProvider || gpsProvider) return true;
        return false;
    }

    private void setLocationService() {
        Intent locationIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        this.startActivityForResult(locationIntent, REQUEST_CODE_LOCATION_SETTINGS);
    }

    private void scanLeDevice(final boolean enable) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
//            mHandler.postDelayed(new Runnable() {
//                @Override
//                public void run() {
//                    mScanning = false;
//                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
//                    invalidateOptionsMenu();
//                }
//            }, SCAN_PERIOD);

            mScanning = true;
            //根据UUID限定能够扫描到的设备
            UUID[] serviceUuids = new UUID[]{UUID.fromString("0000ae8f-0000-1000-8000-00805f9b34fb")};

            mListAdapter.clear();
            mListAdapter.notifyDataSetChanged();

            mBluetoothAdapter.startLeScan(serviceUuids, mLeScanCallback);
            Log.d(TAG, "开始扫描蓝牙广播了");
//            mBluetoothAdapter.startLeScan(mLeScanCallback);
        } else {
            mScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }
        invalidateOptionsMenu();
    }

    // Device scan callback.
    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {

                @Override
                public void onLeScan(final BluetoothDevice device, final int rssi, final byte[] scanRecord) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mListAdapter.addDevice(device, scanRecord, rssi, RssiUtil.getDistance(rssi));
                            mListAdapter.notifyDataSetChanged();
                        }
                    });
                }
            };


    public void startAction(View v) {
        if (!mBluetoothAdapter.isEnabled()) {
            Toast.makeText(this, "请打开蓝牙开关", Toast.LENGTH_LONG).show();
            return;
        }
        if (!mBluetoothAdapter.isMultipleAdvertisementSupported()) {
            Toast.makeText(this, "当前手机不支持BLE Advertise", Toast.LENGTH_LONG).show();
            return;
        }
        mBluetoothLeAdvertiser = mBluetoothAdapter.getBluetoothLeAdvertiser();
        if (mBluetoothLeAdvertiser == null) {
            Toast.makeText(this, "当前手机不支持BLE Advertise", Toast.LENGTH_LONG).show();
            return;
        }

        AdvertiseSettings advertiseSettings = createAdvSettings(false, 0);

        if (advertiseSettings == null) {
            Toast.makeText(this, "当前手机不支持BLE Advertise", Toast.LENGTH_LONG).show();
            return;
        }
        final byte[] broadcastData = {0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07};
        mBluetoothLeAdvertiser.stopAdvertising(mAdvertiseCallback);

        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mBluetoothLeAdvertiser.startAdvertising(createAdvSettings(true, 0), createAdvertiseData(broadcastData), mAdvertiseCallback);

            }
        }, 1000);

    }

    public void stopAction(View v) {
        if (mBluetoothLeAdvertiser != null) {
            mBluetoothLeAdvertiser.stopAdvertising(mAdvertiseCallback);
            Toast.makeText(this, "已经停止蓝牙广播", Toast.LENGTH_LONG).show();
        }
    }

    public AdvertiseSettings createAdvSettings(boolean connectable, int timeoutMillis) {
        AdvertiseSettings.Builder mSettingsbuilder = new AdvertiseSettings.Builder();
        mSettingsbuilder.setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY);
        mSettingsbuilder.setConnectable(connectable);
        mSettingsbuilder.setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH);
        mSettingsbuilder.setTimeout(0);
        AdvertiseSettings mAdvertiseSettings = mSettingsbuilder.build();
        return mAdvertiseSettings;
    }

    public AdvertiseData createAdvertiseData(byte[] data) {
        AdvertiseData.Builder mDataBuilder = new AdvertiseData.Builder();

        mDataBuilder.addServiceUuid(ParcelUuid.fromString("0000ae8f-0000-1000-8000-00805f9b34fb"));
//        mDataBuilder.addServiceData( ParcelUuid.fromString("0000ae8f-0000-1000-8000-00805f9b34fb"),new byte[]{0x64,0x12});
        mDataBuilder.addManufacturerData(0x01AC, data);
        AdvertiseData mAdvertiseData = mDataBuilder.build();
        return mAdvertiseData;
    }

    private AdvertiseCallback mAdvertiseCallback = new AdvertiseCallback() {
        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            super.onStartSuccess(settingsInEffect);

            Toast.makeText(ScanDevicesActivity.this, "开启广播成功", Toast.LENGTH_LONG).show();
        }

        @Override
        public void onStartFailure(int errorCode) {
            super.onStartFailure(errorCode);
            Toast.makeText(ScanDevicesActivity.this, "开启广播失败 errorCode：" + errorCode, Toast.LENGTH_LONG).show();
        }
    };


}
