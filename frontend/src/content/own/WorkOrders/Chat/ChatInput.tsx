import { useState, useRef, ChangeEvent } from 'react';
import {
  Box,
  TextField,
  IconButton,
  Tooltip,
  CircularProgress,
} from '@mui/material';
import SendIcon from '@mui/icons-material/Send';
import MicIcon from '@mui/icons-material/Mic';
import AttachFileIcon from '@mui/icons-material/AttachFile';
import ImageIcon from '@mui/icons-material/Image';
import VoiceRecorder from './VoiceRecorder';
import { useSnackbar } from 'notistack';

interface ChatInputProps {
  onSendMessage: (content: string, type: 'TEXT') => void;
  onSendVoice: (audioBlob: Blob) => void;
  onSendFile: (file: File, type: 'IMAGE' | 'VIDEO' | 'DOCUMENT') => void;
  onTyping: (isTyping: boolean) => void;
  disabled?: boolean;
  isReadOnly?: boolean;
}

export default function ChatInput({
  onSendMessage,
  onSendVoice,
  onSendFile,
  onTyping,
  disabled = false,
  isReadOnly = false,
}: ChatInputProps) {
  const [message, setMessage] = useState('');
  const [showVoiceRecorder, setShowVoiceRecorder] = useState(false);
  const [isUploading, setIsUploading] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);
  const imageInputRef = useRef<HTMLInputElement>(null);
  const typingTimeoutRef = useRef<NodeJS.Timeout | null>(null);
  const { enqueueSnackbar } = useSnackbar();

  const MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB

  const handleMessageChange = (e: ChangeEvent<HTMLInputElement>) => {
    setMessage(e.target.value);

    // Send typing indicator
    onTyping(true);

    // Clear existing timeout
    if (typingTimeoutRef.current) {
      clearTimeout(typingTimeoutRef.current);
    }

    // Set new timeout to stop typing indicator
    typingTimeoutRef.current = setTimeout(() => {
      onTyping(false);
    }, 2000);
  };

  const handleSend = () => {
    if (message.trim() && !disabled) {
      onSendMessage(message.trim(), 'TEXT');
      setMessage('');
      onTyping(false);
    }
  };

  const handleKeyPress = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSend();
    }
  };

  const handleVoiceRecordingComplete = async (audioBlob: Blob) => {
    setShowVoiceRecorder(false);
    onSendVoice(audioBlob);
  };

  const handleFileSelect = async (e: ChangeEvent<HTMLInputElement>, type: 'IMAGE' | 'DOCUMENT') => {
    const file = e.target.files?.[0];
    if (!file) return;

    // Validate file size
    if (file.size > MAX_FILE_SIZE) {
      enqueueSnackbar('File size must be less than 10MB', { variant: 'error' });
      return;
    }

    setIsUploading(true);
    try {
      // Determine file type
      let fileType: 'IMAGE' | 'VIDEO' | 'DOCUMENT' = type;
      if (file.type.startsWith('video/')) {
        fileType = 'VIDEO';
      }

      await onSendFile(file, fileType);
    } catch (error) {
      enqueueSnackbar('Failed to upload file', { variant: 'error' });
    } finally {
      setIsUploading(false);
      if (fileInputRef.current) fileInputRef.current.value = '';
      if (imageInputRef.current) imageInputRef.current.value = '';
    }
  };

  if (isReadOnly) {
    return (
      <Box sx={{ p: 2, bgcolor: 'action.disabledBackground', borderRadius: 1 }}>
        <TextField
          fullWidth
          disabled
          placeholder="This work order is completed. Chat is read-only."
          variant="outlined"
          size="small"
        />
      </Box>
    );
  }

  if (showVoiceRecorder) {
    return (
      <VoiceRecorder
        onRecordingComplete={handleVoiceRecordingComplete}
        onCancel={() => setShowVoiceRecorder(false)}
        maxDuration={60}
      />
    );
  }

  return (
    <Box sx={{ display: 'flex', gap: 1, alignItems: 'flex-end', p: 2 }}>
      <input
        ref={fileInputRef}
        type="file"
        hidden
        accept=".pdf,.doc,.docx,.xls,.xlsx,.txt"
        onChange={(e) => handleFileSelect(e, 'DOCUMENT')}
      />
      <input
        ref={imageInputRef}
        type="file"
        hidden
        accept="image/*,video/*"
        onChange={(e) => handleFileSelect(e, 'IMAGE')}
      />

      <Tooltip title="Attach file">
        <IconButton
          size="small"
          onClick={() => fileInputRef.current?.click()}
          disabled={disabled || isUploading}
        >
          {isUploading ? <CircularProgress size={20} /> : <AttachFileIcon />}
        </IconButton>
      </Tooltip>

      <Tooltip title="Attach image/video">
        <IconButton
          size="small"
          onClick={() => imageInputRef.current?.click()}
          disabled={disabled || isUploading}
        >
          <ImageIcon />
        </IconButton>
      </Tooltip>

      <Tooltip title="Record voice message">
        <IconButton
          size="small"
          onClick={() => setShowVoiceRecorder(true)}
          disabled={disabled}
        >
          <MicIcon />
        </IconButton>
      </Tooltip>

      <TextField
        fullWidth
        multiline
        maxRows={4}
        value={message}
        onChange={handleMessageChange}
        onKeyPress={handleKeyPress}
        placeholder="Type a message..."
        variant="outlined"
        size="small"
        disabled={disabled}
      />

      <Tooltip title="Send message">
        <span>
          <IconButton
            color="primary"
            onClick={handleSend}
            disabled={!message.trim() || disabled}
          >
            <SendIcon />
          </IconButton>
        </span>
      </Tooltip>
    </Box>
  );
}
