const isBrowser = typeof window !== 'undefined';

const getRuntimeValue = (key: string, defaultValue = ''): string => {
  const runtimeValue = isBrowser
      ? window.__RUNTIME_CONFIG__?.[key]?.trim()
      : undefined;
  return runtimeValue || defaultValue;
};

// Next.js requires static references to NEXT_PUBLIC_ vars
const env = {
  API_KEY: process.env.NEXT_PUBLIC_API_KEY,
  AUTH_DOMAIN: process.env.NEXT_PUBLIC_AUTH_DOMAIN,
  DATABASE_URL: process.env.NEXT_PUBLIC_DATABASE_URL,
  PROJECT_ID: process.env.NEXT_PUBLIC_PROJECT_ID,
  STORAGE_BUCKET: process.env.NEXT_PUBLIC_STORAGE_BUCKET,
  MESSAGING_SENDER_ID: process.env.NEXT_PUBLIC_MESSAGING_SENDER_ID,
  ID: process.env.NEXT_PUBLIC_ID,
  MEASUREMENT_ID: process.env.NEXT_PUBLIC_MEASUREMENT_ID,
  GOOGLE_KEY: process.env.NEXT_PUBLIC_GOOGLE_KEY,
  API_URL: process.env.NEXT_PUBLIC_API_URL,
  MUI_X_LICENSE: process.env.NEXT_PUBLIC_MUI_X_LICENSE,
  GOOGLE_TRACKING_ID: process.env.NEXT_PUBLIC_GOOGLE_TRACKING_ID,
  OAUTH2_PROVIDER: process.env.NEXT_PUBLIC_OAUTH2_PROVIDER,
  INVITATION_VIA_EMAIL: process.env.NEXT_PUBLIC_INVITATION_VIA_EMAIL,
  CLOUD_VERSION: process.env.NEXT_PUBLIC_CLOUD_VERSION,
  ENABLE_SSO: process.env.NEXT_PUBLIC_ENABLE_SSO,
  LOGO_PATHS: process.env.NEXT_PUBLIC_LOGO_PATHS,
  CUSTOM_COLORS: process.env.NEXT_PUBLIC_CUSTOM_COLORS,
  BRAND_CONFIG: process.env.NEXT_PUBLIC_BRAND_CONFIG,
  DEMO_LINK: process.env.NEXT_PUBLIC_DEMO_LINK,
  PADDLE_SECRET_TOKEN: process.env.NEXT_PUBLIC_PADDLE_SECRET_TOKEN,
  PADDLE_ENVIRONMENT: process.env.NEXT_PUBLIC_PADDLE_ENVIRONMENT,
};

const getValue = (key: keyof typeof env, defaultValue = ''): string => {
  return env[key] || getRuntimeValue(key, defaultValue);
};

export const firebaseConfig = {
  apiKey: getValue('API_KEY'),
  authDomain: getValue('AUTH_DOMAIN'),
  databaseURL: getValue('DATABASE_URL'),
  projectId: getValue('PROJECT_ID'),
  storageBucket: getValue('STORAGE_BUCKET'),
  messagingSenderId: getValue('MESSAGING_SENDER_ID'),
  appId: getValue('ID'),
  measurementId: getValue('MEASUREMENT_ID')
};

export const googleMapsConfig = {
  apiKey: getValue('GOOGLE_KEY')
};

const rawApiUrl = getValue('API_URL');
export const apiUrl = rawApiUrl
    ? rawApiUrl.endsWith('/')
        ? rawApiUrl
        : rawApiUrl + '/'
    : 'http://localhost:8080/';

export const muiLicense = getValue('MUI_X_LICENSE');

export const zendeskKey = '';

export const googleTrackingId = getValue('GOOGLE_TRACKING_ID');
export const oauth2Provider = getValue('OAUTH2_PROVIDER') as
    | 'GOOGLE'
    | 'MICROSOFT';

export const isEmailVerificationEnabled =
    getValue('INVITATION_VIA_EMAIL') === 'true';

export const isCloudVersion = true;

const apiHostName = new URL(apiUrl).hostname;
export const IS_LOCALHOST =
    apiHostName === 'localhost' || apiHostName === '127.0.0.1';

export const isSSOEnabled = getValue('ENABLE_SSO') === 'true';

export const customLogoPaths: { white?: string; dark: string } | null =
    getValue('LOGO_PATHS') ? JSON.parse(getValue('LOGO_PATHS')) : null;

type ThemeColors = {
  primary: string;
  secondary: string;
  success: string;
  warning: string;
  error: string;
  info: string;
  black: string;
  white: string;
  primaryAlt: string;
};

export const customColors: ThemeColors | null = getValue('CUSTOM_COLORS')
    ? JSON.parse(getValue('CUSTOM_COLORS'))
    : null;

export interface BrandRawConfig {
  name: string;
  shortName: string;
  website: string;
  mail: string;
  addressStreet: string;
  phone: string;
  addressCity: string;
}

export const brandRawConfig: BrandRawConfig | null = getValue('BRAND_CONFIG')
    ? JSON.parse(getValue('BRAND_CONFIG'))
    : null;

export const demoLink: string = getValue('DEMO_LINK');

export const isWhiteLabeled: boolean = !!(customLogoPaths || brandRawConfig);

export const IS_ORIGINAL_CLOUD = !isWhiteLabeled && isCloudVersion;

export const PADDLE_SECRET_TOKEN: string = getValue('PADDLE_SECRET_TOKEN');

export const paddleEnvironment = getValue('PADDLE_ENVIRONMENT') as
    | 'sandbox'
    | 'production';