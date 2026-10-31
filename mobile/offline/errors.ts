// Keep domain errors language-neutral; translate only where they reach the UI.
const messageKeys: Record<string, string> = {
  CONTEXT_CHANGED: 'offline_context_changed',
  CONTEXT_MISMATCH: 'offline_context_changed',
  INVALID_CONTEXT: 'offline_context_changed',
  INVALID_SERVER_URL: 'offline_context_changed',
  OFFLINE_CONTEXT_MISMATCH: 'offline_context_changed',
  CONTEXT_BUSY: 'offline_storage_busy',
  AUTHENTICATION_REQUIRED: 'offline_reconnect',
  NETWORK_UNAVAILABLE: 'offline_network_unavailable',
  OFFLINE_DISABLED: 'offline_disabled',
  OFFLINE_NOT_AUTHORIZED: 'offline_disabled',
  INVALID_AUTHORIZATION: 'offline_disabled',
  OFFLINE_HTTPS_REQUIRED: 'offline_https_required',
  OFFLINE_NATIVE_REQUIRED: 'offline_native_required',
  STORAGE_UNAVAILABLE: 'offline_load_error',
  STORAGE_FAILED: 'offline_save_failed',
  REVISION_LIMIT: 'offline_save_failed',
  PREPARED_LIMIT: 'offline_limit',
  PREPARED_LIMIT_EXCEEDED: 'offline_limit',
  SNAPSHOT_LIMIT: 'offline_limit',
  SNAPSHOT_LIMIT_EXCEEDED: 'offline_limit',
  RESPONSE_LIMIT: 'offline_limit',
  INCOMPLETE_SNAPSHOT: 'offline_invalid_response',
  INVALID_SNAPSHOT: 'offline_invalid_response',
  INVALID_RESPONSE: 'offline_invalid_response',
  INVALID_TASK: 'offline_invalid_response',
  TASK_IDENTITY: 'offline_invalid_response',
  TARGET_MISMATCH: 'offline_invalid_response',
  INVALID_RECEIPT: 'offline_invalid_response',
  INVALID_RECEIPTS: 'offline_invalid_response',
  RECEIPTS_LIMIT: 'offline_invalid_response',
  TARGET_UNAVAILABLE: 'offline_unavailable',
  TARGET_NOT_FOUND: 'offline_unavailable',
  ACCESS_REVOKED: 'offline_unavailable',
  STALE_PREPARATION: 'offline_unavailable',
  EDIT_FORBIDDEN: 'offline_read_only',
  REMOVAL_PENDING: 'offline_removal_pending',
  WORK_PENDING: 'offline_work_pending',
  DRAFT_CHANGED: 'offline_changed_revision',
  SERVER_CHANGED: 'offline_resume_after_conflict',
  PREVIOUS_OPERATION: 'offline_wait_previous',
  RESULT_UNKNOWN: 'offline_unknown',
  OPERATION_MISSING: 'offline_result_changed',
  OPERATION_RESOLVED: 'offline_result_changed',
  UNSUPPORTED_TASK: 'offline_unsupported_task',
  TYPE_NOT_SUPPORTED: 'offline_unsupported_task',
  TEXT_TOO_LONG: 'offline_max_text',
  VALUE_TOO_LONG: 'offline_max_text',
  NOTES_TOO_LONG: 'offline_max_notes',
  INVALID_NUMBER: 'offline_invalid_number',
  INVALID_CHOICE: 'offline_invalid_choice',
  INVALID_TASK_OPTION: 'offline_invalid_choice',
  COMMENT_EMPTY: 'offline_comment_empty',
  COMMENT_ATTACHMENT_PENDING: 'offline_comment_attachment_pending',
  INVALID_COMMENT_CONTENT: 'offline_comment_empty',
  REQUEST_TOO_LARGE: 'offline_request_too_large',
  OPERATION_LIMIT_EXCEEDED: 'offline_request_too_large',
  INVALID_PREDECESSOR: 'offline_resume_after_conflict',
  PREDECESSOR_FAILED: 'offline_resume_after_conflict',
  INVALID_VALUE: 'offline_action_failed'
};

export function offlineErrorKey(error: unknown): string {
  let { code } = (error || {}) as { code?: string };
  const { status, message } = (error || {}) as {
    status?: number;
    message?: string;
  };
  // The existing connected client wraps HTTP bodies in Error.message.
  // Recognize domain codes without ever displaying raw server diagnostics.
  if (!code && message) {
    try {
      const body = JSON.parse(message);
      code = body?.code || body?.message;
    } catch {
      /* Network/native errors use the generic translated message. */
    }
  }
  if (code && Object.prototype.hasOwnProperty.call(messageKeys, code))
    return messageKeys[code];
  if (status === 401) return 'offline_reconnect';
  if (status === 403 || status === 404) return 'offline_unavailable';
  if (status === 413) return 'offline_limit';
  return 'offline_action_failed';
}
