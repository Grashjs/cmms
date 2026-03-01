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
import {
  Avatar,
  Checkbox,
  Divider,
  Searchbar,
  Text,
  useTheme
} from 'react-native-paper';
import { getUserInitials } from '../../utils/displayers';

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
            <Text variant="titleMedium">{t('add')}</Text>
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
    <View
      style={[styles.container, { backgroundColor: theme.colors.background }]}
    >
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
        {usersMini
          .filter(
            (user) =>
              user.firstName
                .toLowerCase()
                .includes(searchQuery.toLowerCase().trim()) ||
              user.lastName
                .toLowerCase()
                .includes(searchQuery.toLowerCase().trim())
          )
          .map((user) => (
            <TouchableOpacity
              onPress={() => {
                toggle(user.id);
              }}
              key={user.id}
            >
              <View style={styles.card}>
                <View style={styles.cardRow}>
                  {user.image ? (
                    <Avatar.Image size={50} source={{ uri: user.image.url }} />
                  ) : (
                    <Avatar.Text
                      size={50}
                      label={getUserInitials(user)}
                      style={{ backgroundColor: theme.colors.primaryContainer }}
                    />
                  )}
                  <View style={{ flex: 1 }}>
                    <View style={styles.cardHeader}>
                      <View style={{ flex: 1 }}>
                        <Text variant="titleMedium" style={styles.cardTitle}>
                          {`${user.firstName} ${user.lastName}`}
                        </Text>
                        <Text
                          variant={'bodySmall'}
                          style={{ color: 'grey' }}
                        >{`#${user.id}`}</Text>
                      </View>
                      {multiple && (
                        <Checkbox
                          status={
                            selectedIds.includes(user.id)
                              ? 'checked'
                              : 'unchecked'
                          }
                          onPress={() => {
                            toggle(user.id);
                          }}
                        />
                      )}
                    </View>
                  </View>
                </View>
              </View>
            </TouchableOpacity>
          ))}
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
  }
});
