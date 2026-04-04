import {
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

  useEffect(() => {
    api
      .get<SuperAdminCompanyDTO[]>('superadmin/companies')
      .then(setCompanies)
      .finally(() => setLoading(false));
  }, []);

  return (
    <>
      <Helmet>
        <title>Superadmin - {t('companies')}</title>
      </Helmet>
      <PageTitleWrapper>
        <Typography variant="h2">{t('companies')}</Typography>
      </PageTitleWrapper>
      <Container maxWidth="lg">
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
                        <Button
                          variant="outlined"
                          size="small"
                          onClick={() =>
                            navigate(`/app/superadmin/companies/${c.id}`)
                          }
                        >
                          {t('details')}
                        </Button>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            )}
          </CardContent>
        </Card>
      </Container>
    </>
  );
}

export default SuperAdminCompanies;
