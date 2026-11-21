import { UserInvitationMiniDTO } from '../models/owns/user';

// Invitation secrets stay in the URL fragment: browsers do not send it to
// HTTP servers or in Referrer headers. Do not accept the old email/role links.
export function getInvitationToken(hash: string): string | null {
  return new URLSearchParams(hash.replace(/^#/, '')).get('invitationToken');
}

export function isInvitationPage(): boolean {
  return (
    window.location.pathname === '/account/register' &&
    new URLSearchParams(window.location.hash.replace(/^#/, '')).has(
      'invitationToken'
    )
  );
}

// New emissions are prepended: keep the most recent role for each address.
export function uniquePendingInvitations(
  invitations: UserInvitationMiniDTO[]
): UserInvitationMiniDTO[] {
  const seen = new Set<string>();
  return invitations.filter(({ email }) => {
    const key = email.trim().toLowerCase();
    if (seen.has(key)) return false;
    seen.add(key);
    return true;
  });
}
