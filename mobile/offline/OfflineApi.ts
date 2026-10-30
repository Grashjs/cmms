import {
  assertSnapshot,
  OfflineEligibility,
  OfflineError,
  OfflineSnapshot,
  OfflineTask,
  OFFLINE_LIMITS,
  OperationReceipt,
  OperationRequest,
  utf8ByteLength
} from './types';

export class OfflineApiError extends OfflineError {
  constructor(public readonly status: number, code: string) {
    super(code);
    this.name = 'OfflineApiError';
  }
}

interface OfflineApiOptions {
  serverUrl: string;
  companyId: number;
  userId: number;
  deviceId: string;
  getToken: () => Promise<string | null>;
  isCurrent: () => boolean;
  fetch?: typeof fetch;
}

/** One client is bound to one server/account generation, never to global Axios headers. */
export class OfflineApi {
  private readonly baseUrl: string;
  private readonly transport: typeof fetch;
  constructor(private readonly options: OfflineApiOptions) {
    this.baseUrl = options.serverUrl.replace(/\/+$/, '');
    this.transport = options.fetch || fetch;
  }

  private async request<T>(
    path: string,
    method: string,
    body?: unknown
  ): Promise<T> {
    if (!this.options.isCurrent())
      throw new OfflineApiError(0, 'CONTEXT_CHANGED');
    const token = await this.options.getToken();
    if (!this.options.isCurrent())
      throw new OfflineApiError(0, 'CONTEXT_CHANGED');
    if (!token) throw new OfflineApiError(401, 'AUTHENTICATION_REQUIRED');
    const controller = new AbortController();
    const timeout = setTimeout(() => controller.abort(), 30000);
    try {
      const response = await this.transport(`${this.baseUrl}${path}`, {
        method,
        headers: {
          Authorization: `Bearer ${token}`,
          Accept: 'application/json',
          'Content-Type': 'application/json'
        },
        ...(body === undefined ? {} : { body: JSON.stringify(body) }),
        signal: controller.signal,
        credentials: 'omit'
      });
      if (!this.options.isCurrent())
        throw new OfflineApiError(0, 'CONTEXT_CHANGED');
      const advertised = Number(response.headers.get('content-length'));
      if (advertised > OFFLINE_LIMITS.snapshotBytes) {
        controller.abort();
        throw new OfflineApiError(413, 'RESPONSE_LIMIT');
      }
      const text = await this.readBounded(response, controller);
      if (!this.options.isCurrent())
        throw new OfflineApiError(0, 'CONTEXT_CHANGED');
      let data: any = null;
      if (text) {
        try {
          data = JSON.parse(text);
        } catch {
          throw new OfflineApiError(response.status, 'INVALID_RESPONSE');
        }
      }
      if (!response.ok) {
        const code =
          data?.code ||
          (/^[A-Z_]+$/.test(data?.message)
            ? data.message
            : `HTTP_${response.status}`);
        throw new OfflineApiError(response.status, code);
      }
      return data as T;
    } catch (error) {
      if (error instanceof OfflineError) throw error;
      throw new OfflineApiError(0, 'NETWORK_UNAVAILABLE');
    } finally {
      clearTimeout(timeout);
    }
  }

  private async readBounded(
    response: Response,
    controller: AbortController
  ): Promise<string> {
    // Fetch streams are used where available. React Native's fetch may expose
    // only text(); both the Content-Length and decoded body are checked there.
    if (response.body?.getReader && typeof TextDecoder !== 'undefined') {
      const reader = response.body.getReader();
      const decoder = new TextDecoder();
      let bytes = 0;
      let text = '';
      for (;;) {
        const chunk = await reader.read();
        if (chunk.done) break;
        bytes += chunk.value.byteLength;
        if (bytes > OFFLINE_LIMITS.snapshotBytes) {
          controller.abort();
          await reader.cancel();
          throw new OfflineApiError(413, 'RESPONSE_LIMIT');
        }
        text += decoder.decode(chunk.value, { stream: true });
      }
      return text + decoder.decode();
    }
    const text = await response.text();
    if (utf8ByteLength(text) > OFFLINE_LIMITS.snapshotBytes)
      throw new OfflineApiError(413, 'RESPONSE_LIMIT');
    return text;
  }

  async eligibility(): Promise<OfflineEligibility> {
    const result = await this.request<OfflineEligibility>(
      '/mobile-offline/eligibility',
      'GET'
    );
    if (
      result.companyId !== this.options.companyId ||
      result.userId !== this.options.userId
    ) {
      throw new OfflineApiError(403, 'CONTEXT_MISMATCH');
    }
    return result;
  }

  async prepare(workOrderId: number): Promise<OfflineSnapshot> {
    const snapshot = await this.request<OfflineSnapshot>(
      `/mobile-offline/preparations/${workOrderId}`,
      'POST',
      { deviceId: this.options.deviceId }
    );
    assertSnapshot(snapshot);
    if (snapshot.workOrder.id !== workOrderId)
      throw new OfflineError('TARGET_MISMATCH');
    return snapshot;
  }

  release(workOrderId: number): Promise<void> {
    return this.request(
      `/mobile-offline/preparations/${workOrderId}?deviceId=${encodeURIComponent(
        this.options.deviceId
      )}`,
      'DELETE'
    );
  }

  sendOperation(operation: OperationRequest): Promise<OperationReceipt> {
    return this.request('/mobile-offline/operations', 'POST', operation);
  }

  async receipts(
    operationIds: string[]
  ): Promise<{ results: OperationReceipt[]; missingOperationIds: string[] }> {
    if (
      operationIds.length === 0 ||
      operationIds.length > OFFLINE_LIMITS.receiptIds ||
      new Set(operationIds).size !== operationIds.length
    ) {
      throw new OfflineError('RECEIPTS_LIMIT');
    }
    const result = await this.request<{
      results: OperationReceipt[];
      missingOperationIds: string[];
    }>('/mobile-offline/receipts', 'POST', { operationIds });
    const requested = new Set(operationIds);
    if (
      !result ||
      !Array.isArray(result.results) ||
      !Array.isArray(result.missingOperationIds)
    ) {
      throw new OfflineError('INVALID_RECEIPTS');
    }
    const returned = [
      ...result.results.map((receipt) => receipt?.operationId),
      ...result.missingOperationIds
    ];
    if (
      returned.length !== operationIds.length ||
      new Set(returned).size !== returned.length ||
      result.results.some((receipt) => !requested.has(receipt.operationId)) ||
      result.missingOperationIds.some((id) => !requested.has(id))
    ) {
      throw new OfflineError('INVALID_RECEIPTS');
    }
    return result;
  }

  /** Field allowlist prevents any composed display task/draft from being sent. */
  async patchTask(
    taskId: number,
    patch: { version: number; notes?: string; value?: string | number }
  ): Promise<OfflineTask> {
    const targets = ['notes', 'value'].filter((field) =>
      Object.prototype.hasOwnProperty.call(patch, field)
    );
    if (targets.length !== 1) throw new OfflineError('PATCH_FIELDS');
    const payload: {
      version: number;
      notes?: string;
      value?: string | number;
    } = { version: patch.version };
    if (Object.prototype.hasOwnProperty.call(patch, 'notes'))
      payload.notes = patch.notes;
    if (Object.prototype.hasOwnProperty.call(patch, 'value'))
      payload.value = patch.value;
    return this.request(`/tasks/${taskId}`, 'PATCH', payload);
  }
}
