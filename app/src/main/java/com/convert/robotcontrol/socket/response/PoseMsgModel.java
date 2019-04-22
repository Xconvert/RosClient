package com.convert.robotcontrol.socket.response;

/**
 * This class describes the model of pose message, which is used to
 * retrieve Turtlebot's pose estimation on the map.
 */
public class PoseMsgModel {

    /**
     * Coordinate X on the map.
     */
    public double x;

    /**
     * Coordinate Y on the map.
     */
    public double y;

    /**
     * Orientation angle.
     */
    public double angle;

}
