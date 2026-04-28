import { RootStackScreenProps } from '../../types';
import { View } from '../../components/Themed';
import Form from '../../components/form';
import * as Yup from 'yup';
import { StyleSheet } from 'react-native';
import { useTranslation } from 'react-i18next';
import { useContext } from 'react';
import { CompanySettingsContext } from '../../contexts/CompanySettingsContext';
import { getImageAndFiles, handleFileUpload } from '../../utils/overall';
import { useDispatch, useSelector } from '../../store';
import { editPart } from '../../slices/part';
import { CustomSnackBarContext } from '../../contexts/CustomSnackBarContext';
import { formatPartValues, getPartFields } from '../../utils/fields';
import { formatCustomFields } from '../../utils/formatters';
import useAuth from '../../hooks/useAuth';
import {
  IField,
  getCustomFieldsIFields,
  getCustomFieldsRequiredShape,
  getCustomFieldsValues
} from '../../models/form';
import { CustomFieldEntityType } from '../../models/customField';

export default function EditPartScreen({
  navigation,
  route
}: RootStackScreenProps<'EditPart'>) {
  const { t } = useTranslation();
  const { part } = route.params;
  const { getFilteredFields } = useAuth();
  const { uploadFiles, getWOFieldsAndShapes } = useContext(
    CompanySettingsContext
  );
  const { showSnackBar } = useContext(CustomSnackBarContext);
  const dispatch = useDispatch();
  const { customFields } = useSelector((state) => state.customFields);

  const defaultShape = {
    name: Yup.string().required(t('required_part_name')),
    ...getCustomFieldsRequiredShape(customFields, CustomFieldEntityType.PART, t)
  };

  const getFieldsAndShapes = (): [Array<IField>, { [key: string]: any }] => {
    const fields = [
      ...getFilteredFields(getPartFields(t)),
      ...getCustomFieldsIFields(customFields, CustomFieldEntityType.PART)
    ];
    return getWOFieldsAndShapes(fields, defaultShape);
  };

  const onEditSuccess = () => {
    showSnackBar(t('changes_saved_success'), 'success');
    navigation.goBack();
  };
  const onEditFailure = (err) => showSnackBar(t('part_edit_failure'), 'error');

  return (
    <View style={styles.container}>
      <Form
        fields={getFieldsAndShapes()[0]}
        validation={Yup.object().shape(getFieldsAndShapes()[1])}
        navigation={navigation}
        submitText={t('save')}
        values={{
          ...part,
          ...getCustomFieldsValues(part),
          assignedTo: part?.assignedTo.map((user) => {
            return {
              label: `${user.firstName} ${user.lastName}`,
              value: user.id.toString()
            };
          }),
          teams: part?.teams.map((team) => {
            return {
              label: team.name,
              value: team.id.toString()
            };
          }),
          vendors: part?.vendors.map((vendor) => {
            return {
              label: vendor.companyName,
              value: vendor.id.toString()
            };
          }),
          customers: part?.customers.map((customer) => {
            return {
              label: customer.name,
              value: customer.id.toString()
            };
          })
        }}
        onChange={({ field, e }) => {}}
        onSubmit={async (values) => {
          let formattedValues = formatPartValues(values);
          formattedValues = formatCustomFields(formattedValues);
          try {
            const imageAndFiles = await handleFileUpload(
              {
                files: formattedValues.files,
                image: formattedValues.image
              },
              uploadFiles
            );
            formattedValues = {
              ...formattedValues,
              image: imageAndFiles.image,
              files: imageAndFiles.files
            };
            await dispatch(editPart(part.id, formattedValues));
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
