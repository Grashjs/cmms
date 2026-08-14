import * as React from 'react';
import { Component, ReactNode } from 'react';
import { StyleSheet, View } from 'react-native';
import { Button, Text } from 'react-native-paper';
import { useTranslation } from 'react-i18next';
import { useAppTheme } from '../custom-theme';
import { spacing } from '../theme/tokens';

interface Props {
  children: ReactNode;
}

interface State {
  hasError: boolean;
}

/**
 * Catches render-time failures anywhere beneath the root layout. Without this,
 * a single bad screen takes down the whole app with a white screen.
 */
export default class ErrorBoundary extends Component<Props, State> {
  state: State = { hasError: false };

  static getDerivedStateFromError() {
    return { hasError: true };
  }

  componentDidCatch(error: Error) {
    console.error('ErrorBoundary caught:', error);
  }

  private onRetry = () => {
    this.setState({ hasError: false });
  };

  render() {
    if (this.state.hasError) {
      return <ErrorFallback onRetry={this.onRetry} />;
    }
    return this.props.children;
  }
}

function ErrorFallback({ onRetry }: { onRetry: () => void }) {
  const { t } = useTranslation();
  const theme = useAppTheme();

  return (
    <View
      style={[styles.container, { backgroundColor: theme.colors.background }]}
    >
      <Text variant="headlineSmall" style={{ color: theme.colors.text }}>
        {t('error_boundary_title')}
      </Text>
      <Text
        variant="bodyMedium"
        style={[styles.description, { color: theme.colors.grey }]}
      >
        {t('error_boundary_description')}
      </Text>
      <Button mode="contained" onPress={onRetry}>
        {t('try_again')}
      </Button>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    padding: spacing.xxxl,
    gap: spacing.md
  },
  description: {
    textAlign: 'center'
  }
});
