import * as React from 'react';
import { useEffect } from 'react';
import { useDispatch, useSelector } from '../../../store';
import { useTranslation } from 'react-i18next';
import Location from '../../../models/location';
import { ScrollView, StyleSheet, TouchableOpacity } from 'react-native';
import { Divider, Text, useTheme } from 'react-native-paper';
import { View } from '../../../components/Themed';
import { getAssetsByLocation } from '../../../slices/asset';
import Tag from '../../../components/Tag';

export default function LocationAssets({
                                         location,
                                         navigation
                                       }: {
  location: Location;
  navigation: any;
}) {
  const { t }: { t: any } = useTranslation();
  const dispatch = useDispatch();
  const { assetsByLocation } = useSelector((state) => state.assets);
  const assets = assetsByLocation[location.id] ?? [];
  const theme = useTheme();

  useEffect(() => {
    dispatch(getAssetsByLocation(location.id));
  }, []);

  return (
    <ScrollView
      style={{ ...styles.container, backgroundColor: theme.colors.background }}
    >
      {assets.map((asset) => (
        <TouchableOpacity
          key={asset.id}
          onPress={() => navigation.push('AssetDetails', { id: asset.id })}
        >
          <View
            style={{
              display: 'flex',
              flexDirection: 'row',
              justifyContent: 'space-between',
              padding: 20,
              alignItems: 'center'
            }}
          >
            <Text style={{ fontWeight: 'bold', flexShrink: 1, marginRight: 10 }}>{asset.name}</Text>
            <Tag
              text={
                asset?.status === 'OPERATIONAL'
                  ? t('operational')
                  : t('down')
              }
              backgroundColor={
                asset.status === 'OPERATIONAL'
                  ? //@ts-ignore
                  theme.colors.success
                  : theme.colors.error
              }
              color='white'
            />
          </View>
          <Divider />
        </TouchableOpacity>
      ))}
    </ScrollView>
  );
}
const styles = StyleSheet.create({
  container: {
    flex: 1
  }
});
