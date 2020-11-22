package com.tuya.smart.rnsdk.camera.activity;

import android.app.Activity;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.ViewGroupManager;
import com.facebook.react.uimanager.annotations.ReactProp;

import androidx.annotation.NonNull;

public class TYCameraPlaybackManager extends ViewGroupManager<CameraPlaybackView> {
    public static final String REACT_CLASS = "TYCameraPlayback";
    ReactApplicationContext mCallerContext;
    Activity mActivity;

    public TYCameraPlaybackManager(Activity activity , ReactApplicationContext context) {
        mCallerContext = context;
        mActivity = context.getCurrentActivity();

    }

    @NonNull
    @Override
    public String getName() {
        return REACT_CLASS;
    }

    @ReactProp(name = "devId")
    public void setDevId(CameraPlaybackView view, String devId) {
        view.setDevId(devId);
       // view.init(mActivity, mCallerContext);
    }

    @ReactProp(name = "mIsRunSoft")
    public void setMIsRunSoft(CameraPlaybackView view, boolean mIsRunSoft) {
        view.setMIsRunSoft(mIsRunSoft);
    }

    @ReactProp(name = "p2pId")
    public void setP2pId(CameraPlaybackView view, String p2pId) {
        view.setP2pId(p2pId);
    }

    @ReactProp(name = "p2pWd")
    public void setP2pWd(CameraPlaybackView view, String p2pWd) {
        view.setP2pWd(p2pWd);
    }

    @ReactProp(name = "localKey")
    public void setLocalKey(CameraPlaybackView view, String localKey) {
        view.setLocalKey(localKey);
    }

    @ReactProp(name = "p2pType")
    public void setP2pType(CameraPlaybackView view, int p2pType) {
        view.setP2pType(p2pType);
    }

    @NonNull
    @Override
    protected CameraPlaybackView createViewInstance(@NonNull ThemedReactContext reactContext) {
        return new CameraPlaybackView(reactContext.getCurrentActivity(),reactContext);
    }
}
