package com.project.christinkcdev.share.sharein.dialog;

import android.app.Activity;

import androidx.appcompat.app.AlertDialog;

import com.project.christinkcdev.share.sharein.R;
import com.project.christinkcdev.share.sharein.database.AccessDatabase;
import com.project.christinkcdev.share.sharein.models.NetworkDevice;
import com.project.christinkcdev.share.sharein.ui.UIConnectionUtils;
import com.project.christinkcdev.share.sharein.ui.callback.NetworkDeviceSelectedListener;
import com.project.christinkcdev.share.sharein.util.NetworkDeviceLoader;

public class ManualIpAddressConnectionDialog extends AbstractSingleTextInputDialog {
    private NetworkDeviceLoader.OnDeviceRegisteredListener mSelfListener = new NetworkDeviceLoader
            .OnDeviceRegisteredListener()
    {
        @Override
        public void onDeviceRegistered(AccessDatabase database, NetworkDevice device, NetworkDevice.Connection connection)
        {
            if (mDialog != null && mDialog.isShowing())
                mDialog.dismiss();

            if (mListener != null)
                mListener.onNetworkDeviceSelected(device, connection);
        }
    };

    private AlertDialog mDialog;
    private NetworkDeviceSelectedListener mListener;

    public ManualIpAddressConnectionDialog(final Activity activity, final UIConnectionUtils utils,
                                           NetworkDeviceSelectedListener listener)
    {
        super(activity);

        mListener = listener;

        setTitle(R.string.butn_enterIpAddress);

        setOnProceedClickListener(R.string.butn_connect, dialog -> {
            doTask(activity, utils);
            return false;
        });
    }

    private void doTask(final Activity activity, final UIConnectionUtils utils)
    {

        final String ipAddress = getEditText().getText().toString();

        if (ipAddress.matches("([0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3})")) {
            utils.makeAcquaintance(activity, null, ipAddress, -1, mSelfListener);
        } else
            getEditText().setError(getContext().getString(R.string.mesg_errorNotAnIpAddress));
    }

    @Override
    public AlertDialog show()
    {
        return mDialog = super.show();
    }
}
