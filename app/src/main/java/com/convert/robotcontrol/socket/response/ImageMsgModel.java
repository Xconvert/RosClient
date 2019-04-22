package com.convert.robotcontrol.socket.response;

/**
 * This class describes the model of image message, which is used
 * to transfer the image captured by the Kinect camera on Turtlebot.
 */
public class ImageMsgModel {

    /**
     * String representation of Base64 encoded image.
     */
    public String base64EncodedImageStr;

}
