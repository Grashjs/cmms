package com.grash.service;

import com.grash.exception.CustomException;
import com.grash.model.Asset;
import com.grash.model.Request;
import com.grash.model.RequestQualification;
import com.grash.model.RequestQualificationCandidate;
import com.grash.model.User;
import com.grash.model.enums.QualificationStatus;
import com.grash.repository.RequestQualificationRepository;
import com.grash.service.triage.AssetMatch;
import com.grash.service.triage.AssetMatcher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * Stores triage suggestions, and turns a human decision about one into a change on the request.
 *
 * <p>The split of responsibilities is the point of the design. {@link AssetMatcher} decides and
 * has no side effects; this service is the only thing that writes, and it writes to the request
 * only when a person asked it to. Between the two sits a stored suggestion that nobody has to
 * act on.
 *
 * <p>That is not the arrangement the original sketch assumed. It expected the existing workflow
 * engine to carry out what the matcher decided, because {@code WorkflowService.runRequest} can
 * already set an asset on a request. It cannot be used for this: the asset it sets is
 * {@code action.getAsset()}, a fixed one picked when the rule was written, and there is no way to
 * hand it a value computed at run time. An apply path had to be built either way, so it was built
 * here, where the decision can also be recorded.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RequestQualificationService {

    private final RequestQualificationRepository requestQualificationRepository;
    private final RequestService requestService;
    private final AssetService assetService;
    private final AssetMatcher assetMatcher;

    /** How many alternatives to offer. Three is about what someone will actually read. */
    @Value("${triage.asset-match.max-candidates:3}")
    private int maxCandidates;

    /**
     * Runs the matcher for a request and stores the result, if there is one worth storing.
     *
     * <p>Runs in its own transaction because its caller is an after-commit listener on a
     * transaction that has already finished. Without {@code REQUIRES_NEW} the lazy associations of
     * the request would have no session to load from.
     *
     * <p>Returns empty in the ordinary cases where triage has nothing to contribute - the request
     * already names an asset, or no asset resembles it. Nothing is written then: an empty
     * suggestion card is worse than no card, because it costs a glance and teaches the reader
     * that the feature has nothing to say.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Optional<RequestQualification> qualify(Long requestId) {
        Request request = requestService.findById(requestId).orElse(null);
        if (request == null) return Optional.empty();
        if (!shouldQualify(request)) return Optional.empty();

        List<AssetMatch> matches = assetMatcher.match(request, maxCandidates);
        if (matches.isEmpty()) {
            log.debug("Triage found no asset candidate for request {}", requestId);
            return Optional.empty();
        }

        RequestQualification qualification = new RequestQualification();
        qualification.setRequest(request);
        // CompanyAudit.beforePersist reads the company off the security context, and there is
        // none here: this runs on an async thread, and a portal request has no authenticated user
        // to begin with. The company has to come from the request itself.
        qualification.setCompany(request.getCompany());
        qualification.setEngine(assetMatcher.engineName());
        qualification.setStatus(QualificationStatus.PENDING);

        int ordinal = 0;
        for (AssetMatch match : matches) {
            Asset asset = assetService.findById(match.assetId()).orElse(null);
            if (asset == null) continue;
            qualification.addCandidate(new RequestQualificationCandidate(
                    asset, match.score(), ordinal++, joinTerms(match.matchedTerms())));
        }
        if (qualification.getCandidates().isEmpty()) return Optional.empty();

        // Retire the previous suggestion only now that there is definitely one to replace it with.
        // Doing it earlier would leave a request that had a usable suggestion with none at all
        // whenever a re-run happens to find nothing.
        requestQualificationRepository.supersedeAllFor(requestId, QualificationStatus.SUPERSEDED);

        RequestQualification saved = requestQualificationRepository.save(qualification);
        log.info("Triage suggested {} asset(s) for request {} using {}",
                saved.getCandidates().size(), requestId, saved.getEngine());
        return Optional.of(saved);
    }

    /**
     * Joins the matched words into the single column that holds them, and stops at the column
     * width. A very long request can match a lot of words; without the cut, the insert would fail
     * on the length and the listener would swallow the exception, so the whole suggestion would
     * disappear over its own footnote. Whole words only, so the explanation stays readable.
     */
    private String joinTerms(List<String> terms) {
        StringBuilder joined = new StringBuilder();
        for (String term : terms) {
            int addition = joined.length() == 0 ? term.length() : term.length() + 2;
            if (joined.length() + addition > MATCHED_TERMS_MAX_LENGTH) break;
            if (joined.length() > 0) joined.append(", ");
            joined.append(term);
        }
        return joined.toString();
    }

    /** Matches {@code request_qualification_candidate.matched_terms}. */
    private static final int MATCHED_TERMS_MAX_LENGTH = 500;

    /**
     * Whether a request is worth triaging at all.
     *
     * <p>A request that already names an asset is skipped, and that is a rule about people rather
     * than about matching: the reporter picked it from a list, which beats anything the matcher
     * can infer from prose, and a card second-guessing a correct answer is pure noise.
     */
    private boolean shouldQualify(Request request) {
        return request.getAsset() == null
                && request.getWorkOrder() == null
                && !request.isCancelled();
    }

    /** The suggestion currently on offer for a request, if any. */
    @Transactional(readOnly = true)
    public Optional<RequestQualification> findLive(Long requestId, User user) {
        return requestQualificationRepository.findLiveOne(requestId, user.getCompany().getId());
    }

    /**
     * Accepts one of the candidates: the request gets that asset, and the qualification records
     * which one was taken and by whom.
     *
     * <p>The chosen asset must be one of the stored candidates. That check is doing real work - it
     * is also the tenancy check. The candidates were produced by a company-scoped query, so an
     * asset that is in the list is by construction an asset of this company, and an id arriving
     * from the client can never reach the request by any other route.
     */
    @Transactional
    public RequestQualification apply(Long qualificationId, Long assetId, User user) {
        RequestQualification qualification = loadOpen(qualificationId, user);
        Request request = qualification.getRequest();

        RequestQualificationCandidate chosen = qualification.getCandidates().stream()
                .filter(candidate -> candidate.getAsset().getId().equals(assetId))
                .findFirst()
                .orElseThrow(() -> new CustomException("Asset is not one of the suggested candidates",
                        HttpStatus.NOT_ACCEPTABLE));

        request.setAsset(chosen.getAsset());
        requestService.save(request);

        qualification.setStatus(QualificationStatus.APPLIED);
        qualification.setChosenAsset(chosen.getAsset());
        qualification.setDecidedBy(user);
        qualification.setDecidedAt(new Date());
        return requestQualificationRepository.save(qualification);
    }

    /**
     * Dismisses the whole suggestion. Kept rather than deleted: "the matcher was wrong about this
     * one" is the most useful thing this feature produces, and it is the only record of it.
     */
    @Transactional
    public RequestQualification reject(Long qualificationId, User user) {
        RequestQualification qualification = loadOpen(qualificationId, user);
        qualification.setStatus(QualificationStatus.REJECTED);
        qualification.setDecidedBy(user);
        qualification.setDecidedAt(new Date());
        return requestQualificationRepository.save(qualification);
    }

    /**
     * Asks for a fresh opinion, for a request whose asset list or description has changed since
     * the last one. The previous row is superseded by {@link #qualify}, not overwritten.
     *
     * <p>The call to {@code qualify} below is a self-invocation and so does not go through the
     * proxy: its {@code REQUIRES_NEW} does not apply and the work joins this transaction instead.
     * That is what we want here - a user pressed a button and is waiting for the answer, so it
     * should be one unit of work. Only the listener path needs its own transaction, and it gets
     * one, because it enters through the bean.
     */
    @Transactional
    public Optional<RequestQualification> rerun(Long requestId, User user) {
        Request request = requestService.findById(requestId)
                .orElseThrow(() -> new CustomException("Request not found", HttpStatus.NOT_FOUND));
        assertCanDecide(request, user);
        return qualify(requestId);
    }

    private RequestQualification loadOpen(Long qualificationId, User user) {
        RequestQualification qualification = requestQualificationRepository
                .findByIdAndCompany_Id(qualificationId, user.getCompany().getId())
                .orElseThrow(() -> new CustomException("Qualification not found", HttpStatus.NOT_FOUND));
        if (qualification.isDecided()) {
            throw new CustomException("This suggestion has already been decided", HttpStatus.NOT_ACCEPTABLE);
        }
        assertCanDecide(qualification.getRequest(), user);
        return qualification;
    }

    /**
     * Deciding on a suggestion writes the asset field of the request, so it takes exactly the
     * authority that editing the request takes - no more, and no separate permission of its own.
     * A request that has already become a work order is closed to this, the same way
     * {@code RequestController.patch} closes it to edits.
     */
    private void assertCanDecide(Request request, User user) {
        if (request.getWorkOrder() != null) {
            throw new CustomException("Request is already approved", HttpStatus.NOT_ACCEPTABLE);
        }
        if (!request.canBeEditedBy(user)) {
            throw new CustomException("Access denied", HttpStatus.FORBIDDEN);
        }
    }
}
