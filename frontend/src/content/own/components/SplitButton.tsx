import * as React from 'react';
import {
  Button,
  ButtonGroup,
  Menu,
  MenuItem,
  SxProps,
  Theme
} from '@mui/material';
import ArrowDropDownIcon from '@mui/icons-material/ArrowDropDown';

interface MenuItemProps {
  label: string;
  onClick: () => void;
}

interface SplitButtonProps {
  label: string;
  startIcon?: React.ReactNode;
  onMainClick: () => void;
  menuItems: MenuItemProps[];
  sx?: SxProps<Theme>;
}

const SplitButton: React.FC<SplitButtonProps> = ({
  label,
  startIcon,
  onMainClick,
  menuItems,
  sx
}) => {
  const [anchorEl, setAnchorEl] = React.useState<null | HTMLElement>(null);
  const open = Boolean(anchorEl);

  const handleArrowClick = (event: React.MouseEvent<HTMLButtonElement>) => {
    setAnchorEl(event.currentTarget);
  };

  const handleClose = () => {
    setAnchorEl(null);
  };

  const handleMenuItemClick = (onClick: () => void) => {
    onClick();
    handleClose();
  };

  return (
    <React.Fragment>
      <ButtonGroup
        variant="contained"
        ref={null}
        aria-label="split button"
        sx={sx}
      >
        <Button onClick={onMainClick} startIcon={startIcon}>
          {label}
        </Button>
        <Button
          size="small"
          aria-controls={open ? 'split-button-menu' : undefined}
          aria-expanded={open ? 'true' : undefined}
          aria-label="select merge strategy"
          aria-haspopup="menu"
          onClick={handleArrowClick}
          style={{
            maxWidth: '35px',
            minWidth: '35px'
          }}
        >
          <ArrowDropDownIcon />
        </Button>
      </ButtonGroup>
      <Menu
        id="split-button-menu"
        anchorEl={anchorEl}
        open={open}
        onClose={handleClose}
        anchorOrigin={{
          vertical: 'bottom',
          horizontal: 'right'
        }}
        transformOrigin={{
          vertical: 'top',
          horizontal: 'right'
        }}
      >
        {menuItems.map((item, index) => (
          <MenuItem
            key={`${item.label}-${index}`}
            onClick={() => handleMenuItemClick(item.onClick)}
          >
            {item.label}
          </MenuItem>
        ))}
      </Menu>
    </React.Fragment>
  );
};

export default SplitButton;
