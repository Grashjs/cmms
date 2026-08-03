import { randomInt } from './generators';
import { Task, TaskBase } from '../models/owns/tasks';

export const formatSelectMultiple = (
  array: { label: string; value: string }[] | undefined
) => {
  return array
    ? array.map(({ value }) => {
        return { id: Number(value) };
      })
    : [];
};

export const formatSelect = (
  object: { label: string; value: string } | undefined
): { id: number } | null => {
  return object?.value ? { id: Number(object.value) } : null;
};

export const formatAssetValues = (
  values,
  applicableCustomFieldIds?: number[]
) => {
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
  return formatCustomFields(newValues, applicableCustomFieldIds);
};

export const formatSwitch = (values: {}, key: string) => {
  return Array.isArray(values[key]) ? values[key].includes('on') : values[key];
};

export const getPriorityLabel = (str: string, t: any) => {
  switch (str) {
    case 'NONE':
      return t('none_priority');
    case 'LOW':
      return t('low_priority');
    case 'MEDIUM':
      return t('medium_priority');
    case 'HIGH':
      return t('high_priority');
    default:
      break;
  }
};

export const getTaskFromTaskBase = (taskBase: TaskBase): Task => {
  return { taskBase, id: randomInt(), notes: '', value: '', images: [] };
};

export const durationToHours = (duration: number) => {
  // Hours, minutes and seconds
  var hrs = ~~(duration / 3600);
  var mins = ~~((duration % 3600) / 60);
  var secs = ~~duration % 60;

  // Output like "1:01" or "4:03:59" or "123:03:59"
  var ret = '';

  if (hrs > 0) {
    ret += '' + hrs + ':' + (mins < 10 ? '0' : '');
  }

  ret += '' + mins + ':' + (secs < 10 ? '0' : '');
  ret += '' + secs;
  return ret;
};
export const getHoursAndMinutesAndSeconds = (
  duration: number
): [number, number, number] => {
  // Hours, minutes and seconds
  const hrs = ~~(duration / 3600);
  const mins = ~~((duration % 3600) / 60);
  var secs = ~~duration % 60;
  return [hrs, mins, secs];
};

export const getHMSString = (duration: number): string => {
  const [hrs, mins, secs] = getHoursAndMinutesAndSeconds(duration);
  return `${hrs}h ${mins}m ${secs}s`;
};

export const getFormattedCostPerUnit = (
  cost: number,
  unit: string,
  getFormattedCurrency: (cost: number) => string
) => {
  return unit
    ? `${getFormattedCurrency(cost)}/ ${unit}`
    : getFormattedCurrency(cost);
};

/**
 * @param applicableFieldIds ids of the fields currently shown in the form. Formik keeps
 *   values of fields that disappeared — switching an asset's category from lift to
 *   ventilation leaves "Anzahl Haltestellen" in the form state — and submitting those
 *   would be rejected by the backend. Omit to send everything, which is what entities
 *   without category-bound fields need.
 */
export const formatCustomFields = (
  values: { [key: string]: any },
  applicableFieldIds?: number[]
) => {
  const newValues = { ...values };
  let customFields: { id: number; value: string }[] = [];
  Object.keys(newValues).forEach((key) => {
    if (key.startsWith('customField_')) {
      const customFieldId = Number(key.split('customField_')[1]);
      const rawValue = newValues[key];
      delete newValues[key];
      if (applicableFieldIds && !applicableFieldIds.includes(customFieldId)) {
        return;
      }
      customFields.push({
        id: customFieldId,
        value:
          rawValue && typeof rawValue === 'object' && 'value' in rawValue
            ? rawValue.value
            : rawValue
      });
    }
  });
  newValues.customFields = customFields;
  return newValues;
};
