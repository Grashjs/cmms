export function captureUtmParams() {
  const params = new URLSearchParams(window.location.search);

  const utm = {
    utm_source: params.get('utm_source') ?? null,
    utm_medium: params.get('utm_medium') ?? null,
    utm_campaign: params.get('utm_campaign') ?? null,
    utm_term: params.get('utm_term') ?? null,
    utm_content: params.get('utm_content') ?? null,
    gclid: params.get('gclid') ?? null
  };

  // If no UTM, fallback to referrer or direct
  if (!utm.utm_source && !utm.gclid) {
    if (document.referrer) {
      const ref = new URL(document.referrer);

      utm.utm_source = ref.hostname.includes('google.')
        ? 'organic'
        : ref.hostname;

      utm.utm_medium = 'referral';
    } else {
      utm.utm_source = 'direct';
      utm.utm_medium = 'none';
    }
  }

  // Save once
  localStorage.setItem('utm_params', JSON.stringify(utm));

  return utm;
}
