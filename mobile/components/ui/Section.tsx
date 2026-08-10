import * as React from 'react';
import { ReactNode, useState } from 'react';
import { Pressable, StyleSheet, View } from 'react-native';
import { Icon, Text } from 'react-native-paper';
import { IconSource } from 'react-native-paper/lib/typescript/components/Icon';
import { useAppTheme } from '../../custom-theme';
import { elevation, fontWeight, radius, spacing, touchTarget } from '../../theme/tokens';

export interface SectionProps {
  title: string;
  icon?: IconSource;
  /** Shown next to the title, e.g. a completed count such as `3/8`. */
  badge?: string;
  children: ReactNode;
  /** When set, the header toggles visibility of the body. */
  collapsible?: boolean;
  defaultCollapsed?: boolean;
  /** Rendered at the right of the header, e.g. an add button. */
  action?: ReactNode;
}

/**
 * A titled group of related fields on a detail screen.
 *
 * The detail screens currently run every field together in one column, so
 * finding a single value means reading the whole page. Grouping gives the eye
 * somewhere to land, and collapsing keeps rarely used groups out of the way
 * without hiding that they exist.
 */
export default function Section({
  title,
  icon,
  badge,
  children,
  collapsible = false,
  defaultCollapsed = false,
  action
}: SectionProps) {
  const theme = useAppTheme();
  const [collapsed, setCollapsed] = useState(collapsible && defaultCollapsed);

  const header = (
    <View style={styles.header}>
      {!!icon && <Icon source={icon} size={20} color={theme.colors.grey} />}
      <Text
        variant="titleSmall"
        style={{ color: theme.colors.text, fontWeight: fontWeight.semibold }}
      >
        {title}
      </Text>
      {!!badge && (
        <Text variant="bodySmall" style={{ color: theme.colors.grey }}>
          {badge}
        </Text>
      )}
      <View style={styles.spacer} />
      {action}
      {collapsible && (
        <Icon
          source={collapsed ? 'chevron-down' : 'chevron-up'}
          size={22}
          color={theme.colors.grey}
        />
      )}
    </View>
  );

  return (
    <View
      style={[
        styles.container,
        elevation.card,
        {
          backgroundColor: theme.colors.card,
          shadowColor: theme.dark ? 'transparent' : '#000'
        }
      ]}
    >
      {collapsible ? (
        <Pressable
          onPress={() => setCollapsed((value) => !value)}
          accessibilityRole="button"
          accessibilityState={{ expanded: !collapsed }}
          accessibilityLabel={title}
        >
          {header}
        </Pressable>
      ) : (
        header
      )}
      {!collapsed && <View style={styles.body}>{children}</View>}
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    borderRadius: radius.lg,
    marginHorizontal: spacing.lg,
    marginBottom: spacing.md,
    paddingHorizontal: spacing.lg,
    paddingVertical: spacing.md
  },
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing.sm,
    minHeight: touchTarget.min
  },
  spacer: {
    flex: 1
  },
  body: {
    paddingTop: spacing.sm,
    gap: spacing.md
  }
});
