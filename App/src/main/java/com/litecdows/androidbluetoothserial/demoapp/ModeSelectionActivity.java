package com.litecdows.androidbluetoothserial.demoapp;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProviders;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ModeSelectionActivity extends AppCompatActivity {

    private TextView connectionText, messagesView, Text_Device;
    private Button connectButton, mode1_auto_follow, mode2_remote_control, mode3_recall;
    private ModeSelectionViewModel viewModel;
    private RockerViewModel rocker;
    private LinearLayout mode_buttons ,debug_windows;
    private ListView listView;
    private String string_rocker_data;
    private static final double DISTANCE3 = 0.3;
    private static final double DEVIATION = 0.1;
    private final String[] keys = new String[]{
            "婴儿车型号：",
            "婴儿车MAC：",
            "控制模式：",
            "转向控制：",
            "人车距离：",
            "人车角度：",
    };
    private String[] values = new String[]{
            "获取中...",
            "获取中...",
            "请选择···",
            "停止",
            "获取中...",
            "获取中...",
    };
    private final int[] images_ic = new int[]{
            R.drawable.ic_directions_car_black_24dp,
            R.drawable.ic_swap_calls_black_24dp,
            R.drawable.ic_alarm_black_24dp,
            R.drawable.ic_alarm_on_black_24dp,
            R.drawable.ic_chrome_reader_mode_black_24dp,
            R.drawable.ic_directions_car_black_24dp,
    };



    @SuppressLint("DefaultLocale")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mode_selection);

        // Enable the back button in the action bar if possible
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Setup our ViewModel
        viewModel = ViewModelProviders.of(this).get(ModeSelectionViewModel.class);

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
        rocker = findViewById(R.id.rockerViewModel);
        mode_buttons = findViewById(R.id.linearLayout4);
        debug_windows = findViewById(R.id.linearLayout);
        ImageView return_Image = findViewById(R.id.mode_selection_toolbar_return);
        ImageView debug_Image = findViewById(R.id.mode_selection_toolbar_debug);
        ScrollView scroll = findViewById(R.id.scrollView3);
        listView = findViewById(R.id.ListView);
        messagesView.setMovementMethod(ScrollingMovementMethod.getInstance());

        //Initialize the ListView
        refreshListView();
        //top left icon; Back to main page
        return_Image.setOnClickListener(v -> ModeSelectionActivity.this.finish());
        //top right icon; open the debug window(scrollview)
        debug_Image.setOnClickListener(v -> {
            if(debug_windows.getVisibility()==View.GONE){
                listView.setVisibility(View.GONE);
                debug_windows.setVisibility(View.VISIBLE);
            }
            else{
                listView.setVisibility(View.VISIBLE);
                debug_windows.setVisibility(View.GONE);
            }
        });
        //rocker controller; front_1, front_2, front_3, left, right, back, stop.
        //Send information to the bluetooth serial port and Update the listview
        if (rocker != null) {refreshRocker();}

        // Start observing the data sent to us by the ViewModel
        viewModel.getConnectionStatus().observe(this, this::onConnectionStatus);
        viewModel.getDeviceName().observe(this, name -> {
            if (!TextUtils.isEmpty(name)) {
                Text_Device.setText(name);
                values[0] = name;
                refreshListView();
            }
        });
        viewModel.getDeviceMac().observe(this, mac -> {
            if (!TextUtils.isEmpty(mac)) {
                values[1] = mac;
                refreshListView();
            }
        });
        viewModel.getMessages().observe(this, message -> {
            if (TextUtils.isEmpty(message)) {
                message = getString(R.string.no_mode_select);
            }
            messagesView.setText(message);
            //keep sliding to the bottom
            int offset = messagesView.getLineCount() * messagesView.getLineHeight();
            if (offset > scroll.getHeight()) {
                //scroll.scrollTo( 0, offset - scroll.getHeight() );
                scroll.smoothScrollTo(0,offset - scroll.getHeight());
            }
        });
        viewModel.getMessage().observe(this,message->{
            if(message.length() == 4){
                int c_0 = message.charAt(0);
                int c_1 = message.charAt(1);
                int c_2 = message.charAt(2);
                int c_3 = message.charAt(3);

                double dis_0 = c_0 + (double)c_1/100 + DEVIATION;
                double dis_1 = c_2 + (double)c_3/100;
                double cos = (dis_0*dis_0 + DISTANCE3* DISTANCE3 - dis_1*dis_1)/(2*dis_0*DISTANCE3);
                double angle  = Math.acos(cos)*180/3.1415926;
                values[4] = String.format("%.2f", (dis_0+dis_1)/2)+"m";
                values[5] = String.format("%.2f", angle)+"°";
                refreshListView();
            }
        });

        modeSelect();
    }

    // Called when the ViewModel updates us of our connectivity status
    private void onConnectionStatus(@NonNull ModeSelectionViewModel.ConnectionStatus connectionStatus) {
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

    public String rockerSentData(double x, double y){
        if(x>=y && x>=-y){
            values[3] = "右转";
            return "R";
        }
        else if(x<y && x>=-y){
            values[3] = "倒车";
            return "X";
        }
        else if(x>=y && x<-y){
            double distance = Math.sqrt(Math.pow(x, 2) + Math.pow(y, 2));
            if(distance > 0 && distance<=0.4){
                values[3] = "前进1";
                return "F";
            }
            else if(distance > 0.4 && distance<=0.7){
                values[3] = "前进2";
                return "E";
            }
            else{
                values[3] = "前进3";
                return "D";
            }
        }
        else if(x<y && x<-y){
            values[3] = "左转";
            return "L";
        }
        else{
            values[3] = "停止";
            return "S";
        }

    }

    public void refreshRocker(){
        rocker.setOnDownActionListener((x, y) -> {
            string_rocker_data = rockerSentData(x,y);
            viewModel.sendMessage(string_rocker_data);
            refreshListView();
        });
        rocker.setOnMoveActionListener((x, y) -> {
            string_rocker_data = rockerSentData(x,y);
            viewModel.sendMessage(string_rocker_data);
            refreshListView();
        });
        rocker.setOnUpActionListener((x, y) -> {
            for(int i = 0; i<10; i++){
                viewModel.sendMessage("S");
                values[3] = "停止";
                refreshListView();
            }
        });
    }

    public void refreshListView(){
        List<Map<String, Object>> list = new ArrayList<>();
        for (int i = 0; i < keys.length; i++) {
            Map<String, Object> objectMap = new HashMap<>();
            objectMap.put("key", keys[i]);
            objectMap.put("value", values[i]);
            objectMap.put("img", images_ic[i]);
            list.add(objectMap);
        }
        SimpleAdapter adapter = new SimpleAdapter(this, list, R.layout.list_item_babycar_state, new String[]{"key", "value", "img"}, new int[]{R.id.key, R.id.value, R.id.img});
        listView.setAdapter(adapter);
    }

    public void modeSelect(){
        // Setup the send button click action
        mode1_auto_follow.setOnClickListener(v -> {
            viewModel.sendMessage("1");
            values[2] = "追踪模式";
            refreshListView();
        });
        mode2_remote_control.setOnClickListener(v -> {
            viewModel.sendMessage("2");
            values[2] = "遥控模式";
            refreshListView();
            mode_buttons.setVisibility(View.GONE);
            rocker.setVisibility(View.VISIBLE);
        });
        mode3_recall.setOnClickListener(v -> {
            viewModel.sendMessage("3");
            values[2] = "一键召回";
            refreshListView();
        });
    }

    // Called when a button in the action bar is pressed
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        if (item.getItemId() == android.R.id.home) {
            // If the back button was pressed, handle it the normal way
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // Called when the user presses the back button
    @Override
    public void onBackPressed() {
        // Close the activity
        if(rocker.getVisibility()==View.VISIBLE || debug_windows.getVisibility()==View.VISIBLE){
            if(rocker.getVisibility()==View.VISIBLE){
                rocker.setVisibility(View.GONE);
                mode_buttons.setVisibility(View.VISIBLE);
                values[2] = "请选择···";
                refreshListView();
            }
            debug_windows.setVisibility(View.GONE);
            listView.setVisibility(View.VISIBLE);
        }
        else {
            finish();
        }
    }

}
