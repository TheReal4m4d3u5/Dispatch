/*
 * Imports the ambulance crew's current-assignment screen.
 *
 * This screen displays:
 *
 * - the crew's assigned ambulance;
 * - the active emergency call;
 * - the ambulance's current location;
 * - the emergency destination;
 * - a responsive coordinate map and route.
 */
import CrewAssignmentScreen from
  "../components/crew/CrewAssignmentScreen";

/*
 * Imports the ambulance lifecycle-control screen.
 *
 * This screen displays only the state transitions that are legal from
 * the ambulance's current status.
 *
 * Examples:
 *
 *     DISPATCHED -> ON_SCENE
 *     ON_SCENE -> TRANSPORTING
 *     TRANSPORTING -> AT_HOSPITAL
 */
import CrewStatusScreen from
  "../components/crew/CrewStatusScreen";

/**
 * Selects the correct screen for an authenticated ambulance crew user.
 *
 * The crew role currently has two authorized paths:
 *
 *     /crew
 *         Displays the current assignment and navigation map.
 *
 *     /crew/status
 *         Displays guarded ambulance lifecycle controls and temporary
 *         state-transition history.
 *
 * This component keeps field-crew routing separate from App.jsx.
 *
 * That improves cohesion because:
 *
 * - App.jsx manages application-level login and role selection.
 * - FieldCrewWorkspace manages field-crew screen selection.
 * - CrewAssignmentScreen manages current-assignment presentation.
 * - CrewStatusScreen manages lifecycle-operation presentation.
 *
 * @param {object} props component properties
 *
 * @param {string} props.currentPath
 *        Current authorized field-crew screen path.
 *
 * @param {number|null|undefined} props.ambulanceId
 *        Ambulance assigned to the logged-in crew member.
 *
 * @returns {JSX.Element}
 *          The field-crew screen associated with the current path.
 */
function FieldCrewWorkspace({
  currentPath,
  ambulanceId,
}) {
  /*
   * Display the current emergency assignment and route map.
   */
  if (currentPath === "/crew") {
    return (
      <CrewAssignmentScreen
        ambulanceId={ambulanceId}
      />
    );
  }

  /*
   * Display the guarded ambulance lifecycle controls.
   *
   * The same ambulance ID is passed to CrewStatusScreen so it can find:
   *
   * 1. The assigned ambulance.
   * 2. The ambulance's active dispatch.
   * 3. The emergency call connected to that dispatch.
   * 4. The ambulance's initial lifecycle status.
   */
  if (currentPath === "/crew/status") {
    return (
      <CrewStatusScreen
        ambulanceId={ambulanceId}
      />
    );
  }

  /*
   * Defensive fallback for an unknown crew path.
   *
   * App.jsx normally prevents this case by allowing navigation only to
   * screens configured for the FIELD_CREW role.
   */
  return (
    <section className="panel workspace-placeholder">
      <header className="panel-header">
        <div>
          <p className="panel-eyebrow">
            Ambulance Crew
          </p>

          <h2>
            Crew Workspace
          </h2>
        </div>
      </header>

      <div className="empty-state">
        <h3>
          Screen not available
        </h3>

        <p>
          Select Current Assignment or Update Status from the ambulance
          crew navigation.
        </p>
      </div>
    </section>
  );
}

/*
 * Exports the field-crew workspace so App.jsx can render it for users
 * assigned the FIELD_CREW role.
 */
export default FieldCrewWorkspace;