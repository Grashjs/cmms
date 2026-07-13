import {
  Box,
  Button,
  Card,
  Checkbox,
  CircularProgress,
  Container,
  Divider,
  FormControl,
  FormControlLabel,
  FormGroup,
  styled,
  Typography
} from '@mui/material';
import { Helmet } from 'react-helmet-async';

import { useTranslation } from 'react-i18next';
import { useContext, useEffect, useState } from 'react';
import { TitleContext } from '../../../../contexts/TitleContext';
import { getDisabledUsersMini } from '../../../../slices/user';
import useAuth from '../../../../hooks/useAuth';
import { useDispatch, useSelector } from '../../../../store';
import { CustomSnackBarContext } from '../../../../contexts/CustomSnackBarContext';
import { useNavigate } from 'react-router-dom';
import PermissionErrorMessage from '../../components/PermissionErrorMessage';

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

function Upgrade() {
  const { t }: { t: any } = useTranslation();
  const { setTitle } = useContext(TitleContext);
  const { user, company, upgrade } = useAuth();
  const dispatch = useDispatch();
  const { disabledUsersMini, usersMini } = useSelector((state) => state.users);
  const [selectedUsers, setSelectedUsers] = useState<{
    [key: number]: boolean;
  }>([]);
  const { showSnackBar } = useContext(CustomSnackBarContext);
  const [upgrading, setUpgrading] = useState<boolean>(false);
  const [maxUsers, setMaxUsers] = useState<number>(0);
  const navigate = useNavigate();

  useEffect(() => {
    setTitle(t('upgrade'));
    if (user.ownsCompany) dispatch(getDisabledUsersMini());
  }, []);

  useEffect(() => {
    setMaxUsers(company.subscription.usersCount - usersMini.length);
  }, [usersMini, disabledUsersMini]);

  const onUpgrade = () => {
    setUpgrading(true);
    let usersIds: number[] = [];
    Object.entries(selectedUsers).forEach(([key, value]) => {
      if (value) {
        usersIds.push(Number(key));
      }
    });
    if (usersIds.length > maxUsers) {
      showSnackBar(t('max_users_description', { maxUsers }), 'error');
      setUpgrading(false);
      return;
    }
    upgrade(usersIds)
      .then((success) => {
        if (success) navigate('/app/work-orders');
      })
      .finally(() => setUpgrading(false));
  };
  const onChange = (value: boolean, id: number) => {
    const newSelectedUsers = { ...selectedUsers };
    newSelectedUsers[id] = value;
    setSelectedUsers(newSelectedUsers);
  };
  if (company.subscription.upgradeNeeded && user.ownsCompany)
    return (
      <>
        <Helmet>
          <title>{t('upgrade')}</title>
        </Helmet>
        <MainContent>
          <Container maxWidth="md">
            <Box textAlign="center">
              <Typography
                variant="h2"
                sx={{
                  my: 2
                }}
              >
                {t('enable_users')}
              </Typography>
              <Typography
                variant="h4"
                color="text.secondary"
                fontWeight="normal"
                sx={{
                  mb: 4
                }}
              >
                {t('upgrade_description')}
              </Typography>
            </Box>
            <Container maxWidth="sm">
              <Card
                sx={{
                  textAlign: 'center',
                  mt: 3,
                  p: 4
                }}
              >
                <FormControl variant="outlined" fullWidth>
                  <Typography
                    variant="h6"
                    sx={{
                      mb: 1
                    }}
                  >
                    {t('max_users_description', { maxUsers })}
                  </Typography>
                  <FormGroup>
                    {disabledUsersMini.map((user) => (
                      <FormControlLabel
                        key={user.id}
                        control={
                          <Checkbox
                            checked={selectedUsers[user.id]}
                            onChange={(event) => {
                              onChange(event.target.checked, user.id);
                            }}
                          />
                        }
                        label={`${user.firstName} ${user.lastName}`}
                      />
                    ))}
                  </FormGroup>
                </FormControl>
                <Divider />
                <Button
                  disabled={upgrading}
                  startIcon={upgrading && <CircularProgress size={'1rem'} />}
                  sx={{ mt: 2 }}
                  onClick={onUpgrade}
                  variant="contained"
                >
                  {t('enable_users')}
                </Button>
              </Card>
            </Container>
          </Container>
        </MainContent>
      </>
    );
  else return <PermissionErrorMessage message={'no_access_page'} />;
}

export default Upgrade;
