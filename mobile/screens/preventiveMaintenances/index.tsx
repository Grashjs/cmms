import * as React from 'react';
import { useCallback, useEffect, useState } from 'react';
import { FlatList, RefreshControl, StyleSheet, View } from 'react-native';
import { Searchbar } from 'react-native-paper';
import { useTranslation } from 'react-i18next';
import { RootStackScreenProps } from '../../types';
import { useDispatch, useSelector } from '../../store';
import { getPreventiveMaintenances } from '../../slices/preventiveMaintenance';
import PreventiveMaintenance from '../../models/preventiveMaintenance';
import { SearchCriteria } from '../../models/page';
import { onSearchQueryChange } from '../../utils/overall';
import { useDebouncedEffect } from '../../hooks/useDebouncedEffect';
import { useAppTheme } from '../../custom-theme';
import { EmptyState, ListSkeleton } from '../../components/ui';
import { spacing } from '../../theme/tokens';
import PreventiveMaintenanceCard from './components/PreventiveMaintenanceCard';

export default function PreventiveMaintenancesScreen({
  navigation
}: RootStackScreenProps<'PreventiveMaintenances'>) {
  const { t } = useTranslation();
  const theme = useAppTheme();
  const dispatch = useDispatch();
  const { preventiveMaintenances, loadingGet } = useSelector(
    (state) => state.preventiveMaintenances
  );
  const [searchQuery, setSearchQuery] = useState('');
  const [startedSearch, setStartedSearch] = useState(false);
  const [refreshing, setRefreshing] = useState(false);
  const [criteria, setCriteria] = useState<SearchCriteria>({
    filterFields: [],
    pageSize: 25,
    pageNum: 0,
    direction: 'DESC'
  });

  useEffect(() => {
    dispatch(getPreventiveMaintenances(criteria));
  }, [criteria]);

  useEffect(() => {
    if (!loadingGet) setRefreshing(false);
  }, [loadingGet]);

  useDebouncedEffect(
    () => {
      if (startedSearch) {
        onSearchQueryChange<PreventiveMaintenance>(
          searchQuery,
          criteria,
          setCriteria,
          setSearchQuery,
          ['name', 'title', 'description']
        );
      }
    },
    [searchQuery],
    600
  );

  const renderItem = useCallback(
    ({ item }: { item: PreventiveMaintenance }) => (
      <PreventiveMaintenanceCard
        preventiveMaintenance={item}
        onPress={() => navigation.navigate('PMDetails', { id: item.id })}
      />
    ),
    [navigation]
  );

  const isInitialLoad = loadingGet && !preventiveMaintenances.content.length;

  return (
    <View
      style={[styles.container, { backgroundColor: theme.colors.background }]}
    >
      <Searchbar
        placeholder={t('search')}
        onFocus={() => setStartedSearch(true)}
        onChangeText={setSearchQuery}
        value={searchQuery}
        style={{ backgroundColor: theme.colors.card }}
      />
      {isInitialLoad ? (
        <ListSkeleton />
      ) : (
        <FlatList
          data={preventiveMaintenances.content}
          keyExtractor={(item) => item.id.toString()}
          renderItem={renderItem}
          contentContainerStyle={styles.list}
          refreshControl={
            <RefreshControl
              refreshing={refreshing}
              tintColor={theme.colors.primary}
              colors={[theme.colors.primary]}
              onRefresh={() => {
                setRefreshing(true);
                dispatch(getPreventiveMaintenances(criteria));
              }}
            />
          }
          ListEmptyComponent={
            <EmptyState
              icon={searchQuery ? 'magnify-close' : 'calendar-sync-outline'}
              title={
                searchQuery
                  ? t('no_element_match_criteria')
                  : t('no_preventive_maintenance')
              }
              description={
                searchQuery
                  ? t('no_element_match_criteria_description')
                  : t('no_preventive_maintenance_description')
              }
            />
          }
        />
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1
  },
  list: {
    paddingTop: spacing.md,
    paddingBottom: 60,
    flexGrow: 1
  }
});
