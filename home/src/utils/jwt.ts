import * as jose from 'jose';
import { Buffer } from 'buffer';

window.Buffer = Buffer;
/* eslint-disable no-bitwise */
export const JWT_EXPIRES_IN = 3600 * 24 * 14;

export const sign = (
  payload: Record<string, any>,
  privateKey: string,
  header: Record<string, any>
) => {
  const now = new Date();
  header.expiresIn = new Date(now.getTime() + header.expiresIn);
  const encodedHeader = btoa(JSON.stringify(header));
  const encodedPayload = btoa(JSON.stringify(payload));
  const signature = btoa(
    Array.from(encodedPayload)
      .map((item, key) =>
        String.fromCharCode(
          item.charCodeAt(0) ^ privateKey[key % privateKey.length].charCodeAt(0)
        )
      )
      .join('')
  );

  return `${encodedHeader}.${encodedPayload}.${signature}`;
};

export const verify = async (token: string): Promise<boolean> => {
  try {
    const decoded = await jose.decodeJwt(token);
    const currentTime = new Date().getTime() / 1000;
    return currentTime <= decoded.exp;
  } catch (error) {
    // Token is invalid or expired
    return false;
  }
};
