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

import static android.graphics.Color.BLUE;

public class OrientationView extends View {
    private static final String TAG = "OrientationView";

    private static final int DEFAULT_SIZE = 200;
    private static final int DEFAULT_POINT_RADIUS = 16;

    private Paint mAreaBackgroundPaint;
    private Paint mPointPaint;
    private Paint mLinePaint;

    private Point mPointPosition;
    private Point mCenterPoint;

    private int mAreaRadius;

    private OnAngleChangeListener mOnAngleChangeListener;

    // 摇杆可移动区域背景
    private Bitmap mAreaBitmap;

    public OrientationView(Context context, AttributeSet attrs) {
        super(context, attrs);

        // 获取自定义属性
        initAttribute(context, attrs);

        mAreaBackgroundPaint = new Paint();
        mAreaBackgroundPaint.setAntiAlias(true);

        mPointPaint = new Paint();
        mPointPaint.setColor(BLUE);
        mPointPaint.setAntiAlias(true);

        mLinePaint = new Paint();
        mLinePaint.setColor(BLUE);
        mLinePaint.setStrokeWidth(DEFAULT_POINT_RADIUS / 3);
        mLinePaint.setAntiAlias(true);

        // 中心点
        mCenterPoint = new Point();

        mPointPosition = new Point();
    }


    private void initAttribute(Context context, AttributeSet attrs) {
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.OrientationView);

        // 可移动区域背景
        Drawable areaBackground = typedArray.getDrawable(R.styleable.OrientationView_background);
        if (null != areaBackground) {
            // 设置背景
            mAreaBitmap = ((BitmapDrawable) areaBackground).getBitmap();
        }
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
        mAreaRadius -= DEFAULT_POINT_RADIUS;

        // 位置
        if (0 == mPointPosition.x || 0 == mPointPosition.y) {
            mPointPosition.set(mCenterPoint.x + mAreaRadius, mCenterPoint.y);
        }

        // 画可移动区域
        Rect src1 = new Rect(0, 0, mAreaBitmap.getWidth(), mAreaBitmap.getHeight());
        Rect dst1 = new Rect(mCenterPoint.x - mAreaRadius, mCenterPoint.y - mAreaRadius, mCenterPoint.x + mAreaRadius, mCenterPoint.y + mAreaRadius);
        canvas.drawBitmap(mAreaBitmap, src1, dst1, mAreaBackgroundPaint);

        // 画点
        canvas.drawCircle(mPointPosition.x, mPointPosition.y, DEFAULT_POINT_RADIUS, mPointPaint);
        canvas.drawCircle(mCenterPoint.x, mCenterPoint.y, DEFAULT_POINT_RADIUS / 2, mPointPaint);
        // 画线
        canvas.drawLine(mCenterPoint.x, mCenterPoint.y, mPointPosition.x, mPointPosition.y, mLinePaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:// 按下
                // 回调开始
                if (mOnAngleChangeListener != null) {
                    mOnAngleChangeListener.onStart();
                }
            case MotionEvent.ACTION_MOVE:// 移动
                float moveX = event.getX();
                float moveY = event.getY();
                mPointPosition = getPositionPoint(mCenterPoint, new Point((int) moveX, (int) moveY), mAreaRadius);
                //移动摇杆到指定位置
                movePoint(mPointPosition.x, mPointPosition.y);
                break;
            case MotionEvent.ACTION_UP:// 抬起
            case MotionEvent.ACTION_CANCEL:// 移出区域
                // 回调结束
                if (mOnAngleChangeListener != null) {
                    mOnAngleChangeListener.onFinish();
                }
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
    private Point getPositionPoint(Point centerPoint, Point touchPoint, float regionRadius) {
        // 两点在X轴的距离
        float lenX = (float) (touchPoint.x - centerPoint.x);
        // 两点在Y轴距离
        float lenY = (float) (touchPoint.y - centerPoint.y);
        // 两点距离
        float lenXY = (float) Math.sqrt((double) (lenX * lenX + lenY * lenY));

        //Log.d(TAG, "getRockerPositionPoint: speed " + speed);

        // 计算弧度
        double radian = Math.acos(lenX / lenXY);
        if (touchPoint.y > centerPoint.y) {
            radian = Math.PI * 2 - radian;
        }

        Log.d(TAG, "getPointPositionPoint: radian " + radian);
        // 回调返回参数
        if (mOnAngleChangeListener != null) {
            double stdAngle = radian / (Math.PI * 2);
//      The value of angle is supposed to be between 0.0 and 1.0.
//      It corresponds to direction as follows:
//                  0.25
//                    ^
//                    |
//            0.5 <-- * --> 0.0/1.0
//                    |
//                    v
//                  0.75
            mOnAngleChangeListener.angle(stdAngle);
        }

        // 计算要显示的位置
        int showPointX = (int) (centerPoint.x + regionRadius * lenX / lenXY);
        int showPointY = (int) (centerPoint.y + regionRadius * lenY / lenXY);
        return new Point(showPointX, showPointY);
    }

    /**
     * 移动到指定位置
     *
     * @param x x坐标
     * @param y y坐标
     */
    private void movePoint(float x, float y) {
        mPointPosition.set((int) x, (int) y);
        invalidate();
    }

    /**
     * 移动摇杆到指定位置
     *
     * @param angle 0.0-1.0
     */
    public void setAngle(double angle) {
        double radian = angle * Math.PI * 2;
        int x = (int) (mCenterPoint.x + mAreaRadius * Math.cos(radian));
        int y = (int) (mCenterPoint.y - mAreaRadius * Math.sin(radian));
        mPointPosition.set(x, y);
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
         * 角度变化
         *
         * @param stdAngle The value of angle is supposed to be between 0.0 and 1.0.
         *                 It corresponds to direction as follows:
         *                 0.25
         *                 ^
         *                 |
         *                 0.5 <-- * --> 0.0/1.0
         *                 |
         *                 v
         *                 0.75
         */
        void angle(double stdAngle);

        // 结束
        void onFinish();
    }
}
