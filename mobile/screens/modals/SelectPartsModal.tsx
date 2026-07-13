import {
  Pressable,
  ScrollView,
  StyleSheet,
  useWindowDimensions
} from 'react-native';
import { View } from '../../components/Themed';
import { RootStackScreenProps } from '../../types';
import { useTranslation } from 'react-i18next';
import * as React from 'react';
import { useContext, useEffect, useState } from 'react';
import { TabBar, TabView } from 'react-native-tab-view';
import { useDispatch, useSelector } from '../../store';
import { PartMiniDTO } from '../../models/part';
import { getPartsMini } from '../../slices/part';
import {
  ActivityIndicator,
  Button,
  Checkbox, Searchbar,
  Text, TextInput,
  useTheme
} from 'react-native-paper';
import { CompanySettingsContext } from '../../contexts/CompanySettingsContext';
import { getMultiParts, getMultiPartsMini } from '../../slices/multipart';
import SetType from '../../models/setType';

const PartsRoute = ({
                      toggle,
                      partsMini,
                      navigation,
                      selectedIds
                    }: {
  toggle: (id: number) => void;
  partsMini: PartMiniDTO[];
  selectedIds: number[];
  navigation: any;
}) => {
  const { getFormattedCurrency } = useContext(CompanySettingsContext);
  const { t } = useTranslation();
  const theme = useTheme();
  const [searchQuery, setSearchQuery] = useState<string>('');
  return (
    <View style={{ flex: 1 }}>
      <Searchbar
        placeholder={t('search')}
        onChangeText={setSearchQuery}
        value={searchQuery}
        style={{ backgroundColor: theme.colors.background }}

      />

      <ScrollView style={{ flex: 1 }}>
        {partsMini.filter(part => part.name.toLowerCase().includes(searchQuery.toLowerCase().trim())).map((part) => (
          <View
            key={part.id}
            style={{
              padding: 10,
              display: 'flex',
              borderRadius: 5,
              flexDirection: 'row',
              elevation: 2,
              justifyContent: 'space-between'
            }}
          >
            <Checkbox
              status={selectedIds.includes(part.id) ? 'checked' : 'unchecked'}
              onPress={() => {
                toggle(part.id);
              }}
            />
            <View
              style={{ display: 'flex', flexDirection: 'column', width: '50%' }}
            >
              <Text variant={'labelMedium'}>{part.name}</Text>
              <Text variant={'bodyMedium'}>
                {getFormattedCurrency(part.cost)}
              </Text>
            </View>
            <Button
              style={{ width: '40%' }}
              mode='outlined'
              buttonColor={'white'}
              onPress={() => navigation.navigate('PartDetails', { id: part.id })}
            >
              {t('details')}
            </Button>
          </View>
        ))}
      </ScrollView>
    </View>
  );
};

