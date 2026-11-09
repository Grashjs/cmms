import React from 'react';
import { ActivityIndicator, Pressable, StyleSheet, Text, View } from 'react-native';
import { useTranslation } from 'react-i18next';

type LockIssue =
  | 'none'
  | 'cancelled'
  | 'failed'
  | 'unavailable'
  | 'authorization'
  | 'denied'
  | 'session'
  | 'preference'
  | 'storage';

export function OfflineLockScreen({
  waiting,
  unlocking,
  issue,
  onRetry,
  onLogout
}: {
  waiting: boolean;
  unlocking: boolean;
  issue: LockIssue;
  onRetry(): void;
  onLogout(): void;
}) {
  const { t } = useTranslation();
  const message =
    issue === 'unavailable'
      ? 'offline_unlock_setup'
      : issue === 'authorization'
      ? 'offline_expired'
      : issue === 'denied'
      ? 'offline_disabled'
      : issue === 'session'
      ? 'offline_unlock_sign_in'
      : issue === 'preference'
      ? 'offline_preference_read_error'
      : issue === 'storage'
      ? 'offline_load_error'
      : issue === 'cancelled'
      ? 'offline_unlock_cancelled'
      : issue === 'failed'
      ? 'offline_unlock_failed'
      : 'offline_unlock_explanation';

  return (
    <View style={styles.screen} accessibilityViewIsModal>
      {waiting ? (
        <ActivityIndicator size="large" color="#244a72" />
      ) : (
        <View style={styles.content}>
          <Text style={styles.title}>{t('offline_unlock_title')}</Text>
          <Text style={styles.message} accessibilityRole="alert">
            {t(message)}
          </Text>
          {unlocking ? (
            <ActivityIndicator size="large" color="#244a72" />
          ) : (
            <>
              {issue !== 'session' && (
                <Pressable
                  accessibilityRole="button"
                  style={styles.button}
                  onPress={onRetry}
                >
                  <Text style={styles.buttonText}>
                    {t(
                      issue === 'authorization' || issue === 'denied'
                        ? 'offline_unlock_check_server'
                        : 'retry'
                    )}
                  </Text>
                </Pressable>
              )}
              <Pressable accessibilityRole="button" onPress={onLogout}>
                <Text style={styles.logout}>{t('offline_unlock_logout')}</Text>
              </Pressable>
            </>
          )}
        </View>
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  screen: {
    flex: 1,
    backgroundColor: '#ffffff',
    justifyContent: 'center',
    alignItems: 'center',
    padding: 28
  },
  content: { width: '100%', maxWidth: 400, alignItems: 'center' },
  title: { fontSize: 24, fontWeight: '600', color: '#192b3c' },
  message: {
    marginTop: 20,
    marginBottom: 28,
    fontSize: 16,
    lineHeight: 23,
    color: '#192b3c',
    textAlign: 'center'
  },
  button: {
    minWidth: 160,
    paddingVertical: 12,
    paddingHorizontal: 20,
    borderRadius: 8,
    backgroundColor: '#244a72',
    alignItems: 'center'
  },
  buttonText: { color: '#ffffff', fontSize: 16, fontWeight: '600' },
  logout: { marginTop: 24, color: '#244a72', fontSize: 16 }
});
