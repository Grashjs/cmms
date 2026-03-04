import {
  Box,
  Button,
  CircularProgress,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Grid,
  Tab,
  Tabs,
  TextField,
  Typography,
  Chip,
  FormControlLabel,
  Switch
} from '@mui/material';
import { ChangeEvent, useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { RequestPortal, RequestPortalField, PortalFieldType } from '../../../../../models/owns/requestPortal';
import { RequestPortalPostDTO } from '../../../../../models/owns/requestPortal';
import AddTwoToneIcon from '@mui/icons-material/AddTwoTone';
import DeleteTwoToneIcon from '@mui/icons-material/DeleteTwoTone';

interface RequestPortalModalProps {
  open: boolean;
  onClose: () => void;
  portal?: RequestPortal;
  onSubmit: (values: RequestPortalPostDTO, action: 'create' | 'edit') => Promise<void>;
}

const availableFieldTypes: PortalFieldType[] = [
  'ASSET',
  'DESCRIPTION',
  'CONTACT',
  'IMAGE',
  'FILES'
];

export default function RequestPortalModal({
  open,
  onClose,
  portal,
  onSubmit
}: RequestPortalModalProps) {
  const { t }: { t: any } = useTranslation();
  const [currentTab, setCurrentTab] = useState<string>('edit');
  const [submitting, setSubmitting] = useState<boolean>(false);

  const [title, setTitle] = useState<string>('');
  const [welcomeMessage, setWelcomeMessage] = useState<string>('');
  const [fields, setFields] = useState<RequestPortalField[]>([]);

  const handleTabsChange = (_event: ChangeEvent<{}>, value: string): void => {
    setCurrentTab(value);
  };

  useEffect(() => {
    if (portal) {
      setTitle(portal.title || '');
      setWelcomeMessage(portal.welcomeMessage || '');
      setFields(portal.fields || []);
    } else {
      setTitle('');
      setWelcomeMessage('');
      setFields([]);
    }
  }, [portal, open]);

  const addField = (type: PortalFieldType) => {
    const newField: RequestPortalField = {
      type,
      location: null,
      asset: null,
      required: false
    };
    setFields([...fields, newField]);
  };

  const removeField = (index: number) => {
    const newFields = fields.filter((_, i) => i !== index);
    setFields(newFields);
  };

  const updateField = (index: number, updates: Partial<RequestPortalField>) => {
    const newFields = fields.map((field, i) => {
      if (i === index) {
        return { ...field, ...updates };
      }
      return field;
    });
    setFields(newFields);
  };

  const handleSave = () => {
    if (!title.trim()) {
      return;
    }
    setSubmitting(true);
    const values: RequestPortalPostDTO = {
      title,
      welcomeMessage,
      fields
    };
    onSubmit(values, portal ? 'edit' : 'create')
      .finally(() => setSubmitting(false));
  };

  const tabs = [
    { value: 'edit', label: t('edit') },
    { value: 'preview', label: t('preview') }
  ];

  const renderFieldChip = (field: RequestPortalField, index: number) => (
    <Chip
      key={index}
      label={`${t(field.type.toLowerCase())} - ${field.required ? t('required') : t('optional')}`}
      onDelete={() => removeField(index)}
      sx={{ m: 0.5 }}
      color={field.required ? 'primary' : 'default'}
    />
  );

  return (
    <Dialog fullWidth maxWidth="sm" open={open} onClose={onClose}>
      <DialogTitle
        sx={{
          p: 3
        }}
      >
        <Typography variant="h4" gutterBottom>
          {portal ? t('edit_request_portal') : t('create_request_portal')}
        </Typography>
      </DialogTitle>
      <DialogContent
        dividers
        sx={{
          p: 3
        }}
      >
        <Box
          sx={{
            display: 'flex',
            justifyContent: 'space-between',
            flexDirection: 'row',
            alignItems: 'center',
            mb: 2
          }}
        >
          <Tabs
            sx={{ mb: 2 }}
            onChange={handleTabsChange}
            value={currentTab}
            variant="scrollable"
            scrollButtons="auto"
            textColor="primary"
            indicatorColor="primary"
          >
            {tabs.map((tab) => (
              <Tab key={tab.value} label={tab.label} value={tab.value} />
            ))}
          </Tabs>
        </Box>

        {currentTab === 'edit' && (
          <Box>
            <Grid container spacing={2}>
              <Grid item xs={12}>
                <TextField
                  fullWidth
                  variant="outlined"
                  label={t('title')}
                  value={title}
                  onChange={(event) => setTitle(event.target.value)}
                  error={title.trim() === ''}
                  helperText={t('required_title')}
                />
              </Grid>
              <Grid item xs={12}>
                <TextField
                  fullWidth
                  multiline
                  rows={3}
                  variant="outlined"
                  label={t('welcome_message')}
                  value={welcomeMessage}
                  onChange={(event) => setWelcomeMessage(event.target.value)}
                />
              </Grid>
              <Grid item xs={12}>
                <Typography variant="subtitle1" gutterBottom>
                  {t('fields')}
                </Typography>
                <Box sx={{ mb: 1 }}>
                  {fields.map((field, index) => renderFieldChip(field, index))}
                </Box>
                <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 1 }}>
                  {availableFieldTypes.map((type) => (
                    <Button
                      key={type}
                      size="small"
                      startIcon={<AddTwoToneIcon />}
                      onClick={() => addField(type)}
                      variant="outlined"
                    >
                      {t(type.toLowerCase())}
                    </Button>
                  ))}
                </Box>
              </Grid>
              {fields.length > 0 && (
                <Grid item xs={12}>
                  <Typography variant="subtitle2" gutterBottom>
                    {t('field_settings')}
                  </Typography>
                  {fields.map((field, index) => (
                    <Box
                      key={index}
                      sx={{
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'space-between',
                        mb: 1,
                        p: 1,
                        border: '1px solid',
                        borderColor: 'divider',
                        borderRadius: 1
                      }}
                    >
                      <Typography variant="body2">
                        {t(field.type.toLowerCase())}
                      </Typography>
                      <FormControlLabel
                        control={
                          <Switch
                            checked={field.required}
                            onChange={(e) =>
                              updateField(index, { required: e.target.checked })
                            }
                            size="small"
                          />
                        }
                        label={t('required')}
                      />
                    </Box>
                  ))}
                </Grid>
              )}
            </Grid>
          </Box>
        )}

        {currentTab === 'preview' && (
          <Box sx={{ p: 2 }}>
            <Typography variant="h5" gutterBottom>
              {title || t('untitled_portal')}
            </Typography>
            <Typography variant="body1" color="text.secondary" sx={{ mb: 3 }}>
              {welcomeMessage || t('no_welcome_message')}
            </Typography>
            <Typography variant="h6" gutterBottom>
              {t('fields')}
            </Typography>
            {fields.length === 0 ? (
              <Typography variant="body2" color="text.secondary">
                {t('no_fields_added')}
              </Typography>
            ) : (
              fields.map((field, index) => (
                <Box
                  key={index}
                  sx={{
                    p: 2,
                    mb: 1,
                    border: '1px solid',
                    borderColor: 'divider',
                    borderRadius: 1
                  }}
                >
                  <Box sx={{ display: 'flex', justifyContent: 'space-between' }}>
                    <Typography variant="body1">
                      {t(field.type.toLowerCase())}
                    </Typography>
                    <Chip
                      label={field.required ? t('required') : t('optional')}
                      size="small"
                      color={field.required ? 'primary' : 'default'}
                    />
                  </Box>
                </Box>
              ))
            )}
          </Box>
        )}
      </DialogContent>
      <DialogActions>
        <Button variant="outlined" onClick={onClose}>
          {t('cancel')}
        </Button>
        <Button
          disabled={
            !title.trim() ||
            submitting
          }
          startIcon={submitting ? <CircularProgress size="1rem" /> : null}
          onClick={handleSave}
          variant="contained"
        >
          {portal ? t('save') : t('create')}
        </Button>
      </DialogActions>
    </Dialog>
  );
}
