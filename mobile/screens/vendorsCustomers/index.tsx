import { View } from '../../components/Themed';
import { StyleSheet, useWindowDimensions } from 'react-native';
import { RootStackScreenProps } from '../../types';
import * as React from 'react';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { TabBar, TabView } from 'react-native-tab-view';
import CustomersScreen from './CustomersScreen';
import VendorsScreen from './VendorsScreen';
import { useTheme } from 'react-native-paper';

export default function VendorsCustomers(
  props: RootStackScreenProps<'VendorsCustomers'>
) {
  const [tabIndex, setTabIndex] = useState(0);
  const { t } = useTranslation();
  const theme = useTheme();
  const layout = useWindowDimensions();
  const [tabs] = useState([
    { key: 'vendors', title: t('vendors') },
    { key: 'customers', title: t('customers') }
  ]);
  const renderScene = ({ route, jumpTo }) => {
    switch (route.key) {
      case 'customers':
        return <CustomersScreen {...props} />;
      case 'vendors':
        return <VendorsScreen {...props} />;
    }
  };
  const renderTabBar = (props) => (
    <TabBar
      {...props}
      indicatorStyle={{ backgroundColor: theme.colors.onPrimary }}
      style={{ backgroundColor: theme.colors.primary }}
    />
  );
  return (
    <View style={styles.container}>
      <TabView
        renderTabBar={renderTabBar}
        navigationState={{ index: tabIndex, routes: tabs }}
        renderScene={renderScene}
        onIndexChange={setTabIndex}
        initialLayout={{ width: layout.width }}
      />
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1
  }
});
