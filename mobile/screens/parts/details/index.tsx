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
import { deletePart, getPartDetails } from '../../../slices/part';
import LoadingDialog from '../../../components/LoadingDialog';
import PartDetails from './PartDetails';
import { TabBar, TabView } from 'react-native-tab-view';
import PartWorkOrders from './PartWorkOrders';
import { CustomSnackBarContext } from '../../../contexts/CustomSnackBarContext';
import PartAssets from './PartAssets';
import PartFiles from './PartFiles';

export default function PartDetailsHome({
                                          navigation,
                                          route
                                        }: RootStackScreenProps<'PartDetails'>) {
  const { id, partProp } = route.params;

  const { t } = useTranslation();
  const { partInfos, loadingGet } = useSelector((state) => state.parts);
  const part = partInfos[id]?.part ?? partProp;
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
        return <PartDetails part={part} />;
      case 'work-orders':
        return <PartWorkOrders part={part} navigation={navigation} />;
      case 'assets':
        return <PartAssets part={part} navigation={navigation} />;
      case 'files':
        return <PartFiles part={part} />;
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
    if (!partProp)
      dispatch(getPartDetails(id));
  }, [partProp]);
  useEffect(() => {
    navigation.setOptions({
      title: part?.name ?? t('loading'),
      headerRight: () => (
        <Pressable
          onPress={() => {
            SheetManager.show('part-details-sheet', {
              payload: {
                onEdit: () => navigation.navigate('EditPart', { part: part }),
                onDelete: () => setOpenDelete(true),
                part
              }
            });
          }}
        >
          <IconButton icon='dots-vertical' />
        </Pressable>
      )
    });
  }, [part]);

  const onDeleteSuccess = () => {
    showSnackBar(t('part_delete_success'), 'success');
    navigation.goBack();
  };
  const onDeleteFailure = (err) =>
    showSnackBar(t('part_delete_failure'), 'error');

  const handleDelete = () => {
    dispatch(deletePart(part?.id)).then(onDeleteSuccess).catch(onDeleteFailure);
    setOpenDelete(false);
  };
  const renderConfirmDelete = () => {
    return (
      <Portal>
        <Dialog visible={openDelete} onDismiss={() => setOpenDelete(false)}>
          <Dialog.Title>{t('confirmation')}</Dialog.Title>
          <Dialog.Content>
            <Text variant='bodyMedium'>{t('confirm_delete_part')}</Text>
          </Dialog.Content>
          <Dialog.Actions>
            <Button onPress={() => setOpenDelete(false)}>{t('cancel')}</Button>
            <Button onPress={handleDelete}>{t('to_delete')}</Button>
          </Dialog.Actions>
        </Dialog>
      </Portal>
    );
  };
  if (part)
    return (
      <View
        style={{
          ...styles.container,
          backgroundColor: theme.colors.background
        }}
      >
        {renderConfirmDelete()}
        <TabView
          renderTabBar={renderTabBar}
          navigationState={{ index: tabIndex, routes: tabs }}
          renderScene={renderScene}
          onIndexChange={setTabIndex}
          initialLayout={{ width: layout.width }}
          style={{ backgroundColor: theme.colors.background }}
          sceneContainerStyle={{ backgroundColor: theme.colors.background }}
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
