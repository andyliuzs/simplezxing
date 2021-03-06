package org.ancode.libzxing.decode;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.support.v7.view.ContextThemeWrapper;

import org.ancode.libzxing.R;


/**
 * 二维码解析管理。
 */
@SuppressLint("RestrictedApi")
public class DecodeManager {

    public void showPermissionDeniedDialog(Context context) {
        // 权限在安装时被关闭了，如小米手机
        new AlertDialog.Builder(new ContextThemeWrapper(context, R.style.CustomDialogStyle)).setTitle(R.string.qr_code_notification)
                .setMessage(R.string.qr_code_camera_not_open)
                .setPositiveButton(R.string.qr_code_positive_button_know, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        dialog = null;
                    }
                }).show();
    }


    public void showCouldNotReadQrCodeFromScanner(Context context, final OnRefreshCameraListener listener) {
        new AlertDialog.Builder(new ContextThemeWrapper(context, R.style.CustomDialogStyle)).setTitle(R.string.qr_code_notification)
                .setMessage(R.string.qr_code_could_not_read_qr_code_from_scanner)
                .setPositiveButton(R.string.qc_code_close, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        if (listener != null) {
                            listener.refresh();
                        }
                    }
                }).show();
    }

    public void showCouldNotReadQrCodeFromPicture(Context context) {
        new AlertDialog.Builder(new ContextThemeWrapper(context, R.style.CustomDialogStyle)).setTitle(R.string.qr_code_notification)
                .setMessage(R.string.qr_code_could_not_read_qr_code_from_picture)
                .setPositiveButton(R.string.qc_code_close, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                }).show();
    }

    public interface OnRefreshCameraListener {
        void refresh();
    }

}
