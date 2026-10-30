import { ScrollView, StyleSheet } from 'react-native';
import * as Yup from 'yup';
import { View } from '../../components/Themed';
import { AuthStackScreenProps } from '../../types';
import { Formik } from 'formik';
import { useTranslation } from 'react-i18next';
import { useContext, useEffect, useState } from 'react';
import {
  Button,
  HelperText,
  Text,
  TextInput,
  useTheme
} from 'react-native-paper';
import { CustomSnackBarContext } from '../../contexts/CustomSnackBarContext';
import * as React from 'react';
import AsyncStorage from '@react-native-async-storage/async-storage';
import { useAppTheme } from '../../custom-theme';
import { useDispatch } from '../../store';
import { getInstanceConfig } from '../../slices/instanceConfig';
import {
  beforeSessionChange,
  finishSessionChange
} from '../../offline/sessionBridge';
import {
  canonicalizeServerUrl,
  getOnlineIdentity,
  revokeLocalAccess
} from '../../offline/accessPolicy';
import { getCredentials, setCredentials } from '../../utils/credentialStore';
import { getApiUrl } from '../../config';
import { Platform } from 'react-native';
import { revertAll } from '../../utils/redux';

export default function CustomServerScreen({
  navigation
}: AuthStackScreenProps<'CustomServer'>) {
  const { t } = useTranslation();
  const { showSnackBar } = useContext(CustomSnackBarContext);
  const theme = useAppTheme();
  const [currentUrl, setCurrentUrl] = useState<string>('');
  const dispatch = useDispatch();

  const changeServer = async (url: string | null) => {
    const canonical = url ? canonicalizeServerUrl(url) : null;
    await beforeSessionChange('server');
    try {
      const previousUrl = await getApiUrl();
      // Finish any legacy migration against the OLD server, before changing the URL.
      await getCredentials(previousUrl);
      const previous =
        Platform.OS === 'web' ? null : await getOnlineIdentity(previousUrl);
      if (previous) await revokeLocalAccess(previous.context);
      await setCredentials(null, previousUrl);
      if (canonical) await AsyncStorage.setItem('customApiUrl', canonical);
      else await AsyncStorage.removeItem('customApiUrl');
      dispatch(revertAll());
    } finally {
      finishSessionChange();
    }
  };

  useEffect(() => {
    // Load the current custom URL if it exists
    const loadCustomUrl = async () => {
      const savedUrl = await AsyncStorage.getItem('customApiUrl');
      if (savedUrl) {
        setCurrentUrl(savedUrl);
      }
    };

    loadCustomUrl();
  }, []);

  return (
    <View style={styles.container}>
      <ScrollView
        style={styles.scrollView}
        contentContainerStyle={{ alignItems: 'center' }}
      >
        <Text style={styles.title}>{t('custom_server_title')}</Text>
        <Text style={styles.description}>{t('custom_server_description')}</Text>

        <Formik
          initialValues={{
            serverUrl: currentUrl,
            submit: null
          }}
          enableReinitialize
          validationSchema={Yup.object().shape({
            serverUrl: Yup.string().required(t('required_url'))
          })}
          onSubmit={async (
            values,
            { setErrors, setStatus, setSubmitting }
          ): Promise<void> => {
            setSubmitting(true);
            try {
              // Ensure URL ends with a slash
              let url = values.serverUrl;
              if (!url.endsWith('/')) {
                url = url + '/';
              }

              // Save the custom URL to AsyncStorage
              await changeServer(url);
              showSnackBar(t('server_url_saved'), 'success');

              dispatch(getInstanceConfig());

              // Navigate back to login
              navigation.goBack();
            } catch (error) {
              showSnackBar(t('error_saving_url'), 'error');
              setStatus({ success: false });
            } finally {
              setSubmitting(false);
            }
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
            <View style={{ alignSelf: 'stretch', paddingHorizontal: 30 }}>
              <TextInput
                error={Boolean(touched.serverUrl && errors.serverUrl)}
                label={t('server_url')}
                onBlur={handleBlur('serverUrl')}
                onChangeText={handleChange('serverUrl')}
                value={values.serverUrl}
                mode="outlined"
                style={{ marginBottom: 10 }}
                autoCapitalize="none"
                placeholder="https://your-server-url.com"
              />
              {Boolean(touched.serverUrl && errors.serverUrl) && (
                <HelperText type="error">
                  {errors.serverUrl?.toString()}
                </HelperText>
              )}

              <Button
                color={theme.colors.primary}
                onPress={() => handleSubmit()}
                loading={isSubmitting}
                style={{ marginTop: 20 }}
                disabled={isSubmitting}
                mode="contained"
              >
                {t('save')}
              </Button>

              <Button
                mode="text"
                onPress={() => navigation.goBack()}
                style={{ marginTop: 10 }}
              >
                {t('cancel')}
              </Button>

              {currentUrl && (
                <Button
                  mode="outlined"
                  onPress={async () => {
                    try {
                      await changeServer(null);
                      setCurrentUrl('');
                      dispatch(getInstanceConfig());
                      showSnackBar(t('server_url_reset'), 'success');
                    } catch {
                      showSnackBar(t('error_saving_url'), 'error');
                    }
                  }}
                  style={{ marginTop: 20 }}
                  color={theme.colors.error}
                >
                  {t('reset_to_default')}
                </Button>
              )}
            </View>
          )}
        </Formik>
      </ScrollView>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center'
  },
  title: {
    fontSize: 20,
    fontWeight: 'bold',
    marginBottom: 20
  },
  description: {
    textAlign: 'center',
    marginBottom: 20,
    paddingHorizontal: 30
  },
  scrollView: {
    marginVertical: 20,
    paddingHorizontal: 10,
    width: '100%'
  }
});
