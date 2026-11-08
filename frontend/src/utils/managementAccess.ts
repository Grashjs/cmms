import { PermissionEntity, Role } from '../models/owns/role';

type Person = {
  id: number;
  companyId: number;
  ownsCompany: boolean;
  role: Role;
};
const roots = [
  'createPermissions',
  'viewPermissions',
  'viewOtherPermissions',
  'editOtherPermissions',
  'deleteOtherPermissions'
] as const;
const approvesRequests = (role: Role) =>
  role.code === 'LIMITED_ADMIN' ||
  role.viewPermissions?.includes(PermissionEntity.SETTINGS);

// Presentation only. The services reload the actor and enforce the same rules for every write.
export function dominates(owned: Role, requested: Role): boolean {
  return (
    !!owned &&
    !!requested &&
    owned.roleType === 'ROLE_CLIENT' &&
    requested.roleType === 'ROLE_CLIENT' &&
    roots.every(
      (root) =>
        Array.isArray(owned[root]) &&
        Array.isArray(requested[root]) &&
        requested[root].every((p) => owned[root].includes(p))
    ) &&
    (!approvesRequests(requested) || !!approvesRequests(owned)) &&
    (requested.code !== 'ADMIN' || owned.code === 'ADMIN')
  );
}

export function canManagePerson(actor: Person, target: Person): boolean {
  return (
    !!actor &&
    !!target &&
    actor.companyId === target.companyId &&
    actor.id !== target.id &&
    !target.ownsCompany &&
    actor.role.editOtherPermissions?.includes(
      PermissionEntity.PEOPLE_AND_TEAMS
    ) &&
    dominates(actor.role, target.role)
  );
}

export function canChangeOtherPassword(actor: Person, target: Person): boolean {
  return canManagePerson(actor, target) && actor.role.code === 'ADMIN';
}

export function canEditCustomRole(
  actor: Person,
  role: Role,
  operation: 'edit' | 'delete'
): boolean {
  return (
    !!actor &&
    role.code === 'USER_CREATED' &&
    actor.role.id !== role.id &&
    dominates(actor.role, role) &&
    actor.role.viewPermissions?.includes(PermissionEntity.SETTINGS) &&
    actor.role[
      operation === 'edit' ? 'editOtherPermissions' : 'deleteOtherPermissions'
    ]?.includes(PermissionEntity.SETTINGS)
  );
}
