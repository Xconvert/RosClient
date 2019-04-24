package com.convert.robotcontrol;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PointF;
import android.media.MediaScannerConnection;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;
import android.widget.Toast;

import com.convert.robotcontrol.callback.MapCallback;
import com.convert.robotcontrol.util.ImageProvider;
import com.convert.robotcontrol.util.MapPrc;

import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.convert.robotcontrol.util.ImageProvider.FILE_PATH;
import static com.convert.robotcontrol.util.ImageProvider.sTempImgName;

public class MapManager {

    private final String TAG = "MapManager";
    private final int RADIUS = 16;
    private int mRate = 1;//显示率

    private MapPrc mMapPrc; //数字地图加工类
    private Context mContext;
    private static Point sRobotPos; //机器人所在
    private static Point sDestPos; //目的地
    private double mOrientationAngle = 0;//0.0-1.0
    private Bitmap mSrcMap; //初始地图
    private Bitmap mDesMap; //要显示的地图
    private double mSrcMapWHRatio;//原图宽高比
    private double mPhoneLWHRatio;//手机宽高比
    private int mPhoneWidth;
    private int mPhoneHeight;
    private MapCallback mCallBack; // 用于更新图片
    //线程池
    private ExecutorService mSingleThreadExecutor = Executors.newSingleThreadExecutor();
    private ExecutorService mUpdateExecutor = Executors.newSingleThreadExecutor();

    //mDesMap 的图形参数, x y 为起点坐标
    private int x = 0;
    private int y = 0;
    private int width;
    private int height;
    private static double mMagnification = 1; //放大倍数 = 手机像素的宽（高） / 原图的的宽（高）
    private double mMaxMgn = 10; //最大放大倍数
    private double mMinMgn = 1; //最小放大倍数

    public MapManager(Context context) {
        if (context != null) mContext = context;
        init();
    }

    private void init() {
        sRobotPos = new Point();
        sDestPos = new Point();
        initMap();
    }

    private void initMap() {
        mMapPrc = new MapPrc();

        //pgm 图像的读取
        InputStream is = mContext.getResources().openRawResource(R.raw.exp0);
        //获取原始地图
        mMapPrc.initMap(is);
        //加工地图
        mMapPrc.mapProcess();
        //获取原图像
        mSrcMap = mMapPrc.getSrcMap();
        width = mMapPrc.getCol();
        height = mMapPrc.getRow();
        //宽 / 高
        mSrcMapWHRatio = width / height;
        //初步加工目标图像，裁剪至适合手机屏幕
//此处没包括虚拟键盘，所以注释
//        DisplayMetrics dm = mContext.getResources().getDisplayMetrics();
//        int h = dm.heightPixels;
//        int w = dm.widthPixels;
        WindowManager windowManager = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        if (windowManager != null) {
            final Display display = windowManager.getDefaultDisplay();
            Point outPoint = new Point();
            display.getRealSize(outPoint);
            int w = outPoint.x;//手机屏幕真实宽度
            int h = outPoint.y;//手机屏幕真实高度
            mPhoneWidth = w;
            mPhoneHeight = h;
            if (w > 1920) {
                mRate = 2;
            }
            Log.i(TAG, "init: phone height is: " + h);
            Log.i(TAG, "init: phone width is: " + w);
            mPhoneLWHRatio = 1.0 * w / h;
            if (mPhoneLWHRatio > mSrcMapWHRatio) {
                //截掉上下两部分, width 不变
                int newHeight = (int) (width / mPhoneLWHRatio);
                x = 0;
                y = (height - newHeight) / 2;
                height = newHeight;
                //记录放大倍数
                mMagnification = w / width;
                mMinMgn = mMagnification;
            } else {
                //截掉左右两部分
                int newWidth = (int) (height * mPhoneLWHRatio);
                y = 0;
                x = (width - newWidth) / 2;
                width = newWidth;
                mMagnification = h / height;
                mMinMgn = mMagnification;
            }

            mDesMap = Bitmap.createBitmap(mSrcMap, x, y, width, height);
        }
    }

