/**
 * A token bucket per caller (konzept §5.4). An agent in a loop must not be able to overrun
 * the CMMS, and the CMMS's own limiter cannot tell one agent from a whole organisation.
 *
 * The bucket is keyed by the key fingerprint, so it survives across MCP sessions from the
 * same client and cannot be reset by reconnecting. It is in-process and therefore per
 * replica: two replicas allow twice the rate, which is the honest trade for holding no
 * state anywhere else.
 */

export interface RateLimiterOptions {
  /** Sustained calls per minute. 0 disables limiting entirely. */
  perMinute: number;
  /** Bucket size, i.e. how much of a burst is tolerated. */
  burst: number;
  now?: () => number;
}

export interface RateDecision {
  allowed: boolean;
  /** Seconds until one token is available again. Only meaningful when `allowed` is false. */
  retryAfterSeconds: number;
  remaining: number;
}

interface Bucket {
  tokens: number;
  updatedAt: number;
}

export class RateLimiter {
  private readonly buckets = new Map<string, Bucket>();
  private readonly perMinute: number;
  private readonly burst: number;
  private readonly now: () => number;

  constructor(options: RateLimiterOptions) {
    this.perMinute = Math.max(0, options.perMinute);
    this.burst = Math.max(1, options.burst);
    this.now = options.now ?? Date.now;
  }

  take(key: string): RateDecision {
    if (this.perMinute === 0) {
      return { allowed: true, retryAfterSeconds: 0, remaining: Number.POSITIVE_INFINITY };
    }

    const now = this.now();
    const perMs = this.perMinute / 60_000;
    const bucket = this.buckets.get(key) ?? { tokens: this.burst, updatedAt: now };

    const refilled = Math.min(this.burst, bucket.tokens + (now - bucket.updatedAt) * perMs);

    if (refilled < 1) {
      this.buckets.set(key, { tokens: refilled, updatedAt: now });
      return {
        allowed: false,
        retryAfterSeconds: Math.max(1, Math.ceil((1 - refilled) / perMs / 1000)),
        remaining: 0,
      };
    }

    const tokens = refilled - 1;
    this.buckets.set(key, { tokens, updatedAt: now });
    // Buckets are tiny and callers are few; sweep occasionally rather than on a timer, so
    // nothing keeps the process alive.
    if (this.buckets.size > 1000) this.sweep(now);
    return { allowed: true, retryAfterSeconds: 0, remaining: Math.floor(tokens) };
  }

  private sweep(now: number): void {
    const idleMs = 10 * 60_000;
    for (const [key, bucket] of this.buckets) {
      if (now - bucket.updatedAt > idleMs) this.buckets.delete(key);
    }
  }
}
