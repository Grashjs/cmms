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
  Chip,
  Alert,
  CircularProgress,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  Autocomplete,
  Stack,
  IconButton,
} from '@mui/material';
import SaveIcon from '@mui/icons-material/Save';
import PersonAddIcon from '@mui/icons-material/PersonAdd';
import DeleteIcon from '@mui/icons-material/Delete';
import axios from 'axios';

interface User {
  id: number;
  email: string;
  firstName: string;
  lastName: string;
  fullName: string;
}

interface Role {
  id: number;
  name: string;
  description: string;
  isSystemRole: boolean;
}

interface UserRole {
  id: number;
  userId: number;
  userEmail: string;
  userName: string;
  roleId: number;
  roleName: string;
  assignedAt: string;
  notes?: string;
}

interface UserRoleAssignmentProps {
  companyId: number;
}

const UserRoleAssignment: React.FC<UserRoleAssignmentProps> = ({ companyId }) => {
  const [users, setUsers] = useState<User[]>([]);
  const [roles, setRoles] = useState<Role[]>([]);
  const [userRoles, setUserRoles] = useState<UserRole[]>([]);
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  
  const [openDialog, setOpenDialog] = useState(false);
  const [selectedUser, setSelectedUser] = useState<User | null>(null);
  const [selectedRoles, setSelectedRoles] = useState<number[]>([]);

  const API_URL = process.env.REACT_APP_API_URL || 'http://localhost:8080';

  useEffect(() => {
    fetchData();
  }, [companyId]);

  const fetchData = async () => {
    setLoading(true);
    try {
      // Fetch company users
      const usersResponse = await axios.get(`${API_URL}/api/users/company/${companyId}`);
      const usersData = usersResponse.data.map((u: any) => ({
        ...u,
        fullName: `${u.firstName} ${u.lastName}`,
      }));
      setUsers(usersData);

      // Fetch company roles
      const rolesResponse = await axios.get(`${API_URL}/api/roles/company/${companyId}`);
      setRoles(rolesResponse.data);

      // Fetch all user-role assignments
      const userRolesResponse = await axios.get(`${API_URL}/api/user-roles/company/${companyId}`);
      setUserRoles(userRolesResponse.data);
    } catch (err) {
      console.error('Error fetching data:', err);
      setError('Failed to load data');
    } finally {
      setLoading(false);
    }
  };

  const getUserRoles = (userId: number): UserRole[] => {
    return userRoles.filter((ur) => ur.userId === userId);
  };

  const handleOpenAssignDialog = (user: User) => {
    setSelectedUser(user);
    const currentRoles = getUserRoles(user.id).map((ur) => ur.roleId);
    setSelectedRoles(currentRoles);
    setOpenDialog(true);
  };

  const handleCloseDialog = () => {
    setOpenDialog(false);
    setSelectedUser(null);
    setSelectedRoles([]);
  };

  const handleRoleToggle = (roleId: number) => {
    setSelectedRoles((prev) =>
      prev.includes(roleId)
        ? prev.filter((id) => id !== roleId)
        : [...prev, roleId]
    );
  };

  const handleSaveUserRoles = async () => {
    if (!selectedUser) return;

    setSaving(true);
    setError(null);
    setSuccess(null);

    try {
      await axios.put(
        `${API_URL}/api/user-roles/user/${selectedUser.id}`,
        selectedRoles
      );

      setSuccess(`Roles updated for ${selectedUser.fullName}`);
      await fetchData();
      handleCloseDialog();
    } catch (err: any) {
      console.error('Error saving roles:', err);
      setError(err.response?.data?.message || 'Failed to save roles');
    } finally {
      setSaving(false);
    }
  };

  const handleRemoveRole = async (userRoleId: number) => {
    if (!window.confirm('Are you sure you want to remove this role?')) {
      return;
    }

    try {
      await axios.delete(`${API_URL}/api/user-roles/${userRoleId}`);
      setSuccess('Role removed successfully');
      await fetchData();
    } catch (err: any) {
      console.error('Error removing role:', err);
      setError(err.response?.data?.message || 'Failed to remove role');
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
      <Typography variant="h4" gutterBottom>
        User Role Assignment
      </Typography>

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

      <Card>
        <CardContent>
          <Table>
            <TableHead>
              <TableRow>
                <TableCell>User</TableCell>
                <TableCell>Email</TableCell>
                <TableCell>Assigned Roles</TableCell>
                <TableCell align="right">Actions</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {users.map((user) => {
                const roles = getUserRoles(user.id);

                return (
                  <TableRow key={user.id}>
                    <TableCell>{user.fullName}</TableCell>
                    <TableCell>{user.email}</TableCell>
                    <TableCell>
                      <Stack direction="row" spacing={1} flexWrap="wrap">
                        {roles.length > 0 ? (
                          roles.map((ur) => (
                            <Chip
                              key={ur.id}
                              label={ur.roleName}
                              size="small"
                              onDelete={() => handleRemoveRole(ur.id)}
                              deleteIcon={<DeleteIcon />}
                            />
                          ))
                        ) : (
                          <Typography variant="body2" color="text.secondary">
                            No roles assigned
                          </Typography>
                        )}
                      </Stack>
                    </TableCell>
                    <TableCell align="right">
                      <Button
                        size="small"
                        startIcon={<PersonAddIcon />}
                        onClick={() => handleOpenAssignDialog(user)}
                      >
                        Manage Roles
                      </Button>
                    </TableCell>
                  </TableRow>
                );
              })}
            </TableBody>
          </Table>
        </CardContent>
      </Card>

      {/* Role Assignment Dialog */}
      <Dialog open={openDialog} onClose={handleCloseDialog} maxWidth="sm" fullWidth>
        <DialogTitle>
          Assign Roles to {selectedUser?.fullName}
        </DialogTitle>
        <DialogContent>
          <Box sx={{ mt: 2 }}>
            {roles.map((role) => (
              <Box
                key={role.id}
                sx={{
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'space-between',
                  p: 1,
                  borderBottom: '1px solid',
                  borderColor: 'divider',
                }}
              >
                <Box>
                  <Typography variant="body1">{role.name}</Typography>
                  <Typography variant="body2" color="text.secondary">
                    {role.description}
                  </Typography>
                  {role.isSystemRole && (
                    <Chip label="System Role" size="small" color="primary" sx={{ mt: 0.5 }} />
                  )}
                </Box>
                <Checkbox
                  checked={selectedRoles.includes(role.id)}
                  onChange={() => handleRoleToggle(role.id)}
                />
              </Box>
            ))}
          </Box>
        </DialogContent>
        <DialogActions>
          <Button onClick={handleCloseDialog}>Cancel</Button>
          <Button
            onClick={handleSaveUserRoles}
            variant="contained"
            disabled={saving}
            startIcon={saving ? <CircularProgress size={20} /> : <SaveIcon />}
          >
            {saving ? 'Saving...' : 'Save'}
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default UserRoleAssignment;
