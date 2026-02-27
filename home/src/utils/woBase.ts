import { WorkOrderBase } from 'src/models/owns/workOrderBase';
import { getPriorityLabel } from './formatters';
import { IField } from '../content/own/type';

export const getWOBaseFields = (
  t: any,
  options?: { delay?: boolean }
): Array<IField> => {
  let result: IField[] = [
    {
      name: 'title',
      type: 'text',
      label: t('title'),
      placeholder: t('enter_wo_title'),
      required: true
    },
    {
      name: 'description',
      type: 'text',
      label: t('description'),
      placeholder: t('description'),
      multiple: true
    },
    {
      name: 'priority',
      type: 'select',
      type2: 'priority',
      label: t('priority'),
      placeholder: t('priority')
    },
    {
      name: 'dueDate',
      type: 'date',
      label: t('due_date')
    },
    {
      name: 'estimatedStartDate',
      type: 'date',
      label: t('estimated_start_date')
    },
    {
      name: 'estimatedDuration',
      type: 'number',
      label: t('estimated_duration'),
      placeholder: t('hours')
    },
    {
      name: 'category',
      type: 'select',
      label: t('category'),
      type2: 'category',
      category: 'work-order-categories'
    },
    {
      name: 'location',
      type: 'select',
      type2: 'location',
      label: t('location'),
      placeholder: 'Select location'
    },
    {
      name: 'asset',
      type: 'select',
      type2: 'asset',
      label: t('asset'),
      placeholder: 'Select Asset',
      relatedFields: [{ field: 'location' }],
      required: true
    },
    {
      name: 'primaryUser',
      type: 'select',
      label: t('primary_worker'),
      type2: 'user'
    },
    {
      name: 'team',
      type: 'select',
      type2: 'team',
      label: t('team'),
      placeholder: 'Select team'
    },
    {
      name: 'image',
      type: 'file',
      fileType: 'image',
      label: t('image')
    },
    {
      name: 'files',
      type: 'file',
      multiple: true,
      label: t('files'),
      fileType: 'file'
    }
  ];
  if (options?.delay) {
    result = result.filter((field) => field.name !== 'dueDate');
    result.splice(3, 0, {
      name: 'dueDateDelay',
      type: 'number',
      label: t('due_date_delay'),
      placeholder: t('due_date_delay_description')
    });
  }
  return result;
};

export const getWOBaseValues = <T extends WorkOrderBase>(t: any, entity: T) => {
  return {
    priority: entity?.priority
      ? {
        label: getPriorityLabel(entity?.priority, t),
        value: entity?.priority
      }
      : null,
    category: entity?.category
      ? {
        label: entity?.category?.name,
        value: entity?.category?.id
      }
      : null,
    primaryUser: entity?.primaryUser
      ? {
        label: `${entity.primaryUser.firstName} ${entity.primaryUser.lastName}`,
        value: entity.primaryUser.id.toString()
      }
      : null,
    assignedTo: entity?.assignedTo.map((worker) => {
      return {
        label: `${worker.firstName} ${worker.lastName}`,
        value: worker.id.toString()
      };
    }),
    customers: entity?.customers.map((customer) => {
      return {
        label: customer.name,
        value: customer.id.toString()
      };
    }),
    team: entity?.team
      ? {
        label: entity.team?.name,
        value: entity.team?.id.toString()
      }
      : null,
    location: entity?.location
      ? {
        label: entity.location.name,
        value: entity.location.id.toString()
      }
      : null,
    asset: entity?.asset
      ? {
        label: entity.asset?.name,
        value: entity.asset?.id.toString()
      }
      : null
  };
};
