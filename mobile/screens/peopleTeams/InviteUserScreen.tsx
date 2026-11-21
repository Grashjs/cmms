import { RefreshControl, ScrollView, Share, StyleSheet } from 'react-native';
import * as Clipboard from 'expo-clipboard';
import { useFocusEffect } from '@react-navigation/native';
import * as Clarity from '@microsoft/react-native-clarity';
import Constants from 'expo-constants';
import { getApiUrl } from '../../config';
import { View } from '../../components/Themed';
import {
  Button,
  Checkbox,
  HelperText,
  List,
  RadioButton,
  Text,
  TextInput,
  useTheme
} from 'react-native-paper';
import { useTranslation } from 'react-i18next';
import * as React from 'react';
import { useCallback, useContext, useEffect, useRef, useState } from 'react';
import { emailRegExp } from '../../utils/validators';
import { useDispatch, useSelector } from '../../store';
import { getRoles } from '../../slices/role';
import { inviteUsers } from '../../slices/user';
import { CustomSnackBarContext } from '../../contexts/CustomSnackBarContext';
import { RootStackScreenProps } from '../../types';
import {
  InvitationDelivery,
  InvitationResponse
} from '../../models/invitation';

const defaultRoleCodes = [
  'ADMIN',
  'LIMITED_ADMIN',
  'TECHNICIAN',
  'LIMITED_TECHNICIAN',
  'VIEW_ONLY',
  'REQUESTER'
];

