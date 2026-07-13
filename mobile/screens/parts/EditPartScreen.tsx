import { RootStackScreenProps } from '../../types';
import { View } from '../../components/Themed';
import Form from '../../components/form';
import * as Yup from 'yup';
import { StyleSheet } from 'react-native';
import { useTranslation } from 'react-i18next';
import { useContext } from 'react';
import { CompanySettingsContext } from '../../contexts/CompanySettingsContext';
import { getImageAndFiles } from '../../utils/overall';
import { useDispatch } from '../../store';
import { editPart } from '../../slices/part';
import { CustomSnackBarContext } from '../../contexts/CustomSnackBarContext';
import { formatPartValues, getPartFields } from '../../utils/fields';
import useAuth from '../../hooks/useAuth';

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
  const shape = {
    name: Yup.string().required(t('required_part_name'))
  };

  const onEditSuccess = () => {
    showSnackBar(t('changes_saved_success'), 'success');
    navigation.goBack();
  };
  const onEditFailure = (err) => showSnackBar(t('part_edit_failure'), 'error');

  return (
    <View style={styles.container}>
      <Form
        fields={getFilteredFields(getPartFields(t))}
        validation={Yup.object().shape(shape)}
        navigation={navigation}
        submitText={t('save')}
        values={{
          ...part,
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
          return new Promise<void>((resolve, rej) => {
            const files = formattedValues.files.find((file) => file.id)
              ? []
              : formattedValues.files;
            uploadFiles(files, formattedValues.image)
              .then((files) => {
                const imageAndFiles = getImageAndFiles(files, part.image);
                formattedValues = {
                  ...formattedValues,
                  image: imageAndFiles.image,
                  files: [...part.files, ...imageAndFiles.files]
                };
                dispatch(editPart(part.id, formattedValues))
                  .then(onEditSuccess)
                  .catch(onEditFailure)
                  .finally(resolve);
              })
              .catch((err) => {
                onEditFailure(err);
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
