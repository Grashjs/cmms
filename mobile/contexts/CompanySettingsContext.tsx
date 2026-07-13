import { createContext, FC, ReactNode, useEffect } from 'react';
import useAuth from '../hooks/useAuth';
import { addFiles } from '../slices/file';
import { useDispatch, useSelector } from '../store';
import { FileType } from '../models/file';
import { getUsersMini } from '../slices/user';
import { IField } from '../models/form';
import * as Yup from 'yup';
import { useTranslation } from 'react-i18next';
import PropTypes from 'prop-types';
import { getWOBaseFields } from '../utils/woBase';

type CompanySettingsContext = {
  getFormattedDate: (dateString: string, hideTime?: boolean) => string;
  uploadFiles: (
    files: { uri: string; name: string; type: string }[],
    images: { uri: string; name: string; type: string }[],
    hidden?: boolean
  ) => Promise<{ id: number; type: FileType }[]>;
  getUserNameById: (id: number) => string | null;
  getWOFieldsAndShapes: (
    defaultFields: Array<IField>,
    defaultShape: { [key: string]: any }
  ) => [Array<IField>, { [key: string]: any }];
  getRequestFieldsAndShapes: () => [Array<IField>, { [key: string]: any }];
  getFormattedCurrency: (amount: number | string) => string;
};

// eslint-disable-next-line @typescript-eslint/no-redeclare
export const CompanySettingsContext = createContext<CompanySettingsContext>(
  {} as CompanySettingsContext
);

