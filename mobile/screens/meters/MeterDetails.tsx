import { Image, Pressable, ScrollView } from 'react-native';
import LoadingDialog from '../../components/LoadingDialog';
import * as React from 'react';
import { useContext, useEffect, useState } from 'react';
import { useDispatch, useSelector } from '../../store';
import { RootStackScreenProps } from '../../types';
import {
  Button,
  Dialog,
  Divider,
  IconButton,
  Portal,
  Text,
  useTheme
} from 'react-native-paper';
import { useTranslation } from 'react-i18next';
import { View } from '../../components/Themed';
import ListField from '../../components/ListField';
import { UserMiniDTO } from '../../models/user';
import { getUserUrl } from '../../utils/urlPaths';
import { CustomSnackBarContext } from '../../contexts/CustomSnackBarContext';
import { deleteMeter, getMeterDetails } from '../../slices/meter';
import { getWorkOrderMeterTriggers } from '../../slices/workOrderMeterTrigger';
import { createReading, getReadings } from '../../slices/reading';
import { CompanySettingsContext } from '../../contexts/CompanySettingsContext';
import { SheetManager } from 'react-native-actions-sheet';
import { canAddReading } from '../../utils/overall';
import NumberInput from '../../components/NumberInput';
import BasicField from '../../components/BasicField';
import useAuth from '../../hooks/useAuth';

