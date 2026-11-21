import { FilterField, SearchOperator } from '../../../../models/owns/page';
import * as Yup from 'yup';
import Form from '../../components/form';
import { IField } from '../../type';
import { useTranslation } from 'react-i18next';
import { Button, Grid, Typography } from '@mui/material';
import { useDispatch, useSelector } from '../../../../store';
import { UserMiniDTO } from '../../../../models/user';
import {
  FilterFieldType,
  filterSingleField,
  getDateValue,
  getLabelAndValue
} from '../../../../utils/filter';
import { useEffect } from 'react';
import { getAssetsMini } from '../../../../slices/asset';
import { getCustomersMini } from '../../../../slices/customer';
import { getTeamsMini } from '../../../../slices/team';
import { getLocationsMini } from '../../../../slices/location';
import { getCategories } from '../../../../slices/category';
import { getUsersMini } from '../../../../slices/user';

type MoreFiltersEntity = 'workOrder' | 'request';

interface OwnProps {
  onFilterChange: (filterFields: FilterField[]) => void;
  filterFields: FilterField[];
  onClose: () => void;
  onReset: () => void;
  showEnumFilters?: boolean;
  entity?: MoreFiltersEntity;
}

function MoreFilters({
  filterFields,
  onFilterChange,
  onClose,
  onReset,
  showEnumFilters = false,
  entity = 'workOrder'
}: OwnProps) {
  const { t }: { t: any } = useTranslation();
  const { customersMini } = useSelector((state) => state.customers);
  const { locationsMini } = useSelector((state) => state.locations);
  const { categories } = useSelector((state) => state.categories);
  const { usersMini } = useSelector((state) => state.users);
  const { assetsMini } = useSelector((state) => state.assets);
  const { teamsMini } = useSelector((state) => state.teams);
  const dispatch = useDispatch();
  const isRequest = entity === 'request';

  const filtersConfig: {
    accessor: string;
    fieldName: string;
    operator?: SearchOperator;
    type: FilterFieldType;
  }[] = [
    { accessor: 'assets', fieldName: 'asset', type: 'array' },
    { accessor: 'categories', fieldName: 'category', type: 'array' },
    { accessor: 'teams', fieldName: 'team', type: 'array' },
    { accessor: 'primaryUsers', fieldName: 'primaryUser', type: 'array' },
    { accessor: 'locations', fieldName: 'location', type: 'array' },
    { accessor: 'createdBy', fieldName: 'createdBy', type: 'array' },
    { accessor: 'completedBy', fieldName: 'completedBy', type: 'array' },
    {
      accessor: 'customers',
      fieldName: 'customers',
      operator: 'inm',
      type: 'array'
    },
    {
      accessor: 'assignedTo',
      fieldName: 'assignedTo',
      operator: 'inm',
      type: 'array'
    },
    {
      accessor: 'archived',
      fieldName: 'archived',
      type: 'simple'
    },
    { accessor: 'createdAt', fieldName: 'createdAt', type: 'date' },
    { accessor: 'dueDate', fieldName: 'dueDate', type: 'dateLessThan' },
    { accessor: 'updatedAt', fieldName: 'updatedAt', type: 'date' },
    { accessor: 'completedOn', fieldName: 'completedOn', type: 'date' }
  ];
  const requestStatuses = ['APPROVED', 'CANCELLED', 'PENDING'];
  const workOrderStatuses = ['OPEN', 'IN_PROGRESS', 'ON_HOLD', 'COMPLETE'];
  const enumFields: Array<IField> = showEnumFilters
    ? [
        {
          name: 'priority',
          type: 'select',
          label: t('priority'),
          multiple: true,
          items: ['NONE', 'LOW', 'MEDIUM', 'HIGH'].map((value) => ({
            label: t(value),
            value
          }))
        },
        {
          name: 'status',
          type: 'select',
          label: t('status'),
          multiple: true,
          items: (isRequest ? requestStatuses : workOrderStatuses).map(
            (value) => ({
              label: t(value),
              value
            })
          )
        }
      ]
    : [];
  const typeField: IField = {
    name: 'type',
    type: 'select',
    label: t('type'),
    items: [
      { label: t('ALL'), value: 'ALL' },
      { label: t('REACTIVE'), value: 'REACTIVE' },
      { label: t('REPEATING'), value: 'REPEATING' }
    ]
  };
  const completedByField: IField = {
    name: 'completedBy',
    type: 'select',
    label: t('completed_by'),
    type2: 'user',
    multiple: true
  };
  const archivedField: IField = {
    name: 'archived',
    type: 'checkbox',
    label: t('archived')
  };
  const completedOnField: IField = {
    name: 'completedOn',
    type: 'dateRange',
    label: t('completed_on')
  };
  const fields: Array<IField> = [
    ...enumFields,
    ...(isRequest ? [] : [typeField]),
    {
      name: 'assets',
      type: 'select',
      label: t('asset'),
      type2: 'asset',
      multiple: true
    },
    {
      name: 'categories',
      type: 'select',
      label: t('category'),
      type2: 'category',
      category: 'work-order-categories',
      multiple: true
    },
    {
      name: 'teams',
      type: 'select',
      label: t('team'),
      type2: 'team',
      multiple: true
    },
    {
      name: 'locations',
      type: 'select',
      label: t('location'),
      type2: 'location',
      multiple: true
    },
    { name: 'peopleGroup', type: 'titleGroupField', label: t('people') },
    {
      name: 'createdBy',
      type: 'select',
      label: t('created_by'),
      type2: 'user',
      multiple: true
    },
    ...(isRequest ? [] : [completedByField]),
    {
      name: 'primaryUsers',
      type: 'select',
      label: t('primary_worker'),
      type2: 'user',
      multiple: true
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
      label: t('customer'),
      type2: 'customer',
      multiple: true
    },
    ...(isRequest ? [] : [archivedField]),
    { name: 'datesGroup', type: 'titleGroupField', label: t('dates') },
    {
      name: 'dueDate',
      type: 'date',
      label: t('due_date')
    },
    {
      name: 'createdAt',
      type: 'dateRange',
      label: t('created_at')
    },
    ...(isRequest ? [] : [completedOnField]),
    {
      name: 'updatedAt',
      type: 'dateRange',
      label: t('updated_at')
    }
  ];

  const getTypeLabelAndValue = (
    operation: SearchOperator
  ): { label: string; value: string } => {
    switch (operation) {
      case 'nu':
        return { label: t('REACTIVE'), value: 'REACTIVE' };
      case 'nn':
        return { label: t('REPEATING'), value: 'REPEATING' };
      default:
        break;
    }
  };
  const getValuesFromFilterFields = () => {
    const typeValue = filterFields.find(
      (filterField) => filterField.field === 'parentPreventiveMaintenance'
    );
    return {
      ...(isRequest
        ? {}
        : {
            type: typeValue
              ? {
                  label: getTypeLabelAndValue(typeValue.operation).label,
                  value: getTypeLabelAndValue(typeValue.operation).value
                }
              : { label: t('ALL'), value: 'ALL' },
            archived: filterFields.find(
              (filterField) => filterField.field === 'archived'
            ).value,
            completedOn: getDateValue(filterFields, 'completedOn'),
            completedBy: getLabelAndValue(
              filterFields,
              usersMini,
              'completedBy',
              null,
              (user: UserMiniDTO) => `${user.firstName} ${user.lastName}`
            )
          }),
      priority:
        filterFields.find(
          (filterField) => filterField.field === 'priority'
        )?.values?.map((value) => ({ label: t(value), value })) ?? [],
      status:
        filterFields.find(
          (filterField) => filterField.field === 'status'
        )?.values?.map((value) => ({ label: t(value), value })) ?? [],
      assets: getLabelAndValue(filterFields, assetsMini, 'asset', 'name'),
      teams: getLabelAndValue(filterFields, teamsMini, 'team', 'name'),
      categories: getLabelAndValue(
        filterFields,
        categories['work-order-categories'],
        'category',
        'name'
      ),
      primaryUsers: getLabelAndValue(
        filterFields,
        usersMini,
        'primaryUser',
        null,
        (user: UserMiniDTO) => `${user.firstName} ${user.lastName}`
      ),
      locations: getLabelAndValue(
        filterFields,
        locationsMini,
        'location',
        'name'
      ),
      assignedTo: getLabelAndValue(
        filterFields,
        usersMini,
        'assignedTo',
        null,
        (user: UserMiniDTO) => `${user.firstName} ${user.lastName}`
      ),
      customers: getLabelAndValue(
        filterFields,
        customersMini,
        'customers',
        'name'
      ),
      createdBy: getLabelAndValue(
        filterFields,
        usersMini,
        'createdBy',
        null,
        (user: UserMiniDTO) => `${user.firstName} ${user.lastName}`
      ),
      createdAt: getDateValue(filterFields, 'createdAt'),
      updatedAt: getDateValue(filterFields, 'updatedAt'),
      dueDate: filterFields.find(
        (filterField) => filterField.field === 'dueDate'
      )?.value
    };
  };
  const shape = {};
  const USER_FIELDS = ['primaryUser', 'completedBy', 'createdBy', 'assignedTo'];

  useEffect(() => {
    const fieldsInUse = new Set(filterFields.map((f) => f.field));

    if (fieldsInUse.has('asset')) dispatch(getAssetsMini());
    if (fieldsInUse.has('customers') && !customersMini.length)
      dispatch(getCustomersMini());
    if (fieldsInUse.has('team') && !teamsMini.length) dispatch(getTeamsMini());
    if (fieldsInUse.has('location') && !locationsMini.length)
      dispatch(getLocationsMini());
    if (
      fieldsInUse.has('category') &&
      !categories['work-order-categories']?.length
    )
      dispatch(getCategories('work-order-categories'));
    if (USER_FIELDS.some((f) => fieldsInUse.has(f)) && !usersMini.length)
      dispatch(getUsersMini());
  }, [filterFields.length]);

  return (
    <Grid
      container
      justifyContent="center"
      alignItems="stretch"
      spacing={1}
      padding={2}
    >
      <Grid item xs={12}>
        <Typography variant="h2">{t('more_filters')}</Typography>
      </Grid>
      <Grid item xs={12}>
        <Form
          // key={`${assetsMini.length}-${teamsMini.length}-${customersMini.length}-${locationsMini.length}-${usersMini.length}-${categories['work-order-categories']?.length}`}
          fields={fields}
          validation={Yup.object().shape(shape)}
          submitText={t('save')}
          values={getValuesFromFilterFields()}
          enableReinitialize
          nextToButton={
            <Button sx={{ ml: 2 }} onClick={onReset} variant={'outlined'}>
              {t('reset')}
            </Button>
          }
          onChange={({ field, e }) => {}}
          onSubmit={async (values) => {
            let newFilters = [...filterFields];
            filtersConfig.forEach((filterConfig) => {
              if (
                isRequest &&
                ['completedBy', 'archived', 'completedOn'].includes(
                  filterConfig.fieldName
                )
              )
                return;
              newFilters = filterSingleField(
                newFilters,
                values,
                filterConfig.accessor,
                filterConfig.fieldName,
                filterConfig.type,
                filterConfig.operator
              );
            });
            if (!isRequest) {
              // type filter
              const type = values?.type ?? { value: 'ALL' };

              newFilters = newFilters.filter(
                ({ field }) => field !== 'parentPreventiveMaintenance'
              );
              switch (type.value) {
                case 'REACTIVE':
                  newFilters.push({
                    field: 'parentPreventiveMaintenance',
                    operation: 'nu',
                    value: ''
                  });
                  break;
                case 'REPEATING':
                  newFilters.push({
                    field: 'parentPreventiveMaintenance',
                    operation: 'nn',
                    value: ''
                  });
                  break;
                default:
                  break;
              }
            }
            if (showEnumFilters) {
              ['priority', 'status'].forEach((fieldName) => {
                newFilters = newFilters.filter(
                  ({ field }) => field !== fieldName
                );
                const selected = values[fieldName] as
                  | { label: string; value: string }[]
                  | undefined;
                if (selected?.length) {
                  newFilters.push({
                    field: fieldName,
                    operation: 'in',
                    value: '',
                    values: selected.map((s) => s.value),
                    enumName: fieldName === 'priority' ? 'PRIORITY' : 'STATUS'
                  });
                }
              });
            }
            onFilterChange(newFilters);
            onClose();
          }}
        />
      </Grid>
    </Grid>
  );
}

export default MoreFilters;
