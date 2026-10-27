import api, { authHeader } from './api';

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresAt: string;
}

// Shared by the mobile login screen and the invitation journey integration check.
export async function authenticate(
  email: string,
  password: string,
  ldap = false
): Promise<AuthResponse> {
  return api.post<AuthResponse>(
    `auth/signin${ldap ? '-ldap' : ''}`,
    ldap ? { username: email, password } : { email, type: 'client', password },
    { headers: await authHeader(true) }
  );
}
