import { ChangeEvent, ReactNode } from 'react';
import { Helmet } from 'react-helmet-async';
import AddTwoToneIcon from '@mui/icons-material/AddTwoTone';
import { Box, Button, Card, Stack, styled, Tab, Tabs } from '@mui/material';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router-dom';
import EditTwoToneIcon from '@mui/icons-material/EditTwoTone';

const TabsContainerWrapper = styled(Box)(
  ({ theme }) => `
      max-width: 100%;
      min-width: 0;
      flex: 1;

      .MuiTabs-scrollableX {
        overflow-x: auto !important;
      }
  `
);

interface SettingsLayoutProps {
  children?: ReactNode;
  tabs: { value: string; label: string }[];
  basePath: string;
  title: string;
  tabIndex: number;
  action?: () => void;
  secondAction?: () => void;
  actionTitle?: string;
  secondActionTitle?: string;
  secondActionIcon?: ReactNode;
  editAction?: boolean;
  rawAction?: ReactNode;
  withoutCard?: boolean;
}

function MultipleTabsLayout(props: SettingsLayoutProps) {
  const {
    children,
    tabIndex,
    title,
    tabs,
    basePath,
    action,
    actionTitle,
    withoutCard,
    editAction,
    secondAction,
    secondActionTitle,
    secondActionIcon,
    rawAction
  } = props;
  const { t }: { t: any } = useTranslation();
  const navigate = useNavigate();
  const currentTab = tabs[tabIndex].value;

  const handleTabsChange = (_event: ChangeEvent<{}>, value: string): void => {
    navigate(`${basePath}/${value}`);
  };

  return (
    <Box mt={1.5} px={{ xs: 2, md: 3, lg: 4 }}>
      <Helmet>
        <title>{title}</title>
      </Helmet>
      <Box
        display="flex"
        justifyContent="space-between"
        alignItems="center"
        flexWrap="wrap"
        gap={1}
        sx={{ borderBottom: 1, borderColor: 'divider', mb: 2.5 }}
      >
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
        {(action || secondAction || rawAction) && (
          <Stack direction="row" spacing={1} sx={{ py: 1 }}>
            {action && (
              <Button
                startIcon={
                  editAction ? <EditTwoToneIcon /> : <AddTwoToneIcon />
                }
                variant="contained"
                onClick={action}
              >
                {actionTitle}
              </Button>
            )}
            {secondAction && secondActionTitle && (
              <Button
                startIcon={secondActionIcon}
                variant="outlined"
                onClick={secondAction}
              >
                {secondActionTitle}
              </Button>
            )}
            {rawAction}
          </Stack>
        )}
      </Box>
      {withoutCard ? (
        children
      ) : (
        <Card>{children}</Card>
      )}
    </Box>
  );
}

export default MultipleTabsLayout;
