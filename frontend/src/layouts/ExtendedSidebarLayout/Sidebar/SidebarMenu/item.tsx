import { FC, ReactNode, useState, useContext } from 'react';
import { NavLink as RouterLink } from 'react-router-dom';
import clsx from 'clsx';
import { SidebarContext } from 'src/contexts/SidebarContext';

import PropTypes from 'prop-types';
import { Button, Tooltip, Badge, Collapse, ListItem } from '@mui/material';
import { useTranslation } from 'react-i18next';
import ExpandLessTwoToneIcon from '@mui/icons-material/ExpandLessTwoTone';
import ExpandMoreTwoToneIcon from '@mui/icons-material/ExpandMoreTwoTone';

interface SidebarMenuItemProps {
  children?: ReactNode;
  link?: string;
  icon?: any;
  badge?: string;
  badgeTooltip?: string;
  open?: boolean;
  active?: boolean;
  name: string;
  collapsed?: boolean;
}

const renderBadge = (badge: string, badgeTooltip: string) => {
  const content =
    badge === '' ? (
      <Badge color="primary" variant="dot" />
    ) : (
      <Badge badgeContent={badge} />
    );
  return badgeTooltip ? (
    <Tooltip title={badgeTooltip} arrow placement="right">
      {content}
    </Tooltip>
  ) : (
    content
  );
};

const SidebarMenuItem: FC<SidebarMenuItemProps> = ({
  children,
  link,
  icon: Icon,
  badge,
  badgeTooltip,
  open: openParent,
  active,
  name,
  collapsed,
  ...rest
}) => {
  const [menuToggle, setMenuToggle] = useState<boolean>(openParent);
  const { t }: { t: any } = useTranslation();
  const { closeSidebar, expandSidebar } = useContext(SidebarContext);
  const label = t(name);

  const toggleMenu = (): void => {
    if (collapsed) {
      // Sub-items need room for labels: expand the rail, then open the group.
      expandSidebar();
      setMenuToggle(true);
      return;
    }
    setMenuToggle((Open) => !Open);
  };

  const withTooltip = (node: JSX.Element) =>
    collapsed ? (
      <Tooltip title={label} placement="right" arrow>
        {node}
      </Tooltip>
    ) : (
      node
    );

  if (children) {
    return (
      <ListItem component="div" className="Mui-children" key={name} {...rest}>
        {withTooltip(
          <Button
            className={clsx({ active: active || (menuToggle && !collapsed) })}
            startIcon={Icon && <Icon />}
            endIcon={
              collapsed ? undefined : menuToggle ? (
                <ExpandLessTwoToneIcon />
              ) : (
                <ExpandMoreTwoToneIcon />
              )
            }
            onClick={toggleMenu}
            aria-expanded={!collapsed && menuToggle}
            aria-label={collapsed ? label : undefined}
            sx={{ textAlign: 'start' }}
          >
            {renderBadge(badge, badgeTooltip)}
            <span className="sidebar-label">{label}</span>
          </Button>
        )}
        <Collapse in={menuToggle && !collapsed}>{children}</Collapse>
      </ListItem>
    );
  }

  return (
    <ListItem component="div" key={name} {...rest}>
      {withTooltip(
        <Button
          disableRipple
          component={RouterLink}
          onClick={closeSidebar}
          to={link}
          startIcon={Icon && <Icon />}
          aria-label={collapsed ? label : undefined}
        >
          <span className="sidebar-label">{label}</span>
          {renderBadge(badge, badgeTooltip)}
        </Button>
      )}
    </ListItem>
  );
};

SidebarMenuItem.propTypes = {
  children: PropTypes.node,
  active: PropTypes.bool,
  link: PropTypes.string,
  icon: PropTypes.elementType,
  badge: PropTypes.string,
  badgeTooltip: PropTypes.string,
  open: PropTypes.bool,
  name: PropTypes.string.isRequired,
  collapsed: PropTypes.bool
};

SidebarMenuItem.defaultProps = {
  open: false,
  active: false,
  collapsed: false
};

export default SidebarMenuItem;
