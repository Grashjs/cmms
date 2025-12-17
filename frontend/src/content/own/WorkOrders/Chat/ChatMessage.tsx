import {
  Avatar,
  Box,
  IconButton,
  Paper,
  Typography,
  Chip,
  Tooltip,
  Menu,
  MenuItem,
} from '@mui/material';
import { useState } from 'react';
import { ChatMessage as ChatMessageType } from '../../../../hooks/useWorkOrderChat';
import { formatDistanceToNow } from 'date-fns';
import ThumbUpIcon from '@mui/icons-material/ThumbUp';
import FavoriteIcon from '@mui/icons-material/Favorite';
import CheckCircleIcon from '@mui/icons-material/CheckCircle';
import WarningIcon from '@mui/icons-material/Warning';
import MoreVertIcon from '@mui/icons-material/MoreVert';
import EditIcon from '@mui/icons-material/Edit';
import DeleteIcon from '@mui/icons-material/Delete';
import MicIcon from '@mui/icons-material/Mic';
import ImageIcon from '@mui/icons-material/Image';
import VideoLibraryIcon from '@mui/icons-material/VideoLibrary';
import DescriptionIcon from '@mui/icons-material/Description';

interface ChatMessageProps {
  message: ChatMessageType;
  currentUserId: number;
  onReaction: (messageId: number, reaction: string) => void;
  onEdit?: (messageId: number) => void;
  onDelete?: (messageId: number) => void;
}

const reactionEmojis = [
  { emoji: '👍', icon: ThumbUpIcon },
  { emoji: '❤️', icon: FavoriteIcon },
  { emoji: '✅', icon: CheckCircleIcon },
  { emoji: '⚠️', icon: WarningIcon },
];

