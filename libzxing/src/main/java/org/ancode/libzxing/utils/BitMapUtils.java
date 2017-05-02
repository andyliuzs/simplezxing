package org.ancode.libzxing.utils;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.PixelFormat;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

/**
 * Created by andyliu on 17-5-2.
 */

public class BitMapUtils {


    public static Bitmap drawableToBitmap(Context context, int id) {
        Resources res = context.getResources();
        Bitmap bmp = BitmapFactory.decodeResource(res, id);
        return bmp;
    }

    public static Drawable bitmapToDrawable(Bitmap bitmap) {
        return new BitmapDrawable(bitmap);
    }




    public static Bitmap drawableToBitmap(Drawable drawable) {



        Bitmap bitmap = Bitmap.createBitmap(

                drawable.getIntrinsicWidth(),

                drawable.getIntrinsicHeight(),

                drawable.getOpacity() != PixelFormat.OPAQUE ? Bitmap.Config.ARGB_8888

                        : Bitmap.Config.RGB_565);

        Canvas canvas = new Canvas(bitmap);

        //canvas.setBitmap(bitmap);

        drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());

        drawable.draw(canvas);

        return bitmap;

    }


}
