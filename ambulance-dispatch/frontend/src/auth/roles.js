/*
 * Defines the user roles and permissions used by the React frontend.
 *
 * This file is the central source of truth for role-based access control
 * in the user interface.
 *
 * The frontend will use these definitions to decide:
 *
 * 1. Which dashboard a logged-in user should see.
 * 2. Which navigation links should be displayed.
 * 3. Which buttons and operations should be visible.
 * 4. Which route should open immediately after login.
 *
 * IMPORTANT SECURITY RULE:
 *
 * Hiding a button in React is not sufficient security.
 *
 * The Spring Boot backend must eventually verify the logged-in user's
 * role before allowing protected operations such as:
 *
 * - dispatching an ambulance;
 * - changing ambulance status;
 * - managing users;
 * - placing units into maintenance;
 * - viewing administrative information.
 *
 * The frontend role system controls presentation and navigation.
 * The backend role system will provide the actual authorization.
 */

/**
 * Immutable collection of application role identifiers.
 *
 * These values should eventually match the role names returned by the
 * Spring Boot authentication endpoint.
 *
 * Object.freeze prevents properties from being added, removed, or
 * replaced while the application is running.
 */
export const ROLE_KEYS = Object.freeze({
  /*
   * A call taker receives emergency calls and enters them into the
   * system.
   */
  CALL_TAKER: "CALL_TAKER",

  /*
   * A dispatcher monitors the priority queue and assigns available
   * ambulances to waiting calls.
   */
  DISPATCHER: "DISPATCHER",

  /*
   * A field crew sees the ambulance and emergency call currently
   * assigned to that crew.
   */
  FIELD_CREW: "FIELD_CREW",

  /*
   * A fleet supervisor manages ambulance availability, maintenance,
   * staffing, and out-of-service conditions.
   */
  FLEET_SUPERVISOR: "FLEET_SUPERVISOR",

  /*
   * An administrator can access every area of the application and will
   * eventually manage users and application configuration.
   */
  ADMINISTRATOR: "ADMINISTRATOR",
});

/**
 * Immutable collection of individual permissions.
 *
 * Permissions describe actions rather than job titles.
 *
 * This is more flexible than checking role names directly throughout
 * every React component.
 *
 * Example:
 *
 * Instead of writing:
 *
 *     if (user.role === "DISPATCHER")
 *
 * a component can ask:
 *
 *     roleHasPermission(
 *       user.role,
 *       PERMISSIONS.DISPATCH_AMBULANCE
 *     )
 *
 * This allows multiple roles to receive the same permission without
 * duplicating authorization logic across the application.
 */
export const PERMISSIONS = Object.freeze({
  /*
   * Allows the user to open the emergency-call intake screen.
   */
  VIEW_CALL_INTAKE: "VIEW_CALL_INTAKE",

  /*
   * Allows the user to submit a new emergency call.
   */
  CREATE_CALL: "CREATE_CALL",

  /*
   * Allows the user to view calls currently waiting in the min-heap
   * priority queue.
   */
  VIEW_WAITING_QUEUE: "VIEW_WAITING_QUEUE",

  /*
   * Allows the user to assign an available ambulance to the next
   * eligible emergency call.
   */
  DISPATCH_AMBULANCE: "DISPATCH_AMBULANCE",

  /*
   * Allows the user to escalate the priority of a waiting call.
   */
  ESCALATE_CALL: "ESCALATE_CALL",

  /*
   * Allows the user to view currently active ambulance assignments.
   */
  VIEW_ACTIVE_DISPATCHES: "VIEW_ACTIVE_DISPATCHES",

  /*
   * Allows the user to view the complete ambulance fleet.
   */
  VIEW_FLEET: "VIEW_FLEET",

  /*
   * Allows the user to register a new ambulance.
   */
  REGISTER_AMBULANCE: "REGISTER_AMBULANCE",

  /*
   * Allows the user to place ambulances into maintenance, mark them as
   * unstaffed, take them out of service, or return them to service.
   */
  MANAGE_FLEET_AVAILABILITY: "MANAGE_FLEET_AVAILABILITY",

  /*
   * Allows an ambulance crew to update the lifecycle of its active
   * dispatch.
   *
   * Example transitions:
   *
   *     DISPATCHED -> ON_SCENE
   *     ON_SCENE -> TRANSPORTING
   *     TRANSPORTING -> AT_HOSPITAL
   */
  UPDATE_ASSIGNED_AMBULANCE: "UPDATE_ASSIGNED_AMBULANCE",

  /*
   * Allows the user to view completed dispatch records.
   */
  VIEW_DISPATCH_HISTORY: "VIEW_DISPATCH_HISTORY",

  /*
   * Allows the user to view system statistics.
   */
  VIEW_STATISTICS: "VIEW_STATISTICS",

  /*
   * Allows an administrator to create, modify, deactivate, or assign
   * roles to application users.
   */
  MANAGE_USERS: "MANAGE_USERS",
});

