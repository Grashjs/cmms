import { attachmentUrl } from 'src/utils/attachment';
import File from '../../../models/owns/file';
import { IconButton, Link, List, ListItem, ListItemText } from '@mui/material';
import DoDisturbOnTwoToneIcon from '@mui/icons-material/DoDisturbOnTwoTone';
import { useContext } from 'react';
import { CompanySettingsContext } from '../../../contexts/CompanySettingsContext';

interface FilesListProps {
  files: File[];
  onRemove: (id: number) => void;
  confirmMessage: string;
  removeDisabled: boolean;
}
export default function FilesList({
  files,
  onRemove,
  confirmMessage,
  removeDisabled
}: FilesListProps) {
  const { getFormattedDate } = useContext(CompanySettingsContext);

  return (
    <List>
      {files.map((file) => (
        <ListItem
          key={file.id}
          secondaryAction={
            removeDisabled ? null : (
              <IconButton
                edge="end"
                aria-label="delete"
                onClick={() => {
                  if (window.confirm(confirmMessage)) {
                    onRemove(file.id);
                  }
                }}
              >
                <DoDisturbOnTwoToneIcon color="error" />
              </IconButton>
            )
          }
        >
          <ListItemText
            primary={
              <Link
                href={attachmentUrl(file.url)}
                target="_blank"
                rel="noopener noreferrer"
                variant="h6"
              >
                {file.name}
              </Link>
            }
            secondary={getFormattedDate(file.createdAt)}
          />
        </ListItem>
      ))}
    </List>
  );
}
