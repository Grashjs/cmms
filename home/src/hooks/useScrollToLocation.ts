"use client";
import { useRef, useEffect } from 'react';
import { usePathname, useSearchParams } from 'next/navigation';

const useScrollToLocation = () => {
  const scrolledRef = useRef(false);
  const pathname = usePathname();
  const searchParams = useSearchParams();
  const hashRef = useRef('');

  useEffect(() => {
    const hash = window.location.hash;

    if (hash) {
      if (hashRef.current !== hash) {
        hashRef.current = hash;
        scrolledRef.current = false;
      }

      if (!scrolledRef.current) {
        const id = hash.replace('#', '');
        const element = document.getElementById(id);
        if (element) {
          element.scrollIntoView(true);
          scrolledRef.current = true;
        }
      }
    }
  }, [pathname, searchParams]); // re-run on navigation changes
};

export default useScrollToLocation;