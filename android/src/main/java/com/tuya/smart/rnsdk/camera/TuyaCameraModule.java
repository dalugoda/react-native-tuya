package com.tuya.smart.rnsdk.camera;

import android.content.Intent;
import android.os.Environment;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;

import com.alibaba.fastjson.JSONException;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;
import com.tuya.smart.android.camera.api.ITuyaHomeCamera;
import com.tuya.smart.android.common.utils.L;
import com.tuya.smart.android.network.http.BusinessResponse;
import com.tuya.smart.camera.camerasdk.bean.TimePieceBean;
import com.tuya.smart.camera.camerasdk.typlayer.callback.OperationDelegateCallBack;
import com.tuya.smart.camera.ipccamerasdk.bean.ConfigCameraBean;
import com.tuya.smart.camera.ipccamerasdk.bean.MonthDays;
import com.tuya.smart.camera.ipccamerasdk.p2p.ICameraP2P;
import com.tuya.smart.camera.middleware.p2p.ICameraConfig;
import com.tuya.smart.camera.middleware.p2p.ITuyaSmartCameraP2P;
import com.tuya.smart.camera.middleware.p2p.TuyaSmartCameraP2P;
import com.tuya.smart.camera.middleware.widget.TuyaCameraView;
import com.tuya.smart.home.sdk.bean.HomeBean;
import com.tuya.smart.home.sdk.callback.ITuyaGetHomeListCallback;
import com.tuya.smart.home.sdk.callback.ITuyaHomeResultCallback;
import com.tuya.smart.rnsdk.camera.activity.CameraLivePreviewActivity;

import androidx.annotation.NonNull;

import com.tuya.smart.rnsdk.camera.activity.RecordInfoBean;
import com.tuya.smart.rnsdk.camera.utils.Constants;
import com.tuya.smart.home.sdk.TuyaHomeSdk;
import com.tuya.smart.rnsdk.camera.utils.ToastUtil;
import com.tuya.smart.rnsdk.utils.TuyaReactUtils;
import com.tuya.smart.sdk.api.IDevListener;
import com.tuya.smart.sdk.bean.DeviceBean;
import com.tuyasmart.camera.devicecontrol.ITuyaCameraDevice;
import com.tuyasmart.camera.devicecontrol.TuyaCameraDeviceControlSDK;
import com.tuyasmart.camera.devicecontrol.api.ITuyaCameraDeviceControlCallback;
import com.tuyasmart.camera.devicecontrol.bean.DpBasicIndicator;
import com.tuyasmart.camera.devicecontrol.bean.DpBasicNightvision;
import com.tuyasmart.camera.devicecontrol.model.DpNotifyModel;
import com.tuyasmart.stencil.utils.MessageUtil;
import com.tuyasmart.stencil.utils.PreferencesUtil;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.tuya.smart.rnsdk.camera.utils.Constants.ARG1_OPERATE_FAIL;
import static com.tuya.smart.rnsdk.camera.utils.Constants.ARG1_OPERATE_SUCCESS;
import static com.tuya.smart.rnsdk.camera.utils.Constants.MSG_DATA_DATE;
import static com.tuya.smart.rnsdk.camera.utils.Constants.MSG_DATA_DATE_BY_DAY_FAIL;
import static com.tuya.smart.rnsdk.camera.utils.Constants.MSG_DATA_DATE_BY_DAY_SUCC;

public class TuyaCameraModule extends ReactContextBaseJavaModule {
    public TuyaCameraView TuyaCameraView;
    ReactApplicationContext reactContext;
    public static  long HOME_ID = 1099001;
    private String picPath, localKey = "";
    private ICameraP2P mCameraP2P;
    private ITuyaSmartCameraP2P mSmartCameraP2P;
    private ITuyaCameraDevice mDeviceControl;
    private static ITuyaHomeCamera homeCamera;
    public static DeviceBean mCameraDevice;
    ITuyaCameraDevice mTuyaCameraDevice;
    protected Map<String, List<String>> mBackDataMonthCache;
    protected Map<String, List<TimePieceBean>> mBackDataDayCache;

    public TuyaCameraModule (ReactApplicationContext reactContext) {
        this.reactContext = reactContext;
    }

    @NonNull
    @Override
    public String getName() {
        return "TuyaCameraModule";
    }

