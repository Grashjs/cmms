import {
  Avatar,
  Box,
  Card,
  CardHeader,
  Typography,
  CircularProgress,
  Divider,
  List,
  ListItem,
  ListItemAvatar,
  ListItemText,
  Radio,
  Stack,
  styled,
  useTheme
} from '@mui/material';
import { useTranslation } from 'react-i18next';
import Text from 'src/components/Text';
import { Engineering } from '@mui/icons-material';
import React, { useEffect, useState } from 'react';
import { useDispatch, useSelector } from '../../../store';
import { getRoles } from '../../../slices/role';
import { Role, RoleCode } from '../../../models/owns/role';

const AvatarWrapperSuccess = styled(Avatar)(
  ({ theme }) => `
      background-color: ${theme.colors.success.lighter};
      color:  ${theme.colors.success.main};
`
);

interface Props {
  onChange: (id: number) => void;
}

function UserRoleCardList({ onChange }: Props) {
  const { t }: { t: any } = useTranslation();
  const theme = useTheme();
  const dispatch = useDispatch();
  const { roles, loadingGet } = useSelector((state) => state.roles);
  const [loadError, setLoadError] = useState(false);
  const [selectedItem, setSelectedItem] = useState<number>();
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
  useEffect(() => {
    dispatch(getRoles({ operation: 'invite' })).catch(() => setLoadError(true));
  }, []);
  const isSelected = (value) => selectedItem === value;

  const handleChange = (value: number) => {
    if (!isSelected(value)) {
      setSelectedItem(value);
      onChange(value);
    }
  };

  const getOrderedRoles = (): Role[] => {
    if (roles.length) {
      const defaultRolesOnly: Role[] = Object.keys(defaultRoles).map((code) => {
        return roles.find((role) => role.code === code);
      });
      const customRoles = roles.filter((role) => role.code === 'USER_CREATED');
      return [...defaultRolesOnly, ...customRoles].filter(Boolean);
    } else return [];
  };
  return (
    <Card>
      <CardHeader title={t('select_user_role')} />
      <Divider />
      {loadingGet ? (
        <Stack
          direction="row"
          width="100%"
          alignItems="center"
          height={200}
          justifyContent="center"
        >
          <CircularProgress />
        </Stack>
      ) : (
        <List disablePadding>
          {(loadError || !roles.length) && (
            <Typography sx={{ p: 2 }}>
              {t(
                loadError ? 'management_access_denied' : 'no_assignable_roles'
              )}
            </Typography>
          )}
          {getOrderedRoles().map((role, index) => (
            <Box
              key={role.id}
              style={{ cursor: 'pointer' }}
              onClick={() => handleChange(role.id)}
            >
              <ListItem
                sx={[
                  { py: 2 },
                  isSelected(role.id) && {
                    border: `2px solid ${theme.colors.primary.main}`,
                    borderRadius: 0.5
                  }
                ]}
              >
                <ListItemAvatar>
                  <AvatarWrapperSuccess>
                    <Engineering />
                  </AvatarWrapperSuccess>
                </ListItemAvatar>

                <ListItemText
                  primary={
                    <Text color="black">
                      {role.code === 'USER_CREATED'
                        ? role.name
                        : t(defaultRoles[role.code].name)}
                    </Text>
                  }
                  primaryTypographyProps={{
                    variant: 'body1',
                    fontWeight: 'bold',
                    color: 'textPrimary',
                    gutterBottom: true,
                    noWrap: true
                  }}
                  secondary={
                    <Text color="black">
                      {role.code === 'USER_CREATED'
                        ? role.description
                        : t(defaultRoles[role.code].description)}
                    </Text>
                  }
                  secondaryTypographyProps={{ variant: 'body2' }}
                />

                <Radio
                  checked={isSelected(role.id)}
                  onChange={() => handleChange(role.id)}
                  name="radio-buttons"
                  color="primary"
                />
              </ListItem>
              <Divider />
            </Box>
          ))}
        </List>
      )}
    </Card>
  );
}

export default UserRoleCardList;
