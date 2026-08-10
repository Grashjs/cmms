import * as React from 'react';
import { useEffect } from 'react';
import { useDispatch, useSelector } from '../../../store';
import { useTranslation } from 'react-i18next';
import Location from '../../../models/location';
import {
  RefreshControl,
  ScrollView,
  StyleSheet,
  TouchableOpacity
} from 'react-native';
import { Divider, Text } from 'react-native-paper';
import { useAppTheme } from '../../../custom-theme';
import { View } from '../../../components/Themed';
import { getWorkOrdersByLocation } from '../../../slices/workOrder';
import { getStatusColor } from '../../../utils/overall';
import Tag from '../../../components/Tag';

export default function LocationWorkOrders({
                                             location,
                                             navigation
                                           }: {
  location: Location;
  navigation: any;
}) {
  const { t }: { t: any } = useTranslation();
  const dispatch = useDispatch();
  const theme = useAppTheme();
  const { workOrdersByLocation, loadingGet } = useSelector(
    (state) => state.workOrders
  );
  const workOrders = workOrdersByLocation[location.id] ?? [];

  useEffect(() => {
    if (location) dispatch(getWorkOrdersByLocation(location.id));
  }, [location]);

  return (
    <ScrollView
      style={{ ...styles.container, backgroundColor: theme.colors.background }}
      refreshControl={
        <RefreshControl
          refreshing={loadingGet}
          colors={[theme.colors.primary]}
        />
      }
    >
      {workOrders.map((workOrder) => (
        <TouchableOpacity
          key={workOrder.id}
          onPress={() => navigation.push('WODetails', { id: workOrder.id })}
        >
          <View
            style={{
              display: 'flex',
              flexDirection: 'row',
              justifyContent: 'space-between',
              padding: 20
            }}
          >
            <Text style={{ fontWeight: 'bold', flexShrink: 1 }}>{workOrder.title}</Text>
            <Tag
              text={t(workOrder.status)}
              color='white'
              backgroundColor={getStatusColor(workOrder.status, theme)}
            />
          </View>
          <Divider />
        </TouchableOpacity>
      ))}
      {!loadingGet && workOrders.length === 0 && (
        <View style={{ padding: 20 }}>
          <Text variant={'titleLarge'}>{t('no_wo_linked_location')}</Text>
        </View>
      )}
    </ScrollView>
  );
}
const styles = StyleSheet.create({
  container: {
    flex: 1
  }
});
