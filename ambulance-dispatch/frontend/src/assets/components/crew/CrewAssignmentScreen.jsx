/*
 * Displays the Ambulance Crew "Current Assignment" screen.
 *
 * This screen is designed for:
 *
 * - desktop monitors;
 * - tablets;
 * - iPads;
 * - mobile phones.
 *
 * The component uses the seeded frontend data so the ambulance crew can
 * immediately see:
 *
 * 1. Which ambulance is assigned to them.
 * 2. Their active dispatch.
 * 3. The emergency call details.
 * 4. A visual map showing:
 *    - where the ambulance is now;
 *    - where the incident is located;
 *    - the route direction from the ambulance to the destination.
 *
 * IMPORTANT:
 *
 * This is a frontend demonstration screen using seeded data.
 *
 * Later, this component should request live data from the Spring Boot
 * backend, for example:
 *
 *     GET /api/crew/assignment
 *     GET /api/ambulances/{id}
 *     GET /api/dispatches/active/{ambulanceId}
 *
 * For now, the component reads from the seeded frontend data file so we
 * can build and test the responsive crew experience before the REST API
 * is finished.
 */

import {
  getActiveDispatchForAmbulance,
  getAmbulanceById,
  getCallById,
} from "../../data/seedData";

/**
 * Renders the current-assignment screen for one ambulance crew member.
 *
 * @param {object} props component properties
 * @param {number|null|undefined} props.ambulanceId the ambulance assigned
 *        to the currently logged-in crew member
 *
 * @returns {JSX.Element} the complete current-assignment screen
 */
