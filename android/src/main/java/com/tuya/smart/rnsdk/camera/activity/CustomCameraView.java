package com.tuya.smart.rnsdk.camera.activity;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.facebook.react.uimanager.events.RCTEventEmitter;
import com.tuya.smart.android.camera.api.ITuyaHomeCamera;
import com.tuya.smart.android.camera.api.bean.CameraPushDataBean;
import com.tuya.smart.android.common.utils.L;
import com.tuya.smart.android.network.http.BusinessResponse;
import com.tuya.smart.camera.camerasdk.typlayer.callback.OnP2PCameraListener;
import com.tuya.smart.camera.camerasdk.typlayer.callback.OnRenderDirectionCallback;
import com.tuya.smart.camera.camerasdk.typlayer.callback.OperationDelegateCallBack;
import com.tuya.smart.camera.ipccamerasdk.bean.ConfigCameraBean;
import com.tuya.smart.camera.ipccamerasdk.p2p.ICameraP2P;
import com.tuya.smart.camera.middleware.p2p.ICameraConfig;
import com.tuya.smart.camera.middleware.p2p.ITuyaSmartCameraP2P;
import com.tuya.smart.camera.middleware.p2p.TuyaSmartCameraP2P;
import com.tuya.smart.camera.middleware.p2p.TuyaSmartCameraP2PFactory;
import com.tuya.smart.camera.middleware.widget.TuyaCameraView;
import com.tuya.smart.camera.utils.AudioUtils;
import com.tuya.smart.home.sdk.TuyaHomeSdk;
import com.tuya.smart.rnsdk.R;
import com.tuya.smart.rnsdk.camera.utils.Constants;
import com.tuya.smart.rnsdk.camera.utils.ToastUtil;
import com.tuya.smart.sdk.api.ITuyaGetBeanCallback;
import com.tuya.smart.sdk.bean.DeviceBean;
import com.tuyasmart.camera.devicecontrol.ITuyaCameraDevice;
import com.tuyasmart.camera.devicecontrol.TuyaCameraDeviceControlSDK;
import com.tuyasmart.camera.devicecontrol.bean.DpPTZControl;
import com.tuyasmart.camera.devicecontrol.bean.DpPTZStop;
import com.tuyasmart.camera.devicecontrol.model.PTZDirection;
import com.tuyasmart.stencil.utils.MessageUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleEventObserver;
import androidx.lifecycle.LifecycleOwner;

/**
 * TODO: document your custom view class.
 */
public class CustomCameraView extends RelativeLayout implements View.OnClickListener, TuyaCameraView.CreateVideoViewCallback, LifecycleEventListener {
    View cameraView;

    private String TAG = "CustomCameraView";

    private Toolbar toolbar;
    private TuyaCameraView mVideoView;
    private ImageView muteImg;
    private TextView qualityTv;
    private ImageView mFullScreenImg;
    private TextView speakTxt, recordTxt, photoTxt, replayTxt, settingTxt, cloudStorageTxt,messageCenterTxt, photoAlbumTxt;

    private ICameraP2P mCameraP2P;
    private static final int ASPECT_RATIO_WIDTH = 9;
    private static final int ASPECT_RATIO_HEIGHT = 16;
    private String p2pId = "", p2pWd = "", localKey = "", mInitStr = "EEGDFHBAKJINGGJKFAHAFKFIGINJGFMEHIEOAACPBFIDKMLKCMBPCLONHCKGJGKHBEMOLNCGPAMC", mP2pKey = "nVpkO1Xqbojgr4Ks";
    private boolean isSpeaking = false;
    private boolean isRecording = false;
    private boolean isPlay = false;
    private int previewMute = ICameraP2P.MUTE;
    private int videoClarity = ICameraP2P.HD;
    private boolean isCustomCameraView = false;

    private String picPath, videoPath;

    private boolean mIsRunSoft;
    private int sdkProvider;

    private String devId;
    private String folderPath;
    private ITuyaCameraDevice mDeviceControl;
    private ITuyaSmartCameraP2P mSmartCameraP2P;

    public static final int MSG_LOGIN_SUCCESS = 15;
    public static final int MSG_LOGIN_FAILURE = 16;
    private static ITuyaHomeCamera homeCamera;

