/*
 * Displays the current emergency assignment for an ambulance crew.
 *
 * The screen retrieves temporary seeded frontend data for:
 *
 * 1. The ambulance assigned to the logged-in crew member.
 * 2. The ambulance's active dispatch.
 * 3. The emergency call connected to that dispatch.
 *
 * The final version will retrieve this information from the Spring Boot
 * REST API instead of seedData.js.
 */
import {
  getActiveDispatchForAmbulance,
  getAmbulanceById,
  getCallById,
} from "../../data/seedData";

/**
 * Renders the ambulance crew's current assignment and route map.
 *
 * @param {object} props component properties
 * @param {number|null} props.ambulanceId ambulance assigned to the crew
 *
 * @returns {JSX.Element} current assignment screen
 */
function CrewAssignmentScreen({ ambulanceId }) {
  /*
   * A crew account must be connected to an ambulance before assignment
   * information can be displayed.
   */
  if (!ambulanceId) {
    return (
      <AssignmentMessage
        title="No ambulance assigned"
        message="This crew account is not currently connected to an ambulance."
      />
    );
  }

  /*
   * Find the ambulance assigned to the logged-in crew member.
   *
   * Current frontend complexity:
   *
   *     O(a)
   *
   * where a is the number of seeded ambulances.
   *
   * The Java backend will eventually use a HashMap, providing average
   * O(1) lookup by ambulance ID.
   */
  const ambulance =
    getAmbulanceById(ambulanceId);

  if (!ambulance) {
    return (
      <AssignmentMessage
        title="Ambulance not found"
        message={`Ambulance ${ambulanceId} does not exist in the seeded fleet data.`}
      />
    );
  }

  /*
   * Find the active dispatch assigned to this ambulance.
   */
  const dispatch =
    getActiveDispatchForAmbulance(
      ambulanceId
    );

  if (!dispatch) {
    return (
      <AssignmentMessage
        title="No active dispatch"
        message={`${ambulance.name} currently has no emergency assignment.`}
      />
    );
  }

  /*
   * Find the emergency call connected to the active dispatch.
   */
  const emergencyCall =
    getCallById(dispatch.callId);

  if (!emergencyCall) {
    return (
      <AssignmentMessage
        title="Emergency call not found"
        message={`Dispatch ${dispatch.dispatchId} references missing call ${dispatch.callId}.`}
      />
    );
  }

  /*
   * The temporary map uses a 20-by-20 logical coordinate grid.
   *
   * The backend Location class also stores x and y coordinates, so this
   * demonstration map matches the current domain model.
   */
  const mapSize = 20;

  /**
   * Converts a logical x or y coordinate into a CSS percentage.
   *
   * Example:
   *
   *     coordinate 10 on a 20-unit map becomes 50%.
   *
   * The result is clamped between 0 and 100 so markers cannot leave the
   * visible map.
   *
   * @param {number} coordinate logical map coordinate
   * @returns {number} CSS percentage
   */
  function coordinateToPercent(
    coordinate
  ) {
    const percentage =
      (coordinate / mapSize) * 100;

    return Math.min(
      100,
      Math.max(0, percentage)
    );
  }

  /*
   * Convert the ambulance's current location into responsive map
   * positions.
   */
  const ambulanceLeft =
    coordinateToPercent(
      ambulance.location.x
    );

  const ambulanceTop =
    coordinateToPercent(
      ambulance.location.y
    );

  /*
   * Convert the emergency destination into responsive map positions.
   */
  const destinationLeft =
    coordinateToPercent(
      emergencyCall.location.x
    );

  const destinationTop =
    coordinateToPercent(
      emergencyCall.location.y
    );

  return (
    <section className="crew-assignment-screen">
      {/*
       * Quick-read information cards.
       *
       * These become:
       *
       * - four columns on wide screens;
       * - two columns on tablets;
       * - one column on mobile phones.
       */}
      <section
        className="statistics-grid"
        aria-label="Current assignment summary"
      >
        <CrewStatistic
          label="Ambulance"
          value={ambulance.name}
          description={`Unit ${ambulance.ambulanceId}`}
        />

        <CrewStatistic
          label="Current Status"
          value={ambulance.status}
          description="Ambulance lifecycle state"
        />

        <CrewStatistic
          label="Call Priority"
          value={emergencyCall.priority}
          description={`Emergency call ${emergencyCall.callId}`}
        />

        <CrewStatistic
          label="Distance"
          value={
            dispatch.distanceToCall.toFixed(
              2
            )
          }
          description="Estimated distance to incident"
        />
      </section>

      {/*
       * Main assignment area.
       *
       * Desktop:
       *
       *     call details | map
       *
       * Tablet and mobile:
       *
       *     call details
       *     map
       */}
      <section className="crew-grid">
        <article className="panel">
          <header className="panel-header">
            <div>
              <p className="panel-eyebrow">
                Active response
              </p>

              <h2>
                Emergency Call Details
              </h2>
            </div>

            <span className="panel-count">
              {emergencyCall.callId}
            </span>
          </header>

          <div className="crew-panel-body">
            <div className="crew-detail-list">
              <CrewDetail
                label="Caller"
                value={
                  emergencyCall.callerName
                }
              />

              <CrewDetail
                label="Emergency"
                value={
                  emergencyCall.description
                }
              />

              <CrewDetail
                label="Priority"
                value={
                  emergencyCall.priority
                }
              />

              <CrewDetail
                label="Dispatch Status"
                value={
                  dispatch.currentStatus
                }
              />

              <CrewDetail
                label="Destination"
                value={
                  dispatch.destination
                }
              />

              <CrewDetail
                label="Current Location"
                value={`(${ambulance.location.x}, ${ambulance.location.y})`}
              />

              <CrewDetail
                label="Incident Location"
                value={`(${emergencyCall.location.x}, ${emergencyCall.location.y})`}
              />

              <CrewDetail
                label="Received At"
                value={
                  emergencyCall.receivedAt
                }
              />

              <CrewDetail
                label="Dispatched At"
                value={
                  dispatch.dispatchedAt
                }
              />
            </div>
          </div>
        </article>

        <article className="panel">
          <header className="panel-header">
            <div>
              <p className="panel-eyebrow">
                Navigation
              </p>

              <h2>
                Where You Are and Where to Go
              </h2>
            </div>
          </header>

          <div className="crew-panel-body">
            {/*
             * This is currently a coordinate-based demonstration map.
             *
             * A future version can replace it with Google Maps, Mapbox,
             * or Leaflet without changing the rest of the crew screen.
             */}
            <div
              className="crew-map"
              aria-label="Map showing the ambulance and emergency destination"
            >
              {/*
               * The SVG line visually connects the ambulance's current
               * position to the incident location.
               */}
              <svg
                className="crew-map-line-layer"
                viewBox="0 0 100 100"
                preserveAspectRatio="none"
                aria-hidden="true"
              >
                <line
                  className="crew-route-line"
                  x1={ambulanceLeft}
                  y1={ambulanceTop}
                  x2={destinationLeft}
                  y2={destinationTop}
                />
              </svg>

              {/*
               * Blue marker showing the ambulance's current position.
               */}
              <MapMarker
                markerClass="crew-map-marker-ambulance"
                left={ambulanceLeft}
                top={ambulanceTop}
                heading="You are here"
                details={`${ambulance.name} (${ambulance.location.x}, ${ambulance.location.y})`}
              />

              {/*
               * Red marker showing the emergency-call destination.
               */}
              <MapMarker
                markerClass="crew-map-marker-destination"
                left={destinationLeft}
                top={destinationTop}
                heading="Go here"
                details={`Call ${emergencyCall.callId} (${emergencyCall.location.x}, ${emergencyCall.location.y})`}
              />
            </div>

            <div className="crew-map-legend">
              <MapLegendItem
                dotClass="crew-map-legend-dot-ambulance"
                label="Current ambulance location"
              />

              <MapLegendItem
                dotClass="crew-map-legend-dot-destination"
                label="Emergency destination"
              />
            </div>
          </div>
        </article>
      </section>
    </section>
  );
}

