/*
 * Imports React's useState Hook.
 *
 * useState allows the root application component to remember:
 *
 * 1. Which seeded user is currently logged in.
 * 2. Which authorized screen the user is currently viewing.
 */
import { useState } from "react";

/*
 * Imports the global application styles.
 *
 * App.css contains:
 *
 * - the login-screen layout;
 * - the authenticated workspace layout;
 * - responsive tablet and mobile rules;
 * - ambulance-crew assignment and map styles.
 */
import "./App.css";

/*
 * Imports the seeded development login screen.
 *
 * LoginPage allows a user to select one seeded account from a dropdown.
 */
import LoginPage from "./pages/LoginPage";

/*
 * Imports the ambulance-crew workspace.
 *
 * This workspace contains:
 *
 * - the responsive current-assignment screen;
 * - the ambulance location map;
 * - the emergency destination marker;
 * - the future ambulance-status controls.
 */
import FieldCrewWorkspace from "./pages/FieldCrewWorkspace";

/*
 * Imports the central role configuration and helper functions.
 */
import {
  getDefaultPathForRole,
  getRoleDefinition,
  getScreensForRole,
  ROLE_KEYS,
} from "./auth/roles";

/**
 * Root React component for the Ambulance Call Center application.
 *
 * App controls the overall frontend workflow:
 *
 * 1. Show the login page before a user logs in.
 * 2. Store the selected seeded user.
 * 3. Open the user's default role workspace.
 * 4. Display only the screens assigned to that role.
 * 5. Pass the crew member's ambulance ID to the crew workspace.
 * 6. Allow the user to navigate between authorized screens.
 * 7. Clear the session when the user logs out.
 *
 * This is currently frontend-only demonstration authentication.
 *
 * Spring Security will later provide real authentication and backend
 * authorization.
 *
 * @returns {JSX.Element} the complete application interface
 */
