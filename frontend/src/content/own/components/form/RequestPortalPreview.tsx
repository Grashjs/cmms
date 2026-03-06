import {
  Autocomplete,
  Box,
  CircularProgress,
  IconButton,
  InputAdornment,
  Stack,
  TextField,
  Typography,
  alpha,
  useTheme,
  Button
} from '@mui/material';
import { FormikProvider, useFormik } from 'formik';
import { useCallback, useEffect, useMemo, useState } from 'react';
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
import Location, { LocationMiniDTO } from '../../../../models/owns/location';
import Asset, { AssetMiniDTO } from '../../../../models/owns/asset';
import SelectLocationModal from './SelectLocationModal';
import SelectAssetModal from './SelectAssetModal';
import FileUpload from '../FileUpload';
import { useDispatch, useSelector } from '../../../../store';
import {
  getLocationsMini,
  getPublicLocationsMini
} from '../../../../slices/location';
import { getAssetsMini, getPublicAssetsMini } from '../../../../slices/asset';
import debounce from 'lodash.debounce';
import { boolean, number, string } from 'yup';
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

export interface RequestPortalFormValues {
  title: string;
  description?: string;
  contact?: string;
  location?: LocationMiniDTO | null;
  asset?: AssetMiniDTO | null;
  images?: File[];
  files?: File[];
}

export interface RequestPortalPreviewProps {
  fieldConfigs: PreviewFieldConfig[];
  preview?: boolean;
  onFieldChange?: (index: number, patch: Partial<PreviewFieldConfig>) => void;
  onLocationSelect?: (index: number, location: LocationMiniDTO | null) => void;
  onAssetSelect?: (index: number, asset: AssetMiniDTO | null) => void;
  onDescriptionChange?: (value: string) => void;
  onTitleChange?: (value: string) => void;
  onContactChange?: (value: string) => void;
  onImagesChange?: (files: File[]) => void;
  onFilesChange?: (files: File[]) => void;
  onSubmit?: () => Promise<void>;
  submitting?: boolean;
  errors?: Record<string, string>;
  portalUUID?: string;
  images?: File[];
  files?: File[];
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
      location: existing?.location
        ? (existing.location as unknown as LocationMiniDTO)
        : null,
      asset: existing?.asset
        ? (existing.asset as unknown as AssetMiniDTO)
        : null
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
      location: c.location ? (c.location as unknown as Location) : null,
      asset: c.asset ? (c.asset as unknown as Asset) : null,
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
  disabled?: boolean;
  portalUUID?: string;
  error?: string;
}