    public void registerCallback(MapCallback callBack) {
        mCallBack = callBack;
    }

    public void unRegisterCallback() {
        mCallBack = null;
    }

    public Bitmap getSrcMap() {
        return mSrcMap;
    }

    public Bitmap getDesMap() {
        return mDesMap;
    }

    public Bitmap getThumbnails() {
        //获取正方形略缩图
        return Bitmap.createBitmap(mDesMap, (mDesMap.getWidth() - mDesMap.getHeight()) / 2, 0, mDesMap.getHeight(), mDesMap.getHeight());
    }

    private void updateThumb() {
        //在video界面删除标记点时候用到
        mUpdateExecutor.execute(new Runnable() {
            @Override
            public void run() {
                mCallBack.updateThumb(Bitmap.createBitmap(mDesMap, (mDesMap.getWidth() - mDesMap.getHeight()) / 2, 0, mDesMap.getHeight(), mDesMap.getHeight()));
            }
        });
    }

    private int checkDxBound(float dx) {
        dx /= mMagnification;
        if (dx > 0 && dx > x) {
            dx = x;
        } else if (dx < 0 && -dx > (mMapPrc.getCol() - x - width)) {
            //dx = mMapPrc.getCol() - x - width;
            dx = 0;//去抖
        }
        return (int) dx;
    }

    private int checkDyBound(float dy) {
        dy /= mMagnification;
        if (dy > 0 && dy > y) {
            dy = y;
        } else if (dy < 0 && -dy > (mMapPrc.getRow() - y - height)) {
            //dy = mMapPrc.getRow() - y - height;//该方法有抖动，因为 y 和 height 是整数，不是精确值
            dy = 0;//去抖
        }
        return (int) dy;
    }

    //地图成像，单一线程执行，保持图片刷新时序性
    private void updateMap(final int x, final int y, final int width, final int height) {
        mUpdateExecutor.execute(new Runnable() {
            @Override
            public void run() {
                Matrix m = new Matrix();
                //把图片放大后再显示，是 mMagnification , 但是像素太高，卡顿，此处取一半 mRate
                float scale = (float) (mMagnification / mRate);
                if (width != mPhoneWidth || height != mPhoneHeight) {
                    final float sx = scale;
                    final float sy = scale;
                    m.setScale(sx, sy);
                }
                //此处 mDesMap 与手机宽高一样
                mDesMap = Bitmap.createBitmap(mSrcMap, x, y, width, height, m, false);
                if (sDestPos.x != 0 || sDestPos.y != 0) {
                    //如果目的地存在，画目的地
                    Canvas canvas = new Canvas(mDesMap);
                    Paint paint = new Paint();
                    paint.setColor(Color.BLUE);
                    canvas.drawCircle((int) ((sDestPos.x - x) * scale), (int) ((sDestPos.y - y) * scale), RADIUS, paint);
                }
                if (isSetRobotPoint()) {
                    //如果 robot 地存在，画 robot
                    int centerX = (int) ((sRobotPos.x - x) * scale);
                    int centerY = (int) ((sRobotPos.y - y) * scale);
                    Canvas canvas = new Canvas(mDesMap);
                    Paint paint = new Paint();
                    paint.setColor(Color.RED);
                    canvas.drawCircle(centerX, centerY, RADIUS, paint);
                    //画方向
                    Paint paintOrientation = new Paint();
                    paintOrientation.setColor(Color.BLUE);
                    //center point
                    canvas.drawCircle(centerX, centerY, RADIUS / 8, paintOrientation);
                    //point on circle
                    double radian = mOrientationAngle * Math.PI * 2;
                    int x = (int) (centerX + RADIUS * Math.cos(radian));
                    int y = (int) (centerY - RADIUS * Math.sin(radian));
                    canvas.drawCircle(x, y, RADIUS / 4, paintOrientation);
                    //draw line
                    canvas.drawLine(centerX, centerY, x, y, paintOrientation);
                }
                if (mCallBack != null) {
                    mCallBack.updateMap(mDesMap);
                }
            }
        });
    }

