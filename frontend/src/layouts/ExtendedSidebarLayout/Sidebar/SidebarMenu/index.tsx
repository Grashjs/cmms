import { alpha, Box, List, ListSubheader, styled } from '@mui/material';
import { matchPath, useLocation } from 'react-router-dom';
import SidebarMenuItem from './item';
import menuItems, { MenuItem } from './items';
import { useTranslation } from 'react-i18next';
import useAuth from '../../../../hooks/useAuth';
import { useEffect } from 'react';
import { getUrgentWorkOrdersCount } from '../../../../slices/workOrder';
import { useDispatch, useSelector } from '../../../../store';
import { getPendingRequestsCount } from '../../../../slices/request';

const MenuWrapper = styled(Box)(
  ({ theme }) => `
  .MuiList-root {
    padding: ${theme.spacing(1)};

    & > .MuiList-root {
      padding: 0 ${theme.spacing(0)} ${theme.spacing(1)};
    }
  }

    .MuiListSubheader-root {
      text-transform: uppercase;
      font-weight: bold;
      font-size: ${theme.typography.pxToRem(12)};
      color: ${theme.colors.alpha.trueWhite[50]};
      padding: ${theme.spacing(0, 2.5)};
      line-height: 1.4;
    }
`
);

const SubMenuWrapper = styled(Box)(
  ({ theme }) => `
    .MuiList-root {

      .MuiListItem-root {
        padding: 1px 0;

        .MuiBadge-root {
          position: absolute;
          right: ${theme.spacing(3.2)};

          .MuiBadge-standard {
            background: ${theme.colors.primary.main};
            font-size: ${theme.typography.pxToRem(10)};
            font-weight: bold;
            text-transform: uppercase;
            color: ${theme.palette.primary.contrastText};
          }
        }
    
        .MuiButton-root {
          display: flex;
          color: ${theme.colors.alpha.trueWhite[70]};
          background-color: transparent;
          width: 100%;
          justify-content: flex-start;
          padding: ${theme.spacing(1.2, 3)};

          .MuiButton-startIcon,
          .MuiButton-endIcon {
            transition: ${theme.transitions.create(['color'])};

            .MuiSvgIcon-root {
              font-size: inherit;
              transition: none;
            }
          }

          .MuiButton-startIcon {
            color: ${theme.colors.alpha.trueWhite[30]};
            font-size: ${theme.typography.pxToRem(20)};
            margin-right: ${theme.spacing(1)};
          }
          
          .MuiButton-endIcon {
            color: ${theme.colors.alpha.trueWhite[50]};
            margin-left: auto;
            opacity: .8;
            font-size: ${theme.typography.pxToRem(20)};
          }

          &.active,
          &:hover {
            background-color: ${alpha(theme.colors.alpha.trueWhite[100], 0.06)};
            color: ${theme.colors.alpha.trueWhite[100]};

            .MuiButton-startIcon,
            .MuiButton-endIcon {
              color: ${theme.colors.alpha.trueWhite[100]};
            }
          }
        }

        &.Mui-children {
          flex-direction: column;

          .MuiBadge-root {
            position: absolute;
            right: ${theme.spacing(7)};
          }
        }

        .MuiCollapse-root {
          width: 100%;

          .MuiList-root {
            padding: ${theme.spacing(1, 0)};
          }

          .MuiListItem-root {
            padding: 1px 0;

            .MuiButton-root {
              padding: ${theme.spacing(0.8, 3)};

              .MuiBadge-root {
                right: ${theme.spacing(3.2)};
              }

              &:before {
                content: ' ';
                background: ${theme.colors.alpha.trueWhite[100]};
                opacity: 0;
                transition: ${theme.transitions.create([
                  'transform',
                  'opacity'
                ])};
                width: 6px;
                height: 6px;
                transform: scale(0);
                transform-origin: center;
                border-radius: 20px;
                margin-right: ${theme.spacing(1.8)};
              }

              &.active,
              &:hover {

                &:before {
                  transform: scale(1);
                  opacity: 1;
                }
              }
            }
          }
        }
      }
    }
`
);

const renderSidebarMenuItems = ({
  items,
  path
}: {
  items: MenuItem[];
  path: string;
}): JSX.Element => (
  <SubMenuWrapper>
    <List component="div">
      {items.reduce((ev, item) => reduceChildRoutes({ ev, item, path }), [])}
    </List>
  </SubMenuWrapper>
);

const reduceChildRoutes = ({
  ev,
  path,
  item
}: {
  ev: JSX.Element[];
  path: string;
  item: MenuItem;
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
      />
    );
  }

  return ev;
};

function SidebarMenu() {
  const location = useLocation();
  const { t }: { t: any } = useTranslation();
  const dispatch = useDispatch();
  const { hasViewPermission, hasFeature, user } = useAuth();
  const { urgentCount } = useSelector((state) => state.workOrders);
  const { pendingCount } = useSelector((state) => state.requests);

  useEffect(() => {
    if (user.id) {
      dispatch(getUrgentWorkOrdersCount());
      dispatch(getPendingRequestsCount());
    }
  }, [user.id]);
  return (
    <>
      {menuItems
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
                item.badge = pendingCount > 0 ? pendingCount.toString() : null;
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
                <ListSubheader component="div" disableSticky>
                  {t(section.heading)}
                </ListSubheader>
              }
            >
              {renderSidebarMenuItems({
                items: section.items,
                path: location.pathname
              })}
            </List>
          </MenuWrapper>
        ))}
    </>
  );
}

export default SidebarMenu;
