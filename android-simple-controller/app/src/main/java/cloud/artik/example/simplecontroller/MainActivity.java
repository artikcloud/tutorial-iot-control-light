/*
 * Copyright (C) 2015 Samsung Electronics Co., Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cloud.artik.example.simplecontroller;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.text.DateFormat;
import java.util.Date;


public class MainActivity  extends Activity {
    private static final String TAG = MainActivity.class.getSimpleName();
    private TextView mLiveStatus;
    private TextView mDeviceStatus;
    private TextView mStatusUpdateTime;
    private TextView mWSStatus;
    private TextView mWSReceived; // received ack or msg at /websocket end point

    private static final String WS_HEADER = "WebSocket /websocket: ";
    private static final String LIVE_HEADER = "WebSocket /live: ";
    private static final String DEVICE_REGISTERED = "device registered ";
    private static final String CONNECTED = "connected ";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        TextView deviceName;
        TextView deviceID;
        Button onBtn;
        Button offBtn;

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mLiveStatus = (TextView)findViewById(R.id.live_status);
        deviceName = (TextView)findViewById(R.id.device_name);
        deviceID = (TextView)findViewById(R.id.device_id);
        mDeviceStatus = (TextView)findViewById(R.id.device_status);
        mStatusUpdateTime = (TextView)findViewById(R.id.status_update_time);

        mWSStatus = (TextView)findViewById(R.id.ws_status);
        mWSReceived = (TextView)findViewById(R.id.ws_received);

        setTitle(R.string.device_monitor_title);

        ArtikCloudSession.getInstance().setContext(this);
        deviceID.setText("Device ID: " + ArtikCloudSession.getInstance().getDeviceID());
        deviceName.setText("Device Name: " + ArtikCloudSession.getInstance().getDeviceName());

        onBtn = (Button)findViewById(R.id.onBtn);
        onBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                try {
                    Log.v(TAG, ": on button is clicked.");
                    ArtikCloudSession.getInstance().sendOnActionInDeviceChannelWS();
                } catch (Exception e) {
                    Log.v(TAG, "Run into Exception");
                    e.printStackTrace();
                }
            }
        });

        offBtn = (Button)findViewById(R.id.offBtn);
        offBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                try {
                    Log.v(TAG, ": off button is clicked.");
                    ArtikCloudSession.getInstance().sendOffActionInDeviceChannelWS();
                } catch (Exception e) {
                    Log.v(TAG, "Run into Exception");
                    e.printStackTrace();
                }
            }
        });
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(this).registerReceiver(mWSUpdateReceiver,
                makeWebsocketUpdateIntentFilter());
        mLiveStatus.setText("Connecting to /live ... ");
        ArtikCloudSession.getInstance().connectFirehoseWS();//non blocking
        mWSStatus.setText("Connecting to /websocket ...");
        ArtikCloudSession.getInstance().connectDeviceChannelWS();//non blocking

    }

    @Override
    protected void onPause() {
        super.onPause();
        ArtikCloudSession.getInstance().disconnectFirehoseWS();
        ArtikCloudSession.getInstance().disconnectDeviceChannelWS();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mWSUpdateReceiver);
    }


    private static IntentFilter makeWebsocketUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ArtikCloudSession.WEBSOCKET_LIVE_ONOPEN);
        intentFilter.addAction(ArtikCloudSession.WEBSOCKET_LIVE_ONMSG);
        intentFilter.addAction(ArtikCloudSession.WEBSOCKET_LIVE_ONCLOSE);
        intentFilter.addAction(ArtikCloudSession.WEBSOCKET_LIVE_ONERROR);
        intentFilter.addAction(ArtikCloudSession.WEBSOCKET_WS_ONOPEN);
        intentFilter.addAction(ArtikCloudSession.WEBSOCKET_WS_ONREG);
        intentFilter.addAction(ArtikCloudSession.WEBSOCKET_WS_ONMSG);
        intentFilter.addAction(ArtikCloudSession.WEBSOCKET_WS_ONACK);
        intentFilter.addAction(ArtikCloudSession.WEBSOCKET_WS_ONCLOSE);
        intentFilter.addAction(ArtikCloudSession.WEBSOCKET_WS_ONERROR);
        return intentFilter;
    }

    private final BroadcastReceiver mWSUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (ArtikCloudSession.WEBSOCKET_LIVE_ONOPEN.equals(action)) {
                displayLiveStatus(LIVE_HEADER + CONNECTED);
            } else if (ArtikCloudSession.WEBSOCKET_LIVE_ONMSG.equals(action)) {
                String status = intent.getStringExtra(ArtikCloudSession.DEVICE_DATA);
                String updateTime = intent.getStringExtra(ArtikCloudSession.TIMESTEP);
                displayDeviceStatus(status, updateTime);
            } else if (ArtikCloudSession.WEBSOCKET_LIVE_ONCLOSE.equals(action) ||
                    ArtikCloudSession.WEBSOCKET_LIVE_ONERROR.equals(action)) {
                displayLiveStatus(LIVE_HEADER + intent.getStringExtra(ArtikCloudSession.ERROR));
            } else  if (ArtikCloudSession.WEBSOCKET_WS_ONOPEN.equals(action)) {
                displayWSStatus(WS_HEADER + CONNECTED);
            } else  if (ArtikCloudSession.WEBSOCKET_WS_ONREG.equals(action)) {
                displayWSStatus(WS_HEADER + DEVICE_REGISTERED);
            } else if (ArtikCloudSession.WEBSOCKET_WS_ONMSG.equals(action)) {
                displayWSReceived(intent.getStringExtra("msg"));
            } else if (ArtikCloudSession.WEBSOCKET_WS_ONACK.equals(action)) {
                displayWSReceived(intent.getStringExtra(ArtikCloudSession.ACK));
            } else if (ArtikCloudSession.WEBSOCKET_WS_ONCLOSE.equals(action) ||
                    ArtikCloudSession.WEBSOCKET_WS_ONERROR.equals(action)) {
                displayWSStatus(WS_HEADER + intent.getStringExtra(ArtikCloudSession.ERROR));
            }

        }
    };

    private void displayLiveStatus(String status) {
        Log.d(TAG, status);
        mLiveStatus.setText(status);
    }

    private void displayDeviceStatus(String status, String updateTimems) {
        mDeviceStatus.setText(status);
        long time_ms = Long.parseLong(updateTimems);
        mStatusUpdateTime.setText(DateFormat.getDateTimeInstance().format(new Date(time_ms)));
    }

    private void displayWSStatus(String status) {
        Log.d(TAG, status);
        mWSStatus.setText(status);
    }

    private void displayWSReceived(String status) {
        Log.d(TAG, status);
        mWSReceived.setText(status);
    }

}