function App() {
  /*
   * Stores the currently logged-in user.
   *
   * Before login:
   *
   *     currentUser === null
   *
   * Example field-crew user:
   *
   * {
   *     userId: 3,
   *     username: "Jordan Rivera",
   *     displayName: "Jordan Rivera",
   *     roleKey: "FIELD_CREW",
   *     ambulanceId: 101
   * }
   */
  const [currentUser, setCurrentUser] =
    useState(null);

  /*
   * Stores the currently selected authorized screen path.
   *
   * Examples:
   *
   *     /dispatcher
   *     /call-intake
   *     /crew
   *     /crew/status
   *     /fleet
   */
  const [currentPath, setCurrentPath] =
    useState("");

  /**
   * Processes a successful seeded-user login.
   *
   * LoginPage supplies:
   *
   * - user ID;
   * - display name;
   * - role;
   * - assigned ambulance ID, when applicable.
   *
   * @param {object} loginData selected seeded-user information
   * @param {number} loginData.userId unique user identifier
   * @param {string} loginData.username user display name
   * @param {string} loginData.displayName user display name
   * @param {string} loginData.roleKey assigned application role
   * @param {number|null} loginData.ambulanceId assigned ambulance
   *
   * @throws {Error} when the supplied role is not configured
   */
  function handleLogin(loginData) {
    /*
     * Verify that the selected role exists before storing the user.
     *
     * Unknown roles are denied by default.
     */
    const roleDefinition =
      getRoleDefinition(loginData.roleKey);

    if (!roleDefinition) {
      throw new Error(
        `Unknown application role: ${loginData.roleKey}`
      );
    }

    /*
     * Create an immutable browser-session user object.
     *
     * The ambulanceId field is especially important for the FIELD_CREW
     * role. It determines which ambulance, dispatch, call, and map
     * destination that crew member may view.
     */
    const authenticatedUser = Object.freeze({
      userId:
        loginData.userId ?? null,

      username:
        loginData.username,

      displayName:
        loginData.displayName
        ?? loginData.username,

      roleKey:
        loginData.roleKey,

      ambulanceId:
        loginData.ambulanceId ?? null,
    });

    /*
     * Store the selected user.
     */
    setCurrentUser(authenticatedUser);

    /*
     * Open the default screen assigned to this role.
     *
     * Examples:
     *
     * CALL_TAKER       -> /call-intake
     * DISPATCHER       -> /dispatcher
     * FIELD_CREW       -> /crew
     * FLEET_SUPERVISOR -> /fleet
     * ADMINISTRATOR    -> /admin
     */
    setCurrentPath(
      getDefaultPathForRole(
        authenticatedUser.roleKey
      )
    );
  }

  /**
   * Logs out the current user.
   *
   * Clearing currentUser causes React to render LoginPage again.
   *
   * Clearing currentPath prevents the next user from inheriting the
   * previous user's selected screen.
   */
  function handleLogout() {
    setCurrentUser(null);
    setCurrentPath("");
  }

  /**
   * Navigates to a screen only when that screen belongs to the current
   * user's role.
   *
   * Time complexity:
   *
   *     O(s)
   *
   * where s is the number of screens assigned to the role.
   *
   * @param {string} requestedPath path selected from the sidebar
   */
  function handleNavigate(requestedPath) {
    /*
     * A user must be logged in before navigation can occur.
     */
    if (!currentUser) {
      return;
    }

    /*
     * Retrieve only the screens authorized for the current role.
     */
    const authorizedScreens =
      getScreensForRole(
        currentUser.roleKey
      );

    /*
     * Confirm that the requested route exists in the role's screen
     * collection.
     */
    const isAuthorizedPath =
      authorizedScreens.some(
        (screen) =>
          screen.path === requestedPath
      );

    /*
     * Change screens only after the authorization check succeeds.
     */
    if (isAuthorizedPath) {
      setCurrentPath(requestedPath);
    }
  }

  /*
   * Display the seeded development login screen when no user is logged
   * in.
   */
  if (!currentUser) {
    return (
      <LoginPage onLogin={handleLogin} />
    );
  }

  /*
   * Retrieve the complete role definition for the authenticated user.
   */
  const roleDefinition =
    getRoleDefinition(
      currentUser.roleKey
    );

  /*
   * Defensive fallback.
   *
   * This should not normally occur because handleLogin validates the
   * role before storing the user.
   */
  if (!roleDefinition) {
    return (
      <LoginPage onLogin={handleLogin} />
    );
  }

  /*
   * Retrieve the sidebar screens assigned to the current role.
   */
  const authorizedScreens =
    getScreensForRole(
      currentUser.roleKey
    );

  /*
   * Find the definition for the active screen.
   *
   * Time complexity:
   *
   *     O(s)
   *
   * where s is the number of authorized screens.
   */
  const activeScreen =
    authorizedScreens.find(
      (screen) =>
        screen.path === currentPath
    );

  return (
    <div className="app authenticated-app">
      {/*
       * Main authenticated application header.
       */}
      <header className="workspace-header">
        <div>
          <p className="eyebrow">
            ICS 240 Data Structures
          </p>

          <h1>
            Ambulance Call Center and Dispatch System
          </h1>

          <p className="header-description">
            Role-based emergency call intake, priority-queue dispatch,
            ambulance tracking, and fleet operations.
          </p>
        </div>

        {/*
         * Current user and logout controls.
         */}
        <div className="user-session">
          <div className="user-session-details">
            <span className="user-name">
              {currentUser.displayName}
            </span>

            <span className="user-role">
              {roleDefinition.displayName}
            </span>

            {/*
             * The assigned ambulance is displayed only for users who
             * have one.
             */}
            {currentUser.ambulanceId && (
              <span className="user-role">
                Ambulance{" "}
                {currentUser.ambulanceId}
              </span>
            )}
          </div>

          <button
            className="secondary-button"
            type="button"
            onClick={handleLogout}
          >
            Log out
          </button>
        </div>
      </header>

      {/*
       * Authenticated sidebar and main content area.
       */}
      <div className="workspace-layout">
        <aside className="role-sidebar">
          <div className="role-summary">
            <p className="panel-eyebrow">
              Current workspace
            </p>

            <h2>
              {roleDefinition.displayName}
            </h2>

            <p>
              {roleDefinition.description}
            </p>
          </div>

          {/*
           * Render one navigation button for every screen assigned to
           * the current role.
           */}
          <nav
            className="role-navigation"
            aria-label={
              `${roleDefinition.displayName} navigation`
            }
          >
            {authorizedScreens.map(
              (screen) => {
                const isActive =
                  screen.path === currentPath;

                return (
                  <button
                    key={screen.id}
                    className={
                      isActive
                        ? "navigation-button active"
                        : "navigation-button"
                    }
                    type="button"
                    onClick={() =>
                      handleNavigate(
                        screen.path
                      )
                    }
                    aria-current={
                      isActive
                        ? "page"
                        : undefined
                    }
                  >
                    {screen.label}
                  </button>
                );
              }
            )}
          </nav>

          <div className="role-permission-summary">
            <strong>
              {
                roleDefinition
                  .permissions
                  .length
              }
            </strong>

            <span>
              frontend permissions assigned
            </span>
          </div>
        </aside>

        <main className="workspace-content">
          {/*
           * Active-screen heading.
           */}
          <header className="screen-header">
            <div>
              <p className="panel-eyebrow">
                {roleDefinition.displayName}
              </p>

              <h2>
                {activeScreen
                  ? activeScreen.label
                  : "Authorized Workspace"}
              </h2>
            </div>

            <div className="connection-status">
              <span
                className="status-indicator"
                aria-hidden="true"
              />

              <span>
                React running
              </span>
            </div>
          </header>

          {/*
           * Select and render the correct role workspace.
           *
           * currentUser is passed so the field-crew workspace can
           * receive the user's assigned ambulance ID.
           */}
          <RoleScreen
            currentUser={currentUser}
            currentPath={currentPath}
          />
        </main>
      </div>

      <footer className="app-footer">
        <p>
          Development role selection · React frontend on port 5173 ·
          Spring Boot backend on port 8080
        </p>
      </footer>
    </div>
  );
}

