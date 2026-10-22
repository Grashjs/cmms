import { Alert, Box, Stack, TextField, Typography } from '@mui/material';
import { useTranslation } from 'react-i18next';
import { InvitationDelivery } from '../../../../models/owns/user';

export default function InvitationLinks({
  invitations
}: {
  invitations: InvitationDelivery[];
}) {
  const { t } = useTranslation();
  return (
    <Stack spacing={2}>
      <Alert severity="info">{t('invitation_links_notice')}</Alert>
      {invitations.map((invitation) => (
        <Box key={invitation.email} data-clarity-mask="true">
          <Typography>{invitation.email}</Typography>
          <TextField
            fullWidth
            label={t('invitation_link')}
            value={invitation.invitationLink}
            InputProps={{ readOnly: true }}
            onFocus={(event) => event.target.select()}
          />
          <Typography variant="caption">
            {t('invitation_expires', {
              date: new Date(invitation.expiresAt).toLocaleString()
            })}
          </Typography>
        </Box>
      ))}
    </Stack>
  );
}
