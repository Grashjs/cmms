import React, { forwardRef, useEffect, useImperativeHandle, useRef, useState } from 'react';
import { View, StyleSheet } from 'react-native';
import SignatureScreen, { SignatureViewRef } from 'react-native-signature-canvas';
import { Button, HelperText, Text, useTheme } from 'react-native-paper';
import { useTranslation } from 'react-i18next';

export type SignatureSaveState = 'dirty' | 'saving' | 'saved' | 'error';
interface SignaturePadProps {
  label: string;
  onChange: (base64Data: string) => void | Promise<void>;
  value?: string;
  durable?: boolean;
  disabled?: boolean;
  onStateChange?: (state: SignatureSaveState) => void;
  onInputStart?: () => void;
}
export interface SignaturePadHandle {
  waitForSave(): Promise<void>;
  isPending(): boolean;
}

const SignaturePad = forwardRef<SignaturePadHandle, SignaturePadProps>(({
  label, onChange, value, durable = false, disabled = false, onStateChange, onInputStart
}, forwardedRef) => {
  const ref = useRef<SignatureViewRef>(null);
  const theme = useTheme();
  const { t } = useTranslation();
  const [hasChanged, setHasChanged] = useState(false);
  const [saveState, setSaveState] = useState<SignatureSaveState>(value ? 'saved' : 'dirty');
  const state = useRef(saveState);
  const alive = useRef(true);
  const phase = useRef<'idle' | 'capturing' | 'writing' | 'restoring'>('idle');
  const captureUncertain = useRef(false);
  const captureTimer = useRef<ReturnType<typeof setTimeout> | null>(null);
  const captured = useRef<string | null>(null);
  const savedImage = useRef(value ?? '');
  const canvasEpoch = useRef(0);
  const [canvas, setCanvas] = useState({ epoch: 0, image: value ?? '' });
  const [restoring, setRestoring] = useState(false);
  const restoringRef = useRef(false);
  const pending = useRef<{
    promise: Promise<void>; resolve(): void; reject(error: unknown): void;
  } | null>(null);
  const callbacks = useRef({ onChange, onStateChange, onInputStart });
  callbacks.current = { onChange, onStateChange, onInputStart };

  useEffect(() => {
    alive.current = true;
    return () => {
      alive.current = false;
      if (captureTimer.current) clearTimeout(captureTimer.current);
      pending.current?.reject(new Error('CONTEXT_CHANGED'));
      pending.current = null;
    };
  }, []);

  const setState = (next: SignatureSaveState) => {
    state.current = next;
    setSaveState(next);
    callbacks.current.onStateChange?.(next);
  };
  const beginSave = () => {
    let resolve: () => void;
    let reject: (error: unknown) => void;
    const promise = new Promise<void>((done, failed) => { resolve = done; reject = failed; });
    // Save Signature is a UI action; a later closure can await this same promise.
    void promise.catch(() => undefined);
    pending.current = { promise, resolve: resolve!, reject: reject! };
    setState('saving');
  };
  const fail = (error: unknown, uncertain: boolean) => {
    if (!alive.current) return;
    if (captureTimer.current) clearTimeout(captureTimer.current);
    captureTimer.current = null;
    captureUncertain.current = uncertain;
    phase.current = 'idle';
    setState('error');
    pending.current?.reject(error);
    pending.current = null;
  };
  const renewCanvas = (image: string) => {
    canvasEpoch.current += 1;
    restoringRef.current = true;
    setRestoring(true);
    setCanvas({ epoch: canvasEpoch.current, image });
  };
  const finishSave = () => {
    captured.current = null;
    phase.current = 'idle';
    setHasChanged(false);
    setState('saved');
    pending.current?.resolve();
    pending.current = null;
  };
  const persist = (image: string, restoreAfterSave = false) => {
    captured.current = image;
    phase.current = 'writing';
    const epoch = canvasEpoch.current;
    void Promise.resolve().then(() => {
      if (!alive.current || epoch !== canvasEpoch.current) throw new Error('CONTEXT_CHANGED');
      return callbacks.current.onChange(image);
    }).then(() => {
      if (!alive.current || epoch !== canvasEpoch.current) return;
      savedImage.current = image;
      // Refresh the library's recovery image without remounting the live canvas.
      setCanvas((current) => ({ ...current, image }));
      // A retained capture may differ from a WebView restored after the failed write.
      if (restoreAfterSave) renewCanvas(image);
      if (restoringRef.current) phase.current = 'restoring';
      else finishSave();
    }).catch((error) => {
      if (alive.current && epoch === canvasEpoch.current) fail(error, false);
    });
  };
  const currentCanvas = () => alive.current && canvas.epoch === canvasEpoch.current;
  const handleOK = (image: string) => {
    if (!currentCanvas()) return;
    if (!durable) { onChange(image); setHasChanged(false); return; }
    if (phase.current !== 'capturing') return;
    if (captureTimer.current) clearTimeout(captureTimer.current);
    captureTimer.current = null;
    // After a capture error the drawing stays frozen. Any successful retry
    // represents that same drawing. Fence the old instance before unlocking it.
    if (captureUncertain.current) renewCanvas(image);
    captureUncertain.current = false;
    persist(image);
  };
  const handleBegin = () => {
    if (!currentCanvas() || (durable && (phase.current !== 'idle' || captureUncertain.current || restoringRef.current))) return;
    setHasChanged(true);
    callbacks.current.onInputStart?.();
    if (durable) { captured.current = null; setState('dirty'); }
  };
  const saveSignature = () => {
    if (!currentCanvas() || disabled || (durable && (phase.current !== 'idle' || restoringRef.current))) return;
    if (!durable) { ref.current?.readSignature(); return; }
    beginSave();
    if (captured.current !== null) { persist(captured.current, true); return; }
    phase.current = 'capturing';
    captureTimer.current = setTimeout(() => {
      if (currentCanvas() && phase.current === 'capturing')
        fail(new Error('offline_signature_save_failed'), true);
    }, 20000);
    try {
      if (!ref.current) throw new Error('SIGNATURE_NOT_READY');
      ref.current.readSignature();
    } catch (error) { fail(error, true); }
  };
  const handleClear = () => {
    if (!currentCanvas() || disabled || (durable && (phase.current !== 'idle' || restoringRef.current))) return;
    if (!durable) { ref.current?.clearSignature(); onChange(''); setHasChanged(false); return; }
    callbacks.current.onInputStart?.();
    // Clearing is explicit: discard the old canvas and all of its callbacks.
    renewCanvas('');
    captureUncertain.current = false;
    beginSave();
    persist('');
  };
  const captureError = () => {
    if (!durable || !currentCanvas()) return;
    if (phase.current === 'capturing')
      fail(new Error('offline_signature_save_failed'), true);
    else if (phase.current === 'restoring')
      fail(new Error('offline_signature_save_failed'), false);
  };

  useImperativeHandle(forwardedRef, () => ({
    async waitForSave() {
      if (!durable) return;
      if (pending.current) await pending.current.promise;
      if (!alive.current) throw new Error('CONTEXT_CHANGED');
      if (state.current !== 'saved')
        throw new Error(state.current === 'error' ? 'offline_signature_save_failed' : 'offline_signature_unsaved');
    },
    isPending() { return durable && state.current !== 'saved'; }
  }));

  const style = `.m-signature-pad--footer .button {
    background-color: ${theme.colors.primary}; color: ${theme.colors.onPrimary};
  }
  body, html { height: 100%; margin: 0; padding: 0; }
  .m-signature-pad--body canvas { width: 100%; height: 100%; }
  ${durable ? '.m-signature-pad--footer { display: none; }' : ''}`;
  const busy = durable && (saveState === 'saving' || restoring);
  return (
    <View style={styles.container}>
      <Text style={styles.label}>{label}</Text>
      <View style={styles.signatureContainer}
        pointerEvents={disabled || busy || (durable && captureUncertain.current) ? 'none' : 'auto'}>
        <SignatureScreen key={canvas.epoch} ref={ref} onOK={handleOK} onBegin={handleBegin}
          onEmpty={() => durable && handleOK('')}
          onError={captureError}
          onLoadEnd={() => {
            if (!currentCanvas()) return;
            restoringRef.current = false;
            setRestoring(false);
            if (phase.current === 'restoring') finishSave();
          }}
          // Keep the drawing on a capture/load error instead of the library's
          // automatic reload. Only Retry or an explicit clear changes it.
          webviewProps={durable ? { onError: captureError } : undefined}
          webStyle={style} dataURL={durable ? canvas.image : value} />
      </View>
      {durable && hasChanged && saveState === 'dirty' && <HelperText type="info">{t('offline_signature_unsaved')}</HelperText>}
      {durable && saveState === 'saving' && <Text>{t('offline_saving')}</Text>}
      {durable && saveState === 'saved' && <Text>{t(savedImage.current ? 'offline_signature_saved' : 'offline_draft_saved')}</Text>}
      {durable && saveState === 'error' && <HelperText type="error">{t('offline_signature_save_failed')}</HelperText>}
      <View style={styles.buttonContainer}>
        <Button mode="outlined" onPress={handleClear} disabled={disabled || busy} style={styles.button}>
          {durable ? t('clear') : 'Clear'}
        </Button>
        {(hasChanged || (durable && saveState === 'error')) && (
          <Button mode="contained" onPress={saveSignature} disabled={disabled || busy} style={styles.button}>
            {durable ? t('save_signature') : 'Save Signature'}
          </Button>
        )}
      </View>
    </View>
  );
});

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
