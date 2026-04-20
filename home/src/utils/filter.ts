import { FilterField, SearchOperator } from 'src/models/owns/page';

export type FilterFieldType = 'simple' | 'array' | 'date';

export const getLabelAndValue = <T extends { id: number }>(
  filterFields: FilterField[],
  minis: T[],
  fieldName: string,
  labelAccessor?: keyof T,
  formatter?: (value: T) => string
): { label: string; value: number }[] => {
  return (
    filterFields
      .find((filterField) => filterField.field === fieldName)
      ?.values?.map((id) => ({
        label: formatter
          ? formatter(minis.find((mini) => mini.id === id))
          : minis.find((mini) => mini.id === id)[labelAccessor].toString(),
        value: id
      })) ?? null
  );
};
export const getDateValue = (
  filterFields: FilterField[],
  fieldName: string
): [string, string] => {
  return [
    filterFields.find(
      (filterField) =>
        filterField.field === fieldName && filterField.operation === 'ge'
    )?.value ?? null,
    filterFields.find(
      (filterField) =>
        filterField.field === fieldName && filterField.operation === 'le'
    )?.value ?? null
  ];
};

export const filterSingleField = (
  filters: FilterField[],
  values: { [key: string]: { label: string; value: number }[] },
  accessor: string,
  fieldName: string,
  type: FilterFieldType,
  operator: SearchOperator = 'in'
): FilterField[] => {
  filters = filters.filter((filterField) => filterField.field !== fieldName);
  if (type === 'simple') {
    if (values[accessor] !== null)
      filters.push({
        field: fieldName,
        operation: 'eq',
        value: values[accessor]
      });
  } else if (type === 'array' && values[accessor]?.length) {
    const ids = values[accessor].map((element) => element.value);
    filters.push({
      field: fieldName,
      operation: operator,
      joinType: operator === 'inm' ? 'LEFT' : null,
      value: '',
      values: ids
    });
  } else if (type === 'date' && values[accessor]?.every((date) => !!date)) {
    const [start, end] = values[accessor];
    filters = [
      ...filters,
      {
        field: fieldName,
        operation: 'ge',
        value: start,
        enumName: 'JS_DATE'
      },
      { field: fieldName, operation: 'le', value: end, enumName: 'JS_DATE' }
    ];
  }
  return filters;
};
