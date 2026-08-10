import * as React from 'react';
import { useEffect, useState } from 'react';
import { Pressable, ScrollView, StyleSheet, View } from 'react-native';
import { Icon, Searchbar, Text } from 'react-native-paper';
import { IconSource } from 'react-native-paper/lib/typescript/components/Icon';
import { useTranslation } from 'react-i18next';
import { RootStackScreenProps } from '../types';
import { useDispatch, useSelector } from '../store';
import {
  clear,
  search,
  SearchResult,
  SearchResultType
} from '../slices/globalSearch';
import { useDebouncedEffect } from '../hooks/useDebouncedEffect';
import { useAppTheme } from '../custom-theme';
import { EmptyState, ListSkeleton } from '../components/ui';
import { raisedSurface } from '../theme/surface';
import { fontWeight, radius, spacing, touchTarget } from '../theme/tokens';
import { PermissionEntity } from '../models/role';
import useAuth from '../hooks/useAuth';

/**
 * How each result type is labelled, iconified and opened. Keeping the mapping
 * in one place means the screen does not branch on type anywhere else.
 */
const TYPE_CONFIG: Record<
  SearchResultType,
  { icon: IconSource; titleKey: string; route: string; permission: PermissionEntity }
> = {
  workOrder: {
    icon: 'clipboard-text-outline',
    titleKey: 'work_orders',
    route: 'WODetails',
    permission: PermissionEntity.WORK_ORDERS
  },
  asset: {
    icon: 'package-variant-closed',
    titleKey: 'assets',
    route: 'AssetDetails',
    permission: PermissionEntity.ASSETS
  },
  location: {
    icon: 'map-marker-outline',
    titleKey: 'locations',
    route: 'LocationDetails',
    permission: PermissionEntity.LOCATIONS
  },
  part: {
    icon: 'cog-outline',
    titleKey: 'parts',
    route: 'PartDetails',
    permission: PermissionEntity.PARTS_AND_MULTIPARTS
  }
};

const ORDER: SearchResultType[] = ['workOrder', 'asset', 'location', 'part'];

export default function GlobalSearchScreen({
  navigation
}: RootStackScreenProps<'GlobalSearch'>) {
  const { t } = useTranslation();
  const theme = useAppTheme();
  const dispatch = useDispatch();
  const { hasViewPermission } = useAuth();
  const { results, loading, error } = useSelector(
    (state) => state.globalSearch
  );
  const [query, setQuery] = useState('');

  // Clearing on unmount keeps a stale result set from flashing the next time
  // the screen is opened, before the new query resolves.
  useEffect(() => () => { dispatch(clear()); }, []);

  useDebouncedEffect(
    () => {
      dispatch(search(query));
    },
    [query],
    400
  );

  const visibleTypes = ORDER.filter(
    (type) =>
      hasViewPermission(TYPE_CONFIG[type].permission) && results[type].length
  );
  const hasQuery = !!query.trim();
  const total = visibleTypes.reduce(
    (sum, type) => sum + results[type].length,
    0
  );

  const open = (result: SearchResult) => {
    // @ts-ignore route names are validated by the map above
    navigation.navigate(TYPE_CONFIG[result.type].route, { id: result.id });
  };

  return (
    <View
      style={[styles.container, { backgroundColor: theme.colors.background }]}
    >
      <Searchbar
        autoFocus
        placeholder={t('search_everything')}
        onChangeText={setQuery}
        value={query}
        style={{ backgroundColor: theme.colors.card }}
      />

      {!hasQuery ? (
        <EmptyState
          icon="magnify"
          title={t('search_everything')}
          description={t('global_search_hint')}
        />
      ) : loading && !total ? (
        <ListSkeleton count={4} />
      ) : error ? (
        <EmptyState
          variant="error"
          title={t('search_failed')}
          description={t('my_day_load_failed_description')}
          action={{ label: t('retry'), onPress: () => dispatch(search(query)) }}
        />
      ) : !total ? (
        <EmptyState
          icon="magnify-close"
          title={t('no_results')}
          description={t('no_results_description', { query: query.trim() })}
        />
      ) : (
        <ScrollView contentContainerStyle={styles.list}>
          {visibleTypes.map((type) => (
            <View key={type} style={styles.group}>
              <View style={styles.groupHeader}>
                <Icon
                  source={TYPE_CONFIG[type].icon}
                  size={18}
                  color={theme.colors.grey}
                />
                <Text
                  variant="titleSmall"
                  style={{
                    color: theme.colors.grey,
                    fontWeight: fontWeight.semibold
                  }}
                >
                  {t(TYPE_CONFIG[type].titleKey)}
                </Text>
              </View>
              {results[type].map((result) => (
                <Pressable
                  key={`${type}-${result.id}`}
                  accessibilityRole="button"
                  accessibilityLabel={`${result.title}${
                    result.subtitle ? `, ${result.subtitle}` : ''
                  }`}
                  onPress={() => open(result)}
                  style={({ pressed }) => [
                    styles.row,
                    raisedSurface(theme),
                    { opacity: pressed ? 0.7 : 1 }
                  ]}
                >
                  <View style={{ flex: 1 }}>
                    <Text
                      variant="bodyLarge"
                      numberOfLines={1}
                      style={{ color: theme.colors.text }}
                    >
                      {result.title}
                    </Text>
                    {!!result.subtitle && (
                      <Text
                        variant="bodySmall"
                        numberOfLines={1}
                        style={{ color: theme.colors.grey }}
                      >
                        {result.subtitle}
                      </Text>
                    )}
                  </View>
                  <Icon
                    source="chevron-right"
                    size={20}
                    color={theme.colors.grey}
                  />
                </Pressable>
              ))}
            </View>
          ))}
        </ScrollView>
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1
  },
  list: {
    paddingVertical: spacing.md,
    paddingBottom: 60
  },
  group: {
    marginBottom: spacing.lg
  },
  groupHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing.sm,
    marginHorizontal: spacing.lg,
    marginBottom: spacing.sm
  },
  row: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing.sm,
    borderRadius: radius.md,
    paddingHorizontal: spacing.lg,
    paddingVertical: spacing.md,
    marginHorizontal: spacing.lg,
    marginBottom: spacing.sm,
    minHeight: touchTarget.min
  }
});
