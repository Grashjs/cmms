import { FileType } from '../models/file';
import Meter from '../models/meter';
import { FilterField, SearchCriteria, SearchOperator } from '../models/page';
import React from 'react';
import { sameDay } from './dates';
import { Priority, WorkOrderStatus } from '../models/workOrder';
import { MD3Theme } from 'react-native-paper';
import mime from 'mime';
import ImagePicker from 'expo-image-picker';

export const canAddReading = (meter: Meter): boolean => {
  if (!meter) {
    return false;
  }
  if (!meter.nextReading) return true;
  return (
    sameDay(new Date(), new Date(meter.nextReading)) &&
    !sameDay(new Date(), new Date(meter.lastReading))
  );
};

export const getImageAndFiles = (
  files: { id: number; type: FileType }[],
  imageFallback?
) => {
  return {
    image: files.find((file) => file.type === 'IMAGE')
      ? { id: files.find((file) => file.type === 'IMAGE').id }
      : imageFallback ?? null,
    files: files
      .filter((file) => file.type === 'OTHER')
      .map((file) => {
        return { id: file.id };
      })
  };
};

export const getNextOccurence = (date: Date, days: number): Date => {
  const incrementDays = (date: Date, days: number) => {
    date.setDate(date.getDate() + days);
    return date;
  };
  let result = date;
  if (result > new Date()) {
    result = incrementDays(result, days);
  } else
    while (result < new Date()) {
      result = incrementDays(result, days);
    }
  return result;
};

export const getRandomColor = () => {
  var letters = '0123456789ABCDEF';
  var color = '#';
  for (var i = 0; i < 6; i++) {
    color += letters[Math.floor(Math.random() * 16)];
  }
  return color;
};

export const pushOrRemove = (array: string[], push: boolean, value: string) => {
  if (push) {
    array.push(value);
  } else {
    const index = array.findIndex((element) => element === value);
    if (index !== -1) array.splice(index, 1);
  }
  return array;
};

export const onSearchQueryChange = <T>(
  query,
  criteria: SearchCriteria,
  setCriteria: React.Dispatch<React.SetStateAction<SearchCriteria>>,
  setSearchQuery: React.Dispatch<React.SetStateAction<string>>,
  fieldsToSearch: Extract<keyof T, string>[]
) => {
  let newFilterFields: FilterField[] = [...criteria.filterFields];

  newFilterFields = newFilterFields.filter(
    // @ts-ignore
    (filterField) => !fieldsToSearch.includes(filterField.field)
  );
  const firstField = fieldsToSearch.shift();
  setSearchQuery(query);
  if (query) {
    newFilterFields = [
      ...newFilterFields,
      {
        field: firstField,
        value: query,
        operation: 'cn' as SearchOperator,
        alternatives: fieldsToSearch.map((field) => ({
          field,
          operation: 'cn' as SearchOperator,
          value: query
        }))
      }
    ];
  }
  setCriteria({ ...criteria, filterFields: newFilterFields });
};

export const getPriorityColor = (
  priority: Priority,
  theme: MD3Theme
): string => {
  switch (priority) {
    case 'NONE':
      return theme.colors.tertiary;
    case 'LOW':
      // @ts-ignore
      return theme.colors.info;
    case 'MEDIUM':
      // @ts-ignore
      return theme.colors.warning;
    case 'HIGH':
      return theme.colors.error;
  }
};
export type ExtendedWorkOrderStatus =
  | WorkOrderStatus
  | 'LATE_WO'
  | 'TODAY_WO'
  | 'HIGH_WO';
export const getStatusColor = (
  status: ExtendedWorkOrderStatus,
  theme: MD3Theme
): string => {
  switch (status) {
    case 'OPEN':
      return theme.colors.tertiary;
    case 'IN_PROGRESS':
      // @ts-ignore
      return theme.colors.success;
    case 'ON_HOLD':
      // @ts-ignore
      return theme.colors.warning;
    case 'LATE_WO':
      return theme.colors.error;
    case 'TODAY_WO':
      return theme.colors.primary;
    case 'HIGH_WO':
      return theme.colors.error;
    case 'COMPLETE':
      return 'black';
  }
};

export function readFileAsync(file: Blob) {
  return new Promise((resolve, reject) => {
    let reader = new FileReader();

    reader.onload = () => {
      resolve(reader.result);
    };

    reader.onerror = reject;

    reader.readAsArrayBuffer(file);
  });
}

export function formatImages(
  result: ImagePicker.ImagePickerResult
): { uri: string; name: string; type: string }[] {
  return result.assets.map((asset) => {
    const fileName = asset.uri.split('/')[asset.uri.split('/').length - 1];
    return {
      uri: asset.uri,
      name: fileName,
      type: mime.getType(fileName)
    };
  });
}
