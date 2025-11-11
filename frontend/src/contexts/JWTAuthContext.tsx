import { createContext, FC, ReactNode, useEffect, useReducer } from 'react';
import { OwnUser, UserResponseDTO } from 'src/models/user';
import api, { authHeader } from 'src/utils/api';
import { verify } from 'src/utils/jwt';
import PropTypes from 'prop-types';
import {
  getCompanySettings,
  getUserInfos,
  getUserSettings
} from '../utils/userApi';
import UserSettings from 'src/models/owns/userSettings';
import CompanySettings from 'src/models/owns/companySettings';
import { GeneralPreferences } from '../models/owns/generalPreferences';
import internationalization from '../i18n/i18n';
import {
  FieldConfiguration,
  FieldType
} from '../models/owns/fieldConfiguration';
import { Company } from '../models/owns/company';
import { PermissionEntity } from 'src/models/owns/role';
import { Audit } from 'src/models/owns/audit';
import OwnSubscription from '../models/owns/ownSubscription';
import { PlanFeature } from '../models/owns/subscriptionPlan';
import { IField } from '../content/own/type';
import WorkOrder from '../models/owns/workOrder';
import { useDispatch } from 'react-redux';
import { revertAll } from 'src/utils/redux';
import Meter from '../models/owns/meter';
import Asset, { AssetDTO } from '../models/owns/asset';
import Location from '../models/owns/location';
import { useZendesk } from 'react-use-zendesk';
import { UiConfiguration } from 'src/models/owns/uiConfiguration';
import { googleTrackingId, IS_LOCALHOST } from '../config';
import ReactGA from 'react-ga4';
import { getLicenseValidity } from '../slices/license';

interface AuthState {
  isInitialized: boolean;
  isAuthenticated: boolean;
  user: UserResponseDTO | null;
  company: Company | null;
  userSettings: UserSettings | null;
  companySettings: CompanySettings | null;
}

export type FieldConfigurationsType = 'workOrder' | 'request';

interface AuthContextValue extends AuthState {
  method: 'JWT';
  login: (email: string, password: string) => Promise<void>;
  loginInternal: (accessToken: string) => void;
  logout: () => void;
  register: (
    values: any,
    invitationMode: boolean
  ) => Promise<{ message: string; success: boolean }>;
  getInfos: () => void;
  switchAccount: (id: number) => Promise<void>;
  patchUserSettings: (values: Partial<UserSettings>) => Promise<void>;
  patchUser: (values: Partial<OwnUser>) => Promise<void>;
  cancelSubscription: () => Promise<void>;
  resumeSubscription: () => Promise<void>;
  patchSubscription: (values: Partial<OwnSubscription>) => Promise<void>;
  patchCompany: (values: Partial<Company>) => Promise<void>;
  updatePassword: (values: {
    oldPassword: string;
    newPassword: string;
  }) => Promise<boolean>;
  downgrade: (users: number[]) => Promise<boolean>;
  upgrade: (users: number[]) => Promise<boolean>;
  resetPassword: (email: string) => Promise<boolean>;
  fetchUserSettings: () => Promise<void>;
  fetchCompanySettings: () => Promise<void>;
  fetchCompany: () => Promise<void>;
  patchGeneralPreferences: (
    values: Partial<GeneralPreferences>
  ) => Promise<void>;
  patchFieldConfiguration: (
    fieldName: string,
    fieldType: FieldType,
    fieldConfigurationsType: FieldConfigurationsType
  ) => Promise<void>;
  hasViewPermission: (permission: PermissionEntity) => boolean;
  hasViewOtherPermission: (permission: PermissionEntity) => boolean;
  hasFeature: (feature: PlanFeature) => boolean;
  hasCreatePermission: (permission: PermissionEntity) => boolean;
  hasEditPermission: <Entity extends Audit>(
    permission: PermissionEntity,
    entity: Entity
  ) => boolean;
  hasDeletePermission: <Entity extends Audit>(
    permission: PermissionEntity,
    entity: Entity
  ) => boolean;
  getFilteredFields: (fields: Array<IField>) => Array<IField>;
  patchUiConfiguration: (values: Omit<UiConfiguration, 'id'>) => Promise<void>;
}

