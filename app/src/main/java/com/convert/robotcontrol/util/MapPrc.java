package com.convert.robotcontrol.util;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.support.annotation.ColorInt;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

//Digital map processing class, map data exists in one-dimensional array
public class MapPrc {
    private final String TAG = "MapPrc";
    private final int BLACK = 0;
    private final int WHITE = 255;
    private final int GRAY = 205;
    private final int  LINE_WIDTH = 3;
    private int mRow; //Picture height
    private int mCol; //Image width
    private int[] mGrayPixels; //Gray value, generally from 0 to 255, white is 255, black is 0
    private int[] mBinaryPixels; //二值图像 Binary image
    private @ColorInt
    int[] mPixels; //Color value, create an empty bitmap, mCol(width), mRow(height), Then use the specified color array mPixels to fill the color from left to right and from top to bottom
    //    private int[][] mMaskErode = {
//            {0, 0, 0},
//            {0, 0, 0},
//            {0, 0, 0}
//    };
//    private int[][] mMaskDilate = {
//            {0, 1, 0},
//            {1, 1, 1},
//            {0, 1, 0}
//    };
    private int[][] mMaskErode = {
            {0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0}
    };
    private int[][] mMaskDilate = {
            {0, 1, 1, 1, 0},
            {1, 1, 1, 1, 1},
            {1, 1, 1, 1, 1},
            {1, 1, 1, 1, 1},
            {0, 1, 1, 1, 0}
    };

    public MapPrc() {

    }

