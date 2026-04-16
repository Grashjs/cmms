import { Box, Typography } from '@mui/material';
import { useTranslation } from 'react-i18next';
import CustomFieldsManager from '../../../components/CustomFields/CustomFieldsManager';
import { CustomFieldEntityType } from '../../../../../models/owns/customField';

function CustomFields() {
  const { t }: { t: any } = useTranslation();

  return <CustomFieldsManager entityType={CustomFieldEntityType.WORK_ORDER} />;
}

export default CustomFields;
