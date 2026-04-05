import {
  Alert,
  Box,
  Button,
  Card,
  CardContent,
  Chip,
  CircularProgress,
  Container,
  FormControl,
  InputLabel,
  MenuItem,
  Select,
  Switch,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
  TextField,
  Tooltip,
  Typography
} from '@mui/material';
import ArrowBackIcon from '@mui/icons-material/ArrowBack';
import { useEffect, useState } from 'react';
import { Helmet } from 'react-helmet-async';
import { useNavigate, useParams } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import PageTitleWrapper from 'src/components/PageTitleWrapper';
import api from 'src/utils/api';
import useAuth from 'src/hooks/useAuth';

interface SuperAdminUserDTO {
  id: number;
  username: string;
  email: string;
  firstName: string;
  lastName: string;
  role: { id: number; name: string; code: string };
}

interface SubscriptionPlan {
  id: number;
  name: string;
  code: string;
}

interface SuperAdminCompanyDetailDTO {
  id: number;
  name: string;
  email: string;
  createdAt: string;
  subscriptionPlanId: number | null;
  subscriptionPlanName: string | null;
  usersLimit: number;
  userCount: number;
  users: SuperAdminUserDTO[];
}

interface FeatureStatus {
  feature: string;
  inPlan: boolean;
  override: boolean | null;
  effective: boolean;
}

