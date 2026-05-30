import {
  Alert,
  ActivityIndicator,
  Image,
  KeyboardAvoidingView,
  Linking,
  PermissionsAndroid,
  Platform,
  Pressable,
  RefreshControl,
  ScrollView,
  StyleSheet,
  TouchableOpacity,
  TextInput as RNTextInput
} from 'react-native';
import { useMentions } from 'react-native-controlled-mentions';
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
  TextInput,
  useTheme
} from 'react-native-paper';
import * as DocumentPicker from 'expo-document-picker';
import { useTranslation } from 'react-i18next';
import * as React from 'react';
import {
  Fragment,
  useCallback,
  useContext,
  useEffect,
  useRef,
  useState
} from 'react';
import { CompanySettingsContext } from '../../contexts/CompanySettingsContext';
import Tag from '../../components/Tag';
import { getPriorityColor, getStatusColor } from '../../utils/overall';
import { PermissionEntity } from '../../models/role';
import useAuth from '../../hooks/useAuth';
import { controlTimer, getLabors } from '../../slices/labor';
import { useDispatch, useSelector } from '../../store';
import {
  durationToHours,
  getHoursAndMinutesAndSeconds
} from '../../utils/formatters';
import {
  editWOPartQuantities,
  getPartQuantitiesByWorkOrder
} from '../../slices/partQuantity';
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
import Labor from '../../models/labor';
import { AudioPlayer } from '../../components/AudioPlayer';
import { Task } from '../../models/tasks';
import { getErrorMessage } from '../../utils/api';
import ImageView from 'react-native-image-viewing';
import { getCustomFieldValuesForDetails } from '../../models/form';
import CommentItem from '../../components/CommentItem';
import { downloadFile } from '../../utils/fileDownload';
import { getCommentsByWorkOrder, createComment } from '../../slices/comment';
import { getUsersMini } from '../../slices/user';
import { TriggersConfig } from 'react-native-controlled-mentions/dist/types/types';
import { useHeaderHeight } from '@react-navigation/elements';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

