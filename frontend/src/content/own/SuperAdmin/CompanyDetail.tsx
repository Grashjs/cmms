import {
  Alert,
  Box,
  Button,
  Card,
  CardContent,
  Chip,
  CircularProgress,
  Container,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
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

interface SuperAdminCompanyDetailDTO {
  id: number;
  name: string;
  email: string;
  createdAt: string;
  subscriptionPlanName: string | null;
  userCount: number;
  users: SuperAdminUserDTO[];
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

  useEffect(() => {
    if (id) {
      api
        .get<SuperAdminCompanyDetailDTO>(`superadmin/companies/${id}`)
        .then(setCompany)
        .catch(() => setError(t('error_loading_data')))
        .finally(() => setLoading(false));
    }
  }, [id]);

  const handleSwitchUser = async (userId: number) => {
    setSwitchingUserId(userId);
    setError(null);
    try {
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

  return (
    <>
      <Helmet>
        <title>Superadmin - {company?.name ?? t('company_details')}</title>
      </Helmet>
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
                <Box display="flex" gap={4} flexWrap="wrap">
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
          </Box>
        )}
      </Container>
    </>
  );
}

export default SuperAdminCompanyDetail;
