import { IField } from '../models/form';
import { formatSelect, formatSelectMultiple } from './formatters';
import { isTask } from '../models/tasks';

export const getWorkOrderFields = (t): IField[] => {
  return [
    {
      name: 'title',
      type: 'text',
      label: t('title'),
      placeholder: t('wo.title_description'),
      required: true
    },
    {
      name: 'description',
      type: 'text',
      label: t('description'),
      placeholder: t('description'),
      multiple: true
    },
    {
      name: 'image',
      type: 'file',
      fileType: 'image',
      label: t('image')
    },
    {
      name: 'dueDate',
      type: 'date',
      label: t('due_date')
    },
    {
      name: 'estimatedStartDate',
      type: 'date',
      label: t('estimated_start_date')
    },
    {
      name: 'estimatedDuration',
      type: 'number',
      label: t('estimated_duration'),
      placeholder: t('hours')
    },
    {
      name: 'priority',
      type: 'select',
      label: t('priority'),
      type2: 'priority'
    },
    {
      name: 'category',
      type: 'select',
      label: t('category'),
      type2: 'category',
      category: 'work-order-categories'
    },
    {
      name: 'primaryUser',
      type: 'select',
      label: t('primary_worker'),
      type2: 'user'
    },
    {
      name: 'assignedTo',
      type: 'select',
      label: t('additional_workers'),
      type2: 'user',
      multiple: true
    },
    {
      name: 'customers',
      type: 'select',
      label: t('customers'),
      type2: 'customer',
      multiple: true
    },
    {
      name: 'team',
      type: 'select',
      type2: 'team',
      label: t('team'),
      placeholder: t('select_team')
    },
    {
      name: 'location',
      type: 'select',
      type2: 'location',
      label: t('location'),
      placeholder: t('select_location')
    },
    {
      name: 'asset',
      type: 'select',
      type2: 'asset',
      label: t('asset'),
      placeholder: t('select_asset'),
      relatedFields: [{ field: 'location' }]
    },
    {
      name: 'tasks',
      type: 'select',
      type2: 'task',
      label: t('tasks'),
      placeholder: t('select_tasks')
    },
    {
      name: 'files',
      type: 'file',
      multiple: true,
      label: t('files'),
      fileType: 'file'
    },
    {
      name: 'requiredSignature',
      type: 'switch',
      label: t('requires_signature')
    }
  ];
};
export const formatWorkOrderValues = (values) => {
  const newValues = { ...values };
  newValues.primaryUser = formatSelect(newValues.primaryUser);
  newValues.location = formatSelect(newValues.location);
  newValues.team = formatSelect(newValues.team);
  newValues.asset = formatSelect(newValues.asset);
  newValues.assignedTo = formatSelectMultiple(newValues.assignedTo);
  newValues.customers = formatSelectMultiple(newValues.customers);
  newValues.category = formatSelect(newValues.category);
  newValues.tasks =
    newValues.tasks?.map((object) => {
      if (isTask(object)) {
        return object;
      } else {
        return object.value;
      }
    }) ?? [];
  return newValues;
};
export const formatRequestValues = (values) => {
  const newValues = { ...values };
  newValues.primaryUser = formatSelect(newValues.primaryUser);
  newValues.location = formatSelect(newValues.location);
  newValues.team = formatSelect(newValues.team);
  newValues.asset = formatSelect(newValues.asset);
  newValues.assignedTo = formatSelectMultiple(newValues.assignedTo);
  newValues.category = formatSelect(newValues.category);
  return newValues;
};
export const getAssetFields = (t): Array<IField> => {
  return [
    {
      name: 'assetInfo',
      type: 'titleGroupField',
      label: t('asset_information')
    },
    {
      name: 'name',
      type: 'text',
      label: t('name'),
      placeholder: t('asset_name_description'),
      required: true
    },
    {
      name: 'location',
      type: 'select',
      type2: 'location',
      label: t('location'),
      placeholder: t('select_asset_location'),
      required: true,
      midWidth: true
    },
    {
      name: 'acquisitionCost',
      type: 'number',
      label: t('acquisition_cost'),
      placeholder: t('acquisition_cost'),
      midWidth: true
    },
    {
      name: 'description',
      type: 'text',
      label: t('description'),
      placeholder: t('description'),
      multiple: true
    },
    {
      name: 'manufacturer',
      type: 'text',
      label: t('manufacturer'),
      placeholder: t('manufacturer'),
      midWidth: true
    },
    {
      name: 'power',
      type: 'text',
      label: t('power'),
      placeholder: t('power'),
      midWidth: true
    },
    {
      name: 'model',
      type: 'text',
      label: t('model'),
      placeholder: t('model'),
      midWidth: true
    },
    {
      name: 'serialNumber',
      type: 'text',
      label: t('serial_number'),
      placeholder: t('serial_number'),
      midWidth: true
    },
    {
      name: 'barCode',
      type: 'barcode',
      label: t('barcode')
    },
    {
      name: 'category',
      midWidth: true,
      label: t('category'),
      placeholder: t('category'),
      type: 'select',
      type2: 'category',
      category: 'asset-categories'
    },
    {
      name: 'area',
      type: 'text',
      midWidth: true,
      label: t('area'),
      placeholder: t('area')
    },
    {
      name: 'image',
      type: 'file',
      fileType: 'image',
      label: t('image')
    },
    {
      name: 'assignedTo',
      type: 'titleGroupField',
      label: t('assigned_to')
    },
    {
      name: 'primaryUser',
      type: 'select',
      type2: 'user',
      label: t('worker'),
      placeholder: t('primary_user_description')
    },
    {
      name: 'assignedTo',
      type: 'select',
      type2: 'user',
      multiple: true,
      label: t('additional_workers'),
      placeholder: 'additional_workers_description'
    },
    {
      name: 'teams',
      type: 'select',
      type2: 'team',
      multiple: true,
      label: t('teams'),
      placeholder: 'Select teams'
    },
    {
      name: 'moreInfos',
      type: 'titleGroupField',
      label: t('more_informations')
    },
    {
      name: 'customers',
      type: 'select',
      type2: 'customer',
      multiple: true,
      label: t('customers'),
      placeholder: 'customers_description'
    },
    {
      name: 'vendors',
      type: 'select',
      type2: 'vendor',
      multiple: true,
      label: t('vendors'),
      placeholder: t('vendors_description')
    },
    {
      name: 'nfcId',
      type: 'nfc',
      label: t('nfc_tag')
    },
    {
      name: 'inServiceDate',
      type: 'date',
      midWidth: true,
      label: t('inServiceDate_description')
    },
    {
      name: 'warrantyExpirationDate',
      type: 'date',
      midWidth: true,
      label: t('warranty_expiration_date')
    },
    {
      name: 'files',
      type: 'file',
      multiple: true,
      label: t('files'),
      fileType: 'file'
    },
    {
      name: 'additionalInfos',
      type: 'text',
      label: t('additional_information'),
      placeholder: t('additional_information'),
      multiple: true
    },
    {
      name: 'structure',
      type: 'titleGroupField',
      label: t('structure')
    },
    {
      name: 'parts',
      type: 'select',
      type2: 'part',
      label: t('parts'),
      multiple: true
    },
    {
      name: 'parentAsset',
      type: 'select',
      type2: 'asset',
      label: t('parent_asset'),
      relatedFields: [{ field: 'location' }]
    }
  ];
};
export const formatAssetValues = (values) => {
  const newValues = { ...values };
  newValues.primaryUser = formatSelect(newValues.primaryUser);
  newValues.location = formatSelect(newValues.location);
  newValues.category = formatSelect(newValues.category);
  newValues.parentAsset = formatSelect(newValues.parentAsset);
  newValues.customers = formatSelectMultiple(newValues.customers);
  newValues.vendors = formatSelectMultiple(newValues.vendors);
  newValues.assignedTo = formatSelectMultiple(newValues.assignedTo);
  newValues.teams = formatSelectMultiple(newValues.teams);
  newValues.parts = formatSelectMultiple(newValues.parts);
  return newValues;
};

