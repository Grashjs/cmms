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
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
  TextField,
  Typography
} from '@mui/material';
import { useEffect, useState } from 'react';
import { Helmet } from 'react-helmet-async';
import { useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import PageTitleWrapper from 'src/components/PageTitleWrapper';
import api from 'src/utils/api';
import { format } from 'date-fns';

interface SuperAdminCompanyDTO {
  id: number;
  name: string;
  email: string;
  createdAt: string;
  subscriptionPlanName: string | null;
  userCount: number;
}

function SuperAdminCompanies() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const [companies, setCompanies] = useState<SuperAdminCompanyDTO[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Create company dialog
  const [createOpen, setCreateOpen] = useState(false);
  const [createName, setCreateName] = useState('');
  const [createEmail, setCreateEmail] = useState('');
  const [adminFirstName, setAdminFirstName] = useState('');
  const [adminLastName, setAdminLastName] = useState('');
  const [adminEmail, setAdminEmail] = useState('');
  const [adminPassword, setAdminPassword] = useState('');
  const [creating, setCreating] = useState(false);

  // Delete confirmation
  const [deleteTarget, setDeleteTarget] = useState<SuperAdminCompanyDTO | null>(null);
  const [deleting, setDeleting] = useState(false);

  useEffect(() => {
    api
      .get<SuperAdminCompanyDTO[]>('superadmin/companies')
      .then(setCompanies)
      .finally(() => setLoading(false));
  }, []);

  const handleCreate = async () => {
    if (!createName.trim()) return;
    setCreating(true);
    setError(null);
    try {
      const created = await api.post<SuperAdminCompanyDTO>('superadmin/companies', {
        name: createName.trim(),
        email: createEmail.trim(),
        adminFirstName: adminFirstName.trim(),
        adminLastName: adminLastName.trim(),
        adminEmail: adminEmail.trim(),
        adminPassword: adminPassword
      });
      setCompanies((prev) => [...prev, created]);
      setCreateOpen(false);
      setCreateName('');
      setCreateEmail('');
      setAdminFirstName('');
      setAdminLastName('');
      setAdminEmail('');
      setAdminPassword('');
    } catch {
      setError('Şirket oluşturulamadı');
    } finally {
      setCreating(false);
    }
  };

  const handleDelete = async () => {
    if (!deleteTarget) return;
    setDeleting(true);
    setError(null);
    try {
      await api.deletes(`superadmin/companies/${deleteTarget.id}`);
      setCompanies((prev) => prev.filter((c) => c.id !== deleteTarget.id));
      setDeleteTarget(null);
    } catch {
      setError('Şirket silinemedi');
    } finally {
      setDeleting(false);
    }
  };

  return (
    <>
      <Helmet>
        <title>Superadmin - {t('companies')}</title>
      </Helmet>
      <PageTitleWrapper>
        <Box display="flex" justifyContent="space-between" alignItems="center">
          <Typography variant="h2">{t('companies')}</Typography>
          <Button variant="contained" onClick={() => setCreateOpen(true)}>
            + Yeni Şirket
          </Button>
        </Box>
      </PageTitleWrapper>
      <Container maxWidth="lg">
        {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}
        <Card>
          <CardContent>
            {loading ? (
              <Box display="flex" justifyContent="center" p={4}>
                <CircularProgress />
              </Box>
            ) : companies.length === 0 ? (
              <Typography color="text.secondary" p={2}>
                {t('no_companies')}
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
                      <b>{t('subscription_plan')}</b>
                    </TableCell>
                    <TableCell>
                      <b>{t('user_count')}</b>
                    </TableCell>
                    <TableCell>
                      <b>{t('created_at')}</b>
                    </TableCell>
                    <TableCell />
                  </TableRow>
                </TableHead>
                <TableBody>
                  {companies.map((c) => (
                    <TableRow key={c.id} hover>
                      <TableCell>{c.name || '-'}</TableCell>
                      <TableCell>{c.email || '-'}</TableCell>
                      <TableCell>
                        {c.subscriptionPlanName ? (
                          <Chip
                            label={c.subscriptionPlanName}
                            size="small"
                            color="primary"
                            variant="outlined"
                          />
                        ) : (
                          '-'
                        )}
                      </TableCell>
                      <TableCell>{c.userCount}</TableCell>
                      <TableCell>
                        {c.createdAt
                          ? format(new Date(c.createdAt), 'dd.MM.yyyy')
                          : '-'}
                      </TableCell>
                      <TableCell>
                        <Box display="flex" gap={1}>
                          <Button
                            variant="outlined"
                            size="small"
                            onClick={() =>
                              navigate(`/app/superadmin/companies/${c.id}`)
                            }
                          >
                            {t('details')}
                          </Button>
                          <Button
                            variant="outlined"
                            size="small"
                            color="error"
                            onClick={() => setDeleteTarget(c)}
                          >
                            Sil
                          </Button>
                        </Box>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            )}
          </CardContent>
        </Card>
      </Container>
      {/* Create company dialog */}
      <Dialog open={createOpen} onClose={() => setCreateOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Yeni Şirket Oluştur</DialogTitle>
        <DialogContent>
          <Typography variant="subtitle2" color="text.secondary" sx={{ mt: 1, mb: 0.5 }}>
            Şirket Bilgileri
          </Typography>
          <TextField
            label="Şirket Adı *"
            fullWidth
            margin="dense"
            value={createName}
            onChange={(e) => setCreateName(e.target.value)}
            autoFocus
          />
          <TextField
            label="Şirket E-postası"
            fullWidth
            margin="dense"
            value={createEmail}
            onChange={(e) => setCreateEmail(e.target.value)}
            type="email"
          />
          <Typography variant="subtitle2" color="text.secondary" sx={{ mt: 2, mb: 0.5 }}>
            Admin Kullanıcı (isteğe bağlı)
          </Typography>
          <Box display="flex" gap={1}>
            <TextField
              label="Ad"
              fullWidth
              margin="dense"
              value={adminFirstName}
              onChange={(e) => setAdminFirstName(e.target.value)}
            />
            <TextField
              label="Soyad"
              fullWidth
              margin="dense"
              value={adminLastName}
              onChange={(e) => setAdminLastName(e.target.value)}
            />
          </Box>
          <TextField
            label="Admin E-postası"
            fullWidth
            margin="dense"
            value={adminEmail}
            onChange={(e) => setAdminEmail(e.target.value)}
            type="email"
          />
          <TextField
            label="Admin Şifresi"
            fullWidth
            margin="dense"
            value={adminPassword}
            onChange={(e) => setAdminPassword(e.target.value)}
            type="password"
            helperText="Boş bırakılırsa rastgele şifre atanır"
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setCreateOpen(false)}>İptal</Button>
          <Button
            variant="contained"
            onClick={handleCreate}
            disabled={creating || !createName.trim()}
            startIcon={creating ? <CircularProgress size={14} color="inherit" /> : null}
          >
            Oluştur
          </Button>
        </DialogActions>
      </Dialog>

      {/* Delete confirmation dialog */}
      <Dialog open={!!deleteTarget} onClose={() => setDeleteTarget(null)}>
        <DialogTitle>Şirketi Sil</DialogTitle>
        <DialogContent>
          <Typography>
            <b>{deleteTarget?.name}</b> şirketini ve tüm verilerini silmek istediğinizden emin misiniz? Bu işlem geri alınamaz.
          </Typography>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDeleteTarget(null)}>İptal</Button>
          <Button
            variant="contained"
            color="error"
            onClick={handleDelete}
            disabled={deleting}
            startIcon={deleting ? <CircularProgress size={14} color="inherit" /> : null}
          >
            Sil
          </Button>
        </DialogActions>
      </Dialog>
    </>
  );
}

export default SuperAdminCompanies;
