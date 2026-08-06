/*
 * Seeded operational data for the Ambulance Call Center frontend.
 *
 * This file provides realistic demonstration records before the React
 * application is connected to the Spring Boot REST API.
 *
 * The seeded data allows each role workspace to display information:
 *
 * - Call takers can see waiting emergency calls.
 * - Dispatchers can see the priority queue, fleet, and active responses.
 * - Ambulance crews can see their current assignment.
 * - Fleet supervisors can see ambulance readiness and maintenance states.
 * - Administrators can see system-wide information.
 *
 * IMPORTANT:
 *
 * This is temporary frontend data.
 *
 * The final application will retrieve these records from Spring Boot
 * endpoints such as:
 *
 *     GET /api/state
 *     GET /api/statistics
 *     GET /api/history
 *     GET /api/ambulances/{id}
 *
 * Object.freeze is used throughout this file to prevent the seeded
 * demonstration records from being modified accidentally.
 */

/**
 * Creates and freezes one location value.
 *
 * The frontend location structure matches the Java Location class:
 *
 *     Location {
 *         double x;
 *         double y;
 *     }
 *
 * @param {number} x horizontal coordinate
 * @param {number} y vertical coordinate
 *
 * @returns {object} immutable location
 */
function createLocation(x, y) {
  /*
   * Reject NaN, Infinity, -Infinity, and non-number values.
   */
  if (!Number.isFinite(x) || !Number.isFinite(y)) {
    throw new Error(
      "Seeded location coordinates must be finite numbers."
    );
  }

  return Object.freeze({
    x,
    y,
  });
}

/**
 * Creates and freezes one emergency-call record.
 *
 * @param {object} configuration call configuration
 * @param {number} configuration.callId unique positive call ID
 * @param {string} configuration.callerName caller's name
 * @param {string} configuration.description emergency description
 * @param {string} configuration.priority emergency priority
 * @param {object} configuration.location incident location
 * @param {string} configuration.receivedAt ISO-8601 timestamp
 * @param {number} configuration.arrivalSequence FCFS sequence number
 * @param {string} configuration.status current call status
 *
 * @returns {object} immutable emergency-call record
 */
function createCall({
  callId,
  callerName,
  description,
  priority,
  location,
  receivedAt,
  arrivalSequence,
  status,
}) {
  if (!Number.isInteger(callId) || callId <= 0) {
    throw new Error(
      "Seeded call ID must be a positive integer."
    );
  }

  if (
    !Number.isInteger(arrivalSequence)
    || arrivalSequence < 0
  ) {
    throw new Error(
      "Seeded arrival sequence must be a nonnegative integer."
    );
  }

  return Object.freeze({
    callId,
    callerName: requireText(
      callerName,
      "Caller name"
    ),
    description: requireText(
      description,
      "Emergency description"
    ),
    priority: requireText(
      priority,
      "Call priority"
    ),
    location,
    receivedAt: requireText(
      receivedAt,
      "Call received time"
    ),
    arrivalSequence,
    status: requireText(
      status,
      "Call status"
    ),
  });
}

/**
 * Creates and freezes one ambulance record.
 *
 * @param {object} configuration ambulance configuration
 * @param {number} configuration.ambulanceId unique ambulance ID
 * @param {string} configuration.name unit display name
 * @param {object} configuration.location current location
 * @param {string} configuration.status lifecycle status
 * @param {number|null} configuration.activeCallId assigned call ID
 * @param {string} configuration.availableSince ISO timestamp
 * @param {string[]} configuration.capabilities unit capabilities
 *
 * @returns {object} immutable ambulance record
 */
function createAmbulance({
  ambulanceId,
  name,
  location,
  status,
  activeCallId,
  availableSince,
  capabilities,
}) {
  if (
    !Number.isInteger(ambulanceId)
    || ambulanceId <= 0
  ) {
    throw new Error(
      "Seeded ambulance ID must be a positive integer."
    );
  }

  if (
    activeCallId !== null
    && (
      !Number.isInteger(activeCallId)
      || activeCallId <= 0
    )
  ) {
    throw new Error(
      "Active call ID must be a positive integer or null."
    );
  }

  if (!Array.isArray(capabilities)) {
    throw new Error(
      "Ambulance capabilities must be an array."
    );
  }

  return Object.freeze({
    ambulanceId,
    name: requireText(
      name,
      "Ambulance name"
    ),
    location,
    status: requireText(
      status,
      "Ambulance status"
    ),
    activeCallId,
    availableSince: requireText(
      availableSince,
      "Available-since time"
    ),

    /*
     * Copy and freeze the capabilities array so outside code cannot
     * modify the original array.
     */
    capabilities: Object.freeze([
      ...capabilities,
    ]),
  });
}

