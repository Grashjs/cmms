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

export default function AssetDetailsHome({
                                           navigation,
                                           route
                                         }: RootStackScreenProps<'AssetDetails'>) {
  const { id, assetProp } = route.params;

  const { t } = useTranslation();
  const { assetInfos, loadingGet } = useSelector((state) => state.assets);
  const asset = assetInfos[id]?.asset ?? assetProp;
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
      indicatorStyle={{ backgroundColor: theme.colors.onPrimary }}
      style={{ backgroundColor: theme.colors.primary }}
    />
  );

  useEffect(() => {
    if (!assetProp)
      dispatch(getAssetDetails(id));
  }, [assetProp]);
  useEffect(() => {
    navigation.setOptions({
      title: asset?.name ?? t('loading'),
      headerRight: () => (
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
          <IconButton icon='dots-vertical' />
        </Pressable>
      )
    });
  }, [asset]);

  const onDeleteSuccess = () => {
    showSnackBar(t('asset_remove_success'), 'success');
    navigation.goBack();
  };
  const onDeleteFailure = (err) =>
    showSnackBar(t('asset_remove_failure'), 'error');

  const handleDelete = () => {
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
            <Text variant='bodyMedium'>{t('confirm_delete_asset')}</Text>
          </Dialog.Content>
          <Dialog.Actions>
            <Button onPress={() => setOpenDelete(false)}>{t('cancel')}</Button>
            <Button onPress={handleDelete}>{t('to_delete')}</Button>
          </Dialog.Actions>
        </Dialog>
      </Portal>
    );
  };
  if (asset)
    return (
      <View style={styles.container}>
        {renderConfirmDelete()}
        <TabView
          renderTabBar={renderTabBar}
          navigationState={{ index: tabIndex, routes: tabs }}
          renderScene={renderScene}
          onIndexChange={setTabIndex}
          initialLayout={{ width: layout.width }}
        />
      </View>
    );
  else return <LoadingDialog visible={true} />;
}

const styles = StyleSheet.create({
  container: {
    flex: 1
  }
});
