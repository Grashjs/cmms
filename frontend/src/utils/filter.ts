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
          ? formatter(minis?.find((mini) => mini.id === id) || ({} as T))
          : minis?.find((mini) => mini.id === id)?.[labelAccessor].toString(),
        value: id
      })) ?? null
  );
};
export const getDateValue = (
  filterFields: FilterField[],
  fieldName: string
): [Date | null, Date | null] => {
  const find = (operation: string) => {
    const value = filterFields.find(
      (f) => f.field === fieldName && f.operation === operation
    )?.value;
    return value ? new Date(value as string) : null;
  };

  return [find('ge'), find('le')];
};

export const loadFilterFields = (
  storageKey: string,
  defaults: FilterField[]
): FilterField[] => {
  try {
    const saved = localStorage.getItem(storageKey);
    if (saved) {
      const parsed: FilterField[] = JSON.parse(saved);
      const fields = new Set(parsed.map((f) => f.field));

      for (const def of defaults) {
        if (!fields.has(def.field)) parsed.push(def);
      }

      return parsed;
    }
  } catch {
    /* ignore invalid JSON */
  }
  return defaults;
};

export const saveFilterFields = (
  storageKey: string,
  filters: FilterField[],
  excludeFields: Set<string>
): void => {
  const toSave = filters.filter((f) => !excludeFields.has(f.field));
  localStorage.setItem(storageKey, JSON.stringify(toSave));
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
