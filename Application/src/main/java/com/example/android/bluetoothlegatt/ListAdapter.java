package com.example.android.bluetoothlegatt;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by sunlipeng on 2019/1/23.
 */
public class ListAdapter extends RecyclerView.Adapter<ListAdapter.ViewHolder> {

    private Context mContext;

    private ArrayList<BluetoothDevice> mLeDevices;
    private LayoutInflater mInflator;
    private ArrayList<byte[]> mRecords;//广播包
    private ArrayList<Integer> mRssi;
    private ArrayList<Double> mDistance;//距離估算
    private OnClickListener mOnClickListener;

    public ListAdapter(Context context, OnClickListener listener) {
        mContext = context;
        mOnClickListener = listener;
        mLeDevices = new ArrayList<BluetoothDevice>();
        mRecords = new ArrayList<>();
        mRssi = new ArrayList<>();
        mDistance = new ArrayList<>();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(mContext).inflate(R.layout.listitem_device, parent, false));
    }

    public void addDevice(BluetoothDevice device, byte[] scanRecord,int rssi, double distance) {
        if (!mLeDevices.contains(device)) {
            mLeDevices.add(device);
            mRecords.add(scanRecord);
            mDistance.add(distance);
            mRssi.add(rssi);
        }
    }

    public BluetoothDevice getDevice(int position) {
        return mLeDevices.get(position);
    }

    public void clear() {
        mLeDevices.clear();
        mRecords.clear();
        mDistance.clear();
        mRssi.clear();
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, final int position) {
        BluetoothDevice device = mLeDevices.get(position);
        final String deviceName = device.getName();
        byte[] scanRecord = mRecords.get(position);

        if (deviceName != null && deviceName.length() > 0) {
            holder.mDeviceName.setText("设备名：" + deviceName);
        } else {
            holder.mDeviceName.setText(R.string.unknown_device);
        }
        holder.mDeviceAddress.setText("MAC地址：" + device.getAddress());
        holder.mTvDeviceData.setText(Arrays.toString(scanRecord));
        holder.mTvDistance.setText(String.valueOf(mDistance.get(position)));
        holder.mTvRssi.setText(String.valueOf(mRssi.get(position)));

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mOnClickListener != null){
                    mOnClickListener.onItemClick(position);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return mLeDevices.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public View rootView;
        public TextView mDeviceName;
        public TextView mDeviceAddress;
        public TextView mTvDeviceData;
        public TextView mTvRssi;
        public TextView mTvDistance;

        public ViewHolder(View rootView) {
            super(rootView);
            this.rootView = rootView;
            this.mDeviceName = (TextView) rootView.findViewById(R.id.device_name);
            this.mDeviceAddress = (TextView) rootView.findViewById(R.id.device_address);
            this.mTvDeviceData = (TextView) rootView.findViewById(R.id.tv_device_data);
            this.mTvRssi = (TextView) rootView.findViewById(R.id.tv_rssi);
            this.mTvDistance = (TextView) rootView.findViewById(R.id.tv_distance);
        }

    }


    public interface OnClickListener{
        void onItemClick(int position);
    }
}
