import React from 'react';
import { usePermissions } from '../contexts/PermissionsContext';

interface PermissionGateProps {
  feature: string;
  type?: 'view' | 'create' | 'edit' | 'delete' | 'any';
  fallback?: React.ReactNode;
  children: React.ReactNode;
}

export const PermissionGate: React.FC<PermissionGateProps> = ({
  feature,
  type = 'view',
  fallback = null,
  children,
}) => {
  const { canView, canCreate, canEdit, canDelete, hasAnyPermission, loading } = usePermissions();

  if (loading) {
    return null;
  }

  let hasPermission = false;

  switch (type) {
    case 'view':
      hasPermission = canView(feature);
      break;
    case 'create':
      hasPermission = canCreate(feature);
      break;
    case 'edit':
      hasPermission = canEdit(feature);
      break;
    case 'delete':
      hasPermission = canDelete(feature);
      break;
    case 'any':
      hasPermission = hasAnyPermission(feature);
      break;
  }

  return hasPermission ? <>{children}</> : <>{fallback}</>;
};

export default PermissionGate;
