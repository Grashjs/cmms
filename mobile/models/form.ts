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
    | 'audio';
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
