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

import { useTranslation } from 'react-i18next';
import { useContext, useEffect, useState } from 'react';
import { TitleContext } from '../../../../contexts/TitleContext';
import { getUsersMini } from '../../../../slices/user';
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

function Downgrade() {
  const { t }: { t: any } = useTranslation();
  const { setTitle } = useContext(TitleContext);
  const { user, company, downgrade } = useAuth();
  const dispatch = useDispatch();
  const { usersMini } = useSelector((state) => state.users);
  const [selectedUsers, setSelectedUsers] = useState<{
    [key: number]: boolean;
  }>([]);
  const { showSnackBar } = useContext(CustomSnackBarContext);
  const [downgrading, setDowngrading] = useState<boolean>(false);
  const [minUsers, setMinUsers] = useState<number>(0);
  const navigate = useNavigate();

  useEffect(() => {
    setTitle(t('downgrade'));
    if (user.ownsCompany) dispatch(getUsersMini());
  }, []);

  useEffect(() => {
    setMinUsers(usersMini.length - company.subscription.usersCount);
  }, [usersMini]);

  const onDowngrade = () => {
    setDowngrading(true);
    let usersIds: number[] = [];
    Object.entries(selectedUsers).forEach(([key, value]) => {
      if (value) {
        usersIds.push(Number(key));
      }
    });
    if (usersIds.length < minUsers) {
      showSnackBar(t('min_users_description', { minUsers }), 'error');
      setDowngrading(false);
      return;
    }
    downgrade(usersIds)
      .then((success) => {
        if (success) navigate('/app/work-orders');
      })
      .finally(() => setDowngrading(false));
  };
  const onChange = (value: boolean, id: number) => {
    const newSelectedUsers = { ...selectedUsers };
    newSelectedUsers[id] = value;
    setSelectedUsers(newSelectedUsers);
  };
  if (company.subscription.downgradeNeeded)
    return (
      <>
<MainContent>
          <Container maxWidth="md">
            <Box textAlign="center">
              <Typography
                variant="h2"
                sx={{
                  my: 2
                }}
              >
                {t('disable_users')}
              </Typography>
              <Typography
                variant="h4"
                color="text.secondary"
                fontWeight="normal"
                sx={{
                  mb: 4
                }}
              >
                {t(
                  user.ownsCompany
                    ? 'downgrade_description'
                    : 'downgrade_description_no_owner'
                )}
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
                  <FormControl variant="outlined" fullWidth>
                    <Typography
                      variant="h6"
                      sx={{
                        mb: 1
                      }}
                    >
                      {t('min_users_description', { minUsers })}
                    </Typography>
                    <FormGroup>
                      {usersMini
                        .filter((userMini) => userMini.id !== user.id)
                        .map((user) => (
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
                    disabled={downgrading}
                    startIcon={
                      downgrading && <CircularProgress size={'1rem'} />
                    }
                    sx={{ mt: 2 }}
                    onClick={onDowngrade}
                    variant="contained"
                  >
                    {t('disable_users')}
                  </Button>
                </Card>
              </Container>
            )}
          </Container>
        </MainContent>
      </>
    );
  else return <PermissionErrorMessage message={'no_access_page'} />;
}

export default Downgrade;
