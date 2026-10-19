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
import { deleteLocation, getLocationDetails } from '../../../slices/location';
import LoadingDialog from '../../../components/LoadingDialog';
import LocationDetails from './LocationDetails';
import { TabBar, TabView } from 'react-native-tab-view';
import LocationWorkOrders from './LocationWorkOrders';
import { CustomSnackBarContext } from '../../../contexts/CustomSnackBarContext';
import LocationAssets from './LocationAssets';
import LocationFiles from './LocationFiles';

export default function LocationDetailsHome({
                                              navigation,
                                              route
                                            }: RootStackScreenProps<'LocationDetails'>) {
  const { id, locationProp } = route.params;

  const { t } = useTranslation();
  const { locationInfos, loadingGet } = useSelector((state) => state.locations);
  const location = locationInfos[id]?.location ?? locationProp;
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
    { key: 'assets', title: t('assets') }
  ]);
  const renderScene = ({ route, jumpTo }) => {
    switch (route.key) {
      case 'details':
        return <LocationDetails location={location} />;
      case 'work-orders':
        return (
          <LocationWorkOrders location={location} navigation={navigation} />
        );
      case 'assets':
        return <LocationAssets location={location} navigation={navigation} />;
      case 'files':
        return <LocationFiles location={location} />;
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
    if (!locationProp)
      dispatch(getLocationDetails(id));
  }, [locationProp]);
  useEffect(() => {
    navigation.setOptions({
      title: location?.name ?? t('loading'),
      headerRight: () => (
        <Pressable
          onPress={() => {
            SheetManager.show('location-details-sheet', {
              payload: {
                onEdit: () => navigation.navigate('EditLocation', { location }),
                onDelete: () => setOpenDelete(true),
                onCreateWorkOrder: () =>
                  navigation.push('AddWorkOrder', { location }),
                onCreateAsset: () => navigation.push('AddAsset', { location }),
                location
              }
            });
          }}
        >
          <IconButton icon='dots-vertical' />
        </Pressable>
      )
    });
  }, [location]);

  const onDeleteSuccess = () => {
    showSnackBar(t('location_delete_success'), 'success');
    navigation.goBack();
  };
  const onDeleteFailure = (err) =>
    showSnackBar(t('location_delete_failure'), 'error');

  const handleDelete = () => {
    dispatch(deleteLocation(location?.id))
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
            <Text variant='bodyMedium'>{t('confirm_delete_location')}</Text>
          </Dialog.Content>
          <Dialog.Actions>
            <Button onPress={() => setOpenDelete(false)}>{t('cancel')}</Button>
            <Button onPress={handleDelete}>{t('to_delete')}</Button>
          </Dialog.Actions>
        </Dialog>
      </Portal>
    );
  };
  if (location)
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