export function AssetLocationClause({
  field,
  onChange,
  locationId,
  excludedIds,
  disabled,
  portalUUID,
  error
}: AssetLocationClauseProps) {
  const { t } = useTranslation();
  const dispatch = useDispatch();
  const [modalOpen, setModalOpen] = useState(false);
  const [searchTerm, setSearchTerm] = useState('');
  const [open, setOpen] = useState(false);

  const isLocation = field.type === 'location';

  // Get data from Redux store
  const { locationsMini } = useSelector((state) => state.locations);
  const { assetsMini } = useSelector((state) => state.assets);

  // Fetch options when autocomplete opens
  const fetchOptions = useCallback(() => {
    if (isLocation) {
      dispatch(
        portalUUID ? getPublicLocationsMini(portalUUID) : getLocationsMini()
      );
    } else {
      dispatch(
        portalUUID
          ? getPublicAssetsMini(portalUUID, locationId || null)
          : getAssetsMini(locationId || null)
      );
    }
  }, [dispatch, isLocation, locationId, portalUUID]);

  // Filter options based on search term
  type Option = {
    label: string;
    value: number;
    dto: LocationMiniDTO | AssetMiniDTO;
  };
  const options: Option[] = useMemo(() => {
    const items = (isLocation ? locationsMini : assetsMini) as (
      | LocationMiniDTO
      | AssetMiniDTO
    )[];

    return items
      .filter((item) => item.id !== excludedIds?.[0])
      .map((item) => ({
        label: item.name,
        value: item.id,
        dto: item
      }))
      .filter((option) =>
        option.label.toLowerCase().includes(searchTerm.toLowerCase())
      );
  }, [isLocation, locationsMini, assetsMini, searchTerm, excludedIds]);
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
        open={open}
        onOpen={() => {
          setOpen(true);
          fetchOptions();
        }}
        disabled={disabled}
        onClose={() => {
          setOpen(false);
        }}
        value={valueOption || undefined}
        onChange={(_, newValue: Option) => {
          onChange(newValue ? newValue.dto : null);
        }}
        options={options}
        getOptionLabel={(option: Option) => option.label}
        isOptionEqualToValue={(option, value) =>
          !value || option.value === value.value
        }
        loading={
          isLocation ? locationsMini.length === 0 : assetsMini.length === 0
        }
        noOptionsText={searchTerm ? 'No results found' : 'Type to search...'}
        renderInput={(params) => (
          <TextField
            {...params}
            variant="outlined"
            required={field.required}
            label={isLocation ? t('location') : t('asset')}
            placeholder={isLocation ? t('select_location') : t('select_asset')}
            error={!!error}
            helperText={error}
            InputProps={{
              ...params.InputProps,
              endAdornment: (
                <>
                  <InputAdornment position="end">
                    <IconButton
                      style={{ marginRight: 10 }}
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
  onDescriptionChange?: (value: string) => void;
  onTitleChange?: (value: string) => void;
  onContactChange?: (value: string) => void;
  onImagesChange?: (files: File[]) => void;
  onFilesChange?: (files: File[]) => void;
  disabled?: boolean;
  error?: string;
  portalUUID?: string;
  images?: File[];
  files?: File[];
}

function PreviewFieldRender({
  config,
  def,
  t,
  onLocationSelect,
  onAssetSelect,
  onDescriptionChange,
  onContactChange,
  onImagesChange,
  onFilesChange,
  disabled,
  error,
  onTitleChange,
  portalUUID,
  images,
  files
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
            disabled={disabled}
            label={getLabel(t('request_title'), config.required)}
            required={config.required}
            error={!!error}
            helperText={error}
            onChange={(e) => onTitleChange?.(e.target.value)}
          />
        );
      case 'DESCRIPTION':
        return (
          <TextField
            fullWidth
            multiline
            rows={3}
            disabled={disabled}
            label={getLabel(t('description'), config.required)}
            required={config.required}
            onChange={(e) => onDescriptionChange?.(e.target.value)}
            error={!!error}
            helperText={error}
          />
        );
      case 'ASSET':
        if (config.selectionMode === 'all' || !config.asset) {
          return (
            <AssetLocationClause
              field={{
                name: 'asset',
                type: 'asset',
                value: undefined,
                required: config.required,
                disabled: true
              }}
              onChange={onAssetSelect || (() => {})}
              disabled={disabled}
              portalUUID={portalUUID}
              error={error}
            />
          );
        } else return null;
      case 'LOCATION':
        if (config.selectionMode === 'all' || !config.location) {
          return (
            <AssetLocationClause
              field={{
                name: 'location',
                type: 'location',
                value: undefined,
                required: config.required,
                disabled: true
              }}
              onChange={onLocationSelect || (() => {})}
              disabled={disabled}
              portalUUID={portalUUID}
              error={error}
            />
          );
        } else return null;
      case 'CONTACT':
        return (
          <TextField
            fullWidth
            disabled={disabled}
            label={getLabel(t('contact'), config.required)}
            required={config.required}
            onChange={(e) => onContactChange?.(e.target.value)}
            error={!!error}
            helperText={error}
          />
        );
      case 'IMAGE':
      case 'FILES': {
        const isImage = def.type === 'IMAGE';
        const fileList = isImage ? images : files;
        return (
          <FileUpload
            title={isImage ? t('image') : t('files')}
            type={isImage ? 'image' : 'file'}
            multiple={!isImage}
            description={''}
            onDrop={(newFiles) => {
              if (isImage) {
                onImagesChange?.(newFiles as File[]);
              } else {
                onFilesChange?.(newFiles as File[]);
              }
            }}
            disabled={disabled}
            error={error}
            files={fileList}
          />
        );
      }
      default:
        return null;
    }
  };

  return renderInput();
}

// ---------------------------------------------------------------------------
// Main RequestPortalPreview Component
// ---------------------------------------------------------------------------

export default function RequestPortalPreview({
  fieldConfigs,
  preview = false,
  onFieldChange,
  onLocationSelect,
  onAssetSelect,
  onDescriptionChange,
  onContactChange,
  onImagesChange,
  onFilesChange,
  onSubmit,
  submitting,
  errors,
  onTitleChange,
  portalUUID,
  images,
  files
}: RequestPortalPreviewProps) {
  const theme = useTheme();
  const { t } = useTranslation();

  const formik = useFormik({
    initialValues: { fieldConfigs },
    onSubmit: async () => {
      await onSubmit?.();
    },
    enableReinitialize: true
  });

  const enabledConfigs = useMemo(
    () => fieldConfigs.filter((c) => c.enabled),
    [fieldConfigs]
  );

  return (
    <FormikProvider value={formik}>
      <Box>
        <Stack spacing={2}>
          {enabledConfigs.map((config, index) => {
            const def = FIELD_DEFS.find((d) => d.type === config.type)!;
            const originalIndex = fieldConfigs.findIndex(
              (c) => c.type === config.type
            );

            // Get error for this field type
            const errorKey = config.type.toLowerCase();
            const fieldError = errors?.[errorKey];

            return (
              <PreviewFieldRender
                key={config.type}
                config={config}
                def={def}
                t={t}
                onLocationSelect={(location) =>
                  onLocationSelect?.(originalIndex, location)
                }
                onAssetSelect={(asset) => onAssetSelect?.(originalIndex, asset)}
                onTitleChange={onTitleChange}
                onDescriptionChange={onDescriptionChange}
                onContactChange={onContactChange}
                onImagesChange={onImagesChange}
                onFilesChange={onFilesChange}
                disabled={preview}
                error={fieldError}
                portalUUID={portalUUID}
                images={images}
                files={files}
              />
            );
          })}
        </Stack>

        {enabledConfigs.length > 0 && (
          <Button
            fullWidth
            variant="contained"
            disabled={preview || submitting}
            onClick={() => formik.handleSubmit()}
            sx={{ mt: 1 }}
          >
            {submitting ? <CircularProgress size={24} /> : t('submit_request')}
          </Button>
        )}
      </Box>
    </FormikProvider>
  );
}
