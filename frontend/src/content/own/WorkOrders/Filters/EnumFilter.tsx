import * as React from 'react';
import { ReactNode } from 'react';
import { enumerate } from '../../../../utils/displayers';
import { Button, Checkbox, ListItemText, Menu, MenuItem } from '@mui/material';
import { useTranslation } from 'react-i18next';
import { FilterField } from '../../../../models/owns/page';
import { pushOrRemove } from '../../../../utils/overall';

interface OwnProps {
  filterFields: FilterField[];
  onChange: (filterFields: FilterField[]) => void;
  completeOptions: string[];
  fieldName: string;
  icon: ReactNode;
}
function EnumFilter({
  filterFields,
  onChange,
  completeOptions,
  fieldName,
  icon
}: OwnProps) {
  const [anchorEl, setAnchorEl] = React.useState<null | HTMLElement>(null);
  const openMenu = Boolean(anchorEl);
  const { t }: { t: any } = useTranslation();

  const handleOpenMenu = (event: React.MouseEvent<HTMLButtonElement>) => {
    setAnchorEl(event.currentTarget);
  };
  const handleCloseMenu = () => {
    setAnchorEl(null);
  };
  return (
    <>
      <Button
        onClick={handleOpenMenu}
        sx={{
          '& .MuiButton-startIcon': { margin: '0px' },
          minWidth: 0
        }}
        variant={'outlined'}
        startIcon={icon}
      >
        {enumerate(
          filterFields
            .find(({ field }) => field === fieldName)
            .values.map((priority) => t(priority))
        )}
      </Button>
      <Menu
        id="basic-menu"
        anchorEl={anchorEl}
        open={openMenu}
        onClose={handleCloseMenu}
        MenuListProps={{
          'aria-labelledby': 'basic-button'
        }}
      >
        {completeOptions.map((option, index) => (
          <MenuItem key={index}>
            <Checkbox
              onChange={(event) => {
                const newFilterFields = [...filterFields];
                const filterFieldIndex = newFilterFields.findIndex(
                  (filterField) => filterField.field === fieldName
                );
                newFilterFields[filterFieldIndex] = {
                  ...newFilterFields[filterFieldIndex],
                  values: pushOrRemove(
                    newFilterFields[filterFieldIndex].values,
                    event.target.checked,
                    option
                  )
                };
                onChange(newFilterFields);
              }}
              checked={filterFields.some(
                (filterField) =>
                  filterField.field === fieldName &&
                  filterField.values.includes(option)
              )}
            />
            <ListItemText primary={t(option)} />
          </MenuItem>
        ))}
      </Menu>
    </>
  );
}
export default EnumFilter;
