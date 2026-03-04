import React, { useState, useEffect, useCallback, useRef } from 'react';
import Joyride, {
  Step,
  CallBackProps,
  STATUS,
  EVENTS,
  ACTIONS
} from 'react-joyride';
import { useTranslation } from 'react-i18next';

interface WorkOrderTourProps {
  run: boolean;
  onComplete: () => void;
}

export default function WorkOrderTour({ run, onComplete }: WorkOrderTourProps) {
  const { t } = useTranslation();
  const [stepIndex, setStepIndex] = useState(0);
  const [ready, setReady] = useState(false); // true once demo-asset is confirmed in DOM
  const pollingRef = useRef<NodeJS.Timeout | null>(null);
  const timeoutRef = useRef<NodeJS.Timeout | null>(null);

  const steps: Step[] = [
    {
      target: 'body',
      title: t('welcome_to_atlas_cmms'),
      content: t('create_first_wo_tour'),
      placement: 'center',
      disableBeacon: true,
      isFixed: true
    },
    {
      target: '[data-tour="sidebar-assets"]',
      content: 'Click here to open Assets.',
      placement: 'right'
    },
    {
      target: '[data-tour="demo-asset"]',
      title: 'This is your demo HVAC system',
      content: "Now let's create a work order for it.",
      placement: 'center'
    }
  ];

  // Poll for demo-asset element when we reach step 2
  useEffect(() => {
    // Clean up any existing polling
    const cleanup = () => {
      if (pollingRef.current) clearInterval(pollingRef.current);
      if (timeoutRef.current) clearTimeout(timeoutRef.current);
    };

    if (!run || stepIndex !== 2) {
      // Not at the async step — if we were previously waiting, mark ready
      // so Joyride isn't blocked on other steps
      if (stepIndex !== 2) setReady(true);
      cleanup();
      return cleanup;
    }

    // We're at step 2 — check if element already exists
    if (document.querySelector('[data-tour="demo-asset"]')) {
      setReady(true);
      return cleanup;
    }

    // Element not yet in DOM — block Joyride and poll
    setReady(false);

    pollingRef.current = setInterval(() => {
      if (document.querySelector('[data-tour="demo-asset"]')) {
        cleanup();
        setReady(true); // Unblocks Joyride — no key reset needed
      }
    }, 200);

    timeoutRef.current = setTimeout(() => {
      cleanup();
      console.warn('[WorkOrderTour] demo-asset not found after 15s — skipping');
      setStepIndex((prev) => prev + 1);
      setReady(true);
    }, 15000);

    return cleanup;
  }, [run, stepIndex]);

  // Reset when tour (re)starts
  useEffect(() => {
    if (run) {
      setStepIndex(0);
      setReady(true); // Steps 0 and 1 don't need polling
    }
  }, [run]);

  const handleJoyrideCallback = useCallback(
    (data: CallBackProps) => {
      const { action, index, status, type } = data;

      if ([STATUS.FINISHED, STATUS.SKIPPED].includes(status as any)) {
        onComplete();
        setStepIndex(0);
        return;
      }

      if (type === EVENTS.STEP_AFTER) {
        const next = action === ACTIONS.PREV ? index - 1 : index + 1;
        // If moving TO step 2, pre-emptively mark not ready until polling confirms
        if (next === 2 && !document.querySelector('[data-tour="demo-asset"]')) {
          setReady(false);
        }
        setStepIndex(next);
      }

      if (type === EVENTS.TARGET_NOT_FOUND) {
        console.warn(
          `[WorkOrderTour] Target not found for step ${index}:`,
          steps[index]?.target
        );
        // Don't skip step 2 — polling will handle it by setting ready=true
        if (index !== 2) {
          setStepIndex((prev) => prev + 1);
        }
      }
    },
    [onComplete, steps]
  );

  return (
    <Joyride
      steps={steps}
      run={run && ready} // ← Only gate here; no key thrashing
      stepIndex={stepIndex}
      continuous
      showProgress
      showSkipButton
      callback={handleJoyrideCallback}
      disableOverlay={false}
      styles={{
        options: {
          zIndex: 10000,
          primaryColor: '#1976d2'
        }
      }}
      locale={{
        back: t('back'),
        close: t('close'),
        last: t('finish'),
        next: t('next'),
        skip: t('skip')
      }}
      floaterProps={{ disableAnimation: true }}
      spotlightPadding={10}
    />
  );
}
