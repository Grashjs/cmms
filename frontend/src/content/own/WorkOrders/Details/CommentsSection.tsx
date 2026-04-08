import {
  Box,
  Button,
  CircularProgress,
  Divider,
  IconButton,
  Stack,
  Typography,
  useTheme
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
import { MentionsTextField } from '@jackstenglein/mui-mentions';
import { getUsersMini } from '../../../../slices/user';

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
  const { usersMini } = useSelector((state) => state.users);

  const [content, setContent] = useState<string>('');
  const [plainTextContent, setPlainTextContent] = useState<string>('');
  const [files, setFiles] = useState<any[]>([]);
  const [showFileUpload, setShowFileUpload] = useState<boolean>(false);
  const theme = useTheme();
  const comments = commentsByWorkOrder[workOrderId] ?? [];

  useEffect(() => {
    dispatch(getCommentsByWorkOrder(workOrderId));
    if (!usersMini.length) {
      dispatch(getUsersMini());
    }
  }, [workOrderId, dispatch, usersMini.length]);

  const handleSubmit = async () => {
    if (!plainTextContent.trim()) return;

    try {
      let fileIds: { id: number }[] = [];

      if (files.length > 0) {
        const uploadedFiles = await uploadFiles(files, [], false);
        fileIds = uploadedFiles.map((f) => ({ id: f.id }));
      }

      // Content is already in the correct format: @[display](user:id)
      // because we configured the markup template as '@[__display__](user:__id__)'
      dispatch(
        createComment({
          workOrder: { id: workOrderId },
          content: content.trim(),
          files: fileIds
        })
      );

      setContent('');
      setPlainTextContent('');
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
        <MentionsTextField
          fullWidth
          multiline
          minRows={3}
          maxRows={6}
          placeholder={t('add_comment_placeholder') || 'Add a comment...'}
          value={content}
          onChange={(newValue, newPlainText, mentions) => {
            setContent(newValue);
            setPlainTextContent(newPlainText);
          }}
          dataSources={[
            {
              trigger: '@',
              markup: '@[__display__](user:__id__)',
              // displayTransform: (id, display) => display || id,
              data: async (query) => {
                const filteredUsers = usersMini.filter((user) =>
                  `${user.firstName} ${user.lastName}`
                    .toLowerCase()
                    .includes(query.toLowerCase())
                );

                return filteredUsers.map((user) => ({
                  id: user.id.toString(),
                  display: `${user.firstName} ${user.lastName}`
                }));
              },
              appendSpaceOnAdd: true,
              allowSpaceInQuery: true
            }
          ]}
          slotProps={{
            suggestionsOverlay: { popper: { sx: { zIndex: 99999 } } }
          }}
          highlightColor={theme.palette.primary.main}
          highlightTextColor
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
            disabled={!plainTextContent.trim() || loadingCreate}
          >
            {t('post_comment')}
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
