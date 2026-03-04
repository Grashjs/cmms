import {
  Alert,
  Box,
  Button,
  CircularProgress,
  Collapse,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Divider,
  FormControlLabel,
  IconButton,
  Radio,
  RadioGroup,
  Stack,
  Switch,
  TextField,
  Tooltip,
  Typography,
  alpha,
  useTheme
} from '@mui/material';
import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import {
  PortalFieldType,
  RequestPortal,
  RequestPortalField,
  RequestPortalPostDTO
} from '../../../../../models/owns/requestPortal';

import AttachFileOutlinedIcon from '@mui/icons-material/AttachFileOutlined';
import BuildOutlinedIcon from '@mui/icons-material/BuildOutlined';
import DescriptionOutlinedIcon from '@mui/icons-material/DescriptionOutlined';
import EditOutlinedIcon from '@mui/icons-material/EditOutlined';
import ExpandLessIcon from '@mui/icons-material/ExpandLess';
import ExpandMoreIcon from '@mui/icons-material/ExpandMore';
import ImageOutlinedIcon from '@mui/icons-material/ImageOutlined';
import InfoOutlinedIcon from '@mui/icons-material/InfoOutlined';
import LocationOnOutlinedIcon from '@mui/icons-material/LocationOnOutlined';
import PersonOutlineIcon from '@mui/icons-material/PersonOutline';
import TitleOutlinedIcon from '@mui/icons-material/TitleOutlined';
import VisibilityOutlinedIcon from '@mui/icons-material/VisibilityOutlined';

// ---------------------------------------------------------------------------
// Types
// ---------------------------------------------------------------------------

interface RequestPortalModalProps {
  open: boolean;
  onClose: () => void;
  portal?: RequestPortal;
  onSubmit: (
    values: RequestPortalPostDTO,
    action: 'create' | 'edit'
  ) => Promise<void>;
}

type SelectionMode = 'all' | 'specific';

/** Extended field type to include LOCATION in the UI */
type AllFieldType = PortalFieldType | 'LOCATION' | 'TITLE';

interface FieldConfig {
  type: AllFieldType;
  enabled: boolean;
  required: boolean;
  selectionMode: SelectionMode;
  /** Whether the options collapse is open */
  optionsOpen: boolean;
}

// ---------------------------------------------------------------------------
// Field definitions — fixed ordered list
// ---------------------------------------------------------------------------

interface FieldDef {
  type: AllFieldType;
  icon: React.ReactNode;
  color: string;
  /** i18n key for display name */
  labelKey: string;
  /** Always enabled, cannot be toggled */
  alwaysEnabled?: boolean;
  /** Always required, cannot be toggled */
  alwaysRequired?: boolean;
  /** Show selection-mode radios */
  hasSelectionPanel?: boolean;
  /** Show the public-link warning */
  hasPublicWarning?: boolean;
}

const FIELD_DEFS: FieldDef[] = [
  {
    type: 'TITLE',
    icon: <TitleOutlinedIcon fontSize="small" />,
    color: '#374151',
    labelKey: 'request_title',
    alwaysEnabled: true,
    alwaysRequired: true
  },
  {
    type: 'LOCATION',
    icon: <LocationOnOutlinedIcon fontSize="small" />,
    color: '#0891b2',
    labelKey: 'location',
    hasSelectionPanel: true,
    hasPublicWarning: true
  },
  {
    type: 'ASSET',
    icon: <BuildOutlinedIcon fontSize="small" />,
    color: '#2563eb',
    labelKey: 'asset',
    hasSelectionPanel: true
  },
  {
    type: 'DESCRIPTION',
    icon: <DescriptionOutlinedIcon fontSize="small" />,
    color: '#7c3aed',
    labelKey: 'description'
  },
  {
    type: 'CONTACT',
    icon: <PersonOutlineIcon fontSize="small" />,
    color: '#059669',
    labelKey: 'contact'
  },
  {
    type: 'IMAGE',
    icon: <ImageOutlinedIcon fontSize="small" />,
    color: '#d97706',
    labelKey: 'images'
  },
  {
    type: 'FILES',
    icon: <AttachFileOutlinedIcon fontSize="small" />,
    color: '#dc2626',
    labelKey: 'files'
  }
];

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

const buildDefaultConfigs = (
  existingFields?: RequestPortalField[]
): FieldConfig[] =>
  FIELD_DEFS.map((def) => {
    const existing = existingFields?.find(
      (f) => (f.type as string) === def.type
    );
    return {
      type: def.type,
      enabled: def.alwaysEnabled ? true : !!existing,
      required: def.alwaysRequired ? true : existing?.required ?? false,
      selectionMode: 'all',
      optionsOpen: false
    };
  });