export default function ChatMessage({
  message,
  currentUserId,
  onReaction,
  onEdit,
  onDelete,
}: ChatMessageProps) {
  const [anchorEl, setAnchorEl] = useState<null | HTMLElement>(null);
  const isOwnMessage = message.user?.id === currentUserId;
  const isSystemMessage = message.messageType === 'SYSTEM';

  const handleMenuOpen = (event: React.MouseEvent<HTMLElement>) => {
    setAnchorEl(event.currentTarget);
  };

  const handleMenuClose = () => {
    setAnchorEl(null);
  };

  const handleEdit = () => {
    if (onEdit) onEdit(message.id);
    handleMenuClose();
  };

  const handleDelete = () => {
    if (onDelete) onDelete(message.id);
    handleMenuClose();
  };

  const getMessageIcon = () => {
    switch (message.messageType) {
      case 'VOICE':
        return <MicIcon fontSize="small" />;
      case 'IMAGE':
        return <ImageIcon fontSize="small" />;
      case 'VIDEO':
        return <VideoLibraryIcon fontSize="small" />;
      case 'DOCUMENT':
        return <DescriptionIcon fontSize="small" />;
      default:
        return null;
    }
  };

  if (isSystemMessage) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', my: 2 }}>
        <Chip
          label={message.content}
          size="small"
          sx={{ bgcolor: 'action.hover' }}
        />
      </Box>
    );
  }

  if (message.deleted) {
    return (
      <Box
        sx={{
          display: 'flex',
          justifyContent: isOwnMessage ? 'flex-end' : 'flex-start',
          mb: 2,
        }}
      >
        <Paper
          sx={{
            p: 1.5,
            maxWidth: '70%',
            bgcolor: 'action.disabledBackground',
            fontStyle: 'italic',
          }}
        >
          <Typography variant="body2" color="text.secondary">
            Message deleted
          </Typography>
        </Paper>
      </Box>
    );
  }

  return (
    <Box
      sx={{
        display: 'flex',
        justifyContent: isOwnMessage ? 'flex-end' : 'flex-start',
        mb: 2,
      }}
    >
      {!isOwnMessage && (
        <Avatar
          sx={{ mr: 1, width: 32, height: 32 }}
          src={`/api/users/${message.user.id}/avatar`}
        >
          {message.user.firstName[0]}
        </Avatar>
      )}

      <Box sx={{ maxWidth: '70%' }}>
        {!isOwnMessage && (
          <Typography variant="caption" color="text.secondary" sx={{ ml: 1 }}>
            {message.user.firstName} {message.user.lastName}
          </Typography>
        )}

        <Paper
          sx={{
            p: 1.5,
            bgcolor: isOwnMessage ? 'primary.main' : 'background.paper',
            color: isOwnMessage ? 'primary.contrastText' : 'text.primary',
            position: 'relative',
          }}
        >
          <Box sx={{ display: 'flex', alignItems: 'flex-start', gap: 1 }}>
            <Box sx={{ flex: 1 }}>
              {getMessageIcon() && (
                <Box sx={{ mb: 1 }}>{getMessageIcon()}</Box>
              )}

              {message.content && (
                <Typography variant="body2" sx={{ whiteSpace: 'pre-wrap' }}>
                  {message.content}
                  {message.edited && (
                    <Typography
                      component="span"
                      variant="caption"
                      sx={{ ml: 1, opacity: 0.7 }}
                    >
                      (edited)
                    </Typography>
                  )}
                </Typography>
              )}

              {message.file && (
                <Box
                  sx={{
                    mt: 1,
                    p: 1,
                    bgcolor: 'action.hover',
                    borderRadius: 1,
                    cursor: 'pointer',
                  }}
                  onClick={() => window.open(message.file!.path, '_blank')}
                >
                  <Typography variant="caption">{message.file.name}</Typography>
                </Box>
              )}
            </Box>

            {isOwnMessage && (
              <IconButton size="small" onClick={handleMenuOpen}>
                <MoreVertIcon fontSize="small" />
              </IconButton>
            )}
          </Box>

          <Box
            sx={{
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'space-between',
              mt: 0.5,
            }}
          >
            <Typography variant="caption" sx={{ opacity: 0.7 }}>
              {formatDistanceToNow(new Date(message.createdAt), {
                addSuffix: true,
              })}
            </Typography>

            {message.readByCurrentUser && isOwnMessage && (
              <Tooltip
                title={`Read by ${message.readBy.length} user${
                  message.readBy.length !== 1 ? 's' : ''
                }`}
              >
                <CheckCircleIcon sx={{ fontSize: 14, ml: 1 }} />
              </Tooltip>
            )}
          </Box>

          {/* Reactions */}
          {message.reactions && message.reactions.length > 0 && (
            <Box sx={{ display: 'flex', gap: 0.5, mt: 1, flexWrap: 'wrap' }}>
              {message.reactions.map((reaction) => (
                <Chip
                  key={reaction.reaction}
                  label={`${reaction.reaction} ${reaction.count}`}
                  size="small"
                  onClick={() => onReaction(message.id, reaction.reaction)}
                  color={reaction.currentUserReacted ? 'primary' : 'default'}
                  sx={{ height: 24 }}
                />
              ))}
            </Box>
          )}

          {/* Quick reactions */}
          <Box
            sx={{
              display: 'flex',
              gap: 0.5,
              mt: 1,
              opacity: 0.7,
              '&:hover': { opacity: 1 },
            }}
          >
            {reactionEmojis.map(({ emoji }) => (
              <IconButton
                key={emoji}
                size="small"
                onClick={() => onReaction(message.id, emoji)}
                sx={{ fontSize: 16 }}
              >
                {emoji}
              </IconButton>
            ))}
          </Box>
        </Paper>
      </Box>

      {/* Context menu for own messages */}
      <Menu anchorEl={anchorEl} open={Boolean(anchorEl)} onClose={handleMenuClose}>
        {onEdit && (
          <MenuItem onClick={handleEdit}>
            <EditIcon fontSize="small" sx={{ mr: 1 }} />
            Edit
          </MenuItem>
        )}
        {onDelete && (
          <MenuItem onClick={handleDelete}>
            <DeleteIcon fontSize="small" sx={{ mr: 1 }} />
            Delete
          </MenuItem>
        )}
      </Menu>
    </Box>
  );
}
