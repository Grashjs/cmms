import type { Buffer as NodeBuffer } from 'buffer';

export {};

declare global {
  interface Window {
    /**
     * Set by utils/jwt.ts, which needs Buffer in the browser. Declared here on purpose:
     * it used to typecheck only because some transitive @types package happened to
     * augment the global scope, and it stopped the moment that package was removed with
     * the dead dependencies. An implicit type is not a contract.
     */
    Buffer: typeof NodeBuffer;
    __RUNTIME_CONFIG__: {
      CLOUD_VERSION: string;
      INVITATION_VIA_EMAIL: string;
      GOOGLE_TRACKING_ID: string;
      GOOGLE_KEY: string;
      API_URL: string;
      NODE_ENV: string;
    };
  }
}
