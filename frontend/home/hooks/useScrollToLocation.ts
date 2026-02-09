import { useRef, useEffect } from 'react';
import { useLocation } from 'react-router-dom';

const useScrollToLocation = () => {
  const scrolledRef = useRef(false);
  const { hash } = useLocation();
  const hashRef = useRef(hash);

  useEffect(() => {
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
  });
};

export default useScrollToLocation;
