import {
  Image,
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
import { getMoreParts, getParts } from '../../slices/part';
import { FilterField, SearchCriteria } from '../../models/page';
import {
  Card,
  IconButton,
  List,
  Searchbar,
  Text,
  Avatar,
  TouchableRipple
} from 'react-native-paper';
import { useTranslation } from 'react-i18next';
import Part from '../../models/part';
import { isCloseToBottom, onSearchQueryChange } from '../../utils/overall';
import { RootStackScreenProps } from '../../types';
import { useDebouncedEffect } from '../../hooks/useDebouncedEffect';
import { useAppTheme } from '../../custom-theme';
import _ from 'lodash';
import { IconWithLabel } from '../../components/IconWithLabel';
import { Asset } from 'expo-asset';
export const getFormattedQuantityWithUnit = (
  quantity: number,
  unit: string
) => {
  return unit ? quantity + ' ' + unit : quantity;
};
export default function PartsScreen({
  navigation,
  route
}: RootStackScreenProps<'Parts'>) {
  const { t } = useTranslation();
  const [startedSearch, setStartedSearch] = useState<boolean>(false);
  const { parts, loadingGet, currentPageNum, lastPage } = useSelector(
    (state) => state.parts
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
    if (hasViewPermission(PermissionEntity.PARTS_AND_MULTIPARTS)) {
      dispatch(
        getParts({ ...criteria, pageSize: 10, pageNum: 0, direction: 'DESC' })
      );
    }
  }, [criteria]);

  const onRefresh = () => {
    setCriteria(getCriteriaFromFilterFields([]));
  };

  const onQueryChange = (query) => {
    onSearchQueryChange<Part>(query, criteria, setCriteria, setSearchQuery, [
      'name',
      'barcode',
      'area',
      'additionalInfos',
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
        contentContainerStyle={{ paddingBottom: 100 }}
        style={styles.scrollView}
        onScroll={({ nativeEvent }) => {
          if (isCloseToBottom(nativeEvent)) {
            if (!loadingGet && !lastPage)
              dispatch(getMoreParts(criteria, currentPageNum + 1));
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
        {!!parts.content.length ? (
          parts.content.map((part) => (
            <TouchableOpacity
              onPress={() =>
                navigation.push('PartDetails', { id: part.id, partProp: part })
              }
              key={part.id}
            >
              <View style={styles.card}>
                <View
                  style={{
                    display: 'flex',
                    flexDirection: 'row',
                    gap: 6
                  }}
                >
                  {part.image ? (
                    <Avatar.Image size={50} source={{ uri: part.image.url }} />
                  ) : (
                    <Avatar.Icon
                      style={{
                        backgroundColor: theme.colors.background
                      }}
                      color={'white'}
                      icon={'archive-outline'}
                      size={50}
                    />
                  )}
                  <View style={{ flex: 1 }}>
                    <View style={styles.cardHeader}>
                      <View style={{ flex: 1 }}>
                        <Text variant="titleMedium" style={styles.cardTitle}>
                          {part.name}
                        </Text>
                      </View>
                    </View>
                    <View style={styles.cardBody}>
                      <IconWithLabel
                        label={t('remaining_parts', {
                          quantity: getFormattedQuantityWithUnit(
                            part.quantity,
                            part.unit
                          )
                        })}
                        icon="toolbox-outline"
                        color={
                          part.quantity < part.minQuantity
                            ? theme.colors.error
                            : theme.colors.grey
                        }
                      />
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
