import { ReactNode } from 'react';
import {
  CustomField,
  CustomFieldEntityType,
  CustomFieldValue
} from '../../models/owns/customField';

export interface TableCustomizedDataType {
  id: string | number;
  [propName: string]: any;
}

export interface TableCustomizedColumnType {
  label: string;
  accessor: string;
}

export interface IField {
  label: string;
  type:
    | 'number'
    | 'text'
    | 'checkbox'
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
  category?:
    | 'purchase-order-categories'
    | 'cost-categories'
    | 'time-categories'
    | 'work-order-categories'
    | 'meter-categories'
    | 'part-categories'
    | 'asset-categories';
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
  items?: { label: string; value: string | number; checked?: boolean }[];
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
  const { label, fieldType, required, options, unit } = customField;
  const iField: IField = {
    // The unit belongs in the label because values are stored as plain strings —
    // showing "Volumenstrom (m³/h)" is what tells the user which unit to type in.
    label: unit ? `${label} (${unit})` : label,
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
import { TFunction } from 'react-i18next';

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
/**
 * A field applies to an asset when it is bound to no category at all — the behaviour of
 * every field before categories existed — or when it lists the asset's own category.
 * Passing undefined for the category id disables the check, which is what every entity
 * type other than ASSET needs.
 */
export const customFieldAppliesToCategory = (
  field: CustomField,
  assetCategoryId?: number | null
): boolean => {
  if (assetCategoryId === undefined) return true;
  if (!field.assetCategories?.length) return true;
  return field.assetCategories.some(({ id }) => id === assetCategoryId);
};

export const getCustomFieldsRequiredShape = (
  customFields: CustomField[],
  customFieldEntityType: CustomFieldEntityType,
  t: TFunction,
  assetCategoryId?: number | null
): { [key: string]: Yup.StringSchema | Yup.ObjectSchema<any> } => {
  const shape: { [key: string]: Yup.StringSchema | Yup.ObjectSchema<any> } = {};
  customFields
    .filter(({ entityType }) => entityType === customFieldEntityType)
    .filter((field) => customFieldAppliesToCategory(field, assetCategoryId))
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
  entityType: CustomFieldEntityType,
  assetCategoryId?: number | null
) =>
  [...customFields]
    .filter((field) => field.entityType === entityType)
    .filter((field) => customFieldAppliesToCategory(field, assetCategoryId))
    .sort((a, b) => a.order - b.order)
    .map((field) => getCustomFieldIField(field));

export const getCustomFieldValuesForDetails = (
  customFieldValues: CustomFieldValue[],
  getFormattedDate: (date: string) => string
): { label: string; value: string; isLink?: boolean }[] =>
  [...(customFieldValues ?? [])]
    .sort((a, b) => a.customField.order - b.customField.order)
    .map(({ customField, value }) => ({
      label: customField.unit
        ? `${customField.label} (${customField.unit})`
        : customField.label,
      value: customField.fieldType.includes('DATE')
        ? getFormattedDate(value)
        : value,
      isLink: customField.fieldType === 'LINK'
    }));

export type SuccessResponse = {
  success: boolean;
  message: string;
};
