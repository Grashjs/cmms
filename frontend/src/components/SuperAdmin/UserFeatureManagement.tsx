import React, { useState, useEffect } from 'react';
import {
  Box,
  Card,
  CardContent,
  Typography,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
  Switch,
  Button,
  Alert,
  CircularProgress,
  Chip,
  FormControlLabel,
} from '@mui/material';
import axios from 'axios';

interface Feature {
  id: number;
  code: string;
  name: string;
  description: string;
  category: string;
  isActive: boolean;
}

interface User {
  id: number;
  email: string;
  firstName: string;
  lastName: string;
}

interface UserFeatureManagementProps {
  userId: number;
  userName?: string;
}

const UserFeatureManagement: React.FC<UserFeatureManagementProps> = ({ userId, userName }) => {
  const [features, setFeatures] = useState<Feature[]>([]);
  const [userFeatures, setUserFeatures] = useState<Record<string, boolean>>({});
  const [hasCustomPermissions, setHasCustomPermissions] = useState(false);
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);

  const API_URL = process.env.REACT_APP_API_URL || 'http://192.168.2.185:8080';

  useEffect(() => {
    fetchData();
  }, [userId]);

  const fetchData = async () => {
    setLoading(true);
    try {
      // Tüm özellikleri getir
      const featuresResponse = await axios.get(`${API_URL}/api/user-features/features`);
      setFeatures(featuresResponse.data);

      // Kullanıcının özelliklerini getir
      const userResponse = await axios.get(`${API_URL}/api/user-features/user/${userId}`);
      setUserFeatures(userResponse.data.features);
      setHasCustomPermissions(userResponse.data.hasCustomPermissions);
    } catch (err) {
      console.error('Error fetching data:', err);
      setError('Veriler yüklenirken hata oluştu');
    } finally {
      setLoading(false);
    }
  };

  const handleFeatureToggle = async (featureCode: string, enabled: boolean) => {
    setSaving(true);
    setError(null);
    setSuccess(null);

    try {
      await axios.post(
        `${API_URL}/api/user-features/user/${userId}/feature/${featureCode}`,
        null,
        { params: { enabled } }
      );

      setUserFeatures((prev) => ({
        ...prev,
        [featureCode]: enabled,
      }));

      setHasCustomPermissions(true);
      setSuccess('Özellik güncellendi');
    } catch (err: any) {
      console.error('Error updating feature:', err);
      setError(err.response?.data?.message || 'Güncelleme başarısız');
    } finally {
      setSaving(false);
    }
  };

  const handleResetPermissions = async () => {
    if (!window.confirm('Tüm özel izinler silinecek ve kullanıcı varsayılan izinlere döndürülecek. Emin misiniz?')) {
      return;
    }

    setSaving(true);
    setError(null);
    setSuccess(null);

    try {
      await axios.delete(`${API_URL}/api/user-features/user/${userId}/reset`);
      
      // Tüm özellikleri açık yap (varsayılan durum)
      const allEnabled: Record<string, boolean> = {};
      features.forEach((f) => {
        allEnabled[f.code] = true;
      });
      setUserFeatures(allEnabled);
      setHasCustomPermissions(false);
      
      setSuccess('Kullanıcı varsayılan izinlere döndürüldü');
    } catch (err: any) {
      console.error('Error resetting permissions:', err);
      setError(err.response?.data?.message || 'Sıfırlama başarısız');
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
      <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 3, alignItems: 'center' }}>
        <Box>
          <Typography variant="h5">Kullanıcı Özellik Yönetimi</Typography>
          {userName && (
            <Typography variant="body2" color="text.secondary">
              {userName}
            </Typography>
          )}
        </Box>
        
        {hasCustomPermissions && (
          <Button
            variant="outlined"
            color="warning"
            onClick={handleResetPermissions}
            disabled={saving}
          >
            Varsayılana Döndür
          </Button>
        )}
      </Box>

      {!hasCustomPermissions && (
        <Alert severity="info" sx={{ mb: 2 }}>
          Bu kullanıcı varsayılan izinlere sahip (tüm özellikler açık). 
          Bir özelliği kapatmak için switch'i kullanın.
        </Alert>
      )}

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
        <Card key={category} sx={{ mb: 2 }}>
          <CardContent>
            <Box sx={{ display: 'flex', alignItems: 'center', mb: 2 }}>
              <Typography variant="h6" sx={{ mr: 2 }}>
                {category}
              </Typography>
              <Chip
                label={`${categoryFeatures.length} özellik`}
                size="small"
                color={getCategoryColor(category)}
              />
            </Box>

            <Table size="small">
              <TableHead>
                <TableRow>
                  <TableCell>Özellik</TableCell>
                  <TableCell>Açıklama</TableCell>
                  <TableCell align="center">Durum</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {categoryFeatures.map((feature) => {
                  const isEnabled = userFeatures[feature.code] !== false;

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
                        <FormControlLabel
                          control={
                            <Switch
                              checked={isEnabled}
                              onChange={(e) =>
                                handleFeatureToggle(feature.code, e.target.checked)
                              }
                              disabled={saving}
                            />
                          }
                          label={isEnabled ? 'Açık' : 'Kapalı'}
                        />
                      </TableCell>
                    </TableRow>
                  );
                })}
              </TableBody>
            </Table>
          </CardContent>
        </Card>
      ))}
    </Box>
  );
};

export default UserFeatureManagement;
