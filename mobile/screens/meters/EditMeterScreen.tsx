import { RootStackScreenProps } from '../../types';
import { View } from '../../components/Themed';
import Form from '../../components/form';
import * as Yup from 'yup';
import { StyleSheet } from 'react-native';
import { useTranslation } from 'react-i18next';
import { useContext } from 'react';
import { CompanySettingsContext } from '../../contexts/CompanySettingsContext';
import { useDispatch } from '../../store';
import { editMeter } from '../../slices/meter';
import { CustomSnackBarContext } from '../../contexts/CustomSnackBarContext';
import { formatMeterValues, getMeterFields } from '../../utils/fields';
import useAuth from '../../hooks/useAuth';

export default function EditMeterScreen({
  navigation,
  route
}: RootStackScreenProps<'EditMeter'>) {
  const { t } = useTranslation();
  const { meter } = route.params;
  const { getFilteredFields } = useAuth();
  const { uploadFiles, getWOFieldsAndShapes } = useContext(
    CompanySettingsContext
  );
  const { showSnackBar } = useContext(CustomSnackBarContext);
  const dispatch = useDispatch();
  const shape = {
    name: Yup.string().required(t('required_meter_name')),
    unit: Yup.string().required(t('required_meter_unit')),
    updateFrequency: Yup.number().required(
      t('required_meter_update_frequency')
    ),
    asset: Yup.object().required(t('required_asset')).nullable()
  };
  const onEditSuccess = () => {
    showSnackBar(t('changes_saved_success'), 'success');
    navigation.goBack();
  };
  const onEditFailure = (err) => showSnackBar(t('meter_edit_failure'), 'error');

  return (
    <View style={styles.container}>
      <Form
        fields={getFilteredFields(getMeterFields(t))}
        validation={Yup.object().shape(shape)}
        navigation={navigation}
        submitText={t('save')}
        values={{
          ...meter,
          users: meter?.users.map((worker) => {
            return {
              label: `${worker?.firstName} ${worker.lastName}`,
              value: worker.id
            };
          }),
          location: meter?.location
            ? {
                label: meter?.location.name,
                value: meter?.location.id
              }
            : null,
          asset: {
            label: meter?.asset.name,
            value: meter?.asset.id
          }
        }}
        onChange={({ field, e }) => {}}
        onSubmit={async (values) => {
          let formattedValues = formatMeterValues(values);
          return new Promise<void>((resolve, rej) => {
            uploadFiles([], values.image)
              .then((files) => {
                formattedValues = {
                  ...formattedValues,
                  image: files.length ? { id: files[0].id } : meter.image
                };
                dispatch(editMeter(meter.id, formattedValues))
                  .then(onEditSuccess)
                  .catch(onEditFailure)
                  .finally(resolve);
              })
              .catch((err) => {
                rej(err);
                onEditFailure(err);
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
