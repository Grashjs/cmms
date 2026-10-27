/** Keep the server's verified Content-Type, disposition and sandbox policy intact. */
export function attachmentUrl(value: string): string | undefined {
  try {
    const url = new URL(value);
    if (!['https:', 'http:'].includes(url.protocol)) return undefined;
    return url.href;
  } catch {
    return undefined;
  }
}

export function openAttachment(value: string): void {
  const url = attachmentUrl(value);
  if (!url) throw new Error('Invalid attachment URL');
  window.open(url, '_blank', 'noopener,noreferrer');
}
