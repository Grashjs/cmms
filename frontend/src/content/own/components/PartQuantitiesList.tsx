import {
  Box,
  Link,
  List,
  ListItem,
  ListItemText,
  TextField,
  Typography
} from '@mui/material';
import { PartQuantityMiniDTO } from '../../../models/owns/partQuantity';
import { useTranslation } from 'react-i18next';
import { useContext } from 'react';
import { CompanySettingsContext } from '../../../contexts/CompanySettingsContext';
import { getFormattedCostPerUnit } from '../../../utils/formatters';

interface PartQuantityListProps {
  partQuantities: PartQuantityMiniDTO[];
  onChange: (value: string, partQuantity: PartQuantityMiniDTO) => void;
  disabled: boolean;
}
export default function PartQuantitiesList({
  partQuantities,
  onChange,
  disabled
}: PartQuantityListProps) {
  const { t }: { t: any } = useTranslation();
  const { getFormattedCurrency } = useContext(CompanySettingsContext);

  return (
    <List>
      {partQuantities.map((partQuantity, index) => (
        <ListItem
          key={partQuantity.part.id}
          secondaryAction={
            <Box display="flex" flexDirection="row" alignItems="center">
              <TextField
                label={t('quantity')}
                variant="outlined"
                sx={{ mr: 1 }}
                type="number"
                disabled={disabled}
                inputProps={{
                  min: '0'
                }}
                size="small"
                onChange={(event) => onChange(event.target.value, partQuantity)}
                defaultValue={partQuantity.quantity}
              />
              <Typography variant="h6">
                {' * '}
                {getFormattedCostPerUnit(
                  partQuantity.part.cost,
                  partQuantity.part.unit,
                  getFormattedCurrency
                )}
              </Typography>
            </Box>
          }
        >
          <ListItemText
            primary={
              <Link
                target="_blank"
                rel="noopener noreferrer"
                href={`/app/inventory/parts/${partQuantity.part.id}`}
                key={partQuantity.part.id}
                variant="h6"
              >
                {partQuantity.part.name}
              </Link>
            }
            secondary={
              <div
                style={{
                  overflow: 'hidden',
                  textOverflow: 'ellipsis',
                  width: '11rem'
                }}
              >
                <Typography noWrap>{partQuantity.part.description}</Typography>
              </div>
            }
          />
        </ListItem>
      ))}
      <ListItem
        secondaryAction={
          <Typography variant="h6" fontWeight="bold">
            {getFormattedCurrency(
              partQuantities.reduce(
                (acc, partQuantity) =>
                  acc + partQuantity.part.cost * partQuantity.quantity,
                0
              )
            )}
          </Typography>
        }
      >
        <ListItemText
          primary={
            <Typography variant="h6" fontWeight="bold">
              {t('total')}
            </Typography>
          }
        />
      </ListItem>
    </List>
  );
}
