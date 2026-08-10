import { StyleSheet, View } from 'react-native';
import { useDispatch, useSelector } from '../../store';
import * as React from 'react';
import { useCallback, useEffect, useState } from 'react';
import useAuth from '../../hooks/useAuth';
import { PermissionEntity } from '../../models/role';
import { getMoreParts, getParts } from '../../slices/part';
import { FilterField, SearchCriteria } from '../../models/page';
import { Searchbar } from 'react-native-paper';
import { useTranslation } from 'react-i18next';
import Part from '../../models/part';
import { onSearchQueryChange } from '../../utils/overall';
import { RootStackScreenProps } from '../../types';
import { useDebouncedEffect } from '../../hooks/useDebouncedEffect';
import { useAppTheme } from '../../custom-theme';
import {
  EmptyState,
  EntityListCard,
  PaginatedEntityList
} from '../../components/ui';

export const getFormattedQuantityWithUnit = (
  quantity: number,
  unit: string
) => (unit ? `${quantity} ${unit}` : `${quantity}`);

export default function PartsScreen({
  navigation
}: RootStackScreenProps<'Parts'>) {
  const { t } = useTranslation();
  const [startedSearch, setStartedSearch] = useState<boolean>(false);
  const { parts, loadingGet, currentPageNum, lastPage } = useSelector(
    (state) => state.parts
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
    if (hasViewPermission(PermissionEntity.PARTS_AND_MULTIPARTS)) {
      dispatch(
        getParts({ ...criteria, pageSize: 10, pageNum: 0, direction: 'DESC' })
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
    onSearchQueryChange<Part>(query, criteria, setCriteria, setSearchQuery, [
      'name',
      'description',
      'additionalInfos',
      'area',
      'barcode',
      'customFieldValues.value'
    ]);
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
    ({ item: part }: { item: Part }) => {
      const lowStock = part.quantity < part.minQuantity;
      return (
        <EntityListCard
          title={part.name}
          imageUrl={part.image?.url}
          icon="archive-outline"
          meta={[
            {
              icon: 'toolbox-outline',
              label: t('remaining_parts', {
                quantity: getFormattedQuantityWithUnit(part.quantity, part.unit)
              }),
              color: lowStock ? theme.colors.error : theme.colors.grey
            }
          ]}
          onPress={() =>
            navigation.push('PartDetails', { id: part.id, partProp: part })
          }
        />
      );
    },
    [navigation, t, theme.colors.error, theme.colors.grey]
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
        data={parts.content}
        keyExtractor={(part) => part.id.toString()}
        renderItem={renderItem}
        loading={loadingGet}
        refreshing={refreshing}
        onRefresh={onRefresh}
        onEndReached={() => {
          if (!loadingGet && !lastPage)
            dispatch(getMoreParts(criteria, currentPageNum + 1));
        }}
        ListEmptyComponent={
          <EmptyState
            icon={isFiltered ? 'filter-remove-outline' : 'archive-outline'}
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