interface AuthProviderProps {
  children: ReactNode;
}

type InitializeAction = {
  type: 'INITIALIZE';
  payload: {
    isAuthenticated: boolean;
    user: UserResponseDTO | null;
    companySettings: CompanySettings | null;
    company: Company | null;
  };
};

type LoginAction = {
  type: 'LOGIN';
  payload: {
    user: UserResponseDTO;
    companySettings: CompanySettings;
    company: Company;
  };
};

type LogoutAction = {
  type: 'LOGOUT';
};

type RegisterAction = {
  type: 'REGISTER';
  payload: {
    user: UserResponseDTO;
    companySettings: CompanySettings;
    company: Company;
  };
};
type PatchUserSettingsAction = {
  type: 'PATCH_USER_SETTINGS';
  payload: {
    userSettings: UserSettings;
  };
};
type PatchUserAction = {
  type: 'PATCH_USER';
  payload: {
    user: UserResponseDTO;
  };
};
type PatchSubscriptionAction = {
  type: 'PATCH_SUBSCRIPTION';
  payload: {
    subscription: OwnSubscription;
  };
};
type CancelSubscriptionAction = {
  type: 'CANCEL_SUBSCRIPTION';
  payload: {};
};
type ResumeSubscriptionAction = {
  type: 'RESUME_SUBSCRIPTION';
  payload: {};
};
type UpgradeAction = {
  type: 'UPGRADE';
  payload: {};
};
type DowngradeAction = {
  type: 'DOWNGRADE';
  payload: {};
};
type PatchCompanyAction = {
  type: 'PATCH_COMPANY';
  payload: {
    company: Company;
  };
};
type FetchUserSettingsAction = {
  type: 'GET_USER_SETTINGS';
  payload: {
    userSettings: UserSettings;
  };
};
type FetchCompanySettingsAction = {
  type: 'GET_COMPANY_SETTINGS';
  payload: {
    companySettings: CompanySettings;
  };
};
type FetchCompanyAction = {
  type: 'GET_COMPANY';
  payload: {
    company: Company;
  };
};
type PatchGeneralPreferencesAction = {
  type: 'PATCH_GENERAL_PREFERENCES';
  payload: {
    generalPreferences: GeneralPreferences;
  };
};
type PatchFieldConfigurationAction = {
  type: 'PATCH_FIELD_CONFIGURATION';
  payload: {
    type: FieldConfigurationsType;
    fieldConfiguration: FieldConfiguration;
  };
};
type PatchUiConfigurationAction = {
  type: 'PATCH_UI_CONFIGURATION';
  payload: {
    uiConfiguration: UiConfiguration;
  };
};
type Action =
  | InitializeAction
  | LoginAction
  | LogoutAction
  | RegisterAction
  | PatchUserSettingsAction
  | PatchUserAction
  | FetchUserSettingsAction
  | FetchCompanySettingsAction
  | PatchGeneralPreferencesAction
  | PatchFieldConfigurationAction
  | FetchCompanyAction
  | PatchCompanyAction
  | PatchSubscriptionAction
  | CancelSubscriptionAction
  | ResumeSubscriptionAction
  | UpgradeAction
  | DowngradeAction
  | PatchUiConfigurationAction;

const initialAuthState: AuthState = {
  isAuthenticated: false,
  isInitialized: false,
  user: null,
  company: null,
  userSettings: null,
  companySettings: null
};

const setSession = (accessToken: string | null): void => {
  if (accessToken) {
    localStorage.setItem('accessToken', accessToken);
  } else {
    localStorage.removeItem('accessToken');
    localStorage.removeItem('companyId');
  }
};

const setCompanyId = (companyId: number) => {
  localStorage.setItem('companyId', companyId.toString());
};

const handlers: Record<
  string,
  (state: AuthState, action: Action) => AuthState
