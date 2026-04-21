import { ReactNode } from 'react';
import { CustomField } from '../../models/owns/customField';

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

export const getCustomFieldIField = (customField: CustomField): IField => {
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
