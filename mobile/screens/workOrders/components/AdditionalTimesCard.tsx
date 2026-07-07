import { Fragment, useContext, useState } from 'react';
import { StyleSheet } from 'react-native';
import {
  Button,
  Dialog,
  Divider,
  IconButton,
  List,
  Portal,
  Text,
  useTheme
} from 'react-native-paper';
import { useTranslation } from 'react-i18next';
import { useDispatch } from '../../../store';
import { deleteLabor } from '../../../slices/labor';
import { CustomSnackBarContext } from '../../../contexts/CustomSnackBarContext';
import { getErrorMessage } from '../../../utils/api';
import { getHoursAndMinutesAndSeconds } from '../../../utils/formatters';
import { PermissionEntity } from '../../../models/role';
import { PlanFeature } from '../../../models/subscriptionPlan';
import type { RootStackParamList } from '../../../types';
import type { NativeStackNavigationProp } from '@react-navigation/native-stack';
import type Labor from '../../../models/labor';
import type WorkOrder from '../../../models/workOrder';
import useAuth from '../../../hooks/useAuth';
import { View } from '../../../components/Themed';

interface AdditionalTimesCardProps {
  labors: Labor[];
  workOrder: WorkOrder;
  navigation: NativeStackNavigationProp<RootStackParamList>;
}

export default function AdditionalTimesCard({
  labors,
  workOrder,
  navigation
}: AdditionalTimesCardProps) {
  const { t } = useTranslation();
  const theme = useTheme();
  const dispatch = useDispatch();
  const { hasEditPermission, hasFeature } = useAuth();
  const { showSnackBar } = useContext(CustomSnackBarContext);
  const [deleteTarget, setDeleteTarget] = useState<Labor | null>(null);

  const handleDelete = () => {
    if (!deleteTarget) return;
    dispatch(deleteLabor(workOrder.id, deleteTarget.id))
      .then(() => showSnackBar(t('operation_success'), 'success'))
      .catch((err) => showSnackBar(getErrorMessage(err), 'error'))
      .finally(() => setDeleteTarget(null));
  };

  const canAddTime =
    hasEditPermission(PermissionEntity.WORK_ORDERS, workOrder) &&
    hasFeature(PlanFeature.ADDITIONAL_TIME);
  const unloggedLabors = labors.filter((labor) => !labor.logged);

  return (
    <View style={styles.shadowedCard}>
      <Portal>
        <Dialog
          visible={!!deleteTarget}
          onDismiss={() => setDeleteTarget(null)}
        >
          <Dialog.Title>{t('confirmation')}</Dialog.Title>
          <Dialog.Content>
            <Text variant="bodyMedium">
              {t('confirm_delete_additional_time')}
            </Text>
          </Dialog.Content>
          <Dialog.Actions>
            <Button onPress={() => setDeleteTarget(null)}>{t('cancel')}</Button>
            <Button onPress={handleDelete}>{t('to_delete')}</Button>
          </Dialog.Actions>
        </Dialog>
      </Portal>
      <Text
        style={{
          marginBottom: 10,
          color: theme.colors.onSurfaceVariant
        }}
      >
        {t('labors')}
      </Text>
      {!unloggedLabors.length ? (
        <Text style={{ fontWeight: 'bold' }}>{t('no_labor')}</Text>
      ) : (
        unloggedLabors.map((labor) => (
          <View key={labor.id} style={styles.laborRow}>
            <List.Item
              style={styles.laborInfo}
              titleStyle={{ fontSize: 14 }}
              title={
                labor.assignedTo
                  ? `${labor.assignedTo.firstName} ${labor.assignedTo.lastName}`
                  : t('not_assigned')
              }
              description={`${
                getHoursAndMinutesAndSeconds(labor.duration)[0]
              }h ${getHoursAndMinutesAndSeconds(labor.duration)[1]}m`}
            />
            {canAddTime && (
              <View style={styles.actions}>
                <IconButton
                  icon="pencil"
                  size={20}
                  onPress={() =>
                    navigation.navigate('EditAdditionalTime', {
                      workOrderId: workOrder.id,
                      labor
                    })
                  }
                />
                <IconButton
                  icon="delete"
                  size={20}
                  onPress={() => setDeleteTarget(labor)}
                />
              </View>
            )}
          </View>
        ))
      )}
      {canAddTime && (
        <Fragment>
          <Divider style={{ marginTop: 5 }} />
          <Button
            onPress={() =>
              navigation.navigate('AddAdditionalTime', {
                workOrderId: workOrder.id
              })
            }
          >
            {t('add_time')}
          </Button>
        </Fragment>
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  shadowedCard: {
    borderRadius: 10,
    paddingHorizontal: 10,
    paddingVertical: 10,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.2,
    marginVertical: 10,
    marginHorizontal: 5,
    elevation: 5
  },
  laborRow: {
    flexDirection: 'row',
    alignItems: 'center'
  },
  laborInfo: {
    flex: 1
  },
  actions: {
    flexDirection: 'row',
    alignItems: 'center'
  }
});
