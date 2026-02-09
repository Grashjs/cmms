import {
  Dialog,
  DialogTitle,
  DialogContent,
  TextField,
  DialogActions,
  Button,
  CircularProgress
} from '@mui/material';
import { useState } from 'react';

interface EmailModalProps {
  open: boolean;
  onClose: () => void;
  onSubmit: (email: string) => Promise<void>;
}

export default function EmailModal({
  open,
  onClose,
  onSubmit
}: EmailModalProps) {
  const [email, setEmail] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState<boolean>(false);

  const handleSubmit = () => {
    if (!email || !/\S+@\S+\.\S+/.test(email)) {
      setError('Please enter a valid email address');
      return;
    }
    setError('');
    setLoading(true);
    onSubmit(email).finally(() => setLoading(false));
  };

  return (
    <Dialog maxWidth={'sm'} open={open} onClose={onClose}>
      <DialogTitle>Enter your email</DialogTitle>
      <DialogContent>
        <TextField
          autoFocus
          margin="dense"
          id="email"
          label="Email Address"
          type="email"
          fullWidth
          variant="outlined"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          onKeyDown={(e) => e.key === 'Enter' && handleSubmit()}
          error={!!error}
          helperText={error}
        />
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose}>Cancel</Button>
        <Button
          startIcon={loading && <CircularProgress size={'1rem'} />}
          onClick={handleSubmit}
          disabled={loading}
        >
          Continue
        </Button>
      </DialogActions>
    </Dialog>
  );
}
