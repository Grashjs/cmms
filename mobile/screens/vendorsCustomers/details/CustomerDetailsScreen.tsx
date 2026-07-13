import { Linking, Pressable, ScrollView, TouchableOpacity } from 'react-native';
import * as React from 'react';
import { useContext, useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { RootStackScreenProps } from '../../../types';
import { useDispatch, useSelector } from '../../../store';
import { View } from '../../../components/Themed';
import {
  Button,
  Dialog,
  Divider,
  IconButton,
  Portal,
  Text,
  useTheme
} from 'react-native-paper';
import LoadingDialog from '../../../components/LoadingDialog';
import { deleteCustomer, getCustomerDetails } from '../../../slices/customer';
import { SheetManager } from 'react-native-actions-sheet';
import { CustomSnackBarContext } from '../../../contexts/CustomSnackBarContext';
import { CompanySettingsContext } from '../../../contexts/CompanySettingsContext';
import BasicField from '../../../components/BasicField';

export default function CustomerDetailsScreen({
                                                navigation,
                                                route
                                              }: RootStackScreenProps<'CustomerDetails'>) {
  const { id, customerProp } = route.params;
  const { customerInfos, loadingGet } = useSelector((state) => state.customers);
  const customer = customerInfos[id]?.customer ?? customerProp;
  const theme = useTheme();
  const { getFormattedCurrency } = useContext(CompanySettingsContext);
  const dispatch = useDispatch();
  const { t } = useTranslation();
  const { showSnackBar } = useContext(CustomSnackBarContext);
  const [openDelete, setOpenDelete] = useState<boolean>(false);
  useEffect(() => {
    if (!route.params.customerProp)
      dispatch(getCustomerDetails(route.params.id));
  }, [route]);

  useEffect(() => {
    navigation.setOptions({
      title: customer?.name ?? t('loading'),
      headerRight: () => (
        <Pressable
          onPress={() => {
            SheetManager.show('customer-details-sheet', {
              payload: {
                onEdit: () => navigation.navigate('EditCustomer', { customer }),
                onDelete: () => setOpenDelete(true),
                customer
              }
            });
          }}
        >
          <IconButton icon='dots-vertical' />
        </Pressable>
      )
    });
  }, [customer]);
  const fieldsToRender: { label: string; value: string }[] = [
    {
      label: t('address'),
      value: customer?.address
    },
    {
      label: t('phone'),
      value: customer?.phone
    },
    {
      label: t('email'),
      value: customer?.email
    },
    {
      label: t('type'),
      value: customer?.customerType
    },
    {
      label: t('billing_currency'),
      value: customer?.billingCurrency?.name
    },
    {
      label: t('hourly_rate'),
      value: !!customer?.rate ? getFormattedCurrency(customer.rate) : null
    }
  ];

  const onDeleteSuccess = () => {
    showSnackBar(t('customer_delete_success'), 'success');
    navigation.goBack();
  };
  const onDeleteFailure = (err) =>
    showSnackBar(t('customer_delete_failure'), 'error');

  const handleDelete = () => {
    dispatch(deleteCustomer(customer?.id))
      .then(onDeleteSuccess)
      .catch(onDeleteFailure);
    setOpenDelete(false);
  };
  const renderConfirmDelete = () => {
    return (
      <Portal>
        <Dialog visible={openDelete} onDismiss={() => setOpenDelete(false)}>
          <Dialog.Title>{t('confirmation')}</Dialog.Title>
          <Dialog.Content>
            <Text variant='bodyMedium'>{t('confirm_delete_customer')}</Text>
          </Dialog.Content>
          <Dialog.Actions>
            <Button onPress={() => setOpenDelete(false)}>{t('cancel')}</Button>
            <Button onPress={handleDelete}>{t('to_delete')}</Button>
          </Dialog.Actions>
        </Dialog>
      </Portal>
    );
  };
  if (customer)
    return (
      <ScrollView style={{ flex: 1, backgroundColor: theme.colors.background }}>
        {renderConfirmDelete()}
        {fieldsToRender.map(
          ({ label, value }, index) =>
            value && <BasicField key={label} label={label} value={value} />
        )}
        {customer.website && (
          <View>
            <View
              style={{
                display: 'flex',
                flexDirection: 'row',
                justifyContent: 'space-between',
                padding: 20
              }}
            >
              <Text>{t('website')}</Text>
              <TouchableOpacity
                onPress={() =>
                  Linking.openURL(
                    customer.website.startsWith('https://')
                      ? customer.website
                      : 'https://' + customer.website
                  )
                }
              >
                <Text
                  style={{ fontWeight: 'bold', color: theme.colors.primary }}
                >
                  {customer.website}
                </Text>
              </TouchableOpacity>
            </View>
            <Divider />
          </View>
        )}
      </ScrollView>
    );
  else return <LoadingDialog visible={true} />;
}
