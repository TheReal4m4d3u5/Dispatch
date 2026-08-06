/*
 * Imports React's useState Hook.
 *
 * useState allows the login page to remember which seeded user is
 * currently selected in the dropdown.
 */
import { useState } from "react";

/*
 * Imports the permanent role identifiers and role definitions.
 *
 * ROLE_KEYS prevents role names from being typed repeatedly as raw
 * Strings.
 *
 * ROLE_DEFINITIONS provides the user-facing name and description for
 * each role.
 */
import {
  ROLE_DEFINITIONS,
  ROLE_KEYS,
} from "../auth/roles";

/**
 * Seeded development users displayed in the login dropdown.
 *
 * This is intentionally a simple demonstration login system.
 *
 * The user selects one account and presses "Log in." No password is
 * required during the current frontend-development stage.
 *
 * Each account has:
 *
 * - a unique ID;
 * - a person's display name;
 * - one application role;
 * - an optional ambulance assignment.
 *
 * IMPORTANT:
 *
 * This is not production authentication. A future Spring Security
 * implementation will verify real usernames and passwords on the
 * backend.
 */
const DEMO_USERS = Object.freeze([
  Object.freeze({
    userId: 1,
    displayName: "Maria Thompson",
    roleKey: ROLE_KEYS.CALL_TAKER,
    ambulanceId: null,
  }),

  Object.freeze({
    userId: 2,
    displayName: "Daniel Brooks",
    roleKey: ROLE_KEYS.DISPATCHER,
    ambulanceId: null,
  }),

  Object.freeze({
    userId: 3,
    displayName: "Jordan Rivera",
    roleKey: ROLE_KEYS.FIELD_CREW,

    /*
     * The ambulance crew account is assigned to ambulance 101.
     *
     * Later, this lets the crew workspace display only the assignment
     * associated with that ambulance.
     */
    ambulanceId: 101,
  }),

  Object.freeze({
    userId: 4,
    displayName: "Priya Patel",
    roleKey: ROLE_KEYS.FLEET_SUPERVISOR,
    ambulanceId: null,
  }),

  Object.freeze({
    userId: 5,
    displayName: "Morgan Reed",
    roleKey: ROLE_KEYS.ADMINISTRATOR,
    ambulanceId: null,
  }),
]);

/**
 * Displays the development login page.
 *
 * The user can move through one dropdown containing every seeded
 * account. Selecting an account automatically selects that person's
 * assigned role.
 *
 * Login process:
 *
 * 1. Select a person from the dropdown.
 * 2. Review the person's role.
 * 3. Press "Log in."
 * 4. App.jsx opens the default workspace for that role.
 *
 * @param {object} props component properties
 * @param {Function} props.onLogin function called after login
 *
 * @returns {JSX.Element} the login-page interface
 */
