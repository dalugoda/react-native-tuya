package com.tuya.smart.rnsdk.camera.activity;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import com.tuya.smart.android.common.utils.L;
import com.tuya.smart.camera.camerasdk.bean.TimePieceBean;
import com.tuya.smart.camera.camerasdk.typlayer.callback.OnP2PCameraListener;
import com.tuya.smart.camera.camerasdk.typlayer.callback.OperationDelegateCallBack;
import com.tuya.smart.camera.ipccamerasdk.bean.MonthDays;
import com.tuya.smart.camera.ipccamerasdk.p2p.ICameraP2P;
import com.tuya.smart.camera.middleware.p2p.TuyaSmartCameraP2PFactory;
import com.tuya.smart.camera.middleware.widget.TuyaCameraView;
import com.tuya.smart.camera.utils.AudioUtils;
import com.tuya.smart.home.sdk.TuyaHomeSdk;
import com.tuya.smart.rnsdk.R;
import com.tuya.smart.rnsdk.camera.utils.ToastUtil;
import com.tuya.smart.sdk.bean.DeviceBean;
import com.tuyasmart.camera.devicecontrol.model.PTZDirection;
import com.tuyasmart.stencil.utils.MessageUtil;

import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import static android.content.Context.WINDOW_SERVICE;
import static com.tuya.smart.rnsdk.camera.utils.Constants.ARG1_OPERATE_FAIL;
import static com.tuya.smart.rnsdk.camera.utils.Constants.ARG1_OPERATE_SUCCESS;
import static com.tuya.smart.rnsdk.camera.utils.Constants.MSG_DATA_DATE;
import static com.tuya.smart.rnsdk.camera.utils.Constants.MSG_DATA_DATE_BY_DAY_FAIL;
import static com.tuya.smart.rnsdk.camera.utils.Constants.MSG_DATA_DATE_BY_DAY_SUCC;
import static com.tuya.smart.rnsdk.camera.utils.Constants.MSG_MUTE;

public class CameraPlaybackView extends RelativeLayout implements OnP2PCameraListener, View.OnClickListener, TuyaCameraView.CreateVideoViewCallback{
    View cameraPlaybackView;
    private String TAG = "cameraPlaybackView";
    private Context context;


    private Activity mActivity;
    private ICameraP2P mCameraP2P;
    private static final int ASPECT_RATIO_WIDTH = 9;
    private static final int ASPECT_RATIO_HEIGHT = 16;
    private String p2pId = "", p2pWd = "", localKey = "",devId="", mInitStr = "EEGDFHBAKJINGGJKFAHAFKFIGINJGFMEHIEOAACPBFIDKMLKCMBPCLONHCKGJGKHBEMOLNCGPAMC", mP2pKey = "nVpkO1Xqbojgr4Ks";

    private TuyaCameraView mVideoView;
    private ImageView muteImg;
    private EditText dateInputEdt;
    private RecyclerView queryRv;
    private Button queryBtn, startBtn, pauseBtn, resumeBtn, stopBtn;

    Context mContext;

    protected Map<String, List<String>> mBackDataMonthCache;
    protected Map<String, List<TimePieceBean>> mBackDataDayCache;
    private int mPlaybackMute = ICameraP2P.MUTE;
    private boolean mIsRunSoft;
    private int p2pType;
    private CameraPlaybackTimeAdapter adapter;
    private List<TimePieceBean> queryDateList;
    private int queryDay;

    private boolean isPlayback = false;

    private DeviceBean mCameraDevice;

    public CameraPlaybackView(Activity activity, Context context) {
        super(context);
        this.context = context;
        init(activity, context);
    }

    public void init(Activity activity, Context context) {
        mActivity = activity;
        mContext = context;
        View view = inflate(context, R.layout.camera_playback_layout,this);
        view.findViewById(R.id.camera_playback_view_container);
        cameraPlaybackView = view;
        initView();
        initData();

        initListener();
    }


