import {
  Alert,
  Image,
  Linking,
  PermissionsAndroid,
  Platform,
  Pressable,
  RefreshControl,
  ScrollView,
  StyleSheet,
  TouchableOpacity
} from 'react-native';
import { View } from '../../components/Themed';
import { RootStackParamList, RootStackScreenProps } from '../../types';
import {
  Button,
  Dialog,
  Divider,
  FAB,
  IconButton,
  List,
  Portal,
  ProgressBar,
  Provider,
  Text,
  useTheme
} from 'react-native-paper';
import Dropdown from 'react-native-dropdown-picker';
import { useTranslation } from 'react-i18next';
import * as React from 'react';
import { Fragment, useContext, useEffect, useState } from 'react';
import { CompanySettingsContext } from '../../contexts/CompanySettingsContext';
import Tag from '../../components/Tag';
import { getPriorityColor } from '../../utils/overall';
import { PermissionEntity } from '../../models/role';
import useAuth from '../../hooks/useAuth';
import { controlTimer, getLabors } from '../../slices/labor';
import { useDispatch, useSelector } from '../../store';
import { durationToHours, getHoursAndMinutesAndSeconds } from '../../utils/formatters';
import { editWOPartQuantities, getPartQuantitiesByWorkOrder } from '../../slices/partQuantity';
import { getAdditionalCosts } from '../../slices/additionalCost';
import { getRelations } from '../../slices/relation';
import Relation, { relationTypes } from '../../models/relation';
import { getTasks } from '../../slices/task';
import { CustomSnackBarContext } from '../../contexts/CustomSnackBarContext';
import {
  changeWorkOrderStatus,
  deleteWorkOrder,
  editWorkOrder,
  getPDFReport,
  getWorkOrderDetails
} from '../../slices/workOrder';
import { PlanFeature } from '../../models/subscriptionPlan';
import PartQuantities from '../../components/PartQuantities';
import { SheetManager } from 'react-native-actions-sheet';
import LoadingDialog from '../../components/LoadingDialog';
import WorkOrder from '../../models/workOrder';
import { DownloadDirectoryPath, downloadFile, DownloadFileOptions } from 'react-native-fs';
import Labor from '../../models/labor';
import { AudioPlayer } from '../../components/AudioPlayer';

