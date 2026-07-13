/**
 * If you are not familiar with React Navigation, refer to the "Fundamentals" guide:
 * https://reactnavigation.org/docs/getting-started
 *
 */
import { createBottomTabNavigator } from '@react-navigation/bottom-tabs';
import { DefaultTheme, NavigationContainer } from '@react-navigation/native';
import { createNativeStackNavigator } from '@react-navigation/native-stack';
import * as React from 'react';
import {
  ColorSchemeName,
  GestureResponderEvent,
  Image,
  Pressable,
  TouchableOpacity,
  View
} from 'react-native';
import ModalScreen from '../screens/ModalScreen';
import NotFoundScreen from '../screens/NotFoundScreen';
import HomeScreen from '../screens/HomeScreen';
import RegisterScreen from '../screens/auth/RegisterScreen';
import LoginScreen from '../screens/auth/LoginScreen';
import VerifyScreen from '../screens/auth/VerifyScreen';
import LoadingScreen from '../screens/auth/LoadingScreen';
import WODetailsScreen from '../screens/workOrders/WODetailsScreen';
import CreateWorkOrder from '../screens/workOrders/CreateWorkOrderScreen';
import EditWorkOrder from '../screens/workOrders/EditWorkOrderScreen';
import CreateRequestScreen from '../screens/requests/CreateRequestScreen';
import CreateAssetScreen from '../screens/assets/CreateAssetScreen';
import CreateLocationScreen from '../screens/locations/CreateLocationScreen';
import CreateMeterScreen from '../screens/meters/CreateMeterScreen';
import CreatePartScreen from '../screens/parts/CreatePartScreen';
import WorkOrderStatsScreen from '../screens/WorkOrderStatsScreen';
import {
  AuthStackParamList,
  RootStackParamList,
  RootTabParamList,
  RootTabScreenProps,
  SuperUserStackParamList
} from '../types';
import LinkingConfiguration from './LinkingConfiguration';
import useAuth from '../hooks/useAuth';
import { useTranslation } from 'react-i18next';
import { IconButton, Text, useTheme } from 'react-native-paper';
import { IconSource } from 'react-native-paper/src/components/Icon';
import MoreEntitiesScreen from '../screens/MoreEntitiesScreen';
import MetersScreen from '../screens/meters/MetersScreen';
import WorkOrdersScreen from '../screens/workOrders/WorkOrdersScreen';
import { SheetManager } from 'react-native-actions-sheet';
import CompleteWorkOrderModal from '../screens/workOrders/CompleteWorkOrderModal';
import SelectPartsModal from '../screens/modals/SelectPartsModal';
import TasksScreen from '../screens/workOrders/TasksScreen';
import SelectCustomersModal from '../screens/modals/SelectCustomersModal';
import SelectVendorsModal from '../screens/modals/SelectCustomersModal';
import SelectUsersModal from '../screens/modals/SelectUsersModal';
import SelectTeamsModal from '../screens/modals/SelectTeamsModal';
import SelectLocationsModal from '../screens/modals/SelectLocationsModal';
import SelectAssetsModal from '../screens/modals/SelectAssetsModal';
import SelectCategoriesModal from '../screens/modals/SelectCategoryModal';
import SelectTasksModal from '../screens/modals/SelectTasksModal';
import SelectChecklistsModal from '../screens/modals/SelectChecklistsModal';
import SelectTasksOrChecklistModal from '../screens/modals/SelectTasksOrChecklistModal';
import PartsScreen from '../screens/parts/PartsScreen';
import VendorsAndCustomersScreen from '../screens/vendorsCustomers';
import PeopleAndTeamsScreen from '../screens/peopleTeams';
import NotificationsScreen from '../screens/NotificationsScreen';
import AssetsScreen from '../screens/assets/AssetsScreen';
import LocationsScreen from '../screens/locations/LocationsScreen';
import AssetDetails from '../screens/assets/details';
import EditAssetScreen from '../screens/assets/EditAssetScreen';
import LocationDetails from '../screens/locations/details';
import EditLocationScreen from '../screens/locations/EditLocationScreen';
import PartDetails from '../screens/parts/details';
import EditPartScreen from '../screens/parts/EditPartScreen';
import CustomerDetailsScreen from '../screens/vendorsCustomers/details/CustomerDetailsScreen';
import EditCustomerScreen from '../screens/vendorsCustomers/EditCustomerScreen';
import VendorDetailsScreen from '../screens/vendorsCustomers/details/VendorDetailsScreen';
import EditVendorScreen from '../screens/vendorsCustomers/EditVendorScreen';
import MeterDetails from '../screens/meters/MeterDetails';
import EditMeterScreen from '../screens/meters/EditMeterScreen';
import TeamDetails from '../screens/peopleTeams/TeamDetails';
import EditTeamScreen from '../screens/peopleTeams/EditTeamScreen';
import RequestDetails from '../screens/requests/RequestDetails';
import EditRequestScreen from '../screens/requests/EditRequestScreen';
import UserDetails from '../screens/peopleTeams/UserDetails';
import UserProfile from '../screens/peopleTeams/Profile';
import InviteUserScreen from '../screens/peopleTeams/InviteUserScreen';
import { navigationRef } from './RootNavigation';
import SettingsScreen from '../screens/SettingsScreen';
import WorkOrderFilters from '../screens/workOrders/WorkOrderFilters';
import CreateAdditionalCost from '../screens/workOrders/CreateAdditionalCost';
import CreateAdditionalTime from '../screens/workOrders/CreateAdditionalTime';
import SelectNfcModal from '../screens/modals/SelectNfcModal';
import SelectBarcodeModal from '../screens/modals/SelectBarcodeModal';
import ScanAssetScreen from '../screens/ScanAssetScreen';
import SelectMetersModal from '../screens/modals/SelectMetersModal';
import {
  createEntities,
  PermissionEntity,
  viewMoreEntities
} from '../models/role';
import RequestsScreen from '../screens/requests/RequestsScreen';
import SwitchAccountScreen from '../screens/superUser/SwitchAccountScreen';
import { FontAwesome, Ionicons, Feather } from '@expo/vector-icons';
import { Fragment, ReactElement, ReactNode } from 'react';

