package org.ancode.libzxing;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.Result;


import org.ancode.libzxing.camera.CameraManager;
import org.ancode.libzxing.decode.CaptureActivityHandler;
import org.ancode.libzxing.decode.DecodeImageCallback;
import org.ancode.libzxing.decode.DecodeImageThread;
import org.ancode.libzxing.decode.DecodeManager;
import org.ancode.libzxing.decode.InactivityTimer;
import org.ancode.libzxing.utils.RealPathUtil;
import org.ancode.libzxing.view.QrCodeFinderView;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * 二维码扫描类。
 */
public class QrCodeActivity extends ActionBarActivity implements Callback, OnClickListener {

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


    public static void launcher(Activity activity, String title, int color, int requestCode) {
        Intent intent = new Intent(activity, QrCodeActivity.class);
        intent.putExtra(QrCodeActivity.QR_TITLE, title);
        intent.putExtra(QrCodeActivity.QR_MAIN_COLOR, color);
        activity.startActivityForResult(intent, requestCode);
        activity.overridePendingTransition(R.anim.push_left_in, R.anim.push_not_move_out);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
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
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
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
                openSystemAlbum();

            }
        });
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
        super.onDestroy();
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
        intent.setAction(Intent.ACTION_GET_CONTENT);
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
            Intent resultIntent = new Intent();
            Bundle bundle = new Bundle();
            bundle.putString("result", resultString);
            resultIntent.putExtras(bundle);
            setResult(RESULT_OK, resultIntent);
            QrCodeActivity.this.finish();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, final Intent data) {
        if (resultCode != RESULT_OK) {
            return;
        }
        switch (requestCode) {
            case REQUEST_PICTURE:
                finish();
                break;
            case REQUEST_SYSTEM_PICTURE:
                try {
                    Uri uri = data.getData();
                    if (null != uri) {
                        String imgPath = RealPathUtil.getRealFilePath(this, uri);
                        Log.e("QRCODE", "imgpath =" + imgPath);
                        if (null != mQrCodeExecutor && !TextUtils.isEmpty(imgPath)) {
                            mQrCodeExecutor.execute(new DecodeImageThread(imgPath, mDecodeImageCallback));
                        }
                    }
                } catch (Exception e) {
                    Log.e("QRCODE", "get file path error", e);
                    e.printStackTrace();
                }

                break;
        }
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
}