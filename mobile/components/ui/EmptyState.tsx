import * as React from 'react';
import { StyleSheet, View } from 'react-native';
import { Button, Icon, Text } from 'react-native-paper';
import { IconSource } from 'react-native-paper/lib/typescript/components/Icon';
import { useAppTheme } from '../../custom-theme';
import { spacing } from '../../theme/tokens';

export interface EmptyStateProps {
  icon?: IconSource;
  title: string;
  /** One sentence on why the list is empty and what to do about it. */
  description?: string;
  action?: { label: string; onPress: () => void };
  /**
   * Renders the error treatment: a warning glyph in the error color. Empty and
   * failed are different situations and should not look identical, since the
   * first is normal and the second needs a retry.
   */
  variant?: 'empty' | 'error';
}

/**
 * Shown in place of a list that has no rows. Lists across the app previously
 * rendered either a bare sentence on a white block or nothing at all, which
 * left "still loading", "nothing here" and "the request failed" visually
 * indistinguishable.
 */
export default function EmptyState({
  icon,
  title,
  description,
  action,
  variant = 'empty'
}: EmptyStateProps) {
  const theme = useAppTheme();
  const isError = variant === 'error';
  const tint = isError ? theme.colors.error : theme.colors.tertiary;

  return (
    <View style={styles.container}>
      <Icon
        source={icon ?? (isError ? 'alert-circle-outline' : 'inbox-outline')}
        size={56}
        color={tint}
      />
      <Text
        variant="titleMedium"
        style={[styles.title, { color: theme.colors.text }]}
      >
        {title}
      </Text>
      {!!description && (
        <Text
          variant="bodyMedium"
          style={[styles.description, { color: theme.colors.grey }]}
        >
          {description}
        </Text>
      )}
      {!!action && (
        <Button
          mode={isError ? 'contained' : 'outlined'}
          onPress={action.onPress}
          style={styles.action}
        >
          {action.label}
        </Button>
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    paddingHorizontal: spacing.xxxl,
    paddingVertical: spacing.xxxl * 2,
    gap: spacing.md
  },
  title: {
    textAlign: 'center'
  },
  description: {
    textAlign: 'center'
  },
  action: {
    marginTop: spacing.sm
  }
});
