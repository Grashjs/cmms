import { Card, Grid } from '@mui/material';
import { useTranslation } from 'react-i18next';
import { IField } from '../type';
import { useContext, useEffect } from 'react';
import { TitleContext } from '../../../contexts/TitleContext';
import {
  addPurchaseOrder,
  respondPurchaseOrder
} from '../../../slices/purchaseOrder';
import { useDispatch } from '../../../store';
import Form from '../components/form';
import * as Yup from 'yup';
import { phoneRegExp } from '../../../utils/validators';
import { formatSelect, formatSwitch } from '../../../utils/formatters';
import { useNavigate } from 'react-router-dom';
import { CustomSnackBarContext } from '../../../contexts/CustomSnackBarContext';
import PermissionErrorMessage from '../components/PermissionErrorMessage';
import useAuth from '../../../hooks/useAuth';
import { PermissionEntity } from '../../../models/owns/role';
import FeatureErrorMessage from '../components/FeatureErrorMessage';
import { PlanFeature } from '../../../models/owns/subscriptionPlan';
import { editPOPartQuantities } from '../../../slices/partQuantity';

function CreatePurchaseOrder() {
  const { t }: { t: any } = useTranslation();
  const { setTitle } = useContext(TitleContext);
  const dispatch = useDispatch();
  const navigate = useNavigate();
  const { hasCreatePermission, hasFeature, hasViewPermission } = useAuth();
  const { showSnackBar } = useContext(CustomSnackBarContext);
  useEffect(() => {
    setTitle(t('new_po'));
  }, []);
  const onCreationSuccess = () => {
    showSnackBar(t('po_create_success'), 'success');
    navigate('/app/purchase-orders');
  };
  const onMissingPartQuantities = () => {
    showSnackBar(t('select_one_part'), 'error');
  };
  const onCreationFailure = (err) =>
    showSnackBar(t('po_create_failure'), 'error');
  const defaultFields: Array<IField> = [
    {
      name: 'purchaseOrderDetails',
      type: 'titleGroupField',
      label: t('po_details')
    },
    {
      name: 'name',
      type: 'text',
      label: t('name'),
      placeholder: t('enter_po_name'),
      required: true,
      midWidth: true
    },
    {
      name: 'category',
      type: 'select',
      label: t('category'),
      type2: 'category',
      category: 'purchase-order-categories',
      placeholder: t('category'),
      midWidth: true
    },
    {
      name: 'shippingDueDate',
      type: 'date',
      label: t('due_date'),
      midWidth: true
    },
    {
      name: 'shippingAdditionalDetail',
      type: 'text',
      label: t('additional_details'),
      midWidth: true,
      multiple: true
    },
    {
      name: 'vendor',
      type: 'select',
      type2: 'vendor',
      label: t('vendor'),
      midWidth: true
    },
    {
      name: 'workOrder',
      type: 'select',
      type2: 'workOrder',
      label: t('work_order'),
      helperText: t('purchase_order_work_order_description'),
      midWidth: true
    },
    {
      name: 'partQuantities',
      type: 'partQuantity',
      label: t('parts'),
      midWidth: true,
      multiple: true
    },
    {
      name: 'shippingInformation',
      type: 'titleGroupField',
      label: t('shipping_information')
    },
    {
      name: 'shippingCompanyName',
      type: 'text',
      label: t('company_name'),
      placeholder: t('company_name'),
      midWidth: true
    },
    {
      name: 'shippingShipToName',
      type: 'text',
      label: t('ship_to'),
      placeholder: t('ship_to'),
      midWidth: true
    },
    {
      name: 'shippingAddress',
      type: 'text',
      label: t('address'),
      placeholder: t('address'),
      midWidth: true
    },
    {
      name: 'shippingCity',
      type: 'text',
      label: t('city'),
      placeholder: t('city'),
      midWidth: true
    },
    {
      name: 'shippingState',
      type: 'text',
      label: t('state'),
      placeholder: t('state'),
      midWidth: true
    },
    {
      name: 'shippingZipCode',
      type: 'number',
      label: t('zip_code'),
      placeholder: t('zip_code'),
      midWidth: true
    },
    {
      name: 'shippingPhone',
      type: 'text',
      label: t('phone'),
      placeholder: t('phone'),
      midWidth: true
    },
    {
      name: 'shippingFax',
      type: 'text',
      label: t('fax_number'),
      placeholder: t('fax_number'),
      midWidth: true
    },
    {
      name: 'additionalInformation',
      type: 'titleGroupField',
      label: t('additional_information')
    },
    {
      name: 'additionalInfoDate',
      type: 'date',
      label: t('po_date'),
      placeholder: t('po_date'),
      midWidth: true
    },
    {
      name: 'additionalInfoNotes',
      type: 'text',
      label: t('notes'),
      placeholder: t('add_notes'),
      midWidth: true,
      multiple: true
    },
    {
      name: 'additionalInfoRequisitionedName',
      type: 'text',
      label: t('requisitioner'),
      placeholder: t('requisitioner'),
      midWidth: true
    },
    {
      name: 'additionalInfoTerm',
      type: 'text',
      label: t('terms'),
      placeholder: t('terms'),
      midWidth: true
    },
    {
      name: 'additionalInfoShippingOrderCategory',
      type: 'text',
      label: t('shipping_method'),
      placeholder: t('shipping_method'),
      midWidth: true
    }
  ];
  const getFields = () => {
    let fields = [...defaultFields];
    if (hasViewPermission(PermissionEntity.SETTINGS)) {
      fields.push({
        name: 'approveOnSubmit',
        type: 'switch',
        label: t('approve_while_submitting')
      });
    }
    return fields;
  };
  const shape = {
    name: Yup.string().required(t('required_name')),
    shippingFax: Yup.string().matches(phoneRegExp, t('invalid_fax')),
    shippingPhone: Yup.string().matches(phoneRegExp, t('invalid_phone'))
  };
  if (hasFeature(PlanFeature.PURCHASE_ORDER)) {
    if (hasCreatePermission(PermissionEntity.PURCHASE_ORDERS))
      return (
        <>
<Grid
            container
            justifyContent="center"
            alignItems="stretch"
            spacing={1}
            paddingX={4}
          >
            <Grid item xs={12}>
              <Card
                sx={{
                  p: 2,
                  display: 'flex',
                  flexDirection: 'row',
                  alignItems: 'center',
                  justifyContent: 'space-between'
                }}
              >
                <Form
                  fields={getFields()}
                  validation={Yup.object().shape(shape)}
                  submitText={t('submit')}
                  values={{
                    shippingDueDate: null,
                    additionalInfoDate: null,
                    approveOnSubmit: false
                  }}
                  onChange={({ field, e }) => {}}
                  onSubmit={async (values) => {
                    if (!values.partQuantities?.length) {
                      onMissingPartQuantities();
                      return;
                    }
                    if (
                      values.partQuantities.some(
                        (partQuantity) => partQuantity.quantity <= 0
                      )
                    ) {
                      showSnackBar(t('each_item_superior_zero'), 'error');
                      return;
                    }
                    values.category = formatSelect(values.category);
                    values.vendor = formatSelect(values.vendor);
                    values.workOrder = formatSelect(values.workOrder);
                    values.approveOnSubmit = formatSwitch(
                      values,
                      'approveOnSubmit'
                    );
                    return dispatch(addPurchaseOrder(values))
                      .then((id: number) => {
                        dispatch(
                          editPOPartQuantities(id, values.partQuantities)
                        )
                          .then(() => {
                            if (values.approveOnSubmit) {
                              dispatch(respondPurchaseOrder(id, true));
                            }
                          })
                          .then(onCreationSuccess)
                          .catch(onCreationFailure);
                      })
                      .catch(onCreationFailure);
                  }}
                />
              </Card>
            </Grid>
          </Grid>
        </>
      );
    else
      return <PermissionErrorMessage message={'no_access_purchase_orders'} />;
  } else
    return (
      <FeatureErrorMessage message={'Upgrade to create Purchase Orders'} />
    );
}

export default CreatePurchaseOrder;
