package org.ancode.libzxing.config;

import android.os.Environment;

/**
 * Created by andyliu on 18-1-24.
 */

public class Constants {
    public static String cache_path=Environment.getExternalStorageDirectory() + "/temp/"+System.currentTimeMillis() + ".jpg";
}
