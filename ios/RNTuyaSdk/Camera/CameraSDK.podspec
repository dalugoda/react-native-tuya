
Pod::Spec.new do |s|
  s.name             = 'CameraSDK'
  s.version          = '0.8.2'
  s.summary          = 'A short description of CameraSDK.'

  s.description      = <<-DESC
TODO: Add long description of the pod here.
                       DESC

  s.homepage         = 'https://registry.code.tuya-inc.top/tuyaIOSSDK/TYSDKDemo.git'
  s.license          = { :type => 'MIT', :file => 'LICENSE' }
  s.author           = { '527548875@qq.com' => 'huangkai@tuya.com' }
  s.source           = { :git => 'https://registry.code.tuya-inc.top/tuyaIOSSDK/TYSDKDemo.git', :tag => s.version.to_s }
  
  s.ios.deployment_target = '8.0'
  
  s.default_subspec = 'UserInfo'
  
  s.subspec 'Base' do |ss|
    ss.source_files = 'Camera/Classes/Base/**/*.{h,m}', 'Camera/Classes/Manager/**/*.{h,m}'
    ss.resources = 'Camera/Classes/Base/Assets/**/*'
    
#    ss.resource_bundles = {
#      'base' => 'TuyaSmartDemo/Classes/Base/Assets/*.{lproj,json}'
#    }
#    
    ss.prefix_header_contents = '#ifdef __OBJC__',
    '#import "TYDemoTheme.h"',
    '#import "TPDemoViewConstants.h"',
    '#import "UIView+TPDemoAdditions.h"',
    '#endif'
    
    ss.dependency 'MBProgressHUD', '~> 0.9.2'
#    ss.dependency 'BlocksKit'
    ss.dependency 'Reachability'
    ss.dependency 'YYModel'
    
    ss.dependency 'TuyaSmartBaseKit'
  end
  
  s.subspec 'Login' do |ss|
    ss.source_files = 'Camera/Classes/Login/**/*.{h,m}'
    ss.dependency 'CameraSDK/Base'
    
    ss.dependency 'TuyaSmartBaseKit'
  end
  
  s.subspec 'SmartScene' do |ss|
    ss.source_files = 'Camera/Classes/SmartScene/**/*.{h,m}'

    ss.dependency 'CameraSDK/Base'
    ss.dependency 'SDWebImage'
    ss.dependency 'TuyaSmartSceneKit'
  end
  
  s.subspec 'DeviceList' do |ss|
    ss.source_files = 'Camera/Classes/DeviceList/**/*.{h,m}'
    ss.resources = 'Camera/Classes/DeviceList/Assets/**/*'
    
    ss.dependency 'CameraSDK/Base'
    
    ss.dependency 'SDWebImage'
    ss.dependency 'TuyaSmartDeviceKit'
  end
  
  s.subspec 'AddDevice' do |ss|
    ss.source_files = 'Camera/Classes/AddDevice/**/*.{h,m}'
    ss.dependency 'CameraSDK/Base'
    
    ss.dependency 'SDWebImage'
    ss.dependency 'Masonry'
    ss.dependency 'TuyaSmartActivatorKit'
  end
  
  s.subspec 'UserInfo' do |ss|
    ss.source_files = 'Camera/Classes/UserInfo/**/*.{h,m}'
    ss.dependency 'CameraSDK/Base'
  end
  
  s.subspec 'IPC' do |ss|
    ss.source_files = 'Camera/Classes/IPC/**/*.{h,m}'
    ss.resources = 'Classes/IPC/Assets/*', 'Classes/IPC/Assets/*.lproj'
    
    ss.dependency 'CameraSDK/Base'
    ss.dependency 'TuyaSmartCameraKit'
    ss.dependency 'TYEncryptImage'
    ss.dependency 'DACircularProgress'
  end
  
  # s.resource_bundles = {
  #   'TuyaSmartDemo' => ['TuyaSmartDemo/Assets/*.png']
  # }

  # s.public_header_files = 'Pod/Classes/**/*.h'
  # s.frameworks = 'UIKit', 'MapKit'
  # s.dependency 'AFNetworking', '~> 2.3'
end
