import { RootStackScreenProps } from '../../types';
import { View } from '../../components/Themed';
import Form from '../../components/form';
import * as Yup from 'yup';
import { StyleSheet } from 'react-native';
import { useTranslation } from 'react-i18next';
import { useContext } from 'react';
import { useDispatch } from '../../store';
import { editTeam } from '../../slices/team';
import { CustomSnackBarContext } from '../../contexts/CustomSnackBarContext';
import { getTeamFields } from '../../utils/fields';
import { formatSelectMultiple } from '../../utils/formatters';

export default function EditTeamScreen({
  navigation,
  route
}: RootStackScreenProps<'EditTeam'>) {
  const { t } = useTranslation();
  const { team } = route.params;
  const { showSnackBar } = useContext(CustomSnackBarContext);
  const dispatch = useDispatch();
  const shape = {
    name: Yup.string().required('required_team_name')
  };
  const onEditSuccess = () => {
    showSnackBar(t('changes_saved_success'), 'success');
    navigation.goBack();
  };
  const onEditFailure = (err) => showSnackBar(t('team_edit_failure'), 'error');

  return (
    <View style={styles.container}>
      <Form
        fields={getTeamFields(t)}
        validation={Yup.object().shape(shape)}
        navigation={navigation}
        submitText={t('save')}
        values={{
          ...team,
          users: team.users.map((user) => {
            return {
              label: `${user.firstName} ${user.lastName}`,
              value: user.id
            };
          })
        }}
        onChange={({ field, e }) => {}}
        onSubmit={async (values) => {
          const newValues = { ...values };
          newValues.users = formatSelectMultiple(newValues.users);
          return dispatch(editTeam(team.id, newValues))
            .then(onEditSuccess)
            .catch(onEditFailure);
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
