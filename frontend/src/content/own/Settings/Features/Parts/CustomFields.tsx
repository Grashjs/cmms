import { Box, Grid } from '@mui/material';
import CustomFieldsManager from '../../../components/CustomFields/CustomFieldsManager';
import { CustomFieldEntityType } from '../../../../../models/owns/customField';

function PartsCustomFields() {
  return <CustomFieldsManager entityType={CustomFieldEntityType.PART} />;
}

export default PartsCustomFields;
