# How to disable users
To disable users, you need to [run a SQL command](./Run%20SQL%20command.md):
Disable all users except for specified ones:
```sql
UPDATE own_user SET enabled = false WHERE lower(email) not in ('example@example.com', 'another@example.com');
```