export const phoneRegExp =
  /^(\+?\d{0,4})?\s?-?\s?(\(?\d{3}\)?)\s?-?\s?(\(?\d{3}\)?)\s?-?\s?(\(?\d{4}\)?)?$/;

export const websiteRegExp =
  /((https?):\/\/)?(www.)?[a-z0-9]+(\.[a-z]{2,}){1,3}(#?\/?[a-zA-Z0-9#]+)*\/?(\?[a-zA-Z0-9-_]+=[a-zA-Z0-9-%]+&?)?$/;

export const emailRegExp =
  /^[a-zA-Z0-9.!#$%&'*+/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$/;

/**
 * Cleans a typed decimal number, accepting "," or "." as the decimal
 * separator (many locales, and the iOS decimal-pad, only offer a comma).
 * The result uses "." and keeps a single separator: "12,5" -> "12.5".
 * When both separators appear, the last one is the decimal separator and
 * the other is dropped as a thousands separator: "12.345,6" -> "12345.6".
 */
export const sanitizeDecimalInput = (value?: string | null): string => {
  let cleaned = (value ?? '').replace(/[^0-9.,]/g, '');
  if (cleaned.includes(',') && cleaned.includes('.')) {
    const decimal =
      cleaned.lastIndexOf(',') > cleaned.lastIndexOf('.') ? ',' : '.';
    cleaned = cleaned.split(decimal === ',' ? '.' : ',').join('');
  }
  cleaned = cleaned.replace(/,/g, '.');
  const firstDot = cleaned.indexOf('.');
  return firstDot === -1
    ? cleaned
    : cleaned.slice(0, firstDot + 1) +
        cleaned.slice(firstDot + 1).replace(/\./g, '');
};

/**
 * Parses a typed decimal number with "," or "." as separator and an
 * optional leading "-". Returns NaN for empty or invalid input so callers
 * can reject it instead of sending null.
 */
export const parseDecimalInput = (value?: string | number | null): number => {
  if (typeof value === 'number') return value;
  const trimmed = (value ?? '').trim();
  const sanitized = sanitizeDecimalInput(trimmed);
  if (sanitized === '' || sanitized === '.') return NaN;
  const parsed = parseFloat(sanitized);
  return trimmed.startsWith('-') ? -parsed : parsed;
};

export const isNumeric = (str: any) => {
  return (
    !isNaN(Number(str)) && // use type coercion to parse the _entirety_ of the string (`parseFloat` alone does not do this)...
    !isNaN(parseFloat(str))
  ); // ...and ensure strings of whitespace fail
};
