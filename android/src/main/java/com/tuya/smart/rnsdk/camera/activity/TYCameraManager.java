package com.tuya.smart.rnsdk.camera.activity;

import android.app.Activity;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.common.MapBuilder;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.ViewGroupManager;
import com.facebook.react.uimanager.annotations.ReactProp;

import java.util.Map;

import androidx.annotation.NonNull;

public class TYCameraManager extends ViewGroupManager<CustomCameraView> {

    public static final String REACT_CLASS = "TYCamera";
    ReactApplicationContext mCallerContext;
    Activity mActivity;

    public TYCameraManager(Activity activity , ReactApplicationContext context) {
       // mActivity = activity;
        mCallerContext = context;
        mActivity = context.getCurrentActivity();
    }

    @Override
    public String getName() {
        return REACT_CLASS;
    }

    @ReactProp(name = "devId")
    public void setDevId(CustomCameraView view, String devId) {
        view.setDevId(devId);
        view.init(mActivity, mCallerContext);
    }

    @ReactProp(name = "folderPath")
    public void setFolderPath(CustomCameraView view, String folderPath) {
        view.setFolderPath(folderPath);
    }

    @NonNull
    @Override
    protected CustomCameraView createViewInstance(@NonNull ThemedReactContext reactContext) {
        //return new CustomCameraView(reactContext, Fresco.newDraweeControllerBuilder(), null, mCallerContext);
        return new CustomCameraView(reactContext);
    }

        public Map getExportedCustomBubblingEventTypeConstants() {
        return MapBuilder.builder()
                .put(
                 "onSettingsClick",
                MapBuilder.of("phasedRegistrationNames",MapBuilder.of("bubbled", "onSettingsPress")))
                .put(
                   "onPhotoAlbumClick",
                MapBuilder.of("phasedRegistrationNames",MapBuilder.of("bubbled", "onSettingsPress")))
                .build();
    }

    // public Map getExportedCustomBubblingEventTypeConstants() {
    //     return MapBuilder.builder()
    //             .put(
    //              "onSettingsClick",
    //             MapBuilder.of("phasedRegistrationNames",MapBuilder.of("bubbled", "onSettingsPress")))
    //             .build();
    // }
}