/**
 * Creates one immutable role definition.
 *
 * This helper reduces duplication when defining roles and freezes the
 * nested arrays so that permissions and screen definitions cannot be
 * changed accidentally at runtime.
 *
 * @param {object} configuration configuration for one role
 * @param {string} configuration.key permanent role identifier
 * @param {string} configuration.displayName user-facing role name
 * @param {string} configuration.description explanation of the role
 * @param {string} configuration.defaultPath first page opened after login
 * @param {string[]} configuration.permissions allowed operations
 * @param {object[]} configuration.screens navigation screens for the role
 *
 * @returns {object} immutable role definition
 */
function createRoleDefinition({
  key,
  displayName,
  description,
  defaultPath,
  permissions,
  screens,
}) {
  /*
   * Validate required role text before creating the definition.
   *
   * Invalid role configuration should fail immediately during
   * application startup rather than causing unpredictable navigation
   * behavior later.
   */
  if (!key || typeof key !== "string") {
    throw new Error(
      "A role definition requires a nonblank string key."
    );
  }

  if (!displayName || typeof displayName !== "string") {
    throw new Error(
      `Role ${key} requires a display name.`
    );
  }

  if (!defaultPath || typeof defaultPath !== "string") {
    throw new Error(
      `Role ${key} requires a default path.`
    );
  }

  if (!Array.isArray(permissions)) {
    throw new Error(
      `Role ${key} permissions must be an array.`
    );
  }

  if (!Array.isArray(screens)) {
    throw new Error(
      `Role ${key} screens must be an array.`
    );
  }

  /*
   * Freeze each screen object separately.
   *
   * Freezing only the outer array would prevent adding or removing
   * screens, but it would not prevent changing an individual screen's
   * label or path.
   */
  const immutableScreens = Object.freeze(
    screens.map((screen) =>
      Object.freeze({
        ...screen,
      })
    )
  );

  /*
   * Return a completely immutable role definition.
   */
  return Object.freeze({
    key,
    displayName,
    description,
    defaultPath,

    /*
     * Copy the permissions before freezing them so the caller cannot
     * retain and later mutate the original array.
     */
    permissions: Object.freeze([...permissions]),

    screens: immutableScreens,
  });
}

/**
 * Complete role configuration for the application.
 *
 * Each role contains:
 *
 * - a permanent role key;
 * - a user-facing name;
 * - a role description;
 * - a default route;
 * - a set of permissions;
 * - navigation screens visible to that role.
 */
