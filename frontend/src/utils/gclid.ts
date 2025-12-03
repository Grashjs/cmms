export function saveGclidFromUrl() {
  const params = new URLSearchParams(window.location.search);
  const gclid = params.get('gclid');

  if (gclid) {
    localStorage.setItem('gclid', gclid);
  }
}

export function getGclid() {
  return localStorage.getItem('gclid');
}
