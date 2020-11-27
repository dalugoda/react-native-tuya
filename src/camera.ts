import { NativeModules } from 'react-native';

const tuya = NativeModules.TuyaCameraModule;

export type CameraLivePreviewParams = {
  countryCode: string;
  uid: string;
  passwd: string;
  devId: string;
};

export type CameraDetailsParams = {
  devId: string;
};

export type CameraIndicatorParams = {
  devId: string;
};

export type CameraNightVisionParams = {
  devId: string;
  nightMode: string;
};

export type CameraPlaybackConfigParams = {
  devId: string;
};

export type CameraHistoryParams = {
  selectedDate: string;
};

export function testFunction() {
  return tuya.testFunction();
}

export function openCameraLivePreview(reactTag:number, params: CameraLivePreviewParams): Promise<string> {
  return tuya.openLivePreview(reactTag, params);
}

export function getCameraTumbnail(): Promise<any> {
  return tuya.getTumbnail();
}

export function getHomeDetails(): Promise<any> {
  return tuya.getHomeDetails();
}

export function getDetails(params: CameraDetailsParams): Promise<any> {
  return tuya.getDetails(params);
}

export function changeCameraIndicatorStatus(params: CameraIndicatorParams): Promise<any> {
  return tuya.changeCameraIndicatorStatus(params);
}

export function changeCameraNightVision(params: CameraNightVisionParams): Promise<any> {
  return tuya.changeCameraNightVision(params);
}

export function getPlayBackConfigInfo(params: CameraPlaybackConfigParams): Promise<any> {
  return tuya.getPlayBackConfigInfo(params);
}

export function getCameraHistoryData(params: CameraHistoryParams): Promise<any> {
  return tuya.getHistoryData(params);
}

export function playCameraHistory(): Promise<any> {
  return tuya.playHistory();
}

// export function historyDataPlay(): Promise<any> {
//   return tuya.getTumbnail();
// }
// export function getCameraHistory(): Promise<any> {
//   return tuya.getCameraHistoryData();
// }

// export function getHistoryData(params: CameraHistoryParams): Promise<any> {
//   return tuya.getHistoryData(params);
// }

// export function playHistoryTest(): Promise<any> {
//   return tuya.getTumbnail();
// }