    //移动，两个参数是移动距离
    public void moveMap(float dx, float dy) {
        x -= checkDxBound(dx);
        y -= checkDyBound(dy);
        updateMap(x, y, width, height);
    }

    //缩放，center：不动点，scale：倍数
    public void zoom(final PointF center, final float scale) {
        mSingleThreadExecutor.execute(new Runnable() {
            @Override
            public void run() {
                //新的放大倍数
                double magnification = mMagnification * scale;
                if (center == null || magnification > mMaxMgn || magnification < mMinMgn) {
                    return;
                }
                //求不动点在 map 的位置
                PointF cInMap = new PointF(x + (float) (center.x / mMagnification),
                        y + (float) (center.y / mMagnification));
                //求 x y
                x = (int) (cInMap.x - center.x / magnification);
                y = (int) (cInMap.y - center.y / magnification);
                width = (int) (mPhoneWidth / magnification);
                height = (int) (mPhoneHeight / magnification);
                //订正 mMagnification x y
                mMagnification = magnification;
                if (x < 0) x = 0;
                if (x + width > mMapPrc.getCol()) x = mMapPrc.getCol() - width;
                if (y < 0) y = 0;
                if (y + height > mMapPrc.getRow()) y = mMapPrc.getRow() - height;
                updateMap(x, y, width, height);
            }
        });
    }

    public void logMapArr() {
        Log.d(TAG, "map: x is " + x + ", y is " + y + ", width is " + width + ", height is " + height + ", 放大倍数: " + mMagnification);

    }

    public void setDesPos(final PointF point) {
        mSingleThreadExecutor.execute(new Runnable() {
            @Override
            public void run() {
                int xInMap = (int) (point.x / mMagnification + x);
                int yInMap = (int) (point.y / mMagnification + y);
                sDestPos.set(xInMap, yInMap);
                updateMap(x, y, width, height);
                Log.i(TAG, "setDesPos: x is " + xInMap + ", y is " + yInMap);
            }
        });
    }

    public void setRobotPos(final Point point) {
        mSingleThreadExecutor.execute(new Runnable() {
            @Override
            public void run() {
                int xInMap = point.x;
                int yInMap = point.y;
                sRobotPos.set(xInMap, yInMap);
                updateMap(x, y, width, height);
                updateThumb();
                Log.d(TAG, "setRobotPos: x is " + xInMap + ", y is " + yInMap);
            }
        });
    }

    public Point getRobotPos() {
        return sRobotPos;
    }

    public Point getDestPos() {
        return sDestPos;
    }

    //根据点击屏幕像素位置设置机器人位置
    public void setRobotPos(PointF pos) {
        int xInMap = (int) (pos.x / mMagnification + x);
        int yInMap = (int) (pos.y / mMagnification + y);
        setRobotPos(new Point(xInMap, yInMap));
    }

    //姿态估计时候改变、返回数据时候改变
    public void setOrientationAngle(double mOrientationAngle) {
        this.mOrientationAngle = mOrientationAngle;
        updateMap(x, y, width, height);
    }

    public double getOrientationAngle() {
        return mOrientationAngle;
    }

    public void clearDesPos() {
        mSingleThreadExecutor.execute(new Runnable() {
            @Override
            public void run() {
                sDestPos.set(0, 0);
                updateMap(x, y, width, height);
                //更新略缩图
                updateThumb();
            }
        });
    }

    //Save image to file
    public void saveMapToFile(){
        if (mSrcMap == null) {
            Log.i(TAG, "saveMapToFile: mSrcMap is null");
            return;
        }
        ImageProvider.saveImage(mSrcMap);
        //Update album
        MediaScannerConnection.scanFile(mContext, new String[]{FILE_PATH + "/" + sTempImgName}, null, null);
        Toast.makeText(mContext, "地图已保存", Toast.LENGTH_SHORT).show();

    }

    public boolean isSetRobotPoint(){
        // if robot point has set, return true
        return sRobotPos.x != 0 || sRobotPos.y != 0;
    }

    public int getMapWidth(){
        return mMapPrc.getCol();
    }

    public int getMapHeight(){
        return mMapPrc.getRow();
    }


}
