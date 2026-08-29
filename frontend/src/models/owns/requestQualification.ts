import { AssetMiniDTO } from './asset';
import { UserMiniDTO } from './user';

export type QualificationStatus =
  | 'PENDING'
  | 'APPLIED'
  | 'REJECTED'
  | 'SUPERSEDED';

export interface QualificationCandidate {
  asset: AssetMiniDTO;
  /**
   * How strongly the asset matches, 0..1. Not a probability and not comparable across
   * engines - see the backend's AssetMatch. The card shows it as a percentage and calls it a
   * match, never a confidence.
   */
  score: number;
  ordinal: number;
  /** The words that produced the match. This is what makes a suggestion checkable at a glance. */
  matchedTerms: string[];
}

export default interface RequestQualification {
  id: number;
  requestId: number;
  status: QualificationStatus;
  /** Which matcher produced the suggestion, e.g. lexical-v1. Shown so the reader can judge it. */
  engine: string;
  candidates: QualificationCandidate[];
  chosenAsset?: AssetMiniDTO;
  decidedBy?: UserMiniDTO;
  decidedAt?: string;
  createdAt: string;
}

/**
 * The response shape of every triage endpoint. The qualification is null for most requests -
 * either the reporter already picked an asset, or nothing in the asset list resembled the text -
 * and that is the ordinary case, not an error.
 */
export interface RequestTriage {
  qualification: RequestQualification | null;
}
