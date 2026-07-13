import { ChangeEvent, ReactNode } from 'react';
import { Helmet } from 'react-helmet-async';
import AddTwoToneIcon from '@mui/icons-material/AddTwoTone';
import { Box, Button, Card, Stack, styled, Tab, Tabs } from '@mui/material';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router-dom';
import EditTwoToneIcon from '@mui/icons-material/EditTwoTone';

const TabsContainerWrapper = styled(Box)(
  ({ theme }) => `
      padding: 0 ${theme.spacing(8)};
      margin-top: 2px;
      position: relative;
      bottom: -1px;
      max-width: 82%;

      .MuiTabs-root {
        height: 44px;
        min-height: 44px;
      }

      .MuiTabs-scrollableX {
        overflow-x: auto !important;
      }

      .MuiTabs-indicator {
          min-height: 4px;
          height: 4px;
          box-shadow: none;
          bottom: -4px;
          background: none;
          border: 0;

          &:after {
            position: absolute;
            left: 50%;
            width: 28px;
            content: ' ';
            margin-left: -14px;
            background: ${theme.colors.primary.main};
            border-radius: inherit;
            height: 100%;
          }
      }

      .MuiTab-root {
          &.MuiButtonBase-root {
              height: 44px;
              min-height: 44px;
              background: ${theme.colors.alpha.white[50]};
              border: 1px solid ${theme.colors.alpha.black[10]};
              border-bottom: 0;
              position: relative;
              margin-right: ${theme.spacing(1)};
              font-size: ${theme.typography.pxToRem(14)};
              color: ${theme.colors.alpha.black[80]};
              border-bottom-left-radius: 0;
              border-bottom-right-radius: 0;

              .MuiTouchRipple-root {
                opacity: .1;
              }

              &:after {
                position: absolute;
                left: 0;
                right: 0;
                width: 100%;
                bottom: 0;
                height: 1px;
                content: '';
                background: ${theme.colors.alpha.black[10]};
              }

              &:hover {
                color: ${theme.colors.alpha.black[100]};
              }
          }

          &.Mui-selected {
              color: ${theme.colors.alpha.black[100]};
              background: ${theme.colors.alpha.white[100]};
              border-bottom-color: ${theme.colors.alpha.white[100]};

              &:after {
                height: 0;
              }
          }
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
    secondActionIcon
  } = props;
  const { t }: { t: any } = useTranslation();
  const navigate = useNavigate();
  const currentTab = tabs[tabIndex].value;

  const handleTabsChange = (_event: ChangeEvent<{}>, value: string): void => {
    navigate(`${basePath}/${value}`);
  };

  return (
    <Box mt={1}>
      <Helmet>
        <title>{title}</title>
      </Helmet>
      <Box display="flex" justifyContent="space-between">
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
        <Stack direction="row" spacing={1} sx={{ mr: 4, my: 1 }}>
          {action && (
            <Button
              startIcon={editAction ? <EditTwoToneIcon /> : <AddTwoToneIcon />}
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
        </Stack>
      </Box>
      {withoutCard ? (
        children
      ) : (
        <Card
          variant="outlined"
          sx={{
            mx: 4
          }}
        >
          {children}
        </Card>
      )}
    </Box>
  );
}

export default MultipleTabsLayout;