/**
 * Chooses the correct role-specific workspace.
 *
 * @param {object} props component properties
 * @param {object} props.currentUser logged-in user
 * @param {string} props.currentPath active authorized route
 *
 * @returns {JSX.Element} the selected role workspace
 */
function RoleScreen({
  currentUser,
  currentPath,
}) {
  /*
   * Dispatcher workspace.
   */
  if (
    currentUser.roleKey
    === ROLE_KEYS.DISPATCHER
  ) {
    return (
      <DispatcherScreen
        currentPath={currentPath}
      />
    );
  }

  /*
   * Call-taker workspace.
   */
  if (
    currentUser.roleKey
    === ROLE_KEYS.CALL_TAKER
  ) {
    return (
      <CallTakerScreen
        currentPath={currentPath}
      />
    );
  }

  /*
   * Ambulance-crew workspace.
   *
   * The ambulanceId connects the logged-in user to that crew's assigned
   * ambulance, active dispatch, emergency call, and map destination.
   */
  if (
    currentUser.roleKey
    === ROLE_KEYS.FIELD_CREW
  ) {
    return (
      <FieldCrewWorkspace
        currentPath={currentPath}
        ambulanceId={
          currentUser.ambulanceId
        }
      />
    );
  }

  /*
   * Fleet-supervisor workspace.
   */
  if (
    currentUser.roleKey
    === ROLE_KEYS.FLEET_SUPERVISOR
  ) {
    return (
      <FleetSupervisorScreen
        currentPath={currentPath}
      />
    );
  }

  /*
   * Administrator workspace.
   */
  if (
    currentUser.roleKey
    === ROLE_KEYS.ADMINISTRATOR
  ) {
    return (
      <AdministratorScreen
        currentPath={currentPath}
      />
    );
  }

  /*
   * Unknown roles receive no operational screen.
   */
  return (
    <EmptyWorkspace
      title="Unknown Role"
      description="This account does not have a configured application workspace."
    />
  );
}

/**
 * Displays dispatcher screens.
 *
 * @param {object} props component properties
 * @param {string} props.currentPath selected dispatcher route
 *
 * @returns {JSX.Element} dispatcher content
 */
function DispatcherScreen({
  currentPath,
}) {
  if (currentPath === "/dispatcher") {
    return (
      <section className="role-dashboard">
        <section
          className="statistics-grid"
          aria-label="Dispatcher statistics"
        >
          <StatisticCard
            label="Available ambulances"
            value={2}
            description="Units currently eligible for dispatch"
          />

          <StatisticCard
            label="Waiting calls"
            value={3}
            description="Calls currently stored in the min heap"
          />

          <StatisticCard
            label="Active dispatches"
            value={2}
            description="Ambulances currently assigned to calls"
          />

          <StatisticCard
            label="Completed dispatches"
            value={2}
            description="Dispatch records stored in history"
          />
        </section>

        <section className="dashboard-grid">
          <WorkspacePanel
            eyebrow="Priority queue"
            title="Next Waiting Call"
            description="The highest-priority waiting call will appear here."
          />

          <WorkspacePanel
            eyebrow="Fleet availability"
            title="Nearest Available Units"
            description="Available ambulances will be compared by distance, availability time, and ambulance ID."
          />
        </section>

        <WorkspacePanel
          eyebrow="Operations"
          title="Active Ambulance Dispatches"
          description="Assigned ambulances and their lifecycle states will appear here."
        />
      </section>
    );
  }

  if (currentPath === "/waiting-calls") {
    return (
      <EmptyWorkspace
        title="Waiting Emergency Calls"
        description="Calls will be displayed in priority and arrival-sequence order."
      />
    );
  }

  if (
    currentPath
    === "/active-dispatches"
  ) {
    return (
      <EmptyWorkspace
        title="Active Dispatches"
        description="Every ambulance currently assigned to an emergency call will appear here."
      />
    );
  }

  if (currentPath === "/fleet") {
    return (
      <EmptyWorkspace
        title="Ambulance Fleet"
        description="Dispatchers can view fleet locations and availability."
      />
    );
  }

  if (
    currentPath
    === "/dispatch-history"
  ) {
    return (
      <EmptyWorkspace
        title="Dispatch History"
        description="Completed dispatch records will be displayed here."
      />
    );
  }

  if (currentPath === "/statistics") {
    return (
      <EmptyWorkspace
        title="System Statistics"
        description="Call, queue, dispatch, and fleet statistics will appear here."
      />
    );
  }

  return (
    <EmptyWorkspace
      title="Dispatcher Workspace"
      description="Select an authorized dispatcher screen."
    />
  );
}