export const formatLocationValues = (values) => {
  const newValues = { ...values };
  newValues.customers = formatSelectMultiple(newValues.customers);
  newValues.vendors = formatSelectMultiple(newValues.vendors);
  newValues.workers = formatSelectMultiple(newValues.workers);
  newValues.teams = formatSelectMultiple(newValues.teams);
  newValues.parentLocation = formatSelect(newValues.parentLocation);
  newValues.longitude = newValues.coordinates?.lng;
  newValues.latitude = newValues.coordinates?.lat;
  return newValues;
};

export const getLocationFields = (t): IField[] => {
  return [
    {
      name: 'name',
      type: 'text',
      label: t('name'),
      placeholder: t('enter_location_name'),
      required: true
    },
    {
      name: 'address',
      type: 'text',
      label: t('address'),
      placeholder: 'Casa, Maroc',
      required: true
    },
    {
      name: 'parentLocation',
      type: 'select',
      type2: 'parentLocation',
      label: t('parent_location'),
      placeholder: t('select_location')
    },
    {
      name: 'workers',
      multiple: true,
      type: 'select',
      type2: 'user',
      label: t('workers'),
      placeholder: t('select_workers')
    },
    {
      name: 'teams',
      multiple: true,
      type: 'select',
      type2: 'team',
      label: t('teams'),
      placeholder: 'Select teams'
    },
    {
      name: 'vendors',
      multiple: true,
      type: 'select',
      type2: 'vendor',
      label: t('vendors'),
      placeholder: 'Select vendors'
    },
    {
      name: 'customers',
      multiple: true,
      type: 'select',
      type2: 'customer',
      label: t('customers'),
      placeholder: 'Select customers'
    },
    {
      name: 'image',
      type: 'file',
      fileType: 'image',
      label: t('image')
    },
    {
      name: 'files',
      type: 'file',
      multiple: true,
      label: t('files'),
      fileType: 'file'
    }
  ];
};

