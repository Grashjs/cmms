import {
  Box,
  Button,
  CircularProgress,
  Divider,
  Stack,
  TextField,
  Typography,
  IconButton,
  styled
} from '@mui/material';
import { useContext, useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useDispatch, useSelector } from '../../../../store';
import {
  createComment,
  getCommentsByWorkOrder
} from '../../../../slices/comment';
import CommentItem from './CommentItem';
import { CompanySettingsContext } from '../../../../contexts/CompanySettingsContext';
import FileUpload from '../../components/FileUpload';
import SendTwoToneIcon from '@mui/icons-material/SendTwoTone';
import AttachFileTwoToneIcon from '@mui/icons-material/AttachFileTwoTone';

interface CommentsSectionProps {
  workOrderId: number;
}

export default function CommentsSection(props: CommentsSectionProps) {
  const { workOrderId } = props;
  const { t } = useTranslation();
  const dispatch = useDispatch();
  const { uploadFiles } = useContext(CompanySettingsContext);
  const { commentsByWorkOrder, loadingComments, loadingCreate } = useSelector(
    (state) => state.comments
  );

  const [content, setContent] = useState<string>('');
  const [files, setFiles] = useState<any[]>([]);
  const [showFileUpload, setShowFileUpload] = useState<boolean>(false);

  const comments = commentsByWorkOrder[workOrderId] ?? [];

  useEffect(() => {
    dispatch(getCommentsByWorkOrder(workOrderId));
  }, [workOrderId, dispatch]);

  const handleSubmit = async () => {
    if (!content.trim()) return;

    try {
      let fileIds: { id: number }[] = [];

      if (files.length > 0) {
        const uploadedFiles = await uploadFiles(files, [], false);
        fileIds = uploadedFiles.map((f) => ({ id: f.id }));
      }

      dispatch(
        createComment({
          workOrder: { id: workOrderId },
          content: content.trim(),
          files: fileIds
        })
      );

      setContent('');
      setFiles([]);
      setShowFileUpload(false);
    } catch (error) {
      console.error('Failed to create comment:', error);
    }
  };

  const handleFileDrop = (droppedFiles: any[]) => {
    setFiles((prev) => [...prev, ...droppedFiles]);
  };

  const handleRemoveFile = (index: number) => {
    setFiles((prev) => prev.filter((_, i) => i !== index));
  };

  return (
    <Box sx={{ p: 2 }}>
      {/* Add Comment Section */}
      <Box sx={{ mb: 3 }}>
        <TextField
          fullWidth
          multiline
          minRows={3}
          maxRows={6}
          placeholder={t('add_comment_placeholder') || 'Add a comment...'}
          value={content}
          onChange={(e) => setContent(e.target.value)}
          sx={{ mb: 2 }}
        />

        {showFileUpload && (
          <Box mb={2}>
            <FileUpload
              title={t('files')}
              description={t('upload')}
              multiple={true}
              type="file"
              onDrop={handleFileDrop}
              files={files}
            />
          </Box>
        )}

        <Stack
          direction="row"
          spacing={1}
          justifyContent="space-between"
          alignItems="center"
        >
          <IconButton
            size="small"
            onClick={() => setShowFileUpload(!showFileUpload)}
            color={showFileUpload ? 'primary' : 'default'}
          >
            <AttachFileTwoToneIcon />
          </IconButton>
          <Button
            variant="contained"
            startIcon={
              loadingCreate ? (
                <CircularProgress size="1rem" />
              ) : (
                <SendTwoToneIcon />
              )
            }
            onClick={handleSubmit}
            disabled={!content.trim() || loadingCreate}
          >
            {t('post_comment') || 'Post Comment'}
          </Button>
        </Stack>
      </Box>

      <Divider sx={{ mb: 3 }} />

      {/* Comments List */}
      {loadingComments ? (
        <Box
          sx={{
            display: 'flex',
            justifyContent: 'center',
            py: 4
          }}
        >
          <CircularProgress />
        </Box>
      ) : comments.length === 0 ? (
        <Box sx={{ textAlign: 'center', py: 4 }}>
          <Typography variant="body1" color="text.secondary">
            {t('no_comments') || 'No comments yet'}
          </Typography>
        </Box>
      ) : (
        <Stack spacing={2}>
          {comments.map((comment) => (
            <CommentItem
              key={comment.id}
              comment={comment}
              workOrderId={workOrderId}
            />
          ))}
        </Stack>
      )}
    </Box>
  );
}
