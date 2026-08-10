import * as React from 'react';
import { useEffect } from 'react';
import { useDispatch, useSelector } from '../../../store';
import { useTranslation } from 'react-i18next';
import Part from '../../../models/part';
import { getWorkOrdersByPart } from '../../../slices/workOrder';
import NestedWorkOrdersList from '../../../components/NestedWorkOrdersList';

export default function PartWorkOrders({
  part,
  navigation
}: {
  part: Part;
  navigation: any;
}) {
  const { t } = useTranslation();
  const dispatch = useDispatch();
  const { workOrdersByPart, loadingGet } = useSelector((state) => state.workOrders);
  const workOrders = workOrdersByPart[part.id] ?? [];

  useEffect(() => {
    if (part) dispatch(getWorkOrdersByPart(part.id));
  }, [dispatch, part]);

  return (
    <NestedWorkOrdersList
      workOrders={workOrders}
      loading={loadingGet}
      onRefresh={() => part && dispatch(getWorkOrdersByPart(part.id))}
      emptyTitle={t('no_wo_linked_part')}
      navigation={navigation}
    />
  );
}
