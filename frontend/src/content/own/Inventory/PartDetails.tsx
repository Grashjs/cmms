import {
  Box,
  Divider,
  Grid,
  IconButton,
  Link,
  List,
  ListItem,
  ListItemButton,
  ListItemText,
  Stack,
  Tab,
  Tabs,
  Typography,
  useTheme
} from '@mui/material';
import { ChangeEvent, useContext, useState } from 'react';
import { useTranslation } from 'react-i18next';
import EditTwoToneIcon from '@mui/icons-material/EditTwoTone';
import DeleteTwoToneIcon from '@mui/icons-material/DeleteTwoTone';
import Part from '../../../models/owns/part';
import { CompanySettingsContext } from '../../../contexts/CompanySettingsContext';
import { PermissionEntity } from '../../../models/owns/role';
import useAuth from '../../../hooks/useAuth';
import ImageViewer from 'react-simple-image-viewer';
import {
  getAssetUrl,
  getCustomerUrl,
  getTeamUrl,
  getUserUrl,
  getVendorUrl,
  getWorkOrderUrl
} from '../../../utils/urlPaths';
import { editPart } from '../../../slices/part';
import { useDispatch, useSelector } from '../../../store';
import FilesList from '../components/FilesList';
import { getAssetsByPart } from '../../../slices/asset';
import { useNavigate } from 'react-router-dom';
import { getWorkOrdersByPart } from '../../../slices/workOrder';
import { getFormattedQuantityWithUnit } from './Parts';
import { getFormattedCostPerUnit } from '../../../utils/formatters';

