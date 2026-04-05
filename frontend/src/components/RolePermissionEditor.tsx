import React, { useState, useEffect } from 'react';
import {
  Box,
  Card,
  CardContent,
  Typography,
  Button,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
  Checkbox,
  FormControlLabel,
  Chip,
  Alert,
  CircularProgress,
  Accordion,
  AccordionSummary,
  AccordionDetails,
  Stack,
} from '@mui/material';
import ExpandMoreIcon from '@mui/icons-material/ExpandMore';
import SaveIcon from '@mui/icons-material/Save';
import axios from 'axios';

interface Feature {
  code: string;
  name: string;
  description: string;
  category: string;
}

interface Permission {
  featureCode: string;
  featureName: string;
  canView: boolean;
  canCreate: boolean;
  canEdit: boolean;
  canDelete: boolean;
}

interface RolePermissionEditorProps {
  roleId: number;
  roleName: string;
  isSystemRole: boolean;
}

const RolePermissionEditor: React.FC<RolePermissionEditorProps> = ({
  roleId,
  roleName,
  isSystemRole,
}) => {
  const [features, setFeatures] = useState<Feature[]>([]);
  const [permissions, setPermissions] = useState<Record<string, Permission>>({});
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);

  const API_URL = process.env.REACT_APP_API_URL || 'http://localhost:8080';

  useEffect(() => {
    fetchData();
  }, [roleId]);

  const fetchData = async () => {
    setLoading(true);
    try {
      // Fetch all features
      const featuresResponse = await axios.get(`${API_URL}/api/features`);
      setFeatures(featuresResponse.data);

      // Fetch current role permissions
      const roleResponse = await axios.get(`${API_URL}/api/roles/${roleId}`);
      const currentPermissions = roleResponse.data.permissions || [];

      // Convert to map
      const permMap: Record<string, Permission> = {};
      currentPermissions.forEach((perm: Permission) => {
        permMap[perm.featureCode] = perm;
      });

      // Initialize missing permissions
      featuresResponse.data.forEach((feature: Feature) => {
        if (!permMap[feature.code]) {
          permMap[feature.code] = {
            featureCode: feature.code,
            featureName: feature.name,
            canView: false,
            canCreate: false,
            canEdit: false,
            canDelete: false,
          };
        }
      });

      setPermissions(permMap);
    } catch (err) {
      console.error('Error fetching data:', err);
      setError('Failed to load permissions');
    } finally {
      setLoading(false);
    }
  };

  const handlePermissionChange = (
    featureCode: string,
    field: 'canView' | 'canCreate' | 'canEdit' | 'canDelete',
    value: boolean
  ) => {
    setPermissions((prev) => {
      const updated = { ...prev };
      
      if (!updated[featureCode]) {
        const feature = features.find((f) => f.code === featureCode);
        updated[featureCode] = {
          featureCode,
          featureName: feature?.name || '',
          canView: false,
          canCreate: false,
          canEdit: false,
          canDelete: false,
        };
      }

      updated[featureCode] = {
        ...updated[featureCode],
        [field]: value,
      };

      // Auto-enable view if any other permission is enabled
      if (value && field !== 'canView') {
        updated[featureCode].canView = true;
      }

      // Auto-disable other permissions if view is disabled
      if (!value && field === 'canView') {
        updated[featureCode].canCreate = false;
        updated[featureCode].canEdit = false;
        updated[featureCode].canDelete = false;
      }

      return updated;
    });
  };

  const handleSelectAll = (category: string, field: 'canView' | 'canCreate' | 'canEdit' | 'canDelete') => {
    const categoryFeatures = features.filter((f) => f.category === category);
    
    setPermissions((prev) => {
      const updated = { ...prev };
      
      categoryFeatures.forEach((feature) => {
        if (!updated[feature.code]) {
          updated[feature.code] = {
            featureCode: feature.code,
            featureName: feature.name,
            canView: false,
            canCreate: false,
            canEdit: false,
            canDelete: false,
          };
        }

        updated[feature.code][field] = true;
        
        // Auto-enable view
        if (field !== 'canView') {
          updated[feature.code].canView = true;
        }
      });

      return updated;
    });
  };

  const handleSave = async () => {
    setSaving(true);
    setError(null);
    setSuccess(null);

    try {
      const permissionsArray = Object.values(permissions);

      await axios.put(
        `${API_URL}/api/roles/${roleId}/permissions`,
        permissionsArray
      );

      setSuccess('Permissions saved successfully!');
    } catch (err: any) {
      console.error('Error saving permissions:', err);
      setError(err.response?.data?.message || 'Failed to save permissions');
    } finally {
      setSaving(false);
    }
  };

  const getCategoryColor = (category: string) => {
    switch (category) {
      case 'Core':
        return 'primary';
      case 'Advanced':
        return 'secondary';
      case 'Premium':
        return 'warning';
      default:
        return 'default';
    }
  };

  const groupedFeatures = features.reduce((acc, feature) => {
    const category = feature.category || 'Other';
    if (!acc[category]) {
      acc[category] = [];
    }
    acc[category].push(feature);
    return acc;
  }, {} as Record<string, Feature[]>);

  if (loading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', p: 4 }}>
        <CircularProgress />
      </Box>
    );
  }

  return (
    <Box sx={{ p: 3 }}>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 3 }}>
        <Box>
          <Typography variant="h4">Edit Permissions: {roleName}</Typography>
          {isSystemRole && (
            <Alert severity="warning" sx={{ mt: 2 }}>
              This is a system role. Changes will affect all companies using this role.
            </Alert>
          )}
        </Box>
        <Button
          variant="contained"
          startIcon={saving ? <CircularProgress size={20} /> : <SaveIcon />}
          onClick={handleSave}
          disabled={saving || isSystemRole}
        >
          {saving ? 'Saving...' : 'Save Permissions'}
        </Button>
      </Box>

      {error && (
        <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError(null)}>
          {error}
        </Alert>
      )}

      {success && (
        <Alert severity="success" sx={{ mb: 2 }} onClose={() => setSuccess(null)}>
          {success}
        </Alert>
      )}

      {Object.entries(groupedFeatures).map(([category, categoryFeatures]) => (
        <Accordion key={category} defaultExpanded={category === 'Core'}>
          <AccordionSummary expandIcon={<ExpandMoreIcon />}>
            <Stack direction="row" spacing={2} alignItems="center">
              <Typography variant="h6">{category} Features</Typography>
              <Chip
                label={`${categoryFeatures.length} features`}
                size="small"
                color={getCategoryColor(category)}
              />
            </Stack>
          </AccordionSummary>
          <AccordionDetails>
            <Box sx={{ mb: 2 }}>
              <Stack direction="row" spacing={1}>
                <Button
                  size="small"
                  onClick={() => handleSelectAll(category, 'canView')}
                >
                  Select All View
                </Button>
                <Button
                  size="small"
                  onClick={() => handleSelectAll(category, 'canCreate')}
                >
                  Select All Create
                </Button>
                <Button
                  size="small"
                  onClick={() => handleSelectAll(category, 'canEdit')}
                >
                  Select All Edit
                </Button>
                <Button
                  size="small"
                  onClick={() => handleSelectAll(category, 'canDelete')}
                >
                  Select All Delete
                </Button>
              </Stack>
            </Box>

            <Table size="small">
              <TableHead>
                <TableRow>
                  <TableCell>Feature</TableCell>
                  <TableCell>Description</TableCell>
                  <TableCell align="center">View</TableCell>
                  <TableCell align="center">Create</TableCell>
                  <TableCell align="center">Edit</TableCell>
                  <TableCell align="center">Delete</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {categoryFeatures.map((feature) => {
                  const perm = permissions[feature.code] || {
                    canView: false,
                    canCreate: false,
                    canEdit: false,
                    canDelete: false,
                  };

                  return (
                    <TableRow key={feature.code}>
                      <TableCell>
                        <Typography variant="body2" fontWeight="medium">
                          {feature.name}
                        </Typography>
                      </TableCell>
                      <TableCell>
                        <Typography variant="body2" color="text.secondary">
                          {feature.description}
                        </Typography>
                      </TableCell>
                      <TableCell align="center">
                        <Checkbox
                          checked={perm.canView}
                          onChange={(e) =>
                            handlePermissionChange(
                              feature.code,
                              'canView',
                              e.target.checked
                            )
                          }
                          disabled={isSystemRole}
                        />
                      </TableCell>
                      <TableCell align="center">
                        <Checkbox
                          checked={perm.canCreate}
                          onChange={(e) =>
                            handlePermissionChange(
                              feature.code,
                              'canCreate',
                              e.target.checked
                            )
                          }
                          disabled={isSystemRole}
                        />
                      </TableCell>
                      <TableCell align="center">
                        <Checkbox
                          checked={perm.canEdit}
                          onChange={(e) =>
                            handlePermissionChange(
                              feature.code,
                              'canEdit',
                              e.target.checked
                            )
                          }
                          disabled={isSystemRole}
                        />
                      </TableCell>
                      <TableCell align="center">
                        <Checkbox
                          checked={perm.canDelete}
                          onChange={(e) =>
                            handlePermissionChange(
                              feature.code,
                              'canDelete',
                              e.target.checked
                            )
                          }
                          disabled={isSystemRole}
                        />
                      </TableCell>
                    </TableRow>
                  );
                })}
              </TableBody>
            </Table>
          </AccordionDetails>
        </Accordion>
      ))}
    </Box>
  );
};

export default RolePermissionEditor;
