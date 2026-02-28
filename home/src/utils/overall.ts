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

export const companyLogosAssets: { src: string; width: number; height: number }[] = [
  { src: '/static/images/industries/logos/adventure-mechanical.png', width: 350, height: 100 },
  { src: '/static/images/industries/logos/sertec.png', width: 326, height: 100 },
  { src: '/static/images/industries/logos/complete-am.png', width: 400, height: 100 },
  { src: '/static/images/industries/logos/kwdc.png', width: 90, height: 100 },
  { src: '/static/images/industries/logos/henalux.png', width: 86, height: 100 },
  { src: '/static/images/industries/logos/penflex.png', width: 150, height: 100 },
  { src: '/static/images/industries/logos/mfwaterwork.png', width: 144, height: 100 }
];