import * as React from 'react';
import { useCallback, useContext, useEffect, useState } from 'react';
import { Alert, ScrollView, StyleSheet, View } from 'react-native';
import {
  Button,
  Dialog,
  FAB,
  IconButton,
  Menu,
  Portal,
  Text
} from 'react-native-paper';
import { useTranslation } from 'react-i18next';
import * as Yup from 'yup';
import { useDispatch, useSelector } from '../../../store';
import { AssetDTO, assetStatuses } from '../../../models/asset';
import AssetDowntime from '../../../models/assetDowntime';
import { IField } from '../../../models/form';
import Form from '../../../components/form';
import useAuth from '../../../hooks/useAuth';
import { PermissionEntity } from '../../../models/role';
import { editAsset } from '../../../slices/asset';
import {
  createAssetDowntime,
  deleteAssetDowntime,
  editAssetDowntime,
  getAssetDowntimes
} from '../../../slices/assetDowntime';
import { CustomSnackBarContext } from '../../../contexts/CustomSnackBarContext';
import { CompanySettingsContext } from '../../../contexts/CompanySettingsContext';
import { useAppTheme } from '../../../custom-theme';
import { spacing } from '../../../theme/tokens';
import {
  EmptyState,
  EntityListCard,
  PaginatedEntityList
} from '../../../components/ui';
import {
  getHoursAndMinutesAndSeconds,
  getHMSString
} from '../../../utils/formatters';
import { getErrorMessage } from '../../../utils/api';

interface AssetDowntimesProps {
  asset: AssetDTO;
  navigation: { navigate: (...args: unknown[]) => void };
}

