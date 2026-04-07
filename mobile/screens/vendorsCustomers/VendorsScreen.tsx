import {
  RefreshControl,
  ScrollView,
  StyleSheet,
  TouchableOpacity,
  View
} from 'react-native';
import { useDispatch, useSelector } from '../../store';
import * as React from 'react';
import { useContext, useEffect, useState } from 'react';
import { CompanySettingsContext } from '../../contexts/CompanySettingsContext';
import useAuth from '../../hooks/useAuth';
import { PermissionEntity } from '../../models/role';
import { getMoreVendors, getVendors } from '../../slices/vendor';
import { FilterField, SearchCriteria } from '../../models/page';
import { Avatar, Searchbar, Text } from 'react-native-paper';
import { useTranslation } from 'react-i18next';
import { Vendor } from '../../models/vendor';
import { isCloseToBottom, onSearchQueryChange } from '../../utils/overall';
import { RootStackScreenProps } from '../../types';
import { useDebouncedEffect } from '../../hooks/useDebouncedEffect';
import { useAppTheme } from '../../custom-theme';
import { IconWithLabel } from '../../components/IconWithLabel';

export default function VendorsScreen({
  navigation
}: RootStackScreenProps<'VendorsCustomers'>) {
  const { t } = useTranslation();
  const [startedSearch, setStartedSearch] = useState<boolean>(false);
  const { vendors, loadingGet, currentPageNum, lastPage } = useSelector(
    (state) => state.vendors
  );
  const theme = useAppTheme();
  const dispatch = useDispatch();
  const [searchQuery, setSearchQuery] = useState('');
  const { getFormattedDate, getUserNameById } = useContext(
    CompanySettingsContext
  );
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
        getVendors({ ...criteria, pageSize: 10, pageNum: 0, direction: 'DESC' })
      );
    }
  }, [criteria]);

  const onRefresh = () => {
    setCriteria(getCriteriaFromFilterFields([]));
  };

  const onQueryChange = (query) => {
    onSearchQueryChange<Vendor>(query, criteria, setCriteria, setSearchQuery, [
      'name',
      'address',
      'phone',
      'email',
      'vendorType',
      'description'
    ]);
  };
  useDebouncedEffect(
    () => {
      if (startedSearch) onQueryChange(searchQuery);
    },
    [searchQuery],
    1000
  );
  return (
    <View
      style={{ ...styles.container, backgroundColor: theme.colors.background }}
    >
      <Searchbar
        placeholder={t('search')}
        onFocus={() => setStartedSearch(true)}
        onChangeText={setSearchQuery}
        value={searchQuery}
        style={{ backgroundColor: theme.colors.background }}
      />
      <ScrollView
        style={styles.scrollView}
        onScroll={({ nativeEvent }) => {
          if (isCloseToBottom(nativeEvent)) {
            if (!loadingGet && !lastPage)
              dispatch(getMoreVendors(criteria, currentPageNum + 1));
          }
        }}
        refreshControl={
          <RefreshControl
            refreshing={loadingGet}
            onRefresh={onRefresh}
            colors={[theme.colors.primary]}
          />
        }
        scrollEventThrottle={400}
      >
        {!!vendors.content.length ? (
          vendors.content.map((vendor) => (
            <TouchableOpacity
              key={vendor.id}
              onPress={() =>
                navigation.push('VendorDetails', {
                  id: vendor.id,
                  vendorProp: vendor
                })
              }
            >
              <View style={styles.card}>
                <View
                  style={{
                    display: 'flex',
                    flexDirection: 'row',
                    gap: 6
                  }}
                >
                  <Avatar.Icon
                    size={50}
                    icon="store-outline"
                    style={{ backgroundColor: theme.colors.background }}
                    color={theme.colors.primary}
                  />
                  <View style={{ flex: 1 }}>
                    <View style={styles.cardHeader}>
                      <View style={{ flex: 1 }}>
                        <Text variant="titleMedium" style={styles.cardTitle}>
                          {vendor.name}
                        </Text>
                      </View>
                    </View>
                    <View style={styles.cardBody}>
                      {vendor.vendorType && (
                        <IconWithLabel
                          label={vendor.vendorType}
                          icon="truck-outline"
                          color={theme.colors.grey}
                        />
                      )}
                      {vendor.address && (
                        <IconWithLabel
                          label={vendor.address}
                          icon="map-marker-outline"
                          color={theme.colors.grey}
                        />
                      )}
                    </View>
                  </View>
                </View>
              </View>
            </TouchableOpacity>
          ))
        ) : loadingGet ? null : (
          <View
            style={{ backgroundColor: 'white', padding: 20, borderRadius: 10 }}
          >
            <Text variant={'titleLarge'}>{t('no_element_match_criteria')}</Text>
          </View>
        )}
      </ScrollView>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    // alignItems: 'center',
    justifyContent: 'center'
  },
  title: {
    fontSize: 20,
    fontWeight: 'bold'
  },
  scrollView: {
    width: '100%',
    height: '100%'
  },
  row: {
    display: 'flex',
    flexDirection: 'row',
    alignItems: 'center'
  },
  card: {
    backgroundColor: 'white',
    marginBottom: 1,
    padding: 10
  },
  cardHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'flex-start',
    marginBottom: 8
  },
  cardTitle: {
    fontWeight: 'bold',
    marginBottom: 4,
    flexShrink: 1
  },
  cardBody: {
    gap: 10
  },
  cardFooter: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginTop: 10
  }
});
