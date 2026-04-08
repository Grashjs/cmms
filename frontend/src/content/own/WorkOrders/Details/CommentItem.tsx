import {
  Box,
  Typography,
  IconButton,
  Avatar,
  Stack,
  styled,
  TextField,
  Button,
  Link
} from '@mui/material';
import Comment from '../../../../models/owns/comment';
import { useContext, useState } from 'react';
import { useTranslation } from 'react-i18next';
import EditTwoToneIcon from '@mui/icons-material/EditTwoTone';
import DeleteTwoToneIcon from '@mui/icons-material/DeleteTwoTone';
import SaveTwoToneIcon from '@mui/icons-material/SaveTwoTone';
import CancelTwoToneIcon from '@mui/icons-material/CancelTwoTone';
import { CompanySettingsContext } from '../../../../contexts/CompanySettingsContext';
import { useDispatch, useSelector } from '../../../../store';
import { deleteComment, updateComment } from '../../../../slices/comment';
import useAuth from '../../../../hooks/useAuth';
import File from '../../../../models/owns/file';
import PictureAsPdfTwoToneIcon from '@mui/icons-material/PictureAsPdfTwoTone';
import ArchiveTwoToneIcon from '@mui/icons-material/ArchiveTwoTone';
import ImageViewer from 'react-simple-image-viewer';
import mime from 'mime';
import { InsertDriveFile } from '@mui/icons-material';
import { getUserUrl } from '../../../../utils/urlPaths';
import { useNavigate } from 'react-router-dom';

const CommentWrapper = styled(Box)(
  ({ theme }) => `
    padding: ${theme.spacing(2)};
    border-radius: ${theme.general.borderRadius};
    background: ${theme.colors.alpha.white[100]};
    border: 1px solid ${theme.colors.alpha.black[10]};
    transition: all 0.2s ease-in-out;
    
    &:hover .comment-actions {
      opacity: 1;
    }
  `
);

interface CommentItemProps {
  comment: Comment;
  workOrderId: number;
}

