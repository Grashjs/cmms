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
import { getCategories, addCategory } from '../../slices/category';
import {
  Avatar,
  Button,
  Checkbox,
  Divider,
  Text,
  TextInput,
  useTheme
} from 'react-native-paper';

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
  const [newCategoryName, setNewCategoryName] = useState<string>('');
  const [showCreateInput, setShowCreateInput] = useState<boolean>(false);
  const [isCreating, setIsCreating] = useState<boolean>(false);
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
            <Text variant="titleMedium">{t('add')}</Text>
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

  const handleCreateCategory = async () => {
    console.log('Creating category with name:', newCategoryName, type);
    if (!newCategoryName.trim()) return;
    setIsCreating(true);
    try {
      const createdCategory = await dispatch(
        addCategory({ name: newCategoryName.trim() }, type)
      );
      setSelectedIds((prev) => [...prev, createdCategory.id]);
      if (!multiple) {
        onChange([createdCategory]);
        navigation.goBack();
      }
      setNewCategoryName('');
      setShowCreateInput(false);
    } catch (err) {
      console.error('Error creating category:', err);
      setNewCategoryName('');
      setShowCreateInput(false);
    } finally {
      setIsCreating(false);
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
          >
            <View style={styles.card}>
              <View style={styles.cardRow}>
                <Avatar.Icon
                  size={50}
                  icon="shape-outline"
                  style={{ backgroundColor: theme.colors.primaryContainer }}
                />
                <View style={{ flex: 1 }}>
                  <View style={styles.cardHeader}>
                    <View style={{ flex: 1 }}>
                      <Text variant="titleMedium" style={styles.cardTitle}>
                        {category.name}
                      </Text>
                      <Text
                        variant={'bodySmall'}
                        style={{ color: 'grey' }}
                      >{`#${category.id}`}</Text>
                    </View>
                    {multiple && (
                      <Checkbox
                        status={
                          selectedIds.includes(category.id)
                            ? 'checked'
                            : 'unchecked'
                        }
                        onPress={() => {
                          toggle(category.id);
                        }}
                      />
                    )}
                  </View>
                </View>
              </View>
            </View>
          </TouchableOpacity>
        ))}
        <Divider />
        {showCreateInput ? (
          <View style={styles.createContainer}>
            <TextInput
              mode="outlined"
              placeholder={t('name')}
              value={newCategoryName}
              onChangeText={setNewCategoryName}
              style={styles.createInput}
              disabled={isCreating}
              autoFocus
            />
            <View style={styles.createActions}>
              <Button
                mode="text"
                onPress={() => {
                  setNewCategoryName('');
                  setShowCreateInput(false);
                }}
                disabled={isCreating}
              >
                {t('cancel')}
              </Button>
              <Button
                mode="contained"
                onPress={handleCreateCategory}
                loading={isCreating}
                disabled={isCreating || !newCategoryName.trim()}
              >
                {t('save')}
              </Button>
            </View>
          </View>
        ) : (
          <Button
            icon={'plus-circle'}
            style={{ margin: 20 }}
            mode={'contained'}
            onPress={() => setShowCreateInput(true)}
          >
            {t('create_category')}
          </Button>
        )}
      </ScrollView>
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
  },
  createContainer: {
    padding: 20
  },
  createInput: {
    marginBottom: 10
  },
  createActions: {
    flexDirection: 'row',
    justifyContent: 'flex-end',
    gap: 10
  }
});
