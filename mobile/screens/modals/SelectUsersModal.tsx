import {
  Pressable,
  RefreshControl,
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
import { UserMiniDTO } from '../../models/user';
import { getUsersMini } from '../../slices/user';
import { Checkbox, Divider, Searchbar, Text, useTheme } from 'react-native-paper';

export default function SelectUsersModal({
                                           navigation,
                                           route
                                         }: RootStackScreenProps<'SelectUsers'>) {
  const { onChange, selected, multiple } = route.params;
  const theme = useTheme();
  const { t }: { t: any } = useTranslation();
  const dispatch = useDispatch();
  const { usersMini, loadingGet } = useSelector((state) => state.users);
  const [selectedUsers, setSelectedUsers] = useState<UserMiniDTO[]>([]);
  const [selectedIds, setSelectedIds] = useState<number[]>([]);
  const [searchQuery, setSearchQuery] = useState<string>('');

  useEffect(() => {
    if (usersMini.length) {
      const newSelectedUsers = selectedIds
        .map((id) => {
          return usersMini.find((user) => user.id == id);
        })
        .filter((user) => !!user);
      setSelectedUsers(newSelectedUsers);
    }
  }, [selectedIds, usersMini]);

  useEffect(() => {
    if (!selectedIds.length) setSelectedIds(selected);
  }, [selected]);

  useEffect(() => {
    if (multiple)
      navigation.setOptions({
        headerRight: () => (
          <Pressable
            disabled={!selectedUsers.length}
            onPress={() => {
              onChange(selectedUsers);
              navigation.goBack();
            }}
          >
            <Text variant='titleMedium'>{t('add')}</Text>
          </Pressable>
        )
      });
  }, [selectedUsers]);

  useEffect(() => {
    dispatch(getUsersMini());
  }, []);

  const onSelect = (ids: number[]) => {
    setSelectedIds(Array.from(new Set([...selectedIds, ...ids])));
    if (!multiple) {
      onChange([usersMini.find((user) => user.id === ids[0])]);
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
    <View style={[styles.container, { backgroundColor: theme.colors.background }]}>
      <Searchbar
        placeholder={t('search')}
        onChangeText={setSearchQuery}
        value={searchQuery}
        style={{ backgroundColor: theme.colors.background }}
      />
      <ScrollView
        refreshControl={
          <RefreshControl
            refreshing={loadingGet}
            onRefresh={() => dispatch(getUsersMini())}
          />
        }
        style={{
          flex: 1,
          backgroundColor: theme.colors.background
        }}
      >
        {usersMini.filter(user => user.firstName.toLowerCase()
            .includes(searchQuery.toLowerCase().trim())
          || user.lastName.toLowerCase().includes(searchQuery.toLowerCase().trim())).map((user) => (
          <TouchableOpacity
            onPress={() => {
              toggle(user.id);
            }}
            key={user.id}
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
                status={selectedIds.includes(user.id) ? 'checked' : 'unchecked'}
                onPress={() => {
                  toggle(user.id);
                }}
              />
            )}
            <Text style={{ flexShrink: 1 }}
                  variant={'titleMedium'}
            >{`${user.firstName} ${user.lastName}`}</Text>
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