interface PartDetailsProps {
  part: Part;
  handleOpenUpdate: () => void;
  handleOpenDelete: () => void;
}
export default function PartDetails(props: PartDetailsProps) {
  const { part, handleOpenUpdate, handleOpenDelete } = props;
  const { hasEditPermission, hasDeletePermission } = useAuth();
  const { t }: { t: any } = useTranslation();
  const { getFormattedDate, getFormattedCurrency } = useContext(
    CompanySettingsContext
  );
  const dispatch = useDispatch();
  const [currentTab, setCurrentTab] = useState<string>('details');
  const [isImageViewerOpen, setIsImageViewerOpen] = useState<boolean>(false);
  const theme = useTheme();
  const { assetsByPart } = useSelector((state) => state.assets);
  const { workOrdersByPart } = useSelector((state) => state.workOrders);
  const navigate = useNavigate();
  const assets = assetsByPart[part?.id] ?? [];
  const workOrders = workOrdersByPart[part?.id] ?? [];
  const tabs = [
    { value: 'details', label: t('details') },
    { value: 'assets', label: t('assets') },
    { value: 'files', label: t('files') },
    { value: 'workOrders', label: t('work_orders') }
    //TODO events
  ];
  const handleTabsChange = (_event: ChangeEvent<{}>, value: string): void => {
    setCurrentTab(value);
    if (value === 'assets' && !assets.length) {
      dispatch(getAssetsByPart(part.id));
    } else if (value === 'workOrders' && !workOrders.length) {
      dispatch(getWorkOrdersByPart(part.id));
    }
  };
  const BasicField = ({
    label,
    value
  }: {
    label: string | number;
    value: string | number;
  }) => {
    return value ? (
      <Grid item xs={12} lg={6}>
        <Typography variant="h6" sx={{ color: theme.colors.alpha.black[70] }}>
          {label}
        </Typography>
        <Typography variant="h6">{value}</Typography>
      </Grid>
    ) : null;
  };
  const firstFieldsToRender = (part: Part): { label: string; value: any }[] => [
    {
      label: t('name'),
      value: part.name
    },
    {
      label: t('id'),
      value: part.id
    },
    {
      label: t('description'),
      value: part.description
    },
    {
      label: t('category'),
      value: part.category?.name
    },
    {
      label: t('additional_information'),
      value: part.additionalInfos
    },
    {
      label: t('cost'),
      value: getFormattedCostPerUnit(part.cost, part.unit, getFormattedCurrency)
    },
    {
      label: t('quantity'),
      value: getFormattedQuantityWithUnit(part.quantity, part.unit)
    },
    {
      label: t('minimum_quantity'),
      value: getFormattedQuantityWithUnit(part.minQuantity, part.unit)
    },
    {
      label: t('barcode'),
      value: part.barcode
    }
  ];
  const areaFieldsToRender = (part: Part): { label: string; value: any }[] => [
    {
      label: t('area'),
      value: part.area
    }
  ];

  return (
    <Grid
      container
      justifyContent="center"
      alignItems="stretch"
      spacing={2}
      padding={4}
    >
      <Grid
        item
        xs={12}
        display="flex"
        flexDirection="row"
        justifyContent="space-between"
      >
        <Box>
          <Typography variant="h2">{part?.name}</Typography>
          <Typography variant="h6">{part?.description}</Typography>
        </Box>
        <Box>
          {hasEditPermission(PermissionEntity.PARTS_AND_MULTIPARTS, part) && (
            <IconButton onClick={handleOpenUpdate} style={{ marginRight: 10 }}>
              <EditTwoToneIcon color="primary" />
            </IconButton>
          )}
          {hasDeletePermission(PermissionEntity.PARTS_AND_MULTIPARTS, part) && (
            <IconButton onClick={handleOpenDelete}>
              <DeleteTwoToneIcon style={{ cursor: 'pointer' }} color="error" />
            </IconButton>
          )}
        </Box>
      </Grid>
      <Divider />
      <Grid item xs={12}>
        <Tabs
          onChange={handleTabsChange}
          value={currentTab}
          variant="scrollable"
          scrollButtons="auto"
          textColor="primary"
          indicatorColor="primary"
        >
          {tabs.map((tab) => (
            <Tab key={tab.value} label={tab.label} value={tab.value} />
          ))}
        </Tabs>
      </Grid>
      <Grid item xs={12}>
        {currentTab === 'details' && (
          <Box>
            <Grid container spacing={2}>
              {part.image && (
                <Grid
                  item
                  xs={12}
                  lg={12}
                  display="flex"
                  justifyContent="center"
                >
                  <img
                    src={part.image.url}
                    style={{ borderRadius: 5, height: 250, cursor: 'pointer' }}
                    onClick={() => setIsImageViewerOpen(true)}
                  />
                </Grid>
              )}
              <Grid item xs={12} lg={12}>
                <Typography sx={{ mb: 1 }} variant="h4">
                  {t('part_details')}
                </Typography>
                <Grid container spacing={2}>
                  {firstFieldsToRender(part).map((field) => (
                    <BasicField
                      key={field.label}
                      label={field.label}
                      value={field.value}
                    />
                  ))}
                </Grid>
              </Grid>
              <Grid item xs={12} lg={12}>
                <Typography sx={{ mt: 2, mb: 1 }} variant="h4">
                  {t('area_details')}
                </Typography>
                <Grid container spacing={2}>
                  {areaFieldsToRender(part).map((field) => (
                    <BasicField
                      key={field.label}
                      label={field.label}
                      value={field.value}
                    />
                  ))}
                </Grid>
              </Grid>
              <Grid item xs={12} lg={12}>
                <Typography sx={{ mt: 2, mb: 1 }} variant="h4">
                  {t('assigned_people')}
                </Typography>
                <Grid container spacing={2}>
                  {!!part.assignedTo.length && (
                    <Grid item xs={12} lg={6}>
                      <Typography
                        variant="h6"
                        sx={{ color: theme.colors.alpha.black[70] }}
                      >
                        Assigned users
                      </Typography>
                      {part.assignedTo.map((user) => (
                        <Link
                          key={user.id}
                          href={getUserUrl(user.id)}
                          variant="h6"
                        >{`${user.firstName} ${user.lastName}`}</Link>
                      ))}
                    </Grid>
                  )}
                  {!!part.customers.length && (
                    <Grid item xs={12} lg={6}>
                      <Typography
                        variant="h6"
                        sx={{ color: theme.colors.alpha.black[70] }}
                      >
                        {t('assigned_customers')}
                      </Typography>
                      {part.customers.map((customer) => (
                        <Link
                          key={customer.id}
                          href={getCustomerUrl(customer.id)}
                          variant="h6"
                        >
                          {customer.name}
                        </Link>
                      ))}
                    </Grid>
                  )}
                  {!!part.vendors.length && (
                    <Grid item xs={12} lg={6}>
                      <Typography
                        variant="h6"
                        sx={{ color: theme.colors.alpha.black[70] }}
                      >
                        {t('assigned_vendors')}
                      </Typography>
                      {part.vendors.map((vendor) => (
                        <Link
                          key={vendor.id}
                          href={getVendorUrl(vendor.id)}
                          variant="h6"
                        >
                          {vendor.companyName}
                        </Link>
                      ))}
                    </Grid>
                  )}
                  {!!part.teams.length && (
                    <Grid item xs={12} lg={6}>
                      <Typography
                        variant="h6"
                        sx={{ color: theme.colors.alpha.black[70] }}
                      >
                        {t('assigned_teams')}
                      </Typography>
                      {part.teams.map((team) => (
                        <Link
                          key={team.id}
                          href={getTeamUrl(team.id)}
                          variant="h6"
                        >
                          {team.name}
                        </Link>
                      ))}
                    </Grid>
                  )}
                </Grid>
              </Grid>
            </Grid>
          </Box>
        )}
        {currentTab === 'assets' && (
          <Box>
            {assets.length ? (
              <List sx={{ width: '100%' }}>
                {assets.map((asset) => (
                  <ListItemButton
                    key={asset.id}
                    divider
                    onClick={() => navigate(getAssetUrl(asset.id))}
                  >
                    <ListItem
                      secondaryAction={
                        <Typography>
                          {getFormattedDate(asset.createdAt)}
                        </Typography>
                      }
                    >
                      <ListItemText
                        primary={asset.name}
                        secondary={t(asset.status)}
                      />
                    </ListItem>
                  </ListItemButton>
                ))}
              </List>
            ) : (
              <Stack direction="row" justifyContent="center" width="100%">
                <Typography variant="h5">
                  {t('no_asset_related_part')}
                </Typography>
              </Stack>
            )}
          </Box>
        )}
        {currentTab === 'files' && (
          <Box>
            {/*<Box display="flex" justifyContent="right">*/}
            {/*  <Button startIcon={<AddTwoToneIcon fontSize="small" />}>*/}
            {/*    {t('file')}*/}
            {/*  </Button>*/}
            {/*</Box>*/}
            <Box sx={{ width: '100%' }}>
              {part.files.length ? (
                <FilesList
                  confirmMessage={t(
                    'Are you sure you want to remove this file from this Part ?'
                  )}
                  files={part.files}
                  removeDisabled={
                    !hasEditPermission(
                      PermissionEntity.PARTS_AND_MULTIPARTS,
                      part
                    )
                  }
                  onRemove={(id: number) => {
                    dispatch(
                      editPart(part.id, {
                        ...part,
                        files: part.files.filter((f) => f.id !== id)
                      })
                    );
                  }}
                />
              ) : (
                <Stack direction="row" justifyContent="center" width="100%">
                  <Typography variant="h5">{t('no_file_found')}</Typography>
                </Stack>
              )}
            </Box>
          </Box>
        )}
        {currentTab === 'workOrders' && (
          <Box>
            {workOrders.length ? (
              <List sx={{ width: '100%' }}>
                {workOrders.map((workOrder) => (
                  <ListItemButton
                    key={workOrder.id}
                    divider
                    onClick={() => navigate(getWorkOrderUrl(workOrder.id))}
                  >
                    <ListItem
                      secondaryAction={
                        <Typography>
                          {getFormattedDate(workOrder.createdAt)}
                        </Typography>
                      }
                    >
                      <ListItemText
                        primary={workOrder.title}
                        secondary={t(workOrder.status)}
                      />
                    </ListItem>
                  </ListItemButton>
                ))}
              </List>
            ) : (
              <Stack direction="row" justifyContent="center" width="100%">
                <Typography variant="h5">{t('no_wo_found')}</Typography>
              </Stack>
            )}
          </Box>
        )}
      </Grid>
      {isImageViewerOpen && (
        <div style={{ zIndex: 100 }}>
          <ImageViewer
            src={[part.image.url]}
            currentIndex={0}
            onClose={() => setIsImageViewerOpen(false)}
            disableScroll={true}
            backgroundStyle={{
              backgroundColor: 'rgba(0,0,0,0.9)'
            }}
            closeOnClickOutside={true}
          />
        </div>
      )}
    </Grid>
  );
}
