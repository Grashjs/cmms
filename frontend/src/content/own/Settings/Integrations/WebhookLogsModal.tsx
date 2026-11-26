import {
  Box,
  Chip,
  CircularProgress,
  Collapse,
  Dialog,
  DialogContent,
  DialogTitle,
  IconButton,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TablePagination,
  TableRow,
  Tooltip,
  Typography
} from '@mui/material';
import { useTranslation } from 'react-i18next';
import { Fragment, useCallback, useContext, useEffect, useState } from 'react';
import CloseIcon from '@mui/icons-material/Close';
import KeyboardArrowDownIcon from '@mui/icons-material/KeyboardArrowDown';
import KeyboardArrowUpIcon from '@mui/icons-material/KeyboardArrowUp';
import ReplayIcon from '@mui/icons-material/Replay';
import { useDispatch, useSelector } from '../../../../store';
import {
  clearWebhookDeliveryLogs,
  getWebhookDeliveryLogs,
  retryWebhookDeliveryLog
} from '../../../../slices/webhookDeliveryLog';
import { WebhookDeliveryLog } from '../../../../models/owns/webhookDeliveryLog';
import { CustomSnackBarContext } from '../../../../contexts/CustomSnackBarContext';
import { CompanySettingsContext } from '../../../../contexts/CompanySettingsContext';
import { getWebhookEventLabelKey } from '../../../../models/owns/webhookEndpoint';
import type { WebhookEvent } from '../../../../models/owns/webhookEndpoint';

interface WebhookLogsModalProps {
  open: boolean;
  endpointId: number | null;
  onClose: () => void;
}

const formatBody = (body: string | null): string => {
  if (!body) return '-';
  try {
    return JSON.stringify(JSON.parse(body), null, 2);
  } catch {
    return body;
  }
};

const PRE_SX = {
  m: 0,
  p: 1.5,
  maxHeight: 280,
  overflow: 'auto',
  fontSize: 12,
  whiteSpace: 'pre-wrap',
  wordBreak: 'break-word',
  bgcolor: 'action.hover',
  borderRadius: 1
} as const;

