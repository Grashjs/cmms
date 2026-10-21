import { FC, useState, createContext } from 'react';
type SidebarContext = {
  sidebarToggle: any;
  toggleSidebar: () => void;
  closeSidebar: () => void;
  // Desktop-only: sidebar shrunk to an icon rail.
  sidebarCollapsed: boolean;
  toggleSidebarCollapsed: () => void;
  expandSidebar: () => void;
};

const COLLAPSED_STORAGE_KEY = 'sidebarCollapsed';

const readCollapsed = (): boolean => {
  try {
    return localStorage.getItem(COLLAPSED_STORAGE_KEY) === 'true';
  } catch {
    return false;
  }
};

const writeCollapsed = (value: boolean) => {
  try {
    localStorage.setItem(COLLAPSED_STORAGE_KEY, String(value));
  } catch {
    // storage unavailable; state still works for this session
  }
};

// eslint-disable-next-line @typescript-eslint/no-redeclare
export const SidebarContext = createContext<SidebarContext>(
  {} as SidebarContext
);

export const SidebarProvider: FC = ({ children }) => {
  const [sidebarToggle, setSidebarToggle] = useState(false);
  const [sidebarCollapsed, setSidebarCollapsed] = useState(readCollapsed);
  const toggleSidebar = () => {
    setSidebarToggle(!sidebarToggle);
  };
  const closeSidebar = () => {
    setSidebarToggle(false);
  };
  const setCollapsed = (value: boolean) => {
    setSidebarCollapsed(value);
    writeCollapsed(value);
  };
  const toggleSidebarCollapsed = () => setCollapsed(!sidebarCollapsed);
  const expandSidebar = () => setCollapsed(false);

  return (
    <SidebarContext.Provider
      value={{
        sidebarToggle,
        toggleSidebar,
        closeSidebar,
        sidebarCollapsed,
        toggleSidebarCollapsed,
        expandSidebar
      }}
    >
      {children}
    </SidebarContext.Provider>
  );
};