    @ReactMethod
    public void getTumbnail(final Promise promise) {
        String path = null;
        try {
            path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/SmartLife/Thumbnail/CameraThumbnail.png";
            WritableMap returnParams = Arguments.createMap();
            returnParams.putString("path",path);

            JSONObject obj = new JSONObject();
            obj.put("path",path);
            promise.resolve(returnParams);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @ReactMethod
    public void getHomeDetails(final Promise promise) {
        TuyaHomeSdk.getHomeManagerInstance().queryHomeList(new ITuyaGetHomeListCallback() {
            @Override
            public void onSuccess(List<HomeBean> homeBeans) {
                if (homeBeans.size() == 0) {

                    return;
                }
                final long homeId = homeBeans.get(0).getHomeId();

                HOME_ID = homeId;
                PreferencesUtil.set("homeId", HOME_ID);
                TuyaHomeSdk.newHomeInstance(homeId).getHomeDetail(getHomeDetailsCallback(promise));
            }

            @Override
            public void onError(String errorCode, String error) {
                TuyaHomeSdk.newHomeInstance(HOME_ID).getHomeLocalCache(new ITuyaHomeResultCallback() {
                    @Override
                    public void onSuccess(HomeBean bean) {
                        promise.resolve(TuyaReactUtils.INSTANCE.parseToWritableMap(bean));
                    }

                    @Override
                    public void onError(String errorCode, String errorMsg) {
                        promise.reject(errorCode, errorMsg);

                    }
                });
            }
        });
    }

    @ReactMethod
    public void openLivePreview(Double reactTag, ReadableMap params) {

        Intent intent = new Intent(reactContext, CameraLivePreviewActivity.class);

        if(intent.resolveActivity(reactContext.getPackageManager()) != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra(Constants.INTENT_DEVID, params.getString("devId"));
            intent.putExtra(Constants.INTENT_COUNTRY_CODE, params.getString("countryCode"));
            intent.putExtra(Constants.INTENT_UID, params.getString("uid"));
            intent.putExtra(Constants.INTENT_PASSWD, params.getString("passwd"));

            reactContext.startActivity(intent);

            Log.d("ReactNative","Starting Activity");
        }
    }

    @ReactMethod
    public void getDetails(ReadableMap params, final Promise promise) {
        String devId = params.getString("devId");
        registerCameraDevice(devId);

        WritableMap returnParams = Arguments.createMap();
        WritableMap camInfo = getCameraInfo(devId);
        WritableMap camStatusInfo = getCameraStatusInfo();

        returnParams.putMap("info", camInfo);
        returnParams.putMap("status", camStatusInfo);

        promise.resolve(returnParams);
    }

    @ReactMethod
    public void changeCameraIndicatorStatus(ReadableMap params, final Promise promise) {
        if(mTuyaCameraDevice != null ) {
            changeIndicatorStatus(promise);
        } else {
            registerCameraDevice(params.getString("devId"));
        }
    }

    @ReactMethod
    public void changeCameraNightVision(ReadableMap params, final Promise promise) {
        if(mTuyaCameraDevice != null ) {
            changeNightVision(params,promise);
        } else {
            registerCameraDevice(params.getString("devId"));

        }
    }

    @Nullable
    public final ITuyaHomeResultCallback getHomeDetailsCallback(@NotNull final Promise promise) {
        ITuyaHomeResultCallback callback = new ITuyaHomeResultCallback() {
            @Override
            public void onSuccess(HomeBean bean) {
                promise.resolve(TuyaReactUtils.INSTANCE.parseToWritableMap(bean));
            }

            @Override
            public void onError(String errorCode, String errorMsg) {
                promise.reject(errorCode, errorMsg);
            }
        };
        return callback;
    }

    private void registerCameraDevice(String devId) {
        mTuyaCameraDevice = TuyaCameraDeviceControlSDK.getCameraDeviceInstance(devId);
        mTuyaCameraDevice.setRegisterDevListener(new IDevListener()  {
            @Override
            public void onDpUpdate(String s, String s1) {
                L.d("TuyaHomeSdk", "onDpUpdate devId:" + s + "  dps " + s1);
                //此处监听所有dp点的信息
            }

            @Override
            public void onRemoved(String s) {

            }

            @Override
            public void onStatusChanged(String s, boolean b) {

            }

            @Override
            public void onNetworkStatusChanged(String s, boolean b) {

            }

            @Override
            public void onDevInfoUpdate(String s) {

            }
        });
    }

    public WritableMap getCameraInfo(String devId) {
        WritableMap camInfo = Arguments.createMap();
        mCameraDevice =  getCameraDevice(devId);
        if(mCameraDevice != null) {
            camInfo.putString("ip", mCameraDevice.getIp());
            camInfo.putString("devId", mCameraDevice.getDevId());
            camInfo.putString("timeZone", mCameraDevice.getTimezoneId());
        }
        return camInfo;
    }

    public WritableMap getCameraStatusInfo() {
        WritableMap camStatusInfo = Arguments.createMap();

        String indicatorStatus = getIndicatorStatus(mTuyaCameraDevice);
        String nightVisionStatus = getNightVisionStatus(mTuyaCameraDevice);

        camStatusInfo.putString("indicator_status", indicatorStatus);
        camStatusInfo.putString("night_vision_status", nightVisionStatus);

        return camStatusInfo;
    }

    public String getIndicatorStatus(ITuyaCameraDevice mTuyaCameraDevice) {
        String indicatorStatus = "";
        if (mTuyaCameraDevice.isSupportCameraDps(DpBasicIndicator.ID)) {
            boolean status = mTuyaCameraDevice.queryBooleanCameraDps(DpBasicIndicator.ID);
            String result = status == true ? "1" : "0";
            indicatorStatus = result;

        } else {
           indicatorStatus = "-1";
        }
        return indicatorStatus;
    }

    public String getNightVisionStatus(ITuyaCameraDevice mTuyaCameraDevice) {
        String nightVisionStatus = "";
        if (mTuyaCameraDevice.isSupportCameraDps(DpBasicNightvision.ID)) {
            String status = mTuyaCameraDevice.queryStringCurrentCameraDps(DpBasicNightvision.ID);

            nightVisionStatus = status;

        } else {
            nightVisionStatus = "-1";
        }
        return nightVisionStatus;
    }


    public void changeIndicatorStatus(final Promise promise) {
        if( mTuyaCameraDevice.isSupportCameraDps(DpBasicIndicator.ID)) {
            boolean currentStatus = mTuyaCameraDevice.queryBooleanCameraDps(DpBasicIndicator.ID);
            mTuyaCameraDevice.registorTuyaCameraDeviceControlCallback(DpBasicIndicator.ID, new ITuyaCameraDeviceControlCallback<Boolean>() {
                @Override
                public void onSuccess(String s, DpNotifyModel.ACTION action, DpNotifyModel.SUB_ACTION sub_action, Boolean status) {
                    String result = status == true ? "1" : "0";
                    promise.resolve(result);
                }

                @Override
                public void onFailure(String s, DpNotifyModel.ACTION action, DpNotifyModel.SUB_ACTION sub_action, String s1, String s2) {
                    promise.reject("-1", "Failure in indicator status change");
                }
            });
            mTuyaCameraDevice.publishCameraDps(DpBasicIndicator.ID, !currentStatus);
        } else {
            promise.reject("-1", "Camera not support.");
        }
    }

    public void changeNightVision(ReadableMap params, final Promise promise) {
        if( mTuyaCameraDevice.isSupportCameraDps(DpBasicNightvision.ID)) {
            String currentStatus = mTuyaCameraDevice.queryStringCurrentCameraDps(DpBasicNightvision.ID);
            mTuyaCameraDevice.registorTuyaCameraDeviceControlCallback(DpBasicNightvision.ID, new ITuyaCameraDeviceControlCallback<String>() {
                @Override
                public void onSuccess(String s, DpNotifyModel.ACTION action, DpNotifyModel.SUB_ACTION sub_action, String status) {
                    promise.resolve(status);
                }

                @Override
                public void onFailure(String s, DpNotifyModel.ACTION action, DpNotifyModel.SUB_ACTION sub_action, String s1, String s2) {
                    promise.reject("-1", "Failure in indicator status change");
                }
            });
            mTuyaCameraDevice.publishCameraDps(DpBasicNightvision.ID, params.getString("nightMode"));
        } else {
            promise.reject("-1", "Camera not support.");
        }
    }

    private DeviceBean getCameraDevice(String devId) {
        DeviceBean deviceBean = TuyaHomeSdk.getDataInstance().getDeviceBean(devId);
        return deviceBean;
    }

    @ReactMethod
    public void getPlayBackConfigInfo(ReadableMap params, final Promise promise) {
        String devId = params.getString("devId");

        mCameraDevice =  getCameraDevice(devId);
        if(mCameraDevice != null) {
            localKey = mCameraDevice.getLocalKey();
        }

        Map<String, Object> map = mCameraDevice.getSkills();
        int p2pType = -1;
        if (map == null || map.size() == 0) {
            p2pType = -1;
        } else {
            p2pType = (Integer) (map.get("p2pType"));
        }

        int intentP2pType = p2pType;
        int sdkProvider = intentP2pType == 1 ? 1 : 2;

        mSmartCameraP2P = new TuyaSmartCameraP2P();

        WritableMap camInfo = Arguments.createMap();
        camInfo.putString("localKey", localKey);
        camInfo.putInt("p2pType", sdkProvider);

        getP2pInfo(devId, camInfo, mSmartCameraP2P, promise);
    }

    private void getP2pInfo(String devId, final WritableMap params, ITuyaSmartCameraP2P mSmartCameraP2P, final Promise promise) {
        mSmartCameraP2P.requestCameraInfo(devId, new ICameraConfig() {
            @Override
            public void onFailure(BusinessResponse var1, ConfigCameraBean var2, String var3) {
                promise.reject("-1", "Failure.");

            }

            @Override
            public void onSuccess(BusinessResponse var1, ConfigCameraBean var2, String var3) {

                String p2pWd = var2.getPassword();
                String p2pId = var2.getP2pId();

                params.putString("p2pWd", p2pWd);
                params.putString("p2pId", p2pId);

                promise.resolve(params);

            }
        });
    }

    @ReactMethod
    public void getHistoryTest(final Promise promise) {
        promise.resolve("test data");
    }

    @ReactMethod
    public void getCameraHistoryData(final Promise promise) {
        promise.resolve("test data");

    }

    @ReactMethod
    public void getHistoryData(ReadableMap params, final Promise promise) {
        mBackDataMonthCache = new HashMap<>();
        mBackDataDayCache = new HashMap<>();
        final String date = params.getString("date");

        if (TextUtils.isEmpty(date)) {
            return;
        }
        if (date.contains("/")) {
            String[] substring = date.split("/");
            if (substring.length > 2) {
                try {
                    int year = Integer.parseInt(substring[0]);
                    int mouth = Integer.parseInt(substring[1]);
                    int queryDay = Integer.parseInt(substring[2]);
                    mCameraP2P.queryRecordDaysByMonth(year, mouth, new OperationDelegateCallBack() {
                        @Override
                        public void onSuccess(int sessionId, int requestId, String data) {
                            MonthDays monthDays = com.alibaba.fastjson.JSONObject.parseObject(data, MonthDays.class);
                            mBackDataMonthCache.put(mCameraP2P.getMonthKey(), monthDays.getDataDays());
                            L.e("TAG",   "MonthDays --- " + data);
                            handleDataDate(date, MessageUtil.getMessage(MSG_DATA_DATE, ARG1_OPERATE_SUCCESS, data), promise);

//                            mHandler.sendMessage(MessageUtil.getMessage(MSG_DATA_DATE, ARG1_OPERATE_SUCCESS, data));
                        }

                        @Override
                        public void onFailure(int sessionId, int requestId, int errCode) {
                            promise.reject("-1", "Failure.");
                        }
                    });
                } catch (Exception e) {
                   // ToastUtil.shortToast(context, "Input Error");
                    promise.reject("-1", "Failure.");
                }
            }
        }
    }

    private void handleDataDate(final String date, Message msg, final Promise promise) {
        if (msg.arg1 == ARG1_OPERATE_SUCCESS) {
            List<String> days = mBackDataMonthCache.get(mCameraP2P.getMonthKey());

            try {
                if (days.size() == 0) {
                    // showErrorToast();
                    return;
                }
                if (!TextUtils.isEmpty(date) && date.contains("/")) {
                    String[] substring = date.split("/");
                    int year = Integer.parseInt(substring[0]);
                    int mouth = Integer.parseInt(substring[1]);
                    int day = Integer.parseInt(substring[2]);
                    mCameraP2P.queryRecordTimeSliceByDay(year, mouth, day, new OperationDelegateCallBack() {
                        @Override
                        public void onSuccess(int sessionId, int requestId, String data) {
                            L.e("TAG", date + " --- " + data);
                            parsePlaybackData(data, promise);
                        }

                        @Override
                        public void onFailure(int sessionId, int requestId, int errCode) {
                            promise.reject("-1", "Failure.");
                        }
                    });
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else {

        }
    }

    private void parsePlaybackData(Object obj, final Promise promise) {
        RecordInfoBean recordInfoBean = com.alibaba.fastjson.JSONObject.parseObject(obj.toString(), RecordInfoBean.class);
        if (recordInfoBean.getCount() != 0) {
            List<TimePieceBean> timePieceBeanList = recordInfoBean.getItems();
            if (timePieceBeanList != null && timePieceBeanList.size() != 0) {
                mBackDataDayCache.put(mCameraP2P.getDayKey(), timePieceBeanList);
            }
            handleDataDay(MessageUtil.getMessage(MSG_DATA_DATE_BY_DAY_SUCC, ARG1_OPERATE_SUCCESS), promise);
            //mHandler.sendMessage(MessageUtil.getMessage(MSG_DATA_DATE_BY_DAY_SUCC, ARG1_OPERATE_SUCCESS));
        } else {
            promise.reject("-1", "Failure.");

        }
    }

    private void handleDataDay(Message msg, final Promise promise) {
        if (msg.arg1 == ARG1_OPERATE_SUCCESS) {
            //Timepieces with data for the query day
            List<TimePieceBean> timePieceBeans = mBackDataDayCache.get(mCameraP2P.getDayKey());
            if (timePieceBeans != null) {
                Log.d("TAG", "Time piece bean list"+timePieceBeans.size());
                promise.resolve(timePieceBeans);

            } else {
                promise.reject("0", "No data.");
            }

        } else {

        }
    }
}
