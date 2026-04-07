import {
  Linking,
  ScrollView,
  StyleSheet,
  TouchableOpacity
} from 'react-native';
import * as Yup from 'yup';
import { View } from '../../components/Themed';
import { AuthStackScreenProps } from '../../types';
import { Formik } from 'formik';
import { useTranslation } from 'react-i18next';
import { phoneRegExp } from '../../utils/validators';
import useAuth from '../../hooks/useAuth';
import { IS_LOCALHOST } from '../../config';
import { useContext, useState } from 'react';
import {
  Button,
  Checkbox,
  HelperText,
  Text,
  TextInput,
  useTheme
} from 'react-native-paper';
import { CustomSnackBarContext } from '../../contexts/CustomSnackBarContext';
import * as React from 'react';

export default function RegisterScreen({
  navigation
}: AuthStackScreenProps<'Register'>) {
  const { t } = useTranslation();
  const { register } = useAuth();
  const { showSnackBar } = useContext(CustomSnackBarContext);
  const [showPassword, setShowPassword] = useState<boolean>(false);
  const theme = useTheme();
  const toggleShowPassword = () => setShowPassword((value) => !value);
  const getFieldsAndShapes = (): [
    { [key: string]: any },
    { [key: string]: any }
  ] => {
    let fields = {
      email: '',
      firstName: '',
      lastName: '',
      phone: '',
      password: '',
      companyName: '',
      submit: null
    };
    let shape = {
      email: Yup.string()
        .email(t('invalid_email'))
        .max(255)
        .required(t('required_email')),
      firstName: Yup.string().max(255).required(t('required_firstName')),
      lastName: Yup.string().max(255).required(t('required_lastName')),
      companyName: Yup.string().max(255).required(t('required_company')),
      phone: Yup.string().matches(phoneRegExp, t('invalid_phone')),
      password: Yup.string().min(8).max(255).required(t('required_password'))
    };
    // if (role) {
    //   const keysToDelete = ['companyName', 'employeesCount'];
    //   keysToDelete.forEach((key) => {
    //     delete fields[key];
    //     delete shape[key];
    //   });
    // }
    return [fields, shape];
  };

  const termsOfServiceUrl = 'https://atlas-cmms.com/terms-of-service';
  return (
    <View style={styles.container}>
      <ScrollView style={styles.scrollView}>
        <Formik
          initialValues={getFieldsAndShapes()[0]}
          validationSchema={Yup.object().shape(getFieldsAndShapes()[1])}
          onSubmit={async (values, { setErrors, setStatus, setSubmitting }) => {
            setSubmitting(true);
            return register(values)
              .then(() => {
                if (!IS_LOCALHOST) {
                  showSnackBar(t('verify_email'), 'success');
                  navigation.navigate('Verify');
                }
              })
              .catch((err) => {
                showSnackBar(t('registration_error'), 'error');
              })
              .finally(() => {
                setStatus({ success: true });
                setSubmitting(false);
              });
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
            setFieldValue
          }) => (
            <View>
              <Text
                style={[
                  styles.requiredNotice,
                  {
                    color: theme.colors.onSurfaceVariant
                  }
                ]}
              >
                {t('required_fields_notice')}
              </Text>
              <TextInput
                error={Boolean(touched.firstName && errors.firstName)}
                label={`${t('first_name')} *`}
                onBlur={handleBlur('firstName')}
                onChangeText={handleChange('firstName')}
                value={values.firstName}
                mode="outlined"
              />
              <HelperText
                type="error"
                visible={Boolean(touched.firstName && errors.firstName)}
              >
                {errors.firstName?.toString()}
              </HelperText>
              <TextInput
                error={Boolean(touched.lastName && errors.lastName)}
                label={`${t('last_name')} *`}
                onBlur={handleBlur('lastName')}
                onChangeText={handleChange('lastName')}
                value={values.lastName}
                mode="outlined"
              />
              <HelperText
                type="error"
                visible={Boolean(touched.lastName && errors.lastName)}
              >
                {errors.lastName?.toString()}
              </HelperText>
              <TextInput
                error={Boolean(touched.email && errors.email)}
                label={`${t('email')} *`}
                onBlur={handleBlur('email')}
                onChangeText={handleChange('email')}
                value={values.email}
                autoCapitalize="none"
                mode="outlined"
              />
              <HelperText
                type="error"
                visible={Boolean(touched.email && errors.email)}
              >
                {errors.email?.toString()}
              </HelperText>
              <TextInput
                error={Boolean(touched.phone && errors.phone)}
                label={t('phone')}
                onBlur={handleBlur('phone')}
                onChangeText={handleChange('phone')}
                value={values.phone}
                mode="outlined"
              />
              <HelperText
                type="error"
                visible={Boolean(touched.phone && errors.phone)}
              >
                {errors.phone?.toString()}
              </HelperText>
              <TextInput
                error={Boolean(touched.password && errors.password)}
                label={`${t('password')} *`}
                onBlur={handleBlur('password')}
                onChangeText={handleChange('password')}
                autoCapitalize="none"
                value={values.password}
                secureTextEntry={!showPassword}
                right={
                  <TextInput.Icon onPress={toggleShowPassword} icon="eye" />
                }
                mode="outlined"
              />
              <HelperText
                type="error"
                visible={Boolean(touched.password && errors.password)}
              >
                {errors.password?.toString()}
              </HelperText>
              <TextInput
                error={Boolean(touched.companyName && errors.companyName)}
                label={`${t('companyName')} *`}
                onBlur={handleBlur('companyName')}
                onChangeText={handleChange('companyName')}
                value={values.companyName}
                mode="outlined"
              />
              <HelperText
                type="error"
                visible={Boolean(touched.companyName && errors.companyName)}
              >
                {errors.companyName?.toString()}
              </HelperText>
              <Button
                color={theme.colors.primary}
                onPress={() => handleSubmit()}
                loading={isSubmitting}
                disabled={isSubmitting}
                mode="contained"
              >
                {t('create_your_account')}
              </Button>
              <View style={styles.checkboxContainer}>
                <View style={styles.row}>
                  <Text>{`${t('i_accept').trim()}`}</Text>
                  <TouchableOpacity
                    onPress={() => {
                      Linking.canOpenURL(termsOfServiceUrl).then(
                        (supported) => {
                          if (supported) {
                            Linking.openURL(termsOfServiceUrl);
                          } else {
                            console.log(
                              "Don't know how to open URI: " + termsOfServiceUrl
                            );
                          }
                        }
                      );
                    }}
                  >
                    <Text style={{ color: theme.colors.primary }}>{` ${t(
                      'terms_conditions'
                    )}`}</Text>
                  </TouchableOpacity>
                </View>
              </View>
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
    fontWeight: 'bold'
  },
  separator: {
    marginVertical: 30,
    height: 1,
    width: '80%'
  },
  scrollView: {
    marginVertical: 20,
    paddingHorizontal: 40,
    width: '100%'
  },
  checkboxContainer: {
    display: 'flex',
    flexDirection: 'row',
    justifyContent: 'flex-start',
    alignItems: 'center',
    marginTop: 10
  },
  checkboxWrapper: {
    borderRadius: 4,
    marginRight: 8,
    justifyContent: 'center',
    alignItems: 'center'
  },
  row: {
    display: 'flex',
    flexDirection: 'row',
    alignItems: 'center'
  },
  requiredNotice: {
    marginBottom: 16
  }
});
