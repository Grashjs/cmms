#!/usr/bin/env bash
# Returns the demo work order to its seeded state. Run seed.mjs once first.
# Direct SQL against the compose Postgres: status OPEN, primary Alice, assignees Alice + Bob,
# task values/notes cleared, "[Offline …" comments deleted, offline tables truncated.
# Extend this as later phases add tables.
set -euo pipefail
cd "$(dirname "$0")/../.."
POSTGRES_USER=$(grep '^POSTGRES_USER=' .env | cut -d= -f2)

docker exec -i atlas_db psql -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d atlas <<'SQL'
DO $$
DECLARE
  wo bigint := (SELECT id FROM work_order WHERE title = 'CH-2 quarterly inspection' ORDER BY id LIMIT 1);
  alice bigint := (SELECT id FROM own_user WHERE email = 'alice@northwind.test');
  bob bigint := (SELECT id FROM own_user WHERE email = 'bob@northwind.test');
BEGIN
  IF wo IS NULL THEN RAISE EXCEPTION 'Demo work order not found; run seed.mjs first'; END IF;

  -- status ordinal: OPEN=0
  UPDATE work_order SET status = 0, primary_user_id = alice, completed_by_id = NULL,
    completed_on = NULL, signature = NULL WHERE id = wo;

  DELETE FROM work_order_assigned_to WHERE work_order_id = wo;
  INSERT INTO work_order_assigned_to (work_order_id, assigned_to_id) VALUES (wo, alice), (wo, bob);

  -- task_type ordinal: SUBTASK=0 starts as 'OPEN', everything else empty
  UPDATE task SET value = CASE WHEN tb.task_type = 0 THEN 'OPEN' ELSE NULL END, notes = NULL
    FROM task_base tb WHERE task.task_base_id = tb.id AND task.work_order_id = wo;

  DELETE FROM comment WHERE work_order_id = wo AND content LIKE '[Offline %';

  IF to_regclass('offline_op') IS NOT NULL THEN EXECUTE 'TRUNCATE offline_op'; END IF;

  RAISE NOTICE 'Reset work order %', wo;
END $$;
SQL
