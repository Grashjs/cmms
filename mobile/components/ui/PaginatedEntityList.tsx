import * as React from 'react';
import { ReactElement } from 'react';
import {
  ActivityIndicator,
  FlatList,
  FlatListProps,
  RefreshControl,
  StyleSheet,
  ViewStyle
} from 'react-native';
import { useAppTheme } from '../../custom-theme';
import { spacing } from '../../theme/tokens';
import ListSkeleton from './ListSkeleton';

export interface PaginatedEntityListProps<T> {
  data: T[];
  keyExtractor: (item: T) => string;
  renderItem: FlatListProps<T>['renderItem'];
  loading: boolean;
  refreshing?: boolean;
  onRefresh?: () => void;
  onEndReached?: () => void;
  loadingMore?: boolean;
  ListHeaderComponent?: ReactElement | null;
  ListEmptyComponent?: ReactElement | null;
  contentContainerStyle?: ViewStyle;
}

/**
 * FlatList wrapper shared by entity index screens. Shows a skeleton on first
 * load, a pull-to-refresh spinner only when the user asked for it, and a footer
 * loader while the next page is in flight.
 */
export default function PaginatedEntityList<T>({
  data,
  keyExtractor,
  renderItem,
  loading,
  refreshing = loading,
  onRefresh,
  onEndReached,
  loadingMore,
  ListHeaderComponent,
  ListEmptyComponent,
  contentContainerStyle
}: PaginatedEntityListProps<T>) {
  const theme = useAppTheme();
  const isInitialLoad = loading && !data.length;

  if (isInitialLoad) {
    return (
      <>
        {ListHeaderComponent}
        <ListSkeleton />
      </>
    );
  }

  return (
    <FlatList
      data={data}
      keyExtractor={keyExtractor}
      renderItem={renderItem}
      ListHeaderComponent={ListHeaderComponent}
      ListEmptyComponent={ListEmptyComponent ?? undefined}
      contentContainerStyle={[styles.content, contentContainerStyle]}
      onEndReached={onEndReached}
      onEndReachedThreshold={0.4}
      refreshControl={
        onRefresh ? (
          <RefreshControl
            refreshing={refreshing}
            onRefresh={onRefresh}
            colors={[theme.colors.primary]}
            tintColor={theme.colors.primary}
          />
        ) : undefined
      }
      ListFooterComponent={
        loadingMore ? (
          <ActivityIndicator
            style={styles.footerLoader}
            color={theme.colors.primary}
          />
        ) : null
      }
    />
  );
}

const styles = StyleSheet.create({
  content: {
    paddingTop: spacing.xs,
    paddingBottom: 100,
    flexGrow: 1
  },
  footerLoader: {
    marginVertical: spacing.lg
  }
});
