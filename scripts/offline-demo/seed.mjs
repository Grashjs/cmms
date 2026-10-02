#!/usr/bin/env node
// Seeds the Offline Work-Order Handoff demo through the Atlas REST API. Node 20+, no dependencies.
// Idempotent: existing users, location, asset and work order are reused; tasks are re-PATCHed
// (a full replace that keeps matching tasks and their values). Use reset.sh to clear demo state.
//
//   API_URL=http://<LAN-IP>:3000/api node scripts/offline-demo/seed.mjs
//
// Atlas has no "create user with password" endpoint. With INVITATION_VIA_EMAIL=false the admin
// invites each technician, and the technician then signs up with the invited email and role.

const API = (process.env.API_URL || 'http://localhost:3000/api').replace(/\/$/, '');
const PASSWORD = process.env.DEMO_PASSWORD || 'NorthwindDemo!2026'; // >= 12 chars, not a common password
const ADMIN = { email: 'admin@northwind.test', firstName: 'Nora', lastName: 'Admin' };
const TECHS = [
  { key: 'alice', email: 'alice@northwind.test', firstName: 'Alice', lastName: 'Ng' },
  { key: 'bob', email: 'bob@northwind.test', firstName: 'Bob', lastName: 'Okafor' },
  { key: 'carol', email: 'carol@northwind.test', firstName: 'Carol', lastName: 'Diaz' }
];
const WO_TITLE = 'CH-2 quarterly inspection';
const TASKS = [
  { label: 'Check refrigerant level', taskType: 'SUBTASK', options: [] },
  { label: 'Inspect condenser coils', taskType: 'SUBTASK', options: [] },
  { label: 'Verify safety controls', taskType: 'SUBTASK', options: [] },
  { label: 'Discharge pressure, psi', taskType: 'NUMBER', options: [] }
];

async function call(method, path, body, token) {
  const res = await fetch(API + path, {
    method,
    headers: {
      'Content-Type': 'application/json',
      ...(token && { Authorization: `Bearer ${token}` })
    },
    body: body === undefined ? undefined : JSON.stringify(body)
  });
  const text = await res.text();
  const data = text ? JSON.parse(text) : null;
  if (!res.ok) {
    const err = new Error(`${method} ${path} → ${res.status} ${text}`);
    err.status = res.status;
    throw err;
  }
  return data;
}

const signin = (email) =>
  call('POST', '/auth/signin', { email, password: PASSWORD }).then((r) => r.accessToken);

const signup = (u, extra) =>
  call('POST', '/auth/signup', {
    email: u.email,
    password: PASSWORD,
    firstName: u.firstName,
    lastName: u.lastName,
    phone: '+10000000000',
    language: 'EN',
    ...extra
  });

const findOne = async (token, entity, field, value, query = '') => {
  const page = await call(
    'POST',
    `/${entity}/search${query}`,
    { filterFields: [{ field, operation: 'eq', value }], pageNum: 0, pageSize: 1, sortField: 'id', direction: 'ASC' },
    token
  );
  return page.content[0] ?? null;
};

async function adminToken() {
  try {
    return await signin(ADMIN.email);
  } catch (e) {
    if (e.status !== 403 && e.status !== 401) throw e;
    console.log(`Creating company Northwind Facilities (${ADMIN.email})`);
    const res = await signup(ADMIN, { companyName: 'Northwind Facilities', employeesCount: 10 });
    return res.message; // signup returns the access token in `message`
  }
}

async function main() {
  const admin = await adminToken();

  const roles = await call('GET', '/roles', undefined, admin);
  const technician = roles.find((r) => r.code === 'TECHNICIAN');
  if (!technician) throw new Error('Technician role not found');

  const users = {};
  for (const t of TECHS) {
    let user = await findOne(admin, 'users', 'email', t.email, '?enabledOnly=false');
    if (!user) {
      console.log(`Inviting and signing up ${t.email}`);
      await call('POST', '/users/invite', { role: { id: technician.id }, emails: [t.email], disableSendingEmail: true }, admin);
      user = (await signup(t, { role: { id: technician.id } })).user;
    }
    users[t.key] = user;
  }

  const ensure = async (entity, field, value, body) =>
    (await findOne(admin, entity, field, value)) ?? call('POST', `/${entity}`, body, admin);

  const location = await ensure('locations', 'name', 'Plant 1', { name: 'Plant 1', address: '1 Northwind Way' });
  const asset = await ensure('assets', 'name', 'Chiller CH-2', { name: 'Chiller CH-2', location: { id: location.id } });
  const workOrder = await ensure('work-orders', 'title', WO_TITLE, {
    title: WO_TITLE,
    description: 'Quarterly inspection of chiller CH-2.',
    priority: 'MEDIUM',
    status: 'OPEN',
    requiredSignature: false,
    location: { id: location.id },
    asset: { id: asset.id },
    primaryUser: { id: users.alice.id },
    assignedTo: [{ id: users.alice.id }, { id: users.bob.id }]
  });
  const tasks = await call('PATCH', `/tasks/work-order/${workOrder.id}`, TASKS, admin);

  console.log(`\nSeeded against ${API}`);
  console.log(`Password for all users: ${PASSWORD}`);
  for (const [k, u] of Object.entries({ admin: { email: ADMIN.email }, ...users }))
    console.log(`  ${k.padEnd(6)} ${u.email}${u.id ? `  (user id ${u.id})` : ''}`);
  console.log(`Work order "${WO_TITLE}": id ${workOrder.id}`);
  console.log(`Tasks: ${tasks.map((t) => `${t.id} ${t.taskBase.label}`).join(' | ')}`);
}

main().catch((e) => {
  console.error(e.message);
  process.exit(1);
});
