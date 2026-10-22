/*
 * Opt-in integration check against a disposable local API, web client, PostgreSQL
 * and Mailpit. It executes the real mobile networking code, not a native app.
 * See dev-docs/Secure invitations.md for setup and the two mail configurations.
 * No invitation link, password or session token is written to disk or stdout.
 */
const assert = require('node:assert/strict');
const { execFileSync } = require('node:child_process');
const { randomUUID, createHash } = require('node:crypto');
const Module = require('node:module');
const path = require('node:path');
const babel = require('@babel/core');

const mode = process.argv[2];
assert(['manual', 'smtp'].includes(mode), 'Specify manual or smtp');
const apiUrl = process.env.INVITATION_TEST_API || 'http://127.0.0.1:8098/';
const webUrl = process.env.INVITATION_TEST_WEB || 'http://127.0.0.1:3007';
const mailUrl = process.env.INVITATION_TEST_MAIL || 'http://127.0.0.1:18025';
for (const url of [apiUrl, webUrl, mailUrl]) {
  assert(
    ['localhost', '127.0.0.1'].includes(new URL(url).hostname),
    'Only disposable loopback services are allowed'
  );
}
const container =
  process.env.INVITATION_TEST_PG || 'cmms-invitation-e2e-postgres';
const mobileRoot = path.resolve(__dirname, '..');
const memory = new Map([['customApiUrl', apiUrl]]);
const storageWrites = [];
const nativeStorage = {
  getItem: async (key) => memory.get(key) ?? null,
  setItem: async (key, value) => {
    storageWrites.push(key);
    memory.set(key, value);
  },
  removeItem: async (key) => {
    storageWrites.push(key);
    memory.delete(key);
  }
};

// Replace only native bridges. HTTP, Redux's thunk, API parsing and login payload
// construction below are the exact implementations used by the mobile app.
const originalLoad = Module._load;
Module._load = function (request, parent, isMain) {
  if (request === '@react-native-async-storage/async-storage')
    return nativeStorage;
  if (request === 'react-native') return { Platform: { OS: 'ios' } };
  if (request === 'expo-constants')
    return { expoConfig: { extra: { API_URL: apiUrl } } };
  return originalLoad.call(this, request, parent, isMain);
};
require.extensions['.ts'] = (module, filename) => {
  assert(
    filename.startsWith(mobileRoot + path.sep),
    'Unexpected TypeScript source'
  );
  const { code } = babel.transformFileSync(filename, {
    babelrc: false,
    configFile: false,
    presets: [require.resolve('@babel/preset-typescript')],
    plugins: [require.resolve('@babel/plugin-transform-modules-commonjs')]
  });
  module._compile(code, filename);
};
const { inviteUsers } = require('../slices/user.ts');
const { authenticate } = require('../utils/authenticate.ts');
const { getUserInfos } = require('../utils/userApi.ts');
const api = require('../utils/api.ts').default;
const { chromium } = require(process.env.PLAYWRIGHT_MODULE || 'playwright');
const pause = (ms) => new Promise((resolve) => setTimeout(resolve, ms));
let stage = 'startup';
let checks = 0;
function passed(name) {
  checks++;
  console.log(`PASS ${name}`);
}
function sql(statement) {
  return execFileSync(
    'docker',
    [
      'exec',
      '-i',
      container,
      'psql',
      '-U',
      'invitation_test',
      '-d',
      'invitation_test',
      '-At',
      '-v',
      'ON_ERROR_STOP=1'
    ],
    { input: statement, encoding: 'utf8', stdio: ['pipe', 'pipe', 'pipe'] }
  ).trim();
}
const counts = () =>
  sql(
    'SELECT (SELECT count(*) FROM own_user), (SELECT count(*) FROM refresh_token);'
  );
async function post(endpoint, body) {
  const response = await fetch(new URL(endpoint, apiUrl), {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body)
  });
  return { status: response.status, data: await response.json() };
}
async function messagesFor(email) {
  const response = await fetch(`${mailUrl}/api/v1/messages`);
  assert(response.ok, 'Mailpit unavailable');
  const data = await response.json();
  return data.messages.filter((message) =>
    message.To.some((to) => to.Address === email)
  );
}
function signupBody(email, token, password) {
  return {
    email,
    password,
    invitationToken: token,
    firstName: 'Invitation',
    lastName: 'Test',
    phone: '',
    language: 'EN'
  };
}