export const formatMeterValues = (values) => {
  const newValues = { ...values };
  newValues.users = formatSelectMultiple(newValues.users);
  //values.teams = formatSelectMultiple(values.teams);
  newValues.location = formatSelect(newValues.location);
  newValues.asset = formatSelect(newValues.asset);
  newValues.updateFrequency = Number(newValues.updateFrequency);
  return newValues;
};
export const getMeterFields = (t): IField[] => {
  return [
    {
      name: 'name',
      type: 'text',
      label: t('name'),
      placeholder: t('enter_meter_name'),
      required: true
    },
    {
      name: 'unit',
      type: 'text',
      label: t('unit'),
      placeholder: t('unit'),
      required: true
    },
    {
      name: 'updateFrequency',
      type: 'number',
      label: t('update_frequency'),
      placeholder: t('update_frequency_in_days'),
      required: true
    },
    {
      name: 'category',
      label: t('category'),
      placeholder: t('category'),
      type: 'select',
      type2: 'category',
      category: 'meter-categories'
    },
    {
      name: 'image',
      type: 'file',
      fileType: 'image',
      label: t('image')
    },
    {
      name: 'location',
      type: 'select',
      type2: 'location',
      label: t('location')
    },
    {
      name: 'asset',
      type: 'select',
      type2: 'asset',
      label: t('asset'),
      relatedFields: [{ field: 'location' }],
      required: true
    },
    {
      name: 'users',
      type: 'select',
      type2: 'user',
      label: t('workers'),
      multiple: true
    }
  ];
};
export const formatPartValues = (values) => {
  const newValues = { ...values };
  newValues.assignedTo = formatSelectMultiple(newValues.assignedTo);
  newValues.teams = formatSelectMultiple(newValues.teams);
  newValues.customers = formatSelectMultiple(newValues.customers);
  newValues.vendors = formatSelectMultiple(newValues.vendors);
  // values.image = formatSelect(values.image);
  // values.files = formatSelect(values.files);
  return newValues;
};
export const getPartFields = (t): IField[] => {
  return [
    {
      name: 'name',
      type: 'text',
      label: t('name'),
      placeholder: t('enter_part_name'),
      required: true
    },
    {
      name: 'description',
      type: 'text',
      label: t('description'),
      placeholder: t('description'),
      multiple: true
    },
    {
      name: 'category',
      label: t('category'),
      placeholder: t('enter_part_category'),
      type: 'select',
      type2: 'category',
      category: 'part-categories'
    },
    {
      name: 'cost',
      type: 'number',
      label: t('cost'),
      placeholder: t('enter_part_cost')
    },
    {
      name: 'quantity',
      type: 'number',
      label: t('quantity'),
      placeholder: t('enter_part_quantity')
    },
    {
      name: 'unit',
      type: 'text',
      label: t('unit'),
      placeholder: t('unit_of_measurement')
    },
    {
      name: 'minQuantity',
      type: 'number',
      label: t('minimum_quantity'),
      placeholder: t('enter_part_minimum_quantity')
    },
    {
      name: 'nonStock',
      type: 'switch',
      label: t('non_stock')
    },
    {
      name: 'barcode',
      type: 'barcode',
      label: t('barcode')
    },
    {
      name: 'area',
      type: 'text',
      label: t('area'),
      placeholder: t('enter_part_area')
    },
    {
      name: 'additionalInfos',
      type: 'text',
      label: t('additional_part_details'),
      placeholder: t('additional_part_details'),
      multiple: true
    },
    {
      name: 'assignedTo',
      type: 'select',
      type2: 'user',
      multiple: true,
      label: t('workers'),
      placeholder: 'Select Workers'
    },
    {
      name: 'teams',
      type: 'select',
      type2: 'team',
      multiple: true,
      label: t('teams'),
      placeholder: 'Select Teams'
    },
    {
      name: 'vendors',
      type: 'select',
      type2: 'vendor',
      multiple: true,
      label: t('vendors'),
      placeholder: 'Select Vendors'
    },
    {
      name: 'customers',
      type: 'select',
      type2: 'customer',
      multiple: true,
      label: t('customers'),
      placeholder: 'Select Customers'
    },
    {
      name: 'image',
      type: 'file',
      label: t('image'),
      fileType: 'image'
    },
    {
      name: 'files',
      type: 'file',
      multiple: true,
      label: t('files')
    }
  ];
};
export const getCustomerFields = (t): IField[] => {
  return [
    {
      name: 'details',
      type: 'titleGroupField',
      label: t('details')
    },
    {
      name: 'name',
      type: 'text',
      label: t('customer_name'),
      placeholder: 'Jonh Doe',
      required: true
    },
    {
      name: 'address',
      type: 'text',
      label: t('address'),
      placeholder: t('address')
    },
    {
      name: 'phone',
      type: 'text',
      label: t('phone'),
      placeholder: '+212611223344',
      required: true
    },
    {
      name: 'website',
      type: 'text',
      label: t('website'),
      placeholder: 'https://web-site.com'
    },
    {
      name: 'email',
      type: 'text',
      label: t('email'),
      placeholder: 'john.doe@gmail.com'
    },
    {
      name: 'customerType',
      type: 'text',
      label: t('customer_type'),
      placeholder: t('customer_type_description')
    },
    {
      name: 'description',
      type: 'text',
      label: t('description'),
      multiple: true,
      placeholder: t('customer_description_description')
    },
    {
      name: 'rate',
      type: 'number',
      label: t('hourly_rate'),
      placeholder: t('hourly_rate'),
      icon: '$'
      // helperText: 'Changes will only apply to Work Orders created in the future'
    },
    {
      name: 'details',
      type: 'titleGroupField',
      label: t('billing_information')
    },
    {
      name: 'billingAddress',
      type: 'text',
      label: t('address'),
      placeholder: t('address')
    },
    {
      name: 'billingAddress2',
      type: 'text',
      label: t('address_line_2'),
      placeholder: t('address_line_2')
    },
    {
      name: 'billingName',
      type: 'text',
      label: t('billing_name'),
      placeholder: t('billing_name')
    },
    {
      name: 'billingCurrency',
      type: 'select',
      type2: 'currency',
      label: t('currency'),
      placeholder: t('select_currency')
    }
  ];
};
export const formatCustomerValues = (values) => {
  const newValues = { ...values };
  newValues.billingCurrency = formatSelect(newValues.billingCurrency);
  newValues.rate = newValues.rate ? Number(newValues.rate) : null;
  return newValues;
};

