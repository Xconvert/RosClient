package com.convert.robotcontrol.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import com.convert.robotcontrol.R;

public class RockerView extends View {
    private static final String TAG = "RockerView";

    private static final int DEFAULT_SIZE = 400;
    private static final int DEFAULT_ROCKER_RADIUS = 50;
    //暂定...最大线速度和最大角速度
    private static final double MAX_SPEED = 1;
    private static final double MAX_ANGULAR_VELOCITY = 1;

    private Paint mAreaBackgroundPaint;
    private Paint mRockerPaint;

    private Point mRockerPosition;
    private Point mCenterPoint;

    private int mAreaRadius;
    private int mRockerRadius;

    private OnAngleChangeListener mOnAngleChangeListener;

    // 摇杆可移动区域背景
    private Bitmap mAreaBitmap;

    // 摇杆背景
    private Bitmap mRockerBitmap;

    public RockerView(Context context, AttributeSet attrs) {
        super(context, attrs);

        // 获取自定义属性
        initAttribute(context, attrs);

        // 移动区域画笔
        mAreaBackgroundPaint = new Paint();
        mAreaBackgroundPaint.setAntiAlias(true);

        // 摇杆画笔
        mRockerPaint = new Paint();
        mRockerPaint.setAntiAlias(true);

        // 中心点
        mCenterPoint = new Point();
        // 摇杆位置
        mRockerPosition = new Point();
    }


    private void initAttribute(Context context, AttributeSet attrs) {
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.RockerView);

        // 可移动区域背景
        Drawable areaBackground = typedArray.getDrawable(R.styleable.RockerView_areaBackground);
        if (null != areaBackground) {
            // 设置背景
            mAreaBitmap = ((BitmapDrawable) areaBackground).getBitmap();
        }

        // 摇杆背景
        Drawable rockerBackground = typedArray.getDrawable(R.styleable.RockerView_rockerBackground);
        if (null != rockerBackground) {
            // 设置摇杆背景
            mRockerBitmap = ((BitmapDrawable) rockerBackground).getBitmap();
        }

        // 摇杆半径
        mRockerRadius = typedArray.getDimensionPixelOffset(R.styleable.RockerView_rockerRadius, DEFAULT_ROCKER_RADIUS);

        typedArray.recycle();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int measureWidth, measureHeight;

        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        if (widthMode == MeasureSpec.EXACTLY) {
            // 具体的值和match_parent
            measureWidth = widthSize;
        } else {
            // wrap_content
            measureWidth = DEFAULT_SIZE;
        }
        if (heightMode == MeasureSpec.EXACTLY) {
            measureHeight = heightSize;
        } else {
            measureHeight = DEFAULT_SIZE;
        }

        setMeasuredDimension(measureWidth, measureHeight);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int measuredWidth = getMeasuredWidth();
        int measuredHeight = getMeasuredHeight();

        int centerX = measuredWidth / 2;
        int centerY = measuredHeight / 2;
        // 中心点
        mCenterPoint.set(centerX, centerY);
        // 可移动区域的半径
        mAreaRadius = (measuredWidth <= measuredHeight) ? centerX : centerY;
        mAreaRadius -= mRockerRadius;

        // 摇杆位置
        if (0 == mRockerPosition.x || 0 == mRockerPosition.y) {
            mRockerPosition.set(mCenterPoint.x, mCenterPoint.y);
        }

        // 画可移动区域
        Rect src1 = new Rect(0, 0, mAreaBitmap.getWidth(), mAreaBitmap.getHeight());
        Rect dst1 = new Rect(mCenterPoint.x - mAreaRadius, mCenterPoint.y - mAreaRadius, mCenterPoint.x + mAreaRadius, mCenterPoint.y + mAreaRadius);
        canvas.drawBitmap(mAreaBitmap, src1, dst1, mAreaBackgroundPaint);

