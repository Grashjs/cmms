import {
  Alert,
  Box,
  Button,
  Card,
  CardContent,
  Chip,
  CircularProgress,
  Container,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  FormControl,
  IconButton,
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
import DeleteIcon from '@mui/icons-material/Delete';
import EditIcon from '@mui/icons-material/Edit';
import PersonAddIcon from '@mui/icons-material/PersonAdd';
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
  role: { id: number; name: string; code: string } | string;
}

interface CompanyRole {
  id: number;
  name: string;
  roleType: string;
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
  expiryDate: string | null;
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
  const [expiryDate, setExpiryDate] = useState<string>('');
  const [savingExpiry, setSavingExpiry] = useState(false);
  const [expirySuccess, setExpirySuccess] = useState(false);
  const [features, setFeatures] = useState<FeatureStatus[]>([]);
  const [savingFeature, setSavingFeature] = useState<string | null>(null);

  // Edit company info
  const [editInfoOpen, setEditInfoOpen] = useState(false);
  const [editName, setEditName] = useState('');
  const [editEmail, setEditEmail] = useState('');
  const [savingInfo, setSavingInfo] = useState(false);

  // Delete company
  const [deleteCompanyOpen, setDeleteCompanyOpen] = useState(false);
  const [deletingCompany, setDeletingCompany] = useState(false);

  // Roles for the company
  const [roles, setRoles] = useState<CompanyRole[]>([]);

  // Add user dialog
  const [addUserOpen, setAddUserOpen] = useState(false);
  const [newUserEmail, setNewUserEmail] = useState('');
  const [newUserFirstName, setNewUserFirstName] = useState('');
  const [newUserLastName, setNewUserLastName] = useState('');
  const [newUserRoleId, setNewUserRoleId] = useState<number | ''>('');
  const [addingUser, setAddingUser] = useState(false);

  // Delete user
  const [deleteUserId, setDeleteUserId] = useState<number | null>(null);
  const [deletingUser, setDeletingUser] = useState(false);

  // Change role
  const [changeRoleUserId, setChangeRoleUserId] = useState<number | null>(null);
  const [changeRoleValue, setChangeRoleValue] = useState<number | ''>('');
  const [savingRole, setSavingRole] = useState(false);

