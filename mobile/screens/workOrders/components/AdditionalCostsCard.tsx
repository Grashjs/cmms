import { Fragment, useContext } from 'react';
import { Alert, StyleSheet } from 'react-native';
import {
  Button,
  Divider,
  IconButton,
  Text,
  useTheme
} from 'react-native-paper';
import { useTranslation } from 'react-i18next';
import { useDispatch } from '../../../store';
import { deleteAdditionalCost } from '../../../slices/additionalCost';
import { CustomSnackBarContext } from '../../../contexts/CustomSnackBarContext';
import { getErrorMessage } from '../../../utils/api';
import { PermissionEntity } from '../../../models/role';
import { PlanFeature } from '../../../models/subscriptionPlan';
import type { RootStackParamList } from '../../../types';
import type { NativeStackNavigationProp } from '@react-navigation/native-stack';
import type AdditionalCost from '../../../models/additionalCost';
import type WorkOrder from '../../../models/workOrder';
import { View } from '../../../components/Themed';

interface AdditionalCostsCardProps {
  additionalCosts: AdditionalCost[];
  workOrder: WorkOrder;
  workOrderId: number;
  hasEditPermission: (entity: PermissionEntity, data?: any) => boolean;
  hasFeature: (feature: PlanFeature) => boolean;
  getFormattedCurrency: (amount: number) => string;
  navigation: NativeStackNavigationProp<RootStackParamList>;
}

export default function AdditionalCostsCard({
  additionalCosts,
  workOrder,
  workOrderId,
  hasEditPermission,
  hasFeature,
  getFormattedCurrency,
  navigation
}: AdditionalCostsCardProps) {
  const { t } = useTranslation();
  const theme = useTheme();
  const dispatch = useDispatch();
  const { showSnackBar } = useContext(CustomSnackBarContext);

  const canEdit = hasEditPermission(PermissionEntity.WORK_ORDERS, workOrder);
  const canAdd = canEdit && hasFeature(PlanFeature.ADDITIONAL_COST);

  return (
    <View style={styles.shadowedCard}>
      <Text
        style={{
          marginBottom: 10,
          color: theme.colors.onSurfaceVariant
        }}
      >
        {t('additional_costs')}
      </Text>
      {!additionalCosts.length ? (
        <Text style={{ fontWeight: 'bold' }}>{t('no_additional_cost')}</Text>
      ) : (
        <View>
          {additionalCosts.map((cost) => (
            <View key={cost.id} style={styles.costRow}>
              <View style={styles.costInfo}>
                <Text style={{ fontWeight: 'bold' }} variant="bodyLarge">
                  {cost.description}
                </Text>
                {cost.assignedTo && (
                  <Text style={{ color: theme.colors.onSurfaceVariant }}>
                    {`${cost.assignedTo.firstName} ${cost.assignedTo.lastName}`}
                  </Text>
                )}
                <Text>{getFormattedCurrency(cost.cost)}</Text>
              </View>
              {canEdit && (
                <View style={styles.actions}>
                  <IconButton
                    icon="pencil"
                    size={20}
                    onPress={() =>
                      navigation.navigate('EditAdditionalCost', {
                        workOrderId,
                        additionalCost: cost
                      })
                    }
                  />
                  <IconButton
                    icon="delete"
                    size={20}
                    onPress={() =>
                      Alert.alert(t('confirmation'), t('confirm_delete_additional_cost'), [
                        { text: t('cancel'), style: 'cancel' },
                        {
                          text: t('to_delete'),
                          style: 'destructive',
                          onPress: () => {
                            dispatch(deleteAdditionalCost(workOrderId, cost.id))
                              .then(() => showSnackBar(t('operation_success'), 'success'))
                              .catch((err) => showSnackBar(getErrorMessage(err), 'error'));
                          }
                        }
                      ])
                    }
                  />
                </View>
              )}
            </View>
          ))}
          <Text style={{ fontWeight: 'bold' }} variant="bodyLarge">
            {t('total')}
          </Text>
          <Text>
            {getFormattedCurrency(
              additionalCosts.reduce(
                (acc, additionalCost) =>
                  additionalCost.includeToTotalCost
                    ? acc + additionalCost.cost
                    : acc,
                0
              )
            )}
          </Text>
        </View>
      )}
      {canAdd && (
        <Fragment>
          <Divider style={{ marginTop: 5 }} />
          <Button
            onPress={() =>
              navigation.navigate('AddAdditionalCost', {
                workOrderId
              })
            }
          >
            {t('add_additional_cost')}
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
  costRow: {
    display: 'flex',
    flexDirection: 'row',
    alignItems: 'center'
  },
  costInfo: {
    flex: 1
  },
  actions: {
    flexDirection: 'row',
    alignItems: 'center'
  }
});
