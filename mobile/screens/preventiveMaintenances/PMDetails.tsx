import * as React from 'react';
import { useContext, useEffect, useState } from 'react';
import { ScrollView, StyleSheet, View } from 'react-native';
import { ActivityIndicator, Switch, Text } from 'react-native-paper';
import { useTranslation } from 'react-i18next';
import { RootStackScreenProps } from '../../types';
import { useDispatch, useSelector } from '../../store';
import {
  clearSinglePM,
  getSinglePreventiveMaintenance,
  patchSchedule
} from '../../slices/preventiveMaintenance';
import { CompanySettingsContext } from '../../contexts/CompanySettingsContext';
import { CustomSnackBarContext } from '../../contexts/CustomSnackBarContext';
import { useAppTheme } from '../../custom-theme';
import { getPriorityColor } from '../../utils/overall';
import { getErrorMessage } from '../../utils/api';
import {
  daysUntil,
  describeFrequency,
  getNextOccurrence
} from '../../utils/schedule';
import { EmptyState, Section } from '../../components/ui';
import { fontWeight, spacing, touchTarget } from '../../theme/tokens';

function Field({ label, value }: { label: string; value?: string | number }) {
  const theme = useAppTheme();
  if (!value) return null;
  return (
    <View>
      <Text style={{ fontSize: 14, color: theme.colors.onSurfaceVariant }}>
        {label}
      </Text>
      <Text variant="titleMedium" style={{ fontWeight: fontWeight.bold }}>
        {value}
      </Text>
    </View>
  );
}

export default function PMDetails({
  navigation,
  route
}: RootStackScreenProps<'PMDetails'>) {
  const { id } = route.params;
  const { t } = useTranslation();
  const theme = useAppTheme();
  const dispatch = useDispatch();
  const { getFormattedDate } = useContext(CompanySettingsContext);
  const { showSnackBar } = useContext(CustomSnackBarContext);
  const { singlePreventiveMaintenance: pm, loadingGet } = useSelector(
    (state) => state.preventiveMaintenances
  );
  const [toggling, setToggling] = useState(false);

  useEffect(() => {
    dispatch(getSinglePreventiveMaintenance(id));
    // Cleared on the way out so the next PM opened does not briefly show this
    // one's details while its own request is in flight.
    return () => {
      dispatch(clearSinglePM());
    };
  }, [id]);

  useEffect(() => {
    if (pm) navigation.setOptions({ title: pm.name || pm.title });
  }, [pm]);

  if (!pm) {
    return loadingGet ? (
      <View style={[styles.centered, { backgroundColor: theme.colors.background }]}>
        <ActivityIndicator color={theme.colors.primary} />
      </View>
    ) : (
      <View style={{ flex: 1, backgroundColor: theme.colors.background }}>
        <EmptyState
          variant="error"
          title={t('not_found')}
          action={{ label: t('retry'), onPress: () => dispatch(getSinglePreventiveMaintenance(id)) }}
        />
      </View>
    );
  }

  const { schedule } = pm;
  const next = getNextOccurrence(schedule);
  const days = next ? daysUntil(next) : null;

  const onToggleSchedule = (enabled: boolean) => {
    if (!schedule) return;
    setToggling(true);
    dispatch(patchSchedule(schedule.id, pm.id, { disabled: !enabled }))
      .catch((error) => showSnackBar(getErrorMessage(error), 'error'))
      .finally(() => setToggling(false));
  };

  return (
    <ScrollView
      style={{ flex: 1, backgroundColor: theme.colors.background }}
      contentContainerStyle={styles.content}
    >
      <Section title={t('schedule')} icon="calendar-sync-outline">
        <View style={styles.toggleRow}>
          <View style={styles.toggleText}>
            <Text variant="titleMedium" style={{ fontWeight: fontWeight.bold }}>
              {describeFrequency(schedule, t)}
            </Text>
            <Text variant="bodySmall" style={{ color: theme.colors.grey }}>
              {schedule?.disabled
                ? t('schedule_paused_description')
                : next
                  ? days === 0
                    ? t('due_today')
                    : t('next_on', {
                        date: getFormattedDate(next.toISOString(), true)
                      })
                  : t('no_upcoming_occurrence')}
            </Text>
          </View>
          <Switch
            value={!schedule?.disabled}
            disabled={!schedule || toggling}
            onValueChange={onToggleSchedule}
            accessibilityLabel={t('schedule')}
          />
        </View>
        <Field
          label={t('starts_on')}
          value={schedule?.startsOn && getFormattedDate(schedule.startsOn, true)}
        />
        <Field
          label={t('ends_on')}
          value={schedule?.endsOn && getFormattedDate(schedule.endsOn, true)}
        />
        <Field
          label={t('due_date_delay')}
          value={
            schedule?.dueDateDelay
              ? t('days_count', { days: schedule.dueDateDelay })
              : undefined
          }
        />
      </Section>

      <Section title={t('work_order_template')} icon="clipboard-text-outline">
        <Field label={t('title')} value={pm.title} />
        <Field label={t('description')} value={pm.description} />
        {pm.priority && pm.priority !== 'NONE' && (
          <View>
            <Text style={{ fontSize: 14, color: theme.colors.onSurfaceVariant }}>
              {t('priority')}
            </Text>
            <Text
              variant="titleMedium"
              style={{
                fontWeight: fontWeight.bold,
                color: getPriorityColor(pm.priority, theme)
              }}
            >
              {t(pm.priority)}
            </Text>
          </View>
        )}
        <Field label={t('asset')} value={pm.asset?.name} />
        <Field label={t('location')} value={pm.location?.name} />
        <Field label={t('category')} value={pm.category?.name} />
        <Field
          label={t('estimated_duration')}
          value={
            pm.estimatedDuration
              ? t('hours_value', { value: pm.estimatedDuration })
              : undefined
          }
        />
      </Section>

      {(pm.primaryUser || !!pm.assignedTo?.length || pm.team) && (
        <Section title={t('people')} icon="account-group-outline">
          <Field
            label={t('primary_worker')}
            value={
              pm.primaryUser &&
              `${pm.primaryUser.firstName} ${pm.primaryUser.lastName}`
            }
          />
          {!!pm.assignedTo?.length && (
            <View>
              <Text
                style={{ fontSize: 14, color: theme.colors.onSurfaceVariant }}
              >
                {t('assigned_to')}
              </Text>
              {pm.assignedTo.map((user) => (
                <Text
                  key={user.id}
                  variant="titleMedium"
                  style={{ fontWeight: fontWeight.bold }}
                >
                  {`${user.firstName} ${user.lastName}`}
                </Text>
              ))}
            </View>
          )}
          <Field label={t('team')} value={pm.team?.name} />
        </Section>
      )}
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  content: {
    paddingVertical: spacing.md,
    paddingBottom: 60
  },
  centered: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center'
  },
  toggleRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing.md,
    minHeight: touchTarget.min
  },
  toggleText: {
    flex: 1
  }
});
