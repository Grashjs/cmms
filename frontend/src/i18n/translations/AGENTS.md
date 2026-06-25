## Translation Rules

When asked to translate key-value pairs, translate only the values.

- Keep the keys unchanged.
- Do not add the key-value pairs to every translation language.

Example:

Input:
```json
{
  "write_better": "Write better agents.md"
}
```

Correct:
```json
{
  "en": {
    "write_better": "Write better agents.md"
  },
  "fr": {
    "write_better": "Écrire un meilleur agents.md"
  }
}
```
Incorrect:
```json
{
  "en": {
    "write_better": "Write better agents.md"
  },
  "fr": {
    "write_better": "Write better agents.md"
  }
}
```