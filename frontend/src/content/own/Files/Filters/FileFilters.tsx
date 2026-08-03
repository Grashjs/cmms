import { useEffect, useState } from 'react';
import { Box, Button, Popover, Typography } from '@mui/material';
import FilterAltTwoToneIcon from '@mui/icons-material/FilterAltTwoTone';
import * as Yup from 'yup';
import { useTranslation } from 'react-i18next';
import Form from '../../components/form';
import { IField } from '../../type';
import { FilterField } from '../../../../models/owns/page';
import {
  filterSingleField,
  getDateValue,
  getLabelAndValue
} from '../../../../utils/filter';
import { useDispatch, useSelector } from '../../../../store';
import { getUsersMini } from '../../../../slices/user';
import { getAssetsMini } from '../../../../slices/asset';
import { UserMiniDTO } from '../../../../models/user';

/**
 * Fields this component owns. Everything else in criteria.filterFields — the free text
 * search on `name`, and whatever the API adds server side — is left untouched.
 *
 * The set is deliberately small. Two obvious candidates do not work:
 *
 * - `type` (IMAGE/OTHER) is stored as the enum *ordinal* in a SMALLINT column
 *   (2026_01_10_1768015926_enums_type.xml), and WrapperSpecification.getRealValue only
 *   converts strings to enums for PRIORITY, STATUS and JS_DATE. A filter carrying "IMAGE"
 *   never reaches the query as an enum, so this needs an EnumName entry in the API first.
 * - `hidden` cannot be filtered at all: FileController.search appends `hidden eq false`
 *   for every client request and SpecificationBuilder ANDs all filter fields, so a
 *   user-supplied `hidden eq true` can only ever return an empty page.
 */
export const FILE_FILTER_FIELDS = [
  'assets',
  'workOrders',
  'createdBy',
  'createdAt'
];

interface OwnProps {
  filterFields: FilterField[];
  onChange: (filterFields: FilterField[]) => void;
  /**
   * Users without the view-other permission are pinned to their own files by the API
   * (FileController.search adds `createdBy eq <own id>`). Offering them the filter would
   * only let them build a contradictory query that returns nothing.
   */
  showCreatedBy: boolean;
}

function FileFilters({ filterFields, onChange, showCreatedBy }: OwnProps) {
  const { t }: { t: any } = useTranslation();
  const [anchorEl, setAnchorEl] = useState<null | HTMLElement>(null);
  const { usersMini } = useSelector((state) => state.users);
  const { assetsMini } = useSelector((state) => state.assets);
  const { workOrdersMini } = useSelector((state) => state.workOrders);
  const dispatch = useDispatch();

  const selectedIds = (field: string): number[] =>
    (filterFields.find((filterField) => filterField.field === field)?.values ??
      []) as number[];

  // Only to resolve already-selected ids to labels — the select loads its own options.
  useEffect(() => {
    if (showCreatedBy && !usersMini.length) dispatch(getUsersMini());
    if (selectedIds('assets').length && !assetsMini.length)
      dispatch(getAssetsMini());
  }, [showCreatedBy]);

  const isActive = filterFields.some((filterField) =>
    FILE_FILTER_FIELDS.includes(filterField.field)
  );

  const fields: Array<IField> = [
    {
      name: 'assets',
      type: 'select',
      label: t('asset'),
      type2: 'asset',
      multiple: true
    },
    {
      name: 'workOrders',
      type: 'select',
      label: t('work_order'),
      type2: 'workOrder',
      multiple: true
    }
  ];
  if (showCreatedBy) {
    fields.push({
      name: 'createdBy',
      type: 'select',
      label: t('uploaded_by'),
      type2: 'user',
      multiple: true
    });
  }
  fields.push({
    name: 'createdAt',
    type: 'dateRange',
    label: t('uploaded_on')
  });

  // getLabelAndValue renders an empty object when the id is not in the mini list, which
  // shows up as "undefined". workOrdersMini is a single page, so a selected work order from
  // outside it would hit exactly that — hence the explicit id fallback.
  const getValuesFromFilterFields = () => ({
    assets: selectedIds('assets').map((id) => {
      const asset = assetsMini.find((mini) => mini.id === id);
      return { label: asset?.name ?? `#${id}`, value: id };
    }),
    workOrders: selectedIds('workOrders').map((id) => {
      const workOrder = workOrdersMini.content.find((mini) => mini.id === id);
      return {
        label: workOrder?.customId ?? workOrder?.title ?? `#${id}`,
        value: id
      };
    }),
    createdBy: getLabelAndValue(
      filterFields,
      usersMini,
      'createdBy',
      null,
      (user: UserMiniDTO) => `${user.firstName} ${user.lastName}`
    ),
    createdAt: getDateValue(filterFields, 'createdAt')
  });

  const handleClose = () => setAnchorEl(null);

  return (
    <>
      <Button
        onClick={(event) => setAnchorEl(event.currentTarget)}
        sx={{
          '& .MuiButton-startIcon': { margin: '0px' },
          minWidth: 0
        }}
        variant={isActive ? 'contained' : 'outlined'}
        startIcon={<FilterAltTwoToneIcon />}
      />
      <Popover
        open={Boolean(anchorEl)}
        anchorEl={anchorEl}
        onClose={handleClose}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'left' }}
        PaperProps={{ sx: { width: { xs: '90%', sm: 400 }, p: 2 } }}
      >
        <Box>
          <Typography variant="h4" sx={{ pb: 1 }}>
            {t('filters')}
          </Typography>
          <Form
            fields={fields}
            validation={Yup.object().shape({})}
            submitText={t('apply')}
            values={getValuesFromFilterFields()}
            enableReinitialize
            nextToButton={
              <Button
                sx={{ ml: 2 }}
                variant="outlined"
                onClick={() => {
                  onChange(
                    filterFields.filter(
                      (filterField) =>
                        !FILE_FILTER_FIELDS.includes(filterField.field)
                    )
                  );
                  handleClose();
                }}
              >
                {t('reset')}
              </Button>
            }
            onChange={({ field, e }) => {}}
            onSubmit={async (values) => {
              let newFilterFields = [...filterFields];
              // 'inm' joins the many-to-many association instead of comparing a column.
              newFilterFields = filterSingleField(
                newFilterFields,
                values,
                'assets',
                'assets',
                'array',
                'inm'
              );
              newFilterFields = filterSingleField(
                newFilterFields,
                values,
                'workOrders',
                'workOrders',
                'array',
                'inm'
              );
              if (showCreatedBy) {
                newFilterFields = filterSingleField(
                  newFilterFields,
                  values,
                  'createdBy',
                  'createdBy',
                  'array'
                );
              }
              newFilterFields = filterSingleField(
                newFilterFields,
                values,
                'createdAt',
                'createdAt',
                'date'
              );
              onChange(newFilterFields);
              handleClose();
            }}
          />
        </Box>
      </Popover>
    </>
  );
}

export default FileFilters;