export const getVendorFields = (t): IField[] => {
  return [
    {
      name: 'companyName',
      type: 'text',
      label: t('company_name'),
      placeholder: 'Atlas',
      required: true
    },
    {
      name: 'address',
      type: 'text',
      label: t('address'),
      placeholder: t('address')
    },
    {
      name: 'phone',
      type: 'text',
      label: t('phone'),
      placeholder: '+00212611223344'
    },
    {
      name: 'website',
      type: 'text',
      label: t('website'),
      placeholder: 'https://web-site.com'
    },
    {
      name: 'name',
      type: 'text',
      label: t('name'),
      placeholder: 'John Doe',
      required: true
    },
    {
      name: 'email',
      type: 'text',
      label: t('email'),
      placeholder: 'john.doe@gmail.com'
    },
    {
      name: 'vendorType',
      type: 'text',
      label: t('vendor_type'),
      placeholder: t('vendor_type_description')
    },
    {
      name: 'description',
      type: 'text',
      label: t('Description'),
      multiple: true,
      placeholder: t('description')
    },
    {
      name: 'rate',
      type: 'number',
      label: t('hourly_rate'),
      placeholder: t('hourly_rate'),
      icon: '$'
    }
  ];
};
export const getTeamFields = (t): IField[] => {
  return [
    {
      name: 'name',
      type: 'text',
      label: t('name'),
      placeholder: t('team_name'),
      required: true
    },
    {
      name: 'description',
      type: 'text',
      multiple: true,
      label: t('description'),
      placeholder: t('description')
    },
    {
      name: 'users',
      type: 'select',
      type2: 'user',
      multiple: true,
      label: t('people_in_team'),
      placeholder: t('people_in_team')
    }
  ];
};
