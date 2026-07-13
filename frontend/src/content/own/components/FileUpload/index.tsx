import {
  Alert,
  Avatar,
  Box,
  Button,
  Divider,
  List,
  ListItem,
  ListItemText,
  styled,
  Typography,
  useTheme,
  Zoom
} from '@mui/material';
import { useTranslation } from 'react-i18next';
import WarningTwoToneIcon from '@mui/icons-material/WarningTwoTone';
import { useDropzone } from 'react-dropzone';
import CloudUploadTwoToneIcon from '@mui/icons-material/CloudUploadTwoTone';
import CloseTwoToneIcon from '@mui/icons-material/CloseTwoTone';
import CheckTwoToneIcon from '@mui/icons-material/CheckTwoTone';
import { useSnackbar } from 'notistack';

const WarningTwoToneIconWrapper = styled(WarningTwoToneIcon)(
  ({ theme }) => `
    color: ${theme.colors.warning.main};
`
);

const ButtonContrast = styled(Button)(
  ({ theme }) => `
    background: ${theme.colors.alpha.trueWhite[10]};
    color: ${theme.colors.alpha.trueWhite[100]};

    &:hover {
      background: ${theme.colors.alpha.trueWhite[30]};
    }
`
);

const BoxUploadWrapper = styled(Box)(
  ({ theme }) => `
    border-radius: ${theme.general.borderRadius};
    padding: ${theme.spacing(2)};
    margin-top: ${theme.spacing(2)};
    background: ${theme.colors.alpha.trueWhite[10]};
    border: 1px dashed ${theme.colors.alpha.trueWhite[30]};
    outline: none;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    transition: ${theme.transitions.create(['border', 'background'])};

    &:hover {
      background: ${theme.colors.alpha.trueWhite[5]};
      border-color: ${theme.colors.alpha.trueWhite[70]};
    }
`
);

const UploadBox = styled(Box)(
  ({ theme }) => `
    border-radius: ${theme.general.borderRadius};
    padding: ${theme.spacing(2)};
    background: ${theme.colors.alpha.black[100]};
`
);

const TypographyPrimary = styled(Typography)(
  ({ theme }) => `
    color: ${theme.colors.alpha.trueWhite[100]};
  `
);

const TypographySecondary = styled(Typography)(
  ({ theme }) => `
    color: ${theme.colors.alpha.trueWhite[70]};
  `
);

const DividerContrast = styled(Divider)(
  ({ theme }) => `
    background: ${theme.colors.alpha.trueWhite[10]};
  `
);

const AvatarWrapper = styled(Avatar)(
  ({ theme }) => `
    background: ${theme.colors.alpha.trueWhite[10]};
    width: ${theme.spacing(7)};
    height: ${theme.spacing(7)};
`
);

const AvatarSuccess = styled(Avatar)(
  ({ theme }) => `
    background: ${theme.colors.success.light};
    width: ${theme.spacing(7)};
    height: ${theme.spacing(7)};
`
);

const AvatarDanger = styled(Avatar)(
  ({ theme }) => `
    background: ${theme.colors.error.light};
    width: ${theme.spacing(7)};
    height: ${theme.spacing(7)};
`
);

interface FileUploadProps {
  title: string;
  type: 'image' | 'file' | 'spreadsheet';
  multiple: boolean;
  description: string;
  onDrop: (files: any) => void;
}
function FileUpload(props: FileUploadProps) {
  const { t }: { t: any } = useTranslation();
  const theme = useTheme();
  const { enqueueSnackbar } = useSnackbar();
  const { title, description, onDrop: setFieldValue, type, multiple } = props;
  const {
    acceptedFiles,
    isDragActive,
    isDragAccept,
    isDragReject,
    getRootProps,
    getInputProps
  } = useDropzone({
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
    onDrop: (acceptedFiles) => {
      setFieldValue(acceptedFiles);
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

  const files = acceptedFiles.map((file, index) => (
    <ListItem
      disableGutters
      component="div"
      key={index}
      sx={{ color: theme.colors.alpha.trueWhite[100] }}
    >
      <ListItemText primary={file.name} />
      <b>{file.size} bytes</b>
      <DividerContrast />
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

      <BoxUploadWrapper {...getRootProps()}>
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
              sx={{
                mt: 2
              }}
            >
              {multiple ? t('drag_many_files') : t('drag_one_file')}
            </TypographyPrimary>
          </>
        )}
      </BoxUploadWrapper>
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
          <DividerContrast
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
