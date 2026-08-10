import { StyleSheet, View } from 'react-native';
import { useDispatch, useSelector } from '../../store';
import * as React from 'react';
import { useCallback, useEffect, useState } from 'react';
import useAuth from '../../hooks/useAuth';
import { PermissionEntity } from '../../models/role';
import { getCustomers, getMoreCustomers } from '../../slices/customer';
import { FilterField, SearchCriteria } from '../../models/page';
import { Searchbar } from 'react-native-paper';
import { useTranslation } from 'react-i18next';
import { Customer } from '../../models/customer';
import { onSearchQueryChange } from '../../utils/overall';
import { RootStackScreenProps } from '../../types';
import { useDebouncedEffect } from '../../hooks/useDebouncedEffect';
import { useAppTheme } from '../../custom-theme';
import {
  EmptyState,
  EntityListCard,
  PaginatedEntityList
} from '../../components/ui';

export default function CustomersScreen({
  navigation
}: RootStackScreenProps<'VendorsCustomers'>) {
  const { t } = useTranslation();
  const [startedSearch, setStartedSearch] = useState<boolean>(false);
  const { customers, loadingGet, currentPageNum, lastPage } = useSelector(
    (state) => state.customers
  );
  const theme = useAppTheme();
  const dispatch = useDispatch();
  const [searchQuery, setSearchQuery] = useState('');
  const [refreshing, setRefreshing] = useState(false);
  const { hasViewPermission } = useAuth();
  const defaultFilterFields: FilterField[] = [];
  const getCriteriaFromFilterFields = (filterFields: FilterField[]) => {
    const initialCriteria: SearchCriteria = {
      filterFields: defaultFilterFields,
      pageSize: 10,
      pageNum: 0,
      direction: 'DESC'
    };
    let newFilterFields = [...initialCriteria.filterFields];
    filterFields.forEach(
      (filterField) =>
        (newFilterFields = newFilterFields.filter(
          (ff) => ff.field != filterField.field
        ))
    );
    return {
      ...initialCriteria,
      filterFields: [...newFilterFields, ...filterFields]
    };
  };
  const [criteria, setCriteria] = useState<SearchCriteria>(
    getCriteriaFromFilterFields([])
  );

  useEffect(() => {
    if (hasViewPermission(PermissionEntity.VENDORS_AND_CUSTOMERS)) {
      dispatch(
        getCustomers({
          ...criteria,
          pageSize: 10,
          pageNum: 0,
          direction: 'DESC'
        })
      );
    }
  }, [criteria]);

  useEffect(() => {
    if (!loadingGet) setRefreshing(false);
  }, [loadingGet]);

  const onRefresh = () => {
    setRefreshing(true);
    setCriteria(getCriteriaFromFilterFields([]));
  };

  const onQueryChange = (query) => {
    onSearchQueryChange<Customer>(
      query,
      criteria,
      setCriteria,
      setSearchQuery,
      [
        'name',
        'address',
        'phone',
        'email',
        'customerType',
        'description',
        'billingAddress',
        'billingName',
        'customFieldValues.value'
      ]
    );
  };
  useDebouncedEffect(
    () => {
      if (startedSearch) onQueryChange(searchQuery);
    },
    [searchQuery],
    1000
  );

  const isFiltered = !!searchQuery;
  const renderItem = useCallback(
    ({ item: customer }: { item: Customer }) => {
      const meta = [];
      if (customer.customerType) {
        meta.push({
          icon: 'account-box-outline' as const,
          label: customer.customerType
        });
      }
      if (customer.address) {
        meta.push({ icon: 'map-marker-outline' as const, label: customer.address });
      }
      return (
        <EntityListCard
          title={customer.name}
          icon="account-group-outline"
          meta={meta}
          onPress={() =>
            navigation.push('CustomerDetails', {
              id: customer.id,
              customerProp: customer
            })
          }
        />
      );
    },
    [navigation]
  );

  return (
    <View style={[styles.container, { backgroundColor: theme.colors.background }]}>
      <Searchbar
        placeholder={t('search')}
        accessibilityLabel={t('search')}
        onFocus={() => setStartedSearch(true)}
        onChangeText={setSearchQuery}
        value={searchQuery}
        style={{ backgroundColor: theme.colors.card }}
      />
      <PaginatedEntityList
        data={customers.content}
        keyExtractor={(customer) => customer.id.toString()}
        renderItem={renderItem}
        loading={loadingGet}
        refreshing={refreshing}
        onRefresh={onRefresh}
        onEndReached={() => {
          if (!loadingGet && !lastPage)
            dispatch(getMoreCustomers(criteria, currentPageNum + 1));
        }}
        ListEmptyComponent={
          <EmptyState
            icon={isFiltered ? 'filter-remove-outline' : 'account-group-outline'}
            title={t('no_element_match_criteria')}
            description={
              isFiltered ? t('no_element_match_criteria_description') : undefined
            }
            action={
              isFiltered
                ? {
                    label: t('reset'),
                    onPress: () => {
                      setSearchQuery('');
                      setCriteria(getCriteriaFromFilterFields([]));
                    }
                  }
                : undefined
            }
          />
        }
      />
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1
  }
});
