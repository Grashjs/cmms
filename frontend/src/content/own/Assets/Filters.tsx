import { FilterField, SearchOperator } from '../../../models/owns/page';
import * as Yup from 'yup';
import Form from '../components/form';
import { IField } from '../type';
import { useTranslation } from 'react-i18next';
import { Grid, Typography } from '@mui/material';
import { useSelector } from '../../../store';
import { UserMiniDTO } from '../../../models/user';
import {
  FilterFieldType,
  filterSingleField,
  getDateValue,
  getLabelAndValue
} from '../../../utils/filter';

interface OwnProps {
  onFilterChange: (filterFields: FilterField[]) => void;
  filterFields: FilterField[];
  onSave: () => void;
}

function Filters({ filterFields, onFilterChange, onSave }: OwnProps) {
  const { t }: { t: any } = useTranslation();
  const { customersMini } = useSelector((state) => state.customers);
  const { locationsMini } = useSelector((state) => state.locations);
  const { categories } = useSelector((state) => state.categories);
  const { usersMini } = useSelector((state) => state.users);
  const { teamsMini } = useSelector((state) => state.teams);

  const filtersConfig: {
    accessor: string;
    fieldName: string;
    operator?: SearchOperator;
    type: FilterFieldType;
  }[] = [
    { accessor: 'categories', fieldName: 'category', type: 'array' },
    { accessor: 'locations', fieldName: 'location', type: 'array' },
    {
      accessor: 'area',
      fieldName: 'area',
      type: 'simple'
    },
    {
      accessor: 'model',
      fieldName: 'model',
      type: 'simple'
    },
    { accessor: 'createdBy', fieldName: 'createdBy', type: 'array' },

    { accessor: 'teams', fieldName: 'team', type: 'array' },
    { accessor: 'primaryUsers', fieldName: 'primaryUser', type: 'array' },
    {
      accessor: 'assignedTo',
      fieldName: 'assignedTo',
      operator: 'inm',
      type: 'array'
    },
    {
      accessor: 'customers',
      fieldName: 'customer',
      operator: 'inm',
      type: 'array'
    },
    {
      accessor: 'vendors',
      fieldName: 'vendor',
      operator: 'inm',
      type: 'array'
    },

    {
      accessor: 'archived',
      fieldName: 'archived',
      type: 'simple'
    },
    { accessor: 'createdAt', fieldName: 'createdAt', type: 'date' },
    { accessor: 'updatedAt', fieldName: 'updatedAt', type: 'date' }
  ];
  const fields: Array<IField> = [
    {
      name: 'categories',
      type: 'select',
      label: t('category'),
      type2: 'category',
      category: 'asset-categories'
    },
    {
      name: 'locations',
      type: 'select',
      label: t('location'),
      type2: 'location',
      multiple: true
    },
    {
      name: 'area',
      type: 'text',
      label: t('area')
    },
    {
      name: 'model',
      type: 'text',
      label: t('model')
    },

    { name: 'peopleGroup', type: 'titleGroupField', label: t('people') },
    {
      name: 'createdBy',
      type: 'select',
      label: t('created_by'),
      type2: 'user',
      multiple: true
    },
    {
      name: 'teams',
      type: 'select',
      label: t('team'),
      type2: 'team',
      multiple: true
    },
    //TODO
    // {
    //   name: 'requestedBy',
    //   type: 'select',
    //   label: t('requested_by'),
    //   type2: 'user',
    //   multiple: true
    // },
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
      label: t('customers'),
      type2: 'customer',
      multiple: true
    },
    {
      name: 'vendors',
      type: 'select',
      label: t('vendors'),
      type2: 'vendor',
      multiple: true
    },
    {
      name: 'archived',
      type: 'checkbox',
      label: t('archived')
    },
    { name: 'datesGroup', type: 'titleGroupField', label: t('dates') },
    {
      name: 'createdAt',
      type: 'dateRange',
      label: t('created_at')
    },
    {
      name: 'updatedAt',
      type: 'dateRange',
      label: t('updated_at')
    }
  ];

  const getValuesFromFilterFields = (): {
    [key: string]:
      | { label: string; value: string }
      | { label: string; value: number }[]
      | boolean
      | [string, string];
  } => {
    return {
      categories: getLabelAndValue(
        filterFields,
        categories['asset-categories'],
        'category',
        'name'
      ),
      locations: getLabelAndValue(
        filterFields,
        locationsMini,
        'location',
        'name'
      ),
      area:
        filterFields.find((filterField) => filterField.field === 'area')
          ?.value ?? null,
      model:
        filterFields.find((filterField) => filterField.field === 'model')
          ?.value ?? null,
      createdBy: getLabelAndValue(
        filterFields,
        usersMini,
        'createdBy',
        null,
        (user: UserMiniDTO) => `${user.firstName} ${user.lastName}`
      ),
      teams: getLabelAndValue(filterFields, teamsMini, 'team', 'name'),
      primaryUsers: getLabelAndValue(
        filterFields,
        usersMini,
        'primaryUser',
        null,
        (user: UserMiniDTO) => `${user.firstName} ${user.lastName}`
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
        'customer',
        'name'
      ),
      vendors: getLabelAndValue(filterFields, customersMini, 'vendor', 'name'),
      archived: filterFields.find(
        (filterField) => filterField.field === 'archived'
      ).value,
      createdAt: getDateValue(filterFields, 'createdAt'),
      updatedAt: getDateValue(filterFields, 'updatedAt')
    };
  };
  const shape = {};

  return (
    <Grid
      container
      justifyContent="center"
      alignItems="stretch"
      spacing={1}
      padding={2}
    >
      <Grid item xs={12}>
        <Typography variant="h2">{t('filters')}</Typography>
      </Grid>
      <Grid item xs={12}>
        <Form
          fields={fields}
          validation={Yup.object().shape(shape)}
          submitText={t('save')}
          values={getValuesFromFilterFields()}
          onChange={({ field, e }) => {}}
          onSubmit={async (values) => {
            let newFilters = [...filterFields];
            filtersConfig.forEach((filterConfig) => {
              newFilters = filterSingleField(
                newFilters,
                values,
                filterConfig.accessor,
                filterConfig.fieldName,
                filterConfig.type,
                filterConfig.operator
              );
            });
            onFilterChange(newFilters);
            onSave();
          }}
        />
      </Grid>
    </Grid>
  );
}

export default Filters;
