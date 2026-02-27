const isBrowser = typeof window !== "undefined";

const getRuntimeValue = (key: string, defaultValue = ""): string => {
  const runtimeValue = isBrowser ? window.__RUNTIME_CONFIG__?.[key]?.trim() : undefined;
  return runtimeValue || defaultValue;
};

// Next.js requires static references to NEXT_PUBLIC_ vars
const env = {
  MAIN_APP_URL: process.env.NEXT_PUBLIC_MAIN_APP_URL,
  API_URL: process.env.NEXT_PUBLIC_API_URL,
  LOGO_PATHS: process.env.NEXT_PUBLIC_LOGO_PATHS,
  CUSTOM_COLORS: process.env.NEXT_PUBLIC_CUSTOM_COLORS,
  BRAND_CONFIG: process.env.NEXT_PUBLIC_BRAND_CONFIG,
  DEMO_LINK: process.env.NEXT_PUBLIC_DEMO_LINK,
  GOOGLE_TRACKING_ID: process.env.NEXT_PUBLIC_GOOGLE_TRACKING_ID,
  PADDLE_SECRET_TOKEN: process.env.NEXT_PUBLIC_PADDLE_SECRET_TOKEN,
  PADDLE_ENVIRONMENT: process.env.NEXT_PUBLIC_PADDLE_ENVIRONMENT,
};

const getValue = (key: keyof typeof env, defaultValue = ""): string => {
  return env[key] || getRuntimeValue(key, defaultValue);
};

const rawApiUrl = getValue("API_URL");
export const apiUrl = rawApiUrl ? (rawApiUrl.endsWith("/") ? rawApiUrl : rawApiUrl + "/") : "http://localhost:8080/";

const rawMainAppUrl = getValue("MAIN_APP_URL");
export const mainAppUrl = rawMainAppUrl
  ? rawMainAppUrl.endsWith("/")
    ? rawMainAppUrl
    : rawMainAppUrl + "/"
  : "http://localhost:3000/";

export const googleTrackingId = getValue("GOOGLE_TRACKING_ID");

export const isCloudVersion = true;

const apiHostName = new URL(apiUrl).hostname;
export const IS_LOCALHOST = apiHostName === "localhost" || apiHostName === "127.0.0.1";

export const customLogoPaths: { white?: string; dark: string } | null = getValue("LOGO_PATHS")
  ? JSON.parse(getValue("LOGO_PATHS"))
  : null;

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

export const customColors: ThemeColors | null = getValue("CUSTOM_COLORS")
  ? JSON.parse(getValue("CUSTOM_COLORS"))
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

export const brandRawConfig: BrandRawConfig | null = getValue("BRAND_CONFIG")
  ? JSON.parse(getValue("BRAND_CONFIG"))
  : null;

export const demoLink: string = getValue("DEMO_LINK");

export const isWhiteLabeled: boolean = !!(customLogoPaths || brandRawConfig);

export const IS_ORIGINAL_CLOUD = !isWhiteLabeled && isCloudVersion;

export const PADDLE_SECRET_TOKEN: string = getValue("PADDLE_SECRET_TOKEN");

export const paddleEnvironment = getValue("PADDLE_ENVIRONMENT") as "sandbox" | "production";
