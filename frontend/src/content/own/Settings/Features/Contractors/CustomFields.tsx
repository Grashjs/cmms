import { Box, Grid } from '@mui/material';
import CustomFieldsManager from '../../../components/CustomFields/CustomFieldsManager';
import { CustomFieldEntityType } from '../../../../../models/owns/customField';

function ContractorsCustomFields() {
  // This page sits behind the "customers" tile, and the customer form reads
  // CUSTOMER fields — see VendorsAndCustomers/Customers.tsx. Upstream has the
  // two pages the other way round, so fields created here showed up on vendors.
  return <CustomFieldsManager entityType={CustomFieldEntityType.CUSTOMER} />;
}

export default ContractorsCustomFields;
