import {
  Box,
  FormControl,
  MenuItem,
  Select,
  SelectChangeEvent,
  useTheme
} from '@mui/material';
import { useTranslation } from 'react-i18next';
import { AssetStatus, assetStatuses } from '../../../../models/owns/asset';

interface PropsType {
  value: AssetStatus;
  onChange: (status: AssetStatus) => void;
  disabled?: boolean;
}

export default function AssetStatusSelect({
  value,
  onChange,
  disabled
}: PropsType) {
  const theme = useTheme();
  const { t } = useTranslation();

  const getColor = (status: AssetStatus) =>
    assetStatuses.find((s) => s.status === status)?.color(theme) ?? 'grey';

  return (
    <FormControl size="small">
      <Select
        value={value}
        onChange={(event: SelectChangeEvent) =>
          onChange(event.target.value as AssetStatus)
        }
        disabled={disabled}
        renderValue={(selected) => (
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
            <Box
              sx={{
                width: 12,
                height: 12,
                borderRadius: '50%',
                bgcolor: getColor(selected as AssetStatus)
              }}
            />
            {t(selected)}
          </Box>
        )}
      >
        {assetStatuses.map((assetStatusConfig) => (
          <MenuItem
            key={assetStatusConfig.status}
            value={assetStatusConfig.status}
          >
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
              <Box
                sx={{
                  width: 12,
                  height: 12,
                  borderRadius: '50%',
                  bgcolor: assetStatusConfig.color(theme)
                }}
              />
              {t(assetStatusConfig.status)}
            </Box>
          </MenuItem>
        ))}
      </Select>
    </FormControl>
  );
}