function WebhookLogsModal({ open, endpointId, onClose }: WebhookLogsModalProps) {
  const { t }: { t: any } = useTranslation();
  const dispatch = useDispatch();
  const { showSnackBar } = useContext(CustomSnackBarContext);
  const { getFormattedDate } = useContext(CompanySettingsContext);
  const { logs, loadingGet } = useSelector((state) => state.webhookDeliveryLogs);
  const [page, setPage] = useState<number>(0);
  const [rowsPerPage, setRowsPerPage] = useState<number>(20);
  const [expandedId, setExpandedId] = useState<number | null>(null);
  const [retryingId, setRetryingId] = useState<number | null>(null);

  const load = useCallback(() => {
    if (endpointId === null) return;
    dispatch(
      getWebhookDeliveryLogs(endpointId, {
        page,
        size: rowsPerPage,
        sort: ['createdAt,desc']
      })
    );
  }, [dispatch, endpointId, page, rowsPerPage]);

  useEffect(() => {
    if (open) {
      load();
    }
  }, [open, load]);

  useEffect(() => {
    if (!open) {
      setPage(0);
      setExpandedId(null);
      dispatch(clearWebhookDeliveryLogs());
    }
  }, [open]);

  const handleRetry = async (log: WebhookDeliveryLog) => {
    setRetryingId(log.id);
    try {
      const newLog = await dispatch(retryWebhookDeliveryLog(log.id));
      showSnackBar(
        newLog?.success
          ? t('webhook_logs_retry_success')
          : t('webhook_logs_retry_failed'),
        newLog?.success ? 'success' : 'error'
      );
      if (page === 0) {
        load();
      } else {
        setPage(0);
      }
    } catch (error) {
      showSnackBar(t('webhook_logs_retry_error'), 'error');
    } finally {
      setRetryingId(null);
    }
  };

  const getEventLabel = (eventType: string) => {
    const key = getWebhookEventLabelKey(eventType as WebhookEvent);
    const label = t(key);
    return label === key ? eventType : label;
  };

  return (
    <Dialog open={open} onClose={onClose} maxWidth="lg" fullWidth>
      <DialogTitle>
        <Box display="flex" justifyContent="space-between" alignItems="center">
          <Typography variant="h4">{t('webhook_logs')}</Typography>
          <IconButton onClick={onClose}>
            <CloseIcon />
          </IconButton>
        </Box>
      </DialogTitle>
      <DialogContent>
        <Box sx={{ overflowX: 'auto' }}>
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell width={48} />
                <TableCell>{t('date')}</TableCell>
                <TableCell>{t('webhook_logs_event')}</TableCell>
                <TableCell>{t('webhook_logs_http_code')}</TableCell>
                <TableCell>{t('webhook_logs_duration_ms')}</TableCell>
                <TableCell>{t('webhook_logs_success')}</TableCell>
                <TableCell align="right">{t('actions')}</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {loadingGet && logs.content.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={7} align="center">
                    <CircularProgress size={24} />
                  </TableCell>
                </TableRow>
              ) : logs.content.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={7} align="center">
                    {t('no_data')}
                  </TableCell>
                </TableRow>
              ) : (
                logs.content.map((log) => {
                  const expanded = expandedId === log.id;
                  return (
                    <Fragment key={log.id}>
                      <TableRow hover sx={{ '& > td': { borderBottom: 'unset' } }}>
                        <TableCell>
                          <IconButton
                            size="small"
                            onClick={() =>
                              setExpandedId(expanded ? null : log.id)
                            }
                          >
                            {expanded ? (
                              <KeyboardArrowUpIcon />
                            ) : (
                              <KeyboardArrowDownIcon />
                            )}
                          </IconButton>
                        </TableCell>
                        <TableCell>{getFormattedDate(log.createdAt)}</TableCell>
                        <TableCell>{getEventLabel(log.eventType)}</TableCell>
                        <TableCell>
                          <Chip
                            size="small"
                            color={
                              log.responseStatus !== null &&
                              log.responseStatus >= 200 &&
                              log.responseStatus < 300
                                ? 'success'
                                : 'error'
                            }
                            label={log.responseStatus ?? '-'}
                          />
                        </TableCell>
                        <TableCell>{log.durationMs ?? '-'}</TableCell>
                        <TableCell>
                          <Chip
                            size="small"
                            variant="outlined"
                            color={log.success ? 'success' : 'error'}
                            label={log.success ? t('yes') : t('no')}
                          />
                        </TableCell>
                        <TableCell align="right">
                          <Tooltip title={t('webhook_logs_resend')}>
                            <span>
                              <IconButton
                                size="small"
                                color="primary"
                                disabled={retryingId !== null}
                                onClick={() => handleRetry(log)}
                              >
                                {retryingId === log.id ? (
                                  <CircularProgress size={18} />
                                ) : (
                                  <ReplayIcon fontSize="small" />
                                )}
                              </IconButton>
                            </span>
                          </Tooltip>
                        </TableCell>
                      </TableRow>
                      <TableRow>
                        <TableCell sx={{ py: 0 }} colSpan={7}>
                          <Collapse in={expanded} timeout="auto" unmountOnExit>
                            <Box py={2}>
                              {log.errorMessage && (
                                <Typography color="error" variant="body2" mb={1}>
                                  {log.errorMessage}
                                </Typography>
                              )}
                              <Typography variant="subtitle2" gutterBottom>
                                {t('webhook_logs_payload')}
                              </Typography>
                              <Box component="pre" sx={PRE_SX}>
                                {formatBody(log.payload)}
                              </Box>
                              <Typography
                                variant="subtitle2"
                                gutterBottom
                                mt={2}
                              >
                                {t('webhook_logs_response_body')}
                              </Typography>
                              <Box component="pre" sx={PRE_SX}>
                                {formatBody(log.responseBody)}
                              </Box>
                            </Box>
                          </Collapse>
                        </TableCell>
                      </TableRow>
                    </Fragment>
                  );
                })
              )}
            </TableBody>
          </Table>
        </Box>
        <TablePagination
          component="div"
          count={logs.totalElements}
          page={page}
          rowsPerPage={rowsPerPage}
          rowsPerPageOptions={[10, 20, 50]}
          onPageChange={(_e, newPage) => setPage(newPage)}
          onRowsPerPageChange={(e) => {
            setRowsPerPage(parseInt(e.target.value, 10));
            setPage(0);
          }}
        />
      </DialogContent>
    </Dialog>
  );
}

export default WebhookLogsModal;
