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
import { TeamMiniDTO } from '../../models/team';
import { getTeamsMini } from '../../slices/team';
import { Checkbox, Divider, Text, useTheme } from 'react-native-paper';

export default function SelectTeamsModal({
                                           navigation,
                                           route
                                         }: RootStackScreenProps<'SelectTeams'>) {
  const { onChange, selected, multiple } = route.params;
  const theme = useTheme();
  const { t }: { t: any } = useTranslation();
  const dispatch = useDispatch();
  const { teamsMini, loadingGet } = useSelector((state) => state.teams);
  const [selectedTeams, setSelectedTeams] = useState<TeamMiniDTO[]>([]);
  const [selectedIds, setSelectedIds] = useState<number[]>([]);

  useEffect(() => {
    if (teamsMini.length) {
      const newSelectedTeams = selectedIds
        .map((id) => {
          return teamsMini.find((team) => team.id == id);
        })
        .filter((team) => !!team);
      setSelectedTeams(newSelectedTeams);
    }
  }, [selectedIds, teamsMini]);

  useEffect(() => {
    if (!selectedIds.length) setSelectedIds(selected);
  }, [selected]);

  useEffect(() => {
    if (multiple)
      navigation.setOptions({
        headerRight: () => (
          <Pressable
            disabled={!selectedTeams.length}
            onPress={() => {
              onChange(selectedTeams);
              navigation.goBack();
            }}
          >
            <Text variant='titleMedium'>{t('add')}</Text>
          </Pressable>
        )
      });
  }, [selectedTeams]);

  useEffect(() => {
    dispatch(getTeamsMini());
  }, []);

  const onSelect = (ids: number[]) => {
    setSelectedIds(Array.from(new Set([...selectedIds, ...ids])));
    if (!multiple) {
      onChange([teamsMini.find((team) => team.id === ids[0])]);
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
        refreshControl={
          <RefreshControl
            refreshing={loadingGet}
            onRefresh={() => dispatch(getTeamsMini())}
          />
        }
        style={{
          flex: 1,
          backgroundColor: theme.colors.background
        }}
      >
        {teamsMini.map((team) => (
          <TouchableOpacity
            onPress={() => {
              toggle(team.id);
            }}
            key={team.id}
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
                status={selectedIds.includes(team.id) ? 'checked' : 'unchecked'}
                onPress={() => {
                  toggle(team.id);
                }}
              />
            )}
            <Text style={{ flexShrink: 1 }} variant={'titleMedium'}>{team.name}</Text>
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
