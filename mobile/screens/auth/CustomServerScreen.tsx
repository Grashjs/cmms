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
import { AsyncStorage } from 'react-native';
import { useAppTheme } from '../../custom-theme';

export default function CustomServerScreen({
  navigation
}: AuthStackScreenProps<'CustomServer'>) {
  const { t } = useTranslation();
  const { showSnackBar } = useContext(CustomSnackBarContext);
  const theme = useAppTheme();
  const [currentUrl, setCurrentUrl] = useState<string>('');

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
            serverUrl: Yup.string()
              .url(t('invalid_url'))
              .required(t('required_url'))
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
              await AsyncStorage.setItem('customApiUrl', url);
              showSnackBar(t('server_url_saved'), 'success');
              
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
            values,
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
                placeholder="https://your-server-url.com"
              />
              {Boolean(touched.serverUrl && errors.serverUrl) && (
                <HelperText type="error">{errors.serverUrl?.toString()}</HelperText>
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
                    await AsyncStorage.removeItem('customApiUrl');
                    setCurrentUrl('');
                    showSnackBar(t('server_url_reset'), 'success');
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