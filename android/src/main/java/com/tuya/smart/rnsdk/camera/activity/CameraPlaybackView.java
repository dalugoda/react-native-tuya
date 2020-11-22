package com.tuya.smart.rnsdk.camera.activity;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.tuya.smart.camera.camerasdk.bean.TimePieceBean;
import com.tuya.smart.camera.camerasdk.typlayer.callback.OnP2PCameraListener;
import com.tuya.smart.camera.ipccamerasdk.p2p.ICameraP2P;
import com.tuya.smart.camera.middleware.p2p.TuyaSmartCameraP2PFactory;
import com.tuya.smart.camera.middleware.widget.TuyaCameraView;
import com.tuya.smart.home.sdk.TuyaHomeSdk;
import com.tuya.smart.rnsdk.R;
import com.tuya.smart.sdk.bean.DeviceBean;
import com.tuyasmart.camera.devicecontrol.model.PTZDirection;

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

public class CameraPlaybackView extends RelativeLayout implements OnP2PCameraListener, View.OnClickListener, TuyaCameraView.CreateVideoViewCallback{
    View cameraPlaybackView;

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

    private DeviceBean mCameraDevice;

    public CameraPlaybackView(Activity activity, Context context) {
        super(context);
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
    public void onClick(View v) {

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