export default function MeterDetails({
                                       navigation,
                                       route
                                     }: RootStackScreenProps<'MeterDetails'>) {
  const { id, meterProp } = route.params;
  const { loadingGet, meterInfos } = useSelector((state) => state.meters);
  const { readingsByMeter } = useSelector((state) => state.readings);
  const { metersTriggers } = useSelector(
    (state) => state.workOrderMeterTriggers
  );
  const currentMeterTriggers = metersTriggers[id] ?? [];
  const currentMeterReadings = readingsByMeter[id] ?? [];
  const [openModal, setOpenModal] = useState<boolean>(false);
  const meter = meterInfos[id]?.meter ?? meterProp;
  const theme = useTheme();
  const [readingValue, setReadingValue] = useState<number>(0);
  const [addedReading, setAddedReading] = useState<boolean>(false);
  const { showSnackBar } = useContext(CustomSnackBarContext);
  const { getFormattedDate } = useContext(CompanySettingsContext);
  const [isSubmitting, setIsSubmitting] = useState<boolean>(false);
  const { t } = useTranslation();
  const { user } = useAuth();
  const dispatch = useDispatch();
  const [openDelete, setOpenDelete] = useState<boolean>(false);
  const fieldsToRender = [
    {
      label: t('location_name'),
      value: meter?.location?.name
    },
    {
      label: t('asset_name'),
      value: meter?.asset?.name
    },
    {
      label: t('reading_frequency'),
      value: t('every_frequency_days', { frequency: meter?.updateFrequency })
    }
  ];
  const onDeleteSuccess = () => {
    showSnackBar(t('meter_delete_success'), 'success');
    navigation.goBack();
  };
  const onDeleteFailure = (err) =>
    showSnackBar(t('meter_delete_failure'), 'error');

  const handleDelete = () => {
    dispatch(deleteMeter(meter?.id))
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
            <Text variant='bodyMedium'>{t('confirm_delete_meter')}</Text>
          </Dialog.Content>
          <Dialog.Actions>
            <Button onPress={() => setOpenDelete(false)}>{t('cancel')}</Button>
            <Button onPress={handleDelete}>{t('to_delete')}</Button>
          </Dialog.Actions>
        </Dialog>
      </Portal>
    );
  };

  useEffect(() => {
    const { id, meterProp } = route.params;
    if (!meterProp)
      dispatch(getMeterDetails(id));
    dispatch(getWorkOrderMeterTriggers(id));
    dispatch(getReadings(id));
  }, [route]);

  useEffect(() => {
    navigation.setOptions({
      title: meter?.name ?? t('loading'),
      headerRight: () => (
        <Pressable
          onPress={() => {
            SheetManager.show('meter-details-sheet', {
              payload: {
                onEdit: () => navigation.navigate('EditMeter', { meter }),
                onDelete: () => setOpenDelete(true),
                meter
              }
            });
          }}
        >
          <IconButton icon='dots-vertical' />
        </Pressable>
      )
    });
  }, [meter]);

  const onAddReading = () => {
    setIsSubmitting(true);
    dispatch(createReading(meter.id, { value: readingValue }))
      .then(() => {
        setAddedReading(true);
        setOpenModal(false);
      })
      .finally(() => setIsSubmitting(false));
  };


  const renderAddReading = () => {
    return (
      <Portal>
        <Dialog
          visible={openModal}
          onDismiss={() => setOpenModal(false)}
          style={{ backgroundColor: 'white' }}
        >
          <Dialog.Title>{t('add_reading')}</Dialog.Title>
          <Dialog.Content>
            <NumberInput
              style={{ width: '100%' }}
              mode='outlined'
              label={t('reading')}
              defaultValue={'0'}
              placeholder={t('meter_reading')}
              onChangeText={(newValue) => {
                setReadingValue(Number(newValue));
              }}
              disabled={isSubmitting}
              error={false}
              onBlur={function(e: any): void {
              }}
              multiline={false}
            />
          </Dialog.Content>
          <Dialog.Actions>
            <Button onPress={() => setOpenModal(false)}>{t('cancel')}</Button>
            <Button
              onPress={onAddReading}
              loading={isSubmitting}
              disabled={isSubmitting}
            >
              {t('add')}
            </Button>
          </Dialog.Actions>
        </Dialog>
      </Portal>
    );
  };
  if (meter)
    return (
      <ScrollView style={{ flex: 1, backgroundColor: theme.colors.background }}>
        {renderConfirmDelete()}
        {renderAddReading()}
        {meter.image && (
          <Image style={{ height: 200 }} source={{ uri: meter.image.url }} />
        )}
        {fieldsToRender.map((field) => (
          <BasicField
            key={field.label}
            label={field.label}
            value={field.value}
          />
        ))}
        <ListField
          values={meter?.users}
          label={t('assigned_to')}
          getHref={(user: UserMiniDTO) => getUserUrl(user.id)}
          getValueLabel={(user: UserMiniDTO) =>
            `${user.firstName} ${user.lastName}`
          }
        />
        {canAddReading(meter) && !addedReading && user.role.code !== 'VIEW_ONLY' && (
          <Button
            onPress={() => setOpenModal(true)}
            mode={'contained'}
            style={{ marginHorizontal: 20, marginVertical: 20 }}
          >
            {t('add_reading')}
          </Button>
        )}
        {!!currentMeterTriggers.length && (
          <Text
            variant={'titleMedium'}
            style={{ color: theme.colors.primary, padding: 20 }}
          >
            {t('wo_triggers')}
          </Text>
        )}
        {currentMeterTriggers.map((trigger) => (
          <View style={{ padding: 20 }} key={trigger.id}>
            <Text style={{ fontWeight: 'bold' }}>{trigger.name}</Text>
            <Text>{`${
              trigger.triggerCondition === 'MORE_THAN'
                ? t('greater_than')
                : t('lower_than')
            } ${trigger.value} ${meter.unit}`}</Text>
          </View>
        ))}
        <Text
          variant={'titleMedium'}
          style={{ color: theme.colors.primary, padding: 20 }}
        >
          {t('reading_history')}
        </Text>
        {[...currentMeterReadings].reverse().map((reading) => (
          <BasicField
            key={reading.id}
            label={getFormattedDate(reading.createdAt)}
            value={`${reading.value} ${meter.unit}`}
          />
        ))}
      </ScrollView>
    );
  else return <LoadingDialog visible={true} />;
}
