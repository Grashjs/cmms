import {
  Box,
  Button,
  Checkbox,
  Chip,
  CircularProgress,
  Dialog,
  DialogContent,
  DialogTitle,
  FormControlLabel,
  Grid,
  InputAdornment,
  Paper,
  TextField,
  Typography
} from '@mui/material';
import { grey } from '@mui/material/colors';
import UserRoleCardList from '../UserRoleCardList';
import { EmailOutlined } from '@mui/icons-material';
import { inviteUsers } from '../../../../slices/user';
import * as React from 'react';
import { useContext, useEffect, useRef, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { emailRegExp } from '../../../../utils/validators';
import { CustomSnackBarContext } from '../../../../contexts/CustomSnackBarContext';
import { useDispatch, useSelector } from '../../../../store';
import { isEmailVerificationEnabled } from '../../../../config';
import {
  InvitationDelivery,
  InvitationResponse
} from '../../../../models/owns/user';
import InvitationLinks from './InvitationLinks';
import { getErrorMessage } from '../../../../utils/api';

export default function InviteUserDialog({
  open,
  onClose,
  onRefreshUsers,
  initialEmail
}: {
  open: boolean;
  onClose: () => void;
  onRefreshUsers: () => void;
  initialEmail?: string;
}) {
  const [isInviteSubmitting, setIsInviteSubmitting] = useState(false);
  const requestVersion = useRef(0);
  const submitting = useRef(false);
  const mounted = useRef(true);
  const [roleId, setRoleId] = useState<number>();
  const { t } = useTranslation();
  const [emails, setEmails] = useState<string[]>([]);
  const bottomRef = useRef<HTMLDivElement>(null);
  const [currentEmail, setCurrentEmail] = useState<string>(initialEmail ?? '');
  const [sendEmail, setSendEmail] = useState(isEmailVerificationEnabled);
  const [issuedInvitations, setIssuedInvitations] = useState<
    InvitationDelivery[]
  >([]);
  const { showSnackBar } = useContext(CustomSnackBarContext);
  const { users } = useSelector((state) => state.users);
  const dispatch = useDispatch();

  useEffect(() => {
    mounted.current = true;
    return () => {
      mounted.current = false;
      requestVersion.current++;
    };
  }, []);

  useEffect(() => {
    if (!open) {
      setIssuedInvitations([]);
      setEmails([]);
      setCurrentEmail(initialEmail ?? '');
      setRoleId(undefined);
      setSendEmail(isEmailVerificationEnabled);
      setIsInviteSubmitting(submitting.current);
    }
    return () => {
      requestVersion.current++;
    };
  }, [open, initialEmail]);

  const handleClose = () => {
    requestVersion.current++;
    setIssuedInvitations([]);
    onClose();
  };

  const onRoleChange = (id: number) => {
    setRoleId(id);
    setTimeout(() => {
      bottomRef.current?.scrollIntoView({ behavior: 'smooth' });
    }, 500);
  };
  const verifyCurrentEmail = (): boolean => {
    if (currentEmail) {
      let error;
      if (emails.length < 20) {
        const emailsClone = [...emails];
        if (emailsClone.includes(currentEmail)) {
          error = 'This email is already selected';
        } else {
          if (users.content.map((user) => user.email).includes(currentEmail)) {
            error = 'A user with this email is already in this company';
          } else {
            if (!currentEmail.match(emailRegExp)) {
              error = 'This email is invalid';
            }
          }
        }
      } else error = 'You can invite a maximum of 20 users at once';
      if (error) {
        showSnackBar(t(error), 'error');
        return false;
      }
    }
    return true;
  };

  return (
    <Dialog fullWidth maxWidth="sm" open={open} onClose={handleClose}>
      <DialogTitle
        sx={{
          p: 3
        }}
      >
        <Typography variant="h4" gutterBottom>
          {t('invite_users')}
        </Typography>
      </DialogTitle>

      <DialogContent
        dividers
        sx={{
          p: 3,
          display: 'flex',
          justifyContent: 'center'
        }}
      >
        <Box sx={{ width: '95%' }}>
          {issuedInvitations.length > 0 ? (
            <>
              <InvitationLinks invitations={issuedInvitations} />
              <Button fullWidth sx={{ mt: 2 }} onClick={handleClose}>
                {t('close')}
              </Button>
            </>
          ) : (
            <>
              <Paper
                elevation={0}
                sx={{
                  mb: 2,
                  p: 2,
                  textAlign: 'center',
                  background: grey[100]
                }}
              >
                <Box
                  component="img"
                  sx={{
                    height: 50,
                    width: 50
                  }}
                  alt={
                    "<a href='https://www.flaticon.com/free-icons/team' title='team icons'>Team icons created by Freepik - Flaticon</a>"
                  }
                  src="/static/images/team.png"
                />
                <Typography variant="h5">{t('bring_people_team')}</Typography>
              </Paper>
              <Box pb={3}>
                <UserRoleCardList onChange={onRoleChange} />
              </Box>
              <>
                <Grid container spacing={1}>
                  {emails.map((email, index) => (
                    <Grid item key={index}>
                      <Chip
                        label={email}
                        onDelete={() => {
                          const emailsClone = [...emails];
                          emailsClone.splice(index, 1);
                          setEmails(emailsClone);
                        }}
                      />
                    </Grid>
                  ))}
                </Grid>
                <TextField
                  sx={{ my: 2 }}
                  fullWidth
                  helperText={t('add_20_users')}
                  label={t('enter_email')}
                  placeholder={t('example@email.com')}
                  name="email"
                  value={currentEmail}
                  onChange={(event) => {
                    setCurrentEmail(event.target.value);
                  }}
                  onKeyDown={(event) => {
                    if (['Enter', 'Tab'].includes(event.key)) {
                      event.preventDefault();
                      if (currentEmail && verifyCurrentEmail()) {
                        const emailsClone = [...emails];
                        emailsClone.push(currentEmail);
                        setEmails(emailsClone);
                        setCurrentEmail('');
                      }
                    }
                  }}
                  variant={'outlined'}
                  required
                  InputProps={{
                    startAdornment: (
                      <InputAdornment position="start">
                        <EmailOutlined />
                      </InputAdornment>
                    )
                  }}
                />
                {isEmailVerificationEnabled && (
                  <FormControlLabel
                    control={
                      <Checkbox
                        checked={sendEmail}
                        onChange={(event) => setSendEmail(event.target.checked)}
                      />
                    }
                    label={t('invitation_send_email')}
                  />
                )}
                <Button
                  fullWidth
                  sx={{ mb: 3 }}
                  onClick={async () => {
                    if (submitting.current) return;
                    if (!roleId) {
                      showSnackBar(t('please_select_role'), 'error');
                      return;
                    }
                    if (!emails.length && !currentEmail) {
                      showSnackBar(t('please_type_emails'), 'error');
                      return;
                    }
                    if (currentEmail && !verifyCurrentEmail()) return;
                    submitting.current = true;
                    setIsInviteSubmitting(true);
                    const version = ++requestVersion.current;
                    try {
                      const response: InvitationResponse = await dispatch(
                        inviteUsers(
                          roleId,
                          currentEmail ? [...emails, currentEmail] : emails,
                          !sendEmail
                        )
                      );
                      if (version !== requestVersion.current) return;
                      setIssuedInvitations(response.invitations);
                      setEmails([]);
                      setCurrentEmail('');
                      onRefreshUsers();
                      showSnackBar(t('users_invite_success'), 'success');
                    } catch (err) {
                      if (version !== requestVersion.current) return;
                      showSnackBar(
                        err.status === 403
                          ? t('management_access_denied')
                          : getErrorMessage(err),
                        'error'
                      );
                    } finally {
                      submitting.current = false;
                      if (mounted.current) setIsInviteSubmitting(false);
                    }
                  }}
                  variant="contained"
                  startIcon={
                    isInviteSubmitting ? <CircularProgress size="1rem" /> : null
                  }
                  disabled={isInviteSubmitting}
                >
                  {t('invite')}
                </Button>
              </>
            </>
          )}
          <Box ref={bottomRef} />
        </Box>
      </DialogContent>
    </Dialog>
  );
}
