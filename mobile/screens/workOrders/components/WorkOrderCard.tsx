import * as React from 'react';
import { useContext } from 'react';
import { StyleSheet, View } from 'react-native';
import { Avatar, Text } from 'react-native-paper';
import { useTranslation } from 'react-i18next';
import WorkOrder from '../../../models/workOrder';
import { UserMiniDTO } from '../../../models/user';
import { CompanySettingsContext } from '../../../contexts/CompanySettingsContext';
import { useAppTheme } from '../../../custom-theme';
import { getPriorityColor, getStatusColor } from '../../../utils/overall';
import { getUserInitials } from '../../../utils/displayers';
import { dayDiff } from '../../../utils/dates';
import { spacing } from '../../../theme/tokens';
import { EntityListCard, EntityListCardMeta } from '../../../components/ui';

/**
 * Assignees appear both as `primaryUser` and inside `assignedTo`, so the two
 * are merged and de-duplicated before display.
 */
const collectAssignees = (workOrder: WorkOrder): UserMiniDTO[] => {
  const users: UserMiniDTO[] = [];
  const seen = new Set<number>();

  if (workOrder.primaryUser) {
    users.push(workOrder.primaryUser);
    seen.add(workOrder.primaryUser.id);
  }
  workOrder.assignedTo?.forEach((user) => {
    if (!seen.has(user.id)) {
      users.push(user);
      seen.add(user.id);
    }
  });
  return users;
};

export default function WorkOrderCard({
  workOrder,
  onPress
}: {
  workOrder: WorkOrder;
  onPress: () => void;
}) {
  const { t } = useTranslation();
  const theme = useAppTheme();
  const { getFormattedDate } = useContext(CompanySettingsContext);

  const assignees = collectAssignees(workOrder);

  // Anything due within two days, or already past due, is worth flagging while
  // the work is still open. Once complete the date is just history.
  const dueDate = workOrder.dueDate ? new Date(workOrder.dueDate) : null;
  const isUrgent =
    !!dueDate &&
    workOrder.status !== 'COMPLETE' &&
    (dayDiff(dueDate, new Date()) <= 2 || new Date() > dueDate);

  const meta: EntityListCardMeta[] = [];
  // `NONE` is the default and carries no signal, so it is left off to keep the
  // row for information that helps decide what to work on next.
  if (workOrder.priority && workOrder.priority !== 'NONE') {
    meta.push({
      icon: 'flag',
      label: t(workOrder.priority),
      color: getPriorityColor(workOrder.priority, theme)
    });
  }
  if (workOrder.asset) {
    meta.push({ icon: 'package-variant-closed', label: workOrder.asset.name });
  }
  if (workOrder.location) {
    meta.push({ icon: 'map-marker-outline', label: workOrder.location.name });
  }
  if (dueDate) {
    meta.push({
      icon: 'clock-alert-outline',
      label: getFormattedDate(workOrder.dueDate),
      color: isUrgent ? theme.colors.error : undefined
    });
  }

  return (
    <EntityListCard
      title={workOrder.title}
      subtitle={`#${workOrder.customId}`}
      imageUrl={workOrder.image?.url}
      icon="clipboard-text-outline"
      badge={{
        label: t(workOrder.status),
        color: getStatusColor(workOrder.status, theme)
      }}
      meta={meta}
      onPress={onPress}
      footer={
        assignees.length ? (
          <View style={styles.assignees}>
            {assignees.slice(0, 3).map((user, index) => (
              <View key={user.id} style={{ marginLeft: index > 0 ? -8 : 0 }}>
                {user.image ? (
                  <Avatar.Image source={{ uri: user.image.url }} size={24} />
                ) : (
                  <Avatar.Text size={24} label={getUserInitials(user)} />
                )}
              </View>
            ))}
            {assignees.length > 3 && (
              <Text
                variant="bodySmall"
                style={{ marginLeft: spacing.sm, color: theme.colors.grey }}
              >
                +{assignees.length - 3}
              </Text>
            )}
          </View>
        ) : null
      }
    />
  );
}

const styles = StyleSheet.create({
  assignees: {
    flexDirection: 'row',
    alignItems: 'center'
  }
});
