import * as React from 'react';
import { Pressable, StyleSheet, View } from 'react-native';
import { Icon, Text } from 'react-native-paper';
import { useTranslation } from 'react-i18next';
import { useAppTheme } from '../custom-theme';
import { fontWeight, spacing, touchTarget } from '../theme/tokens';
import useOfflineSync from '../hooks/useOfflineSync';
import { useDispatch } from '../store';
import { flushOfflineQueue } from '../utils/offlineQueue';

/**
 * Sits under the status bar and reports connectivity plus any field edits
 * waiting to sync. Technicians in a plant basement need to know both.
 */
export default function ConnectivityBanner() {
  const { t } = useTranslation();
  const theme = useAppTheme();
  const dispatch = useDispatch();
  const { pending, flushing, online } = useOfflineSync();

  if (online && pending === 0) return null;

  const offline = !online;
  const tint = offline ? theme.colors.warning : theme.colors.info;

  const message = offline
    ? t('offline_banner')
    : flushing
      ? t('offline_syncing', { count: pending })
      : t('offline_sync_pending', { count: pending });

  return (
    <Pressable
      accessibilityRole="button"
      accessibilityLabel={message}
      disabled={offline || flushing || pending === 0}
      onPress={() => flushOfflineQueue(dispatch)}
      style={[styles.banner, { backgroundColor: tint }]}
    >
      <Icon source={offline ? 'cloud-off-outline' : 'cloud-sync-outline'} size={18} color={theme.colors.white} />
      <Text
        variant="labelMedium"
        numberOfLines={2}
        style={{ color: theme.colors.white, flex: 1, fontWeight: fontWeight.medium }}
      >
        {message}
      </Text>
      {!offline && pending > 0 && !flushing && (
        <Text variant="labelMedium" style={{ color: theme.colors.white }}>
          {t('sync_now')}
        </Text>
      )}
    </Pressable>
  );
}

const styles = StyleSheet.create({
  banner: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing.sm,
    paddingHorizontal: spacing.lg,
    paddingVertical: spacing.sm,
    minHeight: touchTarget.min
  }
});
