import { RootStackScreenProps } from '../../types';
import { View } from '../../components/Themed';
import Form from '../../components/form';
import * as Yup from 'yup';
import { StyleSheet } from 'react-native';
import { useTranslation } from 'react-i18next';
import { useContext } from 'react';
import { CompanySettingsContext } from '../../contexts/CompanySettingsContext';
import { useDispatch } from '../../store';
import { CustomSnackBarContext } from '../../contexts/CustomSnackBarContext';
import { formatMeterValues, getMeterFields } from '../../utils/fields';
import useAuth from '../../hooks/useAuth';
import { addMeter } from '../../slices/meter';

export default function CreateMeterScreen({
  navigation,
  route
}: RootStackScreenProps<'AddMeter'>) {
  const { t } = useTranslation();
  const { uploadFiles } = useContext(CompanySettingsContext);
  const { getFilteredFields } = useAuth();
  const { showSnackBar } = useContext(CustomSnackBarContext);
  const dispatch = useDispatch();
  const onCreationSuccess = () => {
    showSnackBar(t('meter_create_success'), 'success');
    navigation.goBack();
  };
  const onCreationFailure = (err) =>
    showSnackBar(t('meter_create_failure'), 'error');

  const shape = {
    name: Yup.string().required(t('required_meter_name')),
    unit: Yup.string().required(t('required_meter_unit')),
    updateFrequency: Yup.number().required(
      t('required_meter_update_frequency')
    ),
    asset: Yup.object().required(t('required_asset')).nullable()
  };

  return (
    <View style={styles.container}>
      <Form
        fields={getFilteredFields(getMeterFields(t))}
        validation={Yup.object().shape(shape)}
        navigation={navigation}
        submitText={t('add_meter')}
        values={{}}
        onChange={({ field, e }) => {}}
        onSubmit={async (values) => {
          let formattedValues = formatMeterValues(values);
          return new Promise<void>((resolve, rej) => {
            uploadFiles([], values.image)
              .then((files) => {
                formattedValues = {
                  ...formattedValues,
                  image: files.length ? { id: files[0].id } : null
                };
                dispatch(addMeter(formattedValues))
                  .then(onCreationSuccess)
                  .catch(onCreationFailure)
                  .finally(resolve);
              })
              .catch((err) => {
                rej(err);
              });
          });
        }}
      />
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1
  }
});
