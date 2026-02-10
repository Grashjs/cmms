import 'dotenv/config';
import { ExpoConfig, ConfigContext } from 'expo/config';

const apiUrl = process.env.API_URL;
const googleServicesJson = process.env.GOOGLE_SERVICES_JSON;

export default ({ config }: ConfigContext): ExpoConfig => ({
  ...config,
  name: 'Atlas CMMS',
  slug: 'atlas-cmms',
  version: '1.0.37',
  orientation: 'portrait',
  icon: './assets/images/icon.png',
  scheme: 'atlascmms',
  userInterfaceStyle: 'automatic',
  newArchEnabled: false,
  notification: {
    icon: './assets/images/notification.png'
  },
  splash: {
    image: './assets/images/splash.png',
    resizeMode: 'contain',
    backgroundColor: '#ffffff'
  },
  updates: {
    fallbackToCacheTimeout: 0,
    url: 'https://u.expo.dev/803b5007-0c60-4030-ac3a-c7630b223b92',
    assetPatternsToBeBundled: ['**/*']
  },
  ios: {
    bundleIdentifier: 'com.cmms.atlas',
    buildNumber: '9',
    jsEngine: 'hermes',
    supportsTablet: false,
    runtimeVersion: '1.0.37',
    infoPlist: {
      ITSAppUsesNonExemptEncryption: false
    }
  },
  android: {
    adaptiveIcon: {
      foregroundImage: './assets/images/adaptive-icon.png',
      backgroundColor: '#ffffff'
    },
    versionCode: 31,
    package: 'com.atlas.cmms',
    jsEngine: 'hermes',
    googleServicesFile:
      googleServicesJson ?? './android/app/google-services.json',
    runtimeVersion: '1.0.37' // Changed from policy object to fixed string
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
    'react-native-nfc-manager',
    'expo-font',
    'expo-notifications',
    [
      'expo-camera',
      {
        cameraPermission: 'Allow Atlas to access camera.'
      }
    ],
    [
      'expo-build-properties',
      {
        ios: {
          useFrameworks: 'static',
          deploymentTarget: '15.1'
        },
        android: {
          compileSdkVersion: 35,
          targetSdkVersion: 35
        }
      }
    ]
  ]
});
