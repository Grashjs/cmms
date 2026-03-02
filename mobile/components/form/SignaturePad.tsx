import React, { useRef, useState } from 'react';
import { View, StyleSheet, TouchableOpacity } from 'react-native';
import SignatureScreen, {
  SignatureViewRef
} from 'react-native-signature-canvas';
import { Button, Text, useTheme } from 'react-native-paper';
import { IHash } from '../../models/form';

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
  const ref = useRef<SignatureViewRef>(null);
  const theme = useTheme();
  const [hasChanged, setHasChanged] = useState(false);

  const handleOK = (signature: string) => {
    onChange(signature);
    setHasChanged(false);
  };

  const handleBegin = () => {
    setHasChanged(true);
  };

  const saveSignature = () => {
    ref.current.readSignature(); // This triggers onOK
  };

  const handleClear = () => {
    ref.current.clearSignature();
    onChange('');
    setHasChanged(false);
  };

  const style = `.m-signature-pad--footer .button {
    background-color: ${theme.colors.primary};
    color: ${theme.colors.onPrimary};
  }
   body, html {
      height: 100%;
      margin: 0;
      padding: 0;
    }
    .m-signature-pad--body canvas {
      width: 100%;
      height: 100%;
    }`;

  return (
    <View style={styles.container}>
      <Text style={styles.label}>{label}</Text>
      <View style={styles.signatureContainer}>
        <SignatureScreen
          ref={ref}
          onOK={handleOK}
          onBegin={handleBegin}
          webStyle={style}
          dataURL={value}
        />
      </View>
      <View style={styles.buttonContainer}>
        <Button mode="outlined" onPress={handleClear} style={styles.button}>
          Clear
        </Button>
        {hasChanged && (
          <Button
            mode="contained"
            onPress={saveSignature}
            style={styles.button}
          >
            Save Signature
          </Button>
        )}
      </View>
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    marginVertical: 10
  },
  label: {
    fontSize: 16,
    marginBottom: 5
  },
  signatureContainer: {
    height: 200,
    borderColor: '#ccc',
    borderWidth: 1,
    borderRadius: 5,
    overflow: 'hidden'
  },
  buttonContainer: {
    flexDirection: 'row',
    justifyContent: 'flex-end',
    marginTop: 10,
    gap: 10
  },
  button: {
    marginLeft: 10
  }
});

export default SignaturePad;
