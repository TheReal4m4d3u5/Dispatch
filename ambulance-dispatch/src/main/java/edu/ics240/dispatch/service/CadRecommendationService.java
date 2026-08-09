package edu.ics240.dispatch.service;

import edu.ics240.dispatch.core.*;
import edu.ics240.dispatch.service.TravelEstimateProvider.TravelEstimate;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;

/**
 * STEP 3: Determine best choice among eligible ambulances.
 */
@Service
public class CadRecommendationService {

	private final TravelEstimateProvider travelEstimateProvider;
	private final DispatchRecommendationRepository recommendationRepo;

	public CadRecommendationService(TravelEstimateProvider travelEstimateProvider,
			DispatchRecommendationRepository recommendationRepo) {
		this.travelEstimateProvider = travelEstimateProvider;
		this.recommendationRepo = recommendationRepo;
	}

	/**
	 * Recommend an ambulance for the given call and eligible set.
	 */
	public DispatchRecommendation recommend(AmbulanceCall call, List<Ambulance> eligibleAmbulances, long leaseSeconds) {

		Location callLocation = call.getLocation();

// Stage 1: shortlist by straight-line distance
		List<Ambulance> shortlisted = eligibleAmbulances.stream()
				.sorted(Comparator.comparingDouble(a -> a.getLocation().distanceTo(callLocation))).limit(5) // bounded
																											// shortlist;
																											// adjust k
																											// as needed
				.toList();

// Ranking: soonest ETA first, ties broken by longest idle
		Comparator<RankedCandidate> byEta = Comparator.comparing((RankedCandidate c) -> c.estimate().etaMinutes());

        Comparator<RankedCandidate> byLongestIdle = (a, b) -> {
            Instant x = a.ambulance().getAvailableSince();
            Instant y = b.ambulance().getAvailableSince();
            if (x == null && y == null) return 0;
            if (x == null) return 1;    // nulls sort last
            if (y == null) return -1;
            return x.compareTo(y);      // earlier availableSince = idle longer = ranked first
        };
// Stage 2: compute travel estimates and rank
		List<RankedCandidate> candidates = shortlisted.stream().map(a -> {
			TravelEstimate estimate = travelEstimateProvider.estimate(a.getLocation(), callLocation);
			return new RankedCandidate(a, estimate);
		}).sorted(byEta.thenComparing(byLongestIdle)).toList();

		Ambulance recommended = candidates.getFirst().ambulance();

		List<Ambulance> alternatives = candidates.stream().skip(1).map(RankedCandidate::ambulance).toList();

		DispatchRecommendation rec = new DispatchRecommendation(recommendationRepo.nextId(), call, recommended,
				alternatives, Instant.now().plusSeconds(leaseSeconds));

		recommendationRepo.save(rec);
		return rec;
	}

	/**
	 * Simple candidate wrapper.
	 */
	public record RankedCandidate(Ambulance ambulance, TravelEstimate estimate) {
	}
}