export default function Navigation({
  colorScheme
}: {
  colorScheme: ColorSchemeName;
}) {
  const { isAuthenticated, isInitialized, user } = useAuth();
  return (
    <NavigationContainer
      ref={navigationRef}
      linking={LinkingConfiguration}
      theme={DefaultTheme}
    >
      {isInitialized ? (
        isAuthenticated ? (
          user.superAccountRelations.length ? (
            <SuperUserNavigator />
          ) : (
            <RootNavigator />
          )
        ) : (
          <AuthNavigator />
        )
      ) : (
        <LoadingScreen />
      )}
    </NavigationContainer>
  );
}

/**
 * A root stack navigator is often used for displaying modals on top of all other content.
 * https://reactnavigation.org/docs/modal
 */
const Stack = createNativeStackNavigator<RootStackParamList>();

function RootNavigator() {
  const { t } = useTranslation();
  return (
    <Stack.Navigator>
      <Stack.Screen
        name="Root"
        component={BottomTabNavigator}
        options={{ headerShown: false }}
      />
      <Stack.Screen
        name="WODetails"
        component={WODetailsScreen}
        options={{ title: t('wo_details') }}
      />
      <Stack.Screen
        name="Tasks"
        component={TasksScreen}
        options={{ title: t('tasks') }}
      />
      <Stack.Screen
        name="NotFound"
        component={NotFoundScreen}
        options={{ title: 'Oops!' }}
      />
      <Stack.Screen
        name="AddWorkOrder"
        component={CreateWorkOrder}
        options={{ title: t('create_work_order') }}
      />
      <Stack.Screen
        name="EditWorkOrder"
        component={EditWorkOrder}
        options={{ title: t('edit_work_order') }}
      />
      <Stack.Screen
        name="AddRequest"
        component={CreateRequestScreen}
        options={{ title: t('create_request') }}
      />
      <Stack.Screen
        name="AddAsset"
        component={CreateAssetScreen}
        options={{ title: t('create_asset') }}
      />
      <Stack.Screen
        name="AddLocation"
        component={CreateLocationScreen}
        options={{ title: t('create_location') }}
      />
      <Stack.Screen
        name="AddPart"
        component={CreatePartScreen}
        options={{ title: t('create_part') }}
      />
      <Stack.Screen
        name="AddMeter"
        component={CreateMeterScreen}
        options={{ title: t('create_meter') }}
      />
      <Stack.Screen
        name="AddUser"
        component={InviteUserScreen}
        options={{ title: t('invite_users') }}
      />
      <Stack.Screen
        name="WorkOrderStats"
        component={WorkOrderStatsScreen}
        options={{ title: t('stats') }}
      />
      <Stack.Screen
        name="Meters"
        component={MetersScreen}
        options={{ title: t('meters') }}
      />
      <Stack.Screen
        name="MeterDetails"
        component={MeterDetails}
        options={{ title: t('meter_details') }}
      />
      <Stack.Screen
        name="EditMeter"
        component={EditMeterScreen}
        options={{ title: t('edit_meter') }}
      />
      <Stack.Screen
        name="Parts"
        component={PartsScreen}
        options={{ title: t('parts') }}
      />
      <Stack.Screen
        name="PartDetails"
        component={PartDetails}
        options={{ title: t('part') }}
      />
      <Stack.Screen
        name="RequestDetails"
        component={RequestDetails}
        options={{ title: t('request_details') }}
      />
      <Stack.Screen
        name="EditRequest"
        component={EditRequestScreen}
        options={{ title: t('edit_request') }}
      />
      <Stack.Screen
        name="EditPart"
        component={EditPartScreen}
        options={{ title: t('update_part') }}
      />
      <Stack.Screen
        name="VendorsCustomers"
        component={VendorsAndCustomersScreen}
        options={{ title: t('vendors_and_customers') }}
      />
      <Stack.Screen
        name="CustomerDetails"
        component={CustomerDetailsScreen}
        options={{ title: t('customer') }}
      />
      <Stack.Screen
        name="EditCustomer"
        component={EditCustomerScreen}
        options={{ title: t('edit_customer') }}
      />
      <Stack.Screen
        name="VendorDetails"
        component={VendorDetailsScreen}
        options={{ title: t('vendor') }}
      />
      <Stack.Screen
        name="EditVendor"
        component={EditVendorScreen}
        options={{ title: t('edit_vendor') }}
      />
      <Stack.Screen
        name="Assets"
        component={AssetsScreen}
        options={{ title: t('assets') }}
      />
      <Stack.Screen
        name="AssetDetails"
        component={AssetDetails}
        options={{ title: t('asset') }}
      />
      <Stack.Screen
        name="EditAsset"
        component={EditAssetScreen}
        options={{ title: t('edit_asset') }}
      />
      <Stack.Screen
        name="Locations"
        component={LocationsScreen}
        options={{ title: t('locations') }}
      />
      <Stack.Screen
        name="LocationDetails"
        component={LocationDetails}
        options={{ title: t('location') }}
      />
      <Stack.Screen
        name="EditLocation"
        component={EditLocationScreen}
        options={{ title: t('edit_location') }}
      />
      <Stack.Screen
        name="PeopleTeams"
        component={PeopleAndTeamsScreen}
        options={{ title: t('people_teams') }}
      />
      <Stack.Screen
        name="TeamDetails"
        component={TeamDetails}
        options={{ title: t('team') }}
      />
      <Stack.Screen
        name="UserDetails"
        component={UserDetails}
        options={{ title: t('user_details') }}
      />
      <Stack.Screen
        name="UserProfile"
        component={UserProfile}
        options={{ title: t('profile') }}
      />
      <Stack.Screen
        name="EditTeam"
        component={EditTeamScreen}
        options={{ title: t('edit') }}
      />
      <Stack.Screen
        name="Notifications"
        component={NotificationsScreen}
        options={{ title: t('Notifications') }}
      />
      <Stack.Screen
        name="Settings"
        component={SettingsScreen}
        options={{ title: t('settings') }}
      />
      <Stack.Screen
        name="WorkOrderFilters"
        component={WorkOrderFilters}
        options={{ title: t('filters') }}
      />
      <Stack.Screen
        name="ScanAsset"
        component={ScanAssetScreen}
        options={{ title: t('to_scan') }}
      />
      <Stack.Group screenOptions={{ presentation: 'modal' }}>
        <Stack.Screen name="Modal" component={ModalScreen} />
        <Stack.Screen
          name="CompleteWorkOrder"
          component={CompleteWorkOrderModal}
          options={{ title: t('complete_work_order') }}
        />
        <Stack.Screen
          name="SelectParts"
          component={SelectPartsModal}
          options={{ title: t('select_parts') }}
        />
        <Stack.Screen
          name="SelectMeters"
          component={SelectMetersModal}
          options={{ title: t('select_meter') }}
        />
        <Stack.Screen
          name="SelectNfc"
          component={SelectNfcModal}
          options={{ title: t('to_scan') }}
        />
        <Stack.Screen
          name="SelectBarcode"
          component={SelectBarcodeModal}
          options={{ title: t('to_scan') }}
        />
        <Stack.Screen
          name="SelectCustomers"
          component={SelectCustomersModal}
          options={{ title: t('select_customers') }}
        />
        <Stack.Screen
          name="SelectVendors"
          component={SelectVendorsModal}
          options={{ title: t('select_vendors') }}
        />
        <Stack.Screen
          name="SelectUsers"
          component={SelectUsersModal}
          options={{ title: t('select_users') }}
        />
        <Stack.Screen
          name="SelectTeams"
          component={SelectTeamsModal}
          options={{ title: t('select_teams') }}
        />
        <Stack.Screen
          name="SelectLocations"
          component={SelectLocationsModal}
          options={{ title: t('select_locations') }}
        />
        <Stack.Screen
          name="SelectAssets"
          component={SelectAssetsModal}
          options={{ title: t('select_assets') }}
        />
        <Stack.Screen
          name="SelectCategories"
          component={SelectCategoriesModal}
          options={{ title: t('select_categories') }}
        />
        <Stack.Screen
          name="SelectTasks"
          component={SelectTasksModal}
          options={{ title: t('add_task') }}
        />
        <Stack.Screen
          name="SelectChecklists"
          component={SelectChecklistsModal}
          options={{ title: t('add_task') }}
        />
        <Stack.Screen
          name="SelectTasksOrChecklist"
          component={SelectTasksOrChecklistModal}
          options={{ title: t('add_task') }}
        />
        <Stack.Screen
          name="AddAdditionalCost"
          component={CreateAdditionalCost}
          options={{ title: t('add_cost') }}
        />
        <Stack.Screen
          name="AddAdditionalTime"
          component={CreateAdditionalTime}
          options={{ title: t('add_time') }}
        />
      </Stack.Group>
    </Stack.Navigator>
  );
}