/**
 * Creates and freezes one active dispatch record.
 *
 * A dispatch connects:
 *
 * - one emergency call;
 * - one selected ambulance;
 * - the assignment time;
 * - the calculated distance;
 * - the current response status.
 *
 * @param {object} configuration dispatch configuration
 * @param {number} configuration.dispatchId unique dispatch ID
 * @param {number} configuration.callId assigned call ID
 * @param {number} configuration.ambulanceId assigned ambulance ID
 * @param {string} configuration.dispatchedAt assignment timestamp
 * @param {number} configuration.distanceToCall calculated distance
 * @param {string} configuration.currentStatus ambulance response state
 * @param {string} configuration.destination response destination
 *
 * @returns {object} immutable active dispatch
 */
function createActiveDispatch({
  dispatchId,
  callId,
  ambulanceId,
  dispatchedAt,
  distanceToCall,
  currentStatus,
  destination,
}) {
  if (
    !Number.isInteger(dispatchId)
    || dispatchId <= 0
  ) {
    throw new Error(
      "Dispatch ID must be a positive integer."
    );
  }

  if (!Number.isFinite(distanceToCall)) {
    throw new Error(
      "Dispatch distance must be a finite number."
    );
  }

  return Object.freeze({
    dispatchId,
    callId,
    ambulanceId,
    dispatchedAt: requireText(
      dispatchedAt,
      "Dispatch time"
    ),
    distanceToCall,
    currentStatus: requireText(
      currentStatus,
      "Dispatch status"
    ),
    destination: requireText(
      destination,
      "Dispatch destination"
    ),
  });
}

/**
 * Calls currently waiting in the min-heap priority queue.
 *
 * Expected queue order:
 *
 * 1. Lower priority rank first.
 * 2. Earlier arrivalSequence first when priorities are equal.
 *
 * Priority ranking:
 *
 *     CRITICAL      -> 1
 *     HIGH          -> 2
 *     MEDIUM        -> 3
 *     LOW           -> 4
 *     NON_EMERGENCY -> 5
 *
 * The backend PriorityQueue will eventually become the authoritative
 * source for this ordering.
 */
export const SEEDED_WAITING_CALLS =
  Object.freeze([
    createCall({
      callId: 1003,
      callerName: "Evelyn Carter",
      description:
        "Possible cardiac emergency with severe chest pain.",
      priority: "CRITICAL",
      location: createLocation(7, 4),
      receivedAt:
        "2026-08-06T18:58:00Z",
      arrivalSequence: 3,
      status: "WAITING",
    }),

    createCall({
      callId: 1004,
      callerName: "Marcus Lee",
      description:
        "Motor vehicle collision with a possible leg injury.",
      priority: "HIGH",
      location: createLocation(12, 8),
      receivedAt:
        "2026-08-06T18:59:30Z",
      arrivalSequence: 4,
      status: "WAITING",
    }),

    createCall({
      callId: 1005,
      callerName: "Linda Nguyen",
      description:
        "Older adult fell at home and cannot stand.",
      priority: "MEDIUM",
      location: createLocation(5, 13),
      receivedAt:
        "2026-08-06T19:01:00Z",
      arrivalSequence: 5,
      status: "WAITING",
    }),
  ]);

/**
 * Emergency calls that have already been assigned to ambulances.
 *
 * Ambulance crew member Jordan Rivera is assigned to ambulance 101.
 * Ambulance 101 is assigned to call 1001.
 *
 * This allows Jordan's Current Assignment screen to display a complete
 * response record after App.jsx is connected to this seed data.
 */
