import { Pressable, StyleSheet, useWindowDimensions } from 'react-native';

import { View } from '../../../components/Themed';
import { RootStackScreenProps } from '../../../types';
import { useDispatch, useSelector } from '../../../store';
import { useTranslation } from 'react-i18next';
import * as React from 'react';
import { useContext, useEffect, useState } from 'react';
import {
  Button,
  Dialog,
  IconButton,
  Portal,
  Text,
  useTheme
} from 'react-native-paper';
import { SheetManager } from 'react-native-actions-sheet';
import { deleteAsset, getAssetDetails } from '../../../slices/asset';
import LoadingDialog from '../../../components/LoadingDialog';
import AssetDetails from './AssetDetails';
import { TabBar, TabView } from 'react-native-tab-view';
import AssetWorkOrders from './AssetWorkOrders';
import AssetFiles from './AssetFiles';
import AssetParts from './AssetParts';
import { CustomSnackBarContext } from '../../../contexts/CustomSnackBarContext';
import { useOffline } from '../../../offline/OfflineProvider';
import { useOfflineAsset } from '../../../offline/useOfflineAsset';

export default function AssetDetailsHome({
  navigation,
  route
}: RootStackScreenProps<'AssetDetails'>) {
  const { id, assetProp } = route.params;

  const { t } = useTranslation();
  const offline = useOffline();
  const localOnly = offline.enabled && !offline.networkAvailable;
  const local = useOfflineAsset(id);
  const { assetInfos, loadingGet } = useSelector((state) => state.assets);
  const asset = localOnly ? null : assetInfos[id]?.asset ?? assetProp;
  const dispatch = useDispatch();
  const theme = useTheme();
  const layout = useWindowDimensions();
  const [tabIndex, setTabIndex] = useState(0);
  const [openDelete, setOpenDelete] = useState<boolean>(false);
  const { showSnackBar } = useContext(CustomSnackBarContext);
  const [tabs] = useState([
    { key: 'details', title: t('details') },
    { key: 'work-orders', title: t('work_orders') },
    { key: 'files', title: t('files') },
    { key: 'parts', title: t('parts') }
  ]);
  const renderScene = ({ route, jumpTo }) => {
    if (localOnly) {
      if (!local.asset) return null;
      if (route.key === 'details')
        return (
          <AssetDetails offlineAsset={local.asset} navigation={navigation} />
        );
      if (route.key === 'work-orders')
        return (
          <AssetWorkOrders offlineAsset={local.asset} navigation={navigation} />
        );
      return (
        <View style={{ padding: 20 }}>
          <Text>{t('offline_connected_required')}</Text>
        </View>
      );
    }
    switch (route.key) {
      case 'details':
        return <AssetDetails asset={asset} navigation={navigation} />;
      case 'work-orders':
        return <AssetWorkOrders asset={asset} navigation={navigation} />;
      case 'files':
        return <AssetFiles asset={asset} />;
      case 'parts':
        return <AssetParts asset={asset} navigation={navigation} />;
    }
  };
  const renderTabBar = (props) => (
    <TabBar
      {...props}
      scrollEnabled
      indicatorStyle={{ backgroundColor: 'white' }}
      style={{ backgroundColor: theme.colors.primary }}
    />
  );

  useEffect(() => {
    if (!localOnly && !assetProp) dispatch(getAssetDetails(id));
  }, [assetProp, id, localOnly]);
  useEffect(() => {
    navigation.setOptions({
      title: (localOnly ? local.asset?.name : asset?.name) ?? t('loading'),
      headerRight: () =>
        localOnly ? null : (
          <Pressable
            onPress={() => {
              SheetManager.show('asset-details-sheet', {
                payload: {
                  onEdit: () => navigation.navigate('EditAsset', { asset }),
                  onDelete: () => setOpenDelete(true),
                  onCreateWorkOrder: () =>
                    navigation.push('AddWorkOrder', { asset }),
                  onCreateChildAsset: () =>
                    navigation.push('AddAsset', { parentAsset: asset }),
                  asset
                }
              });
            }}
          >
            <IconButton icon="dots-vertical" />
          </Pressable>
        )
    });
    if (localOnly) {
      setOpenDelete(false);
      void SheetManager.hide('asset-details-sheet');
    }
  }, [asset, localOnly, local.asset]);

  const onDeleteSuccess = () => {
    showSnackBar(t('asset_remove_success'), 'success');
    navigation.goBack();
  };
  const onDeleteFailure = (err) =>
    showSnackBar(t('asset_remove_failure'), 'error');

  const handleDelete = () => {
    if (localOnly) return;
    dispatch(deleteAsset(asset?.id))
      .then(onDeleteSuccess)
      .catch(onDeleteFailure);
    setOpenDelete(false);
  };
  const renderConfirmDelete = () => {
    return (
      <Portal>
        <Dialog visible={openDelete} onDismiss={() => setOpenDelete(false)}>
          <Dialog.Title>{t('confirmation')}</Dialog.Title>
          <Dialog.Content>
            <Text variant="bodyMedium">{t('confirm_delete_asset')}</Text>
          </Dialog.Content>
          <Dialog.Actions>
            <Button onPress={() => setOpenDelete(false)}>{t('cancel')}</Button>
            <Button onPress={handleDelete}>{t('to_delete')}</Button>
          </Dialog.Actions>
        </Dialog>
      </Portal>
    );
  };
  if (asset || (localOnly && local.asset))
    return (
      <View style={styles.container}>
        {!localOnly && renderConfirmDelete()}
        <TabView
          renderTabBar={renderTabBar}
          navigationState={{ index: tabIndex, routes: tabs }}
          renderScene={renderScene}
          onIndexChange={setTabIndex}
          initialLayout={{ width: layout.width }}
        />
      </View>
    );
  else if (localOnly && !local.loading)
    return (
      <View style={{ padding: 20 }}>
        <Text>{t('offline_asset_not_available')}</Text>
      </View>
    );
  else return <LoadingDialog visible={true} />;
}

const styles = StyleSheet.create({
  container: {
    flex: 1
  }
});
