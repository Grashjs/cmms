const DIACRITICS = /[\u0300-\u036f]/g;

/**
 * Lowercases, trims and strips diacritics so that "Álamos" and "alamos"
 * compare equal, matching the accent-insensitive search of the backend.
 */
export const normalizeSearchText = (value?: string | null): string => {
  const text = (value ?? '').toLowerCase().trim();
  try {
    return text.normalize('NFD').replace(DIACRITICS, '');
  } catch {
    return text;
  }
};

/**
 * True when `query` appears in `value`, ignoring case and accents.
 * An empty query matches everything.
 */
export const matchesSearch = (
  value: string | null | undefined,
  query: string | null | undefined
): boolean => normalizeSearchText(value).includes(normalizeSearchText(query));