function CrewAssignmentScreen({ ambulanceId }) {
  /*
   * If the logged-in crew user has no assigned ambulance, the crew
   * cannot display an assignment screen.
   */
  if (!ambulanceId) {
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
            No ambulance is assigned to this crew account
          </h3>

          <p>
            Assign an ambulance to the logged-in crew member before
            attempting to display a current call, route, or destination.
          </p>
        </div>
      </section>
    );
  }

  /*
   * Retrieve the crew's ambulance record from the seeded fleet data.
   *
   * Time complexity:
   *
   *     O(a)
   *
   * where a is the number of seeded ambulances.
   */
  const ambulance = getAmbulanceById(ambulanceId);

  /*
   * Retrieve the active dispatch currently associated with the crew's
   * ambulance.
   *
   * Time complexity:
   *
   *     O(d)
   *
   * where d is the number of active seeded dispatches.
   */
  const activeDispatch =
    getActiveDispatchForAmbulance(ambulanceId);

  /*
   * If no ambulance record exists, show a clear message.
   */
  if (!ambulance) {
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
            Ambulance record not found
          </h3>

          <p>
            The logged-in crew account references ambulance{" "}
            {ambulanceId}, but that ambulance does not exist in the
            seeded fleet data.
          </p>
        </div>
      </section>
    );
  }

  /*
   * If the ambulance exists but has no active dispatch, then the crew is
   * currently not responding to a call.
   */
  if (!activeDispatch) {
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
            No active dispatch
          </h3>

          <p>
            {ambulance.name} is currently in status{" "}
            <strong>{ambulance.status}</strong> and does not have an
            active assignment at this time.
          </p>
        </div>
      </section>
    );
  }

  /*
   * Retrieve the emergency call associated with the active dispatch.
   *
   * Time complexity:
   *
   *     O(c)
   *
   * where c is the number of seeded calls.
   */
  const emergencyCall = getCallById(
    activeDispatch.callId
  );

  /*
   * If the dispatch points to a missing call, show an informative error
   * instead of rendering incomplete data.
   */
  if (!emergencyCall) {
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
            Assigned call record not found
          </h3>

          <p>
            Dispatch {activeDispatch.dispatchId} references call{" "}
            {activeDispatch.callId}, but that call does not exist in the
            seeded data source.
          </p>
        </div>
      </section>
    );
  }

  /*
   * The map requires a start point (ambulance location) and an end point
   * (emergency-call location).
   */
  const ambulanceLocation = ambulance.location;
  const destinationLocation = emergencyCall.location;

  /*
   * The frontend "map" is a schematic coordinate grid rather than a
   * real GIS or GPS map.
   *
   * The grid is 20 by 20 units so we can convert the x/y coordinates
   * into percentages for responsive placement.
   *
   * Example:
   *
   * If x = 10 and the grid width is 20,
   * the marker is placed at 50% horizontally.
   */
  const gridSize = 20;

  /*
   * Converts one logical coordinate into a CSS percentage for horizontal
   * or vertical placement inside the map.
   *
   * The result is clamped between 0% and 100% so out-of-range values do
   * not place markers outside the visible map container.
   *
   * @param {number} value coordinate value
   * @returns {number} percentage position between 0 and 100
   */
  function toPercent(value) {
    const percent = (value / gridSize) * 100;

    if (percent < 0) {
      return 0;
    }

    if (percent > 100) {
      return 100;
    }

    return percent;
  }

  /*
   * Convert logical coordinates into map percentages.
   *
   * The x value maps to the horizontal axis.
   * The y value maps to the vertical axis.
   */
  const ambulanceLeft = toPercent(
    ambulanceLocation.x
  );
  const ambulanceTop = toPercent(
    ambulanceLocation.y
  );

  const destinationLeft = toPercent(
    destinationLocation.x
  );
  const destinationTop = toPercent(
    destinationLocation.y
  );

  /*
   * A simple SVG line is used to visually connect the ambulance marker
   * to the destination marker.
   *
   * This gives the crew a quick directional cue:
   *
   * "You are here -> go there"
   */
  const lineX1 = ambulanceLeft;
  const lineY1 = ambulanceTop;
  const lineX2 = destinationLeft;
  const lineY2 = destinationTop;

  return (
    <section className="crew-assignment-screen">
      {/*
       * Top summary statistics for quick glance use on tablets and
       * mobile devices.
       */}
      <section
        className="statistics-grid"
        aria-label="Crew assignment quick summary"
      >
        <article className="statistic-card">
          <span className="statistic-label">
            Ambulance
          </span>

          <strong className="statistic-value">
            {ambulance.name}
          </strong>

          <span className="statistic-description">
            Unit ID {ambulance.ambulanceId}
          </span>
        </article>

        <article className="statistic-card">
          <span className="statistic-label">
            Status
          </span>

          <strong className="statistic-value">
            {ambulance.status}
          </strong>

          <span className="statistic-description">
            Current response lifecycle state
          </span>
        </article>

        <article className="statistic-card">
          <span className="statistic-label">
            Call Priority
          </span>

          <strong className="statistic-value">
            {emergencyCall.priority}
          </strong>

          <span className="statistic-description">
            Emergency call {emergencyCall.callId}
          </span>
        </article>

        <article className="statistic-card">
          <span className="statistic-label">
            Distance
          </span>

          <strong className="statistic-value">
            {activeDispatch.distanceToCall.toFixed(2)}
          </strong>

          <span className="statistic-description">
            Estimated distance to destination
          </span>
        </article>
      </section>

      {/*
       * Main responsive content area.
       *
       * On wide screens, this will appear in two columns:
       * - assignment details;
       * - map.
       *
       * On tablets and mobile devices, the CSS will stack these panels.
       */}
      <section className="crew-grid">
        <article className="panel">
          <header className="panel-header">
            <div>
              <p className="panel-eyebrow">
                Crew Assignment
              </p>

              <h2>
                Current Emergency Call
              </h2>
            </div>

            <span className="panel-count">
              {emergencyCall.callId}
            </span>
          </header>

          <div className="crew-panel-body">
            <div className="crew-detail-list">
              <CrewDetailItem
                label="Caller"
                value={emergencyCall.callerName}
              />

              <CrewDetailItem
                label="Description"
                value={emergencyCall.description}
              />

              <CrewDetailItem
                label="Priority"
                value={emergencyCall.priority}
              />

              <CrewDetailItem
                label="Dispatch Status"
                value={activeDispatch.currentStatus}
              />

              <CrewDetailItem
                label="Received At"
                value={emergencyCall.receivedAt}
              />

              <CrewDetailItem
                label="Dispatched At"
                value={activeDispatch.dispatchedAt}
              />

              <CrewDetailItem
                label="Destination"
                value={activeDispatch.destination}
              />

              <CrewDetailItem
                label="Incident Coordinates"
                value={`(${destinationLocation.x}, ${destinationLocation.y})`}
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
             * A simple, responsive schematic map.
             *
             * The map provides a visual directional summary for tablet
             * and mobile users without needing an external map library.
             *
             * Later, this can be replaced by:
             *
             * - Leaflet
             * - Mapbox
             * - Google Maps
             *
             * once the project is ready for a real location service.
             */}
            <div
              className="crew-map"
              aria-label="Crew location map"
            >
              <svg
                className="crew-map-line-layer"
                viewBox="0 0 100 100"
                preserveAspectRatio="none"
                aria-hidden="true"
              >
                <line
                  x1={lineX1}
                  y1={lineY1}
                  x2={lineX2}
                  y2={lineY2}
                  className="crew-route-line"
                />
              </svg>

              {/*
               * Ambulance marker: where the crew is right now.
               */}
              <div
                className="crew-map-marker crew-map-marker-ambulance"
                style={{
                  left: `${ambulanceLeft}%`,
                  top: `${ambulanceTop}%`,
                }}
              >
                <span className="crew-map-pin" />
                <div className="crew-map-label">
                  <strong>
                    You are here
                  </strong>
                  <span>
                    {ambulance.name} ({ambulanceLocation.x},{" "}
                    {ambulanceLocation.y})
                  </span>
                </div>
              </div>

              {/*
               * Destination marker: where the ambulance should go.
               */}
              <div
                className="crew-map-marker crew-map-marker-destination"
                style={{
                  left: `${destinationLeft}%`,
                  top: `${destinationTop}%`,
                }}
              >
                <span className="crew-map-pin" />
                <div className="crew-map-label">
                  <strong>
                    Go here
                  </strong>
                  <span>
                    Call {emergencyCall.callId} (
                    {destinationLocation.x},{" "}
                    {destinationLocation.y})
                  </span>
                </div>
              </div>
            </div>

            {/*
             * A compact legend and direction summary improve usability
             * on smaller screens.
             */}
            <div className="crew-map-legend">
              <div className="crew-map-legend-item">
                <span className="crew-map-legend-dot crew-map-legend-dot-ambulance" />
                <span>
                  Your ambulance location
                </span>
              </div>

              <div className="crew-map-legend-item">
                <span className="crew-map-legend-dot crew-map-legend-dot-destination" />
                <span>
                  Emergency-call destination
                </span>
              </div>
            </div>
          </div>
        </article>
      </section>
    </section>
  );
}

/**
 * Displays one labeled crew detail field.
 *
 * This small helper keeps the assignment-details panel easier to read
 * and prevents repeated JSX markup.
 *
 * @param {object} props component properties
 * @param {string} props.label field label
 * @param {string} props.value field value
 *
 * @returns {JSX.Element} one labeled data row
 */
function CrewDetailItem({ label, value }) {
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

/*
 * Exports the component so App.jsx can render it for the Ambulance Crew
 * role's "Current Assignment" screen.
 */
export default CrewAssignmentScreen;