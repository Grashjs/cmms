import { useEffect, useState } from 'react';
import useAuth from './useAuth';

interface UseMobileAppPromptReturn {
  isMobile: boolean;
  shouldShowPrompt: boolean;
  showPrompt: () => void;
  dismissPrompt: () => void;
}

const PROMPT_DISMISSED_KEY = 'mobileAppPromptDismissed';
const PROMPT_DELAY_MS = 3000; // 3 seconds delay before showing prompt

/**
 * Hook to detect mobile devices and manage app download prompt
 * @returns Object with mobile detection state and prompt controls
 */
export function useMobileAppPrompt(): UseMobileAppPromptReturn {
  const [isMobile, setIsMobile] = useState(false);
  const [shouldShowPrompt, setShouldShowPrompt] = useState(false);
  const { user } = useAuth();

  useEffect(() => {
    const checkMobile = () => {
      const userAgent = navigator.userAgent || navigator.vendor;
      const isMobileDevice =
        /Android|webOS|iPhone|iPad|iPod|BlackBerry|IEMobile|Opera Mini/i.test(
          userAgent
        ) ||
        (navigator.platform === 'MacIntel' && navigator.maxTouchPoints > 1);

      setIsMobile(isMobileDevice);

      // Show prompt only if mobile and not previously dismissed
      const isDismissed = localStorage.getItem(PROMPT_DISMISSED_KEY);
      if (isMobileDevice && !isDismissed) {
        const timer = setTimeout(() => {
          setShouldShowPrompt(true);
        }, PROMPT_DELAY_MS);

        return () => clearTimeout(timer);
      }
    };

    checkMobile();
  }, []);

  const showPrompt = () => {
    setShouldShowPrompt(true);
  };

  const dismissPrompt = () => {
    setShouldShowPrompt(false);
    localStorage.setItem(PROMPT_DISMISSED_KEY, 'true');
  };

  return {
    isMobile,
    shouldShowPrompt: shouldShowPrompt && !!user,
    showPrompt,
    dismissPrompt
  };
}
