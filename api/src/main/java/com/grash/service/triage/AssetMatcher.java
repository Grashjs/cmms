package com.grash.service.triage;

import com.grash.model.Request;

import java.util.List;

/**
 * Proposes which asset a request is about.
 *
 * <p>This is the seam the feature is built around. Stage 1 answers the question with word
 * overlap ({@link LexicalAssetMatcher}); a later stage may answer it with embeddings, or with a
 * language model, or with both and a vote. Everything downstream - the entity, the endpoints, the
 * card in the request view, the record of what a human did with the suggestion - is written
 * against this interface and does not change when the answer gets better.
 *
 * <p>Two obligations for any implementation:
 *
 * <ul>
 *   <li><b>No side effects.</b> A matcher reads and returns. It must not touch the request, and
 *       must not care whether its answer is used. Storing and applying belong to
 *       {@code RequestQualificationService}, which is what keeps a wrong suggestion cheap.</li>
 *   <li><b>Explain yourself.</b> Every match carries the terms that produced it. An implementation
 *       that cannot say why is not usable here, however good its ranking - the whole workflow
 *       depends on a human being able to accept or dismiss a suggestion in a couple of
 *       seconds.</li>
 * </ul>
 */
public interface AssetMatcher {

    /**
     * Identifies the implementation and its version, e.g. {@code lexical-v1}. Stored with every
     * result: when the matcher is replaced, this is the only thing that lets the old and the new
     * one be told apart in hindsight, and comparing them is the only honest way to know whether
     * the replacement was an improvement.
     */
    String engineName();

    /**
     * Ranked candidates for a request, best first, at most {@code limit} of them. An empty list is
     * a normal and frequent answer - it means the request text has nothing in common with any
     * asset, and saying nothing is the correct behaviour.
     */
    List<AssetMatch> match(Request request, int limit);
}
