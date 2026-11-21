// Called by AttachmentLocalIntegrationTest against disposable local services only.
// No backend responses are mocked. Third-party analytics/fonts are blocked.
const { chromium } = require(process.env.CMMS_PLAYWRIGHT_MODULE || 'playwright');
const assert = require('node:assert/strict');
const fs = require('node:fs');

(async () => {
  const fixtures = JSON.parse(fs.readFileSync(process.argv[2], 'utf8'));
  const browser = await chromium.launch({
    headless: true,
    ...(process.env.CMMS_CHROME_PATH ? { executablePath: process.env.CMMS_CHROME_PATH } : {})
  });
  try {
    const context = await browser.newContext({ locale: 'en-US', acceptDownloads: true });
    await context.route('**/*', route => {
      const host = new URL(route.request().url()).hostname;
      return ['127.0.0.1', 'localhost'].includes(host) ? route.continue() : route.abort();
    });
    const page = await context.newPage();
    await page.goto('http://127.0.0.1:3007/account/login');
    await page.locator('input[name="email"]').fill(fixtures.email);
    await page.locator('input[name="password"]').fill(fixtures.password);
    await page.locator('button[type="submit"]').click();
    await page.waitForURL('**/app/**');
    await page.evaluate(() => localStorage.removeItem('attachment-executed'));
    console.log('PASS real frontend login');

    await page.goto('http://127.0.0.1:3007/app/files');
    const fileRow = page.getByText('fake.pdf', { exact: true }).first();
    await fileRow.waitFor();
    const download = page.waitForEvent('download');
    await fileRow.click();
    assert.equal((await download).suggestedFilename(), 'fake.pdf');
    console.log('PASS real Files screen downloads disguised HTML');

    for (const file of fixtures.files) {
      if (!file.nativeStorage) {
        const headers = await page.evaluate(async url => {
          const response = await fetch(url);
          return { status: response.status, headers: Object.fromEntries(response.headers) };
        }, file.url);
        assert.equal(headers.status, 200, file.name);
        assert.equal(headers.headers['x-content-type-options'], 'nosniff', file.name);
        assert.equal(headers.headers['content-security-policy'],
          "sandbox; default-src 'none'; base-uri 'none'; form-action 'none'; frame-ancestors 'none'", file.name);
        assert.ok(headers.headers['content-disposition'].startsWith(file.image ? 'inline;' : 'attachment;'), file.name);
      }
      if (file.image) {
        assert.equal(await page.evaluate(url => new Promise(resolve => {
          const image = new Image();
          image.onload = () => resolve(image.naturalWidth > 0);
          image.onerror = () => resolve(false);
          image.src = url;
          document.body.append(image);
        }), file.url), true, file.name);
        console.log(`PASS browser image and response headers: ${file.name}`);
      } else {
        const pending = page.waitForEvent('download');
        await page.evaluate(url => {
          const link = document.createElement('a');
          link.href = url;
          document.body.append(link);
          link.click();
          link.remove();
        }, file.url);
        await pending;
        assert.equal(await page.evaluate(() => localStorage.getItem('attachment-executed')), null, file.name);
        console.log(`PASS browser forced download, no script execution: ${file.name}`);
      }
    }
    const publicContext = await browser.newContext({ locale: 'en-US', acceptDownloads: true });
    await publicContext.route('**/*', route => ['127.0.0.1', 'localhost'].includes(new URL(route.request().url()).hostname)
      ? route.continue() : route.abort());
    const publicPage = await publicContext.newPage();
    await publicPage.goto(`http://127.0.0.1:3007/request-portal/${fixtures.portalUuid}`);
    await publicPage.getByText('Harmless attachment test', { exact: true }).waitFor();
    await publicPage.getByRole('textbox').first().fill('Harmless attachment submission');
    await publicPage.locator('input[type="file"]').setInputFiles({
      name: 'portal-fake.pdf', mimeType: 'application/pdf',
      buffer: Buffer.from("<script>localStorage.setItem('attachment-executed','yes')</script>Harmless test")
    });
    const upload = publicPage.waitForResponse(response => response.url().includes('/files/upload/request-portal/') && response.request().method() === 'POST');
    const submitted = publicPage.waitForResponse(response => response.url().includes('/requests/portal/') && response.request().method() === 'POST');
    await publicPage.getByRole('button', { name: /submit request/i }).click();
    const uploaded = await upload;
    assert.equal(uploaded.status(), 200);
    assert.equal((await submitted).status(), 200);
    const created = await uploaded.json();
    const downloaded = publicPage.waitForEvent('download');
    await publicPage.evaluate(url => {
      const link = document.createElement('a'); link.href = url; document.body.append(link); link.click();
    }, created[0].url);
    assert.equal((await downloaded).suggestedFilename(), 'portal-fake.pdf');
    assert.equal(await publicPage.evaluate(() => localStorage.getItem('attachment-executed')), null);
    console.log('PASS anonymous public portal UI upload, request submission and safe download');
    await publicContext.close();
    await context.close();
  } finally {
    await browser.close();
  }
})().catch(error => {
  // Error text can contain signed URLs; do not persist credentials in the report.
  console.error(String(error.message).replace(/https?:\/\/[^\s"']+/g, '[URL redacted]'));
  process.exitCode = 1;
});
