import { FileType } from '../models/owns/file';
import Meter from '../models/owns/meter';
import check from 'check-types';
import { CellBase, Matrix } from 'react-spreadsheet';
import {
  FilterField,
  SearchCriteria,
  SearchOperator
} from '../models/owns/page';
import React from 'react';
import { sameDay } from './dates';
import { googleTrackingId, IS_LOCALHOST } from '../config';
import ReactGA from 'react-ga4';
import { UaEventOptions } from 'react-ga4/types/ga4';

export const canAddReading = (meter: Meter): boolean => {
  if (!meter) {
    return false;
  }
  if (!meter.nextReading) return true;

  return new Date() >= new Date(meter.nextReading);
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

export const getType = (
  object: Object
): 'boolean' | 'string' | 'date' | 'number' | 'array' => {
  if (check.boolean(object)) return 'boolean';
  if (check.string(object)) return 'string';
  if (check.date(object)) return 'date';
  if (check.number(object)) return 'number';
  if (check.array(object)) return 'array';
};

export type SpreadsheetData = {
  [key: string]: Matrix<CellBase<any>>[];
};
export const arrayToAoA = (array: string[][]): SpreadsheetData => {
  const headers = array[0];
  array.shift();
  const result = {};
  headers.forEach((header, index) => {
    const values = [];
    array.forEach((arr) => values.push(arr[index]));
    result[header] = [values.map((value) => [{ value, readOnly: true }])];
  });
  return result;
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
  event,
  criteria: SearchCriteria,
  setCriteria: React.Dispatch<React.SetStateAction<SearchCriteria>>,
  fieldsToSearch: Extract<keyof T, string>[]
) => {
  const query = event.target.value;
  let newFilterFields: FilterField[] = [...criteria.filterFields];

  newFilterFields = newFilterFields.filter(
    // @ts-ignore
    (filterField) => !fieldsToSearch.includes(filterField.field)
  );
  const firstField = fieldsToSearch.shift();
  if (query)
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
  setCriteria({ ...criteria, filterFields: newFilterFields });
};

export const fireGa4Event = (
  optionsOrName: UaEventOptions | string,
  conversionKey?: string,
  params?: any
) => {
  if (
    !IS_LOCALHOST &&
    googleTrackingId &&
    (conversionKey ? !sessionStorage.getItem(conversionKey) : true)
  ) {
    // Fire GA4 event
    ReactGA.event(optionsOrName, params);

    // Fire UET event
    //@ts-ignore
    if (window.uetq) {
      const eventName =
        typeof optionsOrName === 'string'
          ? optionsOrName
          : optionsOrName.action || optionsOrName.category;
      //@ts-ignore
      window.uetq.push('event', eventName, params || {});
    }

    if (conversionKey) sessionStorage.setItem(conversionKey, 'true');
  }
};