const configsToFields = (configs: FieldConfig[]): RequestPortalField[] =>
  configs
    .filter((c) => c.enabled && c.type !== 'TITLE')
    .map((c) => ({
      type: c.type as PortalFieldType,
      location: null,
      asset: null,
      required: c.required
    }));

// ---------------------------------------------------------------------------
// FieldRow
// ---------------------------------------------------------------------------

function FieldRow({
  def,
  config,
  onToggleEnabled,
  onToggleRequired,
  onToggleOptions,
  onSelectionModeChange,
  t
}: {
  def: FieldDef;
  config: FieldConfig;
  onToggleEnabled: () => void;
  onToggleRequired: () => void;
  onToggleOptions: () => void;
  onSelectionModeChange: (m: SelectionMode) => void;
  t: (k: string) => string;
}) {
  const theme = useTheme();
  const disabled = !config.enabled && !def.alwaysEnabled;
  const color = config.enabled ? def.color : theme.palette.text.disabled;

  return (
    <Box
      sx={{
        border: '1px solid',
        borderColor: config.enabled ? alpha(def.color, 0.25) : 'divider',
        borderRadius: 1.5,
        overflow: 'hidden',
        transition: 'border-color 0.2s, box-shadow 0.2s',
        ...(config.enabled && {
          boxShadow: `0 0 0 3px ${alpha(def.color, 0.07)}`
        })
      }}
    >
      {/* ── Main row ── */}
      <Box
        sx={{
          display: 'flex',
          alignItems: 'center',
          gap: 1.5,
          px: 2,
          py: 1.25,
          bgcolor: config.enabled ? alpha(def.color, 0.04) : 'transparent'
        }}
      >
        {/* Icon */}
        <Box
          sx={{
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            width: 32,
            height: 32,
            borderRadius: '50%',
            bgcolor: config.enabled
              ? alpha(def.color, 0.12)
              : alpha('#9ca3af', 0.1),
            color,
            flexShrink: 0,
            transition: 'all 0.2s'
          }}
        >
          {def.icon}
        </Box>

        {/* Label */}
        <Typography
          variant="body2"
          fontWeight={600}
          sx={{
            flexGrow: 1,
            color: config.enabled ? 'text.primary' : 'text.disabled',
            transition: 'color 0.2s'
          }}
        >
          {t(def.labelKey)}
        </Typography>

        {/* Required toggle — only visible when enabled */}
        {config.enabled && !def.alwaysRequired && (
          <FormControlLabel
            control={
              <Switch
                size="small"
                checked={config.required}
                onChange={onToggleRequired}
                sx={{
                  '& .MuiSwitch-switchBase.Mui-checked': { color: def.color },
                  '& .MuiSwitch-switchBase.Mui-checked + .MuiSwitch-track': {
                    bgcolor: def.color
                  }
                }}
              />
            }
            label={
              <Typography variant="caption" color="text.secondary">
                {t('required')}
              </Typography>
            }
            labelPlacement="start"
            sx={{ mr: 0, ml: 0 }}
          />
        )}

        {/* Always-required badge */}
        {def.alwaysRequired && (
          <Typography
            variant="caption"
            sx={{
              px: 1,
              py: 0.25,
              borderRadius: 1,
              bgcolor: alpha(def.color, 0.1),
              color: def.color,
              fontWeight: 600
            }}
          >
            {t('required')}
          </Typography>
        )}

        {/* Options expand (ASSET / LOCATION) */}
        {def.hasSelectionPanel && config.enabled && (
          <Tooltip
            title={config.optionsOpen ? t('hide_options') : t('show_options')}
          >
            <IconButton
              size="small"
              onClick={onToggleOptions}
              sx={{ color: def.color }}
            >
              {config.optionsOpen ? (
                <ExpandLessIcon fontSize="small" />
              ) : (
                <ExpandMoreIcon fontSize="small" />
              )}
            </IconButton>
          </Tooltip>
        )}

        {/* Enable/disable switch */}
        {!def.alwaysEnabled && (
          <Switch
            size="small"
            checked={config.enabled}
            onChange={onToggleEnabled}
            sx={{
              '& .MuiSwitch-switchBase.Mui-checked': { color: def.color },
              '& .MuiSwitch-switchBase.Mui-checked + .MuiSwitch-track': {
                bgcolor: def.color
              }
            }}
          />
        )}
      </Box>

      {/* ── Options panel ── */}
      {def.hasSelectionPanel && (
        <Collapse in={config.enabled && config.optionsOpen}>
          <Divider />
          <Box sx={{ px: 2.5, py: 1.5 }}>
            <RadioGroup
              value={config.selectionMode}
              onChange={(e) =>
                onSelectionModeChange(e.target.value as SelectionMode)
              }
            >
              <FormControlLabel
                value="all"
                control={<Radio size="small" />}
                label={
                  <Typography variant="body2" color="text.secondary">
                    {def.type === 'ASSET'
                      ? t('allow_selection_from_all_assets_at_general')
                      : t('allow_selection_from_all_locations_at_general')}
                  </Typography>
                }
                sx={{ mb: 0.5 }}
              />
              <FormControlLabel
                value="specific"
                control={<Radio size="small" />}
                label={
                  <Typography variant="body2" color="text.secondary">
                    {def.type === 'ASSET'
                      ? t('restrict_to_a_specific_asset')
                      : t('restrict_to_a_specific_location')}
                  </Typography>
                }
              />
            </RadioGroup>

            {/* Public-portal warning for LOCATION */}
            {def.hasPublicWarning && config.selectionMode === 'all' && (
              <Alert
                severity="info"
                icon={<InfoOutlinedIcon fontSize="small" />}
                sx={{ mt: 1.5, fontSize: '0.75rem', py: 0.5 }}
              >
                {t('portal_public_location_warning')}
              </Alert>
            )}
          </Box>
        </Collapse>
      )}
    </Box>
  );
}

