import { createNavigationContainerRef } from '@react-navigation/native';
import type { RootParamList } from '../types';

export const navigationRef = createNavigationContainerRef<RootParamList>();

export function navigate<Name extends keyof RootParamList>(
  name: Name,
  params?: RootParamList[Name]
) {
  if (navigationRef.isReady()) {
    // @ts-ignore
    navigationRef.navigate(name, params);
  }
}
