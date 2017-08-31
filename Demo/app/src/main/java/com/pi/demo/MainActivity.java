package com.pi.demo;

//import android.support.v7.app.AppCompatActivity;
import android.app.Activity;
import android.content.res.Configuration;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.pi.pipanosdk.ErrCode;
import com.pi.pipanosdk.PiPanoSDK;

import java.io.IOException;

public class MainActivity extends Activity implements PiPanoSDK.OnSDKIsReadyListener, PiPanoSDK.OnPreviewIsReadyListener
{

    // Used to load the 'native-lib' library on application startup.
    static
    {
        System.loadLibrary("native-lib");
    }
    private static final String TAG = "MainActivity";
    private final int CAMERA_FRONT = 0;
    private final int CAMERA_BACK = 1;

    private PiPanoSDK mPiPanoSDK = null;

    private enum RunMode_E{PREVIEW, PLAYVIDEO, PHOTO};   // 预览模式；播放视频模式；观看照片模式；
    private RunMode_E mRunMode = RunMode_E.PREVIEW;
    private boolean mSDKIsOK = false;
    private double mSeekOffset = 0.0;
    private Camera mCamera = null;

    private  boolean mbConnected = false;
    int mWidth = 1088;
    int mHeight = 1088;

    // SDK初始化完成会调用此函数
    @Override
    public void onSDKIsReady()
    {
        Log.d(TAG, "SDK is ok");
        mSDKIsOK = true;
    }

    // 预览准备就绪
    @Override
    public void onPreviewIsReady()
    {
        Log.d(TAG, "Preview is ok!!!");

        openCamera(0);

        // 获取SDK的SurfaceTexture
        //Surface sfc = null;
        SurfaceTexture texture = null;
        try
        {
            texture = mPiPanoSDK.getPreviewSurfaceTexture();
            Log.d(TAG, "mPiPanoSDK.getSurfaceTexture() = " + texture);
            //sfc = new Surface(sfct);
            try {

                mCamera.setPreviewTexture(texture);
            } catch (IOException ioe) {
                throw new RuntimeException(ioe);
            }
            mCamera.startPreview();
        } catch (Exception e)
        {
            e.printStackTrace();
        }

        mbConnected = true;
    }