const SetsRoute = ({
                     toggle,
                     multiParts,
                     selectedIds
                   }: {
  toggle: (multiPart: SetType, checked: boolean) => void;
  multiParts: SetType[];
  selectedIds: number[];
}) => {
  const { getFormattedCurrency } = useContext(CompanySettingsContext);
  const { t } = useTranslation();
  const theme = useTheme();
  const [searchQuery, setSearchQuery] = useState<string>('');
  const selectedMultiParts = multiParts
    .filter((multiPart) =>
      multiPart.parts.every((part) => selectedIds.includes(part.id))
    )
    .map((multiPart) => multiPart.id);
  return (
    <View style={{ flex: 1 }}>
      <Searchbar
        placeholder={t('search')}
        onChangeText={setSearchQuery}
        value={searchQuery}
        style={{ backgroundColor: theme.colors.background }}
      />
      <ScrollView style={{ flex: 1 }}>
        {multiParts.filter(multiPart => multiPart.name.toLowerCase().includes(searchQuery.toLowerCase().trim())).map((multiPart) => (
          <View
            key={multiPart.id}
            style={{
              padding: 10,
              display: 'flex',
              flexDirection: 'row',
              elevation: 2,
              justifyContent: 'space-between'
            }}
          >
            <Checkbox
              status={
                selectedMultiParts.includes(multiPart.id)
                  ? 'checked'
                  : 'unchecked'
              }
              onPress={() => {
                toggle(multiPart, selectedMultiParts.includes(multiPart.id));
              }}
            />
            <Text style={{ flexShrink: 1 }} variant={'labelMedium'}>{multiPart.name}</Text>
            <Button
              style={{ width: '40%' }}
              mode='outlined'
              buttonColor={'white'}
            >
              {t('details')}
            </Button>
          </View>
        ))}
      </ScrollView>
    </View>
  );
};
export default function SelectParts({
                                      navigation,
                                      route
                                    }: RootStackScreenProps<'SelectParts'>) {
  const { onChange, selected } = route.params;
  const theme = useTheme();
  const { t }: { t: any } = useTranslation();
  const dispatch = useDispatch();
  const { partsMini, loadingGet } = useSelector((state) => state.parts);
  const { miniMultiParts: multiParts, loadingMultiparts } = useSelector(
    (state) => state.multiParts
  );
  const [tabIndex, setTabIndex] = useState(0);
  const [selectedParts, setSelectedParts] = useState<PartMiniDTO[]>([]);
  const [selectedIds, setSelectedIds] = useState<number[]>([]);
  const [openModal, setOpenModal] = useState<boolean>(false);
  const layout = useWindowDimensions();
  const [tabs] = useState([
    { key: 'parts', title: t('parts') },
    { key: 'sets', title: t('sets_of_parts') }
  ]);
  useEffect(() => {
    if (partsMini.length) {
      const newSelectedParts = selectedIds
        .map((id) => {
          return partsMini.find((part) => part.id == id);
        })
        .filter((part) => !!part);
      setSelectedParts(newSelectedParts);
    }
  }, [selectedIds, partsMini]);

  useEffect(() => {
    if (!selectedIds.length) setSelectedIds(selected);
  }, [selected]);

  useEffect(() => {
    navigation.setOptions({
      headerRight: () => (
        <Pressable
          disabled={!selectedParts.length}
          onPress={() => {
            onChange(selectedParts);
            navigation.goBack();
          }}
        >
          <Text variant='titleMedium'>{t('add')}</Text>
        </Pressable>
      )
    });
  }, [selectedParts]);

  useEffect(() => {
    dispatch(getPartsMini());
    dispatch(getMultiPartsMini());
  }, []);

  const onSelect = (ids: number[]) => {
    setSelectedIds(Array.from(new Set([...selectedIds, ...ids])));
  };
  const onUnSelect = (ids: number[]) => {
    const newSelectedIds = selectedIds.filter((id) => !ids.includes(id));
    setSelectedIds(newSelectedIds);
  };
  const toggle = (id: number) => {
    if (selectedIds.includes(id)) {
      onUnSelect([id]);
    } else {
      onSelect([id]);
    }
  };
  const toggleMultipart = (multiPart: SetType, checked: boolean) => {
    if (checked) {
      onSelect(multiPart.parts.map((part) => part.id));
    } else onUnSelect(multiPart.parts.map((part) => part.id));
  };
  const renderScene = ({ route, jumpTo }) => {
    switch (route.key) {
      case 'parts':
        return (
          <PartsRoute
            toggle={toggle}
            navigation={navigation}
            partsMini={partsMini}
            selectedIds={selectedIds}
          />
        );
      case 'sets':
        return (
          <SetsRoute
            toggle={toggleMultipart}
            multiParts={multiParts}
            selectedIds={selectedIds}
          />
        );
    }
  };
  const renderTabBar = (props) => (
    <TabBar
      {...props}
      indicatorStyle={{ backgroundColor: 'white' }}
      style={{ backgroundColor: theme.colors.primary }}
    />
  );
  return (
    <View
      style={{ ...styles.container, backgroundColor: theme.colors.background }}
    >
      {((loadingGet && tabIndex === 0) ||
        (loadingMultiparts && tabIndex === 1)) && (
        <ActivityIndicator
          style={{ position: 'absolute', top: '45%', left: '45%', zIndex: 10 }}
          size='large'
        />
      )}
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
