/*
 * Copyright (C) 2016 Samsung Electronics Co., Ltd.
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

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.squareup.okhttp.OkHttpClient;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;

import cloud.artik.model.Acknowledgement;
import cloud.artik.model.ActionDetails;
import cloud.artik.model.ActionDetailsArray;
import cloud.artik.model.ActionIn;
import cloud.artik.model.ActionOut;
import cloud.artik.model.MessageOut;
import cloud.artik.model.RegisterMessage;
import cloud.artik.model.WebSocketError;
import cloud.artik.websocket.ArtikCloudWebSocketCallback;
import cloud.artik.websocket.DeviceChannelWebSocket;
import cloud.artik.websocket.FirehoseWebSocket;

public class ArtikCloudSession {
    private final static String TAG = ArtikCloudSession.class.getSimpleName();
    private final static String DEVICE_ID = "<YOUR DEVICE ID>";
    private final static String DEVICE_TOKEN = "<YOUR DEVICE TOKEN>";
    private final static String DEVICE_NAME = "Smart Light";
    private final static String ACTION_NAME_ON = "setOn";
    private final static String ACTION_NAME_OFF = "setOff";

    private static ArtikCloudSession ourInstance = new ArtikCloudSession();
    private static Context ourContext;

    public final static String WEBSOCKET_LIVE_ONOPEN =
            "cloud.artik.example.iot.WEBSOCKET_LIVE_ONOPEN";
    public final static String WEBSOCKET_LIVE_ONMSG =
            "cloud.artik.example.iot.WEBSOCKET_LIVE_ONMSG";
    public final static String WEBSOCKET_LIVE_ONCLOSE =
            "cloud.artik.example.iot.WEBSOCKET_LIVE_ONCLOSE";
    public final static String WEBSOCKET_LIVE_ONERROR =
            "cloud.artik.example.iot.WEBSOCKET_LIVE_ONERROR";
    public final static String WEBSOCKET_WS_ONOPEN =
            "cloud.artik.example.iot.WEBSOCKET_WS_ONOPEN";
    public final static String WEBSOCKET_WS_ONREG =
            "cloud.artik.example.iot.WEBSOCKET_WS_ONREG";
    public final static String WEBSOCKET_WS_ONMSG =
            "cloud.artik.example.iot.WEBSOCKET_WS_ONMSG";
    public final static String WEBSOCKET_WS_ONACK =
            "cloud.artik.example.iot.WEBSOCKET_WS_ONACK";
    public final static String WEBSOCKET_WS_ONCLOSE =
            "cloud.artik.example.iot.WEBSOCKET_WS_ONCLOSE";
    public final static String WEBSOCKET_WS_ONERROR =
            "cloud.artik.example.iot.WEBSOCKET_WS_ONERROR";
    public final static String SDID = "sdid";
    public final static String DEVICE_DATA = "data";
    public final static String TIMESTEP = "ts";
    public final static String ACK = "ack";
    public final static String ERROR = "error";

    private FirehoseWebSocket mFirehoseWS = null; //  end point: /live
    private DeviceChannelWebSocket mDeviceChannelWS = null; // end point: /websocket

    public static ArtikCloudSession getInstance() {
        return ourInstance;
    }

    private ArtikCloudSession() {
        // Do nothing
    }

    public void setContext(Context context) {
        ourContext = context;
    }

    public String getDeviceID() {
        return DEVICE_ID;
    }

    public String getDeviceName() {
        return DEVICE_NAME;
    }

    private void createFirehoseWebsocket() {
        try {
            OkHttpClient client = new OkHttpClient();
            client.setRetryOnConnectionFailure(true);
            mFirehoseWS = new FirehoseWebSocket(client, DEVICE_TOKEN, DEVICE_ID, null, null, null, new ArtikCloudWebSocketCallback() {
                @Override
                public void onOpen(int i, String s) {
                    Log.d(TAG, "FirehoseWebSocket: onOpen()");
                    final Intent intent = new Intent(WEBSOCKET_LIVE_ONOPEN);
                    LocalBroadcastManager.getInstance(ourContext).sendBroadcast(intent);
                }

                @Override
                public void onMessage(MessageOut messageOut) {
                    Log.d(TAG, "FirehoseWebSocket: onMessage(" + messageOut.toString() + ")");
                    final Intent intent = new Intent(WEBSOCKET_LIVE_ONMSG);
                    intent.putExtra(SDID, messageOut.getSdid());
                    intent.putExtra(DEVICE_DATA, messageOut.getData().toString());
                    intent.putExtra(TIMESTEP, messageOut.getTs().toString());
                    LocalBroadcastManager.getInstance(ourContext).sendBroadcast(intent);
                }

                @Override
                public void onAction(ActionOut actionOut) {
                }

                @Override
                public void onAck(Acknowledgement acknowledgement) {
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    final Intent intent = new Intent(WEBSOCKET_LIVE_ONCLOSE);
                    intent.putExtra("error", "mFirehoseWS is closed. code: " + code + "; reason: " + reason);
                    LocalBroadcastManager.getInstance(ourContext).sendBroadcast(intent);
                }

                @Override
                public void onError(WebSocketError ex) {
                    final Intent intent = new Intent(WEBSOCKET_LIVE_ONERROR);
                    intent.putExtra("error", "mFirehoseWS error: " + ex.getMessage());
                    LocalBroadcastManager.getInstance(ourContext).sendBroadcast(intent);
                }

                @Override
                public void onPing(long timestamp) {
                    Log.d(TAG, "FirehoseWebSocket::onPing: " + timestamp);
                }
            });
        } catch (URISyntaxException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Closes a websocket /live connection
     */
    public void disconnectFirehoseWS() {
        if (mFirehoseWS != null) {
            try {
                mFirehoseWS.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        mFirehoseWS = null;
    }

    public void connectFirehoseWS() {
        createFirehoseWebsocket();
        try {
            mFirehoseWS.connect();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void createDeviceChannelWebSockets() {
        try {
            OkHttpClient client = new OkHttpClient();
            client.setRetryOnConnectionFailure(true);

            mDeviceChannelWS = new DeviceChannelWebSocket(true, client, new ArtikCloudWebSocketCallback() {
                @Override
                public void onOpen(int i, String s) {
                    Log.d(TAG, "Registering " + DEVICE_ID);
                    final Intent intent = new Intent(WEBSOCKET_WS_ONOPEN);
                    LocalBroadcastManager.getInstance(ourContext).sendBroadcast(intent);

                    RegisterMessage registerMessage = new RegisterMessage();
                    registerMessage.setAuthorization("bearer " + DEVICE_TOKEN);
                    registerMessage.setCid("myRegisterMessage");
                    registerMessage.setSdid(DEVICE_ID);

                    try {
                        Log.d(TAG, "DeviceChannelWebSocket::onOpen: registering" + DEVICE_ID);
                        mDeviceChannelWS.registerChannel(registerMessage);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onMessage(MessageOut messageOut) {
                    Log.d(TAG, "DeviceChannelWebSocket::onMessage(" + messageOut.toString());
                    final Intent intent = new Intent(WEBSOCKET_WS_ONMSG);
                    intent.putExtra(ACK, messageOut.toString());
                    LocalBroadcastManager.getInstance(ourContext).sendBroadcast(intent);
                }

                @Override
                public void onAction(ActionOut actionOut) {

                }

                @Override
                public void onAck(Acknowledgement acknowledgement) {
                    Log.d(TAG, "DeviceChannelWebSocket::onAck(" + acknowledgement.toString());
                    Intent intent;
                    if (acknowledgement.getMessage() != null && acknowledgement.getMessage().equals("OK")) {
                        intent = new Intent(WEBSOCKET_WS_ONREG);
                    } else {
                        intent = new Intent(WEBSOCKET_WS_ONACK);
                        intent.putExtra(ACK, acknowledgement.toString());
                    }
                    LocalBroadcastManager.getInstance(ourContext).sendBroadcast(intent);
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    final Intent intent = new Intent(WEBSOCKET_WS_ONCLOSE);
                    intent.putExtra(ERROR, "mWebSocket is closed. code: " + code + "; reason: " + reason);
                    LocalBroadcastManager.getInstance(ourContext).sendBroadcast(intent);

                }

                @Override
                public void onError(WebSocketError error) {
                    final Intent intent = new Intent(WEBSOCKET_WS_ONERROR);
                    intent.putExtra(ERROR, "mWebSocket error: " + error.getMessage());
                    LocalBroadcastManager.getInstance(ourContext).sendBroadcast(intent);
                }

                @Override
                public void onPing(long timestamp) {
                    Log.d(TAG, "DeviceChannelWebSocket::onPing: " + timestamp);
                }
            });
        } catch (URISyntaxException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Closes a websocket /websocket connection
     */
    public void disconnectDeviceChannelWS() {
        if (mDeviceChannelWS != null) {
            try {
                mDeviceChannelWS.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        mDeviceChannelWS = null;
    }

    public void connectDeviceChannelWS() {
        createDeviceChannelWebSockets();
        try {
            mDeviceChannelWS.connect();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendOnActionInDeviceChannelWS() {
        new sendActionInBackground().execute(ACTION_NAME_ON);
    }

    public void sendOffActionInDeviceChannelWS() {
        new sendActionInBackground().execute(ACTION_NAME_OFF);
    }

    /*
     * Example of Action sent to ARTIK Cloud over /websocket endpoint
     *  {
        cid:  setOff
        data:  {
                 actions: [
                            {
                              name:  setOff
                              parameter: {}
                            }
                          ]
               }
        ddid:  fde8715961f84798a841be23480b8ce5
        sdid:  null
        ts:   1451606965889
        }
     *
     */
    private void sendActionInDeviceChannelWS(String actionName) {
        ActionIn actionIn = new ActionIn();
        ActionDetails action = new ActionDetails();
        ArrayList<ActionDetails> actions = new ArrayList<>();
        ActionDetailsArray actionDetailsArray = new ActionDetailsArray();

        action.setName(actionName);
        actions.add(action);
        actionDetailsArray.setActions(actions);
        actionIn.setData(actionDetailsArray);
        actionIn.setCid(actionName);
        actionIn.setDdid(DEVICE_ID);
        actionIn.setTs(System.currentTimeMillis());

        try {
            mDeviceChannelWS.sendAction(actionIn);
            Log.d(TAG, "DeviceChannelWebSocket sendAction:" + actionIn.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    class sendActionInBackground extends AsyncTask<String, Void, Void> {
        final static String TAG = "sendActionInBackground";
        @Override
        protected Void doInBackground(String... actionName) {
            try {
                sendActionInDeviceChannelWS(actionName[0]);
            } catch (Exception e) {
                Log.v(TAG, "::doInBackground run into Exception");
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            // Do nothing!
        }
    }


}