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

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.fasterxml.jackson.core.JsonProcessingException;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URISyntaxException;
import java.util.ArrayList;

import io.samsungsami.websocket.Acknowledgement;
import io.samsungsami.websocket.ActionOut;
import io.samsungsami.websocket.DeviceChannelWebSocket;
import io.samsungsami.websocket.Error;
import io.samsungsami.websocket.FirehoseWebSocket;
import io.samsungsami.websocket.MessageOut;
import io.samsungsami.websocket.ActionIn;
import io.samsungsami.websocket.ActionDetails;
import io.samsungsami.websocket.ActionDetailsArray;
import io.samsungsami.websocket.RegisterMessage;
import io.samsungsami.websocket.SamiWebSocketCallback;

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

    private FirehoseWebSocket mLive = null; //  end point: /live
    private DeviceChannelWebSocket mWS = null; // end point: /websocket

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

    private void createLiveWebsocket() {
        try {
            mLive = new FirehoseWebSocket(DEVICE_TOKEN, DEVICE_ID, null, null, new SamiWebSocketCallback() {
                @Override
                public void onOpen(short i, String s) {
                    Log.d(TAG, "connectLiveWebsocket: onOpen()");
                    final Intent intent = new Intent(WEBSOCKET_LIVE_ONOPEN);
                    LocalBroadcastManager.getInstance(ourContext).sendBroadcast(intent);
                }

                @Override
                public void onMessage(MessageOut messageOut) {
                    Log.d(TAG, "connectLiveWebsocket: onMessage(" + messageOut.toString() + ")");
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
                    intent.putExtra("error", "mLive is closed. code: " + code + "; reason: " + reason);
                    LocalBroadcastManager.getInstance(ourContext).sendBroadcast(intent);
                }

                @Override
                public void onError(Error ex) {
                    final Intent intent = new Intent(WEBSOCKET_LIVE_ONERROR);
                    intent.putExtra("error", "mLive error: " + ex.getMessage());
                    LocalBroadcastManager.getInstance(ourContext).sendBroadcast(intent);
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
        if (mLive != null) {
            mLive.close();
        }
        mLive = null;
    }

    public void connectFirehoseWS() {
        createLiveWebsocket();
        mLive.connect();
    }

    private void createWSWebSockets() {
        try {
            mWS = new DeviceChannelWebSocket(true, new SamiWebSocketCallback() {
                @Override
                public void onOpen(short i, String s) {
                    Log.d(TAG, "Registering " + DEVICE_ID);
                    final Intent intent = new Intent(WEBSOCKET_WS_ONOPEN);
                    LocalBroadcastManager.getInstance(ourContext).sendBroadcast(intent);

                    RegisterMessage registerMessage = new RegisterMessage();
                    registerMessage.setAuthorization("bearer " + DEVICE_TOKEN);
                    registerMessage.setCid("myRegisterMessage");
                    registerMessage.setSdid(DEVICE_ID);

                    try {
                        Log.d(TAG, "DeviceChannelWebSocket::onOpen: registering" + DEVICE_ID);
                        mWS.registerChannel(registerMessage);
                    } catch (JsonProcessingException e) {
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
                public void onError(Error error) {
                    final Intent intent = new Intent(WEBSOCKET_WS_ONERROR);
                    intent.putExtra(ERROR, "mWebSocket error: " + error.getMessage());
                    LocalBroadcastManager.getInstance(ourContext).sendBroadcast(intent);
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
        if (mWS != null) {
            mWS.close();
        }
        mWS = null;
    }

    public void connectDeviceChannelWS() {
        createWSWebSockets();
        mWS.connect();
    }

    public void sendOnActionInDeviceChannelWS() {
        sendActionInDeviceChannelWS(ACTION_NAME_ON);
    }

    public void sendOffActionInDeviceChannelWS() {
        sendActionInDeviceChannelWS(ACTION_NAME_OFF);
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
        actionIn.setTs(BigDecimal.valueOf(System.currentTimeMillis()));

        try {
            mWS.sendAction(actionIn);
            Log.d(TAG, "DeviceChannelWebSocket sendAction:" + actionIn.toString());
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

    }

}