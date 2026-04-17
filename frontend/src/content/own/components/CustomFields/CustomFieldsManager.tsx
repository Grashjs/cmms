import { Fragment, useContext, useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import ConfirmDialog from '../../components/ConfirmDialog';
import {
  Autocomplete,
  Box,
  Button,
  CircularProgress,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Divider,
  FormControl,
  FormControlLabel,
  Grid,
  IconButton,
  InputLabel,
  List,
  ListItem,
  ListItemText,
  MenuItem,
  Radio,
  Select,
  styled,
  TextField,
  Typography,
  useTheme
} from '@mui/material';
import EditTwoToneIcon from '@mui/icons-material/EditTwoTone';
import ClearTwoToneIcon from '@mui/icons-material/ClearTwoTone';
import AddIcon from '@mui/icons-material/Add';
import DragIndicatorIcon from '@mui/icons-material/DragIndicator';
import { Formik } from 'formik';
import * as Yup from 'yup';
import { TitleContext } from '../../../../contexts/TitleContext';
import { useDispatch, useSelector } from '../../../../store';
import {
  createCustomField,
  deleteCustomField,
  getCustomFields,
  updateCustomField,
  reorderCustomFieldsAPI
} from '../../../../slices/customField';
import useAuth from '../../../../hooks/useAuth';
import {
  CustomField,
  CustomFieldEntityType,
  CustomFieldType
} from '../../../../models/owns/customField';
import { CustomSnackBarContext } from '../../../../contexts/CustomSnackBarContext';
import { PermissionEntity } from '../../../../models/owns/role';
import PermissionErrorMessage from '../../components/PermissionErrorMessage';
import {
  DragDropContext,
  Droppable,
  Draggable,
  DropResult
} from 'react-beautiful-dnd';

const IconButtonWrapper = styled(IconButton)(
  ({ theme }) => `
    transition: ${theme.transitions.create(['transform', 'background'])};
    transform: scale(1);
    transform-origin: center;

    &:hover {
        transform: scale(1.1);
    }
  `
);

const ListWrapper = styled(List)(
  () => `
      .MuiListItem-root:last-of-type + .MuiDivider-root {
          display: none;
      }
  `
);

interface CustomFieldsManagerProps {
  entityType: CustomFieldEntityType;
}

function CustomFieldsManager({ entityType }: CustomFieldsManagerProps) {
  const { t }: { t: any } = useTranslation();
  const theme = useTheme();
  const [openAddModal, setOpenAddModal] = useState<boolean>(false);
  const [openUpdateModal, setOpenUpdateModal] = useState<boolean>(false);
  const [openDelete, setOpenDelete] = useState<boolean>(false);
  const handleOpenAdd = () => setOpenAddModal(true);
  const handleCloseAdd = () => setOpenAddModal(false);
  const { customFields, loadingGet } = useSelector(
    (state) => state.customFields
  );
  const { setTitle } = useContext(TitleContext);
  const dispatch = useDispatch();
  const {
    user,
    hasViewPermission,
    hasEditPermission,
    hasCreatePermission,
    hasDeletePermission
  } = useAuth();
  const [currentCustomField, setCurrentCustomField] = useState<CustomField>();
  const { showSnackBar } = useContext(CustomSnackBarContext);

  const filteredCustomFields = customFields
    .filter((cf) => cf.entityType === entityType)
    .sort((a, b) => a.order - b.order);

  const handleDelete = (id: number) => {
    dispatch(deleteCustomField(id))
      .then(onDeleteSuccess)
      .catch(onDeleteFailure);
    setOpenDelete(false);
  };

  useEffect(() => {
    setTitle(t('custom_fields'));
    dispatch(getCustomFields());
  }, []);

  const onDeleteSuccess = () => {
    showSnackBar(t('changes_saved_success'), 'success');
  };

  const onDeleteFailure = (err) => {
    showSnackBar(t('something_went_wrong'), 'error');
  };

  const onCreateSuccess = () => {
    showSnackBar(t('changes_saved_success'), 'success');
    handleCloseAdd();
  };

  const onCreateFailure = (err) => {
    showSnackBar(t('something_went_wrong'), 'error');
  };

  const onUpdateSuccess = () => {
    showSnackBar(t('changes_saved_success'), 'success');
    setOpenUpdateModal(false);
  };

  const onUpdateFailure = (err) => {
    showSnackBar(t('something_went_wrong'), 'error');
  };

  const handleDragEnd = (result: DropResult) => {
    if (!result.destination) return;

    const items = Array.from(filteredCustomFields);
    const [reorderedItem] = items.splice(result.source.index, 1);
    items.splice(result.destination.index, 0, reorderedItem);

    const ids = items.map((item) => item.id);
    dispatch(reorderCustomFieldsAPI(ids));
  };

  if (!hasViewPermission(PermissionEntity.SETTINGS)) {
    return <PermissionErrorMessage message={t('no_access_settings')} />;
  }

  return (
    <Grid item xs={12}>
      <Box p={4}>
        <Box
          display="flex"
          justifyContent="flex-end"
          alignItems="center"
          mb={3}
        >
          {hasCreatePermission(PermissionEntity.SETTINGS) && (
            <Button
              variant="contained"
              startIcon={<AddIcon />}
              onClick={handleOpenAdd}
            >
              {t('add_custom_field')}
            </Button>
          )}
        </Box>

        {loadingGet ? (
          <Box display="flex" justifyContent="center" p={4}>
            <CircularProgress />
          </Box>
        ) : (
          <DragDropContext onDragEnd={handleDragEnd}>
            <Droppable droppableId="customFieldsList">
              {(provided) => (
                <ListWrapper
                  {...provided.droppableProps}
                  ref={provided.innerRef}
                >
                  {filteredCustomFields.map((customField, index) => (
                    <Draggable
                      key={customField.id}
                      draggableId={String(customField.id)}
                      index={index}
                    >
                      {(provided) => (
                        <Fragment>
                          <ListItem
                            ref={provided.innerRef}
                            {...provided.draggableProps}
                            sx={{
                              display: { xs: 'block', sm: 'flex' },
                              py: 2,
                              px: 0
                            }}
                          >
                            <Box
                              sx={{
                                cursor: 'grab',
                                '&:active': {
                                  cursor: 'grabbing'
                                },
                                mr: 2,
                                display: 'flex',
                                alignItems: 'center'
                              }}
                              {...provided.dragHandleProps}
                            >
                              <DragIndicatorIcon />
                            </Box>
                            <ListItemText
                              primary={
                                <Typography variant="h6" gutterBottom>
                                  {customField.label}
                                </Typography>
                              }
                              secondary={
                                <Typography
                                  variant="body2"
                                  color="text.secondary"
                                >
                                  {t('type')}:{' '}
                                  {t(customField.fieldType.toLowerCase())} |{' '}
                                  {t('required')}:{' '}
                                  {customField.required ? t('yes') : t('no')}
                                  {entityType ===
                                    CustomFieldEntityType.WORK_ORDER &&
                                    customField.copyOnRepeat && (
                                      <>
                                        {' '}
                                        | {t('copy_on_repeat_wo')}: {t('yes')}
                                      </>
                                    )}
                                </Typography>
                              }
                            />
                            <Box
                              display="flex"
                              alignItems="center"
                              justifyContent="flex-end"
                            >
                              {hasViewPermission(PermissionEntity.SETTINGS) && (
                                <IconButtonWrapper
                                  onClick={() => {
                                    setCurrentCustomField(customField);
                                    setOpenUpdateModal(true);
                                  }}
                                  sx={{
                                    '&:hover': {
                                      background: theme.colors.primary.lighter
                                    },
                                    color: theme.palette.primary.main
                                  }}
                                  color="inherit"
                                  size="small"
                                >
                                  <EditTwoToneIcon fontSize="small" />
                                </IconButtonWrapper>
                              )}
                              {hasViewPermission(PermissionEntity.SETTINGS) && (
                                <IconButtonWrapper
                                  onClick={() => {
                                    setCurrentCustomField(customField);
                                    setOpenDelete(true);
                                  }}
                                  sx={{
                                    '&:hover': {
                                      background: theme.colors.error.lighter
                                    },
                                    color: theme.palette.error.main
                                  }}
                                  color="inherit"
                                  size="small"
                                >
                                  <ClearTwoToneIcon fontSize="small" />
                                </IconButtonWrapper>
                              )}
                            </Box>
                          </ListItem>
                          <Divider />
                        </Fragment>
                      )}
                    </Draggable>
                  ))}
                  {provided.placeholder}
                </ListWrapper>
              )}
            </Droppable>
          </DragDropContext>
        )}

        {/* Add Modal */}
        <Dialog
          fullWidth
          maxWidth="sm"
          open={openAddModal}
          onClose={handleCloseAdd}
        >
          <DialogTitle sx={{ p: 3 }}>
            <Typography variant="h4" gutterBottom>
              {t('add_custom_field')}
            </Typography>
          </DialogTitle>
          <Formik
            initialValues={{
              label: '',
              fieldType: CustomFieldType.SHORT_TEXT,
              required: false,
              copyOnRepeat: false,
              options: []
            }}
            validationSchema={Yup.object().shape({
              label: Yup.string().required(t('required_field')),
              fieldType: Yup.string().required(t('required_field'))
            })}
            onSubmit={async (values, { resetForm, setSubmitting }) => {
              try {
                await dispatch(
                  createCustomField({
                    ...values,
                    entityType
                  })
                );
                onCreateSuccess();
                resetForm();
              } catch (err) {
                onCreateFailure(err);
              } finally {
                setSubmitting(false);
              }
            }}
          >
            {({
              errors,
              handleBlur,
              handleChange,
              handleSubmit,
              isSubmitting,
              touched,
              values,
              setFieldValue
            }) => (
              <form onSubmit={handleSubmit}>
                <DialogContent dividers sx={{ p: 3 }}>
                  <Grid container spacing={3}>
                    <Grid item xs={12}>
                      <TextField
                        error={Boolean(touched.label && errors.label)}
                        fullWidth
                        helperText={touched.label && errors.label}
                        label={t('label')}
                        name="label"
                        onBlur={handleBlur}
                        onChange={handleChange}
                        value={values.label}
                        variant="outlined"
                      />
                    </Grid>
                    <Grid item xs={12}>
                      <FormControl fullWidth>
                        <InputLabel>{t('field_type')}</InputLabel>
                        <Select
                          value={values.fieldType}
                          label={t('field_type')}
                          onChange={(e) =>
                            setFieldValue('fieldType', e.target.value)
                          }
                        >
                          {Object.values(CustomFieldType).map((type) => (
                            <MenuItem key={type} value={type}>
                              {t(type.toLowerCase())}
                            </MenuItem>
                          ))}
                        </Select>
                      </FormControl>
                    </Grid>
                    <Grid item xs={12}>
                      <FormControlLabel
                        control={
                          <Radio
                            checked={values.required}
                            onChange={() => setFieldValue('required', true)}
                          />
                        }
                        label={t('required')}
                      />
                      <FormControlLabel
                        control={
                          <Radio
                            checked={!values.required}
                            onChange={() => setFieldValue('required', false)}
                          />
                        }
                        label={t('optional')}
                      />
                    </Grid>
                    {entityType === CustomFieldEntityType.WORK_ORDER && (
                      <Grid item xs={12}>
                        <FormControlLabel
                          control={
                            <Radio
                              checked={values.copyOnRepeat}
                              onChange={() =>
                                setFieldValue('copyOnRepeat', true)
                              }
                            />
                          }
                          label={t('copy_on_repeat_wo')}
                        />
                        <FormControlLabel
                          control={
                            <Radio
                              checked={!values.copyOnRepeat}
                              onChange={() =>
                                setFieldValue('copyOnRepeat', false)
                              }
                            />
                          }
                          label={t('do_not_copy')}
                        />
                      </Grid>
                    )}
                    {values.fieldType === CustomFieldType.SINGLE_CHOICE && (
                      <Grid item xs={12}>
                        <Autocomplete
                          freeSolo
                          multiple
                          options={[]}
                          value={values.options}
                          onChange={(event, newValue) =>
                            setFieldValue('options', newValue)
                          }
                          renderInput={(params) => (
                            <TextField
                              {...params}
                              label={t('options')}
                              helperText={t('type_enter_to_add_option')}
                            />
                          )}
                        />
                      </Grid>
                    )}
                  </Grid>
                </DialogContent>
                <DialogActions sx={{ p: 3 }}>
                  <Button color="secondary" onClick={handleCloseAdd}>
                    {t('cancel')}
                  </Button>
                  <Button
                    type="submit"
                    startIcon={
                      isSubmitting ? <CircularProgress size="1rem" /> : null
                    }
                    disabled={isSubmitting}
                    variant="contained"
                  >
                    {t('add')}
                  </Button>
                </DialogActions>
              </form>
            )}
          </Formik>
        </Dialog>

        {/* Update Modal */}
        <Dialog
          fullWidth
          maxWidth="sm"
          open={openUpdateModal}
          onClose={() => setOpenUpdateModal(false)}
        >
          <DialogTitle sx={{ p: 3 }}>
            <Typography variant="h4" gutterBottom>
              {t('edit_custom_field')}
            </Typography>
          </DialogTitle>
          <Formik
            initialValues={{
              label: currentCustomField?.label || '',
              fieldType:
                currentCustomField?.fieldType || CustomFieldType.SHORT_TEXT,
              required: currentCustomField?.required || false,
              copyOnRepeat: currentCustomField?.copyOnRepeat || false,
              options: currentCustomField?.options || []
            }}
            validationSchema={Yup.object().shape({
              label: Yup.string().required(t('required_field')),
              fieldType: Yup.string().required(t('required_field'))
            })}
            onSubmit={async (values, { setSubmitting }) => {
              try {
                await dispatch(
                  updateCustomField(currentCustomField.id, values)
                );
                onUpdateSuccess();
              } catch (err) {
                onUpdateFailure(err);
              } finally {
                setSubmitting(false);
              }
            }}
          >
            {({
              errors,
              handleBlur,
              handleChange,
              handleSubmit,
              isSubmitting,
              touched,
              values,
              setFieldValue
            }) => (
              <form onSubmit={handleSubmit}>
                <DialogContent dividers sx={{ p: 3 }}>
                  <Grid container spacing={3}>
                    <Grid item xs={12}>
                      <TextField
                        error={Boolean(touched.label && errors.label)}
                        fullWidth
                        helperText={touched.label && errors.label}
                        label={t('label')}
                        name="label"
                        onBlur={handleBlur}
                        onChange={handleChange}
                        value={values.label}
                        variant="outlined"
                      />
                    </Grid>
                    <Grid item xs={12}>
                      <FormControl fullWidth>
                        <InputLabel>{t('field_type')}</InputLabel>
                        <Select
                          value={values.fieldType}
                          label={t('field_type')}
                          onChange={(e) =>
                            setFieldValue('fieldType', e.target.value)
                          }
                        >
                          {Object.values(CustomFieldType).map((type) => (
                            <MenuItem key={type} value={type}>
                              {t(type.toLowerCase())}
                            </MenuItem>
                          ))}
                        </Select>
                      </FormControl>
                    </Grid>
                    <Grid item xs={12}>
                      <FormControlLabel
                        control={
                          <Radio
                            checked={values.required}
                            onChange={() => setFieldValue('required', true)}
                          />
                        }
                        label={t('required')}
                      />
                      <FormControlLabel
                        control={
                          <Radio
                            checked={!values.required}
                            onChange={() => setFieldValue('required', false)}
                          />
                        }
                        label={t('optional')}
                      />
                    </Grid>
                    {entityType === CustomFieldEntityType.WORK_ORDER && (
                      <Grid item xs={12}>
                        <FormControlLabel
                          control={
                            <Radio
                              checked={values.copyOnRepeat}
                              onChange={() =>
                                setFieldValue('copyOnRepeat', true)
                              }
                            />
                          }
                          label={t('copy_on_repeat_wo')}
                        />
                        <FormControlLabel
                          control={
                            <Radio
                              checked={!values.copyOnRepeat}
                              onChange={() =>
                                setFieldValue('copyOnRepeat', false)
                              }
                            />
                          }
                          label={t('do_not_copy')}
                        />
                      </Grid>
                    )}
                    {values.fieldType === CustomFieldType.SINGLE_CHOICE && (
                      <Grid item xs={12}>
                        <Autocomplete
                          freeSolo
                          multiple
                          options={[]}
                          value={values.options}
                          onChange={(event, newValue) =>
                            setFieldValue('options', newValue)
                          }
                          renderInput={(params) => (
                            <TextField
                              {...params}
                              label={t('options')}
                              helperText={t('type_enter_to_add_option')}
                            />
                          )}
                        />
                      </Grid>
                    )}
                  </Grid>
                </DialogContent>
                <DialogActions sx={{ p: 3 }}>
                  <Button
                    color="secondary"
                    onClick={() => setOpenUpdateModal(false)}
                  >
                    {t('cancel')}
                  </Button>
                  <Button
                    type="submit"
                    startIcon={
                      isSubmitting ? <CircularProgress size="1rem" /> : null
                    }
                    disabled={isSubmitting}
                    variant="contained"
                  >
                    {t('save')}
                  </Button>
                </DialogActions>
              </form>
            )}
          </Formik>
        </Dialog>

        {/* Delete Dialog */}
        <ConfirmDialog
          open={openDelete}
          onCancel={() => setOpenDelete(false)}
          onConfirm={() => handleDelete(currentCustomField.id)}
          confirmText={t('delete')}
          question={t('confirm_delete_custom_field')}
        />
      </Box>
    </Grid>
  );
}

export default CustomFieldsManager;
