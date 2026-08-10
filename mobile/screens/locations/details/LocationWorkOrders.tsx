import * as React from 'react';
import { useEffect } from 'react';
import { useDispatch, useSelector } from '../../../store';
import { useTranslation } from 'react-i18next';
import Location from '../../../models/location';
import { getWorkOrdersByLocation } from '../../../slices/workOrder';
import NestedWorkOrdersList from '../../../components/NestedWorkOrdersList';

export default function LocationWorkOrders({
  location,
  navigation
}: {
  location: Location;
  navigation: any;
}) {
  const { t } = useTranslation();
  const dispatch = useDispatch();
  const { workOrdersByLocation, loadingGet } = useSelector(
    (state) => state.workOrders
  );
  const workOrders = workOrdersByLocation[location.id] ?? [];

  useEffect(() => {
    if (location) dispatch(getWorkOrdersByLocation(location.id));
  }, [dispatch, location]);

  return (
    <NestedWorkOrdersList
      workOrders={workOrders}
      loading={loadingGet}
      onRefresh={() => location && dispatch(getWorkOrdersByLocation(location.id))}
      emptyTitle={t('no_wo_linked_location')}
      navigation={navigation}
    />
  );
}
