import { Alert, CircularProgress, Stack, Typography } from '@mui/material';
import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { InvitationPreview } from '../../../../models/owns/user';
import api, { authHeader } from '../../../../utils/api';
import RegisterJWT from './RegisterJWT';

export default function InvitationRegistration({ token }: { token: string }) {
  const { t } = useTranslation();
  const [invitation, setInvitation] = useState<InvitationPreview>();
  const [failed, setFailed] = useState(false);

  useEffect(() => {
    let active = true;
    setInvitation(undefined);
    setFailed(false);
    if (!token) {
      setFailed(true);
      return;
    }
    api
      .post<InvitationPreview>(
        'auth/invitations/preview',
        { token },
        { headers: authHeader(true), cache: 'no-store' }
      )
      .then((response) => {
        if (active) setInvitation(response);
      })
      .catch(() => {
        if (active) setFailed(true);
      });
    return () => {
      active = false;
    };
  }, [token]);

  if (failed)
    return <Alert severity="error">{t('invitation_unavailable')}</Alert>;
  if (!invitation) return <CircularProgress aria-label={t('loading')} />;
  return (
    <Stack spacing={2}>
      <Typography>
        {t('invitation_join_details', {
          company: invitation.companyName,
          role: invitation.roleName
        })}
      </Typography>
      <RegisterJWT invitation={invitation} invitationToken={token} />
    </Stack>
  );
}
