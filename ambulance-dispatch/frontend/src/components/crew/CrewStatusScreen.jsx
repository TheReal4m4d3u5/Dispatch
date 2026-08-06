/*
 * Imports React's useState Hook.
 *
 * useState allows this screen to maintain a temporary frontend copy of:
 *
 * 1. The ambulance's current lifecycle status.
 * 2. The simulated status-transition history.
 * 3. A success or error message after an operation.
 *
 * These values exist only in the browser for now. The Spring Boot
 * backend will eventually become the authoritative source of ambulance
 * lifecycle state.
 */
import { useState } from "react";

/*
 * Imports temporary seeded-data lookup functions.
 *
 * getAmbulanceById:
 *     Finds the ambulance assigned to the logged-in crew member.
 *
 * getActiveDispatchForAmbulance:
 *     Finds the active dispatch associated with that ambulance.
 *
 * getCallById:
 *     Finds the emergency call associated with the active dispatch.
 */
import {
  getActiveDispatchForAmbulance,
  getAmbulanceById,
  getCallById,
} from "../../data/seedData";

/**
 * Defines the legal status transitions that an ambulance crew may
 * perform from this screen.
 *
 * This object represents the same finite-state-machine rules used by
 * the Java Ambulance domain class.
 *
 * The object keys are current ambulance statuses.
 *
 * Each value is an immutable array of operations that may legally
 * follow that status.
 *
 * Example:
 *
 *     DISPATCHED -> ON_SCENE
 *
 * The crew cannot skip directly from DISPATCHED to TRANSPORTING because
 * the ambulance must first arrive at the emergency scene.
 *
 * IMPORTANT:
 *
 * These frontend checks improve the user interface, but the Spring Boot
 * backend must independently validate every transition.
 *
 * A user could bypass frontend JavaScript and send an HTTP request
 * manually, so backend validation remains required.
 */
const CREW_STATUS_ACTIONS = Object.freeze({
  /*
   * The ambulance has been assigned and is traveling to the emergency.
   */
  DISPATCHED: Object.freeze([
    Object.freeze({
      actionId: "arrive-on-scene",
      label: "Arrive On Scene",
      targetStatus: "ON_SCENE",
      description:
        "Confirm that the ambulance has reached the emergency location.",
    }),
  ]),

  /*
   * The crew is currently evaluating or treating the patient.
   *
   * Two outcomes are possible:
   *
   * 1. Begin transporting the patient.
   * 2. Complete treatment without transport.
   */
  ON_SCENE: Object.freeze([
    Object.freeze({
      actionId: "begin-transport",
      label: "Begin Transport",
      targetStatus: "TRANSPORTING",
      description:
        "Confirm that the patient is being transported to a hospital.",
    }),

    Object.freeze({
      actionId: "complete-without-transport",
      label: "Complete Without Transport",
      targetStatus: "RETURNING_TO_SERVICE",
      description:
        "Confirm that treatment ended on scene and no hospital transport is required.",
    }),
  ]),

  /*
   * The ambulance is transporting the patient to a hospital.
   */
  TRANSPORTING: Object.freeze([
    Object.freeze({
      actionId: "arrive-at-hospital",
      label: "Arrive At Hospital",
      targetStatus: "AT_HOSPITAL",
      description:
        "Confirm that the ambulance and patient have arrived at the hospital.",
    }),
  ]),

  /*
   * The ambulance has arrived at the hospital and must complete the
   * patient handoff.
   */
  AT_HOSPITAL: Object.freeze([
    Object.freeze({
      actionId: "complete-handoff",
      label: "Complete Patient Handoff",
      targetStatus:
        "CLEANING_AND_RESTOCKING",
      description:
        "Complete the hospital handoff and move the ambulance into cleaning and restocking.",
    }),
  ]),

  /*
   * The assignment ended without hospital transport.
   *
   * After the unit reaches its service location and is ready, it may
   * become available again.
   */
  RETURNING_TO_SERVICE: Object.freeze([
    Object.freeze({
      actionId: "return-from-scene",
      label: "Return To Service",
      targetStatus: "AVAILABLE",
      description:
        "Confirm that the ambulance is ready to receive another dispatch.",
    }),
  ]),

  /*
   * The ambulance completed a hospital transport and is being cleaned
   * and restocked.
   */
  CLEANING_AND_RESTOCKING: Object.freeze([
    Object.freeze({
      actionId: "complete-cleaning",
      label: "Complete Cleaning and Restocking",
      targetStatus: "AVAILABLE",
      description:
        "Confirm that cleaning and restocking are complete and the ambulance is ready.",
    }),
  ]),
});