// ---------------------------------------------------------------------------
// PreviewField
// ---------------------------------------------------------------------------

function PreviewField({
  config,
  def,
  t
}: {
  config: FieldConfig;
  def: FieldDef;
  t: (k: string) => string;
}) {
  const renderInput = () => {
    switch (def.type) {
      case 'TITLE':
        return (
          <TextField
            fullWidth
            disabled
            size="small"
            label={t('request_title')}
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
            size="small"
            label={t('description')}
            required={config.required}
          />
        );
      case 'ASSET':
        return (
          <TextField
            fullWidth
            disabled
            size="small"
            label={t('asset')}
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
        return (
          <TextField
            fullWidth
            disabled
            size="small"
            label={t('location')}
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
            size="small"
            label={t('contact')}
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
          <Box
            sx={{
              border: '2px dashed',
              borderColor: 'divider',
              borderRadius: 1.5,
              p: 2.5,
              textAlign: 'center',
              bgcolor: 'action.disabledBackground'
            }}
          >
            {isImage ? (
              <ImageOutlinedIcon sx={{ color: 'text.disabled', mb: 0.5 }} />
            ) : (
              <AttachFileOutlinedIcon
                sx={{ color: 'text.disabled', mb: 0.5 }}
              />
            )}
            <Typography variant="caption" color="text.disabled" display="block">
              {t(isImage ? 'upload_image' : 'upload_files')}
            </Typography>
          </Box>
        );
      }
      default:
        return null;
    }
  };

  return (
    <Box sx={{ mb: 2 }}>
      {renderInput()}
      {config.required && (
        <Typography
          variant="caption"
          color="error"
          sx={{ mt: 0.5, display: 'block' }}
        >
          * {t('required')}
        </Typography>
      )}
    </Box>
  );
}

// ---------------------------------------------------------------------------
// Main component
// ---------------------------------------------------------------------------

