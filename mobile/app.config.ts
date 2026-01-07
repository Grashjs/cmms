import 'dotenv/config';
import { ExpoConfig, ConfigContext } from 'expo/config';

const apiUrl = process.env.API_URL;
const googleServicesJson = process.env.GOOGLE_SERVICES_JSON;

export default ({ config }: ConfigContext): ExpoConfig => ({
  ...config,
  name: 'Atlas CMMS',
  slug: 'atlas-cmms',
  version: '1.0.34',
  orientation: 'portrait',
  icon: './assets/images/icon.png',
  scheme: 'atlascmms',
  userInterfaceStyle: 'automatic',
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
    url: 'https://u.expo.dev/803b5007-0c60-4030-ac3a-c7630b223b92'
  },
  ios: {
    bundleIdentifier: 'com.cmms.atlas',
    buildNumber: '9',
    jsEngine: 'jsc',
    supportsTablet: false,
    runtimeVersion: '1.0.34',
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
    googleServicesFile:
      googleServicesJson ?? './android/app/google-services.json',
    runtimeVersion: '1.0.34' // Changed from policy object to fixed string
  },
  web: {
    favicon: './assets/images/favicon.png'
  },
  extra: {
    API_URL: apiUrl,
    eas: {
      projectId: '803b5007-0c60-4030-ac3a-c7630b223b92'
    },
    updates: {
      assetPatternsToBeBundled: ['**/*']
    }
  },
  plugins: [
    ['react-native-nfc-manager'],
    [
      'expo-asset',
      {
        assets: ['./assets']
      }
    ],
    'expo-font',
    [
      'expo-barcode-scanner',
      {
        cameraPermission: 'Allow Atlas to access camera.'
      }
    ],
    'expo-notifications',
    'expo-build-properties'
  ]
});
