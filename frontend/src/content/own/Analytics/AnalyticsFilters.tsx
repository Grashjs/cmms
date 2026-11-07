import { useEffect, useState } from 'react';
import { Button, Drawer, Grid, Typography } from '@mui/material';
import FilterAltTwoToneIcon from '@mui/icons-material/FilterAltTwoTone';
import { useTranslation } from 'react-i18next';
import * as Yup from 'yup';
import { FilterField } from '../../../models/owns/page';
import { IField } from '../type';
import Form from '../components/form';
import { useDispatch, useSelector } from '../../../store';
import { filterSingleField, getLabelAndValue } from '../../../utils/filter';
import { getLocationsMini } from '../../../slices/location';
import { getTeamsMini } from '../../../slices/team';
import { getCategories } from '../../../slices/category';

// Fields accepted by the analytics endpoints (see AnalyticsFilterService in the API)
const ID_FILTERS: { accessor: string; fieldName: string }[] = [
  { accessor: 'locations', fieldName: 'location' },
  { accessor: 'assetCategories', fieldName: 'asset.category' },
  { accessor: 'teams', fieldName: 'team' },
  { accessor: 'categories', fieldName: 'category' }
];
const PRIORITIES = ['NONE', 'LOW', 'MEDIUM', 'HIGH'];

interface OwnProps {
  filterFields: FilterField[];
  onChange: (filterFields: FilterField[]) => void;
}

export default function AnalyticsFilters({ filterFields, onChange }: OwnProps) {
  const { t }: { t: any } = useTranslation();
  const dispatch = useDispatch();
  const [open, setOpen] = useState<boolean>(false);
  const { locationsMini } = useSelector((state) => state.locations);
  const { teamsMini } = useSelector((state) => state.teams);
  const { categories } = useSelector((state) => state.categories);

  // the labels of the selected values come from these lists
  useEffect(() => {
    if (!open) return;
    if (!locationsMini.length) dispatch(getLocationsMini());
    if (!teamsMini.length) dispatch(getTeamsMini());
    if (!categories['asset-categories']?.length)
      dispatch(getCategories('asset-categories'));
    if (!categories['work-order-categories']?.length)
      dispatch(getCategories('work-order-categories'));
  }, [open]);

  const fields: Array<IField> = [
    {
      name: 'locations',
      type: 'select',
      type2: 'location',
      label: t('location'),
      multiple: true
    },
    {
      name: 'assetCategories',
      type: 'select',
      type2: 'category',
      category: 'asset-categories',
      label: t('asset_category'),
      multiple: true
    },
    {
      name: 'teams',
      type: 'select',
      type2: 'team',
      label: t('team'),
      multiple: true
    },
    {
      name: 'categories',
      type: 'select',
      type2: 'category',
      category: 'work-order-categories',
      label: t('work_order_category'),
      multiple: true
    },
    {
      name: 'priority',
      type: 'select',
      label: t('priority'),
      multiple: true,
      items: PRIORITIES.map((value) => ({ label: t(value), value }))
    }
  ];

  const values = {
    locations: getLabelAndValue(
      filterFields,
      locationsMini,
      'location',
      'name'
    ),
    assetCategories: getLabelAndValue(
      filterFields,
      categories['asset-categories'],
      'asset.category',
      'name'
    ),
    teams: getLabelAndValue(filterFields, teamsMini, 'team', 'name'),
    categories: getLabelAndValue(
      filterFields,
      categories['work-order-categories'],
      'category',
      'name'
    ),
    priority:
      filterFields
        .find((filterField) => filterField.field === 'priority')
        ?.values?.map((value) => ({ label: t(value), value })) ?? []
  };

  const handleSubmit = async (formValues) => {
    let newFilterFields: FilterField[] = [];
    ID_FILTERS.forEach(({ accessor, fieldName }) => {
      newFilterFields = filterSingleField(
        newFilterFields,
        formValues,
        accessor,
        fieldName,
        'array'
      );
    });
    const priorities = formValues.priority as
      | { label: string; value: string }[]
      | undefined;
    if (priorities?.length) {
      newFilterFields.push({
        field: 'priority',
        operation: 'in',
        value: '',
        values: priorities.map((priority) => priority.value),
        enumName: 'PRIORITY'
      });
    }
    onChange(newFilterFields);
    setOpen(false);
  };

  const active = filterFields.length > 0;
  return (
    <>
      <Button
        onClick={() => setOpen(true)}
        variant={active ? 'contained' : 'outlined'}
        startIcon={<FilterAltTwoToneIcon />}
        size="large"
      >
        {active ? `${t('filters')} (${filterFields.length})` : t('filters')}
      </Button>
      <Drawer
        anchor="left"
        open={open}
        onClose={() => setOpen(false)}
        PaperProps={{ sx: { width: { xs: '100%', md: '30%' } } }}
      >
        <Grid container spacing={1} padding={2}>
          <Grid item xs={12}>
            <Typography variant="h2">{t('filters')}</Typography>
          </Grid>
          <Grid item xs={12}>
            <Form
              fields={fields}
              validation={Yup.object().shape({})}
              submitText={t('save')}
              values={values}
              enableReinitialize
              nextToButton={
                <Button
                  sx={{ ml: 2 }}
                  variant="outlined"
                  onClick={() => {
                    onChange([]);
                    setOpen(false);
                  }}
                >
                  {t('reset')}
                </Button>
              }
              onChange={() => {}}
              onSubmit={handleSubmit}
            />
          </Grid>
        </Grid>
      </Drawer>
    </>
  );
}
