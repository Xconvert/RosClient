package com.convert.robotcontrol.callback;

import android.graphics.Bitmap;

public interface MapCallback {
    void updateMap(Bitmap bitmap);
    void updateThumb(Bitmap bitmap);
}