export default function AssetDowntimes({ asset, navigation }: AssetDowntimesProps) {
  const { t } = useTranslation();
  const theme = useAppTheme();
  const dispatch = useDispatch();
  const { showSnackBar } = useContext(CustomSnackBarContext);
  const { getFormattedDate } = useContext(CompanySettingsContext);
  const { hasEditPermission } = useAuth();
  const { assetDowntimesByAsset } = useSelector((state) => state.downtimes);
  const downtimes = (assetDowntimesByAsset[asset?.id] ?? []).filter(
    (downtime) => downtime.duration
  );
  const canEdit = hasEditPermission(PermissionEntity.ASSETS, asset);

  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [openAdd, setOpenAdd] = useState(false);
  const [openEdit, setOpenEdit] = useState(false);
  const [currentDowntime, setCurrentDowntime] = useState<AssetDowntime>();
  const [statusMenuOpen, setStatusMenuOpen] = useState(false);

  const loadDowntimes = useCallback(() => {
    if (!asset?.id) return Promise.resolve();
    return dispatch(getAssetDowntimes(asset.id)).finally(() => {
      setLoading(false);
      setRefreshing(false);
    });
  }, [asset?.id, dispatch]);

  useEffect(() => {
    setLoading(true);
    loadDowntimes();
  }, [loadDowntimes]);

  const verifyValues = (values: {
    startsOn: string;
    hours: number;
    minutes: number;
  }) => {
    const seconds = values.hours * 60 * 60 + values.minutes * 60;
    const startsOn = new Date(values.startsOn);
    startsOn.setSeconds(startsOn.getSeconds() + seconds);
    if (startsOn > new Date()) {
      showSnackBar(t('downtime_end_in_future'), 'error');
      return false;
    }
    return true;
  };

  const fields: Array<IField> = [
    { name: 'startsOn', type: 'date', label: t('started_on') },
    {
      name: 'hours',
      type: 'number',
      label: t('hours'),
      placeholder: t('hours'),
      required: true,
      midWidth: true
    },
    {
      name: 'minutes',
      type: 'number',
      label: t('minutes'),
      placeholder: t('minutes'),
      required: true,
      midWidth: true
    }
  ];

  const shape = {
    startsOn: Yup.string().required(t('required_startsOn')),
    hours: Yup.number().required(t('required_hours')),
    minutes: Yup.number().required(t('required_minutes'))
  };

  const handleDelete = (id: number) => {
    Alert.alert(t('confirmation'), t('confirm_delete_asset_downtime'), [
      { text: t('cancel'), style: 'cancel' },
      {
        text: t('to_delete'),
        style: 'destructive',
        onPress: () => dispatch(deleteAssetDowntime(asset.id, id))
      }
    ]);
  };

  const renderItem = useCallback(
    ({ item: downtime }: { item: AssetDowntime }) => (
      <EntityListCard
        title={getHMSString(downtime.duration)}
        subtitle={getFormattedDate(downtime.startsOn)}
        icon="clock-alert-outline"
        onPress={
          canEdit
            ? () => {
                setCurrentDowntime(downtime);
                setOpenEdit(true);
              }
            : undefined
        }
        footer={
          canEdit ? (
            <IconButton
              icon="delete-outline"
              iconColor={theme.colors.error}
              accessibilityLabel={t('remove_downtime')}
              onPress={() => handleDelete(downtime.id)}
            />
          ) : undefined
        }
      />
    ),
    [canEdit, getFormattedDate, t, theme.colors.error]
  );

  const renderFormDialog = (
    visible: boolean,
    onDismiss: () => void,
    submitText: string,
    title: string,
    initialValues: Record<string, unknown>,
    onSubmit: (values: {
      startsOn: string;
      hours: number;
      minutes: number;
    }) => Promise<unknown>
  ) => (
    <Portal>
      <Dialog
        visible={visible}
        onDismiss={onDismiss}
        style={{ backgroundColor: theme.colors.card, maxHeight: '90%' }}
      >
        <Dialog.Title>{title}</Dialog.Title>
        <Dialog.ScrollArea style={{ paddingHorizontal: 0 }}>
          <ScrollView>
            <Form
              fields={fields}
              validation={Yup.object().shape(shape)}
              submitText={submitText}
              values={initialValues}
              navigation={navigation}
              onSubmit={async (values) => {
                const payload = values as {
                  startsOn: string;
                  hours: number;
                  minutes: number;
                };
                if (!verifyValues(payload)) return;
                try {
                  await onSubmit(payload);
                  onDismiss();
                } catch (err) {
                  showSnackBar(
                    getErrorMessage(err, t('create_downtime_failure')),
                    'error'
                  );
                }
              }}
            />
          </ScrollView>
        </Dialog.ScrollArea>
      </Dialog>
    </Portal>
  );

  const listHeader = (
    <View style={styles.header}>
      <Text variant="labelLarge">{t('asset_status')}</Text>
      {canEdit ? (
        <Menu
          visible={statusMenuOpen}
          onDismiss={() => setStatusMenuOpen(false)}
          anchor={
            <Button
              mode="outlined"
              onPress={() => setStatusMenuOpen(true)}
              accessibilityLabel={t('select_asset_status')}
            >
              {t(asset.status)}
            </Button>
          }
        >
          {assetStatuses.map((assetStatus) => (
            <Menu.Item
              key={assetStatus.status}
              title={t(assetStatus.status)}
              onPress={() => {
                setStatusMenuOpen(false);
                dispatch(editAsset(asset.id, { status: assetStatus.status }));
              }}
            />
          ))}
        </Menu>
      ) : (
        <Text variant="bodyLarge">{t(asset.status)}</Text>
      )}
    </View>
  );

  return (
    <View style={[styles.container, { backgroundColor: theme.colors.background }]}>
      <PaginatedEntityList
        data={downtimes}
        keyExtractor={(downtime) => downtime.id.toString()}
        renderItem={renderItem}
        loading={loading}
        refreshing={refreshing}
        onRefresh={() => {
          setRefreshing(true);
          loadDowntimes();
        }}
        ListHeaderComponent={listHeader}
        ListEmptyComponent={
          <EmptyState icon="clock-alert-outline" title={t('no_downtimes')} />
        }
      />
      {canEdit && (
        <FAB
          icon="plus"
          style={[styles.fab, { backgroundColor: theme.colors.primary }]}
          color={theme.colors.white}
          accessibilityLabel={t('add_downtime')}
          onPress={() => setOpenAdd(true)}
        />
      )}
      {renderFormDialog(openAdd, () => setOpenAdd(false), t('add'), t('add_downtime'), {}, async (values) => {
        await dispatch(
          createAssetDowntime(asset.id, {
            ...values,
            duration: values.hours * 3600 + values.minutes * 60
          })
        );
        showSnackBar(t('create_downtime_success'), 'success');
      })}
      {currentDowntime &&
        renderFormDialog(
          openEdit,
          () => {
            setOpenEdit(false);
            setCurrentDowntime(undefined);
          },
          t('save'),
          t('edit_downtime'),
          {
            ...currentDowntime,
            hours: getHoursAndMinutesAndSeconds(currentDowntime.duration)[0],
            minutes: getHoursAndMinutesAndSeconds(currentDowntime.duration)[1]
          },
          async (values) => {
            await dispatch(
              editAssetDowntime(currentDowntime.id, asset.id, {
                ...currentDowntime,
                ...values,
                duration: values.hours * 3600 + values.minutes * 60
              })
            );
            showSnackBar(t('edit_downtime_success'), 'success');
          }
        )}
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1
  },
  header: {
    paddingHorizontal: spacing.lg,
    paddingTop: spacing.md,
    paddingBottom: spacing.sm,
    gap: spacing.sm
  },
  fab: {
    position: 'absolute',
    right: spacing.lg,
    bottom: spacing.lg
  }
});