const isImage = (file: File) => mime.getType(file.name)?.startsWith('image/');
export default function CommentItem(props: CommentItemProps) {
  const { comment, workOrderId } = props;
  const { t } = useTranslation();
  const { getFormattedDate } = useContext(CompanySettingsContext);
  const navigate = useNavigate();
  const { user } = useAuth();
  const dispatch = useDispatch();
  const [isEditing, setIsEditing] = useState<boolean>(false);
  const [editContent, setEditContent] = useState<string>(comment.content);
  const [isImageViewerOpen, setIsImageViewerOpen] = useState<boolean>(false);
  const [currentImage, setCurrentImage] = useState<string>();
  const [currentImages, setCurrentImages] = useState<string[]>();
  const { loadingDelete } = useSelector((state) => state.comments);

  const isOwner = comment.user?.id === user.id;
  const isSystem = comment.system;

  const handleDelete = () => {
    if (window.confirm(t('confirm_delete_comment'))) {
      dispatch(deleteComment(comment.id, workOrderId));
    }
  };

  const handleUpdate = () => {
    if (editContent.trim()) {
      dispatch(
        updateComment(
          comment.id,
          {
            content: editContent,
            files: comment.files.map((f) => ({ id: f.id }))
          },
          workOrderId
        )
      ).then(() => setIsEditing(false));
    }
  };

  const handleCancelEdit = () => {
    setEditContent(comment.content);
    setIsEditing(false);
  };

  const openImageViewer = (images: string[], image: string) => {
    setCurrentImage(image);
    setCurrentImages(images);
    setIsImageViewerOpen(true);
  };

  const getImageFiles = (): File[] => {
    return comment.files.filter(isImage);
  };

  const getOtherFiles = (): File[] => {
    return comment.files.filter((f) => !isImage(f));
  };

  const imageFiles = getImageFiles();
  const otherFiles = getOtherFiles();
  const imageUrls = imageFiles.map((file) => file.url);

  const getUserName = () => {
    return `${comment.user.firstName} ${comment.user.lastName}`.trim();
  };

  return (
    <>
      <CommentWrapper>
        <Stack direction="row" spacing={2} alignItems="flex-start">
          <Avatar
            sx={{
              width: 40,
              height: 40,
              bgcolor: 'primary.main',
              cursor: 'pointer'
            }}
            onClick={() => navigate(getUserUrl(comment.user.id))}
            src={comment.user.image?.url}
          >
            {`${comment.user?.firstName?.charAt(0) || ''}${
              comment.user?.lastName?.charAt(0) || ''
            }`}
          </Avatar>
          <Box sx={{ flex: 1, minWidth: 0 }}>
            <Stack
              direction="row"
              justifyContent="space-between"
              alignItems="center"
              sx={{ mb: 1 }}
            >
              <Box>
                <Typography
                  onClick={() => navigate(getUserUrl(comment.user.id))}
                  variant="subtitle1"
                  fontWeight="bold"
                  sx={{ cursor: 'pointer' }}
                >
                  {getUserName()}
                </Typography>
                <Typography variant="caption" color="text.secondary">
                  {getFormattedDate(comment.createdAt)}
                </Typography>
              </Box>
              {!isSystem && isOwner && !isEditing && (
                <Box className="comment-actions" sx={{ opacity: 0 }}>
                  <IconButton
                    size="small"
                    onClick={() => setIsEditing(true)}
                    sx={{ mr: 0.5 }}
                  >
                    <EditTwoToneIcon fontSize="small" color="primary" />
                  </IconButton>
                  <IconButton size="small" onClick={handleDelete}>
                    <DeleteTwoToneIcon fontSize="small" color="error" />
                  </IconButton>
                </Box>
              )}
            </Stack>
            {isEditing ? (
              <Box>
                <TextField
                  fullWidth
                  multiline
                  minRows={2}
                  value={editContent}
                  onChange={(e) => setEditContent(e.target.value)}
                  sx={{ mb: 1 }}
                />
                <Stack direction="row" spacing={1}>
                  <Button
                    size="small"
                    variant="contained"
                    onClick={handleUpdate}
                    disabled={!editContent.trim()}
                  >
                    {t('save')}
                  </Button>
                  <Button
                    size="small"
                    variant="outlined"
                    onClick={handleCancelEdit}
                  >
                    {t('cancel')}
                  </Button>
                </Stack>
              </Box>
            ) : (
              <Typography
                variant="body1"
                sx={{
                  whiteSpace: 'pre-wrap',
                  wordBreak: 'break-word',
                  mb: comment.files?.length > 0 ? 1 : 0
                }}
                color={comment.system ? 'grey.600' : undefined}
              >
                {comment.content}
              </Typography>
            )}
            {comment.files?.length > 0 && !isEditing && (
              <Box sx={{ mt: 1 }}>
                {imageFiles.length > 0 && (
                  <Box
                    sx={{
                      display: 'flex',
                      flexWrap: 'wrap',
                      gap: 1,
                      mb: otherFiles.length > 0 ? 1 : 0
                    }}
                  >
                    {imageFiles.map((file) => (
                      <img
                        key={file.id}
                        src={file.url}
                        alt={file.name}
                        style={{
                          width: 100,
                          height: 100,
                          objectFit: 'cover',
                          borderRadius: 4,
                          cursor: 'pointer'
                        }}
                        onClick={() => openImageViewer(imageUrls, file.url)}
                      />
                    ))}
                  </Box>
                )}
                {otherFiles.length > 0 && (
                  <Stack spacing={1}>
                    {otherFiles.map((file) => (
                      <Box
                        key={file.id}
                        sx={{
                          display: 'flex',
                          alignItems: 'center',
                          gap: 1,
                          p: 1,
                          borderRadius: 1,
                          bgcolor: 'background.default'
                        }}
                      >
                        <InsertDriveFile color="error" />
                        <Typography
                          variant="body2"
                          sx={{ flex: 1, minWidth: 0 }}
                          noWrap
                        >
                          {file.name}
                        </Typography>
                        <IconButton
                          size="small"
                          component="a"
                          href={file.url}
                          download={file.name}
                        >
                          <ArchiveTwoToneIcon fontSize="small" />
                        </IconButton>
                      </Box>
                    ))}
                  </Stack>
                )}
              </Box>
            )}
          </Box>
        </Stack>
      </CommentWrapper>
      {isImageViewerOpen && (
        <ImageViewer
          src={currentImages || []}
          currentIndex={imageUrls.indexOf(currentImage || '')}
          onClose={() => setIsImageViewerOpen(false)}
          backgroundStyle={{
            backgroundColor: 'rgba(0,0,0,0.9)'
          }}
          disableScroll
          closeOnClickOutside
        />
      )}
    </>
  );
}
