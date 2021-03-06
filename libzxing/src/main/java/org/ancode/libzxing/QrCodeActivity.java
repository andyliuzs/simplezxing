package org.ancode.libzxing;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.Result;
import com.jph.takephoto.app.TakePhoto;
import com.jph.takephoto.app.TakePhotoActivity;
import com.jph.takephoto.app.TakePhotoImpl;
import com.jph.takephoto.model.InvokeParam;
import com.jph.takephoto.model.TContextWrap;
import com.jph.takephoto.model.TResult;
import com.jph.takephoto.permission.InvokeListener;
import com.jph.takephoto.permission.PermissionManager;
import com.jph.takephoto.permission.TakePhotoInvocationHandler;


import org.ancode.libzxing.camera.CameraManager;
import org.ancode.libzxing.decode.CaptureActivityHandler;
import org.ancode.libzxing.decode.DecodeImageCallback;
import org.ancode.libzxing.decode.DecodeImageThread;
import org.ancode.libzxing.decode.DecodeManager;
import org.ancode.libzxing.decode.InactivityTimer;
import org.ancode.libzxing.view.MyToolBar;
import org.ancode.libzxing.view.QrCodeFinderView;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * 二维码扫描类。
 */
public class QrCodeActivity extends AppCompatActivity implements Callback, OnClickListener, TakePhoto.TakeResultListener, InvokeListener {
    private static final String TAG = "QRCODE";
    private static final int REQUEST_SYSTEM_PICTURE = 0;
    private static final int REQUEST_PICTURE = 1;
    public static final int MSG_DECODE_SUCCEED = 1;
    public static final int MSG_DECODE_FAIL = 2;
    private CaptureActivityHandler mCaptureActivityHandler;
    private boolean mHasSurface;
    private boolean mPermissionOk;
    private InactivityTimer mInactivityTimer;
    private QrCodeFinderView mQrCodeFinderView;
    private SurfaceView mSurfaceView;
    private final DecodeManager mDecodeManager = new DecodeManager();
    /**
     * 声音和振动相关参数
     */
    private static final float BEEP_VOLUME = 0.10f;
    private static final long VIBRATE_DURATION = 200L;
    private MediaPlayer mMediaPlayer;
    private boolean mPlayBeep;
    private boolean mVibrate;
    private boolean mNeedFlashLightOpen = true;
    private Executor mQrCodeExecutor;
    private Handler mHandler;
    private TextView tv_torch_view;
    private TextView qrcode_from_img;
    private String mTitle;
    private int bColor = -1;
    private static final String QR_TITLE = "qr_title";
    private static final String QR_MAIN_COLOR = "qr_main_color";
    /*****takephoto****/
    private TakePhoto takePhoto;
    private InvokeParam invokeParam;

    private static QrCodeCallBack qrCodeCallBack;

    public interface QrCodeCallBack {
        public void handleResult(QrCodeActivity activity, String code);

        public void cancel();

    }

    public static void launcher(Activity activity, String title, int color, int requestCode) {
        Intent intent = new Intent(activity, QrCodeActivity.class);
        intent.putExtra(QrCodeActivity.QR_TITLE, title);
        intent.putExtra(QrCodeActivity.QR_MAIN_COLOR, color);
        activity.startActivityForResult(intent, requestCode);
        activity.overridePendingTransition(R.anim.push_left_in, R.anim.push_not_move_out);
    }

