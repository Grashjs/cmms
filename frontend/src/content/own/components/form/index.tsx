import {
  Autocomplete,
  Box,
  Button,
  Card,
  CircularProgress,
  Grid,
  IconButton,
  Link,
  TextField,
  Typography
} from '@mui/material';
import { ErrorMessage, Formik, FormikProps, useFormikContext } from 'formik';
import { useTranslation } from 'react-i18next';
import FormikErrorFocus from 'formik-error-focus';
import * as Yup from 'yup';
import { ObjectSchema } from 'yup';
import { IField, IHash } from '../../type';
import { InputAdornment } from '@mui/material'; // Added InputAdornment
import SearchIcon from '@mui/icons-material/Search'; // Added SearchIcon
import CheckBoxForm from './CheckBoxForm';
import Field from './Field';
import SelectForm from './SelectForm';
import FileUpload from '../FileUpload';
import DateTimePicker from '@mui/lab/DateTimePicker';
import SelectParts from './SelectParts';
import { useDispatch, useSelector } from '../../../../store';
import CustomSwitch from './CustomSwitch';
import SelectTasksModal from './SelectTasks';
import SelectMapCoordinates from './SelectMapCoordinates';
import SelectAssetModal from './SelectAssetModal'; // Import the new modal
import { getCustomersMini } from '../../../../slices/customer';
import { getVendorsMini } from '../../../../slices/vendor';
import { getLocationChildren, getLocationsMini } from 'src/slices/location';
import { getUsersMini } from '../../../../slices/user';
import { getAssetsMini } from '../../../../slices/asset';
import { getTeamsMini } from '../../../../slices/team';
import AssignmentTwoToneIcon from '@mui/icons-material/AssignmentTwoTone';
import EditTwoToneIcon from '@mui/icons-material/EditTwoTone';
import AddCircleTwoToneIcon from '@mui/icons-material/AddCircleTwoTone';
import { useState } from 'react';
import { getPriorityLabel } from '../../../../utils/formatters';
import { getCategories } from '../../../../slices/category';
import SelectPartQuantities from './SelectPartQuantities';
import { getRoles } from '../../../../slices/role';
import { getCurrencies } from '../../../../slices/currency';
import { DateRangePicker } from '@mui/x-date-pickers-pro/DateRangePicker';
import { LocalizationProvider } from '@mui/x-date-pickers-pro';
import { AdapterDayjs } from '@mui/x-date-pickers-pro/AdapterDayjs';
import useAuth from '../../../../hooks/useAuth';
import DeleteTwoToneIcon from '@mui/icons-material/DeleteTwoTone';
import ClearTwoToneIcon from '@mui/icons-material/ClearTwoTone';
import { CustomSelect } from './CustomSelect2';
import SignaturePad from './SignaturePad';

interface PropsType {
  fields: Array<IField>;
  values?: IHash<any>;
  onSubmit?: (values: IHash<any>) => Promise<any>;
  onCanceled?: () => void;
  onChange?: any;
  submitText?: string;
  validation?: ObjectSchema<any>;
  isLoading?: boolean;
  isButtonEnabled?: (values: IHash<any>, ...props: any[]) => boolean;
}

