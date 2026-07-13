import { ReactNode } from 'react';

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
    | 'dateRange';
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
  name?: string;
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
