import { useTranslation } from 'react-i18next';
import Part from '../../../models/part';
import * as React from 'react';
import { useContext } from 'react';
import { CompanySettingsContext } from '../../../contexts/CompanySettingsContext';
import { Image, ScrollView, StyleSheet } from 'react-native';
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
import { getFormattedCostPerUnit } from '../../../utils/formatters';
import { getFormattedQuantityWithUnit } from '../PartsScreen';

export default function PartDetails({ part }: { part: Part }) {
  const { t } = useTranslation();
  const { getFormattedCurrency } = useContext(CompanySettingsContext);
  const theme = useTheme();
  const fieldsToRender: {
    label: string;
    value: string | number;
  }[] = [
    {
      label: t('name'),
      value: part.name
    },
    {
      label: t('id'),
      value: part.id
    },
    {
      label: t('description'),
      value: part.description
    },
    {
      label: t('additional_information'),
      value: part.additionalInfos
    },
    {
      label: t('cost'),
      value: getFormattedCostPerUnit(part.cost, part.unit, getFormattedCurrency)
    },
    {
      label: t('quantity'),
      value: getFormattedQuantityWithUnit(part.quantity, part.unit)
    },
    {
      label: t('minimum_quantity'),
      value: getFormattedQuantityWithUnit(part.minQuantity, part.unit)
    },
    {
      label: t('barcode'),
      value: part.barcode
    },
    {
      label: t('area'),
      value: part.area
    }
  ];
  return (
    <ScrollView
      style={{ ...styles.container, backgroundColor: theme.colors.background }}
    >
      {part.image && (
        <Image style={{ height: 200 }} source={{ uri: part.image.url }} />
      )}
      {fieldsToRender.map((field) => (
        <BasicField key={field.label} label={field.label} value={field.value} />
      ))}
      <ListField
        values={part?.assignedTo}
        label={t('assigned_to')}
        getHref={(user: UserMiniDTO) => getUserUrl(user.id)}
        getValueLabel={(user: UserMiniDTO) =>
          `${user.firstName} ${user.lastName}`
        }
      />
      <ListField
        values={part?.customers}
        label={t('customers')}
        getHref={(customer: Customer) => getCustomerUrl(customer.id)}
        getValueLabel={(customer: Customer) => customer.name}
      />
      <ListField
        values={part?.vendors}
        label={t('vendors')}
        getHref={(vendor: Vendor) => getVendorUrl(vendor.id)}
        getValueLabel={(vendor: Vendor) => vendor.companyName}
      />
      <ListField
        values={part?.teams}
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