/**
 * Displays call-taker screens.
 */
function CallTakerScreen({
  currentPath,
}) {
  if (currentPath === "/call-intake") {
    return (
      <EmptyWorkspace
        title="Emergency Call Intake"
        description="The call-taker form will collect caller information, emergency details, priority, and coordinates."
      />
    );
  }

  return (
    <EmptyWorkspace
      title="Waiting Calls"
      description="The call taker can review and escalate waiting emergency calls."
    />
  );
}

/**
 * Displays fleet-supervisor screens.
 */
function FleetSupervisorScreen({
  currentPath,
}) {
  if (
    currentPath === "/fleet/register"
  ) {
    return (
      <EmptyWorkspace
        title="Register Ambulance"
        description="Register a new ambulance with an ID, name, location, and availability time."
      />
    );
  }

  if (
    currentPath
    === "/fleet/maintenance"
  ) {
    return (
      <EmptyWorkspace
        title="Maintenance and Availability"
        description="Manage maintenance, staffing, out-of-service, and return-to-service operations."
      />
    );
  }

  if (
    currentPath === "/fleet/history"
  ) {
    return (
      <EmptyWorkspace
        title="Fleet State History"
        description="Review append-only ambulance lifecycle transitions."
      />
    );
  }

  if (currentPath === "/statistics") {
    return (
      <EmptyWorkspace
        title="Fleet Statistics"
        description="Current ambulance lifecycle-state counts will appear here."
      />
    );
  }

  return (
    <EmptyWorkspace
      title="Ambulance Fleet"
      description="The fleet and each unit's operational state will appear here."
    />
  );
}

/**
 * Displays administrator screens.
 */
function AdministratorScreen({
  currentPath,
}) {
  if (
    currentPath === "/admin/users"
  ) {
    return (
      <EmptyWorkspace
        title="User Management"
        description="User accounts and role assignments will be added with Spring Security."
      />
    );
  }

  return (
    <EmptyWorkspace
      title="Administration"
      description={`Administrative placeholder for ${currentPath}.`}
    />
  );
}

/**
 * Reusable statistic card.
 */
function StatisticCard({
  label,
  value,
  description,
}) {
  return (
    <article className="statistic-card">
      <span className="statistic-label">
        {label}
      </span>

      <strong className="statistic-value">
        {value}
      </strong>

      <span className="statistic-description">
        {description}
      </span>
    </article>
  );
}

/**
 * Reusable dashboard information panel.
 */
function WorkspacePanel({
  eyebrow,
  title,
  description,
}) {
  return (
    <article className="panel">
      <header className="panel-header">
        <div>
          <p className="panel-eyebrow">
            {eyebrow}
          </p>

          <h2>
            {title}
          </h2>
        </div>
      </header>

      <div className="empty-state">
        <h3>
          No records yet
        </h3>

        <p>
          {description}
        </p>
      </div>
    </article>
  );
}

/**
 * Reusable placeholder for unfinished role screens.
 */
function EmptyWorkspace({
  title,
  description,
}) {
  return (
    <section className="panel workspace-placeholder">
      <header className="panel-header">
        <div>
          <p className="panel-eyebrow">
            Role-authorized screen
          </p>

          <h2>
            {title}
          </h2>
        </div>
      </header>

      <div className="empty-state">
        <h3>
          Screen ready for implementation
        </h3>

        <p>
          {description}
        </p>
      </div>
    </section>
  );
}

/*
 * Exports the root component for frontend/src/main.jsx.
 */
export default App;