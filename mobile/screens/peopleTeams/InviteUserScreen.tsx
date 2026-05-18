import { RefreshControl, ScrollView, StyleSheet } from 'react-native';

import { View } from '../../components/Themed';
import {
  Button,
  HelperText,
  List,
  RadioButton,
  TextInput,
  useTheme
} from 'react-native-paper';
import { useTranslation } from 'react-i18next';
import * as React from 'react';
import { useContext, useEffect, useState } from 'react';
import { emailRegExp } from '../../utils/validators';
import { useDispatch, useSelector } from '../../store';
import { getRoles } from '../../slices/role';
import { Role, RoleCode } from '../../models/role';
import { inviteUsers } from '../../slices/user';
import { CustomSnackBarContext } from '../../contexts/CustomSnackBarContext';
import { RootStackScreenProps } from '../../types';

export default function InviteUserScreen({
  navigation
}: RootStackScreenProps<'AddUser'>) {
  const theme = useTheme();
  const { t } = useTranslation();
  const dispatch = useDispatch();
  const [email, setEmail] = useState<string>('');
  const { roles, loadingGet } = useSelector((state) => state.roles);
  const [selectedRole, setSelectedRole] = useState<number>();
  const [hasSubmitted, setHasSubmitted] = useState<boolean>(false);
  const [isInviteSubmitting, setIsInviteSubmitting] = useState<boolean>(false);
  const { showSnackBar } = useContext(CustomSnackBarContext);
  useEffect(() => {
    dispatch(getRoles());
  }, []);
  const defaultRoles: Partial<
    Record<RoleCode, { name: string; description: string }>
  > = {
    ADMIN: {
      name: 'ADMIN_name',
      description: 'ADMIN_description'
    },
    LIMITED_ADMIN: {
      name: 'LIMITED_ADMIN_name',
      description: 'LIMITED_ADMIN_description'
    },
    TECHNICIAN: {
      name: 'TECHNICIAN_name',
      description: 'TECHNICIAN_description'
    },
    LIMITED_TECHNICIAN: {
      name: 'LIMITED_TECHNICIAN_name',
      description: 'LIMITED_TECHNICIAN_description'
    },
    VIEW_ONLY: {
      name: 'VIEW_ONLY_name',
      description: 'VIEW_ONLY_description'
    },
    REQUESTER: {
      name: 'REQUESTER_name',
      description: 'REQUESTER_description'
    }
  };
  const getOrderedRoles = (): Role[] => {
    if (roles.length) {
      const defaultRolesOnly: Role[] = Object.keys(defaultRoles).map((code) => {
        return roles.find((role) => role.code === code);
      });
      const customRoles = roles.filter((role) => role.code === 'USER_CREATED');
      return [...defaultRolesOnly, ...customRoles];
    } else return [];
  };
  const onInvite = () => {
    setHasSubmitted(true);
    const invite = (emails: string[]) => {
      setIsInviteSubmitting(true);
      dispatch(inviteUsers(selectedRole, emails))
        .then(() => {
          navigation.goBack();
          showSnackBar(t('user_invite_success'), 'success');
        })
        .catch((err) => showSnackBar(t('user_invite_failure'), 'error'))
        .finally(() => setIsInviteSubmitting(false));
    };
    if (selectedRole) {
      if (emailRegExp.test(email)) {
        invite([email]);
      } else showSnackBar(t('invalid_email'), 'error');
    } else showSnackBar(t('please_select_role'), 'error');
  };
  return (
    <ScrollView
      style={{ ...styles.container, backgroundColor: theme.colors.background }}
      refreshControl={
        <RefreshControl
          refreshing={loadingGet}
          onRefresh={() => dispatch(getRoles())}
          colors={[theme.colors.primary]}
        />
      }
    >
      <View style={{ padding: 20 }}>
        <TextInput
          error={hasSubmitted && !emailRegExp.test(email)}
          label={t('email')}
          onChangeText={(text) => setEmail(text)}
          value={email}
          mode="outlined"
        />
        {!emailRegExp.test(email) && hasSubmitted && (
          <HelperText type="error">{t('invalid_email')}</HelperText>
        )}
        {getOrderedRoles().map((role, index) => (
          <View>
            <List.Item
              title={
                role.code === 'USER_CREATED'
                  ? role.name
                  : t(defaultRoles[role.code].name)
              }
              description={
                role.code === 'USER_CREATED'
                  ? role.description
                  : t(defaultRoles[role.code].description)
              }
              descriptionNumberOfLines={10}
              onPress={() => setSelectedRole(role.id)}
              left={(props) => (
                <RadioButton
                  value="first"
                  status={selectedRole === role.id ? 'checked' : 'unchecked'}
                  onPress={() => setSelectedRole(role.id)}
                />
              )}
            />
          </View>
        ))}
        <Button
          disabled={isInviteSubmitting}
          loading={isInviteSubmitting}
          onPress={onInvite}
          style={{ marginTop: 20 }}
          mode="contained"
        >
          {t('invite')}
        </Button>
      </View>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1
  }
});