function LoginPage({ onLogin }) {
  /*
   * Stores the selected user's numeric ID as a String.
   *
   * HTML select values are always Strings, even when the original
   * underlying value represents a number.
   *
   * The dispatcher account is selected initially because the dispatch
   * board is the primary part of this project.
   */
  const [selectedUserId, setSelectedUserId] =
    useState("2");

  /*
   * Finds the complete user record associated with the selected ID.
   *
   * Number converts the dropdown String value into a number before
   * comparison.
   *
   * Time complexity:
   *
   *     O(u)
   *
   * Array.find may inspect up to u seeded users. Since this array has
   * only five records, the cost is extremely small.
   */
  const selectedUser =
    DEMO_USERS.find(
      (user) =>
        user.userId === Number(selectedUserId)
    ) ?? DEMO_USERS[0];

  /*
   * Retrieves the complete role definition for the selected account.
   *
   * This object contains:
   *
   * - role display name;
   * - role description;
   * - authorized screens;
   * - permissions;
   * - default workspace path.
   */
  const selectedRole =
    ROLE_DEFINITIONS[selectedUser.roleKey];

  /**
   * Updates the selected account when the dropdown changes.
   *
   * Because the select element is controlled by React, its current
   * value comes from selectedUserId.
   *
   * @param {React.ChangeEvent<HTMLSelectElement>} event
   *        browser dropdown-change event
   */
  function handleUserChange(event) {
    setSelectedUserId(event.target.value);
  }

  /**
   * Logs in using the currently selected seeded account.
   *
   * @param {React.FormEvent<HTMLFormElement>} event
   *        browser form-submission event
   */
  function handleSubmit(event) {
    /*
     * Prevent the browser from reloading the entire page.
     *
     * React should handle the state transition from the login page to
     * the role workspace.
     */
    event.preventDefault();

    /*
     * Pass the account information to App.jsx.
     *
     * App.jsx currently expects:
     *
     * {
     *     username,
     *     roleKey
     * }
     *
     * The additional fields are included now so future crew and user
     * features can use them without redesigning the login page.
     */
    onLogin({
      userId: selectedUser.userId,
      username: selectedUser.displayName,
      displayName: selectedUser.displayName,
      roleKey: selectedUser.roleKey,
      ambulanceId: selectedUser.ambulanceId,
    });
  }

  return (
    <main className="login-page">
      {/*
       * Left side of the login page.
       */}
      <section className="login-information">
        <p className="eyebrow">
          ICS 240 Data Structures
        </p>

        <h1>
          Ambulance Call Center and Dispatch System
        </h1>

        <p className="login-introduction">
          Select a seeded user to open that person&apos;s authorized
          workspace.
        </p>

        <div className="login-feature-list">
          <article className="login-feature">
            <strong>
              Stable min-heap queue
            </strong>

            <span>
              Emergency calls are ordered by priority and then by
              first-come-first-served arrival sequence.
            </span>
          </article>

          <article className="login-feature">
            <strong>
              Ambulance lifecycle
            </strong>

            <span>
              Each ambulance uses guarded state transitions and keeps an
              append-only state-change history.
            </span>
          </article>

          <article className="login-feature">
            <strong>
              Role-based workspaces
            </strong>

            <span>
              Each seeded user opens only the screens assigned to that
              person&apos;s role.
            </span>
          </article>
        </div>
      </section>

      {/*
       * Right side containing the user-selection form.
       */}
      <section className="login-form-section">
        <form
          className="login-form"
          onSubmit={handleSubmit}
        >
          <header className="login-form-header">
            <p className="panel-eyebrow">
              Development login
            </p>

            <h2>
              Select a user
            </h2>

            <p>
              Choose an account from the dropdown to preview its
              authorized screens.
            </p>
          </header>

          <div className="form-field">
            <label htmlFor="seeded-user">
              Application user
            </label>

            <select
              id="seeded-user"
              name="seeded-user"
              value={selectedUserId}
              onChange={handleUserChange}
            >
              {/*
               * Creates one dropdown option for every seeded user.
               *
               * The visible option includes both the user's name and
               * role so it is easy to move between workspaces.
               *
               * Time complexity:
               *
               *     O(u)
               *
               * React visits each user once to create the options.
               */}
              {DEMO_USERS.map((user) => {
                const role =
                  ROLE_DEFINITIONS[user.roleKey];

                return (
                  <option
                    key={user.userId}
                    value={user.userId}
                  >
                    {user.displayName}
                    {" — "}
                    {role.displayName}
                  </option>
                );
              })}
            </select>
          </div>

          {/*
           * Displays information about the selected account before the
           * user logs in.
           */}
          <aside className="selected-role-summary">
            <strong>
              {selectedUser.displayName}
            </strong>

            <p>
              Role: {selectedRole.displayName}
            </p>

            <p>
              {selectedRole.description}
            </p>

            {/*
             * The ambulance assignment is displayed only for field-crew
             * accounts.
             */}
            {selectedUser.ambulanceId && (
              <p>
                Assigned ambulance:{" "}
                {selectedUser.ambulanceId}
              </p>
            )}

            <span>
              {selectedRole.screens.length} authorized screens
            </span>
          </aside>

          <button
            className="primary-button"
            type="submit"
          >
            Log in as {selectedUser.displayName}
          </button>

          <p className="development-warning">
            This seeded dropdown will later be replaced by Spring
            Security authentication.
          </p>
        </form>
      </section>
    </main>
  );
}

/*
 * Exports LoginPage for use by App.jsx.
 */
export default LoginPage;