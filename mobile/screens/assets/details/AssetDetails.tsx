import { useTranslation } from 'react-i18next';
import { AssetDTO as Asset } from '../../../models/asset';
import * as React from 'react';
import { useContext } from 'react';
import { CompanySettingsContext } from '../../../contexts/CompanySettingsContext';
import { Image, ScrollView, StyleSheet, TouchableOpacity } from 'react-native';
import { View } from '../../../components/Themed';
import { Divider, Text, useTheme } from 'react-native-paper';
import { UserMiniDTO } from '../../../models/user';
import { Customer } from '../../../models/customer';
import { Vendor } from '../../../models/vendor';
import Team from '../../../models/team';
import {
  getCustomerUrl,
  getTeamUrl,
  getUserUrl,
  getVendorUrl
} from '../../../utils/urlPaths';
import ListField from '../../../components/ListField';
import BasicField from '../../../components/BasicField';

export default function AssetDetails({
                                       asset,
                                       navigation
                                     }: {
  asset: Asset;
  navigation: any;
}) {
  const { getFormattedDate, getFormattedCurrency } = useContext(
    CompanySettingsContext
  );
  const { t } = useTranslation();
  const theme = useTheme();
  const fieldsToRender: {
    label: string;
    value: string | number;
  }[] = [
    { label: t('name'), value: asset?.name },
    { label: t('description'), value: asset?.description },
    { label: t('category'), value: asset?.category?.name },
    { label: t('model'), value: asset?.model },
    { label: t('serial_number'), value: asset?.serialNumber },
    {
      label: t('status'),
      value: asset?.status === 'OPERATIONAL' ? t('operational') : t('down')
    },
    {
      label: t('acquisition_cost'),
      value: asset?.acquisitionCost
        ? getFormattedCurrency(asset?.acquisitionCost)
        : null
    },
    { label: t('area'), value: asset?.area },
    { label: t('barcode'), value: asset?.barCode },
    {
      label: t('placed_in_service'),
      value: getFormattedDate(asset?.inServiceDate)
    },
    {
      label: t('warranty_expiration'),
      value: getFormattedDate(asset?.warrantyExpirationDate)
    }
  ];
  return (
    <ScrollView
      style={{ ...styles.container, backgroundColor: theme.colors.background }}
    >
      {asset.image && (
        <Image style={{ height: 200 }} source={{ uri: asset.image.url }} />
      )}
      {fieldsToRender.map(
        (field) =>
          <BasicField
            key={field.label}
            label={field.label}
            value={field.value}
          />
      )}
      {asset.primaryUser && (
        <View>
          <View
            style={{
              display: 'flex',
              flexDirection: 'row',
              justifyContent: 'space-between',
              padding: 20
            }}
          >
            <Text>{t('primary_worker')}</Text>
            <TouchableOpacity
              onPress={() =>
                navigation.push('UserDetails', { id: asset.primaryUser.id })
              }
            >
              <Text
                style={{
                  fontWeight: 'bold',
                  color: theme.colors.primary
                }}
              >{`${asset.primaryUser.firstName} ${asset.primaryUser.lastName}`}</Text>
            </TouchableOpacity>
          </View>
          <Divider />
        </View>
      )}
      {asset.location && (
        <View>
          <View
            style={{
              display: 'flex',
              flexDirection: 'row',
              justifyContent: 'space-between',
              padding: 20
            }}
          >
            <Text>{t('location')}</Text>
            <TouchableOpacity
              onPress={() =>
                navigation.push('LocationDetails', { id: asset.location.id })
              }
            >
              <Text
                style={{
                  fontWeight: 'bold',
                  color: theme.colors.primary
                }}
              >
                {asset.location.name}
              </Text>
            </TouchableOpacity>
          </View>
          <Divider />
        </View>
      )}
      <ListField
        values={asset?.assignedTo}
        label={t('assigned_to')}
        getHref={(user: UserMiniDTO) => getUserUrl(user.id)}
        getValueLabel={(user: UserMiniDTO) =>
          `${user.firstName} ${user.lastName}`
        }
      />
      <ListField
        values={asset?.customers}
        label={t('customers')}
        getHref={(customer: Customer) => getCustomerUrl(customer.id)}
        getValueLabel={(customer: Customer) => customer.name}
      />
      <ListField
        values={asset?.vendors}
        label={t('vendors')}
        getHref={(vendor: Vendor) => getVendorUrl(vendor.id)}
        getValueLabel={(vendor: Vendor) => vendor.companyName}
      />
      <ListField
        values={asset?.teams}
        label={t('teams')}
        getHref={(team: Team) => getTeamUrl(team.id)}
        getValueLabel={(team: Team) => team.name}
      />
    </ScrollView>
  );
}
const styles = StyleSheet.create({
  container: {
    flex: 1
  }
});