export const SEEDED_ASSIGNED_CALLS =
  Object.freeze([
    createCall({
      callId: 1001,
      callerName: "Robert Wilson",
      description:
        "Patient experiencing difficulty breathing at an apartment building.",
      priority: "CRITICAL",
      location: createLocation(9, 6),
      receivedAt:
        "2026-08-06T18:47:00Z",
      arrivalSequence: 1,
      status: "DISPATCHED",
    }),

    createCall({
      callId: 1002,
      callerName: "Sophia Martinez",
      description:
        "Bicycle accident with possible head injury.",
      priority: "HIGH",
      location: createLocation(3, 11),
      receivedAt:
        "2026-08-06T18:52:00Z",
      arrivalSequence: 2,
      status: "ON_SCENE",
    }),
  ]);

/**
 * Complete seeded call collection.
 *
 * This combined array supports direct call lookup regardless of whether
 * the call is waiting or already assigned.
 *
 * Time complexity to create:
 *
 *     O(w + a)
 *
 * where:
 *
 *     w = number of waiting calls
 *     a = number of assigned calls
 */
export const SEEDED_CALLS =
  Object.freeze([
    ...SEEDED_WAITING_CALLS,
    ...SEEDED_ASSIGNED_CALLS,
  ]);

/**
 * Seeded ambulance fleet.
 *
 * The fleet demonstrates several lifecycle states:
 *
 * - DISPATCHED
 * - ON_SCENE
 * - AVAILABLE
 * - MAINTENANCE
 * - UNSTAFFED
 * - OUT_OF_SERVICE
 */
export const SEEDED_AMBULANCES =
  Object.freeze([
    createAmbulance({
      ambulanceId: 101,
      name: "Medic 101",
      location: createLocation(6, 5),
      status: "DISPATCHED",
      activeCallId: 1001,
      availableSince:
        "2026-08-06T17:20:00Z",
      capabilities: [
        "Advanced Life Support",
        "Cardiac Monitor",
        "Oxygen",
      ],
    }),

    createAmbulance({
      ambulanceId: 102,
      name: "Medic 102",
      location: createLocation(3, 11),
      status: "ON_SCENE",
      activeCallId: 1002,
      availableSince:
        "2026-08-06T16:45:00Z",
      capabilities: [
        "Advanced Life Support",
        "Trauma Equipment",
      ],
    }),

    createAmbulance({
      ambulanceId: 103,
      name: "Medic 103",
      location: createLocation(8, 3),
      status: "AVAILABLE",
      activeCallId: null,
      availableSince:
        "2026-08-06T18:15:00Z",
      capabilities: [
        "Basic Life Support",
        "Oxygen",
      ],
    }),

    createAmbulance({
      ambulanceId: 104,
      name: "Medic 104",
      location: createLocation(14, 9),
      status: "AVAILABLE",
      activeCallId: null,
      availableSince:
        "2026-08-06T17:40:00Z",
      capabilities: [
        "Advanced Life Support",
        "Pediatric Equipment",
      ],
    }),

    createAmbulance({
      ambulanceId: 105,
      name: "Medic 105",
      location: createLocation(2, 2),
      status: "MAINTENANCE",
      activeCallId: null,
      availableSince:
        "2026-08-06T10:00:00Z",
      capabilities: [
        "Basic Life Support",
      ],
    }),

    createAmbulance({
      ambulanceId: 106,
      name: "Medic 106",
      location: createLocation(16, 4),
      status: "UNSTAFFED",
      activeCallId: null,
      availableSince:
        "2026-08-06T09:00:00Z",
      capabilities: [
        "Advanced Life Support",
      ],
    }),

    createAmbulance({
      ambulanceId: 107,
      name: "Medic 107",
      location: createLocation(11, 15),
      status: "OUT_OF_SERVICE",
      activeCallId: null,
      availableSince:
        "2026-08-05T22:00:00Z",
      capabilities: [
        "Basic Life Support",
      ],
    }),
  ]);

/**
 * Active ambulance dispatch assignments.
 */
