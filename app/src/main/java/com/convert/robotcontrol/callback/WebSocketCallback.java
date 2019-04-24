package com.convert.robotcontrol.callback;

public interface WebSocketCallback {
    enum DataType {
        VIDEO,
        ROBOT_POSE;
    }
    void report(DataType type, String data);
}
