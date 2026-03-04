import {
  Alert,
  Autocomplete,
  Box,
  Button,
  IconButton,
  InputAdornment,
  Stack,
  TextField,
  Typography,
  alpha,
  useTheme
} from '@mui/material';
import { FormikProvider, useFormik } from 'formik';
import { useMemo, useState } from 'react';
import { useTranslation } from 'react-i18next';
import BuildOutlinedIcon from '@mui/icons-material/BuildOutlined';
import DescriptionOutlinedIcon from '@mui/icons-material/DescriptionOutlined';
import ImageOutlinedIcon from '@mui/icons-material/ImageOutlined';
import InfoOutlinedIcon from '@mui/icons-material/InfoOutlined';
import LocationOnOutlinedIcon from '@mui/icons-material/LocationOnOutlined';
import PersonOutlineIcon from '@mui/icons-material/PersonOutline';
import AttachFileOutlinedIcon from '@mui/icons-material/AttachFileOutlined';
import SearchIcon from '@mui/icons-material/Search';
import {
  PortalFieldType,
  RequestPortalField
} from '../../../../models/owns/requestPortal';
import { LocationMiniDTO } from '../../../../models/owns/location';
import { AssetMiniDTO } from '../../../../models/owns/asset';
import SelectLocationModal from './SelectLocationModal';
import SelectAssetModal from './SelectAssetModal';
import FileUpload from '../FileUpload';
import { FIELD_DEFS } from '../../Settings/RequestPortal/components/RequestPortalModal';
// ---------------------------------------------------------------------------
// Types
// ---------------------------------------------------------------------------

export type SelectionMode = 'all' | 'specific';

export interface PreviewFieldConfig {
  type: PortalFieldType | 'TITLE';
  enabled: boolean;
  required: boolean;
  selectionMode: SelectionMode;
  location?: LocationMiniDTO | null;
  asset?: AssetMiniDTO | null;
}

export interface RequestPortalPreviewProps {
  title: string;
  welcomeMessage: string;
  fieldConfigs: PreviewFieldConfig[];
  preview?: boolean;
  onFieldChange?: (index: number, patch: Partial<PreviewFieldConfig>) => void;
  onLocationSelect?: (index: number, location: LocationMiniDTO | null) => void;
  onAssetSelect?: (index: number, asset: AssetMiniDTO | null) => void;
}

// ---------------------------------------------------------------------------
// Field definitions
// ---------------------------------------------------------------------------

interface FieldDef {
  type: PortalFieldType | 'TITLE';
  icon: React.ReactNode;
  labelKey: string;
  alwaysEnabled?: boolean;
  alwaysRequired?: boolean;
  hasSelectionPanel?: boolean;
  publicWarningKey?: string;
}

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

export const buildDefaultConfigs = (
  existingFields?: RequestPortalField[]
): PreviewFieldConfig[] => {
  const defaultEnabledFields: (PortalFieldType | 'TITLE')[] = [
    'TITLE',
    'DESCRIPTION',
    'CONTACT',
    'IMAGE',
    'FILES'
  ];
  return FIELD_DEFS.map((def) => {
    const existing = existingFields?.find(
      (f) => (f.type as string) === def.type
    );
    const selectionMode =
      def.type === 'ASSET' || def.type === 'LOCATION'
        ? existing?.[def.type.toLowerCase()]
          ? 'specific'
          : 'all'
        : 'all';
    return {
      type: def.type,
      enabled: def.alwaysEnabled
        ? true
        : !!existing ||
          (!existingFields?.length && defaultEnabledFields.includes(def.type)),
      required: def.alwaysRequired ? true : existing?.required ?? false,
      selectionMode,
      location: existing?.location || null,
      asset: existing?.asset || null
    };
  });
};

export const configsToFields = (
  configs: PreviewFieldConfig[]
): RequestPortalField[] =>
  configs
    .filter((c) => c.enabled && c.type !== 'TITLE')
    .map((c) => ({
      type: c.type as PortalFieldType,
      location: c.location || null,
      asset: c.asset || null,
      required: c.required
    }));

// ---------------------------------------------------------------------------
// AssetLocationClause - Simplified component for asset/location selection
// ---------------------------------------------------------------------------

