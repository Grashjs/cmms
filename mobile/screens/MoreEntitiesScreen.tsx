import { ScrollView, StyleSheet, TouchableOpacity } from 'react-native';
import { View } from '../components/Themed';
import { IconSource } from 'react-native-paper/src/components/Icon';
import { IconButton, Text, useTheme } from 'react-native-paper';
import { useTranslation } from 'react-i18next';
import { RootStackParamList, RootTabScreenProps } from '../types';
import useAuth from '../hooks/useAuth';
import { PermissionEntity } from '../models/role';
import { UiConfiguration } from '../models/uiConfiguration';

export default function MoreEntitiesScreen({
  navigation
}: RootTabScreenProps<'MoreEntities'>) {
  const theme = useTheme();
  const { t } = useTranslation();
  const { hasViewPermission, user } = useAuth();
  const entities: {
    label: string;
    icon: IconSource;
    color: string;
    backgroundColor: string;
    link: keyof RootStackParamList;
    visible: boolean;
    uiConfigKey?: keyof UiConfiguration;
  }[] = [
    {
      label: 'locations',
      icon: 'map-marker',
      color: '#2491d1',
      backgroundColor: '#c8cfd3',
      link: 'Locations',
      visible: hasViewPermission(PermissionEntity.LOCATIONS),
      uiConfigKey: 'locations'
    },
    {
      label: 'assets',
      icon: 'package-variant-closed',
      // @ts-ignore
      color: theme.colors.warning,
      backgroundColor: '#d2d0c4',
      link: 'Assets',
      visible: hasViewPermission(PermissionEntity.ASSETS)
    },
    {
      label: 'parts',
      icon: 'archive-outline',
      color: '#8324d1',
      backgroundColor: '#cfc8d3',
      link: 'Parts',
      visible: hasViewPermission(PermissionEntity.PARTS_AND_MULTIPARTS)
    },
    {
      label: 'meters',
      icon: 'gauge',
      color: '#d12444',
      backgroundColor: '#d3c8ca',
      link: 'Meters',
      visible: hasViewPermission(PermissionEntity.METERS),
      uiConfigKey: 'meters'
    },
    {
      label: 'people_teams',
      icon: 'account',
      color: '#245bd1',
      backgroundColor: '#c8ccd3',
      link: 'PeopleTeams',
      visible: hasViewPermission(PermissionEntity.PEOPLE_AND_TEAMS)
    },
    {
      label: 'vendors_and_customers',
      icon: 'vector-circle',
      //@ts-ignore
      color: theme.colors.warning,
      backgroundColor: '#d2d0c4',
      link: 'VendorsCustomers',
      visible: hasViewPermission(PermissionEntity.VENDORS_AND_CUSTOMERS),
      uiConfigKey: 'vendorsAndCustomers'
    }
  ];
  return (
    <ScrollView
      style={{
        ...styles.container,
        backgroundColor: theme.colors.background,
        paddingHorizontal: 10
      }}
    >
      {entities
        .filter(
          (entity) =>
            entity.visible &&
            (entity.uiConfigKey
              ? user.uiConfiguration[entity.uiConfigKey]
              : true)
        )
        .map(({ label, icon, color, backgroundColor, link }) => (
          <TouchableOpacity
            key={label}
            //@ts-ignore
            onPress={() => navigation.navigate(link)}
          >
            <View
              style={{
                backgroundColor,
                display: 'flex',
                flexDirection: 'row',
                marginVertical: 5,
                borderRadius: 10,
                justifyContent: 'space-between',
                alignItems: 'center',
                width: '100%',
                padding: 20
              }}
            >
              <Text variant={'titleMedium'}>{t(label)}</Text>
              <IconButton icon={icon} iconColor={color} />
            </View>
          </TouchableOpacity>
        ))}
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1
  },
  title: {
    fontSize: 20,
    fontWeight: 'bold'
  },
  separator: {
    marginVertical: 30,
    height: 1,
    width: '80%'
  }
});