  const handleSaveExpiry = async () => {
    if (!id) return;
    setSavingExpiry(true);
    setExpirySuccess(false);
    setError(null);
    try {
      await api.patch(`superadmin/companies/${id}/expiry`, {
        expiryDate: expiryDate ? new Date(expiryDate).toISOString() : null
      });
      setExpirySuccess(true);
    } catch {
      setError('Bitiş tarihi güncellenemedi');
    } finally {
      setSavingExpiry(false);
    }
  };

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
          if (data.expiryDate) setExpiryDate(data.expiryDate.slice(0, 10));
        })
        .catch(() => setError(t('error_loading_data')))
        .finally(() => setLoading(false));
    }
    api
      .get<SubscriptionPlan[]>('superadmin/subscription-plans')
      .then(setPlans)
      .catch(() => {});
    loadFeatures();
    if (id) {
      api
        .get<CompanyRole[]>(`superadmin/companies/${id}/roles`)
        .then(setRoles)
        .catch(() => {});
    }
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

  const handleSaveInfo = async () => {
    if (!id) return;
    setSavingInfo(true);
    setError(null);
    try {
      await api.patch(`superadmin/companies/${id}/info`, { name: editName, email: editEmail });
      setCompany((prev) => prev ? { ...prev, name: editName, email: editEmail } : prev);
      setEditInfoOpen(false);
    } catch {
      setError('Şirket bilgileri güncellenemedi');
    } finally {
      setSavingInfo(false);
    }
  };

  const handleDeleteCompany = async () => {
    if (!id) return;
    setDeletingCompany(true);
    try {
      await api.deletes(`superadmin/companies/${id}`);
      navigate('/app/superadmin/companies');
    } catch {
      setError('Şirket silinemedi');
      setDeletingCompany(false);
      setDeleteCompanyOpen(false);
    }
  };

  const handleAddUser = async () => {
    if (!id || !newUserEmail || newUserRoleId === '') return;
    setAddingUser(true);
    setError(null);
    try {
      const created = await api.post<SuperAdminUserDTO>(`superadmin/companies/${id}/users`, {
        email: newUserEmail,
        firstName: newUserFirstName,
        lastName: newUserLastName,
        roleId: newUserRoleId
      });
      setCompany((prev) =>
        prev ? { ...prev, users: [...(prev.users || []), created], userCount: prev.userCount + 1 } : prev
      );
      setAddUserOpen(false);
      setNewUserEmail('');
      setNewUserFirstName('');
      setNewUserLastName('');
      setNewUserRoleId('');
    } catch {
      setError('Kullanıcı eklenemedi. E-posta zaten kullanımda olabilir.');
    } finally {
      setAddingUser(false);
    }
  };

  const handleDeleteUser = async () => {
    if (!id || deleteUserId === null) return;
    setDeletingUser(true);
    try {
      await api.deletes(`superadmin/companies/${id}/users/${deleteUserId}`);
      setCompany((prev) =>
        prev
          ? { ...prev, users: prev.users.filter((u) => u.id !== deleteUserId), userCount: prev.userCount - 1 }
          : prev
      );
      setDeleteUserId(null);
    } catch {
      setError('Kullanıcı silinemedi');
    } finally {
      setDeletingUser(false);
    }
  };

  const handleChangeRole = async () => {
    if (!id || changeRoleUserId === null || changeRoleValue === '') return;
    setSavingRole(true);
    try {
      await api.patch(`superadmin/companies/${id}/users/${changeRoleUserId}/role`, { roleId: changeRoleValue });
      const roleName = roles.find((r) => r.id === changeRoleValue)?.name ?? '';
      setCompany((prev) =>
        prev
          ? {
              ...prev,
              users: prev.users.map((u) =>
                u.id === changeRoleUserId ? { ...u, role: roleName } : u
              )
            }
          : prev
      );
      setChangeRoleUserId(null);
      setChangeRoleValue('');
    } catch {
      setError('Rol değiştirilemedi');
    } finally {
      setSavingRole(false);
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
        <Box display="flex" justifyContent="space-between" alignItems="flex-start" width="100%">
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
          {company && (
            <Box display="flex" gap={1} mt={1}>
              <Button
                variant="outlined"
                startIcon={<EditIcon />}
                onClick={() => {
                  setEditName(company.name);
                  setEditEmail(company.email ?? '');
                  setEditInfoOpen(true);
                }}
              >
                Düzenle
              </Button>
              <Button
                variant="outlined"
                color="error"
                startIcon={<DeleteIcon />}
                onClick={() => setDeleteCompanyOpen(true)}
              >
                Şirketi Sil
              </Button>
            </Box>
          )}
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

                <Typography variant="h6" gutterBottom sx={{ mt: 3 }}>
                  Abonelik Bitiş Tarihi
                </Typography>
                <Typography variant="body2" color="text.secondary" sx={{ mb: 1 }}>
                  Bu tarihten sonra şirket kullanıcıları giriş yapamaz. Boş bırakılırsa kısıtlama olmaz.
                </Typography>
                {expirySuccess && (
                  <Alert severity="success" sx={{ mb: 2 }}>
                    Bitiş tarihi güncellendi.
                  </Alert>
                )}
                <Box display="flex" gap={2} alignItems="flex-end" flexWrap="wrap">
                  <TextField
                    size="small"
                    label="Bitiş Tarihi"
                    type="date"
                    value={expiryDate}
                    onChange={(e) => setExpiryDate(e.target.value)}
                    InputLabelProps={{ shrink: true }}
                    sx={{ width: 200 }}
                  />
                  <Button
                    variant="contained"
                    color="warning"
                    onClick={handleSaveExpiry}
                    disabled={savingExpiry}
                    startIcon={savingExpiry ? <CircularProgress size={14} color="inherit" /> : null}
                  >
                    Kaydet
                  </Button>
                  {expiryDate && (
                    <Button
                      variant="outlined"
                      color="inherit"
                      disabled={savingExpiry}
                      onClick={() => {
                        setExpiryDate('');
                        api.patch(`superadmin/companies/${id}/expiry`, { expiryDate: null }).catch(() => {});
                      }}
                    >
                      Tarihi Kaldır
                    </Button>
                  )}
                </Box>
              </CardContent>
            </Card>

            <Card>
              <CardContent>
                <Box display="flex" justifyContent="space-between" alignItems="center" mb={2}>
                  <Typography variant="h4">
                    {t('users')} ({company.userCount})
                  </Typography>
                  <Button
                    variant="outlined"
                    size="small"
                    startIcon={<PersonAddIcon />}
                    onClick={() => setAddUserOpen(true)}
                  >
                    Kullanıcı Ekle
                  </Button>
                </Box>
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
                      {company.users.map((user) => {
                        const roleName = typeof user.role === 'string'
                          ? user.role
                          : (user.role as any)?.name ?? (user.role as any)?.code ?? '-';
                        return (
                          <TableRow key={user.id} hover>
                            <TableCell>
                              {user.firstName || user.lastName
                                ? `${user.firstName ?? ''} ${user.lastName ?? ''}`.trim()
                                : user.username ?? '-'}
                            </TableCell>
                            <TableCell>{user.email}</TableCell>
                            <TableCell>
                              {changeRoleUserId === user.id ? (
                                <Box display="flex" gap={1} alignItems="center">
                                  <Select
                                    size="small"
                                    value={changeRoleValue}
                                    onChange={(e) => setChangeRoleValue(e.target.value as number)}
                                    sx={{ minWidth: 140 }}
                                  >
                                    {roles.map((r) => (
                                      <MenuItem key={r.id} value={r.id}>{r.name}</MenuItem>
                                    ))}
                                  </Select>
                                  <Button
                                    size="small"
                                    variant="contained"
                                    disabled={savingRole || changeRoleValue === ''}
                                    onClick={handleChangeRole}
                                  >
                                    {savingRole ? <CircularProgress size={14} /> : 'Kaydet'}
                                  </Button>
                                  <Button size="small" onClick={() => setChangeRoleUserId(null)}>İptal</Button>
                                </Box>
                              ) : (
                                <Chip
                                  label={roleName}
                                  size="small"
                                  variant="outlined"
                                  onClick={() => {
                                    setChangeRoleUserId(user.id);
                                    setChangeRoleValue('');
                                  }}
                                />
                              )}
                            </TableCell>
                            <TableCell>
                              <Box display="flex" gap={1}>
                                <Button
                                  variant="contained"
                                  size="small"
                                  disabled={switchingUserId !== null}
                                  startIcon={
                                    switchingUserId === user.id ? (
                                      <CircularProgress size={14} color="inherit" />
                                    ) : null
                                  }
                                  onClick={() => handleSwitchUser(user.id)}
                                >
                                  {t('switch_to_user')}
                                </Button>
                                <IconButton
                                  size="small"
                                  color="error"
                                  onClick={() => setDeleteUserId(user.id)}
                                >
                                  <DeleteIcon fontSize="small" />
                                </IconButton>
                              </Box>
                            </TableCell>
                          </TableRow>
                        );
                      })}
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

      {/* Edit company info dialog */}
      <Dialog open={editInfoOpen} onClose={() => setEditInfoOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Şirket Bilgilerini Düzenle</DialogTitle>
        <DialogContent>
          <TextField
            label="Şirket Adı"
            fullWidth
            margin="normal"
            value={editName}
            onChange={(e) => setEditName(e.target.value)}
            autoFocus
          />
          <TextField
            label="E-posta"
            fullWidth
            margin="normal"
            value={editEmail}
            onChange={(e) => setEditEmail(e.target.value)}
            type="email"
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setEditInfoOpen(false)}>İptal</Button>
          <Button
            variant="contained"
            onClick={handleSaveInfo}
            disabled={savingInfo || !editName.trim()}
            startIcon={savingInfo ? <CircularProgress size={14} color="inherit" /> : null}
          >
            Kaydet
          </Button>
        </DialogActions>
      </Dialog>

      {/* Delete company confirmation */}
      <Dialog open={deleteCompanyOpen} onClose={() => setDeleteCompanyOpen(false)}>
        <DialogTitle>Şirketi Sil</DialogTitle>
        <DialogContent>
          <Typography>
            <b>{company?.name}</b> şirketini ve tüm verilerini silmek istediğinizden emin misiniz? Bu işlem geri alınamaz.
          </Typography>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDeleteCompanyOpen(false)}>İptal</Button>
          <Button
            variant="contained"
            color="error"
            onClick={handleDeleteCompany}
            disabled={deletingCompany}
            startIcon={deletingCompany ? <CircularProgress size={14} color="inherit" /> : null}
          >
            Sil
          </Button>
        </DialogActions>
      </Dialog>

      {/* Add user dialog */}
      <Dialog open={addUserOpen} onClose={() => setAddUserOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Kullanıcı Ekle</DialogTitle>
        <DialogContent>
          <TextField
            label="E-posta *"
            fullWidth
            margin="normal"
            value={newUserEmail}
            onChange={(e) => setNewUserEmail(e.target.value)}
            type="email"
            autoFocus
          />
          <TextField
            label="Ad"
            fullWidth
            margin="normal"
            value={newUserFirstName}
            onChange={(e) => setNewUserFirstName(e.target.value)}
          />
          <TextField
            label="Soyad"
            fullWidth
            margin="normal"
            value={newUserLastName}
            onChange={(e) => setNewUserLastName(e.target.value)}
          />
          <FormControl fullWidth margin="normal">
            <InputLabel>Rol *</InputLabel>
            <Select
              value={newUserRoleId}
              label="Rol *"
              onChange={(e) => setNewUserRoleId(e.target.value as number)}
            >
              {roles.map((r) => (
                <MenuItem key={r.id} value={r.id}>{r.name}</MenuItem>
              ))}
            </Select>
          </FormControl>
          <Typography variant="caption" color="text.secondary">
            Kullanıcı rastgele şifreyle oluşturulur. Kullanıcı "Şifremi Unuttum" ile şifresini sıfırlayabilir.
          </Typography>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setAddUserOpen(false)}>İptal</Button>
          <Button
            variant="contained"
            onClick={handleAddUser}
            disabled={addingUser || !newUserEmail || newUserRoleId === ''}
            startIcon={addingUser ? <CircularProgress size={14} color="inherit" /> : null}
          >
            Ekle
          </Button>
        </DialogActions>
      </Dialog>

      {/* Delete user confirmation */}
      <Dialog open={deleteUserId !== null} onClose={() => setDeleteUserId(null)}>
        <DialogTitle>Kullanıcıyı Sil</DialogTitle>
        <DialogContent>
          <Typography>
            Bu kullanıcıyı şirketten silmek istediğinizden emin misiniz?
          </Typography>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDeleteUserId(null)}>İptal</Button>
          <Button
            variant="contained"
            color="error"
            onClick={handleDeleteUser}
            disabled={deletingUser}
            startIcon={deletingUser ? <CircularProgress size={14} color="inherit" /> : null}
          >
            Sil
          </Button>
        </DialogActions>
      </Dialog>
    </>
  );
}

export default SuperAdminCompanyDetail;