> = {
  INITIALIZE: (state: AuthState, action: InitializeAction): AuthState => {
    const { isAuthenticated, user, companySettings, company } = action.payload;

    return {
      ...state,
      isAuthenticated,
      isInitialized: true,
      user,
      companySettings,
      company
    };
  },
  LOGIN: (state: AuthState, action: LoginAction): AuthState => {
    const { user, companySettings, company } = action.payload;

    return {
      ...state,
      isAuthenticated: true,
      user,
      companySettings,
      company
    };
  },
  LOGOUT: (state: AuthState): AuthState => ({
    ...state,
    isAuthenticated: false,
    user: null
  }),
  REGISTER: (state: AuthState, action: RegisterAction): AuthState => {
    const { user, companySettings, company } = action.payload;

    return {
      ...state,
      isAuthenticated: true,
      user,
      companySettings,
      company
    };
  },
  PATCH_USER_SETTINGS: (
    state: AuthState,
    action: PatchUserSettingsAction
  ): AuthState => {
    const { userSettings } = action.payload;
    return {
      ...state,
      userSettings
    };
  },
  PATCH_USER: (state: AuthState, action: PatchUserAction): AuthState => {
    const { user } = action.payload;
    return {
      ...state,
      user
    };
  },
  PATCH_SUBSCRIPTION: (
    state: AuthState,
    action: PatchSubscriptionAction
  ): AuthState => {
    const { subscription } = action.payload;
    return {
      ...state,
      company: { ...state.company, subscription }
    };
  },
  CANCEL_SUBSCRIPTION: (
    state: AuthState,
    action: CancelSubscriptionAction
  ): AuthState => {
    return {
      ...state,
      company: {
        ...state.company,
        subscription: { ...state.company.subscription, cancelled: true }
      }
    };
  },
  RESUME_SUBSCRIPTION: (
    state: AuthState,
    action: CancelSubscriptionAction
  ): AuthState => {
    return {
      ...state,
      company: {
        ...state.company,
        subscription: { ...state.company.subscription, cancelled: false }
      }
    };
  },
  PATCH_UI_CONFIGURATION: (
    state: AuthState,
    action: PatchUiConfigurationAction
  ): AuthState => {
    return {
      ...state,
      user: { ...state.user, uiConfiguration: action.payload.uiConfiguration }
    };
  },
  PATCH_COMPANY: (state: AuthState, action: PatchCompanyAction): AuthState => {
    const { company } = action.payload;
    return {
      ...state,
      company
    };
  },
  GET_USER_SETTINGS: (
    state: AuthState,
    action: FetchUserSettingsAction
  ): AuthState => {
    const { userSettings } = action.payload;
    return {
      ...state,
      userSettings
    };
  },
  GET_COMPANY_SETTINGS: (
    state: AuthState,
    action: FetchCompanySettingsAction
  ): AuthState => {
    const { companySettings } = action.payload;
    return {
      ...state,
      companySettings
    };
  },
  GET_COMPANY: (state: AuthState, action: FetchCompanyAction): AuthState => {
    const { company } = action.payload;
    return {
      ...state,
      company
    };
  },
  PATCH_GENERAL_PREFERENCES: (
    state: AuthState,
    action: PatchGeneralPreferencesAction
  ): AuthState => {
    const { generalPreferences } = action.payload;
    return {
      ...state,
      companySettings: {
        ...state.companySettings,
        generalPreferences
      }
    };
  },
  UPGRADE: (state: AuthState, action: FetchCompanyAction): AuthState => {
    return {
      ...state,
      company: {
        ...state.company,
        subscription: { ...state.company.subscription, upgradeNeeded: false }
      }
    };
  },
  DOWNGRADE: (state: AuthState, action: FetchCompanyAction): AuthState => {
    return {
      ...state,
      company: {
        ...state.company,
        subscription: { ...state.company.subscription, downgradeNeeded: false }
      }
    };
  },
  PATCH_FIELD_CONFIGURATION: (
    state: AuthState,
    action: PatchFieldConfigurationAction
  ): AuthState => {
    const { type, fieldConfiguration } = action.payload;
    const stateClone = { ...state };
    if (type === 'workOrder') {
      stateClone.companySettings.workOrderConfiguration.workOrderFieldConfigurations =
        stateClone.companySettings.workOrderConfiguration.workOrderFieldConfigurations.map(
          (fC) => {
            if (fieldConfiguration.id === fC.id) {
              return fieldConfiguration;
            }
            return fC;
          }
        );
    } else {
      stateClone.companySettings.workOrderRequestConfiguration.fieldConfigurations =
        stateClone.companySettings.workOrderRequestConfiguration.fieldConfigurations.map(
          (fC) => {
            if (fieldConfiguration.id === fC.id) {
              return fieldConfiguration;
            }
            return fC;
          }
        );
    }
    return stateClone;
  }
};

