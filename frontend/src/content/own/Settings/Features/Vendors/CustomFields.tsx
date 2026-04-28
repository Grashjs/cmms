import { Box, Grid } from '@mui/material';
import CustomFieldsManager from '../../../components/CustomFields/CustomFieldsManager';
import { CustomFieldEntityType } from '../../../../../models/owns/customField';

function VendorsCustomFields() {
  return <CustomFieldsManager entityType={CustomFieldEntityType.CUSTOMER} />;
}

export default VendorsCustomFields;
