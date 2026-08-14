import { SafeAreaView } from 'react-native-safe-area-context';

export function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <SafeAreaView style={{ flex: 1 }} edges={['bottom']}>
      {children}
    </SafeAreaView>
  );
}
