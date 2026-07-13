export {};

declare global {
  interface Window {
    __RUNTIME_CONFIG__: {
      CLOUD_VERSION: string;
      INVITATION_VIA_EMAIL: string;
      GOOGLE_TRACKING_ID: string;
      GOOGLE_KEY: string;
      API_URL: string;
      NODE_ENV: string;
      MUI_X_LICENSE: string;
    };
  }
}
