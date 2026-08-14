import * as React from 'react';
import { useCallback } from 'react';
import { StyleSheet, View } from 'react-native';
import WorkOrder from '../models/workOrder';
import { useAppTheme } from '../custom-theme';
import { EmptyState, PaginatedEntityList } from './ui';
import WorkOrderCard from '../screens/workOrders/components/WorkOrderCard';

interface NestedWorkOrdersListProps {
  workOrders: WorkOrder[];
  loading: boolean;
  onRefresh: () => void;
  emptyTitle: string;
  navigation: any;
}

/** Work orders linked to a parent asset, part, or location detail tab. */
export default function NestedWorkOrdersList({
  workOrders,
  loading,
  onRefresh,
  emptyTitle,
  navigation
}: NestedWorkOrdersListProps) {
  const theme = useAppTheme();
  const [refreshing, setRefreshing] = React.useState(false);

  const handleRefresh = () => {
    setRefreshing(true);
    onRefresh();
  };

  React.useEffect(() => {
    if (!loading) setRefreshing(false);
  }, [loading]);

  const renderItem = useCallback(
    ({ item }: { item: WorkOrder }) => (
      <WorkOrderCard
        workOrder={item}
        onPress={() => navigation.push('WODetails', { id: item.id })}
      />
    ),
    [navigation]
  );

  return (
    <View style={[styles.container, { backgroundColor: theme.colors.background }]}>
      <PaginatedEntityList
        data={workOrders}
        keyExtractor={(workOrder) => workOrder.id.toString()}
        renderItem={renderItem}
        loading={loading}
        refreshing={refreshing}
        onRefresh={handleRefresh}
        ListEmptyComponent={
          <EmptyState icon="clipboard-text-outline" title={emptyTitle} />
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
