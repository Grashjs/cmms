import { StyleSheet, TouchableOpacity } from 'react-native';

import { Text, View } from '../components/Themed';
import { useTheme } from 'react-native-paper';
import { RootStackScreenProps } from '../types';

export default function NotFoundScreen({
  navigation
}: RootStackScreenProps<'NotFound'>) {
  const theme = useTheme();

  return (
    <View style={styles.container}>
      <Text style={styles.title}>This screen doesn't exist.</Text>
      <TouchableOpacity
        onPress={() => navigation.replace('Root')}
        style={styles.link}
      >
        <Text style={[styles.linkText, { color: theme.colors.primary }]}>Go to home screen!</Text>
      </TouchableOpacity>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    padding: 20
  },
  title: {
    fontSize: 20,
    fontWeight: 'bold'
  },
  link: {
    marginTop: 15,
    paddingVertical: 15
  },
  linkText: {
    fontSize: 14
  }
});
