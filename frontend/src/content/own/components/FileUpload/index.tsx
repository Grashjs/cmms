import {
  alpha,
  Alert,
  Avatar,
  Box,
  Divider,
  FormHelperText,
  List,
  ListItem,
  ListItemText,
  styled,
  Typography,
  useTheme,
  Zoom,
  IconButton
} from '@mui/material';
import { useTranslation } from 'react-i18next';
import WarningTwoToneIcon from '@mui/icons-material/WarningTwoTone';
import { useDropzone } from 'react-dropzone';
import CloudUploadTwoToneIcon from '@mui/icons-material/CloudUploadTwoTone';
import CloseTwoToneIcon from '@mui/icons-material/CloseTwoTone';
import CheckTwoToneIcon from '@mui/icons-material/CheckTwoTone';
import DeleteTwoToneIcon from '@mui/icons-material/DeleteTwoTone';
import { useSnackbar } from 'notistack';

const WarningTwoToneIconWrapper = styled(WarningTwoToneIcon)(
  ({ theme }) => `
    color: ${theme.colors.warning.main};
`
);

const BoxUploadWrapper = styled(Box)(
  ({ theme }) => `
    border-radius: ${theme.general.borderRadius};
    padding: ${theme.spacing(3, 2)};
    margin-top: ${theme.spacing(1.5)};
    background: ${alpha(theme.colors.primary.main, 0.03)};
    border: 1.5px dashed ${alpha(theme.colors.primary.main, 0.35)};
    outline: none;
    cursor: pointer;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    text-align: center;
    transition: ${theme.transitions.create(['border-color', 'background'])};

    &:hover,
    &:focus-visible {
      background: ${alpha(theme.colors.primary.main, 0.07)};
      border-color: ${theme.colors.primary.main};
    }
`
);

const UploadBox = styled(Box)(
  ({ theme }) => `
    border-radius: ${theme.general.borderRadius};
    padding: ${theme.spacing(2)};
    background: ${theme.colors.alpha.white[100]};
    border: 1px solid ${theme.palette.divider};
`
);

const TypographyPrimary = styled(Typography)(
  ({ theme }) => `
    color: ${theme.palette.text.primary};
  `
);

const TypographySecondary = styled(Typography)(
  ({ theme }) => `
    color: ${theme.palette.text.secondary};
  `
);

const AvatarWrapper = styled(Avatar)(
  ({ theme }) => `
    background: ${alpha(theme.colors.primary.main, 0.1)};
    color: ${theme.colors.primary.main};
    width: ${theme.spacing(6)};
    height: ${theme.spacing(6)};
`
);

const AvatarSuccess = styled(Avatar)(
  ({ theme }) => `
    background: ${theme.colors.success.main};
    width: ${theme.spacing(6)};
    height: ${theme.spacing(6)};
`
);

const AvatarDanger = styled(Avatar)(
  ({ theme }) => `
    background: ${theme.colors.error.main};
    width: ${theme.spacing(6)};
    height: ${theme.spacing(6)};
`
);

interface FileUploadProps {
  title: string;
  type: 'image' | 'file' | 'spreadsheet';
  multiple: boolean;
  description: string;
  onDrop: (files: any) => void;
  files?: any[];
  disabled?: boolean;
  error?: string;
}
function FileUpload(props: FileUploadProps) {
  const { t }: { t: any } = useTranslation();
  const theme = useTheme();
  const { enqueueSnackbar } = useSnackbar();
  const {
    title,
    description,
    onDrop: setFieldValue,
    type,
    multiple,
    files: defaultFiles,
    error,
    disabled
  } = props;
  const {
    acceptedFiles,
    isDragActive,
    isDragAccept,
    isDragReject,
    getRootProps,
    getInputProps,
    inputRef
  } = useDropzone({
    disabled: props.disabled,
    accept:
      type === 'image'
        ? {
            'image/*': []
          }
        : type === 'spreadsheet'
        ? {
            'text/csv': ['.csv', '.xls', '.xlsx', '.tsv']
          }
        : {},
    maxFiles: multiple ? 10 : 1,
    onDrop: (newFiles) => {
      if (multiple) {
        setFieldValue([...(defaultFiles || []), ...newFiles]);
      } else {
        setFieldValue(newFiles);
      }
    },
    onDropRejected: (fileRejections) =>
      enqueueSnackbar(
        fileRejections[0].errors.map((error) => error.message),
        {
          variant: 'error',
          anchorOrigin: {
            vertical: 'bottom',
            horizontal: 'center'
          },
          TransitionComponent: Zoom
        }
      )
  });

  const handleRemove = (index: number) => {
    const newFiles = [...(defaultFiles || [])];
    newFiles.splice(index, 1);
    setFieldValue(newFiles);
    // This is a bit of a hack to clear useDropzone internal state if we're using it
    if (inputRef.current) {
      inputRef.current.value = '';
    }
  };

  const filesToDisplay = defaultFiles || [];

  const files = filesToDisplay.map((file, index) => (
    <ListItem
      disableGutters
      component="div"
      key={index}
      sx={{ color: theme.palette.text.primary }}
    >
      <ListItemText primary={file.name} />
      <IconButton
        edge="end"
        aria-label="delete"
        onClick={(e) => {
          e.stopPropagation();
          handleRemove(index);
        }}
        sx={{ color: theme.colors.error.main, ml: 1 }}
      >
        <DeleteTwoToneIcon fontSize="small" />
      </IconButton>
      <Divider />
    </ListItem>
  ));

  return (
    <UploadBox>
      <TypographyPrimary variant="h4" gutterBottom>
        {title || t('file')}
      </TypographyPrimary>
      <TypographySecondary variant="body1">
        {description || t('drag_one_file')}
      </TypographySecondary>

      <BoxUploadWrapper {...getRootProps()} sx={{ borderColor: error ? theme.colors.error.main : undefined }}>
        <input {...getInputProps()} />
        {isDragAccept && (
          <>
            <AvatarSuccess variant="rounded">
              <CheckTwoToneIcon />
            </AvatarSuccess>
            <TypographyPrimary
              sx={{
                mt: 2
              }}
            >
              {t('drop_to_start')}
            </TypographyPrimary>
          </>
        )}
        {isDragReject && (
          <>
            <AvatarDanger variant="rounded">
              <CloseTwoToneIcon />
            </AvatarDanger>
            <TypographyPrimary
              sx={{
                mt: 2
              }}
            >
              {t('invalid_files_type')}
            </TypographyPrimary>
          </>
        )}
        {!isDragActive && (
          <>
            <AvatarWrapper variant="rounded">
              <CloudUploadTwoToneIcon />
            </AvatarWrapper>
            <TypographyPrimary
              fontWeight={600}
              sx={{
                mt: 1.5
              }}
            >
              {multiple ? t('drag_many_files') : t('drag_one_file')}
            </TypographyPrimary>
          </>
        )}
      </BoxUploadWrapper>
      {error && (
        <FormHelperText error sx={{ mx: 2, mt: 1 }}>
          {error}
        </FormHelperText>
      )}
      {files.length > 0 && (
        <>
          {multiple && (
            <Alert
              sx={{
                py: 0,
                mt: 2
              }}
              severity="success"
            >
              {t('you_have_uploaded')} <b>{files.length}</b> {t('files')}!
            </Alert>
          )}
          <Divider
            sx={{
              mt: 2
            }}
          />
          <List disablePadding component="div">
            {files}
          </List>
        </>
      )}
    </UploadBox>
  );
}

export default FileUpload;