interface AssetLocationClauseProps {
  field: {
    name: string;
    type: 'asset' | 'location';
    value: AssetMiniDTO | LocationMiniDTO | null;
    required?: boolean;
    disabled?: boolean;
  };
  onChange: (value: AssetMiniDTO | LocationMiniDTO | null) => void;
  locationId?: number | null;
  excludedIds?: number[];
}

export function AssetLocationClause({
  field,
  onChange,
  locationId,
  excludedIds
}: AssetLocationClauseProps) {
  const { t } = useTranslation();
  const [modalOpen, setModalOpen] = useState(false);

  const isLocation = field.type === 'location';

  const valueOption = field.value
    ? {
        label: (field.value as any).name,
        value: (field.value as any).id,
        dto: field.value
      }
    : null;

  return (
    <>
      <Autocomplete
        fullWidth
        value={valueOption}
        onChange={(_, newValue) => {
          onChange(newValue ? (newValue.dto as any) : null);
        }}
        options={valueOption ? [valueOption] : []}
        getOptionLabel={(option) => option.label}
        isOptionEqualToValue={(option, value) =>
          !value || option.value === value.value
        }
        renderInput={(params) => (
          <TextField
            {...params}
            variant="outlined"
            required={field.required}
            label={isLocation ? t('location') : t('asset')}
            placeholder={isLocation ? t('select_location') : t('select_asset')}
            InputProps={{
              ...params.InputProps,
              endAdornment: (
                <>
                  <InputAdornment position="end">
                    <IconButton
                      size="small"
                      onClick={(e) => {
                        e.stopPropagation();
                        setModalOpen(true);
                      }}
                    >
                      <SearchIcon />
                    </IconButton>
                  </InputAdornment>
                  {params.InputProps.endAdornment}
                </>
              )
            }}
          />
        )}
      />
      {isLocation ? (
        <SelectLocationModal
          open={modalOpen}
          onClose={() => setModalOpen(false)}
          excludedLocationIds={excludedIds}
          maxSelections={1}
          initialSelectedLocations={
            field.value ? [field.value as LocationMiniDTO] : []
          }
          onSelect={(selectedLocations) => {
            onChange(selectedLocations.length ? selectedLocations[0] : null);
            setModalOpen(false);
          }}
        />
      ) : (
        <SelectAssetModal
          open={modalOpen}
          onClose={() => setModalOpen(false)}
          excludedAssetIds={excludedIds}
          locationId={locationId || undefined}
          maxSelections={1}
          initialSelectedAssets={
            field.value ? [field.value as AssetMiniDTO] : []
          }
          onSelect={(selectedAssets) => {
            onChange(selectedAssets.length ? selectedAssets[0] : null);
            setModalOpen(false);
          }}
        />
      )}
    </>
  );
}

// ---------------------------------------------------------------------------
// PreviewFieldRender - Renders a single field in preview mode
// ---------------------------------------------------------------------------

interface PreviewFieldRenderProps {
  config: PreviewFieldConfig;
  def: FieldDef;
  t: (k: string) => string;
  onLocationSelect?: (location: LocationMiniDTO | null) => void;
  onAssetSelect?: (asset: AssetMiniDTO | null) => void;
}