function SuperAdminCompanyDetail() {
  const { t } = useTranslation();
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { loginInternal } = useAuth();
  const [company, setCompany] = useState<SuperAdminCompanyDetailDTO | null>(
    null
  );
  const [loading, setLoading] = useState(true);
  const [switchingUserId, setSwitchingUserId] = useState<number | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [plans, setPlans] = useState<SubscriptionPlan[]>([]);
  const [selectedPlanId, setSelectedPlanId] = useState<number | ''>('');
  const [usersLimit, setUsersLimit] = useState<number>(1);
  const [savingPlan, setSavingPlan] = useState(false);
  const [planSuccess, setPlanSuccess] = useState(false);
  const [features, setFeatures] = useState<FeatureStatus[]>([]);
  const [savingFeature, setSavingFeature] = useState<string | null>(null);

  const loadFeatures = () => {
    if (id) {
      api
        .get<FeatureStatus[]>(`superadmin/companies/${id}/features`)
        .then(setFeatures)
        .catch(() => {});
    }
  };

  useEffect(() => {
    if (id) {
      api
        .get<SuperAdminCompanyDetailDTO>(`superadmin/companies/${id}`)
        .then((data) => {
          setCompany(data);
          if (data.subscriptionPlanId) setSelectedPlanId(data.subscriptionPlanId);
          if (data.usersLimit) setUsersLimit(data.usersLimit);
        })
        .catch(() => setError(t('error_loading_data')))
        .finally(() => setLoading(false));
    }
    api
      .get<SubscriptionPlan[]>('superadmin/subscription-plans')
      .then(setPlans)
      .catch(() => {});
    loadFeatures();
  }, [id]);

  const handleFeatureOverride = async (feature: string, enabled: boolean | null) => {
    if (!id) return;
    setSavingFeature(feature);
    try {
      await api.patch(`superadmin/companies/${id}/features`, { feature, enabled });
      setFeatures((prev) =>
        prev.map((f) =>
          f.feature === feature
            ? { ...f, override: enabled, effective: enabled !== null ? enabled : f.inPlan }
            : f
        )
      );
    } catch {
      setError('Özellik güncellenemedi');
    } finally {
      setSavingFeature(null);
    }
  };

  const handleSavePlan = async () => {
    if (!id || selectedPlanId === '') return;
    setSavingPlan(true);
    setPlanSuccess(false);
    setError(null);
    try {
      await api.patch(`superadmin/companies/${id}/plan`, {
        planId: selectedPlanId,
        usersLimit
      });
      setPlanSuccess(true);
      setCompany((prev) =>
        prev
          ? {
              ...prev,
              subscriptionPlanId: selectedPlanId as number,
              subscriptionPlanName:
                plans.find((p) => p.id === selectedPlanId)?.name ?? prev.subscriptionPlanName,
              usersLimit
            }
          : prev
      );
    } catch {
      setError('Plan güncellenemedi');
    } finally {
      setSavingPlan(false);
    }
  };

  const handleSwitchUser = async (userId: number) => {
    setSwitchingUserId(userId);
    setError(null);
    try {
      const currentToken = window.localStorage.getItem('accessToken');
      if (currentToken) {
        window.localStorage.setItem('superadminToken', currentToken);
      }
      const response = await api.post<{ accessToken: string }>(
        `superadmin/switch/${userId}`,
        {}
      );
      await loginInternal(response.accessToken);
      navigate('/app/work-orders');
    } catch {
      setError(t('switch_user_failed'));
    } finally {
      setSwitchingUserId(null);
    }
  };

  const handleReturnToSuperAdmin = async () => {
    const superadminToken = window.localStorage.getItem('superadminToken');
    if (superadminToken) {
      window.localStorage.removeItem('superadminToken');
      await loginInternal(superadminToken);
      navigate('/app/superadmin/companies');
    }
  };

  return (
    <>
      <Helmet>
        <title>Superadmin - {company?.name ?? t('company_details')}</title>
      </Helmet>
      {localStorage.getItem('superadminToken') && (
        <Box display="flex" justifyContent="flex-end" p={1}>
          <Button variant="contained" color="warning" onClick={handleReturnToSuperAdmin}>
            ← Superadmin'e Dön
          </Button>
        </Box>
      )}
      <PageTitleWrapper>
        <Box>
          <Button
            startIcon={<ArrowBackIcon />}
            onClick={() => navigate('/app/superadmin/companies')}
            sx={{ mb: 1 }}
          >
            {t('companies')}
          </Button>
          <Typography variant="h2">
            {company?.name ?? t('company_details')}
          </Typography>
        </Box>
      </PageTitleWrapper>

      <Container maxWidth="lg">
        {error && (
          <Alert severity="error" sx={{ mb: 2 }}>
            {error}
          </Alert>
        )}

        {loading ? (
          <Box display="flex" justifyContent="center" p={4}>
            <CircularProgress />
          </Box>
        ) : !company ? (
          <Typography>{t('company_not_found')}</Typography>
        ) : (
          <Box display="flex" flexDirection="column" gap={3}>
            <Card>
              <CardContent>
                <Typography variant="h4" gutterBottom>
                  {t('company_details')}
                </Typography>
                <Box display="flex" gap={4} flexWrap="wrap" mb={3}>
                  <Box>
                    <Typography variant="caption" color="text.secondary">
                      {t('email')}
                    </Typography>
                    <Typography>{company.email || '-'}</Typography>
                  </Box>
                  <Box>
                    <Typography variant="caption" color="text.secondary">
                      {t('subscription_plan')}
                    </Typography>
                    <Typography>
                      {company.subscriptionPlanName ? (
                        <Chip
                          label={company.subscriptionPlanName}
                          size="small"
                          color="primary"
                          variant="outlined"
                        />
                      ) : (
                        '-'
                      )}
                    </Typography>
                  </Box>
                  <Box>
                    <Typography variant="caption" color="text.secondary">
                      {t('user_count')}
                    </Typography>
                    <Typography>{company.userCount}</Typography>
                  </Box>
                  <Box>
                    <Typography variant="caption" color="text.secondary">
                      Kullanıcı Limiti
                    </Typography>
                    <Typography>{company.usersLimit || '-'}</Typography>
                  </Box>
                </Box>

                <Typography variant="h6" gutterBottom>
                  Plan Güncelle
                </Typography>
                {planSuccess && (
                  <Alert severity="success" sx={{ mb: 2 }}>
                    Plan başarıyla güncellendi.
                  </Alert>
                )}
                <Box display="flex" gap={2} alignItems="flex-end" flexWrap="wrap">
                  <FormControl size="small" sx={{ minWidth: 200 }}>
                    <InputLabel>Plan</InputLabel>
                    <Select
                      value={selectedPlanId}
                      label="Plan"
                      onChange={(e) => setSelectedPlanId(e.target.value as number)}
                    >
                      {plans.map((plan) => (
                        <MenuItem key={plan.id} value={plan.id}>
                          {plan.name}
                        </MenuItem>
                      ))}
                    </Select>
                  </FormControl>
                  <TextField
                    size="small"
                    label="Kullanıcı Limiti"
                    type="number"
                    value={usersLimit}
                    onChange={(e) => setUsersLimit(Math.max(1, parseInt(e.target.value, 10) || 1))}
                    inputProps={{ min: 1 }}
                    sx={{ width: 160 }}
                  />
                  <Button
                    variant="contained"
                    onClick={handleSavePlan}
                    disabled={savingPlan || selectedPlanId === ''}
                    startIcon={savingPlan ? <CircularProgress size={14} color="inherit" /> : null}
                  >
                    Kaydet
                  </Button>
                </Box>
              </CardContent>
            </Card>

            <Card>
              <CardContent>
                <Typography variant="h4" gutterBottom>
                  {t('users')}
                </Typography>
                {!company.users || company.users.length === 0 ? (
                  <Typography color="text.secondary">
                    {t('no_users')}
                  </Typography>
                ) : (
                  <Table>
                    <TableHead>
                      <TableRow>
                        <TableCell>
                          <b>{t('name')}</b>
                        </TableCell>
                        <TableCell>
                          <b>{t('email')}</b>
                        </TableCell>
                        <TableCell>
                          <b>{t('role')}</b>
                        </TableCell>
                        <TableCell />
                      </TableRow>
                    </TableHead>
                    <TableBody>
                      {company.users.map((user) => (
                        <TableRow key={user.id} hover>
                          <TableCell>
                            {user.firstName || user.lastName
                              ? `${user.firstName ?? ''} ${user.lastName ?? ''}`.trim()
                              : user.username ?? '-'}
                          </TableCell>
                          <TableCell>{user.email}</TableCell>
                          <TableCell>
                            <Chip
                              label={user.role?.name ?? user.role?.code ?? '-'}
                              size="small"
                              variant="outlined"
                            />
                          </TableCell>
                          <TableCell>
                            <Button
                              variant="contained"
                              size="small"
                              disabled={switchingUserId !== null}
                              startIcon={
                                switchingUserId === user.id ? (
                                  <CircularProgress
                                    size={14}
                                    color="inherit"
                                  />
                                ) : null
                              }
                              onClick={() => handleSwitchUser(user.id)}
                            >
                              {t('switch_to_user')}
                            </Button>
                          </TableCell>
                        </TableRow>
                      ))}
                    </TableBody>
                  </Table>
                )}
              </CardContent>
            </Card>

            {features.length > 0 && (
              <Card>
                <CardContent>
                  <Typography variant="h4" gutterBottom>
                    Özellik Override'ları
                  </Typography>
                  <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
                    Toggle kapalı = plandan gelen varsayılan. Açıkça etkinleştir/devre dışı bırakmak için toggle'ı değiştir, sıfırlamak için "Planı Kullan" butonuna bas.
                  </Typography>
                  <Table size="small">
                    <TableHead>
                      <TableRow>
                        <TableCell><b>Özellik</b></TableCell>
                        <TableCell align="center"><b>Planda Var mı?</b></TableCell>
                        <TableCell align="center"><b>Override</b></TableCell>
                        <TableCell align="center"><b>Aktif</b></TableCell>
                        <TableCell align="center"><b>İşlem</b></TableCell>
                      </TableRow>
                    </TableHead>
                    <TableBody>
                      {features.map((f) => (
                        <TableRow key={f.feature} hover>
                          <TableCell>
                            <Typography variant="body2" fontFamily="monospace">
                              {f.feature}
                            </Typography>
                          </TableCell>
                          <TableCell align="center">
                            <Chip
                              label={f.inPlan ? 'Evet' : 'Hayır'}
                              size="small"
                              color={f.inPlan ? 'success' : 'default'}
                              variant="outlined"
                            />
                          </TableCell>
                          <TableCell align="center">
                            {f.override !== null ? (
                              <Chip
                                label={f.override ? 'Açık' : 'Kapalı'}
                                size="small"
                                color={f.override ? 'primary' : 'error'}
                              />
                            ) : (
                              <Typography variant="caption" color="text.secondary">
                                —
                              </Typography>
                            )}
                          </TableCell>
                          <TableCell align="center">
                            {savingFeature === f.feature ? (
                              <CircularProgress size={20} />
                            ) : (
                              <Tooltip title={f.effective ? 'Aktif' : 'Pasif'}>
                                <Switch
                                  checked={f.effective}
                                  size="small"
                                  onChange={(e) =>
                                    handleFeatureOverride(f.feature, e.target.checked)
                                  }
                                />
                              </Tooltip>
                            )}
                          </TableCell>
                          <TableCell align="center">
                            {f.override !== null && (
                              <Button
                                size="small"
                                variant="outlined"
                                color="inherit"
                                disabled={savingFeature === f.feature}
                                onClick={() => handleFeatureOverride(f.feature, null)}
                              >
                                Planı Kullan
                              </Button>
                            )}
                          </TableCell>
                        </TableRow>
                      ))}
                    </TableBody>
                  </Table>
                </CardContent>
              </Card>
            )}
          </Box>
        )}
      </Container>
    </>
  );
}

export default SuperAdminCompanyDetail;
