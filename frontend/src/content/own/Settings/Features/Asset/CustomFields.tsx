import { Box, Grid } from '@mui/material';
import CustomFieldsManager from '../../../components/CustomFields/CustomFieldsManager';
import { CustomFieldEntityType } from '../../../../../models/owns/customField';

function ConfigureAssetFields() {
  return <CustomFieldsManager entityType={CustomFieldEntityType.ASSET} />;
}

export default ConfigureAssetFields;