/**
 * Displays the ambulance crew's lifecycle-update screen.
 *
 * This screen allows the crew to:
 *
 * 1. Review the currently assigned ambulance.
 * 2. Review the active emergency call.
 * 3. See the ambulance's current lifecycle state.
 * 4. View only the legal next operations.
 * 5. Simulate guarded state transitions.
 * 6. Review a temporary transition history.
 *
 * The screen is responsive and uses the same workspace styles as the
 * current-assignment screen.
 *
 * @param {object} props component properties
 *
 * @param {number|null|undefined} props.ambulanceId
 *        Ambulance assigned to the logged-in crew account.
 *
 * @returns {JSX.Element}
 *          The complete ambulance lifecycle-update screen.
 */
function CrewStatusScreen({ ambulanceId }) {
  /*
   * Find the ambulance assigned to the authenticated crew member.
   *
   * Current frontend time complexity:
   *
   *     O(a)
   *
   * where a is the number of seeded ambulances.
   *
   * The Java backend will eventually use:
   *
   *     HashMap<Integer, Ambulance>
   *
   * which provides average O(1) lookup by ambulance ID.
   */
  const ambulance = ambulanceId
    ? getAmbulanceById(ambulanceId)
    : null;

  /*
   * Find the ambulance's active dispatch.
   *
   * This may be null after the call has been completed.
   */
  const activeDispatch = ambulanceId
    ? getActiveDispatchForAmbulance(
        ambulanceId
      )
    : null;

  /*
   * Find the emergency call referenced by the active dispatch.
   *
   * Optional chaining prevents an error when activeDispatch is null.
   */
  const emergencyCall = activeDispatch
    ? getCallById(activeDispatch.callId)
    : null;

  /*
   * Stores the current simulated ambulance status.
   *
   * The seeded ambulance's status becomes the initial value.
   *
   * When no ambulance exists, an empty String is used. The component
   * returns an error message before attempting to use that value.
   */
  const [
    currentStatus,
    setCurrentStatus,
  ] = useState(
    ambulance?.status ?? ""
  );

  /*
   * Stores status transitions performed during this browser session.
   *
   * Each history entry contains:
   *
   * - a unique frontend ID;
   * - the previous status;
   * - the new status;
   * - an ISO timestamp;
   * - a readable reason.
   *
   * The real application will retrieve the authoritative append-only
   * history from the Spring Boot backend.
   */
  const [
    transitionHistory,
    setTransitionHistory,
  ] = useState([]);

  /*
   * Stores the most recent operation result.
   *
   * Example:
   *
   *     Ambulance status changed to ON_SCENE.
   */
  const [
    operationMessage,
    setOperationMessage,
  ] = useState("");

  /*
   * Stores whether the latest message represents a successful operation
   * or an error.
   *
   * Expected values:
   *
   *     "success"
   *     "error"
   *     ""
   */
  const [
    messageType,
    setMessageType,
  ] = useState("");

  /*
   * Return a clear message when the crew account is not associated with
   * an ambulance.
   */
  if (!ambulanceId) {
    return (
      <StatusScreenMessage
        title="No ambulance assigned"
        message="This crew account must be assigned to an ambulance before lifecycle operations can be performed."
      />
    );
  }

  /*
   * Return a clear message when the assigned ambulance ID does not
   * exist in the seeded fleet collection.
   */
  if (!ambulance) {
    return (
      <StatusScreenMessage
        title="Ambulance not found"
        message={`Ambulance ${ambulanceId} does not exist in the current fleet data.`}
      />
    );
  }

  /*
   * Retrieve the legal operations for the current status.
   *
   * The empty frozen array means there are no crew-controlled
   * transitions from the current state.
   *
   * Examples:
   *
   * AVAILABLE:
   *     The ambulance is ready for the dispatcher. The field crew does
   *     not select another state from this screen.
   *
   * MAINTENANCE:
   *     A fleet supervisor must manage the unit.
   */
  const availableActions =
    CREW_STATUS_ACTIONS[currentStatus]
    ?? Object.freeze([]);

  /**
   * Applies one simulated lifecycle transition.
   *
   * This method follows a guarded-transition process:
   *
   * 1. Find the requested action.
   * 2. Confirm that it belongs to the current state.
   * 3. Preserve the previous status.
   * 4. Update the current status.
   * 5. Append an immutable history entry.
   * 6. Display a success message.
   *
   * Time complexity:
   *
   *     O(k + h)
   *
   * where:
   *
   *     k = number of possible actions for the current state
   *     h = number of temporary history entries
   *
   * The action lookup is O(k).
   *
   * The array spread used to append history creates a new array and
   * copies h existing references, making the history update O(h).
   *
   * @param {string} actionId
   *        Identifier of the lifecycle operation selected by the crew.
   */
  function handleStatusAction(actionId) {
    /*
     * Find the selected operation only among actions that are legal from
     * the current state.
     *
     * This prevents an operation from being applied merely because an
     * action ID exists somewhere else in the application.
     */
    const selectedAction =
      availableActions.find(
        (action) =>
          action.actionId === actionId
      );

    /*
     * Reject an unknown or illegal action.
     *
     * This branch should not normally be reached through the rendered
     * buttons, but it protects the component from inconsistent calls.
     */
    if (!selectedAction) {
      setMessageType("error");

      setOperationMessage(
        `The requested operation is not legal from status ${currentStatus}.`
      );

      return;
    }

    /*
     * Preserve the current state before changing it.
     *
     * The preserved value is stored in the transition-history entry.
     */
    const previousStatus = currentStatus;

    /*
     * Capture one timestamp for the complete operation.
     *
     * Using one value ensures the displayed message and history entry
     * refer to the same transition moment.
     */
    const changedAt =
      new Date().toISOString();

    /*
     * Create an immutable temporary history record.
     *
     * Date.now() and the existing history length provide a sufficiently
     * unique key for this local demonstration.
     *
     * The backend will eventually assign authoritative identifiers.
     */
	/*
	 * Create the history entry inside the functional state update.
	 *
	 * existingHistory.length provides a deterministic sequence number.
	 * This avoids Date.now(), which the React purity rule identifies as an
	 * impure function.
	 *
	 * The ambulance ID and sequence number produce identifiers such as:
	 *
	 *     101-transition-1
	 *     101-transition-2
	 *     101-transition-3
	 */
	setTransitionHistory(
	  (existingHistory) => {
	    const historyEntry = Object.freeze({
	      historyId:
	        `${ambulance.ambulanceId}-transition-${existingHistory.length + 1}`,

	      previousStatus,

	      newStatus:
	        selectedAction.targetStatus,

	      changedAt,

	      reason:
	        selectedAction.label,
	    });

	    /*
	     * Return a new array instead of modifying the existing React state.
	     */
	    return [
	      ...existingHistory,
	      historyEntry,
	    ];
	  }
	);

    /*
     * Apply the new lifecycle status.
     */
    setCurrentStatus(
      selectedAction.targetStatus
    );

    /*
     * Append the history record without mutating the existing React
     * state array.
     *
     * React state should be treated as immutable.
     */

    /*
     * Display a success message.
     */
    setMessageType("success");

    setOperationMessage(
      `${ambulance.name} changed from ${previousStatus} to ${selectedAction.targetStatus}.`
    );
  }

  return (
    <section className="crew-status-screen">
      {/*
       * Quick summary cards.
       */}
      <section
        className="statistics-grid"
        aria-label="Ambulance lifecycle summary"
      >
        <StatusStatistic
          label="Ambulance"
          value={ambulance.name}
          description={`Unit ${ambulance.ambulanceId}`}
        />

        <StatusStatistic
          label="Current Status"
          value={currentStatus}
          description="Current simulated lifecycle state"
        />

        <StatusStatistic
          label="Active Call"
          value={
            emergencyCall
              ? emergencyCall.callId
              : "None"
          }
          description={
            emergencyCall
              ? emergencyCall.priority
              : "No active emergency assignment"
          }
        />

        <StatusStatistic
          label="Transitions"
          value={transitionHistory.length}
          description="Changes recorded in this browser session"
        />
      </section>

      {/*
       * Two-column lifecycle workspace.
       */}
      <section className="crew-status-grid">
        <article className="panel">
          <header className="panel-header">
            <div>
              <p className="panel-eyebrow">
                Lifecycle controls
              </p>

              <h2>
                Update Ambulance Status
              </h2>
            </div>

            <span className="panel-count">
              {ambulance.ambulanceId}
            </span>
          </header>

          <div className="crew-panel-body">
            {/*
             * Current status display.
             */}
            <section className="current-status-card">
              <span className="current-status-label">
                Current lifecycle state
              </span>

              <strong className="current-status-value">
                {currentStatus}
              </strong>

              <p>
                Only operations that are legal from this state are
                displayed below.
              </p>
            </section>

            {/*
             * Operation result message.
             *
             * role="status" allows screen readers to announce a
             * successful update without interrupting the user.
             *
             * Errors use role="alert" so they receive greater urgency.
             */}
            {operationMessage && (
              <div
                className={
                  messageType === "error"
                    ? "crew-operation-message error"
                    : "crew-operation-message success"
                }
                role={
                  messageType === "error"
                    ? "alert"
                    : "status"
                }
              >
                {operationMessage}
              </div>
            )}

            {/*
             * Legal next actions.
             */}
            <div className="crew-action-list">
              {availableActions.length > 0 ? (
                availableActions.map(
                  (action) => (
                    <article
                      className="crew-action-card"
                      key={action.actionId}
                    >
                      <div>
                        <h3>
                          {action.label}
                        </h3>

                        <p>
                          {action.description}
                        </p>

                        <span>
                          Next state:{" "}
                          {action.targetStatus}
                        </span>
                      </div>

                      <button
                        className="primary-button crew-action-button"
                        type="button"
                        onClick={() =>
                          handleStatusAction(
                            action.actionId
                          )
                        }
                      >
                        {action.label}
                      </button>
                    </article>
                  )
                )
              ) : (
                <div className="crew-no-actions">
                  <h3>
                    No crew operation available
                  </h3>

                  <p>
                    The current state does not have a lifecycle
                    transition controlled by the ambulance crew.
                  </p>
                </div>
              )}
            </div>
          </div>
        </article>

        <article className="panel">
          <header className="panel-header">
            <div>
              <p className="panel-eyebrow">
                Audit history
              </p>

              <h2>
                Session State Changes
              </h2>
            </div>

            <span className="panel-count">
              {transitionHistory.length}
            </span>
          </header>

          <div className="crew-panel-body">
            {/*
             * Temporary transition history.
             */}
            {transitionHistory.length === 0 ? (
              <div className="crew-no-actions">
                <h3>
                  No changes recorded
                </h3>

                <p>
                  Lifecycle transitions performed during this browser
                  session will appear here.
                </p>
              </div>
            ) : (
              <div className="crew-history-list">
                {transitionHistory.map(
                  (entry, index) => (
                    <article
                      className="crew-history-entry"
                      key={entry.historyId}
                    >
                      <div className="crew-history-number">
                        {index + 1}
                      </div>

                      <div className="crew-history-content">
                        <strong>
                          {entry.previousStatus}
                          {" → "}
                          {entry.newStatus}
                        </strong>

                        <span>
                          {entry.reason}
                        </span>

                        <time
                          dateTime={entry.changedAt}
                        >
                          {formatTimestamp(
                            entry.changedAt
                          )}
                        </time>
                      </div>
                    </article>
                  )
                )}
              </div>
            )}
          </div>
        </article>
      </section>
    </section>
  );
}

