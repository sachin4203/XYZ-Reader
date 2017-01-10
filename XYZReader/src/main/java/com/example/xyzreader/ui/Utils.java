package com.example.xyzreader.ui;

import android.os.Build;

/**
 * Created by kapil pc on 1/6/2017.
 */

public class Utils {

    public static boolean isLollipopOrUp(){

            return Build.VERSION.SDK_INT>Build.VERSION_CODES.LOLLIPOP;
        }

}
