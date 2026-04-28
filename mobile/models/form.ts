import { ReactNode } from 'react';

export type CategoryType =
  | 'purchase-order-categories'
  | 'cost-categories'
  | 'time-categories'
  | 'work-order-categories'
  | 'asset-categories'
  | 'meter-categories'
  | 'part-categories';

export interface IField {
  label: string;
  type:
    | 'number'
    | 'text'
    | 'file'
    | 'groupCheckbox'
    | 'select'
    | 'titleGroupField'
    | 'form'
    | 'date'
    | 'switch'
    | 'partQuantity'
    | 'coordinates'
    | 'dateRange'
    | 'nfc'
    | 'barcode'
    | 'audio'
    | 'signature';
  type2?:
    | 'customer'
    | 'vendor'
    | 'user'
    | 'team'
    | 'part'
    | 'location'
    | 'asset'
    | 'priority'
    | 'task'
    | 'category'
    | 'parentLocation'
    | 'role'
    | 'currency';
  category?: CategoryType;
  name: string;
  placeholder?: string;
  fileType?: 'file' | 'image';
  helperText?: string;
  fullWidth?: boolean;
  multiple?: boolean;
  midWidth?: boolean;
  onPress?: () => void;
  required?: boolean;
  error?: any;
  items?: {
    label: string;
    value: string | number;
    checked?: boolean;
    color?: string;
  }[];
  // listCheckbox?: { label: string; value: string; checked?: boolean }[];
  icon?: ReactNode | string;
  // onPressIcon?: () => void;
  checked?: boolean;
  loading?: boolean;
  excluded?: number;
  relatedFields?: { field: string; value?: any; hide?: boolean }[];
}

export interface IHash<E> {
  [key: string]: E;
}

const getCustomFieldIField = (customField: CustomField): IField => {
  const { label, fieldType, required, options } = customField;
  const iField: IField = {
    label,
    name: `customField_${customField.id}`,
    type: 'text',
    required
  };
  switch (fieldType) {
    case 'SHORT_TEXT':
      iField.type = 'text';
      break;
    case 'LONG_TEXT':
      iField.type = 'text';
      iField.multiple = true;
      break;
    case 'NUMBER':
      iField.type = 'number';
      break;
    case 'SINGLE_CHOICE':
      iField.type = 'select';
      iField.items = options?.map((option) => ({
        label: option,
        value: option
      }));
      break;
    case 'DATE':
      iField.type = 'date';
      break;
    case 'DATE_TIME':
      iField.type = 'date';
      break;
    case 'LINK':
      iField.type = 'text';
      break;
    default:
      iField.type = 'text';
  }
  return iField;
};

import * as Yup from 'yup';
import {
  CustomField,
  CustomFieldEntityType,
  CustomFieldValue
} from './customField';
import { TFunction } from 'i18next';

interface EntityWithCustomFields {
  customFieldValues?: { customField: CustomField; value: string }[];
}

export const getCustomFieldsValues = <T extends EntityWithCustomFields>(
  entity: T
): { [key: string]: string | { label: string; value: string | number } } => {
  const values: {
    [key: string]: string | { label: string; value: string | number };
  } = {};
  entity?.customFieldValues?.forEach((cf) => {
    values[`customField_${cf.customField.id}`] =
      cf.customField.fieldType === 'SINGLE_CHOICE'
        ? { label: cf.value, value: cf.value }
        : cf.value;
  });
  return values;
};
export const getCustomFieldsRequiredShape = (
  customFields: CustomField[],
  customFieldEntityType: CustomFieldEntityType,
  t: TFunction
): { [key: string]: Yup.StringSchema | Yup.ObjectSchema<any> } => {
  const shape: { [key: string]: Yup.StringSchema | Yup.ObjectSchema<any> } = {};
  customFields
    .filter(({ entityType }) => entityType === customFieldEntityType)
    .forEach((field) => {
      if (field.required) {
        shape[`customField_${field.id}`] =
          field.fieldType === 'SINGLE_CHOICE'
            ? Yup.object().required(t('required_field'))
            : Yup.string().required(t('required_field'));
      }
    });
  return shape;
};

export const getCustomFieldsIFields = (
  customFields: CustomField[],
  entityType: CustomFieldEntityType
) =>
  [...customFields]
    .filter((field) => field.entityType === entityType)
    .sort((a, b) => a.order - b.order)
    .map((field) => getCustomFieldIField(field));

export const getCustomFieldValuesForDetails = (
  customFieldValues: CustomFieldValue[],
  getFormattedDate: (date: string) => string
): { label: string; value: string; isLink?: boolean }[] =>
  [...(customFieldValues ?? [])]
    .sort((a, b) => a.customField.order - b.customField.order)
    .map(({ customField, value }) => ({
      label: customField.label,
      value: customField.fieldType.includes('DATE')
        ? getFormattedDate(value)
        : value,
      isLink: customField.fieldType === 'LINK'
    }));
