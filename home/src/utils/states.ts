import { EntityType, OwnHeader } from 'src/content/own/Imports';
import { WorkOrderImportDTO } from '../models/owns/imports';

export const getOwnHeadersConfig = (
  t: any
): Record<EntityType, OwnHeader[]> => {
  const idFormatter = (value) => (isNaN(value) ? null : value);
  const arrayFormatter = (value) => value?.split(',') ?? [];

  const workOrderHeaders: (Omit<OwnHeader, 'keyName'> & {
    keyName: keyof WorkOrderImportDTO;
  })[] = [
    {
      label: t('id'),
      keyName: 'id',
      formatter: idFormatter
    },
    { label: t('title'), keyName: 'title', required: true },
    { label: t('description'), keyName: 'description' },
    { label: t('due_date'), keyName: 'dueDate' },
    { label: t('completed_on'), keyName: 'completedOn' },
    { label: t('status'), keyName: 'status' },
    { label: t('estimated_hours'), keyName: 'estimatedDuration' },
    { label: t('priority'), keyName: 'priority' },
    { label: t('category'), keyName: 'category' },
    { label: t('completed_by'), keyName: 'completedByEmail' },
    {
      label: t('assigned_to'),
      keyName: 'assignedToEmails',
      formatter: arrayFormatter
    },
    { label: t('primary_worker'), keyName: 'primaryUserEmail' },
    { label: t('asset_name'), keyName: 'assetName' },
    { label: t('location_name'), keyName: 'locationName' },
    { label: t('team_name'), keyName: 'teamName' },
    {
      label: t('customers'),
      keyName: 'customersNames',
      formatter: arrayFormatter
    },
    { label: t('feedback'), keyName: 'feedback' },
    { label: t('requires_signature'), keyName: 'requiredSignature' },
    { label: t('archived'), keyName: 'archived' }
  ];
  return {
    'work-orders': workOrderHeaders,
    locations: [
      {
        label: t('id'),
        keyName: 'id',
        formatter: idFormatter
      },
      { label: t('name'), keyName: 'name', required: true },
      { label: t('address'), keyName: 'address' },
      { label: t('longitude'), keyName: 'longitude' },
      { label: t('latitude'), keyName: 'latitude' },
      { label: t('parent_location'), keyName: 'parentLocationName' },
      {
        label: t('customers'),
        keyName: 'customersNames',
        formatter: arrayFormatter
      },
      {
        label: t('vendors'),
        keyName: 'vendorsNames',
        formatter: arrayFormatter
      },
      {
        label: t('assigned_to'),
        keyName: 'workersEmails',
        formatter: arrayFormatter
      },
      {
        label: t('teams'),
        keyName: 'teamsNames',
        formatter: arrayFormatter
      }
    ],
    assets: [
      {
        label: t('id'),
        keyName: 'id',
        formatter: idFormatter
      },
      { label: t('name'), keyName: 'name', required: true },
      { label: t('archived'), keyName: 'archived' },
      { label: t('description'), keyName: 'description' },
      { label: t('location_name'), keyName: 'locationName' },
      { label: t('parent_asset'), keyName: 'parentAssetName' },
      { label: t('area'), keyName: 'area' },
      { label: t('barcode'), keyName: 'barCode' },
      { label: t('model'), keyName: 'model' },
      { label: t('power'), keyName: 'power' },
      { label: t('manufacturer'), keyName: 'manufacturer' },
      { label: t('category'), keyName: 'category' },
      { label: t('primary_worker'), keyName: 'primaryUserEmail' },
      {
        label: t('customers'),
        keyName: 'customersNames',
        formatter: arrayFormatter
      },
      {
        label: t('vendors'),
        keyName: 'vendorsNames',
        formatter: arrayFormatter
      },
      {
        label: t('warranty_expiration_date'),
        keyName: 'warrantyExpirationDate'
      },
      { label: t('additional_information'), keyName: 'additionalInfos' },
      { label: t('serial_number'), keyName: 'serialNumber' },
      {
        label: t('assigned_to'),
        keyName: 'assignedToEmails',
        formatter: arrayFormatter
      },
      {
        label: t('teams'),
        keyName: 'teamsNames',
        formatter: arrayFormatter
      },
      {
        label: t('parts'),
        keyName: 'partsNames',
        formatter: arrayFormatter
      },
      { label: t('status'), keyName: 'status' },
      { label: t('acquisition_cost'), keyName: 'acquisitionCost' }
    ],
    parts: [
      {
        label: t('id'),
        keyName: 'id',
        formatter: idFormatter
      },
      { label: t('name'), keyName: 'name', required: true },
      { label: t('cost'), keyName: 'cost' },
      { label: t('category'), keyName: 'category' },
      { label: t('non_stock'), keyName: 'nonStock' },
      { label: t('barcode'), keyName: 'barcode' },
      { label: t('description'), keyName: 'description' },
      { label: t('quantity'), keyName: 'quantity', required: true },
      { label: t('additional_information'), keyName: 'additionalInfos' },
      { label: t('area'), keyName: 'area' },
      { label: t('minimum_quantity'), keyName: 'minQuantity' },
      { label: t('location_name'), keyName: 'locationName' },
      {
        label: t('customers'),
        keyName: 'customersNames',
        formatter: arrayFormatter
      },
      {
        label: t('vendors'),
        keyName: 'vendorsNames',
        formatter: arrayFormatter
      },
      {
        label: t('assigned_to'),
        keyName: 'assignedToEmails',
        formatter: arrayFormatter
      },
      {
        label: t('teams'),
        keyName: 'teamsNames',
        formatter: arrayFormatter
      }
    ],
    meters: [
      {
        label: t('id'),
        keyName: 'id',
        formatter: idFormatter
      },
      { label: t('name'), keyName: 'name', required: true },
      { label: t('unit'), keyName: 'unit', required: true },
      {
        label: t('update_frequency'),
        keyName: 'updateFrequency',
        required: true
      },
      //TODO Asset
      { label: t('asset_name'), keyName: 'assetName' },
      { label: t('category'), keyName: 'meterCategory' },
      { label: t('location_name'), keyName: 'locationName' },
      {
        label: t('users'),
        keyName: 'usersEmails',
        formatter: arrayFormatter
      }
    ],
    'preventive-maintenances': [
      { label: t('name'), keyName: 'name', required: true },
      { label: t('starts_on'), keyName: 'startsOn' },
      { label: t('ends_on'), keyName: 'endsOn' },
      { label: t('frequency'), keyName: 'frequency', required: true },
      { label: t('due_date_delay'), keyName: 'dueDateDelay' },
      {
        label: t('recurrence_type'),
        keyName: 'recurrenceType',
        required: true
      },
      {
        label: t('recurrence_based_on'),
        keyName: 'recurrenceBasedOn',
        required: true
      },
      {
        label: t('days_of_week'),
        keyName: 'daysOfWeek',
        formatter: arrayFormatter
      },
      ...workOrderHeaders.filter(
        (h) =>
          !(
            [
              'dueDate',
              'completedOn',
              'completedByEmail',
              'status',
              'feedback',
              'archived'
            ] as (keyof WorkOrderImportDTO)[]
          ).includes(h.keyName)
      )
    ]
  };
};