export const CompanySettingsProvider: FC<{ children: ReactNode }> = (props) => {
  const { companySettings, getFilteredFields, isAuthenticated } = useAuth();
  const dispatch = useDispatch();
  const { generalPreferences, workOrderRequestConfiguration } =
    companySettings ?? {
      dateFormat: 'DDMMYY',
      currency: { code: '$' }
    };
  const { usersMini } = useSelector((state) => state.users);
  const { workOrderConfiguration } = companySettings ?? {
    workOrderFieldConfigurations: []
  };
  const { children } = props;
  const { t }: { t: any } = useTranslation();

  const getFormattedDate = (dateString: string, hideTime?: boolean) => {
    if (!dateString) return '';
    const date = new Date(dateString);
    const month = ('0' + (date.getMonth() + 1).toString()).substr(-2);
    const day = ('0' + date.getDate().toString()).substr(-2);
    const year = date.getFullYear().toString().substr(2);
    const time = hideTime
      ? ''
      : (date.getHours() < 10 ? '0' + date.getHours() : date.getHours()) +
        ':' +
        (date.getMinutes() < 10 ? '0' + date.getMinutes() : date.getMinutes());
    if (generalPreferences.dateFormat === 'MMDDYY') {
      return month + '/' + day + '/' + year + ' ' + time;
    } else return day + '/' + month + '/' + year + ' ' + time;
  };
  const getFormattedCurrency = (amount: number): string => {
    const code = generalPreferences.currency.code;
    const currenciesToReverse = ['$'];
    return currenciesToReverse.includes(code)
      ? `${code} ${amount} `
      : `${amount} ${code}`;
  };
  const uploadFiles = async (
    files: { uri: string; name: string; type: string }[],
    images: { uri: string; name: string; type: string }[],
    hidden?: boolean
  ): Promise<{ id: number; type: FileType }[]> => {
    let result: { id: number; type: FileType }[] = [];
    if (files?.length) {
      await dispatch(addFiles(files, 'OTHER', undefined, `${hidden}`)).then(
        (fileIds) => {
          if (Array.isArray(fileIds))
            result = [
              ...fileIds.map((id) => {
                return { id, type: 'OTHER' as const };
              })
            ];
        }
      );
    }
    if (images?.length) {
      await dispatch(addFiles(images, 'IMAGE', undefined, `${hidden}`)).then(
        (images) => {
          if (Array.isArray(images))
            result = [
              ...result,
              ...images.map((imageId) => {
                return { id: imageId, type: 'IMAGE' as const };
              })
            ];
        }
      );
    }
    return result;
  };

  const getUserNameById = (id: number) => {
    const user = usersMini.find((user) => user.id === id);
    return user ? `${user.firstName} ${user.lastName}` : null;
  };
  const getWOFieldsAndShapes = (
    defaultFields: Array<IField>,
    defaultShape
  ): [Array<IField>, { [key: string]: any }] => {
    let fields = [...getFilteredFields(defaultFields)];
    let shape = { ...defaultShape };
    const fieldsToConfigure = [
      'asset',
      'description',
      'priority',
      'images',
      'primaryUser',
      'assignedTo',
      'team',
      'location',
      'dueDate',
      'category',
      'purchaseOrder',
      'files',
      'signature'
    ];
    fieldsToConfigure.forEach((name) => {
      const fieldConfig =
        workOrderConfiguration.workOrderFieldConfigurations.find(
          (woFC) => woFC.fieldName === name
        );
      const fieldIndexInFields = fields.findIndex(
        (field) => field.name === name
      );
      if (fieldIndexInFields !== -1) {
        if (fieldConfig.fieldType === 'REQUIRED') {
          fields[fieldIndexInFields] = {
            ...fields[fieldIndexInFields],
            required: true
          };
          const requiredMessage = t('required_field');
          let yupSchema;
          switch (fields[fieldIndexInFields].type) {
            case 'text':
              yupSchema = Yup.string().required(requiredMessage);
              break;
            case 'date':
              yupSchema = Yup.string().required(requiredMessage);
              break;
            case 'file':
              yupSchema = Yup.array().required(requiredMessage);
              break;
            case 'number':
              yupSchema = Yup.number().required(requiredMessage);
              break;
            case 'select':
              if (fields[fieldIndexInFields].multiple) {
                yupSchema = Yup.array().required(requiredMessage);
              } else {
                yupSchema = Yup.object().required(requiredMessage).nullable();
              }
              break;
            default:
              yupSchema = Yup.object().required(requiredMessage).nullable();
              break;
          }
          shape[name] = yupSchema;
        } else if (fieldConfig.fieldType === 'HIDDEN') {
          fields.splice(fieldIndexInFields, 1);
        }
      }
    });
    return [fields, shape];
  };

  const getRequestFieldsAndShapes = (): [
    Array<IField>,
    { [key: string]: any }
  ] => {
    const defaultFields: Array<IField> = [...getWOBaseFields(t)];
    const defaultShape = {
      title: Yup.string().required(t('required_request_name'))
    };
    let fields = [...getFilteredFields(defaultFields)];
    let shape = { ...defaultShape };
    const fieldsToConfigure = [
      'asset',
      'location',
      'primaryUser',
      'category',
      'dueDate',
      'team'
    ];
    fieldsToConfigure.forEach((name) => {
      const fieldConfig =
        workOrderRequestConfiguration.fieldConfigurations.find(
          (fc) => fc.fieldName === name
        );
      const fieldIndexInFields = fields.findIndex(
        (field) => field.name === name
      );
      if (fieldConfig.fieldType === 'REQUIRED') {
        fields[fieldIndexInFields] = {
          ...fields[fieldIndexInFields],
          required: true
        };
        const requiredMessage = t('required_field');
        let yupSchema;
        switch (fields[fieldIndexInFields].type) {
          case 'text':
            yupSchema = Yup.string().required(requiredMessage);
            break;
          case 'date':
            yupSchema = Yup.string().required(requiredMessage);
            break;
          case 'number':
            yupSchema = Yup.number().required(requiredMessage);
            break;
          default:
            yupSchema = Yup.object().required(requiredMessage).nullable();
            break;
        }
        shape[name] = yupSchema;
      } else if (fieldConfig.fieldType === 'HIDDEN') {
        fields.splice(fieldIndexInFields, 1);
      }
    });

    return [fields, shape];
  };
  useEffect(() => {
    if (isAuthenticated) dispatch(getUsersMini());
  }, [isAuthenticated]);
  return (
    <CompanySettingsContext.Provider
      value={{
        getFormattedDate,
        uploadFiles,
        getUserNameById,
        getWOFieldsAndShapes,
        getRequestFieldsAndShapes,
        getFormattedCurrency
      }}
    >
      {children}
    </CompanySettingsContext.Provider>
  );
};
CompanySettingsProvider.propTypes = {
  children: PropTypes.node.isRequired
};
