import {
  Box,
  Button,
  Card, CardContent,
  Checkbox,
  Avatar,
  CircularProgress,
  Container,
  Divider,
  FormControl, Grid,
  FormControlLabel,
  FormGroup,
  styled,
  Typography
} from '@mui/material';

import { useTranslation } from 'react-i18next';
import { useContext, useEffect, useState } from 'react';
import { TitleContext } from '../../../contexts/TitleContext';
import { getUsersMini } from '../../../slices/user';
import useAuth from '../../../hooks/useAuth';
import { useDispatch, useSelector } from '../../../store';
import { CustomSnackBarContext } from '../../../contexts/CustomSnackBarContext';
import { useNavigate } from 'react-router-dom';
import PermissionErrorMessage from '../components/PermissionErrorMessage';

const MainContent = styled(Box)(
  () => `
    height: 100%;
    display: flex;
    flex: 1;
    overflow: auto;
    flex-direction: column;
    align-items: center;
    justify-content: center;
`
);

function SwitchAccount() {
  const { t }: { t: any } = useTranslation();
  const { setTitle } = useContext(TitleContext);
  const { user, company, downgrade, switchAccount } = useAuth();
  const dispatch = useDispatch();
  const { showSnackBar } = useContext(CustomSnackBarContext);
  const [switching, setSwitching] = useState<boolean>(false);
  const [selectedUserId, setSelectedUserId] = useState<number>(null);
  const navigate = useNavigate();

  useEffect(() => {
    setTitle(t('switch_account'));
  }, []);


  if (user.superAccountRelations.length)
    return (
      <>
<MainContent>
          <Container maxWidth="lg">
            <Box textAlign="center">
              <Typography
                variant="h2"
                sx={{
                  my: 2
                }}
              >
                {t('switch_account')}
              </Typography>
              <Typography
                variant="h4"
                color="text.secondary"
                fontWeight="normal"
                sx={{
                  mb: 4
                }}
              >
                {t('switch_account_description')}
              </Typography>
            </Box>
            {user.ownsCompany && (
              <Container maxWidth="sm">
                <Card
                  sx={{
                    textAlign: 'center',
                    mt: 3,
                    p: 4
                  }}
                >
                  <Grid container spacing={2}>
                    {user.superAccountRelations.map(superAccountRelation => <Grid item
                                                                                  key={superAccountRelation.childUserId}
                                                                                  xs={4}> <Card
                      onClick={() => {
                        if (!switching) {
                          setSwitching(true);
                          setSelectedUserId(superAccountRelation.childUserId);
                          switchAccount(superAccountRelation.childUserId).then(() => navigate('/app/work-orders')).finally(() => setSwitching(false));
                        }
                      }} sx={{ cursor: switching ? 'auto' : 'pointer' }}>
                      <CardContent sx={{ display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
                        <Avatar variant="rounded" src={superAccountRelation.childCompanyLogo?.url}
                                alt={superAccountRelation.childCompanyName} />
                        <Typography fontWeight={'bold'}
                                    sx={{ my: 2 }}>{superAccountRelation.childCompanyName}</Typography>
                        {switching && superAccountRelation.childUserId === selectedUserId ? <CircularProgress /> : null}
                      </CardContent>
                    </Card></Grid>)}
                  </Grid>
                </Card>
              </Container>
            )}
          </Container>
        </MainContent>
      </>
    );
  else return <PermissionErrorMessage message={'no_access_page'} />;
}

export default SwitchAccount;
