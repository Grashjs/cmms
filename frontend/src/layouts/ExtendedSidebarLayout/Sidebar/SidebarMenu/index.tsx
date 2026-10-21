import {
  alpha,
  Box,
  Button,
  Link,
  List,
  ListSubheader,
  Stack,
  styled,
  Typography
} from '@mui/material';
import { matchPath, useLocation } from 'react-router-dom';
import SidebarMenuItem from './item';
import menuItems, { MenuItem } from './items';
import { useTranslation } from 'react-i18next';
import useAuth from '../../../../hooks/useAuth';
import { useEffect } from 'react';
import { getUrgentWorkOrdersCount } from '../../../../slices/workOrder';
import { useDispatch, useSelector } from '../../../../store';
import { getPendingRequestsCount } from '../../../../slices/request';
import { PermissionEntity } from '../../../../models/owns/role';
import dayjs from 'dayjs';
import { isCloudVersion } from 'src/config';
import { getLocalizedHomeUrl } from '../../../../utils/urlPaths';
import { brand } from 'src/theme/tokens';

const MenuWrapper = styled(Box)(
  ({ theme }) => `
  & > .MuiList-root {
    padding: ${theme.spacing(0.5, 1.5)};
  }

    .MuiListSubheader-root {
      text-transform: uppercase;
      font-weight: 600;
      letter-spacing: 0.08em;
      font-size: ${theme.typography.pxToRem(11)};
      color: ${alpha(theme.colors.alpha.trueWhite[100], 0.4)};
      background: transparent;
      padding: ${theme.spacing(2, 1.5, 1)};
      line-height: 1.4;
    }
`
);

const SubMenuWrapper = styled(Box)(
  ({ theme }) => `
    & > .MuiList-root {
      padding: 0;
    }

    .MuiList-root {

      .MuiListItem-root {
        padding: 1px 0;

        .MuiBadge-root {
          position: absolute;
          right: ${theme.spacing(1.5)};

          .MuiBadge-standard {
            background: ${theme.colors.primary.main};
            font-size: ${theme.typography.pxToRem(10)};
            font-weight: 700;
            color: ${theme.palette.primary.contrastText};
          }
        }

        .MuiButton-root {
          display: flex;
          position: relative;
          color: ${alpha(theme.colors.alpha.trueWhite[100], 0.72)};
          background-color: transparent;
          width: 100%;
          min-height: 40px;
          justify-content: flex-start;
          font-weight: 500;
          font-size: ${theme.typography.pxToRem(14)};
          border-radius: ${theme.general.borderRadius};
          padding: ${theme.spacing(1, 1.5)};
          transition: ${theme.transitions.create(['background-color', 'color'], {
            duration: theme.transitions.duration.shortest
          })};

          .sidebar-label {
            overflow: hidden;
            text-overflow: ellipsis;
            white-space: nowrap;
          }

          .MuiButton-startIcon,
          .MuiButton-endIcon {
            transition: ${theme.transitions.create(['color'])};

            .MuiSvgIcon-root {
              font-size: inherit;
              transition: none;
            }
          }

          .MuiButton-startIcon {
            color: ${alpha(theme.colors.alpha.trueWhite[100], 0.45)};
            font-size: ${theme.typography.pxToRem(20)};
            margin-left: 0;
            margin-right: ${theme.spacing(1.5)};
          }

          .MuiButton-endIcon {
            color: ${alpha(theme.colors.alpha.trueWhite[100], 0.4)};
            margin-left: auto;
            margin-right: 0;
            font-size: ${theme.typography.pxToRem(18)};
          }

          &.active {
            background-color: ${alpha(theme.colors.primary.main, 0.2)};
            color: ${theme.colors.alpha.trueWhite[100]};
            font-weight: 600;

            &:before {
              content: '';
              position: absolute;
              left: -12px;
              top: 8px;
              bottom: 8px;
              width: 3px;
              border-radius: 0 3px 3px 0;
              background: ${theme.colors.primary.main};
            }

            .MuiButton-startIcon {
              color: ${brand[300]};
            }
            .MuiButton-endIcon {
              color: ${theme.colors.alpha.trueWhite[100]};
            }
          }

          &:hover:not(.active) {
            background-color: ${alpha(theme.colors.alpha.trueWhite[100], 0.06)};
            color: ${theme.colors.alpha.trueWhite[100]};

            .MuiButton-startIcon,
            .MuiButton-endIcon {
              color: ${alpha(theme.colors.alpha.trueWhite[100], 0.85)};
            }
          }

          &.Mui-focusVisible {
            box-shadow: 0 0 0 2px ${brand[400]};
          }
        }

        &.Mui-children {
          flex-direction: column;

          .MuiBadge-root {
            position: absolute;
            right: ${theme.spacing(5)};
          }

          & > .MuiButton-root.active:before {
            display: none;
          }
        }

        .MuiCollapse-root {
          width: 100%;

          .MuiList-root {
            padding: ${theme.spacing(0.5, 0)};
          }

          .MuiListItem-root {
            padding: 1px 0;

            .MuiButton-root {
              min-height: 36px;
              font-size: ${theme.typography.pxToRem(13)};
              padding: ${theme.spacing(0.75, 1.5, 0.75, 5.5)};

              .MuiBadge-root {
                right: ${theme.spacing(1.5)};
              }

              &:before {
                content: ' ';
                position: absolute;
                left: ${theme.spacing(3)};
                top: 50%;
                background: currentColor;
                opacity: 0.35;
                transition: ${theme.transitions.create(['opacity'])};
                width: 5px;
                height: 5px;
                margin-top: -2.5px;
                border-radius: 20px;
              }

              &.active:before {
                opacity: 1;
                width: 5px;
                bottom: auto;
                background: ${brand[300]};
              }

              &.active,
              &:hover {
                &:before {
                  opacity: 1;
                }
              }
            }
          }
        }
      }
    }

    &.collapsed .MuiList-root .MuiListItem-root .MuiButton-root {
      justify-content: center;
      padding-left: 0;
      padding-right: 0;

      .MuiButton-startIcon {
        margin: 0;
      }
      .sidebar-label {
        display: none;
      }
      .MuiBadge-root {
        right: 6px;
        top: 8px;
      }
      .MuiBadge-standard {
        min-width: 8px;
        height: 8px;
        padding: 0;
        font-size: 0;
      }
    }
`
);

