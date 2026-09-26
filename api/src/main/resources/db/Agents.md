- Always add onDelete=Cascade to foreign keys, or onDelete=Set null if column is nullable
- When creating a table, if it extends CompanyAudit, add a sequence
  `<createSequence sequenceName="new_table_seq" startValue="1" incrementBy="50"/>`
