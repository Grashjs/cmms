import { Box, Grid } from '@mui/material';
import CustomFieldsManager from '../../../components/CustomFields/CustomFieldsManager';
import { CustomFieldEntityType } from '../../../../../models/owns/customField';

function MetersCustomFields() {
  return <CustomFieldsManager entityType={CustomFieldEntityType.METER} />;
}

export default MetersCustomFields;
