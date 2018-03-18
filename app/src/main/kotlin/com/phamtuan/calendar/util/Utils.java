package com.phamtuan.calendar.util;

import android.content.res.Resources;
import android.os.Build;
import android.support.v4.text.TextUtilsCompat;
import android.util.DisplayMetrics;

import java.util.Locale;

/**
 * Created by P.Tuan on 1/23/2018.
 */

public class Utils {
    public static int dpToPx(int dp) {
        DisplayMetrics metrics = Resources.getSystem().getDisplayMetrics();
        float px = dp * (metrics.densityDpi / DisplayMetrics.DENSITY_MEDIUM);
        return Math.round(px);
    }
    public static boolean isRTL() {
        return TextUtilsCompat.getLayoutDirectionFromLocale(Locale.getDefault())
                == android.support.v4.view.ViewCompat.LAYOUT_DIRECTION_RTL;
    }
    public static boolean hasJellyBeanMR2() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2;
    }

}
