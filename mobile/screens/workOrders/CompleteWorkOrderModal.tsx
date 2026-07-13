import { StyleSheet } from 'react-native';
import * as Yup from 'yup';
import { View } from '../../components/Themed';
import { RootStackScreenProps } from '../../types';
import { useTranslation } from 'react-i18next';
import { useContext } from 'react';
import { CompanySettingsContext } from '../../contexts/CompanySettingsContext';
import { IField } from '../../models/form';
import Form from '../../components/form';

export default function CompleteWorkOrderModal({
  navigation,
  route
}: RootStackScreenProps<'CompleteWorkOrder'>) {
  const { onComplete, fieldsConfig } = route.params;
  const { t }: { t: any } = useTranslation();
  const { uploadFiles } = useContext(CompanySettingsContext);

  const getFieldsAndShape = (): [Array<IField>, { [key: string]: any }] => {
    let fields = [];
    let shape = {};
    if (fieldsConfig.feedback) {
      fields.push({
        name: 'feedback',
        type: 'text',
        label: t('feedback'),
        placeholder: t('feedback_description'),
        multiple: true
      });
      shape = { feedback: Yup.string().required(t('required_feedback')) };
    }
    if (fieldsConfig.signature) {
      fields.push({
        name: 'signature',
        type: 'file',
        label: t('signature'),
        fileType: 'image'
      });
      shape = {
        ...shape,
        signature: Yup.array().required(t('required_signature'))
      };
    }
    return [fields, shape];
  };
  return (
    <View style={styles.container}>
      <Form
        fields={getFieldsAndShape()[0]}
        validation={Yup.object().shape(getFieldsAndShape()[1])}
        submitText={t('complete_work_order')}
        values={{}}
        navigation={navigation}
        onChange={({ field, e }) => {}}
        onSubmit={async (values) => {
          return new Promise<void>((resolve, rej) => {
            uploadFiles([], values.signature ?? [])
              .then((files) => {
                onComplete(files[0]?.id, values?.feedback).then(resolve);
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
