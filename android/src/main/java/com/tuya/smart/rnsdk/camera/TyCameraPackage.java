package com.tuya.smart.rnsdk.camera;

import com.facebook.react.ReactPackage;
import com.facebook.react.bridge.NativeModule;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.uimanager.ViewManager;
import com.tuya.smart.rnsdk.camera.activity.TYCameraManager;
import com.tuya.smart.rnsdk.camera.activity.TYCameraPlaybackManager;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import androidx.annotation.NonNull;

public class TyCameraPackage implements ReactPackage {
    @NonNull
    @Override
    public List<NativeModule> createNativeModules(@NonNull ReactApplicationContext reactContext) {
        return Collections.emptyList();
    }

    @NonNull
    @Override
    public List<ViewManager> createViewManagers(@NonNull ReactApplicationContext reactContext) {
        return Arrays.<ViewManager>asList(
                new TYCameraManager(reactContext.getCurrentActivity(), reactContext),
                new TYCameraPlaybackManager(reactContext.getCurrentActivity(), reactContext)
        );
    }
}
