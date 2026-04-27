import { RootStackScreenProps } from '../../types';
import { View } from '../../components/Themed';
import Form from '../../components/form';
import * as Yup from 'yup';
import { StyleSheet } from 'react-native';
import { useTranslation } from 'react-i18next';
import { useContext } from 'react';
import { CompanySettingsContext } from '../../contexts/CompanySettingsContext';
import { useDispatch, useSelector } from '../../store';
import { editMeter } from '../../slices/meter';
import { CustomSnackBarContext } from '../../contexts/CustomSnackBarContext';
import { formatMeterValues, getMeterFields } from '../../utils/fields';
import { formatCustomFields } from '../../utils/formatters';
import useAuth from '../../hooks/useAuth';
import { getImageAndFiles } from '../../utils/overall';
import {
  IField,
  getCustomFieldsIFields,
  getCustomFieldsRequiredShape,
  getCustomFieldsValues
} from '../../models/form';
import { CustomFieldEntityType } from '../../models/customField';

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
  const { customFields } = useSelector((state) => state.customFields);

  const defaultShape = {
    name: Yup.string().required(t('required_meter_name')),
    unit: Yup.string().required(t('required_meter_unit')),
    updateFrequency: Yup.number().required(
      t('required_meter_update_frequency')
    ),
    asset: Yup.object().required(t('required_asset')).nullable(),
    ...getCustomFieldsRequiredShape(
      customFields,
      CustomFieldEntityType.METER,
      t
    )
  };

  const getFieldsAndShapes = (): [Array<IField>, { [key: string]: any }] => {
    const fields = [
      ...getFilteredFields(getMeterFields(t)),
      ...getCustomFieldsIFields(customFields, CustomFieldEntityType.METER)
    ];
    return getWOFieldsAndShapes(fields, defaultShape);
  };

  const onEditSuccess = () => {
    showSnackBar(t('changes_saved_success'), 'success');
    navigation.goBack();
  };
  const onEditFailure = (err) => showSnackBar(t('meter_edit_failure'), 'error');

  return (
    <View style={styles.container}>
      <Form
        fields={getFieldsAndShapes()[0]}
        validation={Yup.object().shape(getFieldsAndShapes()[1])}
        navigation={navigation}
        submitText={t('save')}
        values={{
          ...meter,
          ...getCustomFieldsValues(meter),
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
          formattedValues = formatCustomFields(formattedValues);
          try {
            const uploadedFiles = await uploadFiles([], values.image);
            const imageAndFiles = getImageAndFiles(uploadedFiles, meter.image);
            formattedValues = {
              ...formattedValues,
              image: imageAndFiles.image
            };
            await dispatch(editMeter(meter.id, formattedValues));
            onEditSuccess();
          } catch (err) {
            onEditFailure(err);
            throw err;
          }
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
