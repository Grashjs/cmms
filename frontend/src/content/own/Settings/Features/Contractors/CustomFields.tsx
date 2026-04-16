import { Box, Grid } from '@mui/material';
import CustomFieldsManager from '../../../components/CustomFields/CustomFieldsManager';
import { CustomFieldEntityType } from '../../../../../models/owns/customField';

function ContractorsCustomFields() {
  return <CustomFieldsManager entityType={CustomFieldEntityType.VENDOR} />;
}

export default ContractorsCustomFields;
