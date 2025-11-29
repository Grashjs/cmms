import 'dotenv/config';
import { ExpoConfig, ConfigContext } from 'expo/config';

const apiUrl = process.env.API_URL;
const googleServicesJson = process.env.GOOGLE_SERVICES_JSON;

export default ({ config }: ConfigContext): ExpoConfig => ({
  ...config,
  name: 'Atlas CMMS',
  slug: 'atlas-cmms',
  version: '1.0.31',
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
  assetBundlePatterns: ['**/*'],
  ios: {
    bundleIdentifier: 'com.cmms.atlas',
    buildNumber: '9',
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
    versionCode: 31,
    package: 'com.atlas.cmms',
    googleServicesFile: googleServicesJson ?? './google-services.json',
    runtimeVersion: {
      policy: 'sdkVersion'
    }
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
    'expo-asset',
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
