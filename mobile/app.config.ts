import 'dotenv/config';
import { ExpoConfig, ConfigContext } from 'expo/config';

const apiUrl = process.env.API_URL;

export default ({ config }: ConfigContext): ExpoConfig => ({
  ...config,
  name: 'Atlas CMMS',
  slug: 'atlas-cmms',
  version: '1.0.29',
  orientation: 'portrait',
  icon: './assets/images/icon.png',
  scheme: 'atlascmms',
  userInterfaceStyle: 'automatic',
  notification: {
    icon: './assets/images/notification.png'
  },
  sdkVersion: '48.0.0',
  splash: {
    image: './assets/images/splash.png',
    resizeMode: 'contain',
    backgroundColor: '#ffffff'
  },
  updates: {
    fallbackToCacheTimeout: 0,
    url: 'https://u.expo.dev/803b5007-0c60-4030-ac3a-c7630b223b92'
  },
  assetBundlePatterns: ['**/*'],
  ios: {
    bundleIdentifier: 'com.cmms.atlas',
    buildNumber: '7',
    jsEngine: 'jsc',
    supportsTablet: false,
    runtimeVersion: {
      policy: 'sdkVersion'
    },
    infoPlist: {
      ITSAppUsesNonExemptEncryption: false,
    }
  },
  android: {
    adaptiveIcon: {
      foregroundImage: './assets/images/adaptive-icon.png',
      backgroundColor: '#ffffff'
    },
    versionCode: 26,
    package: 'com.atlas.cmms',
    googleServicesFile: './google-services.json',
    runtimeVersion: '1.0.0'
  },
  web: {
    favicon: './assets/images/favicon.png'
  },
  extra: {
    API_URL: apiUrl,
    eas: {
      projectId: '803b5007-0c60-4030-ac3a-c7630b223b92'
    }
  },
  plugins: [
    [
      'expo-barcode-scanner',
      {
        cameraPermission: 'Allow Atlas to access camera.'
      }
    ]
  ]
});
