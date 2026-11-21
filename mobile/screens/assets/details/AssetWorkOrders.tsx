import * as React from 'react';
import { useEffect, useState } from 'react';
import { useDispatch, useSelector } from '../../../store';
import { useTranslation } from 'react-i18next';
import { AssetDTO } from '../../../models/asset';
import { getAssetWorkOrders } from '../../../slices/asset';
import {
  RefreshControl,
  ScrollView,
  StyleSheet,
  TouchableOpacity
} from 'react-native';
import { Divider, Text, useTheme } from 'react-native-paper';
import { View } from '../../../components/Themed';
import Tag from '../../../components/Tag';
import { getStatusColor } from '../../../utils/overall';
import { useOffline } from '../../../offline/OfflineProvider';
import type { OfflineAsset } from '../../../offline/types';
import type WorkOrder from '../../../models/workOrder';
import {
  assertSessionGeneration,
  sessionGeneration
} from '../../../offline/sessionBridge';

export default function AssetWorkOrders({
  asset,
  navigation,
  offlineAsset
}: {
  asset?: AssetDTO;
  navigation: any;
  offlineAsset?: OfflineAsset;
}) {
  const { t }: { t: any } = useTranslation();
  const { assetInfos, loadingWorkOrders } = useSelector(
    (state) => state.assets
  );
  const offline = useOffline();
  const [localWorkOrders, setLocalWorkOrders] = useState<{
    repository: typeof offline.repository;
    assetId: number;
    values: WorkOrder[];
  } | null>(null);
  const workOrders = offlineAsset
    ? offline.readable &&
      localWorkOrders?.repository === offline.repository &&
      localWorkOrders?.assetId === offlineAsset.id
      ? localWorkOrders.values.filter((item) =>
          offline.prepared.some(
            (prepared) => prepared.id === item.id && prepared.available
          )
        )
      : []
    : assetInfos[asset?.id]?.workOrders ?? [];
  const dispatch = useDispatch();
  const theme = useTheme();

  useEffect(() => {
    if (asset && !offlineAsset) dispatch(getAssetWorkOrders(asset.id));
  }, [asset, offlineAsset]);

  useEffect(() => {
    const repository = offline.repository;
    if (!offlineAsset || !repository || !offline.readable) {
      setLocalWorkOrders(null);
      return;
    }
    let active = true;
    const generation = sessionGeneration();
    void repository
      .getPreparedAssetWorkOrders(offlineAsset.id)
      .then((values) => {
        assertSessionGeneration(generation);
        if (active)
          setLocalWorkOrders({ repository, assetId: offlineAsset.id, values });
      })
      .catch(() => {
        if (active) setLocalWorkOrders(null);
      });
    return () => {
      active = false;
    };
  }, [
    offline.repository,
    offline.readable,
    offline.prepared,
    offlineAsset?.id
  ]);

  return (
    <ScrollView
      style={{ ...styles.container, backgroundColor: theme.colors.background }}
      refreshControl={
        offlineAsset ? undefined : (
          <RefreshControl
            refreshing={loadingWorkOrders}
            colors={[theme.colors.primary]}
          />
        )
      }
    >
      {offlineAsset && (
        <Text style={{ padding: 20 }}>
          {t('offline_asset_prepared_work_orders')}
        </Text>
      )}
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
            <Text style={{ fontWeight: 'bold', marginRight: 5, flexShrink: 1 }}>
              {workOrder.title}
            </Text>
            <Tag
              text={t(workOrder.status)}
              color="white"
              backgroundColor={getStatusColor(workOrder.status, theme)}
            />
          </View>
          <Divider />
        </TouchableOpacity>
      ))}
      {(offlineAsset || !loadingWorkOrders) && workOrders.length === 0 && (
        <View style={{ padding: 20 }}>
          <Text variant={'titleLarge'}>{t('no_wo_linked_asset')}</Text>
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