/**
 * Displays one quick lifecycle statistic.
 *
 * @param {object} props component properties
 * @param {string} props.label statistic label
 * @param {string|number} props.value statistic value
 * @param {string} props.description supporting description
 *
 * @returns {JSX.Element} one statistic card
 */
function StatusStatistic({
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
 * Displays a reusable missing-data message.
 *
 * @param {object} props component properties
 * @param {string} props.title message heading
 * @param {string} props.message detailed explanation
 *
 * @returns {JSX.Element} error or empty-state panel
 */
function StatusScreenMessage({
  title,
  message,
}) {
  return (
    <section className="panel workspace-placeholder">
      <header className="panel-header">
        <div>
          <p className="panel-eyebrow">
            Ambulance Crew
          </p>

          <h2>
            Update Ambulance Status
          </h2>
        </div>
      </header>

      <div className="empty-state">
        <h3>
          {title}
        </h3>

        <p>
          {message}
        </p>
      </div>
    </section>
  );
}

/**
 * Converts an ISO timestamp into a readable local date and time.
 *
 * Example input:
 *
 *     2026-08-06T19:45:00.000Z
 *
 * Example output depends on the user's browser locale.
 *
 * @param {string} timestamp ISO-8601 timestamp
 * @returns {string} localized date-and-time value
 */
function formatTimestamp(timestamp) {
  const parsedDate = new Date(timestamp);

  /*
   * Return the original value if JavaScript cannot parse the timestamp.
   */
  if (Number.isNaN(parsedDate.getTime())) {
    return timestamp;
  }

  return parsedDate.toLocaleString();
}

/*
 * Exports CrewStatusScreen so FieldCrewWorkspace.jsx can render it for
 * the /crew/status route.
 */
export default CrewStatusScreen;