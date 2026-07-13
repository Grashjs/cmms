import { ScrollView, TouchableOpacity } from 'react-native';
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
  TextInput,
  useTheme
} from 'react-native-paper';
import { useTranslation } from 'react-i18next';
import { View } from '../../components/Themed';
import { Formik } from 'formik';
import useAuth from '../../hooks/useAuth';
import UserSettings from '../../models/userSettings';
import { getUserInitials } from '../../utils/displayers';
import * as Yup from 'yup';
import { CustomSnackBarContext } from '../../contexts/CustomSnackBarContext';
import * as Permissions from 'expo-permissions';
import * as ImagePicker from 'expo-image-picker';
import mime from 'mime';
import { CompanySettingsContext } from '../../contexts/CompanySettingsContext';
import { OwnUser } from '../../models/user';
import { formatImages } from '../../utils/overall';

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
    patchUser
  } = useAuth();
  const theme = useTheme();
  const { t } = useTranslation();
  const { showSnackBar } = useContext(CustomSnackBarContext);
  const [changingPicture, setChangingPicture] = useState<boolean>(false);
  const [openChangePassword, setOpenChangePassword] = useState<boolean>();
  const { uploadFiles } = useContext(CompanySettingsContext);
  const fieldsToRender = [
    {
      label: t('id'),
      value: user?.id
    },
    {
      label: t('first_name'),
      value: user?.firstName
    },
    {
      label: t('last_name'),
      value: user?.lastName
    },
    {
      label: t('email'),
      value: user?.email
    },
    {
      label: t('phone'),
      value: user?.phone
    },
    {
      label: t('job_title'),
      value: user?.jobTitle
    },
    ...(user?.role.code === 'REQUESTER'
      ? []
      : [
          {
            label: t('role'),
            value: user?.role.name
          },
          {
            label: t('hourly_rate'),
            value: user?.rate
          }
        ])
  ];
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
    // No permissions request is necessary for launching the image library
    const { status } = await Permissions.askAsync(Permissions.MEDIA_LIBRARY);
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

  function BasicField({
    label,
    value
  }: {
    label: string;
    value: string | number;
  }) {
    if (value)
      return (
        <View
          key={label}
          style={{ paddingHorizontal: 20, paddingVertical: 10 }}
        >
          <Text variant="titleMedium" style={{ fontWeight: 'bold' }}>
            {label}
          </Text>
          <Text variant="bodyLarge">{value}</Text>
          <Divider style={{ marginTop: 5 }} />
        </View>
      );
    else return null;
  }

  const renderChangePassword = () => {
    return (
      <Portal theme={theme}>
        <Dialog
          visible={openChangePassword}
          onDismiss={() => setOpenChangePassword(false)}
          style={{ backgroundColor: 'white', borderRadius: 5 }}
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
  return (
    <ScrollView
      style={{
        flex: 1,
        backgroundColor: theme.colors.background
      }}
    >
      {renderChangePassword()}
      <View style={{ alignItems: 'center', paddingTop: 20 }}>
        {changingPicture ? (
          <ActivityIndicator size="large" />
        ) : (
          <TouchableOpacity onPress={onPictureChange}>
            {user.image ? (
              <Avatar.Image source={{ uri: user.image.url }} />
            ) : (
              <Avatar.Text size={50} label={getUserInitials(user)} />
            )}
          </TouchableOpacity>
        )}
      </View>
      {fieldsToRender.map((field) => (
        <BasicField key={field.label} label={field.label} value={field.value} />
      ))}
      {user?.role.code !== 'REQUESTER' &&
        switches.map(({ title, value, accessor }) => (
          <View style={{ paddingHorizontal: 20, paddingVertical: 10 }}>
            <View
              style={{
                display: 'flex',
                flexDirection: 'row',
                justifyContent: 'space-between',
                alignItems: 'center'
              }}
            >
              <Text style={{ flexShrink: 1 }}>{title}</Text>
              <Switch
                value={Boolean(userSettings ? userSettings[accessor] : false)}
                onValueChange={(checked) => {
                  patchUserSettings({
                    ...userSettings,
                    [accessor]: checked
                  });
                }}
              />
            </View>
            <Divider />
          </View>
        ))}
      <View>
        <Button
          style={{ marginHorizontal: 20, marginBottom: 20 }}
          mode={'outlined'}
          onPress={() => setOpenChangePassword(true)}
        >
          {t('change_password')}
        </Button>
      </View>
    </ScrollView>
  );
}
