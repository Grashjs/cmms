import * as React from 'react';
import { ReactNode } from 'react';
import { Pressable, StyleSheet, View, ViewStyle } from 'react-native';
import { Avatar, Icon, Text } from 'react-native-paper';
import { IconSource } from 'react-native-paper/lib/typescript/components/Icon';
import { useAppTheme } from '../../custom-theme';
import { fontWeight, radius, spacing, touchTarget } from '../../theme/tokens';
import { raisedSurface } from '../../theme/surface';

/**
 * A row of secondary information under the title, such as the asset a work
 * order belongs to or the location of a part. Rows wrap, so a card with many
 * of them grows rather than truncating.
 */
export interface EntityListCardMeta {
  icon: IconSource;
  label: string;
  /** Defaults to the muted foreground. Set it to flag overdue dates and the like. */
  color?: string;
}

export interface EntityListCardProps {
  title: string;
  /** Secondary identifier, conventionally the human-facing id such as `#1042`. */
  subtitle?: string;
  /** Image URL for the leading avatar. Falls back to `icon` when absent. */
  imageUrl?: string;
  icon?: IconSource;
  /** Top-right pill. Used for status on work orders and for stock state on parts. */
  badge?: { label: string; color: string };
  meta?: EntityListCardMeta[];
  /** Rendered at the bottom of the card, e.g. assignee avatars. */
  footer?: ReactNode;
  onPress?: () => void;
  onLongPress?: () => void;
  /**
   * Overrides the label read aloud. The default concatenates the visible text,
   * which is usually right; supply this when the visuals alone are ambiguous.
   */
  accessibilityLabel?: string;
  style?: ViewStyle;
}

export default function EntityListCard({
  title,
  subtitle,
  imageUrl,
  icon = 'file-document-outline',
  badge,
  meta,
  footer,
  onPress,
  onLongPress,
  accessibilityLabel,
  style
}: EntityListCardProps) {
  const theme = useAppTheme();

  // Screen readers announce a card as one unit, so the pieces are joined here
  // rather than being read as a series of disconnected fragments.
  const derivedLabel =
    accessibilityLabel ??
    [title, subtitle, badge?.label, ...(meta ?? []).map((m) => m.label)]
      .filter(Boolean)
      .join(', ');

  return (
    <Pressable
      onPress={onPress}
      onLongPress={onLongPress}
      accessibilityRole={onPress ? 'button' : undefined}
      accessibilityLabel={derivedLabel}
      style={({ pressed }) => [
        styles.card,
        raisedSurface(theme),
        { opacity: pressed && onPress ? 0.7 : 1 },
        style
      ]}
    >
      <View style={styles.row}>
        {imageUrl ? (
          <Avatar.Image size={48} source={{ uri: imageUrl }} />
        ) : (
          <Avatar.Icon
            size={48}
            icon={icon}
            // `primaryContainer` is a deep indigo in both schemes, so the glyph
            // stays white rather than following the foreground role.
            style={{ backgroundColor: theme.colors.primaryContainer }}
            color={theme.colors.white}
          />
        )}

        <View style={styles.body}>
          <View style={styles.header}>
            <View style={styles.headerText}>
              <Text
                variant="titleMedium"
                numberOfLines={2}
                style={{ fontWeight: fontWeight.semibold, color: theme.colors.text }}
              >
                {title}
              </Text>
              {!!subtitle && (
                <Text variant="bodySmall" style={{ color: theme.colors.grey }}>
                  {subtitle}
                </Text>
              )}
            </View>
            {!!badge && (
              <View style={[styles.badge, { backgroundColor: badge.color }]}>
                <Text
                  variant="labelSmall"
                  style={{ color: theme.colors.white, fontWeight: fontWeight.medium }}
                >
                  {badge.label}
                </Text>
              </View>
            )}
          </View>

          {!!meta?.length && (
            <View style={styles.meta}>
              {meta.map((item, index) => (
                <View key={`${item.label}-${index}`} style={styles.metaItem}>
                  <Icon
                    source={item.icon}
                    size={16}
                    color={item.color ?? theme.colors.grey}
                  />
                  <Text
                    variant="bodySmall"
                    numberOfLines={1}
                    style={{ color: item.color ?? theme.colors.grey, flexShrink: 1 }}
                  >
                    {item.label}
                  </Text>
                </View>
              ))}
            </View>
          )}

          {!!footer && <View style={styles.footer}>{footer}</View>}
        </View>
      </View>
    </Pressable>
  );
}

const styles = StyleSheet.create({
  card: {
    borderRadius: radius.lg,
    padding: spacing.md,
    marginHorizontal: spacing.lg,
    marginBottom: spacing.md,
    minHeight: touchTarget.min
  },
  row: {
    flexDirection: 'row',
    gap: spacing.md
  },
  body: {
    flex: 1,
    gap: spacing.sm
  },
  header: {
    flexDirection: 'row',
    alignItems: 'flex-start',
    gap: spacing.sm
  },
  headerText: {
    flex: 1
  },
  badge: {
    paddingHorizontal: spacing.sm,
    paddingVertical: spacing.xs,
    borderRadius: radius.sm
  },
  meta: {
    // Wrapping keeps a long asset name from squeezing the location off the row.
    flexDirection: 'row',
    flexWrap: 'wrap',
    columnGap: spacing.lg,
    rowGap: spacing.xs
  },
  metaItem: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing.xs,
    flexShrink: 1
  },
  footer: {
    flexDirection: 'row',
    alignItems: 'center'
  }
});
