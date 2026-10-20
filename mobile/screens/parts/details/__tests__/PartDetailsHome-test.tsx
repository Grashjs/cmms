import * as React from 'react';
import { beforeEach, describe, expect, it, jest } from '@jest/globals';
import renderer, { act } from 'react-test-renderer';
import { PaperProvider } from 'react-native-paper';
import PartDetailsHome from '../index';

const mockShowSnackBar = jest.fn();
const mockDispatch = jest.fn();

jest.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (key: string) => key })
}));
jest.mock('../../../../store', () => ({
  useDispatch: () => mockDispatch,
  useSelector: (selector: (state: unknown) => unknown) =>
    selector({ parts: { partInfos: {}, loadingGet: false } })
}));
jest.mock('../../../../slices/part', () => ({
  getPartDetails: (id: number) => ({ type: 'getPartDetails', id }),
  deletePart: jest.fn()
}));
jest.mock('../../../../utils/api', () => ({
  isNetworkError: (error: { network?: boolean }) => error?.network === true
}));
jest.mock('../../../../contexts/CustomSnackBarContext', () => {
  const { createContext } = jest.requireActual<typeof React>('react');
  return {
    CustomSnackBarContext: createContext({
      showSnackBar: (...args: unknown[]) => mockShowSnackBar(...args)
    })
  };
});
jest.mock('../../../../components/LoadingDialog', () => () => null);
jest.mock('../../../../components/Themed', () => ({
  View: jest.requireActual<typeof import('react-native')>('react-native').View
}));
jest.mock('../PartDetails', () => () => null);
jest.mock('../PartWorkOrders', () => () => null);
jest.mock('../PartAssets', () => () => null);
jest.mock('../PartFiles', () => () => null);
jest.mock('react-native-tab-view', () => ({
  TabBar: () => null,
  TabView: () => null
}));
jest.mock('react-native-actions-sheet', () => ({
  SheetManager: { show: jest.fn() }
}));

const renderScreen = async (params: { id: number; partProp?: unknown }) => {
  const navigation = { goBack: jest.fn(), setOptions: jest.fn() };
  await act(async () => {
    renderer.create(
      <PaperProvider>
        <PartDetailsHome
          navigation={navigation as any}
          route={{ key: 'PartDetails', name: 'PartDetails', params } as any}
        />
      </PaperProvider>
    );
  });
  return navigation;
};

describe('PartDetailsHome when the part cannot be loaded', () => {
  beforeEach(() => {
    mockShowSnackBar.mockReset();
    mockDispatch.mockReset();
  });

  it('shows a message and goes back when access is denied', async () => {
    mockDispatch.mockReturnValue(
      Promise.reject(Object.assign(new Error('Access denied'), { status: 403 }))
    );
    const navigation = await renderScreen({ id: 5 });
    expect(mockShowSnackBar).toHaveBeenCalledWith(
      'part_not_found_or_no_access',
      'error'
    );
    expect(navigation.goBack).toHaveBeenCalledTimes(1);
  });

  it('shows the network message when the server is not reachable', async () => {
    mockDispatch.mockReturnValue(Promise.reject({ network: true }));
    const navigation = await renderScreen({ id: 5 });
    expect(mockShowSnackBar).toHaveBeenCalledWith(
      'server_not_reachable',
      'error'
    );
    expect(navigation.goBack).toHaveBeenCalledTimes(1);
  });

  it('does not fetch when the part is passed in the route', async () => {
    const navigation = await renderScreen({
      id: 5,
      partProp: { id: 5, name: 'Filter' }
    });
    expect(mockDispatch).not.toHaveBeenCalled();
    expect(navigation.goBack).not.toHaveBeenCalled();
  });
});
