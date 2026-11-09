import { Fragment, useContext } from 'react';
import { Alert, StyleSheet } from 'react-native';
import {
  Button,
  Divider,
  IconButton,
  List,
  Text,
  useTheme
} from 'react-native-paper';
import { useTranslation } from 'react-i18next';
import { useDispatch } from '../../../store';
import { deleteLabor, getLabors } from '../../../slices/labor';
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
import { useOffline } from '../../../offline/OfflineProvider';
import {
  assertSessionGeneration,
  sessionGeneration
} from '../../../offline/sessionBridge';

interface AdditionalTimesCardProps {
  connectedAvailable?: boolean;
  prepared?: boolean;
  canCreatePersonal?: boolean;
  labors: Labor[];
  workOrder: WorkOrder;
  navigation: NativeStackNavigationProp<RootStackParamList>;
}

export default function AdditionalTimesCard({
  labors,
  workOrder,
  navigation,
  connectedAvailable = true,
  prepared = false,
  canCreatePersonal = false
}: AdditionalTimesCardProps) {
  const { t } = useTranslation();
  const theme = useTheme();
  const offline = useOffline();
  const dispatch = useDispatch();
  const { hasEditPermission, hasFeature } = useAuth();
  const { showSnackBar } = useContext(CustomSnackBarContext);

  const canAddTime =
    hasEditPermission(PermissionEntity.WORK_ORDERS, workOrder) &&
    hasFeature(PlanFeature.ADDITIONAL_TIME);
  const unloggedLabors = labors.filter((labor) => !labor.logged);

  return (
    <View style={styles.shadowedCard}>
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
                  disabled={!connectedAvailable}
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
                  disabled={!connectedAvailable}
                  icon="delete"
                  size={20}
                  onPress={() =>
                    Alert.alert(
                      t('confirmation'),
                      t('confirm_delete_additional_time'),
                      [
                        { text: t('cancel'), style: 'cancel' },
                        {
                          text: t('to_delete'),
                          style: 'destructive',
                          onPress: async () => {
                            const generation = sessionGeneration();
                            try {
                              const result = await dispatch(
                                deleteLabor(
                                  workOrder.id,
                                  labor.id,
                                  labor.version
                                )
                              );
                              assertSessionGeneration(generation);
                              showSnackBar(
                                t(
                                  result?.localUpdateError
                                    ? 'offline_time_saved_refresh_pending'
                                    : 'operation_success'
                                ),
                                result?.localUpdateError ? 'error' : 'success'
                              );
                            } catch (error) {
                              if (generation !== sessionGeneration()) return;
                              showSnackBar(getErrorMessage(error), 'error');
                              // Refresh for a new, explicit confirmation; never retry the delete.
                              if (error?.message === 'conflict_error') {
                                if (prepared)
                                  void offline
                                    .refresh(workOrder.id)
                                    .catch(offline.reportError);
                                else void dispatch(getLabors(workOrder.id));
                              }
                            }
                          }
                        }
                      ]
                    )
                  }
                />
              </View>
            )}
          </View>
        ))
      )}
      {(prepared ? canCreatePersonal && offline.writable : canAddTime) && (
        <Fragment>
          <Divider style={{ marginTop: 5 }} />
          <Button
            onPress={() =>
              navigation.navigate('AddAdditionalTime', {
                workOrderId: workOrder.id
              })
            }
          >
            {t(prepared ? 'offline_time_personal' : 'add_time')}
          </Button>
        </Fragment>
      )}
      {prepared && canAddTime && connectedAvailable && (
        <Button
          onPress={() =>
            navigation.navigate('AddAdditionalTime', {
              workOrderId: workOrder.id,
              connectedOnly: true
            })
          }
        >
          {t('offline_time_connected_form')}
        </Button>
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
