import { Image, ScrollView, TouchableOpacity } from 'react-native';
import LoadingDialog from '../../components/LoadingDialog';
import * as React from 'react';
import { useContext, useEffect, useState } from 'react';
import { useDispatch, useSelector } from '../../store';
import { RootStackParamList, RootStackScreenProps } from '../../types';
import {
  ActivityIndicator,
  Button,
  Dialog,
  Divider,
  IconButton,
  Portal,
  Text,
  TextInput,
  useTheme
} from 'react-native-paper';
import { useTranslation } from 'react-i18next';
import { View } from '../../components/Themed';
import { CustomSnackBarContext } from '../../contexts/CustomSnackBarContext';
import {
  approveRequest,
  cancelRequest,
  deleteRequest,
  getRequestDetails
} from '../../slices/request';
import { CompanySettingsContext } from '../../contexts/CompanySettingsContext';
import { PermissionEntity } from '../../models/role';
import useAuth from '../../hooks/useAuth';
import Request from '../../models/request';
import Form from '../../components/form';
import { AudioPlayer } from '../../components/AudioPlayer';

export default function RequestDetails({
  navigation,
  route
}: RootStackScreenProps<'RequestDetails'>) {
  const { id, requestProp } = route.params;
  const { loadingGet, requestInfos } = useSelector((state) => state.requests);
  const request = requestInfos[id]?.request ?? requestProp;
  const theme = useTheme();
  const [approving, setApproving] = useState<boolean>(false);
  const [cancelling, setCancelling] = useState<boolean>(false);
  const { showSnackBar } = useContext(CustomSnackBarContext);
  const [openRejectModal, setOpenRejectModal] = useState<boolean>(false);
  const { getFormattedDate, getUserNameById } = useContext(
    CompanySettingsContext
  );
  const { hasViewPermission, hasEditPermission, hasDeletePermission, user } =
    useAuth();
  const { t } = useTranslation();
  const dispatch = useDispatch();
  const [openDelete, setOpenDelete] = useState<boolean>(false);
  const onApprove = () => {
    setApproving(true);
    dispatch(approveRequest(request.id))
      .then((workOrderId: number) => {
        navigation.push('WODetails', { id: workOrderId });
      })
      .finally(() => setApproving(false));
  };

  const getStatusMeta = (request: Request): [string, string] => {
    if (request.workOrder) {
      // @ts-ignore
      return [t('approved'), theme.colors.success];
    } else if (request.cancelled) {
      return [t('rejected'), theme.colors.error];
    } else return [t('pending'), theme.colors.primary];
  };
  const fieldsToRender = [
    {
      label: t('description'),
      value: request?.description
    },
    {
      label: t('status'),
      value: request ? getStatusMeta(request)[0] : null
    },
    {
      label: t('id'),
      value: request?.id
    },
    {
      label: t('priority'),
      value: t(`${request?.priority.toLowerCase()}_priority`)
    },
    {
      label: t('due_date'),
      value: getFormattedDate(request?.dueDate)
    },
    {
      label: t('estimated_start_date'),
      value: getFormattedDate(request?.estimatedStartDate)
    },
    {
      label: t('category'),
      value: request?.category?.name
    }
  ];
  const onDeleteSuccess = () => {
    showSnackBar(t('request_delete_success'), 'success');
    navigation.goBack();
  };
  const onDeleteFailure = (err) =>
    showSnackBar(t('request_delete_failure'), 'error');

  const handleDelete = () => {
    dispatch(deleteRequest(request?.id))
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
            <Text variant="bodyMedium">{t('confirm_delete_request')}</Text>
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
    const { id, requestProp } = route.params;
    if (!requestProp) dispatch(getRequestDetails(id));
  }, [route]);

  useEffect(() => {
    navigation.setOptions({
      title: request?.title ?? t('loading'),
      headerRight: () => (
        <View style={{ display: 'flex', flexDirection: 'row' }}>
          {hasDeletePermission(PermissionEntity.REQUESTS, request) && (
            <IconButton
              onPress={() => setOpenDelete(true)}
              icon="delete-outline"
            />
          )}
          {!request?.workOrder &&
            !request?.cancelled &&
            hasEditPermission(PermissionEntity.REQUESTS, request) && (
              <IconButton
                icon={'pencil'}
                onPress={() => navigation.navigate('EditRequest', { request })}
              />
            )}
          {approving ? (
            <ActivityIndicator />
          ) : (
            !request?.workOrder &&
            !request?.cancelled &&
            hasViewPermission(PermissionEntity.SETTINGS) && (
              <IconButton onPress={onApprove} icon="check" />
            )
          )}
        </View>
      )
    });
  }, [request, approving]);

  function BasicField({
    label,
    value
  }: {
    label: string;
    value: string | number;
  }) {
    if (value)
      return (
        <View>
          <View
            style={{
              display: 'flex',
              flexDirection: 'row',
              justifyContent: 'space-between',
              padding: 20
            }}
          >
            <Text>{label}</Text>
            <Text style={{ fontWeight: 'bold' }}>{value}</Text>
          </View>
          <Divider />
        </View>
      );
    else return null;
  }

  function ObjectField({
    label,
    value,
    link
  }: {
    label: string;
    value: string | number;
    link: { route: keyof RootStackParamList; id: number };
  }) {
    if (value) {
      return (
        <TouchableOpacity
          onPress={() => {
            if (user.role.code === 'REQUESTER') return;
            // @ts-ignore
            navigation.navigate(link.route, { id: link.id });
          }}
          style={{ marginTop: 20, padding: 20, backgroundColor: 'white' }}
        >
          <Text variant="titleMedium" style={{ fontWeight: 'bold' }}>
            {label}
          </Text>
          <Text variant="bodyLarge">{value}</Text>
        </TouchableOpacity>
      );
    } else return null;
  }

  const RejectDialog = ({
    open,
    onClose,
    onReject
  }: {
    open: boolean;
    onClose: () => void;
    onReject: () => void;
  }) => {
    const [feedback, setFeedback] = useState<string>('');
    const [rejecting, setRejecting] = useState<boolean>(false);
    return (
      <Portal theme={theme}>
        <Dialog visible={open} onDismiss={onClose}>
          <Dialog.Title>{t('reject')}</Dialog.Title>
          <Dialog.Content>
            <View>
              <TextInput
                style={{ width: '100%' }}
                mode="outlined"
                multiline
                label={t('feedback')}
                onChangeText={(value) => setFeedback(value)}
                value={feedback}
              />
            </View>
          </Dialog.Content>
          <Dialog.Actions>
            <Button
              loading={rejecting}
              disabled={rejecting}
              mode={'contained'}
              onPress={() => {
                setRejecting(true);
                if (!feedback.trim()) {
                  showSnackBar(t('required_feedback'), 'error');
                  setRejecting(false);
                  return;
                }
                dispatch(cancelRequest(request.id, feedback))
                  .then(onReject)
                  .finally(() => setRejecting(false));
              }}
            >
              {t('reject')}
            </Button>
          </Dialog.Actions>
        </Dialog>
      </Portal>
    );
  };

  if (request)
    return (
      <ScrollView style={{ flex: 1, backgroundColor: theme.colors.background }}>
        {renderConfirmDelete()}
        {request.image && (
          <Image style={{ height: 200 }} source={{ uri: request.image.url }} />
        )}
        {fieldsToRender.map((field) => (
          <BasicField
            key={field.label}
            label={field.label}
            value={field.value}
          />
        ))}
        {request.audioDescription && (
          <View style={{ backgroundColor: 'white', padding: 20 }}>
            <Text>{t('audio_description')}</Text>
            <AudioPlayer url={request.audioDescription.url} />
          </View>
        )}
        <ObjectField
          label={t('requested_by')}
          value={getUserNameById(request.createdBy)}
          link={{ route: 'UserDetails', id: request.createdBy }}
        />
        {request.asset && (
          <ObjectField
            label={t('asset')}
            value={request.asset.name}
            link={{ route: 'AssetDetails', id: request.asset.id }}
          />
        )}
        {request.location && (
          <ObjectField
            label={t('location')}
            value={request.location.name}
            link={{ route: 'LocationDetails', id: request.location.id }}
          />
        )}
        {request.primaryUser && (
          <ObjectField
            label={t('primary_worker')}
            value={`${request.primaryUser.firstName} ${request.primaryUser.lastName}`}
            link={{ route: 'UserDetails', id: request.primaryUser.id }}
          />
        )}
        {request.team && (
          <ObjectField
            label={t('team')}
            value={request.team.name}
            link={{ route: 'TeamDetails', id: request.team.id }}
          />
        )}
        {!request.workOrder &&
          !request.cancelled &&
          hasViewPermission(PermissionEntity.SETTINGS) && (
            <Button
              disabled={cancelling}
              loading={cancelling}
              onPress={() => setOpenRejectModal(true)}
              mode="contained"
              style={{ margin: 20 }}
              buttonColor={theme.colors.error}
            >
              {t('reject')}
            </Button>
          )}
        <RejectDialog
          open={openRejectModal}
          onClose={() => setOpenRejectModal(false)}
          onReject={() => navigation.goBack()}
        />
      </ScrollView>
    );
  else return <LoadingDialog visible={true} />;
}
