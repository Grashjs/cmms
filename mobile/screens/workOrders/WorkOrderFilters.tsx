import { StyleSheet } from 'react-native';
import { View } from '../../components/Themed';
import { RootStackScreenProps } from '../../types';
import { useTranslation } from 'react-i18next';
import { useSelector } from '../../store';
import { SearchOperator } from '../../models/page';
import {
  FilterFieldType,
  filterSingleField,
  getDateValue,
  getLabelAndValue
} from '../../utils/filter';
import { IField } from '../../models/form';
import { UserMiniDTO } from '../../models/user';
import Form from '../../components/form';
import * as Yup from 'yup';

export default function WorkOrderFilters({
  navigation,
  route
}: RootStackScreenProps<'WorkOrderFilters'>) {
  const { t }: { t: any } = useTranslation();
  const { filterFields, onFilterChange } = route.params;
  const { customersMini } = useSelector((state) => state.customers);
  const { locationsMini } = useSelector((state) => state.locations);
  const { categories } = useSelector((state) => state.categories);
  const { usersMini } = useSelector((state) => state.users);
  const { assetsMini } = useSelector((state) => state.assets);
  const { teamsMini } = useSelector((state) => state.teams);

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
      fieldName: 'customer',
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
    { accessor: 'updatedAt', fieldName: 'updatedAt', type: 'date' },
    { accessor: 'completedOn', fieldName: 'completedOn', type: 'date' }
  ];
  const fields: Array<IField> = [
    {
      name: 'type',
      type: 'select',
      label: t('type'),
      items: [
        { label: t('ALL'), value: 'ALL' },
        { label: t('REACTIVE'), value: 'REACTIVE' },
        { label: t('REPEATING'), value: 'REPEATING' }
      ]
    },
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
    {
      name: 'completedBy',
      type: 'select',
      label: t('completed_by'),
      type2: 'user',
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
      label: t('customer'),
      type2: 'customer',
      multiple: true
    },
    {
      name: 'archived',
      type: 'switch',
      label: t('archived')
    },
    { name: 'datesGroup', type: 'titleGroupField', label: t('dates') },
    {
      name: 'createdAt',
      type: 'dateRange',
      label: t('created_at')
    },
    {
      name: 'completedOn',
      type: 'dateRange',
      label: t('completed_on')
    },
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
  const getValuesFromFilterFields = (): {
    [key: string]:
      | { label: string; value: string }
      | { label: string; value: number }[]
      | boolean
      | string
      | [string, string];
  } => {
    const typeValue = filterFields.find(
      (filterField) => filterField.field === 'parentPreventiveMaintenance'
    );
    return {
      type: typeValue ? getTypeLabelAndValue(typeValue.operation).value : 'ALL',
      archived: filterFields.find(
        (filterField) => filterField.field === 'archived'
      ).value,
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
      completedBy: getLabelAndValue(
        filterFields,
        usersMini,
        'completedBy',
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
      createdBy: getLabelAndValue(
        filterFields,
        usersMini,
        'createdBy',
        null,
        (user: UserMiniDTO) => `${user.firstName} ${user.lastName}`
      ),
      createdAt: getDateValue(filterFields, 'createdAt'),
      updatedAt: getDateValue(filterFields, 'updatedAt'),
      completedOn: getDateValue(filterFields, 'completedOn')
    };
  };
  const shape = {};
  return (
    <View style={styles.container}>
      <Form
        navigation={navigation}
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
          onFilterChange(newFilters);
          navigation.goBack();
        }}
      />
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1
  }
});