    public static  long HOME_ID = 1099001;
    public static DeviceBean mCameraDevice;

    private Context context;
    ReactContext reactContext;
    private OnP2PCameraListener mP2PListener = null;
    private Activity mActivity;
    private boolean isFullScreen = false;
    private LinearLayout mControlLayout;
    private RelativeLayout mVideoViewContainer;
    private int videoContainerWidth = 0;
    private ProgressBar progressVideoView;



    public CustomCameraView(Context context) {
        super(context);
        this.context = context;
        reactContext = (ReactContext) context;
        reactContext.addLifecycleEventListener(this);
       // init(context);
    }


    public void init(Activity activity, Context context) {
        mActivity = activity;
        isCustomCameraView = true;
        View view = inflate(context, R.layout.activity_camera_live_preview,this);
        view.findViewById(R.id.camera_video_view_container);
        cameraView = view;
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        videoContainerWidth = windowManager.getDefaultDisplay().getWidth();
        mP2PListener = new OnP2PCameraListener() {
            @Override
            public void receiveFrameDataForMediaCodec(int i, byte[] bytes, int i1, int i2, byte[] bytes1, boolean b, int i3) {

            }

            @Override
            public void onReceiveFrameYUVData(int i, ByteBuffer byteBuffer, ByteBuffer byteBuffer1, ByteBuffer byteBuffer2, int i1, int i2, int i3, int i4, long l, long l1, long l2, Object o) {

            }

            @Override
            public void onSessionStatusChanged(Object o, int i, int i1) {

            }

            @Override
            public void onReceiveSpeakerEchoData(ByteBuffer byteBuffer, int sampleRate) {
                if (null != mCameraP2P){
                    int length = byteBuffer.capacity();
                    Log.d(TAG, "receiveSpeakerEchoData pcmlength " + length + " sampleRate " + sampleRate);
                    byte[] pcmData = new byte[length];
                    byteBuffer.get(pcmData, 0, length);
                    mCameraP2P.sendAudioTalkData(pcmData,length);
                }
            }
        };
        // Handle physical back button press
        view.setFocusableInTouchMode(true);
        view.requestFocus();
        view.setOnKeyListener(new OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_UP && keyCode == KeyEvent.KEYCODE_BACK) {
                    if(isFullScreen) {
                        setFullScreenView();
                        return true;
                    } else {
                        return false;
                    }

                } else {
                    if(isFullScreen) {
                        return true;
                    } else {
                        return false;
                    }
                }
              //  return false;
            }
        });

        initView();
        initHomeCamera();
        initData();


        if(mDeviceControl != null && mDeviceControl.isSupportCameraDps(DpPTZControl.ID)) {
            mVideoView.setOnRenderDirectionCallback(new OnRenderDirectionCallback() {

                @Override
                public void onLeft() {
                    mDeviceControl.publishCameraDps(DpPTZControl.ID, PTZDirection.LEFT.getDpValue());
                }

                @Override
                public void onRight() {
                    mDeviceControl.publishCameraDps(DpPTZControl.ID,PTZDirection.RIGHT.getDpValue());

                }

                @Override
                public void onUp() {
                    mDeviceControl.publishCameraDps(DpPTZControl.ID,PTZDirection.UP.getDpValue());

                }

                @Override
                public void onDown() {
                    mDeviceControl.publishCameraDps(DpPTZControl.ID,PTZDirection.DOWN.getDpValue());

                }

                @Override
                public void onCancel() {
                    mDeviceControl.publishCameraDps(DpPTZStop.ID,true);

                }
            });
        }
    }

    private void initView() {
        mControlLayout = findViewById(R.id.camera_control_board);
        mVideoView = findViewById(R.id.camera_video_view);
        muteImg = findViewById(R.id.camera_mute);
        qualityTv = findViewById(R.id.camera_quality);
        mFullScreenImg = findViewById(R.id.camera_full_screen);
        speakTxt = findViewById(R.id.speak_Txt);
        recordTxt = findViewById(R.id.record_Txt);
        photoTxt = findViewById(R.id.photo_Txt);
        replayTxt = findViewById(R.id.replay_Txt);
        settingTxt = findViewById(R.id.setting_Txt);
        photoAlbumTxt = findViewById(R.id.photo_album_Txt);
        settingTxt.setOnClickListener(this);
        cloudStorageTxt = findViewById(R.id.cloud_Txt);
        messageCenterTxt =  findViewById(R.id.message_center_Txt);
        mVideoViewContainer = findViewById(R.id.camera_video_view_Rl);
        progressVideoView = findViewById(R.id.progress_bar_video_view);
        progressVideoView.setVisibility(View.VISIBLE);

        setVideoViewSize(isFullScreen);

        muteImg.setSelected(true);
    }

    private void setVideoViewSize(boolean fullScreenMode) {
        RelativeLayout.LayoutParams layoutParams;
        int width = videoContainerWidth;
        int height = 0;
        int topPadding = 0;
        int bottomPadding = 0;
        if(fullScreenMode) {
            width = LayoutParams.MATCH_PARENT;
            height = LayoutParams.MATCH_PARENT;
            bottomPadding = 25;
            topPadding = 35;
        } else {
            height = width * ASPECT_RATIO_WIDTH / ASPECT_RATIO_HEIGHT;
        }

        layoutParams = new RelativeLayout.LayoutParams(width, height);
        mVideoViewContainer.setLayoutParams(layoutParams);
        mVideoViewContainer.setPadding(0,topPadding,0,bottomPadding);

    }

    public static void initHomeCamera() {
        //there is the somethings that need to set.For example the lat and lon;
        //   TuyaSdk.setLatAndLong();
        homeCamera = TuyaHomeSdk.getCameraInstance();
        if (homeCamera != null) {
            homeCamera.registerCameraPushListener(mTuyaGetBeanCallback);
        }
    }

    private void initData() {
        try {
            mCameraDevice =  TuyaHomeSdk.getDataInstance().getDeviceBean(devId);
            localKey = mCameraDevice.getLocalKey();
            Map<String, Object> map = mCameraDevice.getSkills();
            int p2pType = -1;
            if (map == null || map.size() == 0) {
                p2pType = -1;
            } else {
                p2pType = (Integer) (map.get("p2pType"));
            }


            // 拿到的是p2pType ，需要转化成 sdkProvider
            int intentP2pType = p2pType;
            // mIsRunSoft = getIntent().getBooleanExtra("isRunsoft", true);
            mIsRunSoft = true;
            mCameraP2P = TuyaSmartCameraP2PFactory.generateTuyaSmartCamera(intentP2pType);
            mVideoView.setCameraViewCallback(this);
            sdkProvider = intentP2pType == 1 ? 1 : 2;
            mVideoView.createVideoView(sdkProvider);
            if (null == mCameraP2P) {
                showNotSupportToast();
            } else {
                mCameraP2P.isEchoData(true);
                mDeviceControl = TuyaCameraDeviceControlSDK.getCameraDeviceInstance(devId);

                initListener();
                getApi();
            }
        } catch (Exception ex){
            Log.d("INFO ", "exception"+ex);
        }
    }

    private static ITuyaGetBeanCallback<CameraPushDataBean> mTuyaGetBeanCallback = new ITuyaGetBeanCallback<CameraPushDataBean>() {
        @Override
        public void onResult(CameraPushDataBean o) {
            L.d("TAG", "onMqtt_43_Result on callback");
            L.d("TAG", "timestamp=" + o.getTimestamp());
            L.d("TAG", "devid=" + o.getDevId());
            L.d("TAG", "msgid=" + o.getEdata());
            L.d("TAG", "etype=" + o.getEtype());

        }
    };

    private void initListener() {
        if (mCameraP2P == null) return;

        muteImg.setOnClickListener(this);
        qualityTv.setOnClickListener(this);
        mFullScreenImg.setOnClickListener(this);
        speakTxt.setOnClickListener(this);
        recordTxt.setOnClickListener(this);
        photoTxt.setOnClickListener(this);
        replayTxt.setOnClickListener(this);
        photoAlbumTxt.setOnClickListener(this);

        cloudStorageTxt.setOnClickListener(this);
        messageCenterTxt.setOnClickListener(this);
    }

    private void getApi() {
        try {
            Map postData = new HashMap();
            postData.put("devId", devId);
            mSmartCameraP2P = new TuyaSmartCameraP2P();

            mSmartCameraP2P.requestCameraInfo(devId, new ICameraConfig() {
                @Override
                public void onFailure(BusinessResponse var1, ConfigCameraBean var2, String var3) {
                    ToastUtil.showToast(context, "get cameraInfo failed");
                }

                @Override
                public void onSuccess(BusinessResponse var1, ConfigCameraBean var2, String var3) {

                    p2pWd = var2.getPassword();
                    p2pId = var2.getP2pId();
                    initCameraView(var2);
                }
            });
        } catch (Exception ex) {
            Log.d(TAG, "error "+ex.getMessage());
        }
    }

    private void initCameraView(ConfigCameraBean bean) {
        mCameraP2P.createDevice(new OperationDelegateCallBack() {
            @Override
            public void onSuccess(int sessionId, int requestId, String data) {
                mHandler.sendMessage(MessageUtil.getMessage(Constants.MSG_CREATE_DEVICE, Constants.ARG1_OPERATE_SUCCESS));
            }

            @Override
            public void onFailure(int sessionId, int requestId, int errCode) {
                mHandler.sendMessage(MessageUtil.getMessage(Constants.MSG_CREATE_DEVICE, Constants.ARG1_OPERATE_FAIL));
            }
        },bean);
    }



    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case Constants.MSG_CREATE_DEVICE:
                    handleCreateDevice(msg);
                    break;
                case Constants.MSG_CONNECT:
                    handleConnect(msg);
                    break;
                case Constants.MSG_GET_CLARITY:
                    handleClarity(msg);
                    break;
                case Constants.MSG_MUTE:
                    handleMute(msg);
                    break;
                case Constants.MSG_SCREENSHOT:
                    handlesnapshot(msg);
                    break;
                case Constants.MSG_VIDEO_RECORD_BEGIN:
                    ToastUtil.showToast(context, "record start success");
                    break;
                case Constants.MSG_VIDEO_RECORD_FAIL:
                    ToastUtil.showToast(context, "record start fail");
                    break;
                case Constants.MSG_VIDEO_RECORD_OVER:
                    handleVideoRecordOver(msg);
                    break;
                case Constants.MSG_TALK_BACK_BEGIN:
                    handleStartTalk(msg);
                    break;
                case Constants.MSG_TALK_BACK_OVER:
                    handleStopTalk(msg);
                    break;
            }
            super.handleMessage(msg);
        }
    };

    private void handleCreateDevice(Message msg) {
        if (msg.arg1 == Constants.ARG1_OPERATE_SUCCESS) {
            connect();
        } else {
            ToastUtil.showToast(context, "create device fail");
        }
    }

    private void handleConnect(Message msg) {
        if (msg.arg1 == Constants.ARG1_OPERATE_SUCCESS) {
            preview();
        } else {
            ToastUtil.showToast(context, "connect fail");
        }
    }

    private void handleClarity(Message msg) {
        if (msg.arg1 == Constants.ARG1_OPERATE_SUCCESS) {
            qualityTv.setText(videoClarity == ICameraP2P.HD ? "HD" : "SD");
        } else {
            ToastUtil.showToast(context, "operation fail");
        }
    }

    private void handleMute(Message msg) {
        if (msg.arg1 == Constants.ARG1_OPERATE_SUCCESS) {
            muteImg.setSelected(previewMute == ICameraP2P.MUTE);
        } else {
            ToastUtil.showToast(context, "operation fail");
        }
    }

    private void handlesnapshot(Message msg) {
        if (msg.arg1 == Constants.ARG1_OPERATE_SUCCESS) {
            ToastUtil.showToast(context, "snapshot success " + msg.obj);
        } else {
            ToastUtil.showToast(context, "operation fail");
        }
    }

    private void handleVideoRecordOver(Message msg) {
        if (msg.arg1 == Constants.ARG1_OPERATE_SUCCESS) {
            try {
                String message = msg.obj.toString();
                String path = message.split("\\.")[0];
                String[] pathString = path.split("/");
                String fileName = pathString[pathString.length - 1];

                String location = "Storage/SmartLife/Videos/" + devId + "/" + fileName + ".mp4";

                String filePath = String.format("Location: %s\nRecord Saved Successfully", location);

                ToastUtil.showToast(context, filePath);
            } catch (Exception ex){
                ToastUtil.showToast(context, msg.obj.toString());
            }
        } else {
            ToastUtil.showToast(context, "operation fail");
        }
    }

    private void handleStartTalk(Message msg) {
        if (msg.arg1 == Constants.ARG1_OPERATE_SUCCESS) {
            ToastUtil.showToast(context, "start talk success");
        } else {
            ToastUtil.showToast(context, "operation fail ");
        }
    }

    private void handleStopTalk(Message msg) {
        if (msg.arg1 == Constants.ARG1_OPERATE_SUCCESS) {
            ToastUtil.showToast(context, "stop talk success");
        } else {
            ToastUtil.showToast(context, "operation fail");
        }
    }


    private void connect() {

        mCameraP2P.connect(new OperationDelegateCallBack() {
            @Override
            public void onSuccess(int sessionId, int requestId, String data) {

                mHandler.sendMessage(MessageUtil.getMessage(Constants.MSG_CONNECT, Constants.ARG1_OPERATE_SUCCESS));
            }

            @Override
            public void onFailure(int sessionId, int requestId, int errCode) {
                mHandler.sendMessage(MessageUtil.getMessage(Constants.MSG_CONNECT, Constants.ARG1_OPERATE_FAIL, errCode));
            }
        });
    }

    private void preview() {
        mCameraP2P.startPreview(new OperationDelegateCallBack() {
            @Override
            public void onSuccess(int sessionId, int requestId, String data) {
                progressVideoView.setVisibility(View.GONE);
                // mVideoView.onResume();
                isPlay = true;
                if (null != mCameraP2P){
                    AudioUtils.getModel(context);
                    mCameraP2P.registorOnP2PCameraListener(mP2PListener);
                    mCameraP2P.generateCameraView(mVideoView.createdView());
                    getThumbnail();
                }
            }

            @Override
            public void onFailure(int sessionId, int requestId, int errCode) {
                Log.d(TAG, "start preview onFailure, errCode: " + errCode);
                isPlay = false;
            }
        });
    }

    private void getThumbnail() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            String path = folderPath + "/Thumbnail/";
            File file = new File(path);
            if (!file.exists()) {
                file.mkdirs();
            }
            picPath = path;
        }
        mCameraP2P.snapshot(picPath, context, ICameraP2P.PLAYMODE.LIVE, new OperationDelegateCallBack() {
            @Override
            public void onSuccess(int sessionId, int requestId, String fPath) {

                File savedFile = new File(fPath);
                String modifiedPath = savedFile.getParent()+"/"+devId+".png";
//                CLLog.d("RecordStop","dir: "+modifiedPath);
                File newFile = new File(modifiedPath);
                savedFile.renameTo(newFile);
                //mHandler.sendMessage(MessageUtil.getMessage(Constants.MSG_SCREENSHOT, Constants.ARG1_OPERATE_SUCCESS, data));
            }

            @Override
            public void onFailure(int sessionId, int requestId, int errCode) {
                // mHandler.sendMessage(MessageUtil.getMessage(Constants.MSG_SCREENSHOT, Constants.ARG1_OPERATE_FAIL));
            }
        });
    }

    private void showNotSupportToast() {
        ToastUtil.showToast(context, "device is not support!");
    }

    public void setDevId (String devId) {
        this.devId = devId;
    }

    public void setFolderPath (String folderPath) {
        this.folderPath = folderPath;
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.camera_mute) {
            muteClick();
        } else if (id == R.id.camera_quality) {
            setVideoClarity();
        } else if (id == R.id.camera_full_screen) {
            setFullScreenView();
        } else if (id == R.id.speak_Txt) {
            speakClick();
        } else if (id == R.id.record_Txt) {
            recordClick();
        } else if (id == R.id.photo_Txt) {
            snapShotClick();
        }
        else if (id == R.id.photo_album_Txt) {
            onPhotoAlbumBtnClick();
        }
        else if (id == R.id.setting_Txt) {
            onSettingsBtnClick();
        } else if (id == R.id.replay_Txt) {
            onReplayBtnClick();
        } else if (id == R.id.message_center_Txt) {
//            Intent intent3 = new Intent(CameraLivePreviewActivity.this, MotionDetectionActivity.class);
//            intent3.putExtra(INTENT_DEVID, devId);
//            startActivity(intent3);
        }
    }

    @Override
    public void onCreated(Object o) {

    }


    @Override
    public void videoViewClick() {

    }

    @Override
    public void startCameraMove(PTZDirection ptzDirection) {

    }

    @Override
    public void onActionUP() {

    }

    private void muteClick() {
        int mute;
        mute = previewMute == ICameraP2P.MUTE ? ICameraP2P.UNMUTE : ICameraP2P.MUTE;
        mCameraP2P.setMute(ICameraP2P.PLAYMODE.LIVE, mute, new OperationDelegateCallBack() {
            @Override
            public void onSuccess(int sessionId, int requestId, String data) {
                previewMute = Integer.valueOf(data);
                mHandler.sendMessage(MessageUtil.getMessage(Constants.MSG_MUTE, Constants.ARG1_OPERATE_SUCCESS));
            }

            @Override
            public void onFailure(int sessionId, int requestId, int errCode) {
                mHandler.sendMessage(MessageUtil.getMessage(Constants.MSG_MUTE, Constants.ARG1_OPERATE_FAIL));
            }
        });
    }

    private void setVideoClarity() {
        mCameraP2P.setVideoClarity(videoClarity == ICameraP2P.HD ? ICameraP2P.STANDEND : ICameraP2P.HD, new OperationDelegateCallBack() {
            @Override
            public void onSuccess(int sessionId, int requestId, String data) {
                videoClarity = Integer.valueOf(data);
                mHandler.sendMessage(MessageUtil.getMessage(Constants.MSG_GET_CLARITY, Constants.ARG1_OPERATE_SUCCESS));
            }

            @Override
            public void onFailure(int sessionId, int requestId, int errCode) {
                mHandler.sendMessage(MessageUtil.getMessage(Constants.MSG_GET_CLARITY, Constants.ARG1_OPERATE_FAIL));
            }
        });

    }

    private void setFullScreenView() {
        try {
            if(isFullScreen) {
                isFullScreen = false;
                changeControlViewVisibility(true);
                mFullScreenImg.setImageDrawable(getResources().getDrawable(R.drawable.ic_full_screen));
                mActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                setVideoViewSize(isFullScreen);
            } else {
                isFullScreen = true;
                changeControlViewVisibility(false);
                mFullScreenImg.setImageDrawable(getResources().getDrawable(R.drawable.ic_full_screen_exit));
                mActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                setVideoViewSize(isFullScreen);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void changeControlViewVisibility(boolean show) {
        mControlLayout.setVisibility(show ? VISIBLE : GONE);

    }


    private void speakClick() {
        if (isSpeaking) {
            mCameraP2P.stopAudioTalk(new OperationDelegateCallBack() {
                @Override
                public void onSuccess(int sessionId, int requestId, String data) {
                    isSpeaking = false;
                    speakTxt.setSelected(false);
                    mHandler.sendMessage(MessageUtil.getMessage(Constants.MSG_TALK_BACK_OVER, Constants.ARG1_OPERATE_SUCCESS));
                }

                @Override
                public void onFailure(int sessionId, int requestId, int errCode) {
                    isSpeaking = false;
                    speakTxt.setSelected(false);
                    mHandler.sendMessage(MessageUtil.getMessage(Constants.MSG_TALK_BACK_OVER, Constants.ARG1_OPERATE_FAIL));

                }
            });
        } else {
            if (Constants.hasRecordPermission()) {
                mCameraP2P.setEchoData(true);
                mCameraP2P.startAudioTalk(new OperationDelegateCallBack() {
                    @Override
                    public void onSuccess(int sessionId, int requestId, String data) {
                        isSpeaking = true;
                        speakTxt.setSelected(true);
                        mHandler.sendMessage(MessageUtil.getMessage(Constants.MSG_TALK_BACK_BEGIN, Constants.ARG1_OPERATE_SUCCESS));
                    }

                    @Override
                    public void onFailure(int sessionId, int requestId, int errCode) {
                        isSpeaking = false;
                        speakTxt.setSelected(false);
                        mHandler.sendMessage(MessageUtil.getMessage(Constants.MSG_TALK_BACK_BEGIN, Constants.ARG1_OPERATE_FAIL));
                    }
                });
            } else {
                Constants.requestPermission(mActivity, Manifest.permission.RECORD_AUDIO, Constants.EXTERNAL_AUDIO_REQ_CODE, "open_recording");
            }
        }
    }

    private void recordClick() {
        if (!isRecording) {
            if (Constants.hasStoragePermission()) {
                String picPath = folderPath + "/Video/"+devId+"/";
                File file = new File(picPath);
                if (!file.exists()) {
                    file.mkdirs();
                }
                String currentTime = "";
                Date c = Calendar.getInstance().getTime();
                SimpleDateFormat df = new SimpleDateFormat("dd-MMM-yyyy-hh:mm:ss");
                currentTime = df.format(c);
                String fileName = currentTime;
                videoPath = picPath + fileName;
                mCameraP2P.startRecordLocalMp4(picPath, fileName, context, new OperationDelegateCallBack() {
                    @Override
                    public void onSuccess(int sessionId, int requestId, String recFPath) {
                        isRecording = true;
                        mHandler.sendEmptyMessage(Constants.MSG_VIDEO_RECORD_BEGIN);
                    }

                    @Override
                    public void onFailure(int sessionId, int requestId, int errCode) {
                        mHandler.sendEmptyMessage(Constants.MSG_VIDEO_RECORD_FAIL);
                    }
                });
                recordStatue(true);
            } else {
                Constants.requestPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE, Constants.EXTERNAL_STORAGE_REQ_CODE, "open_storage");
            }
        } else {
            mCameraP2P.stopRecordLocalMp4(new OperationDelegateCallBack() {
                @Override
                public void onSuccess(int sessionId, int requestId, String recFPath) {
                    String currentTime = " ";
                    try {
//                        Date c = Calendar.getInstance().getTime();
//                        SimpleDateFormat df = new SimpleDateFormat("dd-MMM-yyyy-hh:mm:ss");
//                        currentTime = df.format(c);
//
//                        File savedFile = new File(recFPath);
//                        String modifiedPath = savedFile.getParent() + "/" + currentTime + ".mp4";
//                        File newFile = new File(modifiedPath);
//                        savedFile.renameTo(newFile);
//
//                        String recordFileSavingPath = "Storage/SmartLife/Video/" + devId+"/"+currentTime + ".mp4";
//                        Log.d("TAG", "recordFileSavingPath-->"+recordFileSavingPath);

                        isRecording = false;
                        mHandler.sendMessage(MessageUtil.getMessage(Constants.MSG_VIDEO_RECORD_OVER, Constants.ARG1_OPERATE_SUCCESS, recFPath));
                    } catch (Exception ex){

                    }
                }

                @Override
                public void onFailure(int sessionId, int requestId, int errCode) {
                    isRecording = false;
                    mHandler.sendMessage(MessageUtil.getMessage(Constants.MSG_VIDEO_RECORD_OVER, Constants.ARG1_OPERATE_FAIL));
                }
            });
            recordStatue(false);
        }
    }

    private void snapShotClick() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            String path = folderPath + "/snapshots/"+devId+"/";
            File file = new File(path);
            if (!file.exists()) {
                file.mkdirs();
            }
            picPath = path;
        }
        mCameraP2P.snapshot(picPath, context, ICameraP2P.PLAYMODE.LIVE, new OperationDelegateCallBack() {
            @Override
            public void onSuccess(int sessionId, int requestId, String ssFPath) {
                String currentTime = "";
                try {
                    Date c = Calendar.getInstance().getTime();
                    SimpleDateFormat df = new SimpleDateFormat("dd-MMM-yyyy-hh:mm:ss");
                    currentTime = df.format(c);

                    File savedFile = new File(ssFPath);
                    String modifiedPath = savedFile.getParent() + "/" + currentTime + ".png";
                    File newFile = new File(modifiedPath);
                    savedFile.renameTo(newFile);

                    String screenShotFileSavingPath = "Storage/SmartLife/snapshots/"+devId+"/" + currentTime + ".png";

                    mHandler.sendMessage(MessageUtil.getMessage(Constants.MSG_SCREENSHOT, Constants.ARG1_OPERATE_SUCCESS, screenShotFileSavingPath));
                } catch (Exception ex) {

                }
            }

            @Override
            public void onFailure(int sessionId, int requestId, int errCode) {
                mHandler.sendMessage(MessageUtil.getMessage(Constants.MSG_SCREENSHOT, Constants.ARG1_OPERATE_FAIL));
            }
        });
    }

    private void recordStatue(boolean isRecording) {
        speakTxt.setEnabled(!isRecording);
        photoTxt.setEnabled(!isRecording);
        replayTxt.setEnabled(!isRecording);
        recordTxt.setEnabled(true);
        recordTxt.setSelected(isRecording);
    }

    @Override
    public void onHostResume() {
        if(!CameraPlaybackView.isPlaybackView) {
            mVideoView.onResume();
            //must register again,or can't callback
            if (null != mCameraP2P) {
                AudioUtils.getModel(context);
                mCameraP2P.registorOnP2PCameraListener(mP2PListener);
                mCameraP2P.generateCameraView(mVideoView.createdView());
                if (mCameraP2P.isConnecting()) {
                    mCameraP2P.startPreview(new OperationDelegateCallBack() {
                        @Override
                        public void onSuccess(int sessionId, int requestId, String data) {
                            isPlay = true;
                        }

                        @Override
                        public void onFailure(int sessionId, int requestId, int errCode) {
                            Log.d(TAG, "start preview onFailure, errCode: " + errCode);
                        }
                    });
                }
            }
        }
    }

    @Override
    public void onHostPause() {

        if(isCustomCameraView) {
            mVideoView.onPause();
            if (isSpeaking) {
                mCameraP2P.stopAudioTalk(null);
            }
            if (isPlay) {
                mCameraP2P.stopPreview(new OperationDelegateCallBack() {
                    @Override
                    public void onSuccess(int sessionId, int requestId, String data) {

                    }

                    @Override
                    public void onFailure(int sessionId, int requestId, int errCode) {

                    }
                });
                isPlay = false;
            }
            if (null != mCameraP2P) {
                mCameraP2P.removeOnP2PCameraListener();
            }
            AudioUtils.changeToNomal(context);
            if (mSmartCameraP2P != null) {
                mSmartCameraP2P.destroyCameraBusiness();
            }
        }
    }

    @Override
    public void onHostDestroy() {

        if(!CameraPlaybackView.isPlaybackView) {
            if (null != mCameraP2P) {
                mCameraP2P.disconnect(new OperationDelegateCallBack() {
                    @Override
                    public void onSuccess(int sessionId, int requestId, String data) {

                    }

                    @Override
                    public void onFailure(int sessionId, int requestId, int errCode) {

                    }
                });
            }
        }
            TuyaSmartCameraP2PFactory.onDestroyTuyaSmartCamera();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (null != mCameraP2P) {
            mCameraP2P.disconnect(new OperationDelegateCallBack() {
                @Override
                public void onSuccess(int sessionId, int requestId, String data) {

                }

                @Override
                public void onFailure(int sessionId, int requestId, int errCode) {

                }
            });
        }
        TuyaSmartCameraP2PFactory.onDestroyTuyaSmartCamera();
    }

    public void onPhotoAlbumBtnClick() {
        try {
            WritableMap event = Arguments.createMap();
            event.putString("name", "photoAlbum");
            ReactContext reactContext = (ReactContext)getContext();
            reactContext.getJSModule(RCTEventEmitter.class).receiveEvent(
                    getId(),
                    "onPhotoAlbumClick",
                    event);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void onSettingsBtnClick() {
        try {
            WritableMap event = Arguments.createMap();
            event.putString("name", "settings");
            ReactContext reactContext = (ReactContext)getContext();
            reactContext.getJSModule(RCTEventEmitter.class).receiveEvent(
                    getId(),
                    "onSettingsClick",
                    event);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void onReplayBtnClick() {
        try {
            isCustomCameraView = false;

            WritableMap event = Arguments.createMap();
            event.putString("name", "playback");
            ReactContext reactContext = (ReactContext)getContext();
            reactContext.getJSModule(RCTEventEmitter.class).receiveEvent(
                    getId(),
                    "onReplayClick",
                    event);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}