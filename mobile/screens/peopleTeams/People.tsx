import {
  Linking,
  RefreshControl,
  ScrollView,
  StyleSheet,
  TouchableOpacity,
  View
} from 'react-native';
import { useDispatch, useSelector } from '../../store';
import * as React from 'react';
import { useContext, useEffect, useState } from 'react';
import { CompanySettingsContext } from '../../contexts/CompanySettingsContext';
import useAuth from '../../hooks/useAuth';
import { PermissionEntity } from '../../models/role';
import { getMoreUsers, getUsers } from '../../slices/user';
import { FilterField, SearchCriteria } from '../../models/page';
import {
  Avatar,
  Divider,
  IconButton,
  Searchbar,
  Text,
  useTheme
} from 'react-native-paper';
import { useTranslation } from 'react-i18next';
import User from '../../models/user';
import { onSearchQueryChange } from '../../utils/overall';
import { RootStackScreenProps } from '../../types';
import { useDebouncedEffect } from '../../hooks/useDebouncedEffect';
import { getUserInitials } from '../../utils/displayers';

export default function People({
                                 navigation
                               }: RootStackScreenProps<'PeopleTeams'>) {
  const { t } = useTranslation();
  const currentUser = useAuth().user;
  const [startedSearch, setStartedSearch] = useState<boolean>(false);
  const { users, loadingGet, currentPageNum, lastPage } = useSelector(
    (state) => state.users
  );
  const theme = useTheme();
  const dispatch = useDispatch();
  const [searchQuery, setSearchQuery] = useState('');
  const { getFormattedDate, getUserNameById } = useContext(
    CompanySettingsContext
  );
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

  const onRefresh = () => {
    setCriteria(getCriteriaFromFilterFields([]));
  };

  const isCloseToBottom = ({
                             layoutMeasurement,
                             contentOffset,
                             contentSize
                           }) => {
    const paddingToBottom = 20;
    return (
      layoutMeasurement.height + contentOffset.y >=
      contentSize.height - paddingToBottom
    );
  };
  const onQueryChange = (query) => {
    onSearchQueryChange<User>(query, criteria, setCriteria, setSearchQuery, [
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
  return (
    <View
      style={{ ...styles.container, backgroundColor: theme.colors.background }}
    >
      <Searchbar
        placeholder={t('search')}
        onFocus={() => setStartedSearch(true)}
        onChangeText={setSearchQuery}
        value={searchQuery}
        style={{ backgroundColor: theme.colors.background }}
      />
      <ScrollView
        style={styles.scrollView}
        onScroll={({ nativeEvent }) => {
          if (isCloseToBottom(nativeEvent)) {
            if (!loadingGet && !lastPage)
              dispatch(getMoreUsers(criteria, currentPageNum + 1));
          }
        }}
        refreshControl={
          <RefreshControl
            refreshing={loadingGet}
            onRefresh={onRefresh}
            colors={[theme.colors.primary]}
          />
        }
        scrollEventThrottle={400}
      >
        {!!users.content.length ? (
          users.content.map((user) => (
            <TouchableOpacity
              key={user.id}
              onPress={() => {
                if (user.id === currentUser.id) {
                  navigation.navigate('UserProfile');
                } else navigation.push('UserDetails', { id: user.id, userProp: user });
              }}
            >
              <View
                style={{
                  display: 'flex',
                  flexDirection: 'row',
                  padding: 20,
                  alignItems: 'center',
                  justifyContent: 'space-between',
                  backgroundColor: 'white'
                }}
              >
                {user.image ? (
                  <Avatar.Image source={{ uri: user.image.url }} />
                ) : (
                  <Avatar.Text size={50} label={getUserInitials(user)} />
                )}
                <Text>{`${user.firstName} ${user.lastName}`}</Text>
                <View>
                  {user.phone && (
                    <IconButton
                      onPress={() => Linking.openURL(`tel:${user.phone}`)}
                      icon={'phone'}
                    />
                  )}
                </View>
              </View>
              <Divider />
            </TouchableOpacity>
          ))
        ) : loadingGet ? null : (
          <View
            style={{ backgroundColor: 'white', padding: 20, borderRadius: 10 }}
          >
            <Text variant={'titleLarge'}>{t('no_element_match_criteria')}</Text>
          </View>
        )}
      </ScrollView>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center'
  },
  title: {
    fontSize: 20,
    fontWeight: 'bold'
  },
  scrollView: {
    width: '100%',
    height: '100%',
    padding: 5
  },
  row: {
    display: 'flex',
    flexDirection: 'row',
    alignItems: 'center'
  }
});
