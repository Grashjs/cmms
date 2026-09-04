/**
 * Layer two of the tool model (konzept §4.3): hand-written names and descriptions for the
 * endpoints an agent actually reaches for.
 *
 * Curation is not cosmetic here. The document describes **6 of its 373 operations** — the
 * other 367 arrive with no summary and no description at all — so a purely generated tool
 * set tells a model the HTTP method and nothing else. The entries below are the difference
 * between "there is a tool called post_assets_search" and "this is how you find an asset".
 *
 * A curated name *replaces* the generated one rather than adding a second tool, so there is
 * exactly one way to call each endpoint. Everything not listed keeps its generated name and
 * a synthesised description (tools/describe.ts).
 *
 * These strings say what an endpoint is and how to address it. They deliberately do not
 * encode business rules — that is the CMMS's job (konzept §1, non-goal "no business logic").
 */

export interface CuratedTool {
  name: string;
  description: string;
}

/** Keyed by `<METHOD> <path>` exactly as the path appears in the OpenAPI document. */
export const CURATED_TOOLS: Record<string, CuratedTool> = {
  'POST /assets/search': {
    name: 'search_assets',
    description: [
      'Find assets (equipment, plant, technical installations) in this organisation.',
      'Filters go in `body.filterFields`, one entry per condition, and every entry is combined with AND — there is no top-level OR, only `alternatives` inside a single condition.',
      'A condition is `{field, operation, value}`; common operations are `eq`, `ne`, `cn` (contains), `gt`, `lt`, `in`.',
      'Page with `body.pageNum` (0-based) and `body.pageSize`; sort with `body.sortField` and `body.direction`.',
      'Enum-valued fields need `enumName` set, and only PRIORITY, STATUS and JS_DATE are supported there — other enum fields are stored as numbers, so a string comparison against them silently matches nothing.',
      'The organisation filter is added by the CMMS; do not send a company condition.',
    ].join(' '),
  },
  'GET /assets/{id}': {
    name: 'get_asset',
    description:
      'Read one asset in full: name, description, location, category, status, custom fields, parent and warranty data. Use search_assets first if you only have a name or a barcode.',
  },
  'GET /assets/mini': {
    name: 'list_assets_mini',
    description:
      'List every asset in the organisation as id plus name only. Cheap way to resolve a name to an id before calling get_asset; returns the whole list, unpaginated.',
  },
  'GET /assets/children/{id}': {
    name: 'get_asset_children',
    description:
      'List the direct children of an asset in the asset hierarchy. Assets nest (a plant contains an air handling unit contains a fan), so this walks exactly one level down.',
  },
  'GET /assets/location/{id}': {
    name: 'get_assets_at_location',
    description: 'List the assets placed at one location.',
  },
  'GET /assets/barcode': {
    name: 'find_asset_by_barcode',
    description:
      'Resolve a scanned barcode to its asset. Answers 404 when no asset carries that code.',
  },
  'POST /work-orders/search': {
    name: 'search_work_orders',
    description: [
      'Find work orders (maintenance jobs). Same filter shape as search_assets: `body.filterFields` conditions combined with AND, `pageNum`/`pageSize` for paging, `sortField`/`direction` for order.',
      'Filtering on priority or status needs `enumName` set to PRIORITY or STATUS respectively, because those columns hold enum ordinals rather than text.',
    ].join(' '),
  },
  'GET /work-orders/{id}': {
    name: 'get_work_order',
    description:
      'Read one work order in full: title, description, status, priority, assignees, asset, location, due date and completion data.',
  },
  'GET /work-orders/asset/{id}': {
    name: 'get_work_orders_for_asset',
    description:
      'List the work orders raised against one asset — the maintenance history of that asset.',
  },
  'GET /work-orders/urgent': {
    name: 'list_urgent_work_orders',
    description: 'List the work orders the CMMS considers urgent for the user the API key belongs to.',
  },
  'PATCH /work-orders/{id}/change-status': {
    name: 'change_work_order_status',
    description:
      'Move a work order to another status (OPEN, IN_PROGRESS, ON_HOLD, COMPLETE). Writing operation: it changes the record and can trigger notifications and automation rules inside the CMMS.',
  },
  'GET /work-order-histories/work-order/{id}': {
    name: 'get_work_order_history',
    description:
      'Read the change history of one work order — who changed what and when, as recorded by the CMMS audit trail.',
  },
  'POST /locations/search': {
    name: 'search_locations',
    description:
      'Find locations (sites, buildings, floors, rooms). Locations nest; the same filter shape as search_assets applies.',
  },
  'GET /locations/{id}': {
    name: 'get_location',
    description: 'Read one location in full, including its parent and address data.',
  },
  'GET /locations/mini': {
    name: 'list_locations_mini',
    description:
      'List every location as id plus name only. Use it to resolve a location name to an id.',
  },
  'POST /meters/search': {
    name: 'search_meters',
    description:
      'Find meters (counters attached to assets: operating hours, kWh, cubic metres). Same filter shape as search_assets.',
  },
  'GET /meters/{id}': {
    name: 'get_meter',
    description:
      'Read one meter: name, unit, update frequency, category and the asset it belongs to.',
  },
  'GET /meters/asset/{id}': {
    name: 'get_meters_for_asset',
    description: 'List the meters attached to one asset.',
  },
  'GET /readings/meter/{id}': {
    name: 'get_meter_readings',
    description: 'List the recorded readings of one meter, which is the measurement history behind it.',
  },
  'POST /readings': {
    name: 'create_meter_reading',
    description:
      'Record a new reading for a meter. Writing operation: a reading can cross a threshold and cause the CMMS to raise a work order on its own.',
  },
  'POST /parts/search': {
    name: 'search_parts',
    description:
      'Find spare parts and stock items. Same filter shape as search_assets; quantity and cost fields are numeric.',
  },
  'GET /parts/{id}': {
    name: 'get_part',
    description: 'Read one part: name, stock quantity, minimum quantity, cost and assigned assets.',
  },
  'POST /requests/search': {
    name: 'search_requests',
    description:
      'Find maintenance requests — reports that have not yet become work orders. Same filter shape as search_assets.',
  },
  'GET /requests/{id}': {
    name: 'get_request',
    description:
      'Read one maintenance request in full, including its approval state and the work order it turned into, if any.',
  },
  'POST /preventive-maintenances/search': {
    name: 'search_preventive_maintenances',
    description:
      'Find preventive maintenance schedules — the recurring plans from which work orders are generated. Same filter shape as search_assets.',
  },
  'GET /preventive-maintenances/{id}': {
    name: 'get_preventive_maintenance',
    description: 'Read one preventive maintenance schedule, including its trigger and task list.',
  },
  'GET /preventive-maintenances/{id}/recent-work-orders': {
    name: 'get_recent_work_orders_for_pm',
    description:
      'List the work orders most recently generated by one preventive maintenance schedule.',
  },
  'GET /asset-categories': {
    name: 'list_asset_categories',
    description:
      'List the asset categories of this organisation. Categories decide which custom fields an asset has, so read this before writing custom field values.',
  },
  'GET /tasks/work-order/{id}': {
    name: 'get_work_order_tasks',
    description: 'List the checklist tasks of one work order, with the value recorded for each.',
  },
  'GET /users/mini': {
    name: 'list_users_mini',
    description:
      'List the users of this organisation as id, name and role only. Use it to resolve a person to an id for an assignment.',
  },
  'GET /teams/mini': {
    name: 'list_teams_mini',
    description: 'List the teams of this organisation as id plus name only.',
  },
};

export function curatedFor(method: string, path: string): CuratedTool | undefined {
  return CURATED_TOOLS[`${method.toUpperCase()} ${path}`];
}
