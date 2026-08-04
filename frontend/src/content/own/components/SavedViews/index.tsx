import { useContext, useEffect, useState } from 'react';
import {
  Box,
  Button,
  CircularProgress,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Divider,
  FormControlLabel,
  IconButton,
  ListItemIcon,
  ListItemText,
  Menu,
  MenuItem,
  Stack,
  Switch,
  TextField,
  Tooltip,
  Typography
} from '@mui/material';
import BookmarkBorderTwoToneIcon from '@mui/icons-material/BookmarkBorderTwoTone';
import BookmarkTwoToneIcon from '@mui/icons-material/BookmarkTwoTone';
import DeleteTwoToneIcon from '@mui/icons-material/DeleteTwoTone';
import EditTwoToneIcon from '@mui/icons-material/EditTwoTone';
import GroupTwoToneIcon from '@mui/icons-material/GroupTwoTone';
import SaveTwoToneIcon from '@mui/icons-material/SaveTwoTone';
import { useTranslation } from 'react-i18next';
import { useDispatch, useSelector } from '../../../../store';
import {
  createSavedView,
  deleteSavedView,
  getSavedViews,
  updateSavedView
} from '../../../../slices/savedView';
import {
  SavedView,
  SavedViewEntityType,
  TableLayout
} from '../../../../models/owns/savedView';
import { SearchCriteria } from '../../../../models/owns/page';
import { CustomSnackBarContext } from '../../../../contexts/CustomSnackBarContext';

interface SavedViewsProps {
  entityType: SavedViewEntityType;
  /** The filter set as it currently stands; stored when the user saves a view. */
  criteria: SearchCriteria;
  /** The table layout as it currently stands. */
  getLayout: () => TableLayout;
  /** Applies a view: the page decides how to merge filters and layout back in. */
  onApply: (view: SavedView) => void;
}

/**
 * View picker for a list page: pick a saved view, save the current one, rename, share, delete.
 *
 * Deliberately holds no filter or layout state of its own. It reads the current state through
 * the props above and hands a whole view back on apply, so the page stays the single owner of
 * its criteria — two components writing the same filter set is how a filter ends up applied
 * twice or not at all.
 */
