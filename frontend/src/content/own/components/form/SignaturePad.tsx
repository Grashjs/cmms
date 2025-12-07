import React, { useRef, useState } from 'react';
import SignatureCanvas from 'react-signature-canvas';
import { Button, Typography } from '@mui/material';
import { useTranslation } from 'react-i18next';

interface SignaturePadProps {
  label: string;
  onChange: (base64Data: string) => void;
  value?: string;
}

const SignaturePad: React.FC<SignaturePadProps> = ({
  label,
  onChange,
  value
}) => {
  const sigCanvas = useRef<SignatureCanvas>(null);
  const { t } = useTranslation();

  const handleEnd = () => {
    if (sigCanvas.current) {
      const signature = sigCanvas.current.toDataURL();
      onChange(signature);
    }
  };

  const handleClear = () => {
    if (sigCanvas.current) {
      sigCanvas.current.clear();
      onChange('');
    }
  };

  // Load existing signature if value is provided
  React.useEffect(() => {
    if (value && sigCanvas.current) {
      sigCanvas.current.fromDataURL(value);
    }
  }, [value]);

  return (
    <div style={styles.container}>
      <Typography variant="h6" fontWeight="bold" sx={{ mb: 0.5 }}>
        {label}
      </Typography>
      <div style={styles.signatureContainer}>
        <SignatureCanvas
          ref={sigCanvas}
          penColor="black"
          canvasProps={{
            style: styles.canvas
          }}
          onEnd={handleEnd}
        />
      </div>
      <div style={styles.buttonContainer}>
        <Button variant={'outlined'} onClick={handleClear}>
          {t('clear')}
        </Button>
      </div>
    </div>
  );
};

const styles = {
  container: {
    marginTop: '10px',
    marginBottom: '10px'
  } as React.CSSProperties,
  label: {
    fontSize: '16px',
    marginBottom: '5px',
    display: 'block',
    fontWeight: 500
  } as React.CSSProperties,
  signatureContainer: {
    height: '200px',
    border: '1px solid #ccc',
    borderRadius: '5px',
    overflow: 'hidden'
  } as React.CSSProperties,
  canvas: {
    width: '100%',
    height: '100%'
  } as React.CSSProperties,
  buttonContainer: {
    display: 'flex',
    justifyContent: 'flex-end',
    marginTop: '10px',
    gap: '10px'
  } as React.CSSProperties
};

export default SignaturePad;