const reducer = (state: AuthState, action: Action): AuthState =>
  handlers[action.type] ? handlers[action.type](state, action) : state;

const AuthContext = createContext<AuthContextValue>({
  ...initialAuthState,
  method: 'JWT',
  login: () => Promise.resolve(),
  loginInternal: () => null,
  logout: () => Promise.resolve(),
  register: () => Promise.resolve({ success: false, message: null }),
  getInfos: () => Promise.resolve(),
  patchUserSettings: () => Promise.resolve(),
  patchCompany: () => Promise.resolve(),
  patchUser: () => Promise.resolve(),
  patchSubscription: () => Promise.resolve(),
  cancelSubscription: () => Promise.resolve(),
  resumeSubscription: () => Promise.resolve(),
  fetchUserSettings: () => Promise.resolve(),
  fetchCompany: () => Promise.resolve(),
  updatePassword: () => Promise.resolve(false),
  resetPassword: () => Promise.resolve(false),
  fetchCompanySettings: () => Promise.resolve(),
  patchGeneralPreferences: () => Promise.resolve(),
  patchUiConfiguration: () => Promise.resolve(),
  patchFieldConfiguration: () => Promise.resolve(),
  hasViewPermission: () => false,
  hasViewOtherPermission: () => false,
  getFilteredFields: () => [],
  hasFeature: () => false,
  hasCreatePermission: () => false,
  hasEditPermission: () => false,
  hasDeletePermission: () => false,
  downgrade: () => Promise.resolve(false),
  upgrade: () => Promise.resolve(false),
  switchAccount: () => Promise.resolve()
});

