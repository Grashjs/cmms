import { combineReducers } from '@reduxjs/toolkit';
import { reducer as customerReducer } from '../slices/customer';
import { reducer as vendorReducer } from '../slices/vendor';
import { reducer as locationReducer } from '../slices/location';
import { reducer as roleReducer } from '../slices/role';
import { reducer as userReducer } from '../slices/user';
import { reducer as teamReducer } from '../slices/team';
import { reducer as meterReducer } from '../slices/meter';
import { reducer as partReducer } from '../slices/part';
import { reducer as purchaseOrderReducer } from '../slices/purchaseOrder';
import { reducer as requestReducer } from '../slices/request';
import { reducer as workOrderReducer } from '../slices/workOrder';
import { reducer as assetReducer } from '../slices/asset';
import { reducer as categoryReducer } from '../slices/category';
import { reducer as multiPartsReducer } from '../slices/multipart';
import { reducer as checklistReducer } from '../slices/checklist';
import { reducer as partQuantityReducer } from '../slices/partQuantity';
import { reducer as laborReducer } from '../slices/labor';
import { reducer as additionalCostReducer } from '../slices/additionalCost';
import { reducer as taskReducer } from '../slices/task';
import { reducer as floorPlanReducer } from '../slices/floorPlan';
import { reducer as currenciesReducer } from '../slices/currency';
import { reducer as workOrderHistoriesReducer } from '../slices/workOrderHistory';
import { reducer as relationReducer } from '../slices/relation';
import { reducer as readingReducer } from '../slices/reading';
import { reducer as fileReducer } from '../slices/file';
import { reducer as subscriptionPlanReducer } from '../slices/subscriptionPlan';
import { reducer as notificationReducer } from '../slices/notification';
import { reducer as workOrderMeterTriggerReducer } from '../slices/workOrderMeterTrigger';
import { reducer as preventiveMaintenanceReducer } from '../slices/preventiveMaintenance';
import { reducer as assetDowntimeReducer } from '../slices/assetDowntime';
import { reducer as woAnalyticsReducer } from '../slices/analytics/workOrder';
import { reducer as assetAnalyticsReducer } from '../slices/analytics/asset';
import { reducer as partAnalyticsReducer } from '../slices/analytics/part';
import { reducer as requestAnalyticsReducer } from '../slices/analytics/request';
import { reducer as userAnalyticsReducer } from '../slices/analytics/user';
import { reducer as exportsReducer } from '../slices/exports';
import { reducer as workflowReducer } from '../slices/workflow';

const rootReducer = combineReducers({
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
  exports: exportsReducer,
  workflows: workflowReducer
});

export default rootReducer;
