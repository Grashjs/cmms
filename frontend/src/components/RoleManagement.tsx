import React, { useState, useEffect } from 'react';
import {
  Box, Card, CardContent, Typography, Button, Table,
  TableBody, TableCell, TableHead, TableRow, Chip, CircularProgress
} from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import axios from 'axios';

interface Role {
  id: number;
  name: string;
  description: string;
  isSystemRole: boolean;
  isActive: boolean;
}

interface RoleManagementProps {
  companyId: number;
}

const RoleManagement: React.FC<RoleManagementProps> = ({ companyId }) => {
  const [roles, setRoles] = useState<Role[]>([]);
  const [loading, setLoading] = useState(false);

  const API_URL = process.env.REACT_APP_API_URL || 'http://localhost:8080';

  useEffect(() => {
    fetchRoles();
  }, [companyId]);

  const fetchRoles = async () => {
    setLoading(true);
    try {
      const response = await axios.get(`${API_URL}/api/roles/company/${companyId}`);
      setRoles(response.data);
    } catch (error) {
      console.error('Error fetching roles:', error);
    } finally {
      setLoading(false);
    }
  };

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
        <Typography variant="h4">Role Management</Typography>
        <Button variant="contained" startIcon={<AddIcon />}>
          Create Role
        </Button>
      </Box>

      <Card>
        <CardContent>
          <Table>
            <TableHead>
              <TableRow>
                <TableCell>Name</TableCell>
                <TableCell>Description</TableCell>
                <TableCell>Type</TableCell>
                <TableCell>Status</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {roles.map((role) => (
                <TableRow key={role.id}>
                  <TableCell>{role.name}</TableCell>
                  <TableCell>{role.description}</TableCell>
                  <TableCell>
                    {role.isSystemRole ? (
                      <Chip label="System" size="small" color="primary" />
                    ) : (
                      <Chip label="Custom" size="small" />
                    )}
                  </TableCell>
                  <TableCell>
                    {role.isActive ? (
                      <Chip label="Active" size="small" color="success" />
                    ) : (
                      <Chip label="Inactive" size="small" color="default" />
                    )}
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </CardContent>
      </Card>
    </Box>
  );
};

export default RoleManagement;
