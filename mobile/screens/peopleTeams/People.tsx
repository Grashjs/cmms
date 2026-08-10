import { Linking, StyleSheet, View } from 'react-native';
import { useDispatch, useSelector } from '../../store';
import * as React from 'react';
import { useCallback, useEffect, useState } from 'react';
import useAuth from '../../hooks/useAuth';
import { PermissionEntity } from '../../models/role';
import { getMoreUsers, getUsers } from '../../slices/user';
import { FilterField, SearchCriteria } from '../../models/page';
import { IconButton, Searchbar } from 'react-native-paper';
import { useTranslation } from 'react-i18next';
import { OwnUser } from '../../models/user';
import { onSearchQueryChange } from '../../utils/overall';
import { RootStackScreenProps } from '../../types';
import { useDebouncedEffect } from '../../hooks/useDebouncedEffect';
import { useAppTheme } from '../../custom-theme';
import {
  EmptyState,
  EntityListCard,
  PaginatedEntityList
} from '../../components/ui';

export default function People({
  navigation
}: RootStackScreenProps<'PeopleTeams'>) {
  const { t } = useTranslation();
  const currentUser = useAuth().user;
  const [startedSearch, setStartedSearch] = useState<boolean>(false);
  const { users, loadingGet, currentPageNum, lastPage } = useSelector(
    (state) => state.users
  );
  const theme = useAppTheme();
  const dispatch = useDispatch();
  const [searchQuery, setSearchQuery] = useState('');
  const [refreshing, setRefreshing] = useState(false);
  const { hasViewPermission } = useAuth();
  const defaultFilterFields: FilterField[] = [];
  const getCriteriaFromFilterFields = (filterFields: FilterField[]) => {
    const initialCriteria: SearchCriteria = {
      filterFields: defaultFilterFields,
      pageSize: 10,
      pageNum: 0,
      direction: 'DESC'
    };
    let newFilterFields = [...initialCriteria.filterFields];
    filterFields.forEach(
      (filterField) =>
        (newFilterFields = newFilterFields.filter(
          (ff) => ff.field != filterField.field
        ))
    );
    return {
      ...initialCriteria,
      filterFields: [...newFilterFields, ...filterFields]
    };
  };
  const [criteria, setCriteria] = useState<SearchCriteria>(
    getCriteriaFromFilterFields([])
  );

  useEffect(() => {
    if (hasViewPermission(PermissionEntity.PEOPLE_AND_TEAMS)) {
      dispatch(
        getUsers({ ...criteria, pageSize: 10, pageNum: 0, direction: 'DESC' })
      );
    }
  }, [criteria]);

  useEffect(() => {
    if (!loadingGet) setRefreshing(false);
  }, [loadingGet]);

  const onRefresh = () => {
    setRefreshing(true);
    setCriteria(getCriteriaFromFilterFields([]));
  };

  const onQueryChange = (query) => {
    onSearchQueryChange<OwnUser>(query, criteria, setCriteria, setSearchQuery, [
      'firstName',
      'lastName',
      'phone',
      'email',
      'jobTitle'
    ]);
  };
  useDebouncedEffect(
    () => {
      if (startedSearch) onQueryChange(searchQuery);
    },
    [searchQuery],
    1000
  );

  const isFiltered = !!searchQuery;
  const renderItem = useCallback(
    ({ item: user }: { item: OwnUser }) => {
      const meta = [];
      if (user.email) {
        meta.push({ icon: 'email-outline' as const, label: user.email });
      }
      if (user.phone) {
        meta.push({ icon: 'phone-outline' as const, label: user.phone });
      }
      return (
        <EntityListCard
          title={`${user.firstName} ${user.lastName}`}
          imageUrl={user.image?.url}
          icon="account-outline"
          meta={meta}
          onPress={() => {
            if (user.id === currentUser.id) {
              navigation.navigate('UserProfile');
            } else {
              navigation.push('UserDetails', { id: user.id, userProp: user });
            }
          }}
          footer={
            user.phone ? (
              <IconButton
                icon="phone"
                iconColor={theme.colors.primary}
                accessibilityLabel={t('phone')}
                onPress={() => Linking.openURL(`tel:${user.phone}`)}
              />
            ) : undefined
          }
        />
      );
    },
    [currentUser.id, navigation, t, theme.colors.primary]
  );

  return (
    <View style={[styles.container, { backgroundColor: theme.colors.background }]}>
      <Searchbar
        placeholder={t('search')}
        accessibilityLabel={t('search')}
        onFocus={() => setStartedSearch(true)}
        onChangeText={setSearchQuery}
        value={searchQuery}
        style={{ backgroundColor: theme.colors.card }}
      />
      <PaginatedEntityList
        data={users.content}
        keyExtractor={(user) => user.id.toString()}
        renderItem={renderItem}
        loading={loadingGet}
        refreshing={refreshing}
        onRefresh={onRefresh}
        onEndReached={() => {
          if (!loadingGet && !lastPage)
            dispatch(getMoreUsers(criteria, currentPageNum + 1));
        }}
        ListEmptyComponent={
          <EmptyState
            icon={isFiltered ? 'filter-remove-outline' : 'account-group-outline'}
            title={t('no_element_match_criteria')}
            description={
              isFiltered ? t('no_element_match_criteria_description') : undefined
            }
            action={
              isFiltered
                ? {
                    label: t('reset'),
                    onPress: () => {
                      setSearchQuery('');
                      setCriteria(getCriteriaFromFilterFields([]));
                    }
                  }
                : undefined
            }
          />
        }
      />
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1
  }
});