    private void initView() {

        mVideoView = findViewById(R.id.camera_video_view);
        muteImg = findViewById(R.id.camera_mute);
        dateInputEdt = findViewById(R.id.date_input_edt);
        queryBtn = findViewById(R.id.query_btn);
        startBtn = findViewById(R.id.start_btn);
        pauseBtn = findViewById(R.id.pause_btn);
        resumeBtn = findViewById(R.id.resume_btn);
        stopBtn = findViewById(R.id.stop_btn);
        queryRv = findViewById(R.id.query_list);

        //播放器view最好宽高比设置16:9
        WindowManager windowManager = (WindowManager) mContext.getSystemService(WINDOW_SERVICE);
        int width = windowManager.getDefaultDisplay().getWidth();
        int height = width * ASPECT_RATIO_WIDTH / ASPECT_RATIO_HEIGHT;
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(width, height);
        findViewById(R.id.camera_video_view_Rl).setLayoutParams(layoutParams);
    }


    private void initData() {
        mBackDataMonthCache = new HashMap<>();
        mBackDataDayCache = new HashMap<>();
//        mIsRunSoft = getIntent().getBooleanExtra("isRunsoft", false);
//        p2pId = getIntent().getStringExtra("p2pId");
//        p2pWd = getIntent().getStringExtra("p2pWd");
//        localKey = getIntent().getStringExtra("localKey");
//        p2pType = getIntent().getIntExtra("p2pType", 1);
//        devId = getIntent().getStringExtra("devId");

        mVideoView.createVideoView(p2pType);
        mVideoView.setCameraViewCallback(this);

        LinearLayoutManager mLayoutManager = new LinearLayoutManager(mContext, LinearLayoutManager.VERTICAL, false);
        queryRv.setLayoutManager(mLayoutManager);
        queryRv.addItemDecoration(new DividerItemDecoration(mContext, DividerItemDecoration.VERTICAL));
        queryDateList = new ArrayList<>();
        adapter = new CameraPlaybackTimeAdapter(mContext, queryDateList);
        queryRv.setAdapter(adapter);

        //there is no need to reconnect（createDevice） with a single column object（Of course，you can create it again）
        mCameraP2P = TuyaSmartCameraP2PFactory.generateTuyaSmartCamera(p2pType);
            mCameraP2P.connectPlayback();

            muteImg.setSelected(true);
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy/MM/dd");
        Date date = new Date(System.currentTimeMillis());
            dateInputEdt.setText(simpleDateFormat.format(date));

        mCameraDevice =  TuyaHomeSdk.getDataInstance().getDeviceBean(devId);

        mVideoView.onResume();
        if (null != mCameraP2P) {
            AudioUtils.getModel(context);
            mCameraP2P.registorOnP2PCameraListener(this);
            mCameraP2P.generateCameraView(mVideoView.createdView());
        }

}

