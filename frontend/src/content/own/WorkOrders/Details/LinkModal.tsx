import {
  Autocomplete,
  Button,
  CircularProgress, debounce,
  Dialog,
  DialogContent,
  DialogTitle,
  Grid,
  MenuItem,
  Select, TextField,
  Typography
} from '@mui/material';
import FormHelperText from '@mui/material/FormHelperText';
import { useTranslation } from 'react-i18next';

import { Formik } from 'formik';

import * as Yup from 'yup';
import { useDispatch, useSelector } from '../../../../store';
import { createRelation } from '../../../../slices/relation';
import { relationTypes } from 'src/models/owns/relation';
import { onSearchQueryChange } from '../../../../utils/overall';
import WorkOrder from '../../../../models/owns/workOrder';
import { useEffect, useMemo, useState } from 'react';
import { SearchCriteria } from '../../../../models/owns/page';
import { getWorkOrders, getWorkOrdersMini } from '../../../../slices/workOrder';

interface LinkModalProps {
  open: boolean;
  onClose: () => void;
  workOrderId: number;
}

export default function LinkModal({
                                    open,
                                    onClose,
                                    workOrderId
                                  }: LinkModalProps) {
  const { t }: { t: any } = useTranslation();
  const dispatch = useDispatch();
  const { workOrdersMini } = useSelector((state) => state.workOrders);
  const [criteria, setCriteria] = useState<SearchCriteria>({
    filterFields: [], pageSize: 10,
    pageNum: 0,
    direction: 'DESC'
  });
  useEffect(() => {
    dispatch(getWorkOrdersMini(criteria));
  }, [criteria]);
  const onQueryChange = (event) => {
    onSearchQueryChange<WorkOrder>(event, criteria, setCriteria, [
      'title',
      'description',
      'feedback'
    ]);
  };
  const debouncedQueryChange = useMemo(() => debounce(onQueryChange, 1300), []);

  return (
    <Dialog fullWidth maxWidth="sm" open={open} onClose={onClose}>
      <DialogTitle
        sx={{
          p: 3
        }}
      >
        <Typography variant="h4" gutterBottom>
          {t('link_wo')}
        </Typography>
        <Typography variant="subtitle2">{t('link_wo_description')}</Typography>
      </DialogTitle>
      <Formik
        initialValues={{
          relationType: 'BLOCKED_BY',
          child: null
        }}
        validationSchema={Yup.object().shape({
          relationType: Yup.string().required(t('required_relationType')),
          child: Yup.object().required(t('required_wo'))
        })}
        onSubmit={(
          _values,
          { resetForm, setErrors, setStatus, setSubmitting }
        ) => {
          setSubmitting(true);
          dispatch(createRelation(workOrderId, _values)).finally(() => {
            setSubmitting(false);
            onClose();
          });
        }}
      >
        {({
            errors,
            handleBlur,
            handleChange,
            handleSubmit,
            setFieldValue,
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
                      <Typography variant="h6" fontWeight="bold">
                        {t('this_wo')}
                      </Typography>
                      <Select
                        fullWidth
                        name="relationType"
                        onBlur={handleBlur}
                        onChange={handleChange}
                        value={values.relationType}
                        variant="outlined"
                      >
                        {relationTypes.map((relationType, index) => (
                          <MenuItem key={index} value={relationType}>
                            {t(relationType)}
                          </MenuItem>
                        ))}
                      </Select>
                    </Grid>
                    <Grid item xs={12}>
                      <Typography variant="h6" fontWeight="bold">
                        {t('the_wo')}
                      </Typography>
                      <Autocomplete
                        fullWidth
                        options={workOrdersMini.content}
                        // @ts-ignore
                        getOptionLabel={(option) => option.title}
                        onInputChange={(event, value) => debouncedQueryChange({ target: { value } })}
                        onChange={(event, value) => {
                          setFieldValue('child', value);
                        }}
                        value={values.child}
                        renderInput={(params) => (
                          <TextField
                            {...params}
                            fullWidth
                            variant="outlined"
                            placeholder={t('select')}
                            error={Boolean(errors.child)}
                            helperText={errors.child ? t('required_wo') : ''}
                          />
                        )}
                      />
                    </Grid>
                  </Grid>
                </Grid>
                <Grid item xs={12} lg={12}>
                  <Button
                    variant="contained"
                    type="submit"
                    startIcon={
                      isSubmitting ? <CircularProgress size="1rem" /> : null
                    }
                    disabled={isSubmitting}
                  >
                    {t('link')}
                  </Button>
                </Grid>
              </Grid>
            </DialogContent>
          </form>
        )}
      </Formik>
    </Dialog>
  );
}
