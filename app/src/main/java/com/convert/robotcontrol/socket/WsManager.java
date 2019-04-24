package com.convert.robotcontrol.socket;

import android.util.Log;
import com.convert.robotcontrol.callback.LoadCallback;
import com.convert.robotcontrol.callback.WebSocketCallback;
import com.convert.robotcontrol.socket.request.ControlMsgModel;
import com.convert.robotcontrol.socket.request.NavigationGoalMsgModel;
import com.convert.robotcontrol.socket.request.PoseEstimateMsgModel;
import com.google.gson.Gson;
import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketAdapter;
import com.neovisionaries.ws.client.WebSocketException;
import com.neovisionaries.ws.client.WebSocketFactory;
import com.neovisionaries.ws.client.WebSocketFrame;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class WsManager {
    private static WsManager sInstance;
    private final String TAG = "WsManager";
    private WebSocketCallback mCallback;
    private LoadCallback mLoadCallBack;
    /**
     * WebSocket config
     */
    private static final int FRAME_QUEUE_SIZE = 5;
    private static final int CONNECT_TIMEOUT = 5000;
    private static final int NVG_SERVER_PORT = 2333;
    private static final int VIDEO_SERVER_PORT = 2334;
    private static final int CTRL_SERVER_PORT = 2335;

    private WsStatus mControlStatus;
    private WebSocket mControlClient;
    private ControlWsListener mControlListener;

    private WsStatus mNavigationStatus;
    private WebSocket mNavigationClient;
    private NavigationWsListener mNavigationListener;

    private WsStatus mVideoStatus;
    private WebSocket mVideoClient;
    private VideoWsListener mVideoListener;


    private WsManager() {
    }

    public static WsManager getInstance() {
        if (sInstance == null) {
            synchronized (WsManager.class) {
                if (sInstance == null) {
                    sInstance = new WsManager();
                }
            }
        }
        return sInstance;
    }

    public void init(String url) {
        try {
            String ctrlUrl = createWsUrl(url, CTRL_SERVER_PORT);
            mControlClient = new WebSocketFactory().createSocket(ctrlUrl, CONNECT_TIMEOUT)
                    .setFrameQueueSize(FRAME_QUEUE_SIZE)//设置帧队列最大值为5
                    .setMissingCloseFrameAllowed(false)//设置不允许服务端关闭连接却未发送关闭帧
                    .addListener(mControlListener = new ControlWsListener())//添加回调监听
                    .connectAsynchronously();//异步连接
            setControlStatus(WsStatus.CONNECTING);

        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            String nvgUrl = createWsUrl(url, NVG_SERVER_PORT);
            mNavigationClient = new WebSocketFactory().createSocket(nvgUrl, CONNECT_TIMEOUT)
                    .setFrameQueueSize(FRAME_QUEUE_SIZE)//设置帧队列最大值为5
                    .setMissingCloseFrameAllowed(false)//设置不允许服务端关闭连接却未发送关闭帧
                    .addListener(mNavigationListener = new NavigationWsListener())//添加回调监听
                    .connectAsynchronously();//异步连接
            setNavigationStatus(WsStatus.CONNECTING);

        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            String videoUrl = createWsUrl(url, VIDEO_SERVER_PORT);
            mVideoClient = new WebSocketFactory().createSocket(videoUrl, CONNECT_TIMEOUT)
                    .setFrameQueueSize(FRAME_QUEUE_SIZE)//设置帧队列最大值为5
                    .setMissingCloseFrameAllowed(false)//设置不允许服务端关闭连接却未发送关闭帧
                    .addListener(mVideoListener = new VideoWsListener())//添加回调监听
                    .connectAsynchronously();//异步连接
            setVideoStatus(WsStatus.CONNECTING);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String createWsUrl(String url, int port){
        StringBuilder sb = new StringBuilder();
        sb.append("ws://");
        sb.append(url);
        sb.append(":");
        sb.append(port);
        return sb.toString();
    }


    /**
     * 继承默认的监听空实现WebSocketAdapter,重写我们需要的方法
     * onTextMessage 收到文字信息
     * onConnected 连接成功
     * onConnectError 连接失败
     * onDisconnected 连接关闭
     */
    private class ControlWsListener extends WebSocketAdapter {
        @Override
        public void onTextMessage(WebSocket websocket, String text) throws Exception {
            super.onTextMessage(websocket, text);
            Log.i(TAG, "ControlWs onTextMessage: " + text);
        }


        @Override
        public void onConnected(WebSocket websocket, Map<String, List<String>> headers)
                throws Exception {
            super.onConnected(websocket, headers);
            Log.i(TAG, "ControlWs Connected succeed");
            setControlStatus(WsStatus.CONNECT_SUCCESS);
            mLoadCallBack.report(true);
        }


        @Override
        public void onConnectError(WebSocket websocket, WebSocketException exception)
                throws Exception {
            super.onConnectError(websocket, exception);
            Log.e(TAG, "ControlWs onConnectError: 连接错误", exception);
            setControlStatus(WsStatus.CONNECT_FAIL);
            mLoadCallBack.report(false);
        }


        @Override
        public void onDisconnected(WebSocket websocket, WebSocketFrame serverCloseFrame, WebSocketFrame clientCloseFrame, boolean closedByServer)
                throws Exception {
            super.onDisconnected(websocket, serverCloseFrame, clientCloseFrame, closedByServer);
            Log.i(TAG, "ControlWs onDisconnected: 断开连接");
            setControlStatus(WsStatus.CONNECT_FAIL);
        }
    }

    private class NavigationWsListener extends WebSocketAdapter {
        @Override
        public void onTextMessage(WebSocket websocket, String text) throws Exception {
            super.onTextMessage(websocket, text);
            Log.d(TAG, "NavigationWs onTextMessage: " + text);
            //收到地图，机器人位置信息
            mCallback.report(WebSocketCallback.DataType.ROBOT_POSE, text);
        }


        @Override
        public void onConnected(WebSocket websocket, Map<String, List<String>> headers)
                throws Exception {
            super.onConnected(websocket, headers);
            Log.i(TAG, "NavigationWs Connected succeed");
            setNavigationStatus(WsStatus.CONNECT_SUCCESS);
        }


        @Override
        public void onConnectError(WebSocket websocket, WebSocketException exception)
                throws Exception {
            super.onConnectError(websocket, exception);
            Log.e(TAG, "NavigationWs onConnectError: 连接错误", exception);
            setNavigationStatus(WsStatus.CONNECT_FAIL);
        }


        @Override
        public void onDisconnected(WebSocket websocket, WebSocketFrame serverCloseFrame, WebSocketFrame clientCloseFrame, boolean closedByServer)
                throws Exception {
            super.onDisconnected(websocket, serverCloseFrame, clientCloseFrame, closedByServer);
            Log.i(TAG, "NavigationWs onDisconnected: 断开连接");
            setNavigationStatus(WsStatus.CONNECT_FAIL);
        }
    }

    private class VideoWsListener extends WebSocketAdapter {
        @Override
        public void onTextMessage(WebSocket websocket, String text) throws Exception {
            super.onTextMessage(websocket, text);
            mCallback.report(WebSocketCallback.DataType.VIDEO, text);
        }


        @Override
        public void onConnected(WebSocket websocket, Map<String, List<String>> headers)
                throws Exception {
            super.onConnected(websocket, headers);
            Log.i(TAG, "VideoWs Connected succeed");
            setVideoStatus(WsStatus.CONNECT_SUCCESS);
        }


        @Override
        public void onConnectError(WebSocket websocket, WebSocketException exception)
                throws Exception {
            super.onConnectError(websocket, exception);
            Log.e(TAG, "VideoWs onConnectError: 连接错误", exception);
            setVideoStatus(WsStatus.CONNECT_FAIL);
        }


        @Override
        public void onDisconnected(WebSocket websocket, WebSocketFrame serverCloseFrame, WebSocketFrame clientCloseFrame, boolean closedByServer)
                throws Exception {
            super.onDisconnected(websocket, serverCloseFrame, clientCloseFrame, closedByServer);
            Log.i(TAG, "VideoWs onDisconnected: 断开连接");
            setVideoStatus(WsStatus.CONNECT_FAIL);
        }
    }

    private void setControlStatus(WsStatus status) {
        this.mControlStatus = status;
    }

    private WsStatus getControlStatus() {
        return mControlStatus;
    }

    private void setNavigationStatus(WsStatus status) {
        this.mNavigationStatus = status;
    }

    private WsStatus getNavigationStatus() {
        return mNavigationStatus;
    }

    private void setVideoStatus(WsStatus status) {
        this.mVideoStatus = status;
    }

    private WsStatus getVideoStatus() {
        return mVideoStatus;
    }

    public void disconnect() {
        if (mControlClient != null)
            mControlClient.disconnect();

        if (mNavigationClient != null)
            mNavigationClient.disconnect();

        if (mVideoClient != null)
            mVideoClient.disconnect();
    }

    public enum WsStatus {
        CONNECT_SUCCESS,//连接成功
        CONNECT_FAIL,//连接失败
        CONNECTING;//正在连接
    }

    public void doControl(ControlMsgModel msg) {
        if (mControlStatus == WsStatus.CONNECT_SUCCESS) {
            Gson gson = new Gson();

            Log.d(TAG, "doControl: " + gson.toJson(msg));
            mControlClient.sendText(gson.toJson(msg));
        }
    }

    public void doPoseEstimate(PoseEstimateMsgModel msg) {
        if (mNavigationStatus == WsStatus.CONNECT_SUCCESS) {
            Gson gson = new Gson();
            mNavigationClient.sendText(gson.toJson(msg));
        }
    }

    public void doNavigationGoal(NavigationGoalMsgModel msg) {
        if (mNavigationStatus == WsStatus.CONNECT_SUCCESS) {
            Gson gson = new Gson();
            mNavigationClient.sendText(gson.toJson(msg));
        }
    }

    public void registerCallback(WebSocketCallback callBack) {
        mCallback = callBack;
    }

    public void unRegisterCallback() {
        mCallback = null;
    }

    public void registerLoadCallBack(LoadCallback callBack) {
        mLoadCallBack = callBack;
    }

    public void unRegisterLoadCallBack() {
        mLoadCallBack = null;
    }
}
