import { combineReducers } from '@reduxjs/toolkit';
import { reducer as calendarReducer } from 'src/slices/calendar';
import { reducer as projectsBoardReducer } from 'src/slices/projects_board';
import { reducer as mailboxReducer } from 'src/slices/mailbox';
import { reducer as customerReducer } from 'src/slices/customer';
import { reducer as vendorReducer } from 'src/slices/vendor';
import { reducer as locationReducer } from 'src/slices/location';
import { reducer as roleReducer } from 'src/slices/role';
import { reducer as userReducer } from 'src/slices/user';
import { reducer as teamReducer } from 'src/slices/team';
import { reducer as meterReducer } from 'src/slices/meter';
import { reducer as partReducer } from 'src/slices/part';
import { reducer as purchaseOrderReducer } from 'src/slices/purchaseOrder';
import { reducer as requestReducer } from 'src/slices/request';
import { reducer as workOrderReducer } from 'src/slices/workOrder';
import { reducer as assetReducer } from 'src/slices/asset';
import { reducer as categoryReducer } from 'src/slices/category';
import { reducer as multiPartsReducer } from 'src/slices/multipart';
import { reducer as checklistReducer } from 'src/slices/checklist';
import { reducer as partQuantityReducer } from 'src/slices/partQuantity';
import { reducer as laborReducer } from 'src/slices/labor';
import { reducer as additionalCostReducer } from 'src/slices/additionalCost';
import { reducer as taskReducer } from 'src/slices/task';
import { reducer as floorPlanReducer } from 'src/slices/floorPlan';
import { reducer as currenciesReducer } from 'src/slices/currency';
import { reducer as workOrderHistoriesReducer } from 'src/slices/workOrderHistory';
import { reducer as relationReducer } from 'src/slices/relation';
import { reducer as readingReducer } from 'src/slices/reading';
import { reducer as fileReducer } from 'src/slices/file';
import { reducer as subscriptionPlanReducer } from 'src/slices/subscriptionPlan';
import { reducer as notificationReducer } from 'src/slices/notification';
import { reducer as workOrderMeterTriggerReducer } from 'src/slices/workOrderMeterTrigger';
import { reducer as preventiveMaintenanceReducer } from 'src/slices/preventiveMaintenance';
import { reducer as assetDowntimeReducer } from 'src/slices/assetDowntime';
import { reducer as woAnalyticsReducer } from 'src/slices/analytics/workOrder';
import { reducer as assetAnalyticsReducer } from 'src/slices/analytics/asset';
import { reducer as partAnalyticsReducer } from 'src/slices/analytics/part';
import { reducer as requestAnalyticsReducer } from 'src/slices/analytics/request';
import { reducer as userAnalyticsReducer } from 'src/slices/analytics/user';
import { reducer as importsReducer } from 'src/slices/imports';
import { reducer as exportsReducer } from 'src/slices/exports';
import { reducer as workflowReducer } from 'src/slices/workflow';

const rootReducer = combineReducers({
  calendar: calendarReducer,
  projectsBoard: projectsBoardReducer,
  mailbox: mailboxReducer,
  customers: customerReducer,
  vendors: vendorReducer,
  locations: locationReducer,
  roles: roleReducer,
  users: userReducer,
  teams: teamReducer,
  assets: assetReducer,
  meters: meterReducer,
  parts: partReducer,
  purchaseOrders: purchaseOrderReducer,
  requests: requestReducer,
  workOrders: workOrderReducer,
  categories: categoryReducer,
  multiParts: multiPartsReducer,
  checklists: checklistReducer,
  partQuantities: partQuantityReducer,
  labors: laborReducer,
  additionalCosts: additionalCostReducer,
  tasks: taskReducer,
  floorPlans: floorPlanReducer,
  currencies: currenciesReducer,
  workOrderHistories: workOrderHistoriesReducer,
  relations: relationReducer,
  readings: readingReducer,
  files: fileReducer,
  subscriptionPlans: subscriptionPlanReducer,
  notifications: notificationReducer,
  workOrderMeterTriggers: workOrderMeterTriggerReducer,
  preventiveMaintenances: preventiveMaintenanceReducer,
  downtimes: assetDowntimeReducer,
  woAnalytics: woAnalyticsReducer,
  assetAnalytics: assetAnalyticsReducer,
  partAnalytics: partAnalyticsReducer,
  requestAnalytics: requestAnalyticsReducer,
  userAnalytics: userAnalyticsReducer,
  imports: importsReducer,
  exports: exportsReducer,
  workflows: workflowReducer
});

export default rootReducer;