const getRemainingTasksLength = (tasks: Task[]): number => {
  const SECONDS_MS = 5_000;

  const mappedTasks = tasks.map((task) => {
    const createdAt = new Date(task.createdAt).getTime();
    const updatedAt = new Date(task.updatedAt).getTime();

    const updatedAfterMoreThanThreshold = updatedAt - createdAt > SECONDS_MS;

    return {
      ...task,
      updatedAfterMoreThanThreshold
    };
  });

  return mappedTasks.filter(
    (task) => !task.value || !task.updatedAfterMoreThanThreshold
  ).length;
};
const triggersConfig: TriggersConfig<'mention'> = {
  mention: {
    trigger: '@',
    pattern: /(@\[[^\]]+\]\(user:[^)]+\))/g,
    isInsertSpaceAfterMention: true,
    textStyle: { fontWeight: 'bold', color: 'blue' },
    getTriggerData: (match: string) => {
      const result = match.match(/@\[(.*?)\]\(user:(.*?)\)/);
      return {
        original: match,
        trigger: '@',
        name: result?.[1] ?? '',
        id: result?.[2] ?? ''
      };
    },
    getTriggerValue: (suggestion) =>
      `@[${suggestion.name}](user:${suggestion.id})`
  }
};
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
  const [dropDownValue, setDropdownValue] = useState<string>(
    workOrder?.status ?? ''
  );
  const {
    hasEditPermission,
    user,
    companySettings,
    hasFeature,
    hasViewPermission
  } = useAuth();
  const { showSnackBar } = useContext(CustomSnackBarContext);
  const { uploadFiles } = useContext(CompanySettingsContext);
  const [runningTimerDuration, setRunningTimerDuration] = useState<string>();
  const { workOrderConfiguration, generalPreferences } = companySettings;
  const [loading, setLoading] = useState<boolean>(false);
  const theme = useTheme();
  const headerHeight = useHeaderHeight();
  const insets = useSafeAreaInsets();
  const scrollViewRef = useRef<ScrollView>(null);
  const [isImageViewerOpen, setIsImageViewerOpen] = useState<boolean>(false);
  const dispatch = useDispatch();
  const { partQuantitiesByWorkOrder, loadingPartQuantities } = useSelector(
    (state) => state.partQuantities
  );
  const partQuantities = partQuantitiesByWorkOrder[id] ?? [];
  const { relationsByWorkOrder, loadingRelations } = useSelector(
    (state) => state.relations
  );
  const { tasksByWorkOrder, loadingTasks } = useSelector(
    (state) => state.tasks
  );
  const tasks = tasksByWorkOrder[id] ?? [];
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
  const [commentContent, setCommentContent] = useState('');
  const [commentFiles, setCommentFiles] = useState<
    { uri: string; name: string; type: string }[]
  >([]);
  const { commentsByWorkOrder, loadingComments, loadingCreate } = useSelector(
    (state) => state.comments
  );
  const { usersMini } = useSelector((state) => state.users);
  const comments = commentsByWorkOrder[id] ?? [];
  const statuses = ['OPEN', 'ON_HOLD', 'IN_PROGRESS', 'COMPLETE'].map(
    (status) => ({ value: status, label: t(status) })
  );
  const [openDelete, setOpenDelete] = React.useState(false);
  const [openArchive, setOpenArchive] = React.useState(false);
  const remainingTasksLength = getRemainingTasksLength(tasks);
  const loadingDetails =
    loadingPartQuantities[id] ||
    loadingTasks[id] ||
    loadingCosts[id] ||
    loadingLabors[id] ||
    loadingRelations[id];
  const fieldsToRender: {
    label: string;
    value: string | number;
    isLink?: boolean;
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
    },
    ...getCustomFieldValuesForDetails(
      workOrder?.customFieldValues,
      getFormattedDate
    )
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
    if (!workOrderProp) dispatch(getWorkOrderDetails(id));
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
            <IconButton icon="dots-vertical" />
          </Pressable>
        )
    });
    //LogBox.ignoreLogs(['VirtualizedLists should never be nested']);
  }, [loadingTasks, workOrder, tasks]);

  useEffect(() => {
    getInfos();
  }, [workOrderProp]);

  useEffect(() => {
    dispatch(getCommentsByWorkOrder(id));
    dispatch(getUsersMini());
  }, [id]);

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

  const actualDownload = async (uri: string): Promise<void> => {
    const rawFileName = workOrder?.title ?? `work-order-${id}`;
    const fileName = `${rawFileName.replace(/[\\/:*?"<>|]/g, '_')}.pdf`;
    await downloadFile(uri, fileName);
  };
  const getRunningTimerDuration = (labor: Labor) => {
    return durationToHours(
      labor.duration +
        (new Date().getTime() - new Date(labor.startedAt).getTime()) / 1000
    );
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
          await actualDownload(uri);
        } else {
          if (Platform.OS === 'android' && Platform.Version >= 29)
            await actualDownload(uri);
          else {
            try {
              const granted = await PermissionsAndroid.request(
                PermissionsAndroid.PERMISSIONS.WRITE_EXTERNAL_STORAGE
              );
              if (granted === 'granted') {
                await actualDownload(uri);
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
        condition:
          (workOrder?.files.length || 0) +
            comments.filter((comment) => comment.files.length).length ===
          0,
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
          .filter((labor) => labor.duration).length === 0,
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
    signature: string | undefined,
    feedback: string | undefined
  ): Promise<any> => {
    return dispatch(
      changeWorkOrderStatus(id, {
        status: 'COMPLETE',
        feedback: feedback ?? null,
        signature
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

  const { textInputProps, triggers } = useMentions({
    value: commentContent,
    onChange: setCommentContent,
    triggersConfig
  });

  const mentionKeyword = triggers?.mention?.keyword ?? null;
  const filteredUsers = (
    mentionKeyword
      ? usersMini.filter((user) =>
          `${user.firstName} ${user.lastName}`
            .toLowerCase()
            .includes(mentionKeyword.toLowerCase())
        )
      : usersMini
  ).map((user) => ({
    id: user.id.toString(),
    name: `${user.firstName} ${user.lastName}`
  }));

  const handleCommentSubmit = async () => {
    if (!commentContent.trim()) return;
    try {
      let fileIds: { id: number }[] = [];
      if (commentFiles.length > 0) {
        const uploadedFiles = await uploadFiles(commentFiles, [], false);
        fileIds = uploadedFiles.map((f) => ({ id: f.id }));
      }
      await dispatch(
        createComment({
          workOrder: { id },
          content: commentContent.trim(),
          files: fileIds
        })
      );
      setCommentContent('');
      setCommentFiles([]);
    } catch (error) {
      console.error('Failed to create comment:', error);
    }
  };

  const pickCommentFile = async () => {
    try {
      const result = await DocumentPicker.getDocumentAsync({
        type: '*/*',
        multiple: true,
        copyToCacheDirectory: true
      });
      if (!result.canceled && result.assets) {
        const newFiles = result.assets.map((asset) => ({
          uri: asset.uri,
          name: asset.name,
          type: asset.mimeType || 'application/octet-stream'
        }));
        setCommentFiles([...commentFiles, ...newFiles]);
      }
    } catch (error) {
      console.error('Error picking document:', error);
    }
  };

  const removeCommentFile = (index: number) => {
    setComm
