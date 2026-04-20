import api, { authHeader } from './api';
import { UserResponseDTO } from '../models/user';
import UserSettings from '../models/owns/userSettings';
import CompanySettings from '../models/owns/companySettings';

export const getUserInfos = async (): Promise<UserResponseDTO> => {
  return api.get<UserResponseDTO>('auth/me');
};
export const getUserSettings = async (id: number): Promise<UserSettings> => {
  return api.get<UserSettings>(`user-settings/${id}`);
};

export const getCompanySettings = async (
  id: number
): Promise<CompanySettings> => {
  return api.get<CompanySettings>(`company-settings/${id}`);
};
