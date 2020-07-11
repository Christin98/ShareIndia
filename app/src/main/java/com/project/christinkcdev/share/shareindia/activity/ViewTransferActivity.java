package com.project.christinkcdev.share.shareindia.activity;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import com.project.christinkcdev.share.shareindia.R;

public class ViewTransferActivity extends AppCompatActivity {

    public static final String TAG = ViewTransferActivity.class.getSimpleName();

    public static final String ACTION_LIST_TRANSFERS = "com.genonbeta.TrebleShot.action.LIST_TRANSFERS";
    public static final String EXTRA_GROUP_ID = "extraGroupId";
    public static final String EXTRA_REQUEST_ID = "extraRequestId";
    public static final String EXTRA_DEVICE_ID = "extraDeviceId";
    public static final String EXTRA_REQUEST_TYPE = "extraRequestType";

    public static final int REQUEST_ADD_DEVICES = 5045;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_transfer);
    }
}