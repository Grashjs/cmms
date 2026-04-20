import { Fragment, ReactNode, useContext, useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import MultipleTabsLayout from '../components/MultipleTabsLayout';
import ConfirmDialog from '../components/ConfirmDialog';
import {
  Box,
  Button,
  CircularProgress,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Divider,
  Grid,
  IconButton,
  List,
  ListItem,
  ListItemText,
  styled,
  TextField,
  Typography,
  useTheme
} from '@mui/material';
import EditTwoToneIcon from '@mui/icons-material/EditTwoTone';
import ClearTwoToneIcon from '@mui/icons-material/ClearTwoTone';
import { Formik } from 'formik';
import * as Yup from 'yup';
import { TitleContext } from '../../../contexts/TitleContext';
import { useDispatch, useSelector } from '../../../store';
import {
  addCategory,
  deleteCategory,
  editCategory,
  getCategories
} from '../../../slices/category';
import useAuth from '../../../hooks/useAuth';
import Category from '../../../models/owns/category';
import { CustomSnackBarContext } from '../../../contexts/CustomSnackBarContext';
import { PermissionEntity } from '../../../models/owns/role';
import PermissionErrorMessage from '../components/PermissionErrorMessage';

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

interface CategoriesLayoutProps {
  children?: ReactNode;
  tabIndex: number;
  basePath: string;
}

function CategoriesLayout(props: CategoriesLayoutProps) {
  const { children, tabIndex, basePath } = props;
  const { t }: { t: any } = useTranslation();
  const theme = useTheme();
  const [openAddCategoryModal, setOpenAddCategoryModal] =
    useState<boolean>(false);
  const [openUpdateCategoryModal, setOpenUpdateCategoryModal] =
    useState<boolean>(false);
  const [openDelete, setOpenDelete] = useState<boolean>(false);
  const handleOpenAdd = () => setOpenAddCategoryModal(true);
  const handleCloseAdd = () => setOpenAddCategoryModal(false);
  const { categories, loading } = useSelector((state) => state.categories);
  const { setTitle } = useContext(TitleContext);
  const dispatch = useDispatch();
  const {
    user,
    hasViewPermission,
    hasEditPermission,
    hasCreatePermission,
    hasDeletePermission
  } = useAuth();
  const { companySettingsId } = user;
  const [currentCategory, setCurrentCategory] = useState<Category>();
  const { showSnackBar } = useContext(CustomSnackBarContext);

  const handleDelete = (id: number) => {
    dispatch(deleteCategory(id, basePath))
      .then(onDeleteSuccess)
      .catch(onDeleteFailure);
    setOpenDelete(false);
  };
  useEffect(() => {
    setTitle(t('categories'));
    if (hasViewPermission(PermissionEntity.CATEGORIES_WEB))
      dispatch(getCategories(basePath));
  }, []);

  const onCreationSuccess = () => {
    handleCloseAdd();
    showSnackBar(t('category_create_success'), 'success');
  };
  const onCreationFailure = (err) =>
    showSnackBar(t('category_create_failure'), 'error');
  const onEditSuccess = () => {
    setOpenUpdateCategoryModal(false);
    showSnackBar(t('changes_saved_success'), 'success');
  };
  const onEditFailure = (err) =>
    showSnackBar(t('category_edit_failure'), 'error');
  const onDeleteSuccess = () => {
    showSnackBar(t('category_delete_success'), 'success');
  };
  const onDeleteFailure = (err) =>
    showSnackBar(t('category_delete_failure'), 'error');

  const tabs = [
    { value: '', label: t('work_orders') },
    { value: 'asset', label: t('assets') },
    { value: 'meter', label: t('meters') },
    { value: 'time', label: t('timers') },
    { value: 'cost', label: t('costs') },
    { value: 'part', label: t('parts') },
    { value: 'purchase-order', label: t('purchase_orders') }
  ];
  const renderModal = () => (
    <Dialog
      fullWidth
      maxWidth="xs"
      open={openAddCategoryModal}
      onClose={handleCloseAdd}
    >
      <DialogTitle
        sx={{
          p: 3
        }}
      >
        <Typography variant="h4" gutterBottom>
          {t('add_category')}
        </Typography>
        <Typography variant="subtitle2">
          {t('add_category_description')}
        </Typography>
      </DialogTitle>
      <Formik
        initialValues={{ name: '', description: null }}
        validationSchema={Yup.object().shape({
          name: Yup.string().max(30).required(t('required_name'))
        })}
        onSubmit={async (
          values,
          { resetForm, setErrors, setStatus, setSubmitting }
        ) => {
          const formattedValues = {
            ...values,
            companySettings: { id: companySettingsId }
          };
          return dispatch(addCategory(formattedValues, basePath))
            .then(onCreationSuccess)
            .catch(onCreationFailure);
        }}
      >
        {({
          errors,
          handleBlur,
          handleChange,
          handleSubmit,
          isSubmitting,
          touched,
          values
        }) => (
          <form onSubmit={handleSubmit}>
            <DialogContent
              dividers
              sx={{
                p: 3
              }}
            >
              <Grid container spacing={3}>
                <Grid item xs={12} lg={12}>
                  <Grid container spacing={3}>
                    <Grid item xs={12}>
                      <TextField
                        error={Boolean(touched.name && errors.name)}
                        fullWidth
                        helperText={touched.name && errors.name}
                        label={t('name')}
                        name="name"
                        onBlur={handleBlur}
                        onChange={handleChange}
                        value={values.name}
                        variant="outlined"
                      />
                    </Grid>
                    <Grid item xs={12}>
                      <TextField
                        fullWidth
                        label={t('description')}
                        multiline
                        rows={3}
                        name="description"
                        onBlur={handleBlur}
                        onChange={handleChange}
                        value={values.description}
                        variant="outlined"
                      />
                    </Grid>
                  </Grid>
                </Grid>
              </Grid>
            </DialogContent>
            <DialogActions
              sx={{
                p: 3
              }}
            >
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
                {t('add_category')}
              </Button>
            </DialogActions>
          </form>
        )}
      </Formik>
    </Dialog>
  );
  const renderUpdateModal = () => (
    <Dialog
      fullWidth
      maxWidth="xs"
      open={openUpdateCategoryModal}
      onClose={() => setOpenUpdateCategoryModal(false)}
    >
      <DialogTitle
        sx={{
          p: 3
        }}
      >
        <Typography variant="h4" gutterBottom>
          {t('edit_category')}
        </Typography>
        <Typography variant="subtitle2">
          {t('edit_category_description')}
        </Typography>
      </DialogTitle>
      <Formik
        initialValues={{
          name: currentCategory?.name,
          description: currentCategory?.description
        }}
        validationSchema={Yup.object().shape({
          name: Yup.string().max(30).required(t('required_name'))
        })}
        onSubmit={async (
          values,
          { resetForm, setErrors, setStatus, setSubmitting }
        ) => {
          const formattedValues = {
            ...values,
            companySettings: { id: companySettingsId }
          };
          return dispatch(
            editCategory(currentCategory.id, formattedValues, basePath)
          )
            .then(onEditSuccess)
            .catch(onEditFailure);
        }}
      >
        {({
          errors,
          handleBlur,
          handleChange,
          handleSubmit,
          isSubmitting,
          touched,
          values
        }) => (
          <form onSubmit={handleSubmit}>
            <DialogContent
              dividers
              sx={{
                p: 3
              }}
            >
              <Grid container spacing={3}>
                <Grid item xs={12} lg={12}>
                  <Grid container spacing={3}>
                    <Grid item xs={12}>
                      <TextField
                        error={Boolean(touched.name && errors.name)}
                        fullWidth
                        helperText={touched.name && errors.name}
                        label={t('name')}
                        name="name"
                        onBlur={handleBlur}
                        onChange={handleChange}
                        value={values.name}
                        variant="outlined"
                      />
                    </Grid>
                    <Grid item xs={12}>
                      <TextField
                        fullWidth
                        label={t('description')}
                        multiline
                        rows={3}
                        name="description"
                        onBlur={handleBlur}
                        onChange={handleChange}
                        value={values.description}
                        variant="outlined"
                      />
                    </Grid>
                  </Grid>
                </Grid>
              </Grid>
            </DialogContent>
            <DialogActions
              sx={{
                p: 3
              }}
            >
              <Button
                color="secondary"
                onClick={() => setOpenUpdateCategoryModal(false)}
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
  );
  if (hasViewPermission(PermissionEntity.CATEGORIES_WEB))
    return (
      <MultipleTabsLayout
        basePath="/app/categories"
        tabs={tabs}
        tabIndex={tabIndex}
        title={t('categories')}
        action={
          hasCreatePermission(PermissionEntity.CATEGORIES)
            ? handleOpenAdd
            : null
        }
        actionTitle={t('category')}
      >
        {renderModal()}
        {renderUpdateModal()}
        <Grid item xs={12}>
          <Box p={4}>
            {categories[basePath]?.length ? (
              <ListWrapper disablePadding>
                {categories[basePath].map((item) => (
                  <Fragment key={item.id}>
                    <ListItem
                      sx={{
                        display: { xs: 'block', md: 'flex' },
                        py: 1.5,
                        px: 2
                      }}
                    >
                      <ListItemText
                        disableTypography
                        primary={
                          <Typography
                            sx={{
                              display: 'block',
                              mb: 1
                            }}
                            variant="h6"
                          >
                            {item.name}
                          </Typography>
                        }
                        secondary={
                          <Typography
                            sx={{
                              display: 'block',
                              mb: 1
                            }}
                            variant="subtitle1"
                          >
                            {item.description}
                          </Typography>
                        }
                      />
                      <Box
                        component="span"
                        sx={{
                          display: { xs: 'none', md: 'inline-block' }
                        }}
                      >
                        <Box ml={3} textAlign="right">
                          {hasEditPermission(
                            PermissionEntity.CATEGORIES,
                            categories[basePath].find(
                              (category) => category.id === item.id
                            )
                          ) && (
                            <IconButtonWrapper
                              onClick={() => {
                                setCurrentCategory(
                                  categories[basePath].find(
                                    (category) => category.id === item.id
                                  )
                                );
                                setOpenUpdateCategoryModal(true);
                              }}
                              sx={{
                                backgroundColor: `${theme.colors.primary.main}`,
                                color: `${theme.palette.getContrastText(
                                  theme.colors.primary.main
                                )}`,
                                transition: `${theme.transitions.create([
                                  'all'
                                ])}`,

                                '&:hover': {
                                  backgroundColor: `${theme.colors.primary.main}`,
                                  color: `${theme.palette.getContrastText(
                                    theme.colors.primary.main
                                  )}`
                                }
                              }}
                              size="small"
                            >
                              <EditTwoToneIcon fontSize="small" />
                            </IconButtonWrapper>
                          )}
                          {hasDeletePermission(
                            PermissionEntity.CATEGORIES,
                            categories[basePath].find(
                              (category) => category.id === item.id
                            )
                          ) && (
                            <IconButtonWrapper
                              onClick={() => {
                                setCurrentCategory(
                                  categories[basePath].find(
                                    (category) => category.id === item.id
                                  )
                                );
                                setOpenDelete(true);
                              }}
                              sx={{
                                ml: 1,
                                backgroundColor: `${theme.colors.error.lighter}`,
                                color: `${theme.colors.error.main}`,
                                transition: `${theme.transitions.create([
                                  'all'
                                ])}`,

                                '&:hover': {
                                  backgroundColor: `${theme.colors.error.main}`,
                                  color: `${theme.palette.getContrastText(
                                    theme.colors.error.main
                                  )}`
                                }
                              }}
                              size="small"
                            >
                              <ClearTwoToneIcon fontSize="small" />
                            </IconButtonWrapper>
                          )}
                        </Box>
                      </Box>
                    </ListItem>
                    <Divider sx={{ mt: 1 }} />
                  </Fragment>
                ))}
              </ListWrapper>
            ) : loading[basePath] ? (
              <Box display="flex" flexDirection="column" alignItems="center">
                <CircularProgress />
              </Box>
            ) : (
              <Box display="flex" flexDirection="column" alignItems="center">
                <Typography variant="h4">
                  {t('no_category_message', {
                    categoryName: tabs[tabIndex].label
                  })}
                </Typography>
                <Typography sx={{ mt: 1 }} variant="h6">
                  {t('no_category_action')}
                </Typography>
              </Box>
            )}
          </Box>
        </Grid>
        <ConfirmDialog
          open={openDelete}
          onCancel={() => {
            setOpenDelete(false);
          }}
          onConfirm={() => handleDelete(currentCategory?.id)}
          confirmText={t('to_delete')}
          question={t('confirm_delete_category')}
        />
      </MultipleTabsLayout>
    );
  else return <PermissionErrorMessage message={'no_access_categories'} />;
}

export default CategoriesLayout;
