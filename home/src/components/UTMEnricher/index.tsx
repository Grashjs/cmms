"use client";

import { enrichWithClientParams } from "src/utils/urlPaths";
import { useEffect } from "react";

export default function UTMEnricher() {
  useEffect(() => {
    const handleClick = (e: MouseEvent) => {
      const target = e.target as HTMLElement;
      const link = target.closest("a[data-utm-enriched]") as HTMLAnchorElement | null;
      if (!link || !link.href) return;

      e.preventDefault();
      window.location.href = enrichWithClientParams(link.href);
    };

    document.addEventListener("click", handleClick);
    return () => document.removeEventListener("click", handleClick);
  }, []);

  return null;
}
