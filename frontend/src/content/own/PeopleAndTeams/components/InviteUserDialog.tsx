import {
  Box,
  Button,
  Chip,
  CircularProgress,
  Dialog,
  DialogContent,
  DialogTitle,
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
import { useContext, useRef, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { emailRegExp } from '../../../../utils/validators';
import { CustomSnackBarContext } from '../../../../contexts/CustomSnackBarContext';
import { useDispatch, useSelector } from '../../../../store';
import { isEmailVerificationEnabled } from '../../../../config';
import CreateUser from './CreateUser';
import { Simulate } from 'react-dom/test-utils';

export default function InviteUserDialog({
  open,
  onClose,
  onRefreshUsers
}: {
  open: boolean;
  onClose: () => void;
  onRefreshUsers: () => void;
}) {
  const [isInviteSubmitting, setIsInviteSubmitting] = useState(false);
  const [roleId, setRoleId] = useState<number>();
  const { t } = useTranslation();
  const [emails, setEmails] = useState<string[]>([]);
  const bottomRef = useRef<HTMLDivElement>(null);
  const [currentEmail, setCurrentEmail] = useState<string>('');
  const { showSnackBar } = useContext(CustomSnackBarContext);
  const { users, loadingGet, singleUser } = useSelector((state) => state.users);
  const dispatch = useDispatch();

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
        setIsInviteSubmitting(false);
        return false;
      }
    }
    return true;
  };

  return (
    <Dialog fullWidth maxWidth="sm" open={open} onClose={onClose}>
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
          {isEmailVerificationEnabled ? (
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
                    if (verifyCurrentEmail()) {
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
              <Button
                fullWidth
                sx={{ mb: 3 }}
                onClick={async () => {
                  setIsInviteSubmitting(true);
                  const invite = (emails: string[]) =>
                    dispatch(inviteUsers(roleId, emails))
                      .then(() => {
                        onClose();
                        setEmails([]);
                        setCurrentEmail('');
                        showSnackBar(t('users_invite_success'), 'success');
                      })
                      .catch((err: { message: string }) => {
                        showSnackBar(JSON.parse(err.message).message, 'error');
                      })
                      .finally(() => setIsInviteSubmitting(false));
                  if (roleId) {
                    if (emails.length || currentEmail) {
                      if (currentEmail) {
                        if (verifyCurrentEmail())
                          invite([...emails, currentEmail]);
                      } else {
                        invite(emails);
                      }
                    } else {
                      showSnackBar(t('please_type_emails'), 'error');
                      setIsInviteSubmitting(false);
                    }
                  } else {
                    showSnackBar(t('please_select_role'), 'error');
                    setIsInviteSubmitting(false);
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
          ) : (
            roleId && (
              <CreateUser
                roleId={roleId}
                onClose={onClose}
                onRefreshUsers={onRefreshUsers}
              />
            )
          )}
          <Box ref={bottomRef} />
        </Box>
      </DialogContent>
    </Dialog>
  );
}