const renderSidebarMenuItems = ({
  items,
  path,
  collapsed
}: {
  items: MenuItem[];
  path: string;
  collapsed?: boolean;
}): JSX.Element => (
  <SubMenuWrapper className={collapsed ? 'collapsed' : undefined}>
    <List component="div">
      {items.reduce(
        (ev, item) => reduceChildRoutes({ ev, item, path, collapsed }),
        []
      )}
    </List>
  </SubMenuWrapper>
);

const reduceChildRoutes = ({
  ev,
  path,
  item,
  collapsed
}: {
  ev: JSX.Element[];
  path: string;
  item: MenuItem;
  collapsed?: boolean;
}): Array<JSX.Element> => {
  const key = item.name;
  const exactMatch = item.link
    ? !!matchPath(
        {
          path: item.link,
          end: true
        },
        path
      )
    : false;

  if (item.items) {
    const partialMatch = item.link
      ? !!matchPath(
          {
            path: item.link,
            end: false
          },
          path
        )
      : false;

    ev.push(
      <SidebarMenuItem
        key={key}
        active={partialMatch}
        open={partialMatch}
        name={item.name}
        icon={item.icon}
        link={item.link}
        badge={item.badge}
        badgeTooltip={item.badgeTooltip}
        collapsed={collapsed}
      >
        {renderSidebarMenuItems({
          path,
          items: item.items
        })}
      </SidebarMenuItem>
    );
  } else {
    ev.push(
      <SidebarMenuItem
        key={key}
        active={exactMatch}
        name={item.name}
        link={item.link}
        badge={item.badge}
        badgeTooltip={item.badgeTooltip}
        icon={item.icon}
        collapsed={collapsed}
      />
    );
  }

  return ev;
};

