import {
  Pressable,
  ScrollView,
  StyleSheet,
  TouchableOpacity,
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
  Avatar,
  Button,
  Checkbox,
  Searchbar,
  Text,
  TextInput,
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
        {partsMini
          .filter((part) =>
            part.name.toLowerCase().includes(searchQuery.toLowerCase().trim())
          )
          .map((part) => (
            <Pressable
              key={part.id}
              onPress={() => {
                toggle(part.id);
              }}
            >
              <View style={styles.card}>
                <View style={styles.cardRow}>
                  <Checkbox
                    status={
                      selectedIds.includes(part.id) ? 'checked' : 'unchecked'
                    }
                  />
                  <Avatar.Icon
                    size={50}
                    icon="archive-outline"
                    style={{ backgroundColor: theme.colors.primaryContainer }}
                  />
                  <View style={{ flex: 1 }}>
                    <View style={styles.cardHeader}>
                      <View style={{ flex: 1 }}>
                        <Text variant="titleMedium" style={styles.cardTitle}>
                          {part.name}
                        </Text>
                        <Text variant={'bodyMedium'} style={{ color: 'grey' }}>
                          {getFormattedCurrency(part.cost)}
                        </Text>
                      </View>
                      <View
                        style={{ flexDirection: 'row', alignItems: 'center' }}
                      >
                        <Button
                          mode="text"
                          buttonColor={'white'}
                          onPress={() =>
                            navigation.navigate('PartDetails', { id: part.id })
                          }
                          style={{ marginRight: 8 }}
                        >
                          {t('details')}
                        </Button>
                      </View>
                    </View>
                  </View>
                </View>
              </View>
            </Pressable>
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
    .filter(
      (multiPart) =>
        multiPart.parts.length > 0 &&
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
        {multiParts
          .filter((multiPart) =>
            multiPart.name
              .toLowerCase()
              .includes(searchQuery.toLowerCase().trim())
          )
          .map((multiPart) => (
            <TouchableOpacity
              key={multiPart.id}
              onPress={() => {
                toggle(multiPart, selectedMultiParts.includes(multiPart.id));
              }}
            >
              <View style={styles.card}>
                <View style={styles.cardRow}>
                  <Avatar.Icon
                    size={50}
                    icon="archive-outline"
                    style={{ backgroundColor: theme.colors.primaryContainer }}
                  />
                  <View style={{ flex: 1 }}>
                    <View style={styles.cardHeader}>
                      <View style={{ flex: 1 }}>
                        <Text variant="titleMedium" style={styles.cardTitle}>
                          {multiPart.name}
                        </Text>
                      </View>
                      <View
                        style={{ flexDirection: 'row', alignItems: 'center' }}
                      >
                        <Checkbox
                          status={
                            selectedMultiParts.includes(multiPart.id)
                              ? 'checked'
                              : 'unchecked'
                          }
                        />
                      </View>
                    </View>
                  </View>
                </View>
              </View>
            </TouchableOpacity>
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
          <Text variant="titleMedium">{t('add')}</Text>
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
  const toggleMultipart = (multiPart: SetType, isCurrentlyChecked: boolean) => {
    if (isCurrentlyChecked) {
      onUnSelect(multiPart.parts.map((part) => part.id));
    } else onSelect(multiPart.parts.map((part) => part.id));
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
          size="large"
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
  },
  card: {
    backgroundColor: 'white',
    marginBottom: 1,
    padding: 10
  },
  cardRow: {
    display: 'flex',
    flexDirection: 'row',
    gap: 6,
    alignItems: 'center'
  },
  cardHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center'
  },
  cardTitle: {
    fontWeight: 'bold',
    flexShrink: 1
  }
});
