import type { BowtieStage } from '@holostaff/sdk';
import { holostaffSourceId, holostaffTenantId } from '../config';

/**
 * Holostaff: an optional in-product success manager for maintenance teams.
 *
 * Off by default. Nothing loads and nothing is contacted unless both
 * HOLOSTAFF_TENANT_ID and HOLOSTAFF_SOURCE_ID are set (runtime config,
 * so self-hosters can enable it on the prebuilt image without a
 * rebuild). The SDK sits behind a dynamic import, so with the ids
 * empty no visitor ever downloads its code.
 *
 * Called from the authenticated layout only, so it can never run on
 * the public request portal or the auth pages.
 *
 * Docs: https://docs.holostaff.ai
 */

// Journey stages, by the route the user is on. First match wins. The
// copilot uses this to know whether someone is importing their data,
// working their maintenance backlog, or looking at plans; everything
// else (stall detection, what to say, whether to say anything at all)
// comes from the journey map, not from this file.
const STAGE_ROUTES: [RegExp, BowtieStage][] = [
  [/^\/app\/imports/, 'onboarding'],
  [/^\/app\/(subscription|upgrade|downgrade)/, 'expansion'],
  [/^\/app\//, 'adoption']
];

export const isHolostaffEnabled = (): boolean =>
  Boolean(holostaffTenantId && holostaffSourceId);

let sdk: Promise<typeof import('@holostaff/sdk')> | null = null;
let currentStage: BowtieStage | null = null;

export const holostaffMarkPath = (pathname: string): void => {
  if (!isHolostaffEnabled()) return;
  sdk =
    sdk ??
    import('@holostaff/sdk').then((mod) => {
      mod.holostaff.init({
        tenantId: holostaffTenantId,
        sourceId: holostaffSourceId,
        // A manager's screen can show requester contact details, so mask
        // the content of every input in the session capture, not just
        // PII field types.
        observe: { maskAllInputs: true }
      });
      return mod;
    });
  const stage = STAGE_ROUTES.find(([pattern]) => pattern.test(pathname))?.[1];
  sdk
    .then(({ holostaff }) => {
      if (stage && stage !== currentStage) {
        currentStage = stage;
        holostaff.markStageEntry(stage);
      }
    })
    .catch((error) => {
      // eslint-disable-next-line no-console
      console.warn('Holostaff did not load:', error);
    });
};
