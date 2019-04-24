package com.convert.robotcontrol.socket.request;

import java.lang.reflect.Type;

/**
 * This class describes the model of control message, which is used to
 * directly control Turtlebot's movement.
 */
public class ControlMsgModel extends RequestMsgModel {

    /**
     * Used by gson to perform dynamic dispatch.
     */
    public static final String typeFieldValue = "control";

    /**
     * Linear speed of Turtlebot.
     */
    public double linear;

    /**
     * Angular speed of Turtlebot.
     */
    public double angular;

}
