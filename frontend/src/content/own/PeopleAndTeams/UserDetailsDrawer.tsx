import { ChangeEvent, useEffect, useState } from 'react';
import {
  alpha,
  Avatar,
  Box,
  CardContent,
  Divider,
  styled,
  Tab,
  Tabs,
  Typography,
  useTheme
} from '@mui/material';
import { useTranslation } from 'react-i18next';
import Scrollbar from 'src/components/Scrollbar';
import Chart from 'react-apexcharts';
import type { ApexOptions } from 'apexcharts';
import { OwnUser } from '../../../models/user';
import { useDispatch, useSelector } from '../../../store';
import { getTwoWeeksWorkOrders } from '../../../slices/analytics/user';
import { getDayAndMonthAndYear } from '../../../utils/dates';

const AvatarPrimary = styled(Avatar)(
  ({ theme }) => `
    background: ${theme.colors.primary.lighter};
    color: ${theme.colors.primary.main};
    width: ${theme.spacing(8)};
    height: ${theme.spacing(8)};
`
);

const TabsContainerWrapper = styled(CardContent)(
  ({ theme }) => `
        background-color: ${theme.colors.alpha.black[5]};

        .MuiTabs-flexContainer {
            justify-content: center;
        }
  `
);

interface PropsType {
  user: OwnUser;
}

function UserDetailsDrawer({ user }: PropsType) {
  const { t }: { t: any } = useTranslation();
  const theme = useTheme();
  const dispatch = useDispatch();

  const [currentTab, setCurrentTab] = useState<string>('overview');
  const { twoWeeksWorkOrders } = useSelector((state) => state.userAnalytics);
  const completedWorkOrdersCount = twoWeeksWorkOrders.reduce(
    (acc, day) => day.completed + acc,
    0
  );
  const tabs = [
    { value: 'overview', label: t('overview') },
    { value: 'activity', label: t('activity') }
  ];
  useEffect(() => {
    if (user.id) dispatch(getTwoWeeksWorkOrders(user.id));
  }, [user.id]);
  const handleTabsChange = (_event: ChangeEvent<{}>, value: string): void => {
    setCurrentTab(value);
  };
  const fieldsToRender = (
    user: OwnUser
  ): { label: string; value: string | number }[] => [
    {
      label: t('id'),
      value: user.id
    },
    {
      label: t('first_name'),
      value: user.firstName
    },
    {
      label: t('last_name'),
      value: user.lastName
    },
    {
      label: t('email'),
      value: user.email
    },
    {
      label: t('phone'),
      value: user.phone
    },
    {
      label: t('job_title'),
      value: user.jobTitle
    },
    {
      label: t('role'),
      value: user.role.name
    },
    {
      label: t('hourly_rate'),
      value: user.rate
    }
  ];

  //////////////////////////////////////////////////////////////////////////////////////////////
  const chart3Options: ApexOptions = {
    stroke: {
      curve: 'smooth',
      width: [0, 5]
    },
    theme: {
      mode: theme.palette.mode
    },
    chart: {
      background: 'transparent',
      toolbar: {
        show: false
      }
    },
    colors: [alpha(theme.colors.primary.main, 0.4), theme.colors.primary.main],
    fill: {
      opacity: 1
    },
    labels: twoWeeksWorkOrders.map((day) => getDayAndMonthAndYear(day.date)),
    xaxis: {
      type: 'datetime'
    },
    dataLabels: {
      enabled: false
    },
    grid: {
      strokeDashArray: 5,
      borderColor: theme.palette.divider
    },
    legend: {
      show: true
    }
  };

  const chart3Data = [
    {
      name: t('created'),
      type: 'column',
      data: twoWeeksWorkOrders.map((day) => day.created)
    },
    {
      name: t('completed'),
      type: 'line',
      data: twoWeeksWorkOrders.map((day) => day.completed)
    }
  ];

  return (
    <Box
      sx={{
        height: '100%',
        width: { xs: 340, lg: 400 }
      }}
    >
      <Scrollbar>
        <Box
          sx={{
            textAlign: 'center',
            mt: 1
          }}
        >
          <Typography component="span" variant="subtitle2">
            {t('user_details')}
          </Typography>
        </Box>
        <Divider sx={{ my: 1 }} />

        <Box
          sx={{
            textAlign: 'center'
          }}
        >
          <AvatarPrimary
            sx={{
              mx: 'auto',
              my: 2
            }}
            variant="rounded"
            src={user.image?.url}
          >
            <Typography variant="h1">
              {Array.from(user.firstName)[0].toUpperCase()}
            </Typography>
          </AvatarPrimary>
          <Typography variant="h3" noWrap gutterBottom>
            {`${user.firstName} ${user.lastName}`}
          </Typography>
        </Box>
        <Divider sx={{ my: 3 }} />

        <TabsContainerWrapper>
          <Tabs
            onChange={handleTabsChange}
            value={currentTab}
            variant="scrollable"
            scrollButtons="auto"
            textColor="primary"
            indicatorColor="primary"
          >
            {tabs.map((tab) => (
              <Tab key={tab.value} label={tab.label} value={tab.value} />
            ))}
          </Tabs>
        </TabsContainerWrapper>

        <Divider sx={{ my: 1 }} />

        {currentTab === 'overview' && (
          <>
            {fieldsToRender(user).map(({ label, value }) =>
              value ? (
                <>
                  <Box mt={1} px={3}>
                    <Typography component="span" variant="subtitle2">
                      {label}
                    </Typography>
                    <Typography variant="h5">{value}</Typography>
                  </Box>
                  <Divider sx={{ my: 1 }} />
                </>
              ) : null
            )}
          </>
        )}

        {currentTab === 'activity' && (
          <Box sx={{ mt: 3, px: 3, textAlign: 'center' }}>
            <Typography variant="subtitle2">
              {t('wo_complete_last_14_days')}
            </Typography>
            <Typography variant="h2">{completedWorkOrdersCount}</Typography>
            {completedWorkOrdersCount === 0 && (
              <Typography variant="subtitle2" color="green">
                {t('no_wo_complete_last_14_days')}
              </Typography>
            )}
            <Box flexGrow={1} px={2} pb={2}>
              <Chart
                options={chart3Options}
                series={chart3Data}
                type="line"
                height={'100%'}
              />
            </Box>
          </Box>
        )}
      </Scrollbar>
    </Box>
  );
}

export default UserDetailsDrawer;
