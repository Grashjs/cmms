import { Box, Grid } from '@mui/material';
import CustomFieldsManager from '../../../components/CustomFields/CustomFieldsManager';
import { CustomFieldEntityType } from '../../../../../models/owns/customField';

function ConfigureLocationFields() {
  return <CustomFieldsManager entityType={CustomFieldEntityType.LOCATION} />;
}

export default ConfigureLocationFields;
