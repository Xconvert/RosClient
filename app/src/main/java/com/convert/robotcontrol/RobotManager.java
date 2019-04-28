package com.convert.robotcontrol;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.graphics.PointF;
import android.util.Base64;
import android.util.Log;

import com.convert.robotcontrol.callback.Callback;
import com.convert.robotcontrol.callback.WebSocketCallback;
import com.convert.robotcontrol.socket.WsManager;
import com.convert.robotcontrol.socket.request.ControlMsgModel;
import com.convert.robotcontrol.socket.request.NavigationGoalMsgModel;
import com.convert.robotcontrol.socket.request.PoseEstimateMsgModel;
import com.convert.robotcontrol.socket.response.ImageMsgModel;
import com.convert.robotcontrol.socket.response.PoseMsgModel;
import com.google.gson.Gson;

public class RobotManager implements WebSocketCallback {

    private final String TAG = "RobotManager";

    private static RobotManager sRobotManager;

    private final int ONE_PX = 1;

    private Callback mCallback;
    //map manager
    private MapManager mMapManager;
    //webSocket
    private WsManager mWsManager;

    private RobotManager() {
    }

    public static RobotManager getInstance() {
        if (sRobotManager == null) {
            synchronized (RobotManager.class) {
                if (sRobotManager == null) {
                    sRobotManager = new RobotManager();
                }
            }
        }
        return sRobotManager;
    }

    public void init(Context context) {
        Log.i(TAG, "init");
        mMapManager = new MapManager(context);
        mWsManager = WsManager.getInstance();
    }

    public void registerCallBack(Callback callback) {
        mMapManager.registerCallback(callback);
        mWsManager.registerCallback(this);
        mCallback = callback;
    }

    public void close() {
        Log.i(TAG, "close");
        mMapManager.unRegisterCallback();
        mWsManager.unRegisterCallback();
        mWsManager.disconnect();
    }

    //============================
    //========== map =============
    //============================

    public Bitmap getThumbnails() {
        return mMapManager.getThumbnails();
    }

    public void clearDesPos() {
        mMapManager.clearDesPos();
    }

    public boolean isSetRobotPoint() {
        return mMapManager.isSetRobotPoint();
    }

    public double getOrientationAngle() {
        return mMapManager.getOrientationAngle();
    }

    public void setOrientationAngle(double stdAngle) {
        mMapManager.setOrientationAngle(stdAngle);
    }

    public void logMapArr() {
        mMapManager.logMapArr();
    }

    public void setRobotPos(PointF pointF) {
        mMapManager.setRobotPos(pointF);
    }

    public void setDesPos(PointF pointF) {
        mMapManager.setDesPos(pointF);
    }


    public void zoom(PointF center, float scale) {
        mMapManager.zoom(center, scale);
    }

    public void moveMap(float dx, float dy) {
        mMapManager.moveMap(dx, dy);
    }

    public Bitmap getDesMap() {
        return mMapManager.getDesMap();
    }

    public void robotPsUp(){
        mMapManager.moveRobot(MapManager.UP, ONE_PX);
    }

    public void robotPsLeft(){
        mMapManager.moveRobot(MapManager.LEFT, ONE_PX);
    }

    public void robotPsDown(){
        mMapManager.moveRobot(MapManager.DOWN, ONE_PX);
    }

    public void robotPsRight(){
        mMapManager.moveRobot(MapManager.RIGHT, ONE_PX);
    }

    //============================
    //======== webSocket =========
    //============================

    public void doControl(double lineSpeed, double angularVelocity) {
        ControlMsgModel msg = new ControlMsgModel();
        msg.type = ControlMsgModel.typeFieldValue;
        msg.linear = lineSpeed;
        msg.angular = angularVelocity;
        mWsManager.doControl(msg);
    }

    public void doNavigationGoal() {
        Log.i(TAG, "doNavigationGoal");
        NavigationGoalMsgModel msg = new NavigationGoalMsgModel();
        msg.type = NavigationGoalMsgModel.typeFieldValue;
        msg.x = getXProp();
        msg.y = getYProp();
        WsManager.getInstance().doNavigationGoal(msg);
    }

    public void doPoseEstimate() {
        PoseEstimateMsgModel msg = new PoseEstimateMsgModel();
        msg.type = PoseEstimateMsgModel.typeFieldValue;
        Point robotPos = mMapManager.getRobotPos();
        msg.x = (double) robotPos.x / mMapManager.getMapWidth();
        msg.y = 1 - (double) robotPos.y / mMapManager.getMapHeight();
        msg.angle = mMapManager.getOrientationAngle();
        mWsManager.doPoseEstimate(msg);
        Log.i(TAG, "doPoseEstimate: x is " + msg.x + ", y is " + msg.y + ", angle is " + msg.angle);
    }

    private Bitmap stringToBitmap(String string) {

        // 将字符串转换成Bitmap类型
        Bitmap bitmap = null;
        try {
            byte[] bitmapArray = Base64.decode(string, Base64.DEFAULT);

            bitmap = BitmapFactory.decodeByteArray(bitmapArray, 0, bitmapArray.length);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return bitmap;
    }

    private Point PoseMsgModelToPoint(PoseMsgModel msg) {
        int xInMap = (int) (msg.x * mMapManager.getMapWidth());
        int yInMap = (int) ((1 - msg.y) * mMapManager.getMapHeight());
        return new Point(xInMap, yInMap);
    }

    //proportion of the map's abscissa
    private double getXProp() {
        return (double) mMapManager.getDestPos().x / mMapManager.getMapWidth();
    }

    //Proportion of the ordinate on the map
    private double getYProp() {
        return 1 - (double) mMapManager.getDestPos().y / mMapManager.getMapHeight();
    }

    @Override
    public void report(DataType type, String data) {
        if (type == DataType.VIDEO) {
            Gson gson = new Gson();
            ImageMsgModel response = gson.fromJson(data, ImageMsgModel.class);
            //Log.i(TAG, "VideoWs onTextMessage: " + response.base64EncodedImageStr);
            mCallback.updateVideo(stringToBitmap(response.base64EncodedImageStr));
        } else if (type == DataType.ROBOT_POSE) {
            //收到地图，机器人位置信息
            Log.i(TAG, "report: data is " + data);
            Gson gson = new Gson();
            PoseMsgModel msg = gson.fromJson(data, PoseMsgModel.class);
            mMapManager.setOrientationAngle(msg.angle);
            mMapManager.setRobotPos(PoseMsgModelToPoint(msg));
        }
    }

    //control function and navigation can not coexist
    public void startCtrl() {
        mWsManager.connectCtrlClient();
    }

    public void finishCtrl() {
        mWsManager.disconnectCtrlClient();
    }
}