    public static void launcherWithCallBack(Activity activity, String title, int color, QrCodeCallBack callBack) {
        QrCodeActivity.qrCodeCallBack = callBack;
        Intent intent = new Intent(activity, QrCodeActivity.class);
        intent.putExtra(QrCodeActivity.QR_TITLE, title);
        intent.putExtra(QrCodeActivity.QR_MAIN_COLOR, color);
        activity.startActivity(intent);
        activity.overridePendingTransition(R.anim.push_left_in, R.anim.push_not_move_out);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getTakePhoto().onCreate(savedInstanceState);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qr_code);
        Intent intent = getIntent();
        if (intent != null) {
            mTitle = intent.getStringExtra(QR_TITLE);
            bColor = intent.getIntExtra(QR_MAIN_COLOR, -1);
        }
        initView();
        initData();


    }

    public void setNetReachable(boolean reachable) {
        if (mQrCodeFinderView != null) {
            mQrCodeFinderView.netReachable(reachable);
        }
    }


    @Override
    protected void onSaveInstanceState(Bundle outState) {
        getTakePhoto().onSaveInstanceState(outState);
        super.onSaveInstanceState(outState);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            //点左上角返回关闭

            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);

    }

    private void initToolBar() {
        MyToolBar toolbar = (MyToolBar) findViewById(R.id.toolbar);
        if (bColor != -1) {
            toolbar.setBackgroundColor(bColor);
        }
        setSupportActionBar(toolbar);
        setTitle(mTitle);
//        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
//        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true); //设置返回键可用
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    private void checkPermission() {
        boolean hasHardware = checkCameraHardWare(this);
        if (hasHardware) {
            if (!hasCameraPermission()) {
                findViewById(R.id.qr_code_view_background).setVisibility(View.VISIBLE);
                mQrCodeFinderView.setVisibility(View.GONE);
                mPermissionOk = false;
            } else {
                mPermissionOk = true;
            }
        } else {
            mPermissionOk = false;

            finish();
        }
    }

    private void initView() {
        initToolBar();
        mQrCodeFinderView = (QrCodeFinderView) findViewById(R.id.qr_code_view_finder);
        mSurfaceView = (SurfaceView) findViewById(R.id.qr_code_preview_view);
        mHasSurface = false;
        tv_torch_view = (TextView) findViewById(R.id.tv_torch_view);
        tv_torch_view.setOnClickListener(this);
        qrcode_from_img = (TextView) findViewById(R.id.qrcode_from_img);
        qrcode_from_img.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
//                openSystemAlbum();
//                takePhoto.onPickFromDocuments();
                takePhoto.onPickMultiple(1);
            }
        });
        tv_torch_view.setVisibility(View.GONE);
        qrcode_from_img.setVisibility(View.GONE);
    }

    private void initData() {
        CameraManager.init(this);
        mInactivityTimer = new InactivityTimer(QrCodeActivity.this);
        mQrCodeExecutor = Executors.newSingleThreadExecutor();
        mHandler = new WeakHandler(this);
    }

    private boolean hasCameraPermission() {
        PackageManager pm = getPackageManager();
        return PackageManager.PERMISSION_GRANTED == pm.checkPermission("android.permission.CAMERA", getPackageName());
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkPermission();
        if (!mPermissionOk) {
            mDecodeManager.showPermissionDeniedDialog(this);
            return;
        }
        SurfaceHolder surfaceHolder = mSurfaceView.getHolder();
        turnFlashLightOff();
        if (mHasSurface) {
            initCamera(surfaceHolder);
        } else {
            surfaceHolder.addCallback(this);
            surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        }

        mPlayBeep = true;
        AudioManager audioService = (AudioManager) getSystemService(AUDIO_SERVICE);
        if (audioService.getRingerMode() != AudioManager.RINGER_MODE_NORMAL) {
            mPlayBeep = false;
        }
        initBeepSound();
        mVibrate = true;
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mCaptureActivityHandler != null) {
            mCaptureActivityHandler.quitSynchronously();
            mCaptureActivityHandler = null;
        }
        CameraManager.get().closeDriver();
    }

    @Override
    protected void onDestroy() {
        if (null != mInactivityTimer) {
            mInactivityTimer.shutdown();
        }
        if(qrCodeCallBack!=null){
            qrCodeCallBack.cancel();
            qrCodeCallBack = null;
        }
        super.onDestroy();
        hideWaitDialog();
    }

    /**
     * Handler scan result
     *
     * @param result
     */
    public void handleDecode(Result result) {
        mInactivityTimer.onActivity();
        playBeepSoundAndVibrate();
        if (null == result) {
            mDecodeManager.showCouldNotReadQrCodeFromScanner(this, new DecodeManager.OnRefreshCameraListener() {
                @Override
                public void refresh() {
                    restartPreview();
                }
            });
        } else {
            String resultString = result.getText();
            handleResult(resultString);
        }
    }

    private void initCamera(SurfaceHolder surfaceHolder) {
        try {
            CameraManager.get().openDriver(surfaceHolder);
        } catch (IOException e) {
            // 基本不会出现相机不存在的情况
            Toast.makeText(this, getString(R.string.qr_code_camera_not_found), Toast.LENGTH_SHORT).show();
            finish();
            return;
        } catch (RuntimeException re) {
            re.printStackTrace();
            mDecodeManager.showPermissionDeniedDialog(this);
            return;
        }
        mQrCodeFinderView.setVisibility(View.VISIBLE);
        mSurfaceView.setVisibility(View.VISIBLE);
//        mLlFlashLight.setVisibility(View.VISIBLE);
        findViewById(R.id.qr_code_view_background).setVisibility(View.GONE);
        if (mCaptureActivityHandler == null) {
            mCaptureActivityHandler = new CaptureActivityHandler(this);
        }
    }

    private void restartPreview() {
        if (null != mCaptureActivityHandler) {
            mCaptureActivityHandler.restartPreviewAndDecode();
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    /* 检测相机是否存在 */
    private boolean checkCameraHardWare(Context context) {
        PackageManager packageManager = context.getPackageManager();
        return packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (!mHasSurface) {
            mHasSurface = true;
            initCamera(holder);
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        mHasSurface = false;
    }

    public Handler getCaptureActivityHandler() {
        return mCaptureActivityHandler;
    }

    private void initBeepSound() {
        if (mPlayBeep && mMediaPlayer == null) {
            // The volume on STREAM_SYSTEM is not adjustable, and users found it too loud,
            // so we now play on the music stream.
            setVolumeControlStream(AudioManager.STREAM_MUSIC);
            mMediaPlayer = new MediaPlayer();
            mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mMediaPlayer.setOnCompletionListener(mBeepListener);

            AssetFileDescriptor file = getResources().openRawResourceFd(R.raw.beep);
            try {
                mMediaPlayer.setDataSource(file.getFileDescriptor(), file.getStartOffset(), file.getLength());
                file.close();
                mMediaPlayer.setVolume(BEEP_VOLUME, BEEP_VOLUME);
                mMediaPlayer.prepare();
            } catch (IOException e) {
                mMediaPlayer = null;
            }
        }
    }

    private void playBeepSoundAndVibrate() {
        if (mPlayBeep && mMediaPlayer != null) {
            mMediaPlayer.start();
        }
        if (mVibrate) {
            Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
            vibrator.vibrate(VIBRATE_DURATION);
        }
    }

    /**
     * When the beep has finished playing, rewind to queue up another one.
     */
    private final MediaPlayer.OnCompletionListener mBeepListener = new MediaPlayer.OnCompletionListener() {
        public void onCompletion(MediaPlayer mediaPlayer) {
            mediaPlayer.seekTo(0);
        }
    };

    @Override
    public void onClick(View v) {
        int i = v.getId();
        if (i == R.id.tv_torch_view) {
            if (mNeedFlashLightOpen) {
                turnFlashlightOn();
            } else {
                turnFlashLightOff();
            }

        }
    }

    private void openSystemAlbum() {
        Intent intent = new Intent();
        /* 开启Pictures画面Type设定为image */
        intent.setType("image/*");
        /* 使用Intent.ACTION_GET_CONTENT这个Action */
        intent.setAction(Intent.ACTION_GET_CONTENT);//本action会调用出文件存储选择
//        intent.setAction(Intent.ACTION_PICK);//本action只会显示出带有图片的路径
        /* 取得相片后返回本画面 */
        startActivityForResult(intent, REQUEST_SYSTEM_PICTURE);
    }

    private void turnFlashlightOn() {
        mNeedFlashLightOpen = false;
        tv_torch_view.setText(getString(R.string.close_light));
        CameraManager.get().setFlashLight(true);
    }

    private void turnFlashLightOff() {
        mNeedFlashLightOpen = true;
        tv_torch_view.setText(getString(R.string.open_light));
        CameraManager.get().setFlashLight(false);
    }

    private void handleResult(String resultString) {
        if (TextUtils.isEmpty(resultString)) {
            mDecodeManager.showCouldNotReadQrCodeFromScanner(this, new DecodeManager.OnRefreshCameraListener() {
                @Override
                public void refresh() {
                    restartPreview();
                }
            });
        } else {
            if (qrCodeCallBack != null) {
                qrCodeCallBack.handleResult(this, resultString);
            } else {
                Intent resultIntent = new Intent();
                Bundle bundle = new Bundle();
                bundle.putString("result", resultString);
                resultIntent.putExtras(bundle);
                setResult(RESULT_OK, resultIntent);
                QrCodeActivity.this.finish();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, final Intent data) {
        getTakePhoto().onActivityResult(requestCode, resultCode, data);
//        if (resultCode != RESULT_OK) {
//            return;
//        }
//        switch (requestCode) {
//            case REQUEST_PICTURE:
//                finish();
//                break;
//            case REQUEST_SYSTEM_PICTURE:
//                try {
//                    Uri uri = data.getData();
//                    if (null != uri) {
//                        String imgPath = RealPathUtil.getRealFilePath(this, uri);
//                        Log.e("QRCODE", "imgpath =" + imgPath);
//                        if (null != mQrCodeExecutor && !TextUtils.isEmpty(imgPath)) {
//                            mQrCodeExecutor.execute(new DecodeImageThread(imgPath, mDecodeImageCallback));
//                        }
//                    }
//                } catch (Exception e) {
//                    Log.e("QRCODE", "get file path error", e);
//                    e.printStackTrace();
//                }
//
//                break;
//        }
    }

    private DecodeImageCallback mDecodeImageCallback = new DecodeImageCallback() {
        @Override
        public void decodeSucceed(Result result) {
            Log.i("QRCODE", "success " + result.getText());
            mHandler.obtainMessage(MSG_DECODE_SUCCEED, result).sendToTarget();
        }

        @Override
        public void decodeFail(int type, String reason) {
            mHandler.sendEmptyMessage(MSG_DECODE_FAIL);
            Log.i("QRCODE", "failed " + reason);
        }
    };


    private static class WeakHandler extends Handler {
        private WeakReference<QrCodeActivity> mWeakQrCodeActivity;
        private DecodeManager mDecodeManager = new DecodeManager();

        public WeakHandler(QrCodeActivity imagePickerActivity) {
            super();
            this.mWeakQrCodeActivity = new WeakReference<>(imagePickerActivity);
        }

        @Override
        public void handleMessage(Message msg) {
            QrCodeActivity qrCodeActivity = mWeakQrCodeActivity.get();
            switch (msg.what) {
                case MSG_DECODE_SUCCEED:
                    Result result = (Result) msg.obj;
                    Log.i("QRCODE", "scan success reault = " + result);
                    if (null == result) {
                        mDecodeManager.showCouldNotReadQrCodeFromPicture(qrCodeActivity);
                    } else {
                        String resultString = result.getText();
                        handleResult(resultString);
                    }
                    break;
                case MSG_DECODE_FAIL:
                    mDecodeManager.showCouldNotReadQrCodeFromPicture(qrCodeActivity);
                    break;
            }
            super.handleMessage(msg);
        }

        private void handleResult(String resultString) {
            QrCodeActivity imagePickerActivity = mWeakQrCodeActivity.get();
            //图片解析成功
            if (TextUtils.isEmpty(resultString)) {
//                Toast.makeText(imagePickerActivity, "扫描结果为空，请重试！", Toast.LENGTH_SHORT).show();
                Log.v("QRCODE", "scan result is null");
            } else {
                Intent resultIntent = new Intent();
                Bundle bundle = new Bundle();
                bundle.putString("result", resultString);
                resultIntent.putExtras(bundle);
                imagePickerActivity.setResult(RESULT_OK, resultIntent);
                imagePickerActivity.finish();
            }
        }

    }

    /*****takephoto****/
    @Override
    public void takeSuccess(TResult result) {
        String path = null;

        if (result != null) {

            path = result.getImage().getCompressPath();
            if (TextUtils.isEmpty(path)) {
                path = result.getImage().getOriginalPath();
            }

            if (TextUtils.isEmpty(path)) {
                Log.e(TAG, "take path is null");
                Toast.makeText(this, "获取图片路径失败!", Toast.LENGTH_SHORT).show();
                return;
            }
            if (null != mQrCodeExecutor && !TextUtils.isEmpty(path)) {
                mQrCodeExecutor.execute(new DecodeImageThread(path, mDecodeImageCallback));
            }
        } else {
            Log.e(TAG, "take result  is null");
            Toast.makeText(this, "获取图片路径失败!", Toast.LENGTH_SHORT).show();
        }
        Log.i(TAG, "takeSuccess：" + path);
    }

    @Override
    public void takeFail(TResult result, String msg) {
        Log.i(TAG, "takeFail:" + msg);
        Toast.makeText(this, "获取图片失败!", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void takeCancel() {
        Log.i(TAG, getResources().getString(com.jph.takephoto.R.string.msg_operation_canceled));
    }

    @Override
    public PermissionManager.TPermissionType invoke(InvokeParam invokeParam) {
        PermissionManager.TPermissionType type = PermissionManager.checkPermission(TContextWrap.of(this), invokeParam.getMethod());
        if (PermissionManager.TPermissionType.WAIT.equals(type)) {
            this.invokeParam = invokeParam;
        }
        return type;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        //以下代码为处理Android6.0、7.0动态权限所需
        PermissionManager.TPermissionType type = PermissionManager.onRequestPermissionsResult(requestCode, permissions, grantResults);
        PermissionManager.handlePermissionsResult(this, type, invokeParam, this);
    }

    /**
     * 获取TakePhoto实例
     *
     * @return
     */
    public TakePhoto getTakePhoto() {
        if (takePhoto == null) {
            takePhoto = (TakePhoto) TakePhotoInvocationHandler.of(this).bind(new TakePhotoImpl(this, this));
        }
        return takePhoto;
    }


    private ProgressDialog progressDialog;

    public void showWaitDialog(String msg) {
        try {
            if (progressDialog == null) {
                progressDialog = new ProgressDialog(this);
                progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                progressDialog.setMessage(msg);
                progressDialog.setIndeterminate(true);
//                indeterminateDrawable
                progressDialog.setIndeterminateDrawable(getResources().getDrawable(R.drawable.loading_dialog_color));
                progressDialog.setCancelable(true);
                progressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialogInterface) {

                    }
                });

            }
            if (!progressDialog.isShowing())
                progressDialog.show();
        } catch (WindowManager.BadTokenException e) {

        }
    }

    public void hideWaitDialog() {
        if (progressDialog != null) {
            progressDialog.dismiss();
            progressDialog = null;
        }
    }

    public void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
}