export default function InviteUserScreen({
  navigation
}: RootStackScreenProps<'AddUser'>) {
  const theme = useTheme();
  const { t } = useTranslation();
  const dispatch = useDispatch();
  const { roles, loadingGet } = useSelector((state) => state.roles);
  const { showSnackBar } = useContext(CustomSnackBarContext);
  const [email, setEmail] = useState('');
  const [selectedRole, setSelectedRole] = useState<number>();
  const [hasSubmitted, setHasSubmitted] = useState(false);
  const [isInviteSubmitting, setIsInviteSubmitting] = useState(false);
  const [sendEmail, setSendEmail] = useState(true);
  const [invitations, setInvitations] = useState<InvitationDelivery[]>([]);
  const [transferring, setTransferring] = useState(false);
  const inFlight = useRef(false);
  const transferInFlight = useRef(false);
  const generation = useRef(0);
  const privacyReady = useRef<Promise<boolean>>(Promise.resolve(true));
  const clearSecrets = useCallback(() => {
    generation.current++;
    inFlight.current = false;
    transferInFlight.current = false;
    setInvitations([]);
    setIsInviteSubmitting(false);
    setTransferring(false);
    setEmail('');
    setSelectedRole(undefined);
    setHasSubmitted(false);
  }, []);

  useFocusEffect(
    useCallback(() => {
      let focused = true;
      let pausedHere = false;
      // Bearer links must not appear in session recordings.
      // Serialize pause/resume across rapid blur/focus transitions.
      privacyReady.current = privacyReady.current
        .then(async () => {
          const apiUrl = await getApiUrl();
          const captureConfigured =
            apiUrl.toLowerCase().includes('api.atlas-cmms.com') &&
            Constants.expoConfig?.extra?.CLARITY_ID;
          if (!focused) return false;
          if (!captureConfigured) return true;
          if (!(await Clarity.isPaused())) {
            pausedHere = await Clarity.pause();
            return pausedHere;
          }
          return true;
        })
        .catch(() => false);
      return () => {
        focused = false;
        clearSecrets();
        privacyReady.current = privacyReady.current
          .then(async () => {
            if (pausedHere) await Clarity.resume();
            return true;
          })
          .catch(() => false);
      };
    }, [clearSecrets])
  );

  useEffect(() => {
    dispatch(getRoles({ operation: 'invite' })).catch(() =>
      showSnackBar(t('management_access_denied'), 'error')
    );
  }, [dispatch]);
  const orderedRoles = [
    ...defaultRoleCodes.map((code) => roles.find((role) => role.code === code)),
    ...roles.filter((role) => role.code === 'USER_CREATED')
  ].filter(Boolean);

  const onInvite = async () => {
    if (inFlight.current || invitations.length) return;
    setHasSubmitted(true);
    const normalizedEmail = email.trim().toLowerCase();
    if (!selectedRole || !roles.some((role) => role.id === selectedRole)) {
      showSnackBar(t('please_select_role'), 'error');
      return;
    }
    if (!emailRegExp.test(normalizedEmail)) {
      showSnackBar(t('invalid_email'), 'error');
      return;
    }
    inFlight.current = true;
    setIsInviteSubmitting(true);
    const currentGeneration = generation.current;
    try {
      if (!(await privacyReady.current)) throw new Error();
      if (currentGeneration !== generation.current) return;
      const response: InvitationResponse = await dispatch(
        inviteUsers(selectedRole, [normalizedEmail], !sendEmail)
      );
      if (currentGeneration !== generation.current) return;
      if (!response.success || !response.invitations?.length) throw new Error();
      setInvitations(response.invitations);
      showSnackBar(t('invitation_created'), 'success');
    } catch (error) {
      if (currentGeneration === generation.current)
        showSnackBar(
          t(
            error?.status === 403
              ? 'management_access_denied'
              : 'user_invite_failure'
          ),
          'error'
        );
    } finally {
      if (currentGeneration === generation.current) {
        inFlight.current = false;
        setIsInviteSubmitting(false);
      }
    }
  };

  const transmit = async (invitation: InvitationDelivery, copy: boolean) => {
    if (transferInFlight.current) return;
    transferInFlight.current = true;
    setTransferring(true);
    const currentGeneration = generation.current;
    try {
      if (copy) {
        if (!(await Clipboard.setStringAsync(invitation.invitationLink)))
          throw new Error();
        if (currentGeneration === generation.current)
          showSnackBar(t('invitation_copied'), 'success');
      } else {
        await Share.share({ message: invitation.invitationLink });
      }
    } catch {
      if (currentGeneration === generation.current)
        showSnackBar(
          t(copy ? 'invitation_copy_failed' : 'invitation_share_failed'),
          'error'
        );
    } finally {
      if (currentGeneration === generation.current) {
        transferInFlight.current = false;
        setTransferring(false);
      }
    }
  };

  const close = () => {
    clearSecrets();
    navigation.goBack();
  };
  return (
    <ScrollView
      style={{ ...styles.container, backgroundColor: theme.colors.background }}
      refreshControl={
        invitations.length ? undefined : (
          <RefreshControl
            refreshing={loadingGet}
            onRefresh={() =>
              !inFlight.current &&
              dispatch(getRoles({ operation: 'invite' })).catch(() =>
                showSnackBar(t('management_access_denied'), 'error')
              )
            }
            colors={[theme.colors.primary]}
          />
        )
      }
    >
      <View style={{ padding: 20 }}>
        {!loadingGet && !roles.length && !invitations.length && (
          <Text>{t('no_assignable_roles')}</Text>
        )}
        {invitations.length ? (
          <>
            <Text variant="titleLarge">{t('invitation_created')}</Text>
            <Text style={{ marginVertical: 12 }}>
              {t('invitation_delivery_notice')}
            </Text>
            {invitations.map((invitation) => (
              <View key={invitation.email} style={{ marginBottom: 16 }}>
                <Text>{invitation.email}</Text>
                <Text selectable testID="invitation-link">
                  {invitation.invitationLink}
                </Text>
                <Text>
                  {t('invitation_expires', {
                    date: new Date(invitation.expiresAt).toLocaleString()
                  })}
                </Text>
                <Button
                  testID="copy-invitation"
                  icon="content-copy"
                  disabled={transferring}
                  onPress={() => transmit(invitation, true)}
                >
                  {t('invitation_copy')}
                </Button>
                <Button
                  testID="share-invitation"
                  icon="share-variant"
                  disabled={transferring}
                  onPress={() => transmit(invitation, false)}
                >
                  {t('invitation_share')}
                </Button>
              </View>
            ))}
            <Text>{t('invitation_browser_acceptance')}</Text>
            <Button
              testID="close-invitation"
              onPress={close}
              style={{ marginTop: 20 }}
            >
              {t('close')}
            </Button>
          </>
        ) : (
          <>
            <TextInput
              testID="invitation-email"
              disabled={isInviteSubmitting}
              error={hasSubmitted && !emailRegExp.test(email.trim())}
              label={t('email')}
              onChangeText={setEmail}
              value={email}
              mode="outlined"
              autoCapitalize="none"
              keyboardType="email-address"
            />
            {!emailRegExp.test(email.trim()) && hasSubmitted && (
              <HelperText type="error">{t('invalid_email')}</HelperText>
            )}
            {orderedRoles.map((role) => (
              <List.Item
                key={role.id}
                testID={`invitation-role-${role.id}`}
                disabled={isInviteSubmitting}
                title={
                  role.code === 'USER_CREATED'
                    ? role.name
                    : t(`${role.code}_name`)
                }
                description={
                  role.code === 'USER_CREATED'
                    ? role.description
                    : t(`${role.code}_description`)
                }
                descriptionNumberOfLines={10}
                onPress={() => setSelectedRole(role.id)}
                left={() => (
                  <RadioButton
                    disabled={isInviteSubmitting}
                    value={String(role.id)}
                    status={selectedRole === role.id ? 'checked' : 'unchecked'}
                    onPress={() => setSelectedRole(role.id)}
                  />
                )}
              />
            ))}
            <Checkbox.Item
              testID="invitation-send-email"
              label={t('invitation_send_email')}
              status={sendEmail ? 'checked' : 'unchecked'}
              disabled={isInviteSubmitting}
              onPress={() => setSendEmail(!sendEmail)}
            />
            <HelperText type="info">
              {t('invitation_email_optional')}
            </HelperText>
            <Button
              testID="create-invitation"
              disabled={isInviteSubmitting}
              loading={isInviteSubmitting}
              onPress={onInvite}
              style={{ marginTop: 20 }}
              mode="contained"
            >
              {t('invite')}
            </Button>
          </>
        )}
      </View>
    </ScrollView>
  );
}

const styles = StyleSheet.create({ container: { flex: 1 } });
