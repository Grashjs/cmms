import { Database, Model, appSchema, tableSchema } from '@nozbe/watermelondb';
import SQLiteAdapter from '@nozbe/watermelondb/adapters/sqlite';
import { schemaMigrations } from '@nozbe/watermelondb/Schema/migrations';
import { OfflineError } from './types';

// The row payloads are JSON, while identity, availability and queue selectors are
// indexed SQLite columns. Business snapshots never share a row with a draft.
export class OfflineWorkOrderRecord extends Model {
  static table = 'offline_work_orders';
}
export class OfflineTaskRecord extends Model {
  static table = 'offline_tasks';
}
export class OfflineDraftRecord extends Model {
  static table = 'offline_drafts';
}
export class OfflineOperationRecord extends Model {
  static table = 'offline_operations';
}
export class OfflineCleanupRecord extends Model {
  static table = 'offline_cleanup';
}
export class OfflineDraftClockRecord extends Model {
  static table = 'offline_draft_clock';
}
export class OfflineFileRecord extends Model {
  static table = 'offline_files';
}
export class OfflineAssetRecord extends Model {
  static table = 'offline_assets';
}

export const offlineSchema = appSchema({
  version: 1,
  tables: [
    tableSchema({
      name: 'offline_files',
      columns: [
        { name: 'payload', type: 'string' },
        {
          name: 'server_file_id',
          type: 'number',
          isOptional: true,
          isIndexed: true
        }
      ]
    }),
    tableSchema({
      name: 'offline_assets',
      columns: [{ name: 'payload', type: 'string' }]
    }),
    tableSchema({
      name: 'offline_draft_clock',
      columns: [{ name: 'revision', type: 'number' }]
    }),
    tableSchema({
      name: 'offline_work_orders',
      columns: [
        { name: 'payload', type: 'string' },
        { name: 'prepared_at', type: 'string' },
        { name: 'available', type: 'boolean', isIndexed: true },
        { name: 'availability_revision', type: 'number' },
        { name: 'unavailable_reason', type: 'string', isOptional: true }
      ]
    }),
    tableSchema({
      name: 'offline_tasks',
      columns: [
        { name: 'work_order_id', type: 'string', isIndexed: true },
        { name: 'payload', type: 'string' },
        { name: 'available', type: 'boolean' },
        { name: 'unavailable_reason', type: 'string', isOptional: true }
      ]
    }),
    tableSchema({
      name: 'offline_drafts',
      columns: [
        { name: 'work_order_id', type: 'string', isIndexed: true },
        { name: 'task_id', type: 'string', isIndexed: true },
        { name: 'payload', type: 'string' },
        { name: 'revision', type: 'number' }
      ]
    }),
    tableSchema({
      name: 'offline_cleanup',
      columns: [
        { name: 'created_at', type: 'number' },
        { name: 'kind', type: 'string' },
        { name: 'token', type: 'string', isOptional: true }
      ]
    }),
    tableSchema({
      name: 'offline_operations',
      columns: [
        { name: 'work_order_id', type: 'string', isIndexed: true },
        { name: 'task_id', type: 'string', isIndexed: true },
        { name: 'request', type: 'string' },
        { name: 'status', type: 'string', isIndexed: true },
        { name: 'attempt_count', type: 'number' },
        { name: 'receipt', type: 'string', isOptional: true },
        { name: 'created_at', type: 'number' },
        { name: 'updated_at', type: 'number' },
        { name: 'submitted_revision', type: 'number', isOptional: true },
        { name: 'server_comment_id', type: 'number', isOptional: true },
        { name: 'snapshot_confirmed', type: 'boolean', isOptional: true }
      ]
    })
  ]
});

export const offlineMigrations = schemaMigrations({ migrations: [] });

export async function createOfflineDatabase(
  dbName: string,
  contextKey: string
): Promise<Database> {
  const adapter = new SQLiteAdapter({
    schema: offlineSchema,
    migrations: offlineMigrations,
    dbName,
    jsi: false
  });
  await adapter.initializingPromise;
  const database = new Database({
    adapter,
    modelClasses: [
      OfflineWorkOrderRecord,
      OfflineTaskRecord,
      OfflineDraftRecord,
      OfflineOperationRecord,
      OfflineCleanupRecord,
      OfflineDraftClockRecord,
      OfflineFileRecord,
      OfflineAssetRecord
    ]
  });
  const stored = await database.localStorage.get<string>(
    'atlas.offline.context'
  );
  if (stored && stored !== contextKey) {
    throw new OfflineError('CONTEXT_MISMATCH');
  }
  if (!stored)
    await database.localStorage.set('atlas.offline.context', contextKey);
  return database;
}

const opened = new Map<string, Promise<Database>>();

export async function openOfflineDatabase(
  contextKey: string
): Promise<Database> {
  let opening = opened.get(contextKey);
  if (!opening) {
    opening = (async () => {
      const Crypto = await import('expo-crypto');
      const { offlineStorageDirectory } = await import('./storage');
      const hash = await Crypto.digestStringAsync(
        Crypto.CryptoDigestAlgorithm.SHA256,
        contextKey
      );
      const directory = decodeURI(
        (await offlineStorageDirectory()).replace(/^file:\/\//, '')
      ).replace(/\/?$/, '/');
      return createOfflineDatabase(
        `${directory}atlas_offline_${hash}`,
        contextKey
      );
    })();
    opened.set(contextKey, opening);
    opening.catch(() => opened.delete(contextKey));
  }
  return opening;
}