    public void openCamera(int cameraIndex)
    {
        Log.d(TAG, "openCamera(): cameraIndex = " + cameraIndex);
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        int numCameras = Camera.getNumberOfCameras();
        for (int i = 0; i < numCameras; i++)
        {
            Camera.getCameraInfo(i, cameraInfo);
            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT && cameraIndex == CAMERA_FRONT)
            {
                mCamera = Camera.open(i);
            }
            else if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK && cameraIndex == CAMERA_BACK)
            {
                mCamera = Camera.open(i);
            }
        }
        if (null == mCamera)
        {
            throw new RuntimeException("Unable to open camera");
        }

        Camera.Parameters parms = mCamera.getParameters();
        CameraUtils.choosePreviewSize(parms, mWidth, mHeight);
        // Try to set the frame rate to a constant value.
        int mCameraPreviewThousandFps = CameraUtils.chooseFixedPreviewFps(parms, 30 * 1000);
        // Give the camera a hint that we're recording video.  This can have a big
        // impact on frame rate.
        parms.setRecordingHint(true);
        mCamera.setParameters(parms);
        Camera.Size cameraPreviewSize = parms.getPreviewSize();
        String previewFacts = cameraPreviewSize.width + "x" + cameraPreviewSize.height + " @" + (mCameraPreviewThousandFps / 1000.0f) + "fps";
        Log.i(TAG, "Camera config: " + previewFacts);
    }

    public void startVideoStream()
    {
        Log.d(TAG, "startVideoStream()");

        if (!mSDKIsOK)
        {
            return;
        }

        mPiPanoSDK.setPreviewIsReadyListener(this); // 设置监听预览是否准备就绪
        int res = ErrCode.UNKNOWN_ERROR;
        mPiPanoSDK.setPreviewTextureSize(mWidth, mHeight);  // 设置预览分辨率
        mPiPanoSDK.setShowMode(PiPanoSDK.EPM_SPHEREOUT);    // 设置显示模式
        res = mPiPanoSDK.startPreview();
        if (res != ErrCode.SUCCESS)
        {
            return;
        }
    }

    public  void stopVideoStream()
    {
        if (mbConnected)
        {
            mPiPanoSDK.stopPreview();
        }

        if (null != mCamera)
        {
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 创建SDK对象
        mPiPanoSDK = new PiPanoSDK();
        mPiPanoSDK.setSDKIsReadyListener(this);
        int ret = 0;
        ret = mPiPanoSDK.init(this);
        if (ret != ErrCode.SUCCESS)
        {
            Log.e(TAG, "mPI_PanoPlayer.init() failed!, ret = " + ret);
            return;
        }
        // 获取SDK图像渲染窗口
        View panoPlayerView = mPiPanoSDK.getPlayerView();
        if (null == panoPlayerView)
        {
            Log.e(TAG, "null == panoPlayerView");
            return;
        }

        // 关联显示控件
        LinearLayout layout = (LinearLayout)findViewById(R.id.videoview);
        layout.addView(panoPlayerView);

        initUI();

        Log.d(TAG, "initUsbData() over");
    }

    private void initUI()
    {
        RadioGroup showModeRadioGroup = (RadioGroup)findViewById(R.id.showmodegroup);
        showModeRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener()
        {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int i)
            {
                switch (i)
                {
                    case R.id.radioButtonFishEye:
                        mPiPanoSDK.setShowMode(PiPanoSDK.EPM_SPHEREOUT);
                        Log.e(TAG,"鱼眼");
                        break;
                    case R.id.radioButtonOut:
                        mPiPanoSDK.setShowMode(PiPanoSDK.EPM_SPHEREASTEROID);
                        Log.e(TAG,"小行星");
                        break;
                    case R.id.radioButtonIn:
                        mPiPanoSDK.setShowMode(PiPanoSDK.EPM_SPHEREIN);
                        Log.e(TAG,"内圆");
                        break;
                    case R.id.radioButtonOneEyePano:
                        mPiPanoSDK.setShowMode(PiPanoSDK.EPM_CYLINDERPLANE);
                        Log.e(TAG,"展开");
                        break;

                    default:
                        Log.e(TAG,"show mode error "+i);
                        mPiPanoSDK.setShowMode(PiPanoSDK.EPM_SPHEREOUT);
                        break;
                }
            }
        });

        // 播放文件
        Button openVideoButton = (Button)findViewById(R.id.openvideo);
        openVideoButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                if (RunMode_E.PREVIEW == mRunMode)
                {// 如果先前是预览模式，则先停止接收预览数据
                    if (mbConnected)
                    {
                        stopVideoStream();
                    }
                }

                mRunMode = RunMode_E.PLAYVIDEO;
                mSeekOffset = 0.0;
                mPiPanoSDK.setShowMode(PiPanoSDK.EPM_SPHEREOUT);
                mPiPanoSDK.openVideo("/mnt/sdcard/sdk_test.mp4");
            }
        });

        // 播放
        Button playVideoButton = (Button)findViewById(R.id.play);
        playVideoButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                if (RunMode_E.PLAYVIDEO != mRunMode)
                {// 必须在播放文件模式下
                    showTmsg("请先打开视频文件");
                    return;
                }

                mPiPanoSDK.resume();
            }
        });

        // 暂停
        Button pauseVideoButton = (Button)findViewById(R.id.pause);
        pauseVideoButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                if (RunMode_E.PLAYVIDEO != mRunMode)
                {// 必须在播放文件模式下
                    showTmsg("请先打开视频文件");
                    return;
                }

                mPiPanoSDK.pause();
            }
        });

        // 停止播放
        Button stopVideoButton = (Button)findViewById(R.id.stop);
        stopVideoButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                if (RunMode_E.PLAYVIDEO != mRunMode)
                {// 必须在播放文件模式下
                    showTmsg("请先打开视频文件");
                    return;
                }

                mPiPanoSDK.stop();
            }
        });

        // 快进
        Button seekVideoButton = (Button)findViewById(R.id.seek);
        seekVideoButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                if (RunMode_E.PLAYVIDEO != mRunMode)
                {// 必须在播放文件模式下
                    showTmsg("请先打开视频文件");
                    return;
                }

                mSeekOffset = mPiPanoSDK.getVideoProgress();
                Log.d(TAG, "mSeekOffset = " + mSeekOffset);
                mSeekOffset += 0.05;
                if (mSeekOffset > 1.0)
                {
                    mSeekOffset = 1.0;
                }
                mPiPanoSDK.seek(mSeekOffset);
            }
        });

        // 打开照片
        Button openPhotoButton = (Button)findViewById(R.id.openphoto);
        openPhotoButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                if (RunMode_E.PREVIEW == mRunMode)
                {// 如果先前是预览模式，则先停止接收预览数据
                    if (mbConnected)
                    {
                        stopVideoStream();
                    }
                }

                mRunMode = RunMode_E.PHOTO;
                mPiPanoSDK.setShowMode(PiPanoSDK.EPM_SPHEREOUT);
                mPiPanoSDK.openPhoto("/mnt/sdcard/sdk_photo_test.jpg");
            }
        });

        // 开始预览
        Button openPreViewButton = (Button)findViewById(R.id.preview);
        openPreViewButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                boolean connect = false;

                startVideoStream();
                mRunMode = RunMode_E.PREVIEW;
                showTmsg("开始预览");
            }
        });

    }

    //文字提示方法
    private void showTmsg(String msg)
    {
        Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();
    }

    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    public native String stringFromJNI();

    // Quit Unity
    @Override protected void onDestroy ()
    {
        Log.d(TAG, "onDestroy()");
        mPiPanoSDK.stopPreview();
        mPiPanoSDK.onDestroy();

        super.onDestroy();
    }

    // Pause Unity
    @Override protected void onPause()
    {
        Log.d(TAG, "onPause()");
        super.onPause();
        mPiPanoSDK.onPause();
    }

    // Resume Unity
    @Override protected void onResume()
    {
        Log.d(TAG, "onResume()");
        super.onResume();
        mPiPanoSDK.onResume();
       // mPiPanoSDK.startPreview();
    }


    // This ensures the layout will be correct.
    @Override public void onConfigurationChanged(Configuration newConfig)
    {
        Log.d(TAG, "onConfigurationChanged()");
        super.onConfigurationChanged(newConfig);
        mPiPanoSDK.onConfigurationChanged(newConfig);
    }

    // Notify Unity of the focus change.
    @Override public void onWindowFocusChanged(boolean hasFocus)
    {
        Log.d(TAG, "onWindowFocusChanged()");
        super.onWindowFocusChanged(hasFocus);
        mPiPanoSDK.onWindowFocusChanged(hasFocus);

    }

}
