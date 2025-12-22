import { useEffect, useRef, useState } from 'react';
import {
  Box,
  Paper,
  Typography,
  CircularProgress,
  Alert,
  Chip,
  Divider,
} from '@mui/material';
import { useWorkOrderChat } from '../../../../hooks/useWorkOrderChat';
import workOrderMessageService, {
  SendMessageRequest,
} from '../../../../services/workOrderMessage';
import ChatMessage from './ChatMessage';
import ChatInput from './ChatInput';
import { useSnackbar } from 'notistack';
import fileService from '../../../../services/files';

interface WorkOrderChatPanelProps {
  workOrderId: number;
  currentUserId: number;
  isWorkOrderCompleted: boolean;
}

export default function WorkOrderChatPanel({
  workOrderId,
  currentUserId,
  isWorkOrderCompleted,
}: WorkOrderChatPanelProps) {
  const {
    messages,
    setMessages,
    isConnected,
    typingUsers,
    sendTypingIndicator,
  } = useWorkOrderChat(workOrderId);

  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const messagesEndRef = useRef<HTMLDivElement>(null);
  const { enqueueSnackbar } = useSnackbar();

  // Load messages on mount
  useEffect(() => {
    loadMessages();
  }, [workOrderId]);

  // Scroll to bottom when new messages arrive
  useEffect(() => {
    scrollToBottom();
  }, [messages]);

  // Mark messages as read when they come into view
  useEffect(() => {
    if (messages.length > 0) {
      const unreadMessages = messages.filter(
        (msg) => !msg.readByCurrentUser && msg.user?.id !== currentUserId
      );
      if (unreadMessages.length > 0) {
        // Mark all as read after a short delay
        const timeout = setTimeout(() => {
          workOrderMessageService.markAllAsRead(workOrderId).catch(console.error);
        }, 1000);
        return () => clearTimeout(timeout);
      }
    }
  }, [messages, workOrderId, currentUserId]);

  const loadMessages = async () => {
    try {
      setIsLoading(true);
      const response = await workOrderMessageService.getMessages(workOrderId);
      setMessages(response.data);
      setError(null);
    } catch (err) {
      setError('Failed to load messages');
      console.error('Error loading messages:', err);
    } finally {
      setIsLoading(false);
    }
  };

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  };

  const handleSendMessage = async (content: string, type: 'TEXT') => {
    try {
      const request: SendMessageRequest = {
        workOrderId,
        messageType: type,
        content,
      };

      await workOrderMessageService.sendMessage(request);
      // Message will be added via WebSocket
    } catch (err) {
      enqueueSnackbar('Failed to send message', { variant: 'error' });
      console.error('Error sending message:', err);
    }
  };

  const handleSendVoice = async (audioBlob: Blob) => {
    try {
      // Upload audio file first
      const formData = new FormData();
      formData.append('file', audioBlob, 'voice-message.webm');
      formData.append('type', 'OTHER');

      const uploadResponse = await fileService.create(formData);
      const fileId = uploadResponse.data.id;

      // Send message with file reference
      const request: SendMessageRequest = {
        workOrderId,
        messageType: 'VOICE',
        fileId,
      };

      await workOrderMessageService.sendMessage(request);
      enqueueSnackbar('Voice message sent', { variant: 'success' });
    } catch (err) {
      enqueueSnackbar('Failed to send voice message', { variant: 'error' });
      console.error('Error sending voice message:', err);
    }
  };

  const handleSendFile = async (
    file: File,
    type: 'IMAGE' | 'VIDEO' | 'DOCUMENT'
  ) => {
    try {
      // Upload file first
      const formData = new FormData();
      formData.append('file', file);
      formData.append('type', type === 'DOCUMENT' ? 'OTHER' : type);

      const uploadResponse = await fileService.create(formData);
      const fileId = uploadResponse.data.id;

      // Send message with file reference
      const request: SendMessageRequest = {
        workOrderId,
        messageType: type,
        fileId,
        content: file.name,
      };

      await workOrderMessageService.sendMessage(request);
      enqueueSnackbar('File sent', { variant: 'success' });
    } catch (err) {
      enqueueSnackbar('Failed to send file', { variant: 'error' });
      console.error('Error sending file:', err);
      throw err;
    }
  };

  const handleReaction = async (messageId: number, reaction: string) => {
    try {
      await workOrderMessageService.toggleReaction(messageId, reaction);
      // Reaction will be updated via WebSocket
    } catch (err) {
      enqueueSnackbar('Failed to add reaction', { variant: 'error' });
      console.error('Error adding reaction:', err);
    }
  };

  const handleEdit = (messageId: number) => {
    // TODO: Implement edit functionality
    enqueueSnackbar('Edit functionality coming soon', { variant: 'info' });
  };

  const handleDelete = async (messageId: number) => {
    try {
      await workOrderMessageService.updateMessage(messageId, { deleted: true });
      // Message will be updated via WebSocket
      enqueueSnackbar('Message deleted', { variant: 'success' });
    } catch (err) {
      enqueueSnackbar('Failed to delete message', { variant: 'error' });
      console.error('Error deleting message:', err);
    }
  };

  const handleTyping = (isTyping: boolean) => {
    // Get current user info (you'll need to pass this or get from context)
    const userName = 'Current User'; // Replace with actual user name
    sendTypingIndicator(currentUserId, userName, isTyping);
  };

  if (isLoading) {
    return (
      <Box
        sx={{
          display: 'flex',
          justifyContent: 'center',
          alignItems: 'center',
          height: 400,
        }}
      >
        <CircularProgress />
      </Box>
    );
  }

  if (error) {
    return (
      <Alert severity="error" sx={{ m: 2 }}>
        {error}
      </Alert>
    );
  }

  return (
    <Paper sx={{ height: '600px', display: 'flex', flexDirection: 'column' }}>
      {/* Header */}
      <Box sx={{ p: 2, borderBottom: 1, borderColor: 'divider' }}>
        <Typography variant="h6">Work Order Chat</Typography>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mt: 0.5 }}>
          <Chip
            label={isConnected ? 'Connected' : 'Disconnected'}
            color={isConnected ? 'success' : 'error'}
            size="small"
          />
          {isWorkOrderCompleted && (
            <Chip label="Read-only" color="warning" size="small" />
          )}
        </Box>
      </Box>

      {/* Messages */}
      <Box
        sx={{
          flex: 1,
          overflowY: 'auto',
          p: 2,
          bgcolor: 'background.default',
        }}
      >
        {messages.length === 0 ? (
          <Box
            sx={{
              display: 'flex',
              justifyContent: 'center',
              alignItems: 'center',
              height: '100%',
            }}
          >
            <Typography variant="body2" color="text.secondary">
              No messages yet. Start the conversation!
            </Typography>
          </Box>
        ) : (
          <>
            {messages.map((message) => (
              <ChatMessage
                key={message.id}
                message={message}
                currentUserId={currentUserId}
                onReaction={handleReaction}
                onEdit={handleEdit}
                onDelete={handleDelete}
              />
            ))}
            <div ref={messagesEndRef} />
          </>
        )}

        {/* Typing indicators */}
        {typingUsers.length > 0 && (
          <Box sx={{ mt: 1 }}>
            <Typography variant="caption" color="text.secondary">
              {typingUsers.join(', ')}{' '}
              {typingUsers.length === 1 ? 'is' : 'are'} typing...
            </Typography>
          </Box>
        )}
      </Box>

      <Divider />

      {/* Input */}
      <ChatInput
        onSendMessage={handleSendMessage}
        onSendVoice={handleSendVoice}
        onSendFile={handleSendFile}
        onTyping={handleTyping}
        isReadOnly={isWorkOrderCompleted}
      />
    </Paper>
  );
}
