package com.convert.robotcontrol.util;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Calendar;
import java.util.TimeZone;

public class ImageProvider {

    private final String TAG = "ImageProvider";
    private Activity mActivity;

    //相册请求码
    public static final int ALBUM_REQUEST_CODE = 1;
    //相机请求码
    public static final int CAMERA_REQUEST_CODE = 2;
    //调用照相机返回图片文件
    public static final String FILE_PATH =  Environment.getExternalStorageDirectory().getPath() + "/DCIM/Camera";
    public static Uri sTempImgUri;
    public static String sTempImgName;

    public ImageProvider(Activity activity){
        mActivity = activity;
        Log.d(TAG, "ImageProvider FILE_PATH : " + FILE_PATH);
    }

    //Get pictures from camera
    public void takePhoto() {

        sTempImgName = createImageName();
        //用于保存调用相机拍照后所生成的文件
        File file = new File(FILE_PATH, sTempImgName);
        //跳转到调用系统相机
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {   //如果在Android7.0以上,使用FileProvider获取Uri
            intent.setFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            sTempImgUri = FileProvider.getUriForFile(mActivity, "com.iconvert.imageprocessing.provider", file);

        } else {    //否则使用Uri.fromFile(file)方法获取Uri
            sTempImgUri = Uri.fromFile(file);
        }
        intent.putExtra(MediaStore.EXTRA_OUTPUT, sTempImgUri);
        mActivity.startActivityForResult(intent, CAMERA_REQUEST_CODE);
    }

    private static String createImageName() {
        Calendar cal = Calendar.getInstance();
        cal.setTimeZone(TimeZone.getTimeZone("GMT+8:00"));

        String year = String.valueOf(cal.get(Calendar.YEAR));
        String month = String.valueOf(cal.get(Calendar.MONTH));
        String day = String.valueOf(cal.get(Calendar.DATE));
        String hour;
        if (cal.get(Calendar.AM_PM) == 0)
            hour = String.valueOf(cal.get(Calendar.HOUR));
        else
            hour = String.valueOf(cal.get(Calendar.HOUR)+12);
        String minute = String.valueOf(cal.get(Calendar.MINUTE));
        String second = String.valueOf(cal.get(Calendar.SECOND));

        StringBuilder builder = new StringBuilder();
        builder.append("IMG_");
        builder.append(year);
        if (month.length() == 1){
            builder.append("0");
        }
        builder.append(month);
        if (day.length() == 1){
            builder.append("0");
        }
        builder.append(day);
        builder.append("_");

        if (hour.length() == 1){
            builder.append("0");
        }
        builder.append(hour);

        if (minute.length() == 1){
            builder.append("0");
        }
        builder.append(minute);

        if (second.length() == 1){
            builder.append("0");
        }
        builder.append(second);

        builder.append(".jpg");
        return builder.toString();
    }

    //Get an image from an album
    public void getPicFromAlbm() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        mActivity.startActivityForResult(intent, ALBUM_REQUEST_CODE);
    }

    //Save image to local
    public static void saveImage(Bitmap bmp) {
        File appDir = new File(FILE_PATH);
        if (!appDir.exists()) {
            appDir.mkdir();
        }
        sTempImgName = createImageName();
        File file = new File(appDir, sTempImgName);
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(file);
            bmp.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            fos.flush();
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (fos != null) {
                    fos.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

}
