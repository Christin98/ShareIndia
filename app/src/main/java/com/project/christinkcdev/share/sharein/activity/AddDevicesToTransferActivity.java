package com.project.christinkcdev.share.sharein.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.genonbeta.android.framework.ui.callback.SnackbarSupport;
import com.getkeepsafe.taptargetview.TapTarget;
import com.getkeepsafe.taptargetview.TapTargetView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.project.christinkcdev.share.sharein.R;
import com.project.christinkcdev.share.sharein.app.Activity;
import com.project.christinkcdev.share.sharein.database.AccessDatabase;
import com.project.christinkcdev.share.sharein.fragment.TransferAssigneeListFragment;
import com.project.christinkcdev.share.sharein.models.NetworkDevice;
import com.project.christinkcdev.share.sharein.models.TransferGroup;
import com.project.christinkcdev.share.sharein.service.WorkerService;
import com.project.christinkcdev.share.sharein.task.AddDeviceRunningTask;

import jonathanfinerty.once.Once;

public class AddDevicesToTransferActivity extends Activity
        implements SnackbarSupport, WorkerService.OnAttachListener {

    public static final String TAG = AddDevicesToTransferActivity.class.getSimpleName();

    public static final int REQUEST_CODE_CHOOSE_DEVICE = 0;

    public static final String EXTRA_DEVICE_ID = "extraDeviceId";
    public static final String EXTRA_GROUP_ID = "extraGroupId";

    private TransferGroup mGroup = null;
    private AddDeviceRunningTask mTask;
    private FloatingActionButton mActionButton;
    private ProgressBar mProgressBar;
    private ViewGroup mLayoutStatusContainer;
    private TextView mProgressTextLeft;
    private TextView mProgressTextRight;
    private IntentFilter mFilter = new IntentFilter(AccessDatabase.ACTION_DATABASE_CHANGE);
    private BroadcastReceiver mReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            if (AccessDatabase.ACTION_DATABASE_CHANGE.equals(intent.getAction()))
                if (intent.hasExtra(AccessDatabase.EXTRA_TABLE_NAME)
                        && AccessDatabase.TABLE_TRANSFERGROUP.equals(intent.getStringExtra(AccessDatabase.EXTRA_TABLE_NAME)))
                    if (!checkGroupIntegrity())
                        finish();
        }
    };

    public static void startInstance(Context context, long groupId)
    {
        context.startActivity(new Intent(context, AddDevicesToTransferActivity.class)
                .putExtra(EXTRA_GROUP_ID, groupId)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_devices_to_transfer);

        if (!checkGroupIntegrity())
            return;

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        Bundle assigneeFragmentArgs = new Bundle();
        assigneeFragmentArgs.putLong(TransferAssigneeListFragment.ARG_GROUP_ID, mGroup.groupId);
        assigneeFragmentArgs.putBoolean(TransferAssigneeListFragment.ARG_USE_HORIZONTAL_VIEW, false);

        mProgressBar = findViewById(R.id.progressBar);
        mProgressTextLeft = findViewById(R.id.text1);
        mProgressTextRight = findViewById(R.id.text2);
        mActionButton = findViewById(R.id.content_fab);
        mLayoutStatusContainer = findViewById(R.id.layoutStatusContainer);

        TransferAssigneeListFragment assigneeListFragment =
                (TransferAssigneeListFragment) getSupportFragmentManager()
                        .findFragmentById(R.id.assigneeListFragment);

        if (assigneeListFragment == null) {
            assigneeListFragment = (TransferAssigneeListFragment) Fragment
                    .instantiate(this, TransferAssigneeListFragment.class.getName(), assigneeFragmentArgs);

            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

            transaction.add(R.id.assigneeListFragment, assigneeListFragment);
            transaction.commit();
        }

        if (!Once.beenDone(Once.THIS_APP_INSTALL, "taptarget")) {
            TapTargetView.showFor(this, TapTarget.forView(mActionButton, "Add Devices", "Tap Here to Connrct")
                            .outerCircleColor(R.color.colorSecondary) // Specify a color for the outer circle
                            .outerCircleAlpha(0.96f) // Specify the alpha amount for the outer circle
                            .targetCircleColor(R.color.colorPrimary) // Specify a color for the target circle
                            .titleTextSize(20) // Specify the size (in sp) of the title text
                            .titleTextColor(R.color.colorPrimary) // Specify the color of the title text
                            .descriptionTextSize(10) // Specify the size (in sp) of the description text
                            .descriptionTextColor(R.color.colorPrimary) // Specify the color of the description text
                            .textColor(R.color.colorPrimary) // Specify a color for both the title and description text
                            .textTypeface(Typeface.SANS_SERIF) // Specify a typeface for the text
                            .dimColor(R.color.colorOnSecondary) // If set, will dim behind the view with 30% opacity of the given color
                            .drawShadow(true) // Whether to draw a drop shadow or not
                            .cancelable(false) // Whether tapping outside the outer circle dismisses the view
                            .tintTarget(true) // Whether to tint the target view's color
                            .transparentTarget(false) // Specify whether the target is transparent (displays the content underneath)
//                        .icon(Drawable) // Specify a custom drawable to draw as the target
                            .targetRadius(60), // Specify the target radius (in dp),
                     new TapTargetView.Listener() {
                        @Override
                        public void onTargetClick(TapTargetView view) {
                            super.onTargetClick(view);
                        }
                    });
            Once.markDone("taptarget");
        }


        resetStatusViews();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        int id = item.getItemId();

        if (id == android.R.id.home || id == R.id.actions_add_devices_done) {
            if (mTask != null)
                mTask.getInterrupter().interrupt();

            finish();
        } else if (id == R.id.actions_add_devices_help) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);

            builder.setTitle(R.string.text_help)
                    .setMessage(R.string.text_addDeviceHelp)
                    .setPositiveButton(R.string.butn_close, null);

            builder.show();
        } else
            return super.onOptionsItemSelected(item);

        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.actions_add_devices, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == android.app.Activity.RESULT_OK) {
            if (requestCode == REQUEST_CODE_CHOOSE_DEVICE
                    && data != null
                    && data.hasExtra(ConnectionManagerActivity.EXTRA_DEVICE_ID)
                    && data.hasExtra(ConnectionManagerActivity.EXTRA_CONNECTION_ADAPTER)) {
                String deviceId = data.getStringExtra(ConnectionManagerActivity.EXTRA_DEVICE_ID);
                String connectionAdapter = data.getStringExtra(ConnectionManagerActivity.EXTRA_CONNECTION_ADAPTER);

                try {
                    NetworkDevice networkDevice = new NetworkDevice(deviceId);
                    NetworkDevice.Connection connection = new NetworkDevice.Connection(deviceId, connectionAdapter);

                    getDatabase().reconstruct(networkDevice);
                    getDatabase().reconstruct(connection);

                    doCommunicate(networkDevice, connection);
                } catch (Exception e) {
                    Toast.makeText(AddDevicesToTransferActivity.this,
                            R.string.mesg_somethingWentWrong, Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    @Override
    protected void onPreviousRunningTask(@Nullable WorkerService.RunningTask task)
    {
        super.onPreviousRunningTask(task);

        if (task instanceof AddDeviceRunningTask) {
            mTask = ((AddDeviceRunningTask) task);
            mTask.setAnchorListener(this);
        }
    }

    @Override
    protected void onStart()
    {
        super.onStart();
        checkForTasks();
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        registerReceiver(mReceiver, mFilter);

        if (!checkGroupIntegrity())
            finish();
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        unregisterReceiver(mReceiver);
    }

    @Override
    public void onAttachedToTask(WorkerService.RunningTask task)
    {
        takeOnProcessMode();
    }

    public boolean checkGroupIntegrity()
    {
        try {
            if (getIntent() == null || !getIntent().hasExtra(EXTRA_GROUP_ID))
                throw new Exception(getString(R.string.text_empty));

            mGroup = new TransferGroup(getIntent().getLongExtra(EXTRA_GROUP_ID, -1));

            try {
                getDatabase().reconstruct(mGroup);
            } catch (Exception e) {
                throw new Exception(getString(R.string.mesg_notValidTransfer));
            }

            return true;
        } catch (Exception e) {
            Toast.makeText(AddDevicesToTransferActivity.this,
                    e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
        }

        return false;
    }

    public Snackbar createSnackbar(final int resId, final Object... objects)
    {
        return Snackbar.make(findViewById(R.id.container), getString(resId, objects), Snackbar.LENGTH_LONG);
    }

    public void doCommunicate(final NetworkDevice device, final NetworkDevice.Connection connection)
    {
        AddDeviceRunningTask task = new AddDeviceRunningTask(mGroup, device, connection);

        task.setTitle(getString(R.string.mesg_communicating))
                .setAnchorListener(this)
                .setContentIntent(this, getIntent())
                .run(this);

        attachRunningTask(task);
    }

    @Override
    public Intent getIntent()
    {
        return super.getIntent();
    }

    public void resetStatusViews()
    {
        mProgressBar.setMax(0);
        mProgressBar.setProgress(0);

//        mTextMain.setText(R.string.text_addDevicesToTransfer);
        mActionButton.setImageResource(R.drawable.ic_add_white_24dp);
        mLayoutStatusContainer.setVisibility(View.GONE);
        mActionButton.setOnClickListener(v -> startConnectionManagerActivity());
    }

    private void startConnectionManagerActivity()
    {
        startActivityForResult(new Intent(AddDevicesToTransferActivity.this, ConnectionManagerActivity.class)
                .putExtra(ConnectionManagerActivity.EXTRA_ACTIVITY_SUBTITLE, getString(R.string.text_addDevicesToTransfer)), REQUEST_CODE_CHOOSE_DEVICE);
    }

    public void takeOnProcessMode()
    {
        mLayoutStatusContainer.setVisibility(View.VISIBLE);
        mActionButton.setImageResource(R.drawable.ic_close_white_24dp);
        mActionButton.setOnClickListener(v -> {
            if (mTask != null)
                mTask.getInterrupter().interrupt();
        });
    }

    public void updateProgress(final int total, final int current)
    {
        if (isFinishing())
            return;

        runOnUiThread(() -> {
            mProgressTextLeft.setText(String.valueOf(current));
            mProgressTextRight.setText(String.valueOf(total));
        });

        mProgressBar.setProgress(current);
        mProgressBar.setMax(total);
    }

}