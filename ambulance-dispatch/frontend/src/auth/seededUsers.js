/*
 * Seeded development users for the Ambulance Call Center application.
 *
 * These accounts allow us to test role-based screens before the
 * Spring Boot backend, database, and Spring Security authentication
 * system are complete.
 *
 * Each seeded user has:
 *
 * 1. A unique numeric ID.
 * 2. A display name.
 * 3. A login username.
 * 4. A development password.
 * 5. One application role.
 * 6. An optional ambulance ID for field-crew users.
 *
 * IMPORTANT SECURITY WARNING:
 *
 * These accounts exist entirely in the React frontend.
 *
 * Anyone can inspect JavaScript delivered to the browser, so these
 * passwords are not secret and must never be used in production.
 *
 * Later, the Spring Boot backend will:
 *
 * 1. Store password hashes rather than plain-text passwords.
 * 2. Verify credentials with Spring Security.
 * 3. Retrieve users from a database.
 * 4. Return the authenticated user's assigned role.
 * 5. Protect REST endpoints with backend authorization.
 */

import { ROLE_KEYS } from "./roles";

/**
 * Shared development password used by every seeded account.
 *
 * Using one obvious password makes it easier to demonstrate all roles
 * while the real authentication system is not yet implemented.
 *
 * This value must be removed when Spring Security is added.
 */
export const DEVELOPMENT_PASSWORD = "demo123";

/**
 * Creates and validates one immutable seeded-user record.
 *
 * Centralizing object creation prevents malformed development accounts
 * from being added accidentally.
 *
 * @param {object} configuration seeded-user configuration
 * @param {number} configuration.userId unique positive user ID
 * @param {string} configuration.displayName name shown in the interface
 * @param {string} configuration.username value entered during login
 * @param {string} configuration.password development password
 * @param {string} configuration.roleKey role from ROLE_KEYS
 * @param {number|null} configuration.ambulanceId assigned ambulance ID
 *
 * @returns {object} immutable seeded-user record
 *
 * Time complexity:
 *
 *     O(1)
 *
 * The function performs a fixed number of validations and creates one
 * fixed-size object.
 *
 * Space complexity:
 *
 *     O(1)
 *
 * Each user stores a fixed number of fields.
 */
function createSeededUser({
  userId,
  displayName,
  username,
  password,
  roleKey,
  ambulanceId = null,
}) {
  /*
   * User IDs will eventually correspond to database primary keys.
   */
  if (!Number.isInteger(userId) || userId <= 0) {
    throw new Error(
      "A seeded user requires a positive integer user ID."
    );
  }

  /*
   * Store a trimmed, nonblank display name.
   */
  const normalizedDisplayName = requireText(
    displayName,
    "Display name"
  );

  /*
   * Usernames are normalized to lowercase so login matching is
   * case-insensitive.
   *
   * For example:
   *
   *     Dispatcher
   *     dispatcher
   *     DISPATCHER
   *
   * all resolve to the same account.
   */
  const normalizedUsername = requireText(
    username,
    "Username"
  ).toLowerCase();

  /*
   * The password must exist, even though it is temporary development
   * data.
   */
  const normalizedPassword = requireText(
    password,
    "Password"
  );

  /*
   * Confirm that the role is one of the recognized ROLE_KEYS values.
   */
  const validRoles = Object.values(ROLE_KEYS);

  if (!validRoles.includes(roleKey)) {
    throw new Error(
      `Unknown seeded-user role: ${roleKey}`
    );
  }

  /*
   * Only field-crew accounts currently need an assigned ambulance ID.
   *
   * A null value means that the user is not directly assigned to an
   * ambulance.
   */
  if (
    ambulanceId !== null
    && (!Number.isInteger(ambulanceId) || ambulanceId <= 0)
  ) {
    throw new Error(
      "Assigned ambulance ID must be a positive integer or null."
    );
  }

  /*
   * Return a frozen object so application code cannot change a user's
   * role or credentials while the React application is running.
   */
  return Object.freeze({
    userId,
    displayName: normalizedDisplayName,
    username: normalizedUsername,
    password: normalizedPassword,
    roleKey,
    ambulanceId,
  });
}

/**
 * Complete seeded account collection.
 *
 * One user is provided for every role currently defined by the
 * application.
 *
 * Development login credentials:
 *
 * CALL TAKER
 *     Username: calltaker
 *     Password: demo123
 *
 * DISPATCHER
 *     Username: dispatcher
 *     Password: demo123
 *
 * FIELD CREW
 *     Username: crew
 *     Password: demo123
 *
 * FLEET SUPERVISOR
 *     Username: fleet
 *     Password: demo123
 *
 * ADMINISTRATOR
 *     Username: admin
 *     Password: demo123
 */
