import { Box, Button, Dialog, DialogContent, DialogTitle, Typography } from '@mui/material';
import { useTranslation } from 'react-i18next';
import Form from '../../components/form';
import * as Yup from 'yup';
import { IField } from '../../type';
import { useContext, useEffect, useRef, useState } from 'react';
import { StoreReturnType } from '../../../../store';
import api, { getErrorMessage } from '../../../../utils/api';
import { CustomSnackBarContext } from '../../../../contexts/CustomSnackBarContext';
import WorkOrder, { WorkOrderStatusGuard } from '../../../../models/owns/workOrder';
import Labor from '../../../../models/owns/labor';

interface SignatureProps {
  workOrderId: number;
  onReviewGuard: (guard: WorkOrderStatusGuard) => void;
  open: boolean;
  onClose: () => void;
  fieldsConfig: { feedback: boolean; signature: boolean };
  onComplete: (
    signature: string | undefined,
    feedback: string
  ) => Promise<StoreReturnType>;
}
export default function CompleteWOModal({
  workOrderId,
  onReviewGuard,
  open,
  onClose,
  onComplete,
  fieldsConfig
}: SignatureProps) {
  const { t }: { t: any } = useTranslation();
  const { showSnackBar } = useContext(CustomSnackBarContext);
  const [conflict, setConflict] = useState(false);
  const [reviewed, setReviewed] = useState<{ workOrder: WorkOrder; guard: WorkOrderStatusGuard } | null>(null);
  const inputRevision = useRef(0);
  const session = useRef(0);
  const active = useRef(false);
  useEffect(() => {
    active.current = open;
    session.current += 1;
    setConflict(false);
    setReviewed(null);
  }, [open, workOrderId]);

  const getFieldsAndShape = (): [Array<IField>, { [key: string]: any }] => {
    let fields: IField[] = [];
    let shape = {};
    if (fieldsConfig.feedback) {
      fields.push({
        name: 'feedback',
        type: 'text',
        label: t('feedback'),
        placeholder: t('feedback_description'),
        multiple: true
      });
      shape = { feedback: Yup.string().required(t('required_feedback')) };
    }
    if (fieldsConfig.signature) {
      fields.push({
        name: 'signature',
        type: 'signature',
        label: t('signature')
      });
      shape = {
        ...shape,
        signature: Yup.string().required(t('required_signature'))
      };
    }
    return [fields, shape];
  };
  return (
    <Dialog fullWidth maxWidth="sm" open={open} onClose={onClose}>
      <DialogTitle
        sx={{
          p: 3
        }}
      >
        <Typography variant="h4" gutterBottom>
          {t('close_wo')}
        </Typography>
      </DialogTitle>
      <DialogContent
        dividers
        sx={{
          p: 3
        }}
      >
        {conflict && (
          <Box>
            <Typography color="error">{t('work_order_status_review')}</Typography>
            <Button onClick={async () => {
              const currentSession = session.current;
              try {
                const [workOrder, labors] = await Promise.all([
                  api.get<WorkOrder>(`work-orders/${workOrderId}`),
                  api.get<Labor[]>(`labors/work-order/${workOrderId}`)
                ]);
                if (!active.current || session.current !== currentSession) return;
                setReviewed({ workOrder, guard: {
                  baseRevision: workOrder.timerStatusRevision,
                  activeTimerRuns: labors.filter((labor) => labor.logged && labor.status === 'RUNNING')
                    .map((labor) => ({ laborId: labor.id, timerRunId: labor.timerRunId }))
                } });
              } catch (error) {
                if (active.current && session.current === currentSession)
                  showSnackBar(t(getErrorMessage(error)), 'error');
              }
            }}>{t('work_order_status_review_current')}</Button>
            {reviewed && (
              <Box>
                <Typography>{t('work_order_status_review_value', {
                  status: t(reviewed.workOrder.status), count: reviewed.guard.activeTimerRuns.length
                })}</Typography>
                <Button onClick={() => {
                  onReviewGuard(reviewed.guard);
                  setReviewed(null);
                  setConflict(false);
                }}>{t('work_order_status_review_confirm')}</Button>
              </Box>
            )}
          </Box>
        )}
        <Form
          fields={getFieldsAndShape()[0]}
          validation={Yup.object().shape(getFieldsAndShape()[1])}
          submitText={t('close')}
          values={{}}
          isButtonEnabled={() => !conflict}
          onChange={() => { inputRevision.current += 1; }}
          onSignatureInput={() => { inputRevision.current += 1; }}
          onSubmit={async (values) => {
            if (conflict) return;
            const revision = inputRevision.current;
            const submittedSession = session.current;
            return onComplete(values.signature, values.feedback)
              .then(() => {
                if (active.current && session.current === submittedSession && revision === inputRevision.current) onClose();
              })
              .catch((err) => {
                if (!active.current || session.current !== submittedSession) return;
                if (err?.message === 'conflict_error' || err?.status === 409) setConflict(true);
                showSnackBar(t(getErrorMessage(err)), 'error');
              });
          }}
        />
      </DialogContent>
    </Dialog>
  );
}