        // 画摇杆
        Rect src2 = new Rect(0, 0, mRockerBitmap.getWidth(), mRockerBitmap.getHeight());
        Rect dst2 = new Rect(mRockerPosition.x - mRockerRadius, mRockerPosition.y - mRockerRadius, mRockerPosition.x + mRockerRadius, mRockerPosition.y + mRockerRadius);
        canvas.drawBitmap(mRockerBitmap, src2, dst2, mRockerPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:// 按下
                // 回调开始
                mOnAngleChangeListener.onStart();
            case MotionEvent.ACTION_MOVE:// 移动
                float moveX = event.getX();
                float moveY = event.getY();
                mRockerPosition = getRockerPositionPoint(mCenterPoint, new Point((int) moveX, (int) moveY), mAreaRadius);
                //移动摇杆到指定位置
                moveRocker(mRockerPosition.x, mRockerPosition.y);
                break;
            case MotionEvent.ACTION_UP:// 抬起
            case MotionEvent.ACTION_CANCEL:// 移出区域
                // 回调结束
                mOnAngleChangeListener.onFinish();

                //移动摇杆到指定位置
                moveRocker(mCenterPoint.x, mCenterPoint.y);
                break;
        }
        return true;
    }

    /**
     * 获取摇杆实际要显示的位置（点），执行回调
     *
     * @param centerPoint  中心点
     * @param touchPoint   触摸点
     * @param regionRadius 摇杆可活动区域半径
     * @return 摇杆实际显示的位置（点）
     */
    private Point getRockerPositionPoint(Point centerPoint, Point touchPoint, float regionRadius) {
        // 两点在X轴的距离
        float lenX = (float) (touchPoint.x - centerPoint.x);
        // 两点在Y轴距离
        float lenY = (float) (touchPoint.y - centerPoint.y);
        // 两点距离
        float lenXY = (float) Math.sqrt((double) (lenX * lenX + lenY * lenY));
        //线速度
        double speed = MAX_SPEED;;
        if (lenXY < regionRadius){
            speed = MAX_SPEED * lenXY / regionRadius;
        }
        //Log.d(TAG, "getRockerPositionPoint: speed " + speed);

        // 计算弧度
        double radian = Math.acos(lenX / lenXY);
        if (radian < 0.2){
            radian = 0;
            speed = 0;
        } else if (radian < 0.5) {
            speed /= 2;
        } else if (radian < 1.37){

        } else if (radian < 1.77 ){
            radian = Math.PI / 2;
        } else if (radian < 2.64){
            speed /= 2;
        } else if (radian < 2.94){

        } else {
            radian = Math.PI;
            speed = 0;
        }
        double angularVelocity = MAX_ANGULAR_VELOCITY * (1 - radian / Math.PI * 2);
        //Log.d(TAG, "getRockerPositionPoint: radian " + angularVelocity);
        // 回调返回参数
        if (mOnAngleChangeListener != null){
            mOnAngleChangeListener.angle(speed, angularVelocity);
        }

        if (lenXY  <= regionRadius) { // 触摸位置在可活动范围内
            return touchPoint;
        } else { // 触摸位置在可活动范围以外
            // 计算要显示的位置
            radian = radian * (touchPoint.y > centerPoint.y ? 1 : -1);
            int showPointX = (int) (centerPoint.x + regionRadius * Math.cos(radian));
            int showPointY = (int) (centerPoint.y + regionRadius * Math.sin(radian));
            return new Point(showPointX, showPointY);
        }
    }

    /**
     * 移动摇杆到指定位置
     *
     * @param x x坐标
     * @param y y坐标
     */
    private void moveRocker(float x, float y) {
        mRockerPosition.set((int) x, (int) y);
        invalidate();
    }


    /**
     * 添加摇杆摇动角度的监听
     *
     * @param listener 回调接口
     */
    public void setOnAngleChangeListener(OnAngleChangeListener listener) {
        mOnAngleChangeListener = listener;
    }


    /**
     * 摇动角度的监听接口，记录移动速度，角度
     */
    public interface OnAngleChangeListener {
        // 开始
        void onStart();

        /**
         * 摇杆角度变化
         * @param lineSpeed 线速度
         * @param angularVelocity 角速度
         *
         */
        void angle(double lineSpeed, double angularVelocity);

        // 结束
        void onFinish();
    }
}