const AuthStack = createNativeStackNavigator<AuthStackParamList>();
const SuperUserStack = createNativeStackNavigator<SuperUserStackParamList>();

function AuthNavigator() {
  const { t } = useTranslation();
  return (
    <AuthStack.Navigator>
      <AuthStack.Screen
        name="Login"
        component={LoginScreen}
        options={{ title: t('login') }}
      />
      <AuthStack.Screen
        name="Register"
        component={RegisterScreen}
        options={{ title: t('register') }}
      />
      <AuthStack.Screen
        name="Verify"
        component={VerifyScreen}
        options={{ title: t('verify_email_title') }}
      />
      <AuthStack.Screen
        name="CustomServer"
        component={React.lazy(() => import('../screens/auth/CustomServerScreen'))}
        options={{ title: t('custom_server') }}
      />
    </AuthStack.Navigator>
  );
}

function SuperUserNavigator() {
  const { t } = useTranslation();
  return (
    <SuperUserStack.Navigator>
      <SuperUserStack.Screen
        name="SwitchAccount"
        component={SwitchAccountScreen}
        options={{ title: t('switch_account') }}
      />
    </SuperUserStack.Navigator>
  );
}

/**
 * A bottom tab navigator displays tab buttons on the bottom of the display to switch screens.
 * https://reactnavigation.org/docs/bottom-tab-navigator
 */