export default (props: PropsType) => {
  const { t }: { t: any } = useTranslation();
  const shape: IHash<any> = {};
  const {
    companySettings: { generalPreferences }
  } = useAuth();

  props.fields.forEach((f) => {
    shape[f.name] = Yup.string();
    if (f.required) {
      shape[f.name] = shape[f.name].required();
    }
  });

  const validationSchema = Yup.object().shape(shape);

  const handleChange = (formik: FormikProps<IHash<any>>, field: string, e) => {
    // eslint-disable-next-line @typescript-eslint/no-unused-expressions
    props.onChange && props.onChange({ field, e });
    if (props.fields.length == 1) {
      formik.setFieldTouched(field, true);
    }
    formik.setFieldValue(field, e);
    return formik.handleChange(field);
  };

  const filterRelatedFields = (fields: IField[], formik): IField[] => {
    const fieldsClone = [...fields];
    const withRelatedFields = fields.filter(
      (field) => field.relatedFields?.length
    );
    withRelatedFields.forEach(({ relatedFields, name, type }) => {
      relatedFields.forEach((relatedField) => {
        if (
          formik.values[name] === relatedField.value ||
          formik.values[name]?.value === relatedField.value ||
          (type === 'switch' &&
            formik.values[name] &&
            (formik.values[name][0] === 'on') === relatedField.value) ||
          (formik.values[name] === undefined && !relatedField.value)
        ) {
          if (relatedField.hide) {
            const fieldToDeleteIndex = fieldsClone.findIndex(
              (field) => field.name === relatedField.field
            );
            fieldsClone.splice(fieldToDeleteIndex, 1);
          }
        }
      });
    });
    return fieldsClone;
  };
  return (
    <>
      <Formik<IHash<any>>
        validationSchema={props.validation || validationSchema}
        validateOnChange={false}
        validateOnBlur={false}
        initialValues={props.values || {}}
        onSubmit={(
          values,
          { resetForm, setErrors, setStatus, setSubmitting }
        ) => {
          setSubmitting(true);
          props.onSubmit(values).finally(() => {
            // resetForm();
            setStatus({ success: true });
            setSubmitting(false);
          });
        }}
      >
        {(formik) => (
          <Grid container spacing={2}>
            {filterRelatedFields(props.fields, formik).map((field, index) => {
              return (
                <Grid item xs={12} lg={field.midWidth ? 6 : 12} key={index}>
                  {field.type === 'select' ? (
                    <CustomSelect field={field} handleChange={handleChange} />
                  ) : field.type === 'checkbox' ? (
                    <CheckBoxForm
                      label={field.label}
                      onChange={(e) => {
                        handleChange(formik, field.name, e.target.checked);
                      }}
                      checked={formik.values[field.name]}
                    />
                  ) : field.type === 'groupCheckbox' ? (
                    <CheckBoxForm
                      label={field.label}
                      type="groupCheckbox"
                      listCheckbox={field.items}
                      key={field.name}
                    />
                  ) : field.type === 'switch' ? (
                    <CustomSwitch
                      title={field.label}
                      description={field.helperText}
                      name={field.name}
                      handleChange={formik.handleChange}
                      checked={formik.values[field.name]}
                    />
                  ) : field.type === 'titleGroupField' ? (
                    <Typography variant="h3" sx={{ pb: 1 }}>
                      {t(`${field.label}`)}
                    </Typography>
                  ) : field.type === 'file' ? (
                    <Box>
                      <FileUpload
                        multiple={field.multiple}
                        title={field.label}
                        type={field.fileType || 'file'}
                        description={t('upload')}
                        onDrop={(files) => {
                          formik.setFieldValue(field.name, files);
                        }}
                      />
                    </Box>
                  ) : field.type === 'date' ? (
                    <Box>
                      <Box pb={1}>
                        <b>{field.label}:</b>
                      </Box>
                      <DateTimePicker
                        value={formik.values[field.name] ?? null}
                        onChange={(newValue) => {
                          handleChange(formik, field.name, newValue);
                        }}
                        inputFormat={
                          generalPreferences.dateFormat === 'MMDDYY'
                            ? 'MM/dd/yyyy HH:mm'
                            : 'dd/MM/yyyy HH:mm'
                        }
                        ampm={false}
                        renderInput={(params) => (
                          <TextField
                            {...params}
                            fullWidth
                            placeholder={t('select_date')}
                            required={field?.required}
                            error={!!formik.errors[field.name] || field.error}
                            helperText={
                              !!formik.errors[field.name] || field.error
                                ? formik.errors[field.name]
                                : ''
                            }
                          />
                        )}
                      />
                    </Box>
                  ) : field.type === 'dateRange' ? (
                    <Box>
                      <Box pb={1}>
                        <b>{field.label}:</b>
                      </Box>
                      <LocalizationProvider
                        localeText={{ start: t('start'), end: t('end') }}
                        dateAdapter={AdapterDayjs}
                      >
                        <DateRangePicker
                          value={formik.values[field.name] ?? [null, null]}
                          onChange={(newValue) => {
                            handleChange(formik, field.name, newValue);
                          }}
                          renderInput={(startProps, endProps) => (
                            <>
                              <TextField {...startProps} />
                              <Box sx={{ mx: 2 }}> {t('to')} </Box>
                              <TextField {...endProps} />
                            </>
                          )}
                        />
                      </LocalizationProvider>
                    </Box>
                  ) : field.type === 'coordinates' ? (
                    <SelectMapCoordinates
                      selected={formik.values[field.name]}
                      onChange={(coordinates) => {
                        handleChange(formik, field.name, coordinates);
                      }}
                    />
                  ) : field.type === 'partQuantity' ? (
                    <SelectPartQuantities
                      selected={formik.values[field.name] ?? []}
                      onChange={(newPartQuantities) => {
                        handleChange(formik, field.name, newPartQuantities);
                      }}
                    />
                  ) : field.type === 'signature' ? (
                    <SignaturePad
                      label={field.label}
                      value={formik.values[field.name]}
                      onChange={(signature) => {
                        formik.setFieldValue(field.name, signature);
                      }}
                    />
                  ) : (
                    <Field
                      key={index}
                      {...field}
                      isDisabled={formik.isSubmitting}
                      type={field.type}
                      label={field.label}
                      placeholder={field.placeholder}
                      value={formik.values[field.name]}
                      onBlur={formik.handleBlur}
                      // onChange={formik.handleChange}
                      onChange={(e) => {
                        handleChange(formik, field.name, e.target.value);
                      }}
                      error={!!formik.errors[field.name] || field.error}
                      errorMessage={formik.errors[field.name]}
                      fullWidth={field.fullWidth}
                    />
                  )}
                </Grid>
              );
            })}

            <Grid item xs={12}>
              <Button
                type="submit"
                sx={{
                  mt: { xs: 2, sm: 0 }
                }}
                onClick={() => formik.handleSubmit()}
                variant="contained"
                startIcon={
                  formik.isSubmitting ? <CircularProgress size="1rem" /> : null
                }
                disabled={Boolean(formik.errors.submit) || formik.isSubmitting}
              >
                {t(props.submitText)}
              </Button>

              {props.onCanceled && (
                <Button
                  sx={{
                    mt: { xs: 2, sm: 0 }
                  }}
                  onClick={() => props.onCanceled}
                  variant="outlined"
                  disabled
                >
                  {t(props.submitText)}
                </Button>
              )}
            </Grid>
            <FormikErrorFocus
              // See scroll-to-element for configuration options: https://www.npmjs.com/package/scroll-to-element
              offset={0}
              align={'bottom'}
              focusDelay={200}
              ease={'linear'}
              duration={500}
              formik={formik}
            />
          </Grid>
        )}
      </Formik>
    </>
  );
};
