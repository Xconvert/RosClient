//package com.convert.robotcontrol.publish;
//
//import android.support.annotation.NonNull;
//
//import com.convert.robotcontrol.node.RoverOSNode;
//
///**
// * This class implements publisher that publish control message to Turtlebot control topic
// * at a constant rate.
// */
//public class ControlMsgPublisher {
//
//    /**
//     * Linear speed (forward or backward).
//     */
//    private double linear = 0.0;
//
//    /**
//     * Angular speed (left or right).
//     */
//    private double angular = 0.0;
//
//    /**
//     * Linear speed scale factor. Will be multiplied with linear speed to produce final linear speed.
//     */
//    private double linearScale = 1.0;
//
//    /**
//     * Angular speed scale factor. Will be multiplied with angular speed to produce final angular speed.
//     */
//    private double angularScale = 1.0;
//
//    /**
//     * Construct publisher firing messages at given rate.
//     *
//     * @param intervalMillis interval between two adjacent publishes
//     */
//    private ControlMsgPublisher(@NonNull RoverOSNode node, final long intervalMillis) {
//        final Publisher<Twist> publisher = node.publishOnTopic(GraphName.of("/cmd_vel_mux/input/teleop"), geometry_msgs.Twist.class);
//        new Thread(() -> {
//            while (!Thread.currentThread().isInterrupted()) {
//                if (ControlServer.this.getConnections().size() > 0) {
//                    double linearValue, angularValue;
//                    synchronized (this) {
//                        linearValue = linear * linearScale;
//                        angularValue = angular * angularScale;
//                    }
//
//                    geometry_msgs.Twist msg = publisher.newMessage();
//                    msg.getLinear().setX(linearValue);
//                    msg.getAngular().setZ(angularValue);
//                    publisher.publish(msg);
//                }
//
//                try {
//                    Thread.sleep(intervalMillis);
//                } catch (InterruptedException e) {
//                    Thread.currentThread().interrupt();
//                }
//            }
//        }).start();
//    }
//
//    /**
//     * Set linear speed.
//     *
//     * @param value linear speed value
//     */
//    private synchronized void setLinear(double value) {
//        linear = value;
//    }
//
//    /**
//     * Set angular speed.
//     *
//     * @param value angular speed value
//     */
//    private synchronized void setAngular(double value) {
//        angular = value;
//    }
//
//    /**
//     * Set linear speed scale factor.
//     *
//     * @param value linear speed scale factor value
//     */
//    private synchronized void setLinearScale(double value) {
//        linearScale = value;
//    }
//
//    /**
//     * Set angular speed scale factor.
//     *
//     * @param value angular speed scale factor value
//     */
//    private synchronized void setAngularScale(double value) {
//        angularScale = value;
//    }
//}