    public void initMap(InputStream is) {
        BufferedInputStream bis = new BufferedInputStream(is);
        try {
            //读取图片数据 i:0-51是其他数据...
            int size = bis.available();
            byte[] data = new byte[size];
            bis.read(data, 0, size);
            Log.i(TAG, "init: data size is " + size);

            int index = 0;
            for (; index < size; index++) {
                if (data[index] == (byte)-51) {
                    break;
                }
            }

            byte[] dataDescribe = new byte[index];
            System.arraycopy(data, 0, dataDescribe, 0, index);
            String temp = new String(dataDescribe);
            String [] dataArr = temp.split("\n");
//            下面是前四行数据
//            P5(格式)
//            # CREATOR: map_saver.cpp 0.050 m/pix
//            640 512(图片宽度、图片高度)
//            255(图片数据的最大值)
            String[] dataLW = dataArr[2].split(" ");
            mRow = Integer.parseInt(dataLW[1]);//512
            mCol = Integer.parseInt(dataLW[0]);//640
            mGrayPixels = new int[mRow * mCol];
            mBinaryPixels = new int[mRow * mCol];
            mPixels = new int[mRow * mCol];
            Log.i(TAG, "init: mCol :" + mCol);
            Log.i(TAG, "init: mRow :" + mRow);

            byte[] dataPixels = new byte[mRow * mCol];
            System.arraycopy(data, index, dataPixels, 0, size - index);
            for (int i = 0; i < mCol * mRow; i++) {
                int tmp = dataPixels[i];
                if (tmp < 0) {
                    tmp += 256;
                }
                mGrayPixels[i] = tmp;
                mBinaryPixels[i] = (tmp == BLACK) ? BLACK : WHITE;
                mPixels[i] = Color.rgb(tmp, tmp, tmp);
                //Log.d(TAG, "initMap: " + tmp + " i: " + i);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                bis.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public int getCol() {
        return mCol;
    }

    public int getRow() {
        return mRow;
    }

    public Bitmap getSrcMap() {
        if (mPixels == null) {
            Log.i(TAG, "getSrcMap: mPixels is null");
            return null;
        }
        return Bitmap.createBitmap(mPixels, mCol, mRow, Bitmap.Config.ARGB_8888);
    }

    //描边 Stroke
    public void stroke() {
        if (mGrayPixels == null) return;
        int oldPixel = mGrayPixels[0];
        //横向识别
        for (int i = 1; i < mGrayPixels.length; i++) {
            if (oldPixel == BLACK || mGrayPixels[i] == BLACK) {
                oldPixel = mGrayPixels[i];
                continue;
            } else if (mGrayPixels[i] != oldPixel) {
                oldPixel = mGrayPixels[i];
                mGrayPixels[i] = BLACK;
                mBinaryPixels[i] = BLACK;
                mPixels[i] = Color.rgb(BLACK, BLACK, BLACK);
            }
        }
        //竖向识别
        for (int i = 0; i < mCol; i++) {
            oldPixel = mGrayPixels[i];
            for (int j = 1; j < mRow; j++) {
                if (oldPixel == BLACK || mGrayPixels[i + j * mCol] == BLACK) {
                    oldPixel = mGrayPixels[i + j * mCol];
                    continue;
                } else if (mGrayPixels[i + j * mCol] != oldPixel) {
                    oldPixel = mGrayPixels[i + j * mCol];
                    mGrayPixels[i + j * mCol] = BLACK;
                    mBinaryPixels[i + j * mCol] = BLACK;
                    mPixels[i + j * mCol] = Color.rgb(BLACK, BLACK, BLACK);
                }
            }

        }
    }

    //二值化图像转彩图
    public void binary2Gray() {
        for (int i = 0; i < mRow * mCol; i++) {
            if (mBinaryPixels[i] == BLACK){
                mGrayPixels[i] = BLACK;
            } else if (mGrayPixels[i] == GRAY ||
                    (i > 0 && mGrayPixels[i - 1] == GRAY) ||
                    (i >= mCol && mGrayPixels[i - mCol] == GRAY)){
                mGrayPixels[i] = GRAY;
            } else {
                mGrayPixels[i] = WHITE;
            }
            //Correct gray pixels
            if (mGrayPixels[i] == GRAY &&
                    ((i > 0 && mGrayPixels[i - 1] == WHITE) ||
                    (i >= mCol && mGrayPixels[i - mCol] == WHITE))){
                mGrayPixels[i] = WHITE;
            }

            mPixels[i] = Color.rgb(mGrayPixels[i], mGrayPixels[i], mGrayPixels[i]);
        }
    }

    //腐蚀
    public void erode() {
        int mh = mMaskErode.length;
        int mw = mMaskErode[1].length;
        int sh = (mh + 1) / 2;
        int sw = (mw + 1) / 2;

        int[] d = new int[mCol * mRow];
        for (int i = 0; i < mCol * mRow; i++) {
            d[i] = WHITE;
        }

        for (int i = (mh - 1) / 2 + 1; i < mRow - (mh - 1) / 2; i++) {
            for (int j = (mw - 1) / 2 + 1; j < mCol - (mw - 1) / 2; j++) {
                int s = 0;

                for (int m = 0; m < mh; m++) {
                    for (int n = 0; n < mw; n++) {
                        if (mMaskErode[m][n] * 255 == mBinaryPixels[j + n - sw + (i + m - sh) * mCol])
                            s++;
                    }
                }

                d[j + i * mCol] = (s > 1) ? BLACK : WHITE;
            }
        }
        mBinaryPixels = d;
    }

    //膨胀
    public void dilate() {

        int mh = mMaskDilate.length;
        int mw = mMaskDilate[1].length;
        int sh = (mh + 1) / 2;
        int sw = (mw + 1) / 2;

        int[] d = new int[mCol * mRow];
        for (int i = 0; i < mCol * mRow; i++) {
            d[i] = WHITE;
        }

        for (int i = (mh - 1) / 2 + 1; i < mRow - (mh - 1) / 2; i++) {
            for (int j = (mw - 1) / 2 + 1; j < mCol - (mw - 1) / 2; j++) {
                int s = 0;

                for (int m = 0; m < mh; m++) {
                    for (int n = 0; n < mw; n++) {
                        if (mMaskDilate[m][n] * mBinaryPixels[j + n - sw + (i + m - sh) * mCol] == WHITE)
                            s = WHITE;
                    }
                }

                d[j + i * mCol] = s;
            }
        }

        mBinaryPixels = d;
    }

    //边界变细
    public void lineThinning(){
        if (mGrayPixels == null) return;
        int len = mGrayPixels.length;
        for (int i = 0; i < len; i++) {
            if (mGrayPixels[i] == BLACK) {
                int top = i - LINE_WIDTH * mCol, bottom = i + LINE_WIDTH * mCol, left = i - LINE_WIDTH, right = i + LINE_WIDTH;
                if (top > 0 && bottom < len && (left % mCol) > 0 && (i % mCol + LINE_WIDTH) < mRow) {
                    //If there are no white pixels on the top, bottom, left or right, turn the pixel into gray
                    if (mGrayPixels[top] != WHITE && mGrayPixels[bottom] != WHITE && mGrayPixels[left] != WHITE && mGrayPixels[right] != WHITE &&
                            mGrayPixels[top - LINE_WIDTH + 1 + mCol] != WHITE &&
                            mGrayPixels[top + LINE_WIDTH - 1 + mCol] != WHITE &&
                            mGrayPixels[bottom - LINE_WIDTH + 1 - mCol] != WHITE &&
                            mGrayPixels[bottom + LINE_WIDTH - 1 - mCol] != WHITE){
                        mGrayPixels[i] = GRAY;
                        mPixels[i] = Color.rgb(GRAY, GRAY, GRAY);
                    }
                }
            }
        }
    }

    //图像处理
    public void mapProcess() {
        stroke();
        erode();
        dilate();
        binary2Gray();
        lineThinning();
    }

}
