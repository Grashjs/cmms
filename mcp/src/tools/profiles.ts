import type { ToolDefinition } from './registry.js';

/**
 * Profiles decide what a client *sees*, not what it is *allowed* to do (konzept §4.3/E4).
 * The permission answer comes from the CMMS and from the user the API key belongs to; a
 * profile only keeps the visible tool set small enough for a model to choose well from.
 *
 * Consequence worth stating: widening a profile grants nothing. A `full` profile on a
 * read-only key still cannot write — the CMMS answers 403. Narrowing a profile, on the
 * other hand, does not protect a powerful key either; that is what the key's user is for.
 */

export interface Profile {
  name: string;
  description: string;
  includes(tool: ToolDefinition): boolean;
}

/** Tags whose entities make up the asset-management surface. */
const ASSET_TAGS = new Set([
  'Assets',
  'Asset Categories',
  'Asset Downtime',
  'Asset Analytics',
  'Locations',
  'Meters',
  'Meter Categories',
  'Readings',
  'Work Order Meter Triggers',
]);

export const PROFILES: Record<string, Profile> = {
  readonly: {
    name: 'readonly',
    description: 'Every reading tool, including the POST search and analytics queries. No writes.',
    includes: (tool) => tool.classification.readOnly,
  },
  core: {
    name: 'core',
    description:
      'Only the hand-curated tools: assets, work orders, locations, meters, parts, requests and preventive maintenance. Small, well described, reading and writing.',
    includes: (tool) => tool.curated,
  },
  'core-readonly': {
    name: 'core-readonly',
    description: 'The curated tools, reading only. The right default for an untrusted client.',
    includes: (tool) => tool.curated && tool.classification.readOnly,
  },
  assets: {
    name: 'assets',
    description:
      'Assets, locations, meters and readings, reading and writing. Nothing about work orders, people, purchasing or settings.',
    includes: (tool) => ASSET_TAGS.has(tool.operation.tag),
  },
  workorders: {
    name: 'workorders',
    description:
      'Work orders, requests, tasks and preventive maintenance, reading and writing, plus reading assets and locations for context.',
    includes: (tool) =>
      [
        'Work Orders',
        'Work Order History',
        'Work Order Analytics',
        'Work Order Categories',
        'Requests',
        'Request Analytics',
        'Request Triage',
        'Tasks',
        'Checklists',
        'Preventive Maintenances',
        'Schedules',
      ].includes(tool.operation.tag) ||
      (tool.classification.readOnly && ['Assets', 'Locations', 'Users', 'Teams'].includes(tool.operation.tag)),
  },
  full: {
    name: 'full',
    description:
      'Every operation the document describes, except the ones that cannot be expressed as a JSON tool call and the ones this server never offers. 300+ tools — expect a model to choose badly.',
    includes: () => true,
  },
};

export function resolveProfile(name: string): Profile {
  const profile = PROFILES[name];
  if (!profile) {
    throw new Error(
      `Unknown PROFILE ${JSON.stringify(name)}. Available: ${Object.keys(PROFILES).join(', ')}`,
    );
  }
  return profile;
}