/**
 * Displays one quick-read statistic.
 */
function CrewStatistic({
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
 * Displays one labeled assignment field.
 */
function CrewDetail({
  label,
  value,
}) {
  return (
    <div className="crew-detail-item">
      <span className="crew-detail-label">
        {label}
      </span>

      <span className="crew-detail-value">
        {value}
      </span>
    </div>
  );
}

/**
 * Displays one responsive map marker.
 */
function MapMarker({
  markerClass,
  left,
  top,
  heading,
  details,
}) {
  return (
    <div
      className={
        `crew-map-marker ${markerClass}`
      }
      style={{
        left: `${left}%`,
        top: `${top}%`,
      }}
    >
      <span className="crew-map-pin" />

      <div className="crew-map-label">
        <strong>
          {heading}
        </strong>

        <span>
          {details}
        </span>
      </div>
    </div>
  );
}

/**
 * Displays one entry beneath the map.
 */
function MapLegendItem({
  dotClass,
  label,
}) {
  return (
    <div className="crew-map-legend-item">
      <span
        className={
          `crew-map-legend-dot ${dotClass}`
        }
      />

      <span>
        {label}
      </span>
    </div>
  );
}

/**
 * Displays a reusable missing-assignment message.
 */
function AssignmentMessage({
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
            Current Assignment
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

/*
 * Exports the component for FieldCrewWorkspace.jsx.
 */
export default CrewAssignmentScreen;