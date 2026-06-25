import { Dialog, DialogContent, DialogTitle, Typography } from '@mui/material';
import { useTranslation } from 'react-i18next';
import { useDispatch } from '../../../store';
import { restockPart } from '../../../slices/part';
import Form from '../components/form';
import * as Yup from 'yup';
import { useContext } from 'react';
import { CustomSnackBarContext } from '../../../contexts/CustomSnackBarContext';
import { getErrorMessage } from '../../../utils/api';
import Part from '../../../models/owns/part';

interface RestockPartModalProps {
  open: boolean;
  onClose: () => void;
  part: Part;
}

export default function RestockPartModal({
  open,
  onClose,
  part
}: RestockPartModalProps) {
  const { t }: { t: any } = useTranslation();
  const { showSnackBar } = useContext(CustomSnackBarContext);
  const dispatch = useDispatch();

  return (
    <Dialog fullWidth maxWidth="sm" open={open} onClose={onClose}>
      <DialogTitle sx={{ p: 3 }}>
        <Typography variant="h4" gutterBottom>
          {t('restock_part')}
        </Typography>
      </DialogTitle>
      <DialogContent dividers sx={{ p: 3 }}>
        <Form
          fields={[
            {
              name: 'quantity',
              type: 'number',
              label: t('quantity'),
              required: true
            },
            {
              name: 'description',
              type: 'text',
              label: t('description'),
              multiple: true
            }
          ]}
          validation={Yup.object().shape({
            quantity: Yup.number()
              .moreThan(0, t('quantity_must_be_positive'))
              .required(t('required_field'))
          })}
          submitText={t('restock')}
          values={{}}
          onChange={({ field, e }) => {}}
          onSubmit={async (values) => {
            return dispatch(
              restockPart(part.id, {
                quantity: Number(values.quantity),
                description: values.description
              })
            )
              .then(() => {
                showSnackBar(t('restocked_successfully'), 'success');
                onClose();
              })
              .catch((err) => showSnackBar(getErrorMessage(err), 'error'));
          }}
        />
      </DialogContent>
    </Dialog>
  );
}
