import React, { createContext, useContext, useState, useEffect } from 'react';
import axios from 'axios';

interface PermissionLevel {
  canView: boolean;
  canCreate: boolean;
  canEdit: boolean;
  canDelete: boolean;
}

interface PermissionsContextType {
  permissions: Record<string, PermissionLevel>;
  loading: boolean;
  canView: (feature: string) => boolean;
  canCreate: (feature: string) => boolean;
  canEdit: (feature: string) => boolean;
  canDelete: (feature: string) => boolean;
  hasAnyPermission: (feature: string) => boolean;
  refetch: () => void;
}

const PermissionsContext = createContext<PermissionsContextType | undefined>(undefined);

export const PermissionsProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [permissions, setPermissions] = useState<Record<string, PermissionLevel>>({});
  const [loading, setLoading] = useState(true);

  const API_URL = process.env.REACT_APP_API_URL || 'http://localhost:8080';

  const fetchPermissions = async () => {
    try {
      // TODO: Get userId and companyId from auth context
      const userId = 1; // Placeholder
      const companyId = 1; // Placeholder

      const response = await axios.get(
        `${API_URL}/api/permissions/user/${userId}/company/${companyId}`
      );

      setPermissions(response.data.permissions || {});
    } catch (error) {
      console.error('Error fetching permissions:', error);
      setPermissions({});
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchPermissions();
  }, []);

  const canView = (feature: string): boolean => {
    return permissions[feature]?.canView || false;
  };

  const canCreate = (feature: string): boolean => {
    return permissions[feature]?.canCreate || false;
  };

  const canEdit = (feature: string): boolean => {
    return permissions[feature]?.canEdit || false;
  };

  const canDelete = (feature: string): boolean => {
    return permissions[feature]?.canDelete || false;
  };

  const hasAnyPermission = (feature: string): boolean => {
    const perm = permissions[feature];
    if (!perm) return false;
    return perm.canView || perm.canCreate || perm.canEdit || perm.canDelete;
  };

  return (
    <PermissionsContext.Provider
      value={{
        permissions,
        loading,
        canView,
        canCreate,
        canEdit,
        canDelete,
        hasAnyPermission,
        refetch: fetchPermissions,
      }}
    >
      {children}
    </PermissionsContext.Provider>
  );
};

export const usePermissions = () => {
  const context = useContext(PermissionsContext);
  if (!context) {
    throw new Error('usePermissions must be used within PermissionsProvider');
  }
  return context;
};
