import * as React from 'react';
import { ReactNode } from 'react';
import {
  Button,
  Checkbox,
  FormControlLabel,
  Menu,
  MenuItem
} from '@mui/material';
import { useTranslation } from 'react-i18next';
import { FilterField } from '../../../../models/owns/page';
import { SuperAccountRelation } from '../../../../models/user';

interface OwnProps {
  filterFields: FilterField[];
  onChange: (filterFields: FilterField[]) => void;
  superAccountRelations: SuperAccountRelation[];
  icon: ReactNode;
}

function CompanyFilter({
  filterFields,
  onChange,
  superAccountRelations,
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

  const existingFilter = filterFields.find(({ field }) => field === 'company');
  const selectedIds: number[] = existingFilter?.values ?? [];

  const handleChange = (companyId: number) => {
    let newFilterFields = [...filterFields];
    const filterFieldIndex = newFilterFields.findIndex(
      (filterField) => filterField.field === 'company'
    );
    const isCurrentlySelected =
      selectedIds.length === 1 && selectedIds[0] === companyId;
    if (isCurrentlySelected) {
      newFilterFields = newFilterFields.filter((f) => f.field !== 'company');
    } else {
      const newFilter: FilterField = {
        field: 'company',
        operation: 'inm',
        joinType: 'LEFT' as const,
        value: '',
        values: [companyId]
      };
      if (filterFieldIndex === -1) {
        newFilterFields.push(newFilter);
      } else {
        newFilterFields[filterFieldIndex] = newFilter;
      }
    }
    onChange(newFilterFields);
  };

  const selectedName =
    selectedIds.length === 1
      ? superAccountRelations.find(
          (rel) => rel.childCompanyId === selectedIds[0]
        )?.childCompanyName
      : null;

  return (
    <>
      <Button
        onClick={handleOpenMenu}
        sx={{
          '& .MuiButton-startIcon': { margin: '0px' },
          minWidth: 0
        }}
        variant={selectedName ? 'contained' : 'outlined'}
        startIcon={icon}
      >
        {selectedName ?? t('all_companies')}
      </Button>
      <Menu
        id="company-filter-menu"
        anchorEl={anchorEl}
        open={openMenu}
        onClose={handleCloseMenu}
        MenuListProps={{
          'aria-labelledby': 'company-filter-button'
        }}
      >
        {superAccountRelations.map((relation) => {
          const isChecked =
            selectedIds.length === 1 &&
            selectedIds[0] === relation.childCompanyId;
          return (
            <MenuItem
              key={relation.childCompanyId}
              onClick={() => handleChange(relation.childCompanyId)}
            >
              <FormControlLabel
                control={<Checkbox checked={isChecked} />}
                label={relation.childCompanyName}
                onClick={(e) => e.preventDefault()}
              />
            </MenuItem>
          );
        })}
      </Menu>
    </>
  );
}
export default CompanyFilter;