export const SEEDED_ACTIVE_DISPATCHES =
  Object.freeze([
    createActiveDispatch({
      dispatchId: 5001,
      callId: 1001,
      ambulanceId: 101,
      dispatchedAt:
        "2026-08-06T18:48:00Z",
      distanceToCall: 2.83,
      currentStatus: "DISPATCHED",
      destination:
        "Apartment complex at coordinate (9, 6)",
    }),

    createActiveDispatch({
      dispatchId: 5002,
      callId: 1002,
      ambulanceId: 102,
      dispatchedAt:
        "2026-08-06T18:53:00Z",
      distanceToCall: 4.12,
      currentStatus: "ON_SCENE",
      destination:
        "Bike trail entrance at coordinate (3, 11)",
    }),
  ]);

/**
 * Seeded completed dispatch history.
 *
 * These records demonstrate what the future paginated history endpoint
 * will return.
 */
export const SEEDED_DISPATCH_HISTORY =
  Object.freeze([
    Object.freeze({
      dispatchId: 4999,
      callId: 998,
      ambulanceId: 104,
      priority: "HIGH",
      dispatchedAt:
        "2026-08-06T16:10:00Z",
      completedAt:
        "2026-08-06T17:04:00Z",
      completionType:
        "TRANSPORTED_TO_HOSPITAL",
      finalDestination:
        "Central Medical Center",
    }),

    Object.freeze({
      dispatchId: 4998,
      callId: 997,
      ambulanceId: 103,
      priority: "MEDIUM",
      dispatchedAt:
        "2026-08-06T14:35:00Z",
      completedAt:
        "2026-08-06T15:12:00Z",
      completionType:
        "TREATED_ON_SCENE",
      finalDestination:
        "Patient residence",
    }),
  ]);

/**
 * Seeded system statistics.
 *
 * The backend will eventually maintain these values incrementally
 * through SystemStatisticsAccumulator.
 */
export const SEEDED_STATISTICS =
  Object.freeze({
    totalAcceptedCalls: 7,
    waitingCalls: SEEDED_WAITING_CALLS.length,
    activeDispatches:
      SEEDED_ACTIVE_DISPATCHES.length,
    completedDispatches:
      SEEDED_DISPATCH_HISTORY.length,

    availableAmbulances:
      SEEDED_AMBULANCES.filter(
        (ambulance) =>
          ambulance.status === "AVAILABLE"
      ).length,

    totalAmbulances:
      SEEDED_AMBULANCES.length,

    peakQueueDepth: 5,
    totalEscalations: 1,
  });

/**
 * Finds one ambulance by its unique ID.
 *
 * Time complexity:
 *
 *     O(a)
 *
 * Array.find may inspect all a ambulance records.
 *
 * The backend will use HashMap<Integer, Ambulance>, which provides
 * average O(1) lookup by ambulance ID.
 *
 * @param {number} ambulanceId requested ambulance ID
 * @returns {object|null} matching ambulance or null
 */
export function getAmbulanceById(
  ambulanceId
) {
  return (
    SEEDED_AMBULANCES.find(
      (ambulance) =>
        ambulance.ambulanceId
        === ambulanceId
    ) ?? null
  );
}

/**
 * Finds one emergency call by its unique ID.
 *
 * Time complexity:
 *
 *     O(c)
 *
 * where c is the number of seeded calls.
 *
 * @param {number} callId requested call ID
 * @returns {object|null} matching call or null
 */
export function getCallById(callId) {
  return (
    SEEDED_CALLS.find(
      (call) =>
        call.callId === callId
    ) ?? null
  );
}

/**
 * Finds the active dispatch assigned to one ambulance.
 *
 * This will be used by the Ambulance Crew workspace.
 *
 * Example:
 *
 *     getActiveDispatchForAmbulance(101)
 *
 * returns the dispatch assigned to Jordan Rivera's ambulance.
 *
 * Time complexity:
 *
 *     O(d)
 *
 * where d is the number of active dispatches.
 *
 * @param {number} ambulanceId assigned ambulance ID
 * @returns {object|null} matching active dispatch or null
 */
export function getActiveDispatchForAmbulance(
  ambulanceId
) {
  return (
    SEEDED_ACTIVE_DISPATCHES.find(
      (dispatch) =>
        dispatch.ambulanceId
        === ambulanceId
    ) ?? null
  );
}

/**
 * Validates and normalizes required seeded text.
 *
 * @param {string} value text being validated
 * @param {string} fieldName name used in the error message
 *
 * @returns {string} trimmed nonblank text
 *
 * @throws {Error} when the supplied value is not valid text
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