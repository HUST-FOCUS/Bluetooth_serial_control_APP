package com.harrysoft.androidbluetoothserial.demoapp;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProviders;

public class ModeSelectionActivity extends AppCompatActivity {

    private TextView connectionText, messagesView, Text_Device;
    private Button connectButton, mode1_auto_follow, mode2_remote_control, mode3_recall;
    private CommunicateViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Setup our activity
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mode_selection);
        // Enable the back button in the action bar if possible
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Setup our ViewModel
        viewModel = ViewModelProviders.of(this).get(CommunicateViewModel.class);

        // This method return false if there is an error, so if it does, we should close.
        if (!viewModel.setupViewModel(getIntent().getStringExtra("device_name"), getIntent().getStringExtra("device_mac"))) {
            finish();
            return;
        }

        // Setup our Views
        connectionText = findViewById(R.id.mode_selection_connection_text);
        messagesView = findViewById(R.id.mode_selection_messages);
        connectButton = findViewById(R.id.mode_selection_connect);
        Text_Device = findViewById(R.id.mode_selection_toolbar_title);
        mode1_auto_follow = findViewById(R.id.mode_button1);
        mode2_remote_control = findViewById(R.id.mode_button2);
        mode3_recall = findViewById(R.id.mode_button3);
        ImageView return_Image = findViewById(R.id.mode_selection_toolbar_return);

        //top left icon; Back to main page
        return_Image.setOnClickListener(new View.OnClickListener()
        {@Override
        public void onClick(View v)
        {
            ModeSelectionActivity.this.finish();
        }
        });

        // Start observing the data sent to us by the ViewModel
        viewModel.getConnectionStatus().observe(this, this::onConnectionStatus);
        viewModel.getDeviceName().observe(this, name -> {
            if (!TextUtils.isEmpty(name)) {
                Text_Device.setText(name);
            }
        });
        viewModel.getMessages().observe(this, message -> {
            if (TextUtils.isEmpty(message)) {
                message = getString(R.string.no_mode_select);
            }
            messagesView.setText(message);
        });

        //setup the FrameLayout title
        Text_Device.setText(viewModel.getDeviceName().getValue());
        // Setup the send button click action
        mode1_auto_follow.setOnClickListener(v -> viewModel.sendMessage("1"));
        mode2_remote_control.setOnClickListener(v -> viewModel.sendMessage("2"));
        mode3_recall.setOnClickListener(v -> viewModel.sendMessage("3"));
    }

    // Called when the ViewModel updates us of our connectivity status
    private void onConnectionStatus(CommunicateViewModel.ConnectionStatus connectionStatus) {
        switch (connectionStatus) {
            case CONNECTED:
                connectionText.setText(R.string.status_connected);
                mode1_auto_follow.setEnabled(true);
                mode2_remote_control.setEnabled(true);
                mode3_recall.setEnabled(true);
                connectButton.setEnabled(true);
                connectButton.setText(R.string.disconnect);
                connectButton.setOnClickListener(v -> viewModel.disconnect());
                break;

            case CONNECTING:
                connectionText.setText(R.string.status_connecting);
                mode1_auto_follow.setEnabled(false);
                mode2_remote_control.setEnabled(false);
                mode3_recall.setEnabled(false);
                connectButton.setEnabled(false);
                connectButton.setText(R.string.connect);
                break;

            case DISCONNECTED:
                connectionText.setText(R.string.status_disconnected);
                mode1_auto_follow.setEnabled(false);
                mode2_remote_control.setEnabled(false);
                mode3_recall.setEnabled(false);
                connectButton.setEnabled(true);
                connectButton.setText(R.string.connect);
                connectButton.setOnClickListener(v -> viewModel.connect());
                break;
        }
    }

    // Called when a button in the action bar is pressed
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId()) {
            case android.R.id.home:
                // If the back button was pressed, handle it the normal way
                onBackPressed();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    // Called when the user presses the back button
    @Override
    public void onBackPressed() {
        // Close the activity
        finish();
    }
}
