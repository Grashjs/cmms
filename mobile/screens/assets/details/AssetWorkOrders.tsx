import * as React from 'react';
import { useEffect } from 'react';
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
import { Divider, Text } from 'react-native-paper';
import { useAppTheme } from '../../../custom-theme';
import { View } from '../../../components/Themed';
import Tag from '../../../components/Tag';
import { getStatusColor } from '../../../utils/overall';

export default function AssetWorkOrders({
                                          asset,
                                          navigation
                                        }: {
  asset: AssetDTO;
  navigation: any;
}) {
  const { t }: { t: any } = useTranslation();
  const { assetInfos, loadingWorkOrders } = useSelector(
    (state) => state.assets
  );
  const workOrders = assetInfos[asset?.id]?.workOrders ?? [];
  const dispatch = useDispatch();
  const theme = useAppTheme();

  useEffect(() => {
    if (asset) dispatch(getAssetWorkOrders(asset.id));
  }, [asset]);

  return (
    <ScrollView
      style={{ ...styles.container, backgroundColor: theme.colors.background }}
      refreshControl={
        <RefreshControl
          refreshing={loadingWorkOrders}
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
            <Text style={{ fontWeight: 'bold', marginRight: 5, flexShrink: 1 }}>
              {workOrder.title}
            </Text>
            <Tag
              text={t(workOrder.status)}
              color='white'
              backgroundColor={getStatusColor(workOrder.status, theme)}
            />
          </View>
          <Divider />
        </TouchableOpacity>
      ))}
      {!loadingWorkOrders && workOrders.length === 0 && (
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
