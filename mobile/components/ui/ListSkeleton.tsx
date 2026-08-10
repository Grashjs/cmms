import * as React from 'react';
import { useEffect, useRef } from 'react';
import { Animated, Easing, StyleSheet, View } from 'react-native';
import { useAppTheme } from '../../custom-theme';
import { elevation, radius, spacing } from '../../theme/tokens';

export interface ListSkeletonProps {
  /** Number of placeholder rows. Roughly one screenful is usually right. */
  count?: number;
}

/**
 * Placeholder rows shown while a list loads for the first time.
 *
 * This replaces the centered spinner the screens used, which gave no hint of
 * what was arriving and made the layout jump once content replaced it. The
 * shapes deliberately mirror `EntityListCard` so the transition is a fill-in
 * rather than a reflow.
 */
export default function ListSkeleton({ count = 6 }: ListSkeletonProps) {
  const theme = useAppTheme();
  const pulse = useRef(new Animated.Value(0)).current;

  useEffect(() => {
    const animation = Animated.loop(
      Animated.sequence([
        Animated.timing(pulse, {
          toValue: 1,
          duration: 700,
          easing: Easing.inOut(Easing.ease),
          useNativeDriver: true
        }),
        Animated.timing(pulse, {
          toValue: 0,
          duration: 700,
          easing: Easing.inOut(Easing.ease),
          useNativeDriver: true
        })
      ])
    );
    animation.start();
    // Leaving the loop running after unmount keeps a timer alive for a list
    // that is no longer on screen.
    return () => animation.stop();
  }, [pulse]);

  const opacity = pulse.interpolate({
    inputRange: [0, 1],
    outputRange: [0.35, 0.7]
  });

  const blockColor = theme.dark ? '#3a3d4a' : '#d9dbe6';

  return (
    <View
      accessibilityRole="progressbar"
      accessibilityLabel="Loading"
      style={styles.container}
    >
      {Array.from({ length: count }).map((_, index) => (
        <View
          key={index}
          style={[
            styles.card,
            elevation.card,
            {
              backgroundColor: theme.colors.card,
              shadowColor: theme.dark ? 'transparent' : '#000'
            }
          ]}
        >
          <Animated.View
            style={[
              styles.avatar,
              { backgroundColor: blockColor, opacity }
            ]}
          />
          <View style={styles.body}>
            <Animated.View
              style={[
                styles.line,
                { width: '70%', backgroundColor: blockColor, opacity }
              ]}
            />
            <Animated.View
              style={[
                styles.line,
                { width: '40%', backgroundColor: blockColor, opacity }
              ]}
            />
            <Animated.View
              style={[
                styles.line,
                { width: '55%', backgroundColor: blockColor, opacity }
              ]}
            />
          </View>
        </View>
      ))}
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    paddingTop: spacing.md
  },
  card: {
    flexDirection: 'row',
    gap: spacing.md,
    borderRadius: radius.lg,
    padding: spacing.md,
    marginHorizontal: spacing.lg,
    marginBottom: spacing.md
  },
  avatar: {
    width: 48,
    height: 48,
    borderRadius: radius.pill
  },
  body: {
    flex: 1,
    gap: spacing.sm,
    justifyContent: 'center'
  },
  line: {
    height: 10,
    borderRadius: radius.sm
  }
});