export const ROLE_DEFINITIONS = Object.freeze({
  /**
   * CALL TAKER
   *
   * Primary responsibility:
   *
   * Receive emergency calls and enter complete, correctly prioritized
   * information into the system.
   */
  [ROLE_KEYS.CALL_TAKER]: createRoleDefinition({
    key: ROLE_KEYS.CALL_TAKER,

    displayName: "Call Taker",

    description:
      "Receives emergency calls, records caller information, and places calls into the waiting priority queue.",

    defaultPath: "/call-intake",

    permissions: [
      PERMISSIONS.VIEW_CALL_INTAKE,
      PERMISSIONS.CREATE_CALL,
      PERMISSIONS.VIEW_WAITING_QUEUE,
      PERMISSIONS.ESCALATE_CALL,
    ],

    screens: [
      {
        id: "call-intake",
        label: "Call Intake",
        path: "/call-intake",
      },
      {
        id: "waiting-calls",
        label: "Waiting Calls",
        path: "/waiting-calls",
      },
    ],
  }),

  /**
   * DISPATCHER
   *
   * Primary responsibility:
   *
   * Monitor waiting calls and available ambulances, assign units, and
   * supervise active emergency responses.
   */
  [ROLE_KEYS.DISPATCHER]: createRoleDefinition({
    key: ROLE_KEYS.DISPATCHER,

    displayName: "Dispatcher",

    description:
      "Monitors the emergency-call queue, assigns ambulances, and tracks active dispatch operations.",

    defaultPath: "/dispatcher",

    permissions: [
      PERMISSIONS.VIEW_WAITING_QUEUE,
      PERMISSIONS.DISPATCH_AMBULANCE,
      PERMISSIONS.ESCALATE_CALL,
      PERMISSIONS.VIEW_ACTIVE_DISPATCHES,
      PERMISSIONS.VIEW_FLEET,
      PERMISSIONS.VIEW_DISPATCH_HISTORY,
      PERMISSIONS.VIEW_STATISTICS,
    ],

    screens: [
      {
        id: "dispatcher-dashboard",
        label: "Dispatch Board",
        path: "/dispatcher",
      },
      {
        id: "waiting-calls",
        label: "Waiting Calls",
        path: "/waiting-calls",
      },
      {
        id: "active-dispatches",
        label: "Active Dispatches",
        path: "/active-dispatches",
      },
      {
        id: "fleet",
        label: "Ambulance Fleet",
        path: "/fleet",
      },
      {
        id: "dispatch-history",
        label: "Dispatch History",
        path: "/dispatch-history",
      },
      {
        id: "statistics",
        label: "Statistics",
        path: "/statistics",
      },
    ],
  }),

  /**
   * FIELD CREW
   *
   * Primary responsibility:
   *
   * View the emergency call assigned to the crew and report ambulance
   * lifecycle progress.
   */
  [ROLE_KEYS.FIELD_CREW]: createRoleDefinition({
    key: ROLE_KEYS.FIELD_CREW,

    displayName: "Ambulance Crew",

    description:
      "Views the crew's assigned emergency call and updates the ambulance lifecycle during the response.",

    defaultPath: "/crew",

    permissions: [
      PERMISSIONS.UPDATE_ASSIGNED_AMBULANCE,
    ],

    screens: [
      {
        id: "crew-assignment",
        label: "Current Assignment",
        path: "/crew",
      },
      {
        id: "crew-status",
        label: "Update Status",
        path: "/crew/status",
      },
    ],
  }),

  /**
   * FLEET SUPERVISOR
   *
   * Primary responsibility:
   *
   * Manage ambulance readiness, maintenance, staffing, registration,
   * and return-to-service operations.
   */
  [ROLE_KEYS.FLEET_SUPERVISOR]: createRoleDefinition({
    key: ROLE_KEYS.FLEET_SUPERVISOR,

    displayName: "Fleet Supervisor",

    description:
      "Manages ambulance registration, maintenance, staffing, availability, and return-to-service operations.",

    defaultPath: "/fleet",

    permissions: [
      PERMISSIONS.VIEW_FLEET,
      PERMISSIONS.REGISTER_AMBULANCE,
      PERMISSIONS.MANAGE_FLEET_AVAILABILITY,
      PERMISSIONS.VIEW_DISPATCH_HISTORY,
      PERMISSIONS.VIEW_STATISTICS,
    ],

    screens: [
      {
        id: "fleet",
        label: "Ambulance Fleet",
        path: "/fleet",
      },
      {
        id: "register-ambulance",
        label: "Register Ambulance",
        path: "/fleet/register",
      },
      {
        id: "maintenance",
        label: "Maintenance",
        path: "/fleet/maintenance",
      },
      {
        id: "fleet-history",
        label: "Fleet History",
        path: "/fleet/history",
      },
      {
        id: "statistics",
        label: "Statistics",
        path: "/statistics",
      },
    ],
  }),

  /**
   * ADMINISTRATOR
   *
   * Primary responsibility:
   *
   * Maintain the overall application and manage user access.
   *
   * Administrators receive every currently defined permission.
   */
  [ROLE_KEYS.ADMINISTRATOR]: createRoleDefinition({
    key: ROLE_KEYS.ADMINISTRATOR,

    displayName: "Administrator",

    description:
      "Manages users, roles, system access, fleet operations, dispatch operations, and application reporting.",

    defaultPath: "/admin",

    /*
     * Object.values returns every permission value defined in the
     * PERMISSIONS object.
     */
    permissions: Object.values(PERMISSIONS),

    screens: [
      {
        id: "admin-dashboard",
        label: "Administration",
        path: "/admin",
      },
      {
        id: "call-intake",
        label: "Call Intake",
        path: "/call-intake",
      },
      {
        id: "dispatcher-dashboard",
        label: "Dispatch Board",
        path: "/dispatcher",
      },
      {
        id: "fleet",
        label: "Ambulance Fleet",
        path: "/fleet",
      },
      {
        id: "dispatch-history",
        label: "Dispatch History",
        path: "/dispatch-history",
      },
      {
        id: "statistics",
        label: "Statistics",
        path: "/statistics",
      },
      {
        id: "user-management",
        label: "User Management",
        path: "/admin/users",
      },
    ],
  }),
});

