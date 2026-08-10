/**
 * Learn more about deep linking with React Navigation
 * https://reactnavigation.org/docs/deep-linking
 * https://reactnavigation.org/docs/configuring-links
 */

import { LinkingOptions } from '@react-navigation/native';
import * as Linking from 'expo-linking';

import { RootStackParamList } from '../types';

/**
 * Only routes that are reachable without navigation parameters are mapped.
 * Detail screens take entity objects rather than ids, so they cannot be
 * reconstructed from a URL until they are changed to resolve by id.
 */
const linking: LinkingOptions<RootStackParamList> = {
  prefixes: [Linking.createURL('/')],
  config: {
    screens: {
      Root: {
        screens: {
          Home: 'home',
          WorkOrders: 'work-orders',
          Requests: 'requests',
          MoreEntities: 'more'
        }
      },
      Assets: 'assets',
      Locations: 'locations',
      Parts: 'parts',
      Meters: 'meters',
      PeopleTeams: 'people',
      VendorsCustomers: 'vendors',
      WorkOrderStats: 'work-order-stats',
      Notifications: 'notifications',
      Settings: 'settings',
      ScanAsset: 'scan',
      NotFound: '*'
    }
  }
};

export default linking;