    private void initListener() {
        muteImg.setOnClickListener(this);
        queryBtn.setOnClickListener(this);
        startBtn.setOnClickListener(this);
        pauseBtn.setOnClickListener(this);
        resumeBtn.setOnClickListener(this);
        stopBtn.setOnClickListener(this);
        adapter.setListener(new CameraPlaybackTimeAdapter.OnTimeItemListener() {
            @Override
            public void onClick(TimePieceBean timePieceBean) {
                mCameraP2P.startPlayBack(timePieceBean.getStartTime(),
                        timePieceBean.getEndTime(),
                        timePieceBean.getStartTime()+1500, new OperationDelegateCallBack() {
                            @Override
                            public void onSuccess(int sessionId, int requestId, String data) {
                                Log.d(TAG, "OperationDelegateCallBack "+data);

                                isPlayback = true;
                            }

                            @Override
                            public void onFailure(int sessionId, int requestId, int errCode) {
                                isPlayback = false;
                            }
                        }, new OperationDelegateCallBack() {
                            @Override
                            public void onSuccess(int sessionId, int requestId, String data) {
                                Log.d(TAG, "OperationDelegateCallBack "+data);

                                isPlayback = false;
                            }

                            @Override
                            public void onFailure(int sessionId, int requestId, int errCode) {
                                isPlayback = false;
                            }
                        });
            }
        });
    }

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_MUTE:
                    handleMute(msg);
                    break;
                case MSG_DATA_DATE:
                    handleDataDate(msg);
                    break;
                case MSG_DATA_DATE_BY_DAY_SUCC:
                case MSG_DATA_DATE_BY_DAY_FAIL:
                    handleDataDay(msg);
                    break;
            }
            super.handleMessage(msg);
        }
    };

    private void handleDataDay(Message msg) {
        if (msg.arg1 == ARG1_OPERATE_SUCCESS) {
            queryDateList.clear();
            //Timepieces with data for the query day
            List<TimePieceBean> timePieceBeans = mBackDataDayCache.get(mCameraP2P.getDayKey());
            if (timePieceBeans != null) {
                Log.d(TAG, "Time piece bean list"+timePieceBeans.size());
                queryDateList.addAll(timePieceBeans);
            } else {
               // showErrorToast();
            }

            adapter.notifyDataSetChanged();
        } else {

        }
    }

    private void handleDataDate(Message msg) {
        if (msg.arg1 == ARG1_OPERATE_SUCCESS) {
            List<String> days = mBackDataMonthCache.get(mCameraP2P.getMonthKey());

            try {
                if (days.size() == 0) {
                   // showErrorToast();
                    return;
                }
                final String inputStr = dateInputEdt.getText().toString();
                if (!TextUtils.isEmpty(inputStr) && inputStr.contains("/")) {
                    String[] substring = inputStr.split("/");
                    int year = Integer.parseInt(substring[0]);
                    int mouth = Integer.parseInt(substring[1]);
                    int day = Integer.parseInt(substring[2]);
                    mCameraP2P.queryRecordTimeSliceByDay(year, mouth, day, new OperationDelegateCallBack() {
                        @Override
                        public void onSuccess(int sessionId, int requestId, String data) {
                            L.e(TAG, inputStr + " --- " + data);
                            parsePlaybackData(data);
                        }

                        @Override
                        public void onFailure(int sessionId, int requestId, int errCode) {
                            mHandler.sendEmptyMessage(MSG_DATA_DATE_BY_DAY_FAIL);
                        }
                    });
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else {

        }
    }

    private void parsePlaybackData(Object obj) {
        RecordInfoBean recordInfoBean = JSONObject.parseObject(obj.toString(), RecordInfoBean.class);
        if (recordInfoBean.getCount() != 0) {
            List<TimePieceBean> timePieceBeanList = recordInfoBean.getItems();
            if (timePieceBeanList != null && timePieceBeanList.size() != 0) {
                mBackDataDayCache.put(mCameraP2P.getDayKey(), timePieceBeanList);
            }
            mHandler.sendMessage(MessageUtil.getMessage(MSG_DATA_DATE_BY_DAY_SUCC, ARG1_OPERATE_SUCCESS));
        } else {
            mHandler.sendMessage(MessageUtil.getMessage(MSG_DATA_DATE_BY_DAY_FAIL, ARG1_OPERATE_FAIL));
        }
    }

    private void handleMute(Message msg) {
        if (msg.arg1 == ARG1_OPERATE_SUCCESS) {
            muteImg.setSelected(mPlaybackMute == ICameraP2P.MUTE);
        } else {
            ToastUtil.shortToast(context, "operation fail");
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.camera_mute) {
            muteClick();
        } else if (id == R.id.query_btn) {
            queryDayByMonthClick();
        } else if (id == R.id.start_btn) {
            startPlayback();
        } else if (id == R.id.pause_btn) {
            pauseClick();
        } else if (id == R.id.resume_btn) {
            resumeClick();
        } else if (id == R.id.stop_btn) {
            stopClick();
        }
    }

    private void startPlayback() {
        if (null != queryDateList && queryDateList.size() > 0) {
            TimePieceBean timePieceBean = queryDateList.get(0);
            if (null != timePieceBean) {
                mCameraP2P.startPlayBack(timePieceBean.getStartTime(), timePieceBean.getEndTime(), timePieceBean.getStartTime(), new OperationDelegateCallBack() {
                    @Override
                    public void onSuccess(int sessionId, int requestId, String data) {
                        isPlayback = true;
                    }

                    @Override
                    public void onFailure(int sessionId, int requestId, int errCode) {

                    }
                }, new OperationDelegateCallBack() {
                    @Override
                    public void onSuccess(int sessionId, int requestId, String data) {
                        isPlayback = false;
                    }

                    @Override
                    public void onFailure(int sessionId, int requestId, int errCode) {

                    }
                });
            }
        } else {
            ToastUtil.shortToast(context, "No data for query date");
        }
    }

    private void stopClick() {
        mCameraP2P.stopPlayBack(new OperationDelegateCallBack() {
            @Override
            public void onSuccess(int sessionId, int requestId, String data) {

            }

            @Override
            public void onFailure(int sessionId, int requestId, int errCode) {

            }
        });
        isPlayback = false;
    }

    private void resumeClick() {
        mCameraP2P.resumePlayBack(new OperationDelegateCallBack() {
            @Override
            public void onSuccess(int sessionId, int requestId, String data) {
                isPlayback = true;
            }

            @Override
            public void onFailure(int sessionId, int requestId, int errCode) {

            }
        });
    }

    private void pauseClick() {
        mCameraP2P.pausePlayBack(new OperationDelegateCallBack() {
            @Override
            public void onSuccess(int sessionId, int requestId, String data) {
                isPlayback = false;
            }

            @Override
            public void onFailure(int sessionId, int requestId, int errCode) {

            }
        });
    }

    private void queryDayByMonthClick() {
        String inputStr = dateInputEdt.getText().toString();
        if (TextUtils.isEmpty(inputStr)) {
            return;
        }
        if (inputStr.contains("/")) {
            String[] substring = inputStr.split("/");
            if (substring.length > 2) {
                try {
                    int year = Integer.parseInt(substring[0]);
                    int mouth = Integer.parseInt(substring[1]);
                    queryDay = Integer.parseInt(substring[2]);
                    mCameraP2P.queryRecordDaysByMonth(year, mouth, new OperationDelegateCallBack() {
                        @Override
                        public void onSuccess(int sessionId, int requestId, String data) {
                            MonthDays monthDays = JSONObject.parseObject(data, MonthDays.class);
                            mBackDataMonthCache.put(mCameraP2P.getMonthKey(), monthDays.getDataDays());
                            L.e(TAG,   "MonthDays --- " + data);

                            mHandler.sendMessage(MessageUtil.getMessage(MSG_DATA_DATE, ARG1_OPERATE_SUCCESS, data));
                        }

                        @Override
                        public void onFailure(int sessionId, int requestId, int errCode) {
                            mHandler.sendMessage(MessageUtil.getMessage(MSG_DATA_DATE, ARG1_OPERATE_FAIL));
                        }
                    });
                } catch (Exception e) {
                    ToastUtil.shortToast(context, "Input Error");
                }
            }
        }
    }

    private void muteClick() {
        int mute;
        mute = mPlaybackMute == ICameraP2P.MUTE ? ICameraP2P.UNMUTE : ICameraP2P.MUTE;
        mCameraP2P.setMute(ICameraP2P.PLAYMODE.PLAYBACK, mute, new OperationDelegateCallBack() {
            @Override
            public void onSuccess(int sessionId, int requestId, String data) {
                mPlaybackMute = Integer.valueOf(data);
                mHandler.sendMessage(MessageUtil.getMessage(MSG_MUTE, ARG1_OPERATE_SUCCESS));
            }

            @Override
            public void onFailure(int sessionId, int requestId, int errCode) {
                mHandler.sendMessage(MessageUtil.getMessage(MSG_MUTE, ARG1_OPERATE_FAIL));
            }
        });
    }


    public void setMIsRunSoft (boolean mIsRunSoft) {
        this.mIsRunSoft = mIsRunSoft;
    }

    public void setP2pId (String p2pId) {
        this.p2pId = p2pId;
    }

    public void setP2pWd (String p2pWd) {
        this.p2pWd = p2pWd;
    }

    public void setLocalKey (String localKey) {
        this.localKey = localKey;
    }

    public void setP2pType (int p2pType) {
        this.p2pType = p2pType;
    }

    public void setDevId (String devId) {
        this.devId = devId;
    }

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
    public void onReceiveSpeakerEchoData(ByteBuffer byteBuffer, int i) {

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
}
