import { createContext, FC, useContext, useEffect } from 'react';
import useAuth from '../hooks/useAuth';
import { addFiles } from '../slices/file';
import { useDispatch, useSelector } from '../store';
import { FileType } from '../models/owns/file';
import { getUsersMini } from '../slices/user';
import { IField } from '../content/own/type';
import * as Yup from 'yup';
import { useTranslation } from 'react-i18next';
import { CustomSnackBarContext } from './CustomSnackBarContext';
import mailToLink from 'mailto-link';
import { useBrand } from '../hooks/useBrand';
import { format } from 'date-fns';
import { utcToZonedTime } from 'date-fns-tz';

type CompanySettingsContext = {
  getFormattedDate: (dateString: string, hideTime?: boolean) => string;
  uploadFiles: (
    files: any[],
    images: any[],
    hidden?: boolean
  ) => Promise<{ id: number; type: FileType }[]>;
  getUserNameById: (id: number) => string | null;
  getWOFieldsAndShapes: (
    defaultFields: Array<IField>,
    defaultShape: { [key: string]: any }
  ) => [Array<IField>, { [key: string]: any }];
  getFormattedCurrency: (amount: number | string) => string;
  requestSubscriptionChange: () => void;
};

// eslint-disable-next-line @typescript-eslint/no-redeclare
export const CompanySettingsContext = createContext<CompanySettingsContext>(
  {} as CompanySettingsContext
);

export const CompanySettingsProvider: FC = ({ children }) => {
  const { companySettings, getFilteredFields, isAuthenticated, company, user } =
    useAuth();
  const dispatch = useDispatch();
  const { generalPreferences } = companySettings ?? {
    dateFormat: 'DDMMYY',
    currency: { code: '$' },
    timeZone: Intl.DateTimeFormat().resolvedOptions().timeZone
  };
  const { allUsersMini } = useSelector((state) => state.users);
  const { workOrderConfiguration } = companySettings ?? {
    workOrderFieldConfigurations: []
  };
  const { t }: { t: any } = useTranslation();
  const { showSnackBar } = useContext(CustomSnackBarContext);
  const brandConfig = useBrand();

  const getFormattedDate = (dateString: string, hideTime?: boolean) => {
    if (!dateString) return '';

    const tz = generalPreferences.timeZone;
    const date = utcToZonedTime(new Date(dateString), tz);

    const timeStr = hideTime ? '' : format(date, ' HH:mm');

    if (generalPreferences.dateFormat === 'MMDDYY') {
      return format(date, 'MM/dd/yy') + timeStr;
    } else {
      return format(date, 'dd/MM/yy') + timeStr;
    }
  };
  const getFormattedCurrency = (amount: number): string => {
    const code = generalPreferences.currency.code;
    const currenciesToReverse = ['$'];
    return currenciesToReverse.includes(code)
      ? `${code} ${amount} `
      : `${amount} ${code}`;
  };

  const onUploadError = (err: { message: string }) => {
    showSnackBar(JSON.parse(err.message).message, 'error');
  };

  const uploadFiles = async (
    files: [],
    images: [],
    hidden?: boolean
  ): Promise<{ id: number; type: FileType }[]> => {
    let result: { id: number; type: FileType }[] = [];
    if (files?.length) {
      await dispatch(addFiles(files, 'OTHER', undefined, `${hidden}`))
        .then((fileIds) => {
          if (Array.isArray(fileIds))
            result = [
              ...fileIds.map((id) => {
                return { id, type: 'OTHER' as const };
              })
            ];
        })
        .catch(onUploadError);
    }
    if (images?.length) {
      await dispatch(addFiles(images, 'IMAGE', undefined, `${hidden}`))
        .then((images) => {
          if (Array.isArray(images))
            result = [
              ...result,
              ...images.map((imageId) => {
                return { id: imageId, type: 'IMAGE' as const };
              })
            ];
        })
        .catch(onUploadError);
    }
    return result;
  };

  const getUserNameById = (id: number) => {
    const user = allUsersMini.find((user) => user.id === id);
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
              yupSchema = Yup.string().required(requiredMessage).nullable();
              break;
            case 'date':
              yupSchema = Yup.string().required(requiredMessage).nullable();
              break;
            case 'file':
              yupSchema = Yup.array().required(requiredMessage);
              break;
            case 'number':
              yupSchema = Yup.number().required(requiredMessage).nullable();
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
  const requestSubscriptionChange = () => {
    window.open(
      mailToLink({
        to: brandConfig.mail,
        subject: 'Subscription change request',
        body: `Dear ${brandConfig.name} Team,

I would like to request an upgrade to my current ${brandConfig.name} plan.

Account Information:
- Company Name: ${company.name}
- Current Plan: ${company.subscription.subscriptionPlan.name}
- Account Email: ${user.email}

Upgrade Details:
- Desired Plan: [Basic/Professional/Enterprise]
- Number of Users Needed: [Number]
- Preferred Billing Cycle: [Monthly/Annual]
- Preferred Payment Method: [Credit Card/Bank Transfer]

Additional Comments:
[Add any specific requirements or questions here]

Thank you for your assistance.

Best regards,
${user.firstName} ${user.lastName}
[Your Position]
${company.name}
`
      })
    );
  };
  useEffect(() => {
    if (isAuthenticated) dispatch(getUsersMini(true));
  }, [isAuthenticated]);
  return (
    <CompanySettingsContext.Provider
      value={{
        getFormattedDate,
        uploadFiles,
        getUserNameById,
        getWOFieldsAndShapes,
        getFormattedCurrency,
        requestSubscriptionChange
      }}
    >
      {children}
    </CompanySettingsContext.Provider>
  );
};
