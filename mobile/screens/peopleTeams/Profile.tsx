import { ScrollView, StyleSheet, TouchableOpacity, View } from 'react-native';
import * as React from 'react';
import { Fragment, useContext, useEffect, useState } from 'react';
import { RootStackScreenProps } from '../../types';
import {
  ActivityIndicator,
  Avatar,
  Button,
  Dialog,
  Divider,
  HelperText,
  Portal,
  Switch,
  Text,
  TextInput
} from 'react-native-paper';
import { useTranslation } from 'react-i18next';
import { Formik } from 'formik';
import useAuth from '../../hooks/useAuth';
import UserSettings from '../../models/userSettings';
import { getUserInitials } from '../../utils/displayers';
import * as Yup from 'yup';
import { CustomSnackBarContext } from '../../contexts/CustomSnackBarContext';
import * as ImagePicker from 'expo-image-picker';
import { CompanySettingsContext } from '../../contexts/CompanySettingsContext';
import { OwnUser } from '../../models/user';
import { formatImages } from '../../utils/overall';
import { useAppTheme } from '../../custom-theme';
import { IconWithLabel } from '../../components/IconWithLabel';
import { useThemeMode } from '../../theme';

export default function UserProfile({
  navigation,
  route
}: RootStackScreenProps<'UserProfile'>) {
  const {
    user,
    fetchUserSettings,
    patchUserSettings,
    userSettings,
    updatePassword,
    patchUser,
    deleteAccount,
    logout
  } = useAuth();
  const theme = useAppTheme();
  const { resolvedScheme } = useThemeMode();
  const isDarkMode = resolvedScheme === 'dark';
  const sectionSurfaceColor = isDarkMode ? '#111827' : theme.colors.surface;
  const sectionTextColor = isDarkMode ? '#e5e7eb' : theme.colors.onSurface;
  const sectionMutedColor = isDarkMode
    ? '#cbd5e1'
    : theme.colors.onSurfaceVariant;
  const { t } = useTranslation();
  const { showSnackBar } = useContext(CustomSnackBarContext);
  const [changingPicture, setChangingPicture] = useState<boolean>(false);
  const [openChangePassword, setOpenChangePassword] = useState<boolean>();
  const [openDeleteAccountDialog, setOpenDeleteAccountDialog] =
    useState<boolean>(false);
  const [deletingAccount, setDeletingAccount] = useState<boolean>(false);
  const { uploadFiles } = useContext(CompanySettingsContext);
  const switches: {
    value: boolean;
    title: string;
    accessor: keyof UserSettings;
  }[] = [
    {
      value: userSettings?.emailNotified,
      title: t('email_notifications'),
      accessor: 'emailNotified'
    },
    {
      value: userSettings?.emailUpdatesForWorkOrders,
      title: t('email_updates_wo'),
      accessor: 'emailUpdatesForWorkOrders'
    },
    {
      value: userSettings?.emailUpdatesForRequests,
      title: t('email_updates_requests'),
      accessor: 'emailUpdatesForRequests'
    },
    {
      value: userSettings?.emailUpdatesForPurchaseOrders,
      title: t('po_emails'),
      accessor: 'emailUpdatesForPurchaseOrders'
    }
  ];

  useEffect(() => {
    fetchUserSettings();
  }, []);
  const onPictureChange = async () => {
    const { status } = await ImagePicker.requestMediaLibraryPermissionsAsync();

    if (status === 'granted') {
      let result = await ImagePicker.launchImageLibraryAsync({
        mediaTypes: ImagePicker.MediaTypeOptions.Images,
        allowsMultipleSelection: false,
        quality: 1
      });

      if (!result.canceled) {
        setChangingPicture(true);
        uploadFiles([], formatImages(result), true)
          .then((files) =>
            patchUser({ image: { id: files[0].id } } as Partial<OwnUser>)
          )
          .finally(() => setChangingPicture(false));
      }
    }
  };

  const renderChangePassword = () => {
    return (
      <Portal theme={theme}>
        <Dialog
          visible={openChangePassword}
          onDismiss={() => setOpenChangePassword(false)}
          style={{ backgroundColor: sectionSurfaceColor, borderRadius: 5 }}
        >
          <Dialog.Title>{t('change_password')}</Dialog.Title>
          <Formik
            initialValues={{
              oldPassword: '',
              newPassword: '',
              confirmPassword: ''
            }}
            validationSchema={Yup.object().shape({
              oldPassword: Yup.string()
                .required(t('required_old_password'))
                .min(8, t('invalid_password')),
              newPassword: Yup.string()
                .required(t('required_new_password'))
                .min(8, t('invalid_password')),
              confirmPassword: Yup.string().oneOf(
                [Yup.ref('newPassword'), null],
                t('passwords_must_match')
              )
            })}
            onSubmit={async (
              _values,
              { resetForm, setErrors, setStatus, setSubmitting }
            ) => {
              setSubmitting(true);
              return updatePassword(_values)
                .then(() => {
                  setOpenChangePassword(false);
                  showSnackBar(t('password_change_success'), 'success');
                })
                .catch((err) => showSnackBar(t('wrong_password'), 'error'))
                .finally(() => setSubmitting(false));
            }}
          >
            {({
              errors,
              handleBlur,
              handleChange,
              handleSubmit,
              isSubmitting,
              touched,
              values
            }) => (
              <Fragment>
                <Dialog.Content>
                  <TextInput
                    error={Boolean(touched.oldPassword && errors.oldPassword)}
                    label={t('current_password')}
                    onBlur={handleBlur('oldPassword')}
                    onChangeText={handleChange('oldPassword')}
                    value={values.oldPassword}
                    secureTextEntry={true}
                    mode="outlined"
                  />
                  {Boolean(touched.oldPassword && errors.oldPassword) && (
                    <HelperText type="error">
                      {errors.oldPassword?.toString()}
                    </HelperText>
                  )}
                  <TextInput
                    error={Boolean(touched.newPassword && errors.newPassword)}
                    label={t('new_password')}
                    onBlur={handleBlur('newPassword')}
                    onChangeText={handleChange('newPassword')}
                    value={values.newPassword}
                    secureTextEntry={true}
                    mode="outlined"
                  />
                  {Boolean(touched.newPassword && errors.newPassword) && (
                    <HelperText type="error">
                      {errors.newPassword?.toString()}
                    </HelperText>
                  )}
                  <TextInput
                    error={Boolean(
                      touched.confirmPassword && errors.confirmPassword
                    )}
                    label={t('confirm_password')}
                    onBlur={handleBlur('confirmPassword')}
                    onChangeText={handleChange('confirmPassword')}
                    value={values.confirmPassword}
                    secureTextEntry={true}
                    mode="outlined"
                  />
                  {Boolean(
                    touched.confirmPassword && errors.confirmPassword
                  ) && (
                    <HelperText type="error">
                      {errors.confirmPassword?.toString()}
                    </HelperText>
                  )}
                </Dialog.Content>
                <Dialog.Actions>
                  <Button onPress={() => setOpenChangePassword(false)}>
                    {t('cancel')}
                  </Button>
                  <Button
                    loading={isSubmitting}
                    disabled={isSubmitting}
                    onPress={() => handleSubmit()}
                  >
                    {t('change_password')}
                  </Button>
                </Dialog.Actions>
              </Fragment>
            )}
          </Formik>
        </Dialog>
      </Portal>
    );
  };

  const handleCloseDeleteAccountDialog = () => {
    setOpenDeleteAccountDialog(false);
    setDeletingAccount(false);
  };

  const handleConfirmDeleteAccount = async () => {
    setDeletingAccount(true);

    try {
      await deleteAccount();
      setOpenDeleteAccountDialog(false);
      showSnackBar(t('account_deleted'), 'success');
      await logout();
      navigation.reset({
        index: 0,
        routes: [{ name: 'Root' }]
      });
    } catch (err) {
      showSnackBar(t('account_delete_error'), 'error');
    } finally {
      setDeletingAccount(false);
    }
  };

  const renderDeleteAccountDialog = () => {
    return (
      <Portal theme={theme}>
        <Dialog
          visible={openDeleteAccountDialog}
          onDismiss={handleCloseDeleteAccountDialog}
          style={{ backgroundColor: sectionSurfaceColor, borderRadius: 5 }}
        >
          <Dialog.Title>{t('delete_account')}</Dialog.Title>
          <Dialog.Content>
            <Text>{t('delete_account_confirmation')}</Text>
          </Dialog.Content>
          <Dialog.Actions>
            <Button
              accessibilityLabel={t('cancel')}
              onPress={handleCloseDeleteAccountDialog}
            >
              {t('cancel')}
            </Button>
            <Button
              mode="contained"
              buttonColor={theme.colors.error}
              textColor={theme.colors.onError}
              accessibilityLabel={t('confirm_delete_account')}
              loading={deletingAccount}
              disabled={deletingAccount}
              onPress={handleConfirmDeleteAccount}
            >
              {t('confirm_delete_account')}
            </Button>
          </Dialog.Actions>
        </Dialog>
      </Portal>
    );
  };
  return (
    <ScrollView
      style={{
        flex: 1,
        backgroundColor: theme.colors.background
      }}
    >
      {renderChangePassword()}
      {renderDeleteAccountDialog()}
      <View
        style={{
          alignItems: 'center',
          paddingVertical: 30,
          backgroundColor: sectionSurfaceColor
        }}
      >
        {changingPicture ? (
          <ActivityIndicator size="large" />
        ) : (
          <TouchableOpacity onPress={onPictureChange}>
            {user.image ? (
              <Avatar.Image size={100} source={{ uri: user.image.url }} />
            ) : (
              <Avatar.Text
                size={100}
                label={getUserInitials(user)}
                style={{ backgroundColor: theme.colors.background }}
              />
            )}
            <View
              style={{
                position: 'absolute',
                bottom: 0,
                right: 0,
                backgroundColor: theme.colors.primary,
                borderRadius: 20,
                padding: 4,
                borderWidth: 2,
                borderColor: sectionSurfaceColor
              }}
            >
              <Avatar.Icon
                size={20}
                icon="camera"
                color="white"
                style={{ backgroundColor: 'transparent' }}
              />
            </View>
          </TouchableOpacity>
        )}
        <Text
          variant="headlineSmall"
          style={{ marginTop: 15, fontWeight: 'bold', color: sectionTextColor }}
        >
          {`${user.firstName} ${user.lastName}`}
        </Text>
        <Text variant="bodyLarge" style={{ color: sectionMutedColor }}>
          {user.jobTitle}
        </Text>
      </View>

      <View style={styles.section}>
        <Text
          variant="titleMedium"
          style={[styles.sectionTitle, { color: sectionMutedColor }]}
        >
          {t('informations')}
        </Text>
        <View
          style={[styles.sectionContent, { backgroundColor: sectionSurfaceColor }]}
        >
          {user.email && (
            <IconWithLabel
              label={user.email}
              icon="email-outline"
              color={sectionMutedColor}
            />
          )}
          {user.phone && (
            <IconWithLabel
              label={user.phone}
              icon="phone-outline"
              color={sectionMutedColor}
            />
          )}
          {user.role && (
            <IconWithLabel
              label={user.role.name}
              icon="shield-account-outline"
              color={sectionMutedColor}
            />
          )}
          {user.rate > 0 && (
            <IconWithLabel
              label={`${user.rate} / ${t('hour')}`}
              icon="currency-usd"
              color={sectionMutedColor}
            />
          )}
        </View>
      </View>

      {user?.role.code !== 'REQUESTER' && (
        <View style={styles.section}>
          <Text
            variant="titleMedium"
            style={[styles.sectionTitle, { color: sectionMutedColor }]}
          >
            {t('notifications')}
          </Text>
          <View
            style={[styles.sectionContent, { backgroundColor: sectionSurfaceColor }]}
          >
            {switches.map(({ title, value, accessor }, index) => (
              <Fragment key={accessor}>
                <View style={styles.switchRow}>
                  <Text style={{ flexShrink: 1, fontSize: 16, color: sectionTextColor }}>
                    {title}
                  </Text>
                  <Switch
                    value={Boolean(
                      userSettings ? userSettings[accessor] : false
                    )}
                    onValueChange={(checked) => {
                      patchUserSettings({
                        ...userSettings,
                        [accessor]: checked
                      });
                    }}
                  />
                </View>
              </Fragment>
            ))}
          </View>
        </View>
      )}

      <View style={{ padding: 20 }}>
        <Button
          style={{ marginBottom: 12 }}
          mode={'outlined'}
          onPress={() => setOpenChangePassword(true)}
        >
          {t('change_password')}
        </Button>
        <Button
          mode="contained"
          buttonColor={theme.colors.error}
          textColor={theme.colors.onError}
          onPress={() => setOpenDeleteAccountDialog(true)}
        >
          {t('delete_account')}
        </Button>
      </View>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  section: {},
  sectionTitle: {
    marginHorizontal: 20,
    marginVertical: 8,
    textTransform: 'uppercase',
    fontSize: 12,
    fontWeight: 'bold'
  },
  sectionContent: {
    padding: 20,
    gap: 10
  },
  infoRow: {},
  switchRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center'
  }
});
