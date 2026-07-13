import {
  Pressable,
  ScrollView,
  StyleSheet,
  TouchableOpacity
} from 'react-native';
import { View } from '../../components/Themed';
import { RootStackScreenProps } from '../../types';
import { useTranslation } from 'react-i18next';
import * as React from 'react';
import { useEffect, useState } from 'react';
import { useDispatch, useSelector } from '../../store';
import Category from '../../models/category';
import { getCategories } from '../../slices/category';
import { Checkbox, Divider, Text, useTheme } from 'react-native-paper';

export default function SelectCategoriesModal({
                                                navigation,
                                                route
                                              }: RootStackScreenProps<'SelectCategories'>) {
  const { onChange, selected, multiple, type } = route.params;
  const theme = useTheme();
  const { t }: { t: any } = useTranslation();
  const dispatch = useDispatch();
  const { categories } = useSelector((state) => state.categories);
  const [selectedCategories, setSelectedCategories] = useState<Category[]>([]);
  const [selectedIds, setSelectedIds] = useState<number[]>([]);
  const currentCategories = categories[type] ?? [];
  useEffect(() => {
    if (currentCategories.length) {
      const newSelectedCategories = selectedIds
        .map((id) => {
          return currentCategories.find((category) => category.id == id);
        })
        .filter((category) => !!category);
      setSelectedCategories(newSelectedCategories);
    }
  }, [selectedIds, currentCategories]);

  useEffect(() => {
    if (!selectedIds.length) setSelectedIds(selected);
  }, [selected]);

  useEffect(() => {
    if (multiple)
      navigation.setOptions({
        headerRight: () => (
          <Pressable
            disabled={!selectedCategories.length}
            onPress={() => {
              onChange(selectedCategories);
              navigation.goBack();
            }}
          >
            <Text variant='titleMedium'>{t('add')}</Text>
          </Pressable>
        )
      });
  }, [selectedCategories]);

  useEffect(() => {
    dispatch(getCategories(type));
  }, []);

  const onSelect = (ids: number[]) => {
    setSelectedIds(Array.from(new Set([...selectedIds, ...ids])));
    if (!multiple) {
      onChange([currentCategories.find((category) => category.id === ids[0])]);
      navigation.goBack();
    }
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

  return (
    <View style={styles.container}>
      <ScrollView
        // refreshControl={
        //   <RefreshControl refreshing={loadingGet} onRefresh={() => dispatch(getCategories())} />}
        style={{
          flex: 1,
          backgroundColor: theme.colors.background
        }}
      >
        {currentCategories.map((category) => (
          <TouchableOpacity
            onPress={() => {
              toggle(category.id);
            }}
            key={category.id}
            style={{
              borderRadius: 5,
              padding: 15,
              backgroundColor: 'white',
              display: 'flex',
              flexDirection: 'row',
              elevation: 2,
              alignItems: 'center'
            }}
          >
            {multiple && (
              <Checkbox
                status={
                  selectedIds.includes(category.id) ? 'checked' : 'unchecked'
                }
                onPress={() => {
                  toggle(category.id);
                }}
              />
            )}
            <Text variant={'titleMedium'}>{category.name}</Text>
            <Divider />
          </TouchableOpacity>
        ))}
      </ScrollView>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1
  }
});