export const SEEDED_USERS = Object.freeze([
  /*
   * Receives emergency calls and places them into the waiting
   * priority queue.
   */
  createSeededUser({
    userId: 1,
    displayName: "Maria Thompson",
    username: "calltaker",
    password: DEVELOPMENT_PASSWORD,
    roleKey: ROLE_KEYS.CALL_TAKER,
  }),

  /*
   * Monitors waiting calls and assigns available ambulances.
   */
  createSeededUser({
    userId: 2,
    displayName: "Daniel Brooks",
    username: "dispatcher",
    password: DEVELOPMENT_PASSWORD,
    roleKey: ROLE_KEYS.DISPATCHER,
  }),

  /*
   * Represents an ambulance crew member.
   *
   * This account is assigned to ambulance 101 so its future crew
   * dashboard can show only that ambulance's current assignment.
   */
  createSeededUser({
    userId: 3,
    displayName: "Jordan Rivera",
    username: "crew",
    password: DEVELOPMENT_PASSWORD,
    roleKey: ROLE_KEYS.FIELD_CREW,
    ambulanceId: 101,
  }),

  /*
   * Manages ambulance readiness, maintenance, staffing, and return to
   * service.
   */
  createSeededUser({
    userId: 4,
    displayName: "Priya Patel",
    username: "fleet",
    password: DEVELOPMENT_PASSWORD,
    roleKey: ROLE_KEYS.FLEET_SUPERVISOR,
  }),

  /*
   * Has access to every configured application permission and screen.
   */
  createSeededUser({
    userId: 5,
    displayName: "Morgan Reed",
    username: "admin",
    password: DEVELOPMENT_PASSWORD,
    roleKey: ROLE_KEYS.ADMINISTRATOR,
  }),
]);

/**
 * Finds a seeded user by username.
 *
 * Username matching is case-insensitive and ignores leading and
 * trailing whitespace.
 *
 * Examples:
 *
 *     findSeededUserByUsername("dispatcher")
 *     findSeededUserByUsername(" Dispatcher ")
 *     findSeededUserByUsername("DISPATCHER")
 *
 * all return the dispatcher account.
 *
 * Time complexity:
 *
 *     O(u)
 *
 * Array.find performs a linear search through u seeded users.
 *
 * Since the development account list contains only five users, a
 * linear search is simple and appropriate.
 *
 * A future database-backed implementation will use an indexed username
 * column for approximately O(log u) or database-index lookup behavior.
 *
 * @param {string} username username entered on the login form
 * @returns {object|null} matching user or null when no user exists
 */
export function findSeededUserByUsername(
  username
) {
  /*
   * Reject non-String input before calling trim or toLowerCase.
   */
  if (typeof username !== "string") {
    return null;
  }

  const normalizedUsername =
    username.trim().toLowerCase();

  /*
   * A blank username cannot match any seeded account.
   */
  if (!normalizedUsername) {
    return null;
  }

  return (
    SEEDED_USERS.find(
      (user) =>
        user.username === normalizedUsername
    ) ?? null
  );
}

/**
 * Validates development login credentials.
 *
 * The function first finds the username and then compares the supplied
 * password with the seeded password.
 *
 * Time complexity:
 *
 *     O(u)
 *
 * Username lookup performs one linear search through the seeded users.
 *
 * @param {string} username username entered by the user
 * @param {string} password password entered by the user
 * @returns {object|null} authenticated user or null when invalid
 */
export function authenticateSeededUser(
  username,
  password
) {
  /*
   * Find the account associated with the submitted username.
   */
  const user = findSeededUserByUsername(
    username
  );

  /*
   * Do not continue when the username does not exist.
   */
  if (!user) {
    return null;
  }

  /*
   * Passwords are compared exactly.
   *
   * Unlike usernames, passwords are intentionally case-sensitive.
   */
  if (user.password !== password) {
    return null;
  }

  /*
   * Return a safe authenticated-user object that excludes the password.
   *
   * The password should not be copied into application session state or
   * displayed elsewhere in the interface.
   */
  return Object.freeze({
    userId: user.userId,
    displayName: user.displayName,
    username: user.username,
    roleKey: user.roleKey,
    ambulanceId: user.ambulanceId,
  });
}

/**
 * Returns account information that may safely be displayed on the
 * development login page.
 *
 * Passwords are intentionally excluded from each returned account.
 * The page can display the shared DEVELOPMENT_PASSWORD separately.
 *
 * Time complexity:
 *
 *     O(u)
 *
 * One new public object is created for every seeded user.
 *
 * Space complexity:
 *
 *     O(u)
 *
 * The returned array contains u public account records.
 *
 * @returns {object[]} immutable public seeded-account list
 */
export function getPublicSeededUsers() {
  return Object.freeze(
    SEEDED_USERS.map((user) =>
      Object.freeze({
        userId: user.userId,
        displayName: user.displayName,
        username: user.username,
        roleKey: user.roleKey,
        ambulanceId: user.ambulanceId,
      })
    )
  );
}

/**
 * Validates and normalizes required text.
 *
 * @param {string} value value being validated
 * @param {string} fieldName field name used in errors
 * @returns {string} trimmed nonblank String
 *
 * @throws {Error} when the value is not a nonblank String
 */
function requireText(
  value,
  fieldName
) {
  if (
    typeof value !== "string"
    || !value.trim()
  ) {
    throw new Error(
      `${fieldName} cannot be blank.`
    );
  }

  return value.trim();
}