(async () => {
  let browser;
  try {
    stage = 'legitimate company creation';
    const suffix = randomUUID().slice(0, 8);
    const password = `Invitation-${randomUUID()}!`;
    const owner = await post('auth/signup', {
      ...signupBody(`owner-${suffix}@example.test`, undefined, password),
      companyName: `Invitation ${mode} ${suffix}`,
      employeesCount: 5,
      timeZone: 'UTC'
    });
    assert(
      owner.status === 200 && owner.data.success && owner.data.refreshToken,
      `Owner signup failed (${owner.status})`
    );
    memory.set('accessToken', owner.data.message);
    memory.set('companyId', String(owner.data.user.companyId));
    const ownerToken = owner.data.message;
    const roles = await api.get('roles');
    const role = roles.find((entry) => entry.code === 'VIEW_ONLY');
    assert(role, 'Invitable role missing');
    passed(`${mode}: legitimate company created`);

    browser = await chromium.launch({
      headless: true,
      ...(process.env.CHROME_PATH
        ? { executablePath: process.env.CHROME_PATH }
        : {})
    });
    async function issue(email, manual) {
      memory.set('accessToken', ownerToken);
      const previousWrites = storageWrites.length;
      const noDispatch = () => {
        throw new Error('Invitation data must not enter Redux');
      };
      const response = await inviteUsers(role.id, [email], manual)(
        noDispatch,
        noDispatch,
        null
      );
      assert(
        response.success && response.invitations.length === 1,
        'Missing invitation data'
      );
      assert(
        storageWrites.length === previousWrites,
        'Invitation issuance wrote to native storage'
      );
      const invitation = response.invitations[0];
      assert(
        invitation.email === email &&
          new Date(invitation.expiresAt) > new Date(),
        'Invalid recipient or expiration'
      );
      const link = new URL(invitation.invitationLink);
      assert(
        link.origin === webUrl && link.pathname === '/account/register',
        'Wrong invitation destination'
      );
      const token = new URLSearchParams(link.hash.slice(1)).get(
        'invitationToken'
      );
      assert(
        token && token.length >= 43 && !link.search,
        'Expected a secure fragment token'
      );
      const digest = createHash('sha256').update(token).digest('hex');
      assert(
        sql(
          `SELECT count(*) FROM user_invitation WHERE token_hash='${digest}' AND consumed_at IS NULL;`
        ) === '1',
        'Digest missing in PostgreSQL'
      );
      assert(
        sql(`SELECT count(*) FROM own_user WHERE email='${email}';`) === '0',
        'Issuance created a user'
      );
      return { ...invitation, token };
    }
    async function newPage() {
      const context = await browser.newContext();
      // No server responses are mocked; avoid third-party telemetry in this test.
      await context.route('**/*', (route) => {
        const host = new URL(route.request().url()).hostname;
        return ['127.0.0.1', 'localhost'].includes(host)
          ? route.continue()
          : route.abort();
      });
      return { context, page: await context.newPage() };
    }
    async function rejectLink(invitation, label) {
      stage = label;
      const before = counts();
      const preview = await post('auth/invitations/preview', {
        token: invitation.token
      });
      assert(
        preview.status === 406,
        `Invalid preview status (${preview.status})`
      );
      const refused = await post(
        'auth/signup',
        signupBody(invitation.email, invitation.token, password)
      );
      assert(
        refused.status === 406 &&
          !refused.data.refreshToken &&
          !refused.data.accessToken,
        'Refusal issued a session'
      );
      const { context, page } = await newPage();
      try {
        await page.goto(invitation.invitationLink);
        await page.getByRole('alert').waitFor();
        assert(
          (await page.locator('input[name="password"]').count()) === 0,
          'Rejected link displayed signup form'
        );
        assert(
          await page.evaluate(
            () =>
              !localStorage.getItem('accessToken') &&
              !localStorage.getItem('refreshToken')
          ),
          'Rejected browser has a session'
        );
      } finally {
        await context.close();
      }
      assert(
        counts() === before,
        'Refused invitation created a user or refresh token'
      );
      passed(label);
    }
    async function acceptAndLogin(invitation, label) {
      stage = `${label}: web acceptance`;
      const preview = await post('auth/invitations/preview', {
        token: invitation.token
      });
      assert(
        preview.status === 200 &&
          preview.data.email === invitation.email &&
          preview.data.roleId === role.id &&
          preview.data.companyId === owner.data.user.companyId,
        'Preview scope mismatch'
      );
      const { context, page } = await newPage();
      try {
        await page.goto(invitation.invitationLink);
        const email = page.locator('input[name="email"]');
        await email.waitFor();
        assert(
          (await email.inputValue()) === invitation.email &&
            (await email.isDisabled()),
          'Invited email is not fixed'
        );
        await page.locator('input[name="firstName"]').fill('Mobile');
        await page.locator('input[name="lastName"]').fill('Invitee');
        await page.locator('input[name="password"]').fill(password);
        const responsePromise = page.waitForResponse(
          (response) =>
            response.url() === new URL('auth/signup', apiUrl).href &&
            response.request().method() === 'POST'
        );
        await page.locator('button[type="submit"]').click();
        const response = await responsePromise;
        assert(
          response.status() === 200 && (await response.json()).success,
          `Web signup failed (${response.status()})`
        );
        await page.waitForFunction(
          () => !!localStorage.getItem('accessToken') && !location.hash
        );
        // The invitation itself must not be persisted by the web client either.
        assert(
          await page.evaluate(
            (token) => !JSON.stringify(localStorage).includes(token),
            invitation.token
          ),
          'Web persisted an invitation token'
        );
      } finally {
        await context.close();
      }
      passed(`${label}: real web form accepted invitation`);

      stage = `${label}: mobile HTTP login`;
      memory.delete('accessToken');
      memory.delete('refreshToken');
      memory.delete('companyId');
      const session = await authenticate(invitation.email, password);
      assert(
        session.accessToken && session.refreshToken,
        'Mobile login did not return a session'
      );
      memory.set('accessToken', session.accessToken);
      memory.set('companyId', String(owner.data.user.companyId));
      const me = await getUserInfos();
      assert(
        me.email === invitation.email &&
          me.role.id === role.id &&
          me.companyId === owner.data.user.companyId,
        'Mobile identity mismatch'
      );
      assert(
        sql(
          `SELECT count(*) FROM user_invitation WHERE accepted_user_id=${me.id} AND consumed_at IS NOT NULL AND invited_by_id=${owner.data.user.id};`
        ) === '1',
        'Acceptance audit missing'
      );
      passed(
        `${label}: real mobile login request, identity and PostgreSQL audit`
      );
      await rejectLink(
        invitation,
        `${label}: consumed link refused without account/session`
      );
    }

    stage = `${mode}: mobile invitation issuance`;
    const email = `invite-${suffix}@example.test`;
    // false requests an email; with server mail disabled the link still works.
    let invitation = await issue(email, false);
    if (mode === 'manual') {
      await pause(500);
      assert(
        (await messagesFor(email)).length === 0,
        'Unexpected SMTP delivery'
      );
      passed('manual: mobile action returned link without SMTP');
      const previous = invitation;
      invitation = await issue(email, true);
      assert(invitation.token !== previous.token, 'Renewal reused a token');
      await rejectLink(
        previous,
        'manual: replaced link refused without account/session'
      );
      await acceptAndLogin(invitation, 'manual renewed invitation');
    } else {
      stage = 'SMTP delivery';
      let messages = [];
      for (let attempt = 0; attempt < 30; attempt++) {
        messages = await messagesFor(email);
        if (messages.length) break;
        await pause(500);
      }
      assert(messages.length === 1, 'Expected one local SMTP invitation');
      const message = await (
        await fetch(`${mailUrl}/api/v1/message/${messages[0].ID}`)
      ).json();
      assert(
        message.HTML.includes(invitation.invitationLink) ||
          message.Text.includes(invitation.invitationLink),
        'SMTP link does not match returned mobile link'
      );
      passed('smtp: SMTP catcher received the same link returned to mobile');
      await acceptAndLogin(invitation, 'SMTP invitation');

      stage = 'manual transmission with SMTP enabled';
      const manualEmail = `manual-${suffix}@example.test`;
      invitation = await issue(manualEmail, true);
      await pause(500);
      assert(
        (await messagesFor(manualEmail)).length === 0,
        'Opt-out still sent invitation email'
      );
      passed(
        'smtp: manual opt-out returns link without sending invitation email'
      );
      await acceptAndLogin(invitation, 'manual invitation with SMTP enabled');
    }
    console.log(
      `${checks} integration checks passed (${mode}); native UI not launched.`
    );
  } catch (error) {
    // Keep errors useful without logging responses, credentials or bearer links.
    console.error(
      `FAIL ${stage}: ${String(error.message)
        .replace(/https?:\/\/\S+/g, '[URL omitted]')
        .replace(/invitationToken[^\s]*/g, '[token omitted]')}`
    );
    process.exitCode = 1;
  } finally {
    if (browser) await browser.close();
    memory.clear();
  }
})();
