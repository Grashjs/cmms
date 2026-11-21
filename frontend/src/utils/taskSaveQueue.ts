import type { Task } from '../models/owns/tasks';

export type TaskFields = Partial<Pick<Task, 'value' | 'notes'>>;
export type TaskDraft = {
  value?: { revision: number; value: Task['value'] };
  notes?: { revision: number; value: string };
};

export class TaskSavePausedError extends Error {
  constructor() { super('task_save_paused'); }
}

/** Serialize connected saves of a task, retaining the last acknowledged server version. */
export class TaskSaveQueue {
  private versions = new Map<number, number>();

  private tails = new Map<number, Promise<Task>>();

  private paused = new Set<number>();

  constructor(private send: (id: number, patch: TaskFields & { version?: number }) => Promise<Task>) {}

  observe(task: Task): void {
    if (this.tails.has(task.id) || this.paused.has(task.id)) return;
    this.acknowledge(task);
  }

  private acknowledge(task: Task): void {
    const current = this.versions.get(task.id);
    if (task.version !== undefined && (current === undefined || task.version >= current)) {
      this.versions.set(task.id, task.version);
    }
  }

  enqueue(id: number, fields: TaskFields): Promise<Task> {
    const captured = { ...fields };
    const previous = this.tails.get(id) || Promise.resolve();
    const next = previous.catch(() => undefined).then(async () => {
      if (this.paused.has(id)) throw new TaskSavePausedError();
      try {
        const saved = await this.send(id, { ...captured, version: this.versions.get(id) });
        this.acknowledge(saved);
        return saved;
      } catch (error) {
        this.paused.add(id);
        throw error;
      }
    });
    this.tails.set(id, next);
    const clear = () => { if (this.tails.get(id) === next) this.tails.delete(id); };
    next.then(clear, clear);
    return next;
  }

  /** Only after explicit review/discard against a freshly fetched server task. */
  resume(task: Task): void {
    this.acknowledge(task);
    this.paused.delete(task.id);
  }
}

export function projectTaskDraft(server: Task, draft?: TaskDraft): Task {
  return { ...server, ...(draft?.value ? { value: draft.value.value } : {}),
    ...(draft?.notes ? { notes: draft.notes.value } : {}) };
}

export function acknowledgeTaskDraft(draft: TaskDraft, field: keyof TaskDraft, revision: number): TaskDraft {
  const next = { ...draft };
  if (next[field]?.revision === revision) delete next[field];
  return next;
}
