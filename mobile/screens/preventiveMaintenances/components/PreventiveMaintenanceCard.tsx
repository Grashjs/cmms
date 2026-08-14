import * as React from 'react';
import { useContext } from 'react';
import { useTranslation } from 'react-i18next';
import PreventiveMaintenance from '../../../models/preventiveMaintenance';
import { CompanySettingsContext } from '../../../contexts/CompanySettingsContext';
import { useAppTheme } from '../../../custom-theme';
import { getPriorityColor } from '../../../utils/overall';
import {
  daysUntil,
  describeFrequency,
  getNextOccurrence
} from '../../../utils/schedule';
import { EntityListCard, EntityListCardMeta } from '../../../components/ui';

export default function PreventiveMaintenanceCard({
  preventiveMaintenance,
  onPress
}: {
  preventiveMaintenance: PreventiveMaintenance;
  onPress: () => void;
}) {
  const { t } = useTranslation();
  const theme = useAppTheme();
  const { getFormattedDate } = useContext(CompanySettingsContext);

  const { schedule } = preventiveMaintenance;
  const next = getNextOccurrence(schedule);
  const paused = !!schedule?.disabled;

  const meta: EntityListCardMeta[] = [
    { icon: 'repeat', label: describeFrequency(schedule, t) }
  ];

  if (preventiveMaintenance.priority && preventiveMaintenance.priority !== 'NONE') {
    meta.push({
      icon: 'flag',
      label: t(preventiveMaintenance.priority),
      color: getPriorityColor(preventiveMaintenance.priority, theme)
    });
  }
  if (preventiveMaintenance.asset) {
    meta.push({
      icon: 'package-variant-closed',
      label: preventiveMaintenance.asset.name
    });
  }
  if (preventiveMaintenance.location) {
    meta.push({
      icon: 'map-marker-outline',
      label: preventiveMaintenance.location.name
    });
  }
  if (next) {
    // Due within a couple of days is the thing worth noticing in a long list,
    // so it is coloured rather than left as another grey line.
    const days = daysUntil(next);
    meta.push({
      icon: 'calendar-clock',
      label:
        days === 0
          ? t('due_today')
          : t('next_on', { date: getFormattedDate(next.toISOString(), true) }),
      color: days <= 2 ? theme.colors.warning : undefined
    });
  }

  const title = preventiveMaintenance.name || preventiveMaintenance.title;

  return (
    <EntityListCard
      title={title}
      // The generated work order's title is worth showing only when it differs
      // from the schedule's own name, which is often not the case.
      subtitle={
        preventiveMaintenance.title !== title
          ? preventiveMaintenance.title
          : undefined
      }
      icon="calendar-sync-outline"
      badge={
        paused
          ? { label: t('paused'), color: theme.colors.grey }
          : { label: t('active'), color: theme.colors.success }
      }
      meta={meta}
      onPress={onPress}
    />
  );
}