export default function WODetailsScreen({
                                          navigation,
                                          route
                                        }: RootStackScreenProps<'WODetails'>) {
  const { id, workOrderProp } = route.params;
  const { workOrderInfos, loadingGet } = useSelector(
    (state) => state.workOrders
  );
  const workOrder = workOrderInfos[id]?.workOrder ?? workOrderProp;
  const { t } = useTranslation();
  const [openDropDown, setOpenDropDown] = useState<boolean>(false);
  const [dropDownValue, setDropdownValue] = useState<string>(
    workOrder?.status ?? ''
  );
  const {
    hasEditPermission, user, companySettings, hasFeature,
    hasViewPermission
  } = useAuth();
  const { showSnackBar } = useContext(CustomSnackBarContext);
  const [runningTimerDuration, setRunningTimerDuration] = useState<string>();
  const { workOrderConfiguration, generalPreferences } = companySettings;
  const [loading, setLoading] = useState<boolean>(false);
  const theme = useTheme();
  const dispatch = useDispatch();
  const { partQuantitiesByWorkOrder, loadingPartQuantities } = useSelector(
    (state) => state.partQuantities
  );
  const partQuantities = partQuantitiesByWorkOrder[id] ?? [];
  const { workOrderHistories } = useSelector(
    (state) => state.workOrderHistories
  );
  const { relationsByWorkOrder, loadingRelations } = useSelector(
    (state) => state.relations
  );
  const { tasksByWorkOrder, loadingTasks } = useSelector(
    (state) => state.tasks
  );
  const tasks = tasksByWorkOrder[id] ?? [];
  const currentWorkOrderHistories = workOrderHistories[id] ?? [];
  const currentWorkOrderRelations = relationsByWorkOrder[id] ?? [];
  const { costsByWorkOrder, loadingCosts } = useSelector(
    (state) => state.additionalCosts
  );
  const { timesByWorkOrder, loadingLabors } = useSelector(
    (state) => state.labors
  );
  const labors = timesByWorkOrder[id] ?? [];
  const primaryTime = labors.find(
    (labor) => labor.logged && labor.assignedTo.id === user.id
  );
  const additionalCosts = costsByWorkOrder[id] ?? [];
  const runningTimer = primaryTime?.status === 'RUNNING';
  const [controllingTime, setControllingTime] = useState<boolean>(false);
  const { getFormattedDate, getUserNameById, getFormattedCurrency } =
    useContext(CompanySettingsContext);
  const [isExtended, setIsExtended] = React.useState(true);
  const statuses = ['OPEN', 'ON_HOLD', 'IN_PROGRESS', 'COMPLETE'].map(
    (status) => ({ value: status, label: t(status) })
  );
  const [openDelete, setOpenDelete] = React.useState(false);
  const [openArchive, setOpenArchive] = React.useState(false);
  const loadingDetails =
    loadingPartQuantities[id] ||
    loadingTasks[id] ||
    loadingCosts[id] ||
    loadingLabors[id] ||
    loadingRelations[id];
  const fieldsToRender: {
    label: string;
    value: string | number;
  }[] = [
    {
      label: t('description'),
      value: workOrder?.description
    },
    {
      label: t('due_date'),
      value: getFormattedDate(workOrder?.dueDate)
    },
    {
      label: t('estimated_start_date'),
      value: getFormattedDate(workOrder?.estimatedStartDate)
    },
    {
      label: t('estimated_duration'),
      value: !!workOrder?.estimatedDuration
        ? t('estimated_hours_in_text', { hours: workOrder?.estimatedDuration })
        : null
    },
    {
      label: t('category'),
      value: workOrder?.category?.name
    },
    {
      label: t('created_at'),
      value: getFormattedDate(workOrder?.createdAt)
    }
  ];
  const touchableFields: {
    label: string;
    value: string | number;
    link: { route: keyof RootStackParamList; id: number };
    permissionEntity: PermissionEntity;
    address?: string;
  }[] = [
    {
      label: t('asset'),
      value: workOrder?.asset?.name,
      link: { route: 'AssetDetails', id: workOrder?.asset?.id },
      permissionEntity: PermissionEntity.ASSETS
    },
    {
      label: t('location'),
      value: workOrder?.location?.name,
      link: { route: 'LocationDetails', id: workOrder?.location?.id },
      permissionEntity: PermissionEntity.LOCATIONS,
      address: workOrder?.location?.address
    },
    {
      label: t('team'),
      value: workOrder?.team?.name,
      link: { route: 'TeamDetails', id: workOrder?.team?.id },
      permissionEntity: PermissionEntity.PEOPLE_AND_TEAMS
    },
    {
      label: t('primary_worker'),
      value: workOrder?.primaryUser
        ? `${workOrder.primaryUser.firstName} ${workOrder.primaryUser.lastName}`
        : null,
      link: { route: 'UserDetails', id: workOrder?.primaryUser?.id },
      permissionEntity: PermissionEntity.PEOPLE_AND_TEAMS
    }
  ];
  const getInfos = () => {
    if (!workOrderProp)
      dispatch(getWorkOrderDetails(id));
    if (!generalPreferences.simplifiedWorkOrder) {
      dispatch(getPartQuantitiesByWorkOrder(id));
      dispatch(getLabors(id));
      dispatch(getAdditionalCosts(id));
      dispatch(getRelations(id));
    }
    dispatch(getTasks(id));
  };
  useEffect(() => {
    navigation.setOptions({
      headerRight: () =>
        workOrder &&
        !loadingTasks[id] && (
          <Pressable
            onPress={() => {
              SheetManager.show('work-order-details-sheet', {
                payload: {
                  onEdit: () =>
                    navigation.navigate('EditWorkOrder', { workOrder, tasks }),
                  onOpenArchive: () => {
                    setOpenArchive(true);
                  },
                  onDelete: () => {
                    setOpenDelete(true);
                  },
                  onGenerateReport,
                  workOrder
                }
              });
            }}
          >
            <IconButton icon='dots-vertical' />
          </Pressable>
        )
    });
    //LogBox.ignoreLogs(['VirtualizedLists should never be nested']);
  }, [loadingTasks, workOrder, tasks]);

  useEffect(() => {
    getInfos();
  }, [workOrderProp]);
  useEffect(() => {
    let intervalId;

    // Function to update timer duration every minute
    if (primaryTime?.status === 'RUNNING') {
      const updateTimerDuration = () => {
        // Calculate new duration here
        const newDuration = getRunningTimerDuration(primaryTime);
        setRunningTimerDuration(newDuration);
      };
      updateTimerDuration();
      // Update timer duration every minute
      intervalId = setInterval(updateTimerDuration, 1000);
    }
    // Cleanup function
    return () => {
      if (intervalId) clearInterval(intervalId);
      setRunningTimerDuration('0:00');
    };
  }, [primaryTime, runningTimer]); // Run effect whenever runningTimer changes

  const actualDownload = (uri: string): Promise<any> => {
    const fileName = workOrder.title;
    //Define path to store file along with the extension
    const path = `${DownloadDirectoryPath}/${fileName}.pdf`;
    //Define options
    const options: DownloadFileOptions = {
      fromUrl: uri,
      toFile: path
    };
    //Call downloadFile
    const response = downloadFile(options);
    return response.promise.then(async (res) => {
      //Transform response
      if (res && res.statusCode === 200 && res.bytesWritten > 0) {
        Linking.openURL(uri);
      } else {
        console.log(res);
      }
    });
  };
  const getRunningTimerDuration = (labor: Labor) => {
    return durationToHours(labor.duration + (new Date().getTime() - new Date(labor.startedAt).getTime()) / 1000);
  };
  const onDeleteSuccess = () => {
    showSnackBar(t('wo_delete_success'), 'success');
    navigation.goBack();
  };
  const onArchiveSuccess = () => {
    showSnackBar(t('wo_archive_success'), 'success');
    navigation.goBack();
  };
  const onArchiveFailure = (err) =>
    showSnackBar(t('wo_archive_failure'), 'error');
  const onDeleteFailure = (err) =>
    showSnackBar(t('wo_delete_failure'), 'error');

  const handleDelete = () => {
    dispatch(deleteWorkOrder(id)).then(onDeleteSuccess).catch(onDeleteFailure);
    setOpenDelete(false);
  };
  const onArchive = () => {
    dispatch(editWorkOrder(id, { ...workOrder, archived: true }))
      .then(onArchiveSuccess)
      .catch(onArchiveFailure);
  };
  const onGenerateReport = () => {
    setLoading(true);
    dispatch(getPDFReport(id))
      .then(async (uri: string) => {
        if (Platform.OS === 'ios') {
          actualDownload(uri);
        } else {
          try {
            const granted = await PermissionsAndroid.request(
              PermissionsAndroid.PERMISSIONS.WRITE_EXTERNAL_STORAGE
            );
            if (granted === PermissionsAndroid.RESULTS.GRANTED) {
              actualDownload(uri);
            } else {
              Alert.alert(
                t('error'),
                t('storage_permission_needed_description')
              );
            }
          } catch (err) {
            console.error(err);
          }
        }
      })
      .catch((err: Error) => console.error(err.message))
      .finally(() => setLoading(false));
  };
  const canComplete = (): boolean => {
    let error;
    const fieldsToTest = [
      {
        name: 'completeFiles',
        condition: !workOrder?.files.length,
        message: 'required_files_on_completion'
      },
      {
        name: 'completeTasks',
        condition: tasks.some((task) => !task.value),
        message: 'required_tasks_on_completion'
      },
      {
        name: 'completeTime',
        condition: labors
          .filter((labor) => labor.logged)
          .some((labor) => !labor.duration),
        message: 'required_labor_on_completion'
      },
      {
        name: 'completeParts',
        condition: !partQuantities.length,
        message: 'required_part_on_completion'
      },
      {
        name: 'completeCost',
        condition: !additionalCosts.length,
        message: 'required_cost_on_completion'
      }
    ];
    fieldsToTest.every((field) => {
      const fieldConfig =
        workOrderConfiguration.workOrderFieldConfigurations.find(
          (woFC) => woFC.fieldName === field.name
        );
      if (fieldConfig.fieldType === 'REQUIRED' && field.condition) {
        showSnackBar(t(field.message), 'error');
        error = true;
        return false;
      }
      return true;
    });

    return !error;
  };
  const onScroll = ({ nativeEvent }) => {
    const currentScrollPosition =
      Math.floor(nativeEvent?.contentOffset?.y) ?? 0;

    setIsExtended(currentScrollPosition <= 0);
  };
  const onCompleteWO = (
    signatureId: number | undefined,
    feedback: string | undefined
  ): Promise<any> => {
    return dispatch(
      changeWorkOrderStatus(id, {
        status: 'COMPLETE',
        feedback: feedback ?? null,
        signature: signatureId ? { id: signatureId } : null
      })
    ).then(() => navigation.navigate('Root'));
  };
  const onStatusChange = (status: string) => {
    if (status === 'COMPLETE') {
      if (canComplete()) {
        if (
          generalPreferences.askFeedBackOnWOClosed ||
          workOrder?.requiredSignature
        ) {
          let error;
          if (workOrder?.requiredSignature) {
            if (!hasFeature(PlanFeature.SIGNATURE)) {
              error =
                'Signature on Work Order completion is not available in your current subscription plan.';
            }
          }
          if (error) {
            showSnackBar(t(error), 'error');
          } else {
            navigation.navigate('CompleteWorkOrder', {
              onComplete: onCompleteWO,
              fieldsConfig: {
                feedback: generalPreferences.askFeedBackOnWOClosed,
                signature: workOrder?.requiredSignature
              }
            });
            return;
          }
        }
      } else return;
    }
    setLoading(true);
    dispatch(
      changeWorkOrderStatus(id, {
        status
      })
    ).finally(() => setLoading(false));
  };
  const groupRelations = (
    relations: Relation[]
  ): { [key: string]: { id: number; workOrder: WorkOrder }[] } => {
    const isParent = (relation: Relation): boolean => {
      return relation.parent.id === workOrder.id;
    };
    const result = {};
    relationTypes.forEach((relationType) => {
      result[relationType] = [];
    });
    relations.forEach((relation) => {
      switch (relation.relationType) {
        case 'BLOCKS':
          if (isParent(relation)) {
            result['BLOCKS'].push({
              id: relation.id,
              workOrder: relation.child
            });
          } else
            result['BLOCKED_BY'].push({
              id: relation.id,
              workOrder: relation.parent
            });
          break;
        case 'DUPLICATE_OF':
          if (isParent(relation)) {
            result['DUPLICATE_OF'].push({
              id: relation.id,
              workOrder: relation.child
            });
          } else
            result['DUPLICATED_BY'].push({
              id: relation.id,
              workOrder: relation.parent
            });
          break;
        case 'RELATED_TO':
          result['RELATED_TO'].push({
            id: relation.id,
            workOrder: isParent(relation) ? relation.child : relation.parent
          });
          break;
        case 'SPLIT_FROM':
          if (isParent(relation)) {
            result['SPLIT_FROM'].push({
              id: relation.id,
              workOrder: relation.child
            });
          } else
            result['SPLIT_TO'].push({
              id: relation.id,
              workOrder: relation.parent
            });
          break;
        default:
          break;
      }
    });

    return result;
  };
  useEffect(() => {
    if (dropDownValue !== workOrder?.status && dropDownValue) onStatusChange(dropDownValue);
  }, [dropDownValue]);

  function ObjectField({
                         label,
                         value,
                         link,
                         permissionEntity,
                         address
                       }: {
    label: string;
    value: string | number;
    link: { route: keyof RootStackParamList; id: number };
    permissionEntity: PermissionEntity;
    address?: string;
  }) {
    if (value) {
      const openMaps = () => {
        if (address) {
          const encodedAddress = encodeURIComponent(address);
          const encodedLabel = encodeURIComponent(String(value));
          
          // URL para abrir o Google Maps com o endereço
          const googleMapsUrl = `https://www.google.com/maps/search/?api=1&query=${encodedAddress}`;
          
          Linking.openURL(googleMapsUrl).catch((err) => {
            console.error('Erro ao abrir o mapa:', err);
          });
        }
      };

      const isLocation = label === t('location');

      return (
        <TouchableOpacity
          // @ts-ignore
          disabled={!hasViewPermission(permissionEntity)}
          onPress={() => {
            // @ts-ignore
            navigation.navigate(link.route, { id: link.id });
          }}
          style={{ marginTop: 20 }}
        >
          <Text style={{ fontSize: 14, color: theme.colors.onSurfaceVariant }}>
            {label}
          </Text>
          <View style={{ flexDirection: 'row', alignItems: 'center' }}>
            <View style={{ flex: 1 }}>
              <Text variant='titleMedium' style={{ fontWeight: 'bold' }}>{value}</Text>
              {isLocation && address && (
                <Text style={{ fontSize: 14, color: theme.colors.onSurfaceVariant, marginTop: 4 }}>{address}</Text>
              )}
            </View>
            {isLocation && address && (
              <IconButton
                icon="directions"
                size={24}
                iconColor={theme.colors.primary}
                onPress={(e) => {
                  e.stopPropagation();
                  openMaps();
                }}
              />
            )}
          </View>
        </TouchableOpacity>
      );
    } else return null;
  }

  function BasicField({
                        label,
                        value
                      }: {
    label: string;
    value: string | number;
  }) {
    return (
      <View style={{ marginTop: 20 }}>
        <Text style={{ fontSize: 14, color: theme.colors.onSurfaceVariant }}>
          {label}
        </Text>
        <Text variant='titleMedium' style={{ fontWeight: 'bold' }}>{value}</Text>
      </View>
    );
  }

  const renderConfirmArchive = () => {
    return (
      <Portal>
        <Dialog visible={openArchive} onDismiss={() => setOpenArchive(false)}>
          <Dialog.Title>{t('confirmation')}</Dialog.Title>
          <Dialog.Content>
            <Text variant='bodyMedium'>
              {t('wo_archive_confirm') + workOrder.title + ' ?'}
            </Text>
          </Dialog.Content>
          <Dialog.Actions>
            <Button onPress={() => setOpenArchive(false)}>{t('cancel')}</Button>
            <Button onPress={onArchive}>{t('archive')}</Button>
          </Dialog.Actions>
        </Dialog>
      </Portal>
    );
  };
  const renderConfirmDelete = () => {
    return (
      <Portal>
        <Dialog visible={openDelete} onDismiss={() => setOpenDelete(false)}>
          <Dialog.Title>{t('confirmation')}</Dialog.Title>
          <Dialog.Content>
            <Text variant='bodyMedium'>{t('confirm_delete_wo')}</Text>
          </Dialog.Content>
          <Dialog.Actions>
            <Button onPress={() => setOpenDelete(false)}>{t('cancel')}</Button>
            <Button onPress={handleDelete}>{t('to_delete')}</Button>
          </Dialog.Actions>
        </Dialog>
      </Portal>
    );
  };
  if (workOrder)
    return (
      <View style={styles.container}>
        <Provider theme={theme}>
          {renderConfirmDelete()}
          {renderConfirmArchive()}
          <ScrollView
            onScroll={onScroll}
            style={{
              paddingHorizontal: 20
            }}
            refreshControl={
              <RefreshControl
                refreshing={loading || loadingDetails}
                onRefresh={getInfos}
              />
            }
          >
            <Text variant='displaySmall'>{workOrder.title}</Text>
            <View style={styles.row}>
              <Text
                variant='titleMedium'
                style={{ marginRight: 10 }}
              >{`#${id}`}</Text>
              <Tag
                text={t('priority_label', { priority: t(workOrder.priority) })}
                color='white'
                backgroundColor={getPriorityColor(workOrder.priority, theme)}
              />
            </View>
            {workOrder.image && (
              <Image
                style={{ height: 200, marginTop: 20 }}
                source={{ uri: workOrder.image.url }}
              />
            )}
            <View style={{ marginTop: 20 }}>
              <View style={styles.dropdown}>
                <Dropdown
                  disabled={!hasEditPermission(PermissionEntity.WORK_ORDERS, workOrder)}
                  value={workOrder.status}
                  items={statuses}
                  open={openDropDown}
                  setOpen={setOpenDropDown}
                  setValue={setDropdownValue}
                />
              </View>
              {workOrder.audioDescription &&
                <View style={{ backgroundColor: 'white', paddingVertical: 20 }}>
                  <Text>{t('audio_description')}</Text>
                  <AudioPlayer url={workOrder.audioDescription.url} />
                </View>}
              {fieldsToRender.map(
                ({ label, value }, index) =>
                  value && (
                    <BasicField key={label} label={label} value={value} />
                  )
              )}
              {touchableFields.map(
                ({ label, value, link, permissionEntity }) =>
                  value && (
                    <ObjectField
                      key={label}
                      label={label}
                      value={value}
                      link={link}
                      permissionEntity={permissionEntity}
                      address={workOrder?.location?.address}
                    />
                  )
              )}
              {(workOrder.parentRequest || workOrder.createdBy) && (
                <ObjectField
                  label={
                    workOrder.parentRequest ? t('approved_by') : t('created_by')
                  }
                  value={getUserNameById(workOrder.createdBy)}
                  link={{ route: 'UserDetails', id: workOrder.createdBy }}
                  permissionEntity={PermissionEntity.PEOPLE_AND_TEAMS}
                />
              )}
              {workOrder.status === 'COMPLETE' && (
                <View>
                  {workOrder.completedBy && (
                    <ObjectField
                      label={t('completed_by')}
                      value={`${workOrder.completedBy.firstName} ${workOrder.completedBy.lastName}`}
                      link={{
                        route: 'UserDetails',
                        id: workOrder.completedBy.id
                      }}
                      permissionEntity={PermissionEntity.PEOPLE_AND_TEAMS}
                    />
                  )}
                  <BasicField
                    label={t('completed_on')}
                    value={getFormattedDate(workOrder.completedOn)}
                  />
                  {workOrder.feedback && (
                    <BasicField
                      label={t('feedback')}
                      value={workOrder.feedback}
                    />
                  )}
                  {workOrder.signature && (
                    <View style={{ marginTop: 20 }}>
                      <Divider style={{ marginBottom: 20 }} />
                      <Text
                        variant='titleMedium'
                        style={{ fontWeight: 'bold' }}
                      >
                        {t('signature')}
                      </Text>
                      <Image
                        source={{ uri: workOrder.signature.url }}
                        style={{ height: 200 }}
                      />
                    </View>
                  )}
                </View>
              )}
              {workOrder.parentRequest && (
                <ObjectField
                  label={t('requested_by')}
                  value={getUserNameById(workOrder.parentRequest.createdBy)}
                  link={{
                    route: 'RequestDetails',
                    id: workOrder.parentRequest.id
                  }}
                  permissionEntity={PermissionEntity.PEOPLE_AND_TEAMS}
                />
              )}
              {!!workOrder.assignedTo.length && (
                <View style={{ marginTop: 20 }}>
                  <Text style={{ fontSize: 14, color: theme.colors.onSurfaceVariant }}>
                    {t('assigned_to')}
                  </Text>
                  {workOrder.assignedTo.map((user) => (
                    <TouchableOpacity key={user.id} style={{ marginTop: 5 }}>
                      <Text
                        variant='bodyLarge'
                        style={{ marginTop: 15 }}
                      >{`${user.firstName} ${user.lastName}`}</Text>
                    </TouchableOpacity>
                  ))}
                  {workOrder.customers.map((customer) => (
                    <TouchableOpacity
                      key={customer.id}
                      style={{ marginTop: 5 }}
                    >
                      <Text variant='bodyLarge' style={{ marginTop: 15 }}>
                        {customer.name}
                      </Text>
                    </TouchableOpacity>
                  ))}
                </View>
              )}
              {!generalPreferences.simplifiedWorkOrder && (
                <View>
                  <View style={styles.shadowedCard}>
                    <Text
                      style={{ marginBottom: 10, color: theme.colors.onSurfaceVariant }}
                    >
                      {t('parts')}
                    </Text>
                    <PartQuantities
                      partQuantities={partQuantities}
                      isPO={false}
                      navigation={navigation}
                      rootId={id}
                      disabled={!hasEditPermission(
                        PermissionEntity.WORK_ORDERS,
                        workOrder
                      )}
                    />
                    {hasEditPermission(
                      PermissionEntity.WORK_ORDERS,
                      workOrder
                    ) && <Fragment>
                      <Divider style={{ marginTop: 5 }} />
                      <Button
                        onPress={() =>
                          navigation.navigate('SelectParts', {
                            onChange: (selectedParts) => {
                              dispatch(
                                editWOPartQuantities(
                                  id,
                                  selectedParts.map((part) => part.id)
                                )
                              ).catch((error) =>
                                showSnackBar(t('not_enough_part'), 'error')
                              );
                            },
                            selected: partQuantities.map(
                              (partQuantity) => partQuantity.part.id
                            )
                          })
                        }
                      >
                        {t('add_parts')}
                      </Button>
                    </Fragment>
                    }
                  </View>
                  <View style={styles.shadowedCard}>
                    <Text
                      style={{ marginBottom: 10, color: theme.colors.onSurfaceVariant }}
                    >
                      {t('additional_costs')}
                    </Text>
                    {!additionalCosts.length ? (
                      <Text style={{ fontWeight: 'bold' }}>
                        {t('no_additional_cost')}
                      </Text>
                    ) : (
                      <View>
                        {additionalCosts.map((cost) => (
                          <View
                            key={cost.id}
                            style={{ display: 'flex', flexDirection: 'column' }}
                          >
                            <Text
                              style={{ fontWeight: 'bold' }}
                              variant='bodyLarge'
                            >
                              {cost.description}
                            </Text>
                            <Text>{getFormattedCurrency(cost.cost)}</Text>
                          </View>
                        ))}
                        <Text
                          style={{ fontWeight: 'bold' }}
                          variant='bodyLarge'
                        >
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
                    {hasEditPermission(
                      PermissionEntity.WORK_ORDERS,
                      workOrder
                    ) && <Fragment>
                      <Divider style={{ marginTop: 5 }} />
                      <Button
                        disabled={
                          !(
                            hasEditPermission(
                              PermissionEntity.WORK_ORDERS,
                              workOrder
                            ) && hasFeature(PlanFeature.ADDITIONAL_COST)
                          )
                        }
                        onPress={() =>
                          navigation.push('AddAdditionalCost', {
                            workOrderId: workOrder.id
                          })
                        }
                      >
                        {t('add_additional_cost')}
                      </Button></Fragment>}
                  </View>
                </View>
              )}
              {!!tasks.length && (
                <View style={styles.shadowedCard}>
                  <Text style={{ marginBottom: 10, color: theme.colors.onSurfaceVariant }}>
                    {t('tasks')}
                  </Text>
                  <TouchableOpacity
                    onPress={() =>
                      navigation.navigate('Tasks', {
                        workOrderId: id,
                        tasksProps: tasks
                      })
                    }
                  >
                    <Text variant='titleLarge' style={{ fontWeight: 'bold' }}>
                      {' '}
                      {t('remaining_tasks', {
                        count: tasks.filter((task) => !task.value).length
                      })}
                    </Text>
                    <Text variant='bodyMedium'>
                      {t('complete_tasks_percent', {
                        percent: (
                          (tasks.filter((task) => task.value).length * 100) /
                          tasks.length
                        ).toFixed(0)
                      })}
                    </Text>
                    <Divider style={{ marginTop: 5 }} />
                    <ProgressBar
                      progress={
                        tasks.filter((task) => task.value).length / tasks.length
                      }
                    />
                  </TouchableOpacity>
                </View>
              )}
              {!generalPreferences.simplifiedWorkOrder && (
                <View>
                  {!!workOrder.files.length && (
                    <View style={styles.shadowedCard}>
                      <Text
                        style={{
                          marginBottom: 10,
                          color: theme.colors.onSurfaceVariant
                        }}
                      >
                        {t('files')}
                      </Text>
                      {workOrder.files.map((file) => (
                        <List.Item
                          key={file.id}
                          titleStyle={{ color: theme.colors.primary }}
                          title={file.name}
                          onPress={() => {
                            Linking.openURL(file.url);
                          }}
                        />
                      ))}
                    </View>
                  )}
                  {!!currentWorkOrderRelations.length && (
                    <View style={styles.shadowedCard}>
                      <Text
                        style={{
                          marginBottom: 10,
                          color: theme.colors.onSurfaceVariant
                        }}
                      >
                        {t('links')}
                      </Text>
                      {Object.entries(
                        groupRelations(currentWorkOrderRelations)
                      ).map(
                        ([relationType, relations]) =>
                          !!relations.length && (
                            <View>
                              <Text style={{ fontWeight: 'bold' }}>
                                {t(relationType)}
                              </Text>
                              {relations.map((relation) => (
                                <List.Item
                                  title={relation.workOrder.title}
                                  onPress={() =>
                                    navigation.push('WODetails', {
                                      id: relation.workOrder.id
                                    })
                                  }
                                  description={getFormattedDate(
                                    relation.workOrder.createdAt
                                  )}
                                />
                              ))}
                            </View>
                          )
                      )}
                    </View>
                  )}
                  <View style={styles.shadowedCard}>
                    <Text
                      style={{ marginBottom: 10, color: theme.colors.onSurfaceVariant }}
                    >
                      {t('labors')}
                    </Text>
                    {labors
                      .filter((labor) => !labor.logged)
                      .map((labor) => (
                        <List.Item
                          key={labor.id}
                          title={
                            labor.assignedTo
                              ? `${labor.assignedTo.firstName} ${labor.assignedTo.lastName}`
                              : t('not_assigned')
                          }
                          description={`${
                            getHoursAndMinutesAndSeconds(labor.duration)[0]
                          }h ${
                            getHoursAndMinutesAndSeconds(labor.duration)[1]
                          }m`}
                        />
                      ))}

                    {hasEditPermission(
                      PermissionEntity.WORK_ORDERS,
                      workOrder
                    ) && <Fragment>
                      <Divider style={{ marginTop: 5 }} />
                      <Button
                        disabled={
                          !(
                            hasEditPermission(
                              PermissionEntity.WORK_ORDERS,
                              workOrder
                            ) && hasFeature(PlanFeature.ADDITIONAL_TIME)
                          )
                        }
                        onPress={() =>
                          navigation.push('AddAdditionalTime', {
                            workOrderId: workOrder.id
                          })
                        }
                      >
                        {t('add_time')}
                      </Button>
                    </Fragment>
                    }
                  </View>
                  {!!currentWorkOrderHistories.length && (
                    <View style={styles.shadowedCard}>
                      <Text
                        style={{
                          marginBottom: 10,
                          color: theme.colors.onSurfaceVariant
                        }}
                      >
                        {t('history')}
                      </Text>
                      {currentWorkOrderHistories.map((workOrderHistory) => (
                        <List.Item
                          key={workOrderHistory.id}
                          title={`${workOrderHistory.user.firstName} ${workOrderHistory.user.lastName}`}
                          description={getFormattedDate(
                            workOrderHistory.createdAt
                          )}
                        />
                      ))}
                    </View>
                  )}
                </View>
              )}
            </View>
          </ScrollView>
          {!generalPreferences.simplifiedWorkOrder && hasEditPermission(PermissionEntity.WORK_ORDERS, workOrder) && (
            <FAB
              icon={runningTimer ? 'stop' : 'play'}
              label={
                runningTimer
                  ? runningTimerDuration
                  : durationToHours(primaryTime?.duration)
              }
              disabled={controllingTime}
              theme={theme}
              variant={runningTimer ? 'primary' : 'secondary'}
              color='white'
              onPress={() => {
                setControllingTime(true);
                dispatch(controlTimer(!runningTimer, id)).finally(() =>
                  setControllingTime(false)
                );
              }}
              visible={true}
              style={[styles.fabStyle]}
            />
          )}
        </Provider>
      </View>
    );
  else return <LoadingDialog visible={true} />;
}

const styles = StyleSheet.create({
  container: {
    flex: 1
  },
  separator: {
    marginVertical: 30,
    height: 1,
    width: '80%'
  },
  startButton: { position: 'absolute', bottom: 20, right: '10%' },
  row: { display: 'flex', flexDirection: 'row', alignItems: 'center' },
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
  fabStyle: {
    bottom: 16,
    right: 16,
    position: 'absolute'
  },
  dropdown: { zIndex: 10 }
});
