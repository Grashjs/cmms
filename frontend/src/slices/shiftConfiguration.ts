import type { AppThunk } from 'src/store';
import { ShiftConfigurationShowDTO } from '../models/user';
import api from '../utils/api';

const basePath = 'shift-configurations';

export const patchShiftConfiguration =
  (userId: number, data: Partial<ShiftConfigurationShowDTO>): AppThunk =>
  async () => {
    return api.patch<ShiftConfigurationShowDTO>(
      `${basePath}/user/${userId}`,
      data
    );
  };
