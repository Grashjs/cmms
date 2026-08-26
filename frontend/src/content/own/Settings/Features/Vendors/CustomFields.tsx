import { Box, Grid } from '@mui/material';
import CustomFieldsManager from '../../../components/CustomFields/CustomFieldsManager';
import { CustomFieldEntityType } from '../../../../../models/owns/customField';

function VendorsCustomFields() {
  // The other half of the swap fixed in Contractors/CustomFields.tsx: the
  // vendor form reads VENDOR fields, so this page has to manage those.
  return <CustomFieldsManager entityType={CustomFieldEntityType.VENDOR} />;
}

export default VendorsCustomFields;