export const AuthProvider: FC<AuthProviderProps> = (props) => {
  const globalDispatch = useDispatch();
  const { children } = props;
  const [state, dispatch] = useReducer(reducer, initialAuthState);
  const { loginUser: loginZendesk, logoutUser: logoutZendesk } = useZendesk();
  const switchLanguage = ({ lng }: { lng: any }) => {
    internationalization.changeLanguage(lng);
  };
  const updateUserInfos = async () => {
    const user = await getUserInfos();
    setCompanyId(user.companyId);
    return user;
  };
  const setupUser = async (companySettings: CompanySettings) => {
    switchLanguage({
      lng: companySettings.generalPreferences.language.toLowerCase()
    });
  };
  const getInfos = async (): Promise<void> => {
    try {
      const accessToken = window.localStorage.getItem('accessToken');

      if (accessToken && (await verify(accessToken))) {
        setSession(accessToken);
        const user = await updateUserInfos();
        const company = await api.get<Company>(`companies/${user.companyId}`);
        await setupUser(company.companySettings);
        dispatch({
          type: 'INITIALIZE',
          payload: {
            isAuthenticated: true,
            user,
            companySettings: company.companySettings,
            company
          }
        });
      } else {
        dispatch({
          type: 'INITIALIZE',
          payload: {
            isAuthenticated: false,
            user: null,
            companySettings: null,
            company: null
          }
        });
      }
    } catch (err) {
      console.error(err);
      dispatch({
        type: 'INITIALIZE',
        payload: {
          isAuthenticated: false,
          user: null,
          companySettings: null,
          company: null
        }
      });
    }
  };
  const login = async (email: string, password: string): Promise<void> => {
    const response = await api.post<{ accessToken: string }>(
      'auth/signin',
      {
        email,
        type: 'client',
        password
      },
      { headers: authHeader(true) }
    );
    const { accessToken } = response;
    return loginInternal(accessToken);
  };
  const loginInternal = async (accessToken: string) => {
    globalDispatch(revertAll());
    setSession(accessToken);
    try {
      loginZendesk(accessToken);
    } catch (err) {
      console.error(err);
    }
    const user = await updateUserInfos();
    const company = await api.get<Company>(`companies/${user.companyId}`);
    await setupUser(company.companySettings);
    //@ts-ignore
    dispatch({
      type: 'LOGIN',
      payload: {
        user,
        companySettings: company.companySettings,
        company
      }
    });
  };
  const switchAccount = async (id: number): Promise<void> => {
    const response = await api.get<{ accessToken: string }>(
      `auth/switch-account?id=${id}`
    );
    const { accessToken } = response;
    return loginInternal(accessToken);
  };
  const logout = async (): Promise<void> => {
    setSession(null);
    try {
      logoutZendesk();
    } catch (err) {
      console.error(err);
    }
    //TODO this is not working
    // caches.keys().then((names) => {
    //   names.forEach((name) => {
    //     caches.delete(name);
    //   });
    // });
    dispatch({ type: 'LOGOUT' });
  };

  const register = async (
    values,
    invitationMode: boolean
  ): Promise<{ success: boolean; message: string }> => {
    if (!IS_LOCALHOST && googleTrackingId)
      ReactGA.event({
        category: 'sign_up',
        action: 'sign_up',
        label: 'sign_up'
      });
    const response = await api.post<{ message: string; success: boolean }>(
      'auth/signup',
      values,
      { headers: authHeader(true) }
    );
    const { message, success } = response;
    if (message.startsWith('Successful') || invitationMode) {
      return;
    } else {
      await loginInternal(message);
      return response;
    }
  };

  const patchUserSettings = async (
    values: Partial<UserSettings>
  ): Promise<void> => {
    const userSettings = await api.patch<UserSettings>(
      `user-settings/${state.userSettings.id}`,
      values
    );
    dispatch({
      type: 'PATCH_USER_SETTINGS',
      payload: {
        userSettings
      }
    });
  };
  const patchCompany = async (values: Partial<Company>): Promise<void> => {
    const company = await api.patch<Company>(`companies/${state.company.id}`, {
      ...state.company,
      ...values
    });
    dispatch({
      type: 'PATCH_COMPANY',
      payload: {
        company
      }
    });
  };
  const patchUser = async (values: Partial<OwnUser>): Promise<void> => {
    const user = await api.patch<UserResponseDTO>(`users/${state.user.id}`, {
      ...state.user,
      ...values
    });
    dispatch({
      type: 'PATCH_USER',
      payload: {
        user
      }
    });
  };
  const patchSubscription = async (values: OwnSubscription): Promise<void> => {
    dispatch({
      type: 'PATCH_SUBSCRIPTION',
      payload: {
        subscription: values
      }
    });
  };
  const cancelSubscription = async (): Promise<void> => {
    const response = await api.get<{ success: boolean }>(`fast-spring/cancel`);
    const { success } = response;
    if (success) {
      dispatch({
        type: 'CANCEL_SUBSCRIPTION',
        payload: {}
      });
    }
  };
  const resumeSubscription = async (): Promise<void> => {
    const response = await api.get<{ success: boolean }>(`fast-spring/resume`);
    const { success } = response;
    if (success) {
      dispatch({
        type: 'RESUME_SUBSCRIPTION',
        payload: {}
      });
    }
  };
  const updatePassword = async (values: {
    oldPassword: string;
    newPassword: string;
  }): Promise<boolean> => {
    const response = await api.post<{ success: boolean }>(
      `auth/updatepwd`,
      values
    );
    const { success } = response;
    return success;
  };
  const resetPassword = async (email: string): Promise<boolean> => {
    const response = await api.get<{ success: boolean }>(
      `auth/resetpwd?email=${email}`,
      { headers: authHeader(true) }
    );
    const { success } = response;
    return success;
  };
  const fetchUserSettings = async (): Promise<void> => {
    const userSettings = await getUserSettings(state.user.userSettingsId);
    dispatch({
      type: 'GET_USER_SETTINGS',
      payload: {
        userSettings
      }
    });
  };

  const fetchCompanySettings = async (): Promise<void> => {
    const companySettings = await getCompanySettings(
      state.user.companySettingsId
    );
    dispatch({
      type: 'GET_COMPANY_SETTINGS',
      payload: {
        companySettings
      }
    });
  };
  const fetchCompany = async (): Promise<void> => {
    const company = await api.get<Company>(state.user.companyId);
    dispatch({
      type: 'GET_COMPANY',
      payload: {
        company
      }
    });
  };
  const patchGeneralPreferences = async (
    values: Partial<GeneralPreferences>
  ): Promise<void> => {
    const generalPreferences = await api.patch<GeneralPreferences>(
      `general-preferences/${state.companySettings.generalPreferences.id}`,
      { ...state.companySettings.generalPreferences, ...values }
    );
    dispatch({
      type: 'PATCH_GENERAL_PREFERENCES',
      payload: {
        generalPreferences
      }
    });
  };

  const patchUiConfiguration = async (
    values: Omit<UiConfiguration, 'id'>
  ): Promise<void> => {
    const uiConfiguration = await api.patch<UiConfiguration>(
      `ui-configurations`,
      values
    );
    dispatch({
      type: 'PATCH_UI_CONFIGURATION',
      payload: {
        uiConfiguration
      }
    });
  };

  const patchFieldConfiguration = async (
    fieldName: string,
    fieldType: FieldType,
    fieldConfigurationsType: FieldConfigurationsType
  ): Promise<void> => {
    let id;
    if (fieldConfigurationsType === 'workOrder') {
      id =
        state.companySettings.workOrderConfiguration.workOrderFieldConfigurations.find(
          (workOrderFieldConfiguration) =>
            workOrderFieldConfiguration.fieldName === fieldName
        ).id;
    } else {
      id =
        state.companySettings.workOrderRequestConfiguration.fieldConfigurations.find(
          (fieldConfiguration) => fieldConfiguration.fieldName === fieldName
        ).id;
    }
    const fieldConfiguration = await api.patch<FieldConfiguration>(
      `field-configurations/${id}`,
      { fieldType }
    );
    dispatch({
      type: 'PATCH_FIELD_CONFIGURATION',
      payload: {
        type: fieldConfigurationsType,
        fieldConfiguration
      }
    });
  };
  const hasViewPermission = (permissionEntity: PermissionEntity) => {
    return state.user.role.viewPermissions.includes(permissionEntity);
  };
  const hasViewOtherPermission = (permissionEntity: PermissionEntity) => {
    return state.user.role.viewOtherPermissions.includes(permissionEntity);
  };
  const hasCreatePermission = (permissionEntity: PermissionEntity) => {
    return state.user.role.createPermissions.includes(permissionEntity);
  };
  const hasEditPermission = <Entity extends Audit>(
    permissionEntity: PermissionEntity,
    entity: Entity
  ) => {
    if (!entity) return false;
    if (permissionEntity === PermissionEntity.PEOPLE_AND_TEAMS) {
      return (
        state.user.id === entity.id ||
        state.user.role.editOtherPermissions.includes(permissionEntity)
      );
    } else if (permissionEntity === PermissionEntity.WORK_ORDERS) {
      const isAssignedTo = (workOrder: WorkOrder, user: OwnUser): boolean => {
        let users = [];
        if (workOrder.primaryUser) {
          users.push(workOrder.primaryUser);
        }
        if (workOrder.team) {
          users = users.concat(workOrder.team.users);
        }
        if (workOrder.assignedTo) {
          users = users.concat(workOrder.assignedTo);
        }
        return users.some((user1) => user1.id === user.id);
      };
      return (
        state.user.id === entity.createdBy ||
        state.user.role.editOtherPermissions.includes(permissionEntity) ||
        isAssignedTo(entity as unknown as WorkOrder, state.user)
      );
    } else if (permissionEntity === PermissionEntity.METERS) {
      const isAssignedTo = (meter: Meter, user: OwnUser): boolean => {
        return meter.users.some((user1) => user1.id === user.id);
      };
      return (
        state.user.id === entity.createdBy ||
        state.user.role.editOtherPermissions.includes(permissionEntity) ||
        isAssignedTo(entity as unknown as Meter, state.user)
      );
    } else if (permissionEntity === PermissionEntity.ASSETS) {
      const isAssignedTo = (asset: AssetDTO, user: OwnUser): boolean => {
        let users = [];
        if (asset.primaryUser) {
          users.push(asset.primaryUser);
        }
        if (asset.assignedTo) {
          users = users.concat(asset.assignedTo);
        }
        return users.some((user1) => user1.id === user.id);
      };
      return (
        state.user.id === entity.createdBy ||
        state.user.role.editOtherPermissions.includes(permissionEntity) ||
        isAssignedTo(entity as unknown as AssetDTO, state.user)
      );
    } else if (permissionEntity === PermissionEntity.LOCATIONS) {
      const isAssignedTo = (location: Location, user: OwnUser): boolean => {
        let users = [];
        if (location.workers) {
          users = users.concat(location.workers);
        }
        return users.some((user1) => user1.id === user.id);
      };
      return (
        state.user.id === entity.createdBy ||
        state.user.role.editOtherPermissions.includes(permissionEntity) ||
        isAssignedTo(entity as unknown as Location, state.user)
      );
    }
    return (
      state.user.id === entity.createdBy ||
      state.user.role.editOtherPermissions.includes(permissionEntity)
    );
  };
  const hasDeletePermission = <Entity extends Audit>(
    permissionEntity: PermissionEntity,
    entity: Entity
  ) => {
    if (!entity) return false;
    return (
      state.user.id === entity.createdBy ||
      state.user.role.deleteOtherPermissions.includes(permissionEntity)
    );
  };
  const hasFeature = (feature: PlanFeature) => {
    return state.company.subscription.subscriptionPlan.features.includes(
      feature
    );
  };
  const getFilteredFields = (defaultFields: Array<IField>): IField[] => {
    let fields = [...defaultFields];
    if (!hasFeature(PlanFeature.FILE)) {
      fields = fields.filter((field) => field.type !== 'file');
    }
    const uiConfigurationFieldConfig: {
      key: keyof UiConfiguration;
      type2: IField['type2'][];
    }[] = [
      { key: 'locations', type2: ['location'] },
      { key: 'vendorsAndCustomers', type2: ['vendor', 'customer'] }
    ];
    const uiConfiguration = state.user.uiConfiguration;
    fields = fields.filter((field) => {
      for (const { key, type2 } of uiConfigurationFieldConfig) {
        if (!uiConfiguration[key] && type2.includes(field.type2)) {
          return false;
        }
      }
      return true;
    });
    return fields;
  };
  const upgrade = async (users: number[]) => {
    try {
      const { success } = await api.post<{ success: boolean }>(
        'subscriptions/upgrade',
        users,
        {},
        true
      );
      if (success)
        dispatch({
          type: 'UPGRADE',
          payload: {}
        });
      return success;
    } catch (err) {
      return false;
    }
  };
  const downgrade = async (users: number[]) => {
    try {
      const { success } = await api.get<{ success: boolean }>(
        `subscriptions/downgrade?usersIds=${users.join(',')}`
      );
      if (success)
        dispatch({
          type: 'DOWNGRADE',
          payload: {}
        });
      return success;
    } catch (err) {
      return false;
    }
  };
  useEffect(() => {
    getInfos();
  }, []);

  return (
    <AuthContext.Provider
      value={{
        ...state,
        method: 'JWT',
        login,
        logout,
        register,
        getInfos,
        patchUser,
        patchSubscription,
        cancelSubscription,
        resumeSubscription,
        patchCompany,
        updatePassword,
        resetPassword,
        patchUserSettings,
        fetchUserSettings,
        fetchCompanySettings,
        fetchCompany,
        patchGeneralPreferences,
        patchFieldConfiguration,
        hasViewPermission,
        hasViewOtherPermission,
        hasFeature,
        getFilteredFields,
        hasEditPermission,
        hasDeletePermission,
        hasCreatePermission,
        upgrade,
        downgrade,
        switchAccount,
        loginInternal,
        patchUiConfiguration
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};

AuthProvider.propTypes = {
  children: PropTypes.node.isRequired
};

export default AuthContext;
