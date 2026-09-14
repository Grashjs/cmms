import 'dotenv/config';
import { ExpoConfig, ConfigContext } from 'expo/config';
import fs from 'fs';
import path from 'path';

const apiUrl = process.env.API_URL;
const clarityId = process.env.CLARITY_ID;
const googleServicesPlist = process.env.GOOGLE_SERVICES_PLIST;
const sentryDsn = process.env.SENTRY_DSN;
const sentryEnvironment = process.env.SENTRY_ENVIRONMENT || 'production';

console.warn('KHHI - ', process.env);
const androidGoogleServicesPath = path.resolve(
  __dirname,
  'android/app/google-services.json'
);
if (process.env.GOOGLE_SERVICES_BASE64) {
  fs.writeFileSync(
    androidGoogleServicesPath,
    Buffer.from(process.env.GOOGLE_SERVICES_BASE64, 'base64').toString('utf-8')
  );
}

const plugins: ExpoConfig['plugins'] = [
  'react-native-nfc-manager',
  'expo-font',
  'expo-notifications',
  '@react-native-community/datetimepicker',
  '@react-native-firebase/app',
  './plugins/ios/withFmtXcode26Fix',
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
        compileSdkVersion: 36,
        targetSdkVersion: 36
      }
    }
  ]
];

if (process.env.SENTRY_AUTH_TOKEN) {
  plugins.push([
    '@sentry/react-native/expo',
    {
      url: 'https://sentry.io/',
      organization: process.env.SENTRY_ORG,
      project: 'mobile',
      authToken: process.env.SENTRY_AUTH_TOKEN
    }
  ]);
}

export default ({ config }: ConfigContext): ExpoConfig => ({
  ...config,
  name: 'Atlas CMMS',
  slug: 'atlas-cmms',
  version: '1.0.48',
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
    buildNumber: '2',
    jsEngine: 'hermes',
    supportsTablet: false,
    runtimeVersion: 'appVersion',
    googleServicesFile: googleServicesPlist ?? './GoogleService-Info.plist',
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
    edgeToEdgeEnabled: true,
    googleServicesFile: androidGoogleServicesPath,
    runtimeVersion: 'appVersion' // Changed from policy object to fixed string
  },
  web: {
    favicon: './assets/images/favicon.png'
  },
  extra: {
    API_URL: apiUrl,
    CLARITY_ID: clarityId,
    SENTRY_DSN: sentryDsn,
    SENTRY_ENVIRONMENT: sentryEnvironment,
    eas: {
      projectId: '803b5007-0c60-4030-ac3a-c7630b223b92'
    }
  },
  plugins
});