function SidebarMenu({ collapsed = false }: { collapsed?: boolean }) {
  const location = useLocation();
  const { t, i18n }: { t: any; i18n: any } = useTranslation();
  const dispatch = useDispatch();
  const { hasViewPermission, hasFeature, user, company } = useAuth();
  const { urgentCount } = useSelector((state) => state.workOrders);
  const { pendingCount } = useSelector((state) => state.requests);
  const TRIAL_DAYS = 15;
  const daysPassed = dayjs().diff(dayjs(company.createdAt), 'day');
  const daysLeft = TRIAL_DAYS - daysPassed;

  useEffect(() => {
    if (user.id) {
      dispatch(getUrgentWorkOrdersCount());
      if (user.role.code !== 'REQUESTER') dispatch(getPendingRequestsCount());
    }
  }, [user.id]);
  return (
    <>
      {isCloudVersion &&
        !collapsed &&
        !company.demo &&
        user.ownsCompany &&
        !company.subscription.activated &&
        user.superAccountRelations.length === 0 && (
          <Stack
            sx={{
              backgroundColor: alpha(brand[500], 0.18),
              border: `1px solid ${alpha(brand[400], 0.35)}`,
              p: 2,
              mx: 1.5,
              mt: 2,
              borderRadius: 1.5
            }}
            spacing={1}
          >
            <Typography color={'white'} fontSize={'16px'} fontWeight={'bold'}>
              {daysLeft > 0
                ? `Your trial ends in ${daysLeft} days`
                : `Your trial has ended`}
            </Typography>
            <Typography color={'white'} fontSize={'14px'}>
              You are on the {company.subscription.subscriptionPlan.name} plan
            </Typography>
            <Button
              component={Link}
              href={
                isCloudVersion
                  ? '/app/subscription/plans'
                  : getLocalizedHomeUrl(
                      'pricing?type=selfhosted',
                      i18n.language
                    )
              }
              {...(isCloudVersion
                ? {}
                : { target: '_blank', rel: 'noopener noreferrer' })}
              variant="contained"
              color="primary"
              sx={{ mt: 1 }}
            >
              Upgrade
            </Button>
          </Stack>
        )}
      <>
        {(user.superAccountRelations.length
          ? [
              {
                ...menuItems[0],
                items: menuItems[0].items.filter((item) =>
                  [
                    'work_orders',
                    'Statistics',
                    'requests',
                    'preventive_maintenance'
                  ].includes(item.name)
                )
              }
            ]
          : menuItems
        )
          .map((section, index) => {
            const sectionClone = { ...section };
            sectionClone.items = sectionClone.items.filter((item) => {
              const hasPermission = item.permission
                ? hasViewPermission(item.permission)
                : true;
              const featured = item.planFeature
                ? hasFeature(item.planFeature)
                : true;

              const inUiConfig: boolean = user.uiConfiguration
                ? item.uiConfigKey
                  ? user.uiConfiguration[item.uiConfigKey]
                  : true
                : true;

              return hasPermission && featured && inUiConfig;
            });
            if (index === 0) {
              //ownItems
              sectionClone.items = sectionClone.items.map((item) => {
                if (item.name === 'work_orders') {
                  item.badge = urgentCount > 0 ? urgentCount.toString() : null;
                } else if (item.name === 'requests') {
                  item.badge =
                    pendingCount > 0 ? pendingCount.toString() : null;
                }
                return item;
              });
            }
            return sectionClone;
          })
          .map((section) => (
            <MenuWrapper key={section.heading}>
              <List
                component="div"
                subheader={
                  collapsed || !section.heading ? undefined : (
                    <ListSubheader component="div" disableSticky>
                      {t(section.heading)}
                    </ListSubheader>
                  )
                }
              >
                {renderSidebarMenuItems({
                  items: section.items,
                  path: location.pathname,
                  collapsed
                })}
              </List>
            </MenuWrapper>
          ))}
      </>
    </>
  );
}

export default SidebarMenu;