const BottomTab = createBottomTabNavigator<RootTabParamList>();

function CreateTabBarButton(props: {
  onPress: (
    e: React.MouseEvent<HTMLAnchorElement, MouseEvent> | GestureResponderEvent
  ) => void;
  children: React.ReactNode;
}): ReactElement {
  const theme = useTheme();

  return (
    <TouchableOpacity
      style={{
        top: -25,
        justifyContent: 'center',
        alignItems: 'center'
      }}
      onPress={props.onPress}
    >
      <View
        style={{
          width: 20
        }}
      >
        {props.children}
      </View>
    </TouchableOpacity>
  );
}

function BottomTabNavigator({ navigation }: RootTabScreenProps<'Home'>) {
  const theme = useTheme();
  const { t } = useTranslation();
  const { hasViewPermission, hasCreatePermission, user } = useAuth();
  const uiConfiguration = user.uiConfiguration;
  let leftButtonsCount = user.role.code !== 'REQUESTER' ? 1 : 0;
  let rightButtonsCount = 0;
  if (createEntities.some((entity) => hasCreatePermission(entity))) {
    leftButtonsCount++;
  }
  if (
    hasViewPermission(PermissionEntity.REQUESTS) &&
    uiConfiguration.requests
  ) {
    rightButtonsCount++;
  }
  if (viewMoreEntities.some((entity) => hasViewPermission(entity))) {
    rightButtonsCount++;
  }

  // Determine whether to show the create button big in the center
  const showBigCreateButton = leftButtonsCount === rightButtonsCount;

  return (
    <BottomTab.Navigator
      initialRouteName={user.role.code === 'REQUESTER' ? 'Requests' : 'Home'}
      screenOptions={{
        tabBarActiveTintColor: theme.colors.primary,
        tabBarStyle: {
          position: 'absolute',
          bottom: 10,
          left: 20,
          right: 20,
          elevation: 8,
          borderRadius: 15,
          zIndex: 10,
          height: 50
        }
      }}
    >
      {user.role.code !== 'REQUESTER' && (
        <BottomTab.Screen
          name="Home"
          component={HomeScreen}
          options={({ navigation }: RootTabScreenProps<'Home'>) => ({
            headerTitle: (props) => (
              <Text
                style={{
                  color: theme.colors.primary,
                  fontSize: 22,
                  fontWeight: 'bold'
                }}
              >
                Atlas
              </Text>
            ),
            title: t('home'),
            tabBarIcon: ({ color, focused }) => (
              <Ionicons
                name={focused ? 'home' : 'home-outline'}
                color={color}
                size={30}
              />
            ),
            headerRight: () => (
              <Pressable
                onPress={() => {
                  navigation.navigate('Settings');
                }}
              >
                <IconButton icon="cog-outline" />
              </Pressable>
            )
          })}
        />
      )}
      <BottomTab.Screen
        name="WorkOrders"
        component={WorkOrdersScreen}
        options={{
          title: t('work_orders'),
          tabBarIcon: ({ color, focused }) => (
            <Ionicons
              name={focused ? 'clipboard' : 'clipboard-outline'}
              size={30}
              color={color}
            />
          )
        }}
      />
      {createEntities.some((entity) => hasCreatePermission(entity)) && (
        <BottomTab.Screen
          name="AddEntities"
          component={View}
          listeners={{
            tabPress: (e) => {
              e.preventDefault();
              if (user.role.code === 'REQUESTER')
                navigation.navigate('AddRequest');
              else
                SheetManager.show('create-entities-sheet', {
                  payload: { navigation }
                });
            }
          }}
          options={{
            title: showBigCreateButton ? '' : t('create'),
            tabBarIcon: ({ focused }) => (
              <IconButton
                icon={'plus-circle'}
                iconColor={theme.colors.primary}
                size={showBigCreateButton ? 60 : 35}
              />
            ),
            ...(showBigCreateButton
              ? {
                  tabBarButton: (props) => (
                    <CreateTabBarButton onPress={props.onPress}>
                      {props.children}
                    </CreateTabBarButton>
                  )
                }
              : {})
          }}
        />
      )}
      {hasViewPermission(PermissionEntity.REQUESTS) &&
        uiConfiguration.requests && (
          <BottomTab.Screen
            name="Requests"
            component={RequestsScreen}
            options={{
              title: t('requests'),
              tabBarIcon: ({ color, focused }) => (
                <Feather name="inbox" color={color} size={30} />
              )
            }}
          />
        )}
      {viewMoreEntities.some((entity) => hasViewPermission(entity)) && (
        <BottomTab.Screen
          name="MoreEntities"
          component={MoreEntitiesScreen}
          options={{
            title: t('more'),
            tabBarIcon: ({ color, focused }) => (
              <Ionicons
                name={focused ? 'menu' : 'menu-outline'}
                size={30}
                color={color}
              />
            )
          }}
        />
      )}
    </BottomTab.Navigator>
  );
}
