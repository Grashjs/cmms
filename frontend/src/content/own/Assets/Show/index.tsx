import { useContext, useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import MultipleTabsLayout from '../../components/MultipleTabsLayout';
import { TitleContext } from '../../../../contexts/TitleContext';
import { useLocation, useNavigate, useParams } from 'react-router-dom';
import Asset, { AssetDTO } from '../../../../models/owns/asset';
import AssetWorkOrders from './AssetWorkOrders';
import AssetDetails from './AssetDetails';
import AssetParts from './AssetParts';
import AssetFiles from './AssetFiles';
import { isNumeric } from 'src/utils/validators';
import { IField } from '../../type';
import {
  Box,
  Dialog,
  DialogContent,
  DialogTitle,
  Typography
} from '@mui/material';
import Form from '../../components/form';
import * as Yup from 'yup';
import {
  addAsset,
  deleteAsset,
  editAsset,
  getAssetDetails
} from '../../../../slices/asset';
import { useDispatch, useSelector } from '../../../../store';
import { CustomSnackBarContext } from '../../../../contexts/CustomSnackBarContext';
import { formatAssetValues } from '../../../../utils/formatters';
import { CompanySettingsContext } from '../../../../contexts/CompanySettingsContext';
import { PermissionEntity } from '../../../../models/owns/role';
import PermissionErrorMessage from '../../components/PermissionErrorMessage';
import useAuth from '../../../../hooks/useAuth';
import DeleteTwoToneIcon from '@mui/icons-material/DeleteTwoTone';
import ConfirmDialog from '../../components/ConfirmDialog';
import AssetMeters from './AssetMeters';
import { handleFileUpload } from '../../../../utils/overall';
import AssetDowntimes from './AssetDowntimes';
import AssetAnalytics from './AssetAnalytics';
import { getCustomFields } from '../../../../slices/customField';
import { CustomFieldEntityType } from '../../../../models/owns/customField';
import {
  getCustomFieldsIFields,
  getCustomFieldsRequiredShape,
  getCustomFieldsValues
} from '../../type';
import { getAssetUrl } from '../../../../utils/urlPaths';

interface PropsType {}

const ShowAsset = ({}: PropsType) => {
  const { t }: { t: any } = useTranslation();
  const { assetId } = useParams();
  const [openUpdateModal, setOpenUpdateModal] = useState<boolean>(false);
  const [copyAssetData, setCopyAssetData] = useState<AssetDTO | null>(null);
  const { setTitle } = useContext(TitleContext);
  const { uploadFiles } = useContext(CompanySettingsContext);
  const location = useLocation();
  const { showSnackBar } = useContext(CustomSnackBarContext);
  const { assetInfos, loadingGet } = useSelector((state) => state.assets);
  const { customFields } = useSelector((state) => state.customFields);
  const asset: AssetDTO = assetInfos[assetId]?.asset;
  const navigate = useNavigate();
  const [openDelete, setOpenDelete] = useState<boolean>(false);
  const {
    hasViewPermission,
    hasEditPermission,
    hasDeletePermission,
    getFilteredFields
  } = useAuth();
  const dispatch = useDispatch();

  useEffect(() => {
    if (isNumeric(assetId)) dispatch(getAssetDetails(Number(assetId)));
  }, [assetId]);

  useEffect(() => {
    if (openUpdateModal && !customFields.length) {
      dispatch(getCustomFields());
    }
  }, [openUpdateModal]);

  const handleOpenUpdateModal = () => setOpenUpdateModal(true);
  const handleCopyAsset = () => {
    setCopyAssetData(asset);
    setOpenUpdateModal(true);
  };
  const handleCloseUpdateModal = () => {
    setOpenUpdateModal(false);
    setCopyAssetData(null);
  };

  const handleDelete = () => {
    dispatch(deleteAsset(asset.id))
      .then(onDeleteSuccess)
      .catch(onDeleteFailure);
  };
  const getAssetFormValues = (entity: AssetDTO) => ({
    ...entity,
    location: entity?.location
      ? {
          label: entity?.location.name,
          value: entity?.location.id
        }
      : null,
    category: entity?.category
      ? {
          label: entity.category.name,
          value: entity.category.id
        }
      : null,
    primaryUser: entity?.primaryUser
      ? {
          label: `${entity?.primaryUser.firstName} ${entity?.primaryUser.lastName}`,
          value: entity?.primaryUser.id
        }
      : null,
    assignedTo:
      entity?.assignedTo?.map((user) => {
        return {
          label: `${user.firstName} ${user.lastName}`,
          value: user.id
        };
      }) ?? [],
    customers: entity?.customers?.map((customer) => {
      return {
        label: customer.name,
        value: customer.id
      };
    }),
    vendors: entity?.vendors?.map((vendor) => {
      return {
        label: vendor.companyName,
        value: vendor.id
      };
    }),
    teams: entity?.teams?.map((team) => {
      return {
        label: team.name,
        value: team.id
      };
    }),
    parts:
      entity?.parts?.map((part) => {
        return {
          label: part.name,
          value: part.id
        };
      }) ?? [],
    parentAsset: entity?.parentAsset
      ? {
          label: entity.parentAsset.name,
          value: entity.parentAsset.id
        }
      : null,
    ...getCustomFieldsValues(entity)
  });

  useEffect(() => {
    setTitle(asset?.name);
  }, [asset]);

  const arr = location.pathname.split('/');

  const tabs = [
    { value: 'details', label: t('details') },
    { value: 'work-orders', label: t('work_orders') },
    { value: 'parts', label: t('parts') },
    { value: 'files', label: t('files') },
    { value: 'meters', label: t('meters') },
    { value: 'downtimes', label: t('downtimes') },
    { value: 'analytics', label: t('Statistics') }
  ];
  const tabIndex = tabs.findIndex((tab) => tab.value === arr[arr.length - 1]);
  const onDeleteSuccess = () => {
    showSnackBar(t('asset_remove_success'), 'success');
    navigate('/app/assets');
  };
  const onDeleteFailure = (err) =>
    showSnackBar(t('asset_remove_failure'), 'error');
  const defaultFields: Array<IField> = [
    {
      name: 'assetInfo',
      type: 'titleGroupField',
      label: t('asset_information')
    },
    {
      name: 'name',
      type: 'text',
      label: t('name'),
      placeholder: t('asset_name_description'),
      required: true
    },
    {
      name: 'location',
      type: 'select',
      type2: 'location',
      label: t('location'),
      placeholder: t('select_asset_location'),
      required: true,
      midWidth: true
    },
    {
      name: 'acquisitionCost',
      type: 'number',
      label: t('acquisition_cost'),
      placeholder: t('acquisition_cost'),
      midWidth: true
    },
    {
      name: 'description',
      type: 'text',
      label: t('description'),
      placeholder: t('description'),
      multiple: true
    },
    {
      name: 'manufacturer',
      type: 'text',
      label: t('manufacturer'),
      placeholder: t('manufacturer'),
      midWidth: true
    },
    {
      name: 'power',
      type: 'text',
      label: t('power'),
      placeholder: t('power'),
      midWidth: true
    },
    {
      name: 'model',
      type: 'text',
      label: t('model'),
      placeholder: t('model'),
      midWidth: true
    },
    {
      name: 'barCode',
      type: 'text',
      label: t('barcode'),
      placeholder: t('barcode'),
      midWidth: true
    },
    {
      name: 'serialNumber',
      type: 'text',
      label: t('serial_number'),
      placeholder: t('serial_number'),
      midWidth: true
    },
    {
      name: 'category',
      midWidth: true,
      label: t('category'),
      placeholder: t('category'),
      type: 'select',
      type2: 'category',
      category: 'asset-categories'
    },
    {
      name: 'area',
      type: 'text',
      midWidth: true,
      label: t('area'),
      placeholder: t('area')
    },
    {
      name: 'image',
      type: 'file',
      fileType: 'image',
      label: t('image')
    },
    {
      name: 'assignedTo',
      type: 'titleGroupField',
      label: t('assigned_to')
    },
    {
      name: 'primaryUser',
      type: 'select',
      type2: 'user',
      label: t('worker'),
      placeholder: t('primary_user_description')
    },
    {
      name: 'assignedTo',
      type: 'select',
      type2: 'user',
      multiple: true,
      label: t('additional_workers'),
      placeholder: 'Select additional workers'
    },
    {
      name: 'teams',
      type: 'select',
      type2: 'team',
      multiple: true,
      label: t('teams'),
      placeholder: t('teams_description')
    },
    {
      name: 'moreInfos',
      type: 'titleGroupField',
      label: t('more_informations')
    },
    {
      name: 'customers',
      type: 'select',
      type2: 'customer',
      multiple: true,
      label: t('customers'),
      placeholder: t('customers_description')
    },
    {
      name: 'vendors',
      type: 'select',
      type2: 'vendor',
      multiple: true,
      label: t('vendors'),
      placeholder: t('vendors_description')
    },
    {
      name: 'inServiceDate',
      type: 'date',
      midWidth: true,
      label: t('inServiceDate_description')
    },
    {
      name: 'warrantyExpirationDate',
      type: 'date',
      midWidth: true,
      label: t('warranty_expiration_date')
    },
    {
      name: 'additionalInfos',
      type: 'text',
      label: t('additional_information'),
      placeholder: t('additional_information'),
      multiple: true
    },
    {
      name: 'files',
      type: 'file',
      multiple: true,
      label: t('files'),
      fileType: 'file'
    },
    {
      name: 'structure',
      type: 'titleGroupField',
      label: t('structure')
    },
    { name: 'parts', type: 'select', type2: 'part', label: t('parts') },
    {
      name: 'parentAsset',
      type: 'select',
      type2: 'asset',
      label: t('parent_asset'),
      excluded: Number(assetId)
    },
    ...getCustomFieldsIFields(customFields, CustomFieldEntityType.ASSET)
  ];

  const shape = {
    name: Yup.string().required(t('required_asset_name')),
    ...getCustomFieldsRequiredShape(
      customFields,
      CustomFieldEntityType.ASSET,
      t
    )
  };
  const onEditSuccess = () => {
    setOpenUpdateModal(false);
    setCopyAssetData(null);
    showSnackBar(t('changes_saved_success'), 'success');
  };
  const onEditFailure = (err) =>
    showSnackBar(t('asset_update_failure'), 'error');
  const onCopySuccess = (id: number) => {
    setOpenUpdateModal(false);
    setCopyAssetData(null);
    navigate(getAssetUrl(id));
  };
  const onCopyFailure = (err) =>
    showSnackBar(t('asset_create_failure'), 'error');

  const renderAssetUpdateModal = () => (
    <Dialog
      fullWidth
      maxWidth="md"
      open={openUpdateModal}
      onClose={handleCloseUpdateModal}
    >
      <DialogTitle
        sx={{
          p: 3
        }}
      >
        <Typography variant="h4" gutterBottom>
          {copyAssetData ? t('copy_asset') : t('edit_asset')}
        </Typography>
        <Typography variant="subtitle2">
          {copyAssetData
            ? t('copy_asset_description')
            : t('edit_asset_description')}
        </Typography>
      </DialogTitle>
      <DialogContent
        dividers
        sx={{
          p: 3
        }}
      >
        <Box>
          <Form
            fields={getFilteredFields(defaultFields)}
            validation={Yup.object().shape(shape)}
            submitText={copyAssetData ? t('add') : t('save')}
            values={
              copyAssetData
                ? { ...getAssetFormValues(copyAssetData), id: null }
                : getAssetFormValues(asset)
            }
            onChange={({ field, e }) => {}}
            onSubmit={async (values) => {
              let formattedValues = formatAssetValues(values);
              try {
                const imageAndFiles = await handleFileUpload(
                  {
                    files: formattedValues.files,
                    image: formattedValues.image
                  },
                  uploadFiles
                );

                formattedValues = {
                  ...formattedValues,
                  image: imageAndFiles.image,
                  files: imageAndFiles.files
                };

                if (copyAssetData) {
                  const newAsset: AssetDTO = await dispatch(
                    addAsset(formattedValues)
                  );
                  await onCopySuccess(newAsset.id);
                } else {
                  await dispatch(editAsset(Number(assetId), formattedValues));
                  await onEditSuccess();
                }
              } catch (err) {
                if (copyAssetData) {
                  onCopyFailure(err);
                } else {
                  onEditFailure(err);
                }
                throw err;
              }
            }}
          />
        </Box>
      </DialogContent>
    </Dialog>
  );
  if (hasViewPermission(PermissionEntity.ASSETS))
    return (
      <MultipleTabsLayout
        basePath={`/app/assets/${assetId}`}
        tabs={tabs}
        tabIndex={tabIndex}
        title={`Asset`}
        action={
          hasEditPermission(PermissionEntity.ASSETS, asset)
            ? handleOpenUpdateModal
            : null
        }
        actionTitle={t('edit')}
        secondAction={() => {
          setOpenDelete(true);
        }}
        secondActionTitle={
          hasDeletePermission(PermissionEntity.ASSETS, asset)
            ? t('to_delete')
            : null
        }
        secondActionIcon={<DeleteTwoToneIcon />}
        withoutCard
        editAction
      >
        {isNumeric(assetId) ? (
          tabIndex === 0 ? (
            <AssetDetails
              asset={asset}
              loading={loadingGet}
              onCopy={handleCopyAsset}
            />
          ) : tabIndex === 1 ? (
            <AssetWorkOrders asset={asset} />
          ) : tabIndex === 2 ? (
            <AssetParts asset={asset} />
          ) : tabIndex === 3 ? (
            <AssetFiles asset={asset} />
          ) : tabIndex === 4 ? (
            <AssetMeters asset={asset} />
          ) : tabIndex === 5 ? (
            <AssetDowntimes asset={asset} />
          ) : (
            tabIndex === 6 && <AssetAnalytics id={Number(assetId)} />
          )
        ) : null}
        <ConfirmDialog
          open={openDelete}
          onCancel={() => {
            setOpenDelete(false);
          }}
          onConfirm={handleDelete}
          confirmText={t('to_delete')}
          question={t('confirm_delete_asset')}
        />
        {renderAssetUpdateModal()}
      </MultipleTabsLayout>
    );
  else return <PermissionErrorMessage message={'no_access_assets'} />;
};

export default ShowAsset;