export default function RequestPortalModal({
  open,
  onClose,
  portal,
  onSubmit
}: RequestPortalModalProps) {
  const { t }: { t: any } = useTranslation();
  const theme = useTheme();

  const [activeTab, setActiveTab] = useState<'edit' | 'preview'>('edit');
  const [submitting, setSubmitting] = useState(false);
  const [title, setTitle] = useState('');
  const [welcomeMessage, setWelcomeMessage] = useState('');
  const [fieldConfigs, setFieldConfigs] = useState<FieldConfig[]>(() =>
    buildDefaultConfigs()
  );
  const [titleTouched, setTitleTouched] = useState(false);

  useEffect(() => {
    if (open) {
      setActiveTab('edit');
      setTitleTouched(false);
      setSubmitting(false);
      setTitle(portal?.title ?? '');
      setWelcomeMessage(portal?.welcomeMessage ?? '');
      setFieldConfigs(buildDefaultConfigs(portal?.fields));
    }
  }, [portal, open]);

  // ── Mutators ──────────────────────────────────────────────────────────────

  const updateConfig = (index: number, patch: Partial<FieldConfig>) =>
    setFieldConfigs((prev) =>
      prev.map((c, i) => (i === index ? { ...c, ...patch } : c))
    );

  const toggleEnabled = (index: number) => {
    const next = !fieldConfigs[index].enabled;
    updateConfig(index, {
      enabled: next,
      // close options panel when disabling
      optionsOpen: next ? fieldConfigs[index].optionsOpen : false
    });
  };

  // ── Save ──────────────────────────────────────────────────────────────────

  const handleSave = () => {
    setTitleTouched(true);
    if (!title.trim()) return;
    setSubmitting(true);
    onSubmit(
      { title, welcomeMessage, fields: configsToFields(fieldConfigs) },
      portal ? 'edit' : 'create'
    ).finally(() => setSubmitting(false));
  };

  // ── Preview: only enabled fields ─────────────────────────────────────────

  const enabledConfigs = fieldConfigs.filter((c) => c.enabled);

  // ── Render ────────────────────────────────────────────────────────────────

  return (
    <Dialog
      fullWidth
      maxWidth="sm"
      open={open}
      onClose={onClose}
      PaperProps={{ sx: { borderRadius: 2 } }}
    >
      {/* Title */}
      <DialogTitle sx={{ px: 3, pt: 3, pb: 1.5 }}>
        <Typography variant="h5" fontWeight={700}>
          {portal ? t('edit_request_portal') : t('create_request_portal')}
        </Typography>
      </DialogTitle>

      {/* Tab bar */}
      <Box
        sx={{
          display: 'flex',
          px: 3,
          borderBottom: '1px solid',
          borderColor: 'divider',
          gap: 0.5
        }}
      >
        {(['edit', 'preview'] as const).map((tab) => {
          const active = activeTab === tab;
          return (
            <Button
              key={tab}
              size="small"
              startIcon={
                tab === 'edit' ? (
                  <EditOutlinedIcon fontSize="small" />
                ) : (
                  <VisibilityOutlinedIcon fontSize="small" />
                )
              }
              onClick={() => setActiveTab(tab)}
              disableElevation
              disableRipple
              sx={{
                borderRadius: 0,
                borderBottom: '2px solid',
                borderColor: active ? 'primary.main' : 'transparent',
                color: active ? 'primary.main' : 'text.secondary',
                fontWeight: active ? 700 : 400,
                pb: 1.25,
                mb: '-1px',
                textTransform: 'none',
                '&:hover': { bgcolor: 'transparent', color: 'primary.main' }
              }}
            >
              {t(tab)}
            </Button>
          );
        })}
      </Box>

      <DialogContent sx={{ px: 3, py: 2.5 }}>
        {/* ═══════════════════════ EDIT TAB ═══════════════════════ */}
        {activeTab === 'edit' && (
          <Stack spacing={2.5}>
            {/* Portal meta */}
            <TextField
              fullWidth
              size="small"
              label={t('title')}
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              onBlur={() => setTitleTouched(true)}
              error={titleTouched && !title.trim()}
              helperText={
                titleTouched && !title.trim() ? t('required_title') : ''
              }
            />

            <TextField
              fullWidth
              multiline
              rows={2}
              size="small"
              label={t('welcome_message')}
              value={welcomeMessage}
              onChange={(e) => setWelcomeMessage(e.target.value)}
            />

            {/* Section header */}
            <Box>
              <Typography variant="subtitle1" fontWeight={700} gutterBottom>
                {t('configure_form_fields')}
              </Typography>
              <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
                {t('configure_form_fields_description')}
              </Typography>

              <Stack spacing={1.5}>
                {FIELD_DEFS.map((def, i) => (
                  <FieldRow
                    key={def.type}
                    def={def}
                    config={fieldConfigs[i]}
                    onToggleEnabled={() => toggleEnabled(i)}
                    onToggleRequired={() =>
                      updateConfig(i, { required: !fieldConfigs[i].required })
                    }
                    onToggleOptions={() =>
                      updateConfig(i, {
                        optionsOpen: !fieldConfigs[i].optionsOpen
                      })
                    }
                    onSelectionModeChange={(m) =>
                      updateConfig(i, { selectionMode: m })
                    }
                    t={t}
                  />
                ))}
              </Stack>
            </Box>
          </Stack>
        )}

        {/* ═══════════════════════ PREVIEW TAB ═══════════════════════ */}
        {activeTab === 'preview' && (
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
              enabledConfigs.map((config, i) => {
                const def = FIELD_DEFS.find((d) => d.type === config.type)!;
                return <PreviewField key={i} config={config} def={def} t={t} />;
              })
            )}

            {enabledConfigs.length > 0 && (
              <Button fullWidth variant="contained" disabled sx={{ mt: 1 }}>
                {t('submit_request')}
              </Button>
            )}
          </Box>
        )}
      </DialogContent>

      {/* Actions */}
      <DialogActions sx={{ px: 3, pb: 2.5, pt: 1, gap: 1 }}>
        <Button
          variant="outlined"
          onClick={onClose}
          sx={{ textTransform: 'none' }}
        >
          {t('cancel')}
        </Button>
        <Button
          variant="contained"
          onClick={handleSave}
          disabled={!title.trim() || submitting}
          startIcon={
            submitting ? <CircularProgress size="1rem" color="inherit" /> : null
          }
          sx={{ textTransform: 'none', minWidth: 100 }}
        >
          {portal ? t('save') : t('create')}
        </Button>
      </DialogActions>
    </Dialog>
  );
}