function PreviewFieldRender({
  config,
  def,
  t,
  onLocationSelect,
  onAssetSelect
}: PreviewFieldRenderProps) {
  const getLabel = (str: string, required: boolean) => {
    return `${str} ${required ? '(' + t('required') + ')' : ''}`;
  };

  const renderInput = () => {
    switch (def.type) {
      case 'TITLE':
        return (
          <TextField
            fullWidth
            disabled
            label={getLabel(t('request_title'), config.required)}
            required={config.required}
          />
        );
      case 'DESCRIPTION':
        return (
          <TextField
            fullWidth
            multiline
            rows={3}
            disabled
            label={getLabel(t('description'), config.required)}
            required={config.required}
          />
        );
      case 'ASSET':
        if (config.selectionMode === 'specific') {
          return (
            <AssetLocationClause
              field={{
                name: 'asset',
                type: 'asset',
                value: config.asset || null,
                required: config.required,
                disabled: true
              }}
              onChange={onAssetSelect || (() => {})}
            />
          );
        }
        return (
          <TextField
            fullWidth
            disabled
            label={getLabel(t('asset'), config.required)}
            placeholder={t('select_asset')}
            required={config.required}
            InputProps={{
              endAdornment: (
                <BuildOutlinedIcon
                  fontSize="small"
                  sx={{ color: 'text.disabled' }}
                />
              )
            }}
          />
        );
      case 'LOCATION':
        if (config.selectionMode === 'specific') {
          return (
            <AssetLocationClause
              field={{
                name: 'location',
                type: 'location',
                value: config.location || null,
                required: config.required,
                disabled: true
              }}
              onChange={onLocationSelect || (() => {})}
            />
          );
        }
        return (
          <TextField
            fullWidth
            disabled
            label={getLabel(t('location'), config.required)}
            placeholder={t('select_location')}
            required={config.required}
            InputProps={{
              endAdornment: (
                <LocationOnOutlinedIcon
                  fontSize="small"
                  sx={{ color: 'text.disabled' }}
                />
              )
            }}
          />
        );
      case 'CONTACT':
        return (
          <TextField
            fullWidth
            disabled
            label={getLabel(t('contact'), config.required)}
            required={config.required}
            InputProps={{
              endAdornment: (
                <PersonOutlineIcon
                  fontSize="small"
                  sx={{ color: 'text.disabled' }}
                />
              )
            }}
          />
        );
      case 'IMAGE':
      case 'FILES': {
        const isImage = def.type === 'IMAGE';
        return (
          <FileUpload
            title={isImage ? t('image') : t('files')}
            type={isImage ? 'image' : 'file'}
            multiple={!isImage}
            description={''}
            onDrop={() => {}}
            disabled
          />
        );
      }
      default:
        return null;
    }
  };

  return <Box>{renderInput()}</Box>;
}

// ---------------------------------------------------------------------------
// Main RequestPortalPreview Component
// ---------------------------------------------------------------------------

export default function RequestPortalPreview({
  title,
  welcomeMessage,
  fieldConfigs,
  preview = false,
  onFieldChange,
  onLocationSelect,
  onAssetSelect
}: RequestPortalPreviewProps) {
  const theme = useTheme();
  const { t } = useTranslation();

  const formik = useFormik({
    initialValues: { title, welcomeMessage, fieldConfigs },
    onSubmit: () => {},
    enableReinitialize: true
  });

  const enabledConfigs = useMemo(
    () => fieldConfigs.filter((c) => c.enabled),
    [fieldConfigs]
  );

  return (
    <FormikProvider value={formik}>
      <Box>
        {/* Portal header card */}
        <Box
          sx={{
            p: 2.5,
            mb: 3,
            borderRadius: 1.5,
            bgcolor: alpha(theme.palette.primary.main, 0.05),
            borderLeft: '4px solid',
            borderColor: 'primary.main'
          }}
        >
          <Typography variant="h6" fontWeight={700} gutterBottom>
            {title || t('untitled_portal')}
          </Typography>
          {welcomeMessage ? (
            <Typography variant="body2" color="text.secondary">
              {welcomeMessage}
            </Typography>
          ) : (
            <Typography
              variant="body2"
              color="text.disabled"
              fontStyle="italic"
            >
              {t('no_welcome_message')}
            </Typography>
          )}
        </Box>

        {enabledConfigs.length === 0 ? (
          <Typography
            variant="body2"
            color="text.disabled"
            textAlign="center"
            py={4}
          >
            {t('no_fields_enabled')}
          </Typography>
        ) : (
          <Stack spacing={2}>
            {enabledConfigs.map((config, index) => {
              const def = FIELD_DEFS.find((d) => d.type === config.type)!;
              const originalIndex = fieldConfigs.findIndex(
                (c) => c.type === config.type
              );

              return (
                <PreviewFieldRender
                  key={config.type}
                  config={config}
                  def={def}
                  t={t}
                  onLocationSelect={(location) =>
                    onLocationSelect?.(originalIndex, location)
                  }
                  onAssetSelect={(asset) =>
                    onAssetSelect?.(originalIndex, asset)
                  }
                />
              );
            })}
          </Stack>
        )}

        {enabledConfigs.length > 0 && (
          <Button fullWidth variant="contained" disabled sx={{ mt: 1 }}>
            {t('submit_request')}
          </Button>
        )}
      </Box>
    </FormikProvider>
  );
}
