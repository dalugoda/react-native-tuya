package com.tuya.smart.rnsdk.camera.activity;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
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

    private Toolbar toolbar;
    private TuyaCameraView mVideoView;
    private ImageView muteImg;
    private TextView qualityTv;
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

    private String picPath, videoPath;

    private boolean mIsRunSoft;
    private int sdkProvider;

    private String devId;
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


    public CustomCameraView(Context context) {
        super(context);
        this.context = context;
        reactContext = (ReactContext) context;
        reactContext.addLifecycleEventListener(this);
       // init(context);
    }


    public void init(Activity activity, Context context) {
        Log.d("TAG ", "init  --> ");
        mActivity = activity;
        View view = inflate(context, R.layout.activity_camera_live_preview,this);
        view.findViewById(R.id.camera_video_view_container);
        cameraView = view;
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
                    Log.d("TAG", "receiveSpeakerEchoData pcmlength " + length + " sampleRate " + sampleRate);
                    byte[] pcmData = new byte[length];
                    byteBuffer.get(pcmData, 0, length);
                    mCameraP2P.sendAudioTalkData(pcmData,length);
                }
            }
        };
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
        mVideoView = findViewById(R.id.camera_video_view);
        muteImg = findViewById(R.id.camera_mute);
        qualityTv = findViewById(R.id.camera_quality);
        speakTxt = findViewById(R.id.speak_Txt);
        recordTxt = findViewById(R.id.record_Txt);
        photoTxt = findViewById(R.id.photo_Txt);
        //replayTxt = findViewById(R.id.replay_Txt);
        settingTxt = findViewById(R.id.setting_Txt);
        photoAlbumTxt = findViewById(R.id.photo_album_Txt);
        settingTxt.setOnClickListener(this);
        cloudStorageTxt = findViewById(R.id.cloud_Txt);
        messageCenterTxt =  findViewById(R.id.message_center_Txt);

        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        int width = windowManager.getDefaultDisplay().getWidth();
        int height = width * ASPECT_RATIO_WIDTH / ASPECT_RATIO_HEIGHT;
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(width, height);
        // layoutParams.addRule(RelativeLayout.BELOW, R.id.toolbar_view);
        findViewById(R.id.camera_video_view_Rl).setLayoutParams(layoutParams);

        muteImg.setSelected(true);
        Log.d("TAG ", "init view --> ");
    }

    public static void initHomeCamera() {
        Log.d("TAG ", "init home camaeara --> ");
        //there is the somethings that need to set.For example the lat and lon;
        //   TuyaSdk.setLatAndLong();
        homeCamera = TuyaHomeSdk.getCameraInstance();
        if (homeCamera != null) {
            Log.d("TAG ", "camera not null  --> ");
            homeCamera.registerCameraPushListener(mTuyaGetBeanCallback);
        }
    }

    private void initData() {
        try {
            Log.d("TAG", "init data --> ");
            mCameraDevice =  TuyaHomeSdk.getDataInstance().getDeviceBean(devId);
            Log.d("TAG", "device bean --> "+mCameraDevice);
            localKey = mCameraDevice.getLocalKey();
            Log.d("TAG", "loal key --> "+localKey);
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
                Log.d("INFO ", "p2p type --> null");
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
        speakTxt.setOnClickListener(this);
        recordTxt.setOnClickListener(this);
        photoTxt.setOnClickListener(this);
       // replayTxt.setOnClickListener(this);
        photoAlbumTxt.setOnClickListener(this);

        cloudStorageTxt.setOnClickListener(this);
        messageCenterTxt.setOnClickListener(this);
    }

    private void getApi() {
        try {
            Log.d("TAG", "dev id --> "+devId);
            Map postData = new HashMap();
            postData.put("devId", devId);
            mSmartCameraP2P = new TuyaSmartCameraP2P();

            mSmartCameraP2P.requestCameraInfo(devId, new ICameraConfig() {
                @Override
                public void onFailure(BusinessResponse var1, ConfigCameraBean var2, String var3) {
                    ToastUtil.shortToast(context, "get cameraInfo failed");
                }

                @Override
                public void onSuccess(BusinessResponse var1, ConfigCameraBean var2, String var3) {

                    p2pWd = var2.getPassword();
                    p2pId = var2.getP2pId();
                    initCameraView(var2);
                }
            });
        } catch (Exception ex) {
            Log.d("TAG", "error "+ex.getMessage());
        }
    }

    private void initCameraView(ConfigCameraBean bean) {
        mCameraP2P.createDevice(new OperationDelegateCallBack() {
            @Override
            public void onSuccess(int sessionId, int requestId, String data) {

                Log.d("TAG", "init camera view onsuccess");
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
                    ToastUtil.shortToast(context, "record start success");
                    break;
                case Constants.MSG_VIDEO_RECORD_FAIL:
                    ToastUtil.shortToast(context, "record start fail");
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
            ToastUtil.shortToast(context, "create device fail");
        }
    }

    private void handleConnect(Message msg) {
        if (msg.arg1 == Constants.ARG1_OPERATE_SUCCESS) {
            preview();
        } else {
            ToastUtil.shortToast(context, "connect fail");
        }
    }

    private void handleClarity(Message msg) {
        if (msg.arg1 == Constants.ARG1_OPERATE_SUCCESS) {
            qualityTv.setText(videoClarity == ICameraP2P.HD ? "HD" : "SD");
        } else {
            ToastUtil.shortToast(context, "operation fail");
        }
    }

    private void handleMute(Message msg) {
        if (msg.arg1 == Constants.ARG1_OPERATE_SUCCESS) {
            muteImg.setSelected(previewMute == ICameraP2P.MUTE);
        } else {
            ToastUtil.shortToast(context, "operation fail");
        }
    }

    private void handlesnapshot(Message msg) {
        if (msg.arg1 == Constants.ARG1_OPERATE_SUCCESS) {
            ToastUtil.shortToast(context, "snapshot success " + msg.obj);
        } else {
            ToastUtil.shortToast(context, "operation fail");
        }
    }

    private void handleVideoRecordOver(Message msg) {
        if (msg.arg1 == Constants.ARG1_OPERATE_SUCCESS) {
            ToastUtil.shortToast(context, "record success " + msg.obj);
        } else {
            ToastUtil.shortToast(context, "operation fail");
        }
    }

    private void handleStartTalk(Message msg) {
        if (msg.arg1 == Constants.ARG1_OPERATE_SUCCESS) {
            ToastUtil.shortToast(context, "start talk success -->" + msg.obj);
        } else {
            ToastUtil.shortToast(context, "operation fail --> ");
        }
    }

    private void handleStopTalk(Message msg) {
        if (msg.arg1 == Constants.ARG1_OPERATE_SUCCESS) {
            ToastUtil.shortToast(context, "stop talk success -->" + msg.obj);
        } else {
            ToastUtil.shortToast(context, "operation fail --> ");
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
                Log.d("TAG", "start preview onSuccess -->");

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
                Log.d("TAG", "start preview onFailure, errCode: " + errCode);
                isPlay = false;
            }
        });
    }

    private void getThumbnail() {
        Log.d("TAG", "get thumbnail -->");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            String path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/SmartLife/Thumbnail/";
            File file = new File(path);
            if (!file.exists()) {
                file.mkdirs();
            }
            picPath = path;
        }
        mCameraP2P.snapshot(picPath, context, ICameraP2P.PLAYMODE.LIVE, new OperationDelegateCallBack() {
            @Override
            public void onSuccess(int sessionId, int requestId, String fPath) {
                Log.d("TAG", "snapshot data --> "+fPath);// "/storage/emulated/0/SmartLife/Thumbnail/1603367041293.png"

                File savedFile = new File(fPath);
                Log.d("TAG", "savedFile.getParent() --> "+savedFile.getParent());
                String modifiedPath = savedFile.getParent()+"/"+"CameraThumbnail"+".png";
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
        ToastUtil.shortToast(context, "device is not support!");
    }

    public void setDevId (String devId) {
        Log.d("TAG ", "Set dev id --> "+devId);
        this.devId = devId;
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.camera_mute) {
            muteClick();
        } else if (id == R.id.camera_quality) {
            setVideoClarity();
        } else if (id == R.id.speak_Txt) {
            speakClick();
        } else if (id == R.id.record_Txt) {
            recordClick();
        } else if (id == R.id.photo_Txt) {
            snapShotClick();
        }
        else if (id == R.id.photo_album_Txt) {
            //
        }
        else if (id == R.id.setting_Txt) {
            onSettingsBtnClick();
           // callBackSettingsPress.invoke();
//            Intent intent1 = new Intent(CameraLivePreviewActivity.this, SettingsActivity.class);
//            intent1.putExtra("devId", devId);
//            startActivity(intent1);
        } else if (id == R.id.cloud_Txt) {
//            if (sdkProvider == SDK_PROVIDER_V1) {
//                showNotSupportToast();
//                return;
//            }
//            Intent intent2 = new Intent(CameraLivePreviewActivity.this, CameraCloudStorageActivity.class);
//            intent2.putExtra(INTENT_DEVID, devId);
//            intent2.putExtra(INTENT_SDK_POROVIDER, sdkProvider);
//            intent2.putExtra(INTENT_HOME_ID, HOME_ID);
//            startActivity(intent2);
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


    private void speakClick() {
        if (isSpeaking) {
            mCameraP2P.stopAudioTalk(new OperationDelegateCallBack() {
                @Override
                public void onSuccess(int sessionId, int requestId, String data) {
                    isSpeaking = false;
                    mHandler.sendMessage(MessageUtil.getMessage(Constants.MSG_TALK_BACK_OVER, Constants.ARG1_OPERATE_SUCCESS));
                }

                @Override
                public void onFailure(int sessionId, int requestId, int errCode) {
                    isSpeaking = false;
                    mHandler.sendMessage(MessageUtil.getMessage(Constants.MSG_TALK_BACK_OVER, Constants.ARG1_OPERATE_FAIL));

                }
            });
        } else {
            if (Constants.hasRecordPermission()) {
                mCameraP2P.setEchoData(true);
                mCameraP2P.startAudioTalk(new OperationDelegateCallBack() {
                    @Override
                    public void onSuccess(int sessionId, int requestId, String data) {
                        Log.d("TAG", "start speaking -->" + data);
                        isSpeaking = true;
                        mHandler.sendMessage(MessageUtil.getMessage(Constants.MSG_TALK_BACK_BEGIN, Constants.ARG1_OPERATE_SUCCESS));
                    }

                    @Override
                    public void onFailure(int sessionId, int requestId, int errCode) {
                        isSpeaking = false;
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
                String picPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/SmartLife/Video/";
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
                    //String currentTime = " ";
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
//                        String recordFileSavingPath = "Storage/SmartLife/Video/" + currentTime + ".mp4";
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
            String path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/SmartLife/ScreenShots/";
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

                    String screenShotFileSavingPath = "Storage/SmartLife/ScreenShots/" + currentTime + ".png";

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
       // replayTxt.setEnabled(!isRecording);
        recordTxt.setEnabled(true);
        recordTxt.setSelected(isRecording);
    }

    @Override
    public void onHostResume() {
        mVideoView.onResume();
        Log.d("TAG", "on resume -->");
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
                        Log.d("TAG", "start preview onFailure, errCode: " + errCode);
                    }
                });
            }
        }
    }

    @Override
    public void onHostPause() {
        Log.d("TAG", "on pause --------------->");
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

    @Override
    public void onHostDestroy() {
        Log.d("TAG", "on destroy -->");

    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        Log.d("TAG,", "onDetachedFromWindow -->");
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

    public void onSettingsBtnClick() {
        try {
        WritableMap event = Arguments.createMap();
        WritableMap camInfo = Arguments.createMap();
            camInfo.putString("camera_ip", mCameraDevice.getIp());
            camInfo.putString("camera_devId", mCameraDevice.getDevId());
            camInfo.putString("camera_timeZone", mCameraDevice.getTimezoneId());
        event.putMap("cameraInfo", camInfo);
        ReactContext reactContext = (ReactContext)getContext();
        reactContext.getJSModule(RCTEventEmitter.class).receiveEvent(
                getId(),
                "onSettingsClick",
                event);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void onSettingsBtnClick1() {
        // Create map for params
        WritableMap payload = Arguments.createMap();
        // Put data to map
        payload.putString("devId", devId);
        // Get EventEmitter from context and send event
        this.reactContext
                .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                .emit("loadSettings", payload);
    }
}