/**
 * Returns the full definition for a role.
 *
 * Time complexity:
 *
 *     O(1)
 *
 * ROLE_DEFINITIONS is a JavaScript object, so retrieving a role by its
 * key is a direct property lookup.
 *
 * @param {string} roleKey role identifier returned during login
 * @returns {object|null} role definition or null when not recognized
 */
export function getRoleDefinition(roleKey) {
  return ROLE_DEFINITIONS[roleKey] ?? null;
}

/**
 * Determines whether a role contains one particular permission.
 *
 * Time complexity:
 *
 *     O(p)
 *
 * Array.includes performs a linear search through the role's permission
 * array, where p is the number of permissions assigned to the role.
 *
 * The number of permissions is small and fixed, so this is appropriate
 * for the current application.
 *
 * @param {string} roleKey role being checked
 * @param {string} permission requested permission
 * @returns {boolean} true when the role contains the permission
 */
export function roleHasPermission(
  roleKey,
  permission
) {
  const role = getRoleDefinition(roleKey);

  /*
   * An unknown role receives no permissions.
   *
   * This follows a secure deny-by-default rule.
   */
  if (!role) {
    return false;
  }

  return role.permissions.includes(permission);
}

/**
 * Returns the first route that should open after a successful login.
 *
 * Unknown roles are redirected to the login page rather than being
 * allowed into an arbitrary application screen.
 *
 * @param {string} roleKey authenticated user's role
 * @returns {string} default route for the role
 */
export function getDefaultPathForRole(roleKey) {
  const role = getRoleDefinition(roleKey);

  return role
    ? role.defaultPath
    : "/login";
}

/**
 * Returns the navigation screens available to one role.
 *
 * Because each role's screens array is frozen, callers cannot add,
 * remove, or reorder the central role configuration.
 *
 * @param {string} roleKey authenticated user's role
 * @returns {object[]} immutable role-screen array
 */
export function getScreensForRole(roleKey) {
  const role = getRoleDefinition(roleKey);

  /*
   * Return a frozen empty array for an unknown role.
   */
  return role
    ? role.screens
    : Object.freeze([]);
}