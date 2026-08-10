import * as React from 'react';
import { useEffect } from 'react';
import { useDispatch, useSelector } from '../../../store';
import { useTranslation } from 'react-i18next';
import { AssetDTO } from '../../../models/asset';
import { getAssetWorkOrders } from '../../../slices/asset';
import NestedWorkOrdersList from '../../../components/NestedWorkOrdersList';

export default function AssetWorkOrders({
  asset,
  navigation
}: {
  asset: AssetDTO;
  navigation: any;
}) {
  const { t } = useTranslation();
  const { assetInfos, loadingWorkOrders } = useSelector((state) => state.assets);
  const workOrders = assetInfos[asset?.id]?.workOrders ?? [];
  const dispatch = useDispatch();

  useEffect(() => {
    if (asset) dispatch(getAssetWorkOrders(asset.id));
  }, [asset, dispatch]);

  return (
    <NestedWorkOrdersList
      workOrders={workOrders}
      loading={loadingWorkOrders}
      onRefresh={() => asset && dispatch(getAssetWorkOrders(asset.id))}
      emptyTitle={t('no_wo_linked_asset')}
      navigation={navigation}
    />
  );
}