export default function SavedViews({
  entityType,
  criteria,
  getLayout,
  onApply
}: SavedViewsProps) {
  const { t }: { t: any } = useTranslation();
  const dispatch = useDispatch();
  const { showSnackBar } = useContext(CustomSnackBarContext);
  const { viewsByEntity, loadingGet } = useSelector(
    (state) => state.savedViews
  );
  const views = viewsByEntity[entityType] ?? [];

  const [anchorEl, setAnchorEl] = useState<null | HTMLElement>(null);
  const [activeViewId, setActiveViewId] = useState<number | null>(null);
  const [saveDialogOpen, setSaveDialogOpen] = useState(false);
  const [name, setName] = useState('');
  const [shared, setShared] = useState(false);
  const [renamingView, setRenamingView] = useState<SavedView | null>(null);
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    dispatch(getSavedViews(entityType));
  }, [entityType]);

  const activeView = views.find((view) => view.id === activeViewId) ?? null;

  const closeMenu = () => setAnchorEl(null);

  const handleApply = (view: SavedView) => {
    setActiveViewId(view.id);
    onApply(view);
    closeMenu();
  };

  const openSaveDialog = (view: SavedView | null) => {
    setRenamingView(view);
    setName(view ? view.name : '');
    setShared(view ? view.shared : false);
    setSaveDialogOpen(true);
    closeMenu();
  };

  const handleSubmit = async () => {
    const trimmed = name.trim();
    if (!trimmed) return;
    setSubmitting(true);
    try {
      if (renamingView) {
        // Renaming keeps the stored filters and layout: the user opened the dialog from the
        // pencil, not from "save current view", so overwriting what they see would be a
        // surprise. Overwriting is the separate action below.
        await dispatch(
          updateSavedView(renamingView.id, { name: trimmed, shared })
        );
      } else {
        const created = await dispatch(
          createSavedView({
            name: trimmed,
            entityType,
            criteria,
            columnLayout: getLayout(),
            shared
          })
        );
        if (created && typeof created === 'object' && 'id' in created) {
          setActiveViewId((created as SavedView).id);
        }
      }
      setSaveDialogOpen(false);
      showSnackBar(t('saved_view_saved'), 'success');
    } catch {
      showSnackBar(t('saved_view_save_failed'), 'error');
    } finally {
      setSubmitting(false);
    }
  };

  const handleOverwrite = async (view: SavedView) => {
    try {
      await dispatch(
        updateSavedView(view.id, { criteria, columnLayout: getLayout() })
      );
      showSnackBar(t('saved_view_updated'), 'success');
    } catch {
      showSnackBar(t('saved_view_save_failed'), 'error');
    }
    closeMenu();
  };

  const handleDelete = async (view: SavedView) => {
    try {
      await dispatch(deleteSavedView(entityType, view.id));
      if (activeViewId === view.id) setActiveViewId(null);
      showSnackBar(t('saved_view_deleted'), 'success');
    } catch {
      showSnackBar(t('saved_view_delete_failed'), 'error');
    }
    closeMenu();
  };

  return (
    <>
      <Tooltip title={t('saved_views')}>
        <Button
          onClick={(event) => setAnchorEl(event.currentTarget)}
          variant={activeView ? 'contained' : 'outlined'}
          startIcon={
            activeView ? <BookmarkTwoToneIcon /> : <BookmarkBorderTwoToneIcon />
          }
          sx={{ whiteSpace: 'nowrap' }}
        >
          {activeView ? activeView.name : t('saved_views')}
        </Button>
      </Tooltip>
      <Menu
        anchorEl={anchorEl}
        open={Boolean(anchorEl)}
        onClose={closeMenu}
        PaperProps={{ sx: { minWidth: 280 } }}
      >
        {loadingGet && views.length === 0 && (
          <MenuItem disabled>
            <CircularProgress size="1rem" />
          </MenuItem>
        )}
        {!loadingGet && views.length === 0 && (
          <MenuItem disabled>
            <Typography variant="body2">{t('no_saved_views')}</Typography>
          </MenuItem>
        )}
        {views.map((view) => (
          <MenuItem
            key={view.id}
            selected={view.id === activeViewId}
            onClick={() => handleApply(view)}
          >
            <ListItemText
              primary={
                <Stack direction="row" spacing={1} alignItems="center">
                  <Typography>{view.name}</Typography>
                  {view.shared && (
                    <Tooltip title={t('saved_view_shared')}>
                      <GroupTwoToneIcon fontSize="small" color="disabled" />
                    </Tooltip>
                  )}
                </Stack>
              }
            />
            {view.editable && (
              <Stack direction="row" spacing={0.5} sx={{ ml: 1 }}>
                <Tooltip title={t('saved_view_overwrite')}>
                  <IconButton
                    size="small"
                    onClick={(event) => {
                      event.stopPropagation();
                      handleOverwrite(view);
                    }}
                  >
                    <SaveTwoToneIcon fontSize="small" />
                  </IconButton>
                </Tooltip>
                <Tooltip title={t('rename')}>
                  <IconButton
                    size="small"
                    onClick={(event) => {
                      event.stopPropagation();
                      openSaveDialog(view);
                    }}
                  >
                    <EditTwoToneIcon fontSize="small" />
                  </IconButton>
                </Tooltip>
                <Tooltip title={t('to_delete')}>
                  <IconButton
                    size="small"
                    onClick={(event) => {
                      event.stopPropagation();
                      handleDelete(view);
                    }}
                  >
                    <DeleteTwoToneIcon fontSize="small" />
                  </IconButton>
                </Tooltip>
              </Stack>
            )}
          </MenuItem>
        ))}
        <Divider />
        <MenuItem onClick={() => openSaveDialog(null)}>
          <ListItemIcon>
            <BookmarkTwoToneIcon fontSize="small" />
          </ListItemIcon>
          <ListItemText primary={t('save_current_view')} />
        </MenuItem>
      </Menu>

      <Dialog
        open={saveDialogOpen}
        onClose={() => setSaveDialogOpen(false)}
        fullWidth
        maxWidth="xs"
      >
        <DialogTitle>
          {renamingView ? t('rename') : t('save_current_view')}
        </DialogTitle>
        <DialogContent>
          <Box sx={{ pt: 1 }}>
            <TextField
              autoFocus
              fullWidth
              label={t('name')}
              value={name}
              onChange={(event) => setName(event.target.value)}
              onKeyDown={(event) => {
                if (event.key === 'Enter') handleSubmit();
              }}
            />
            <FormControlLabel
              sx={{ mt: 2 }}
              control={
                <Switch
                  checked={shared}
                  onChange={(event) => setShared(event.target.checked)}
                />
              }
              label={t('saved_view_share_with_company')}
            />
          </Box>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setSaveDialogOpen(false)}>{t('cancel')}</Button>
          <Button
            variant="contained"
            onClick={handleSubmit}
            disabled={submitting || !name.trim()}
          >
            {t('save')}
          </Button>
        </DialogActions>
      </Dialog>
    </>
  );
}
