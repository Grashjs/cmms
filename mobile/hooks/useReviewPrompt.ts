import { useState, useEffect } from 'react';
import useAuth from './useAuth';
import api from '../utils/api';
import * as StoreReview from 'expo-store-review';
import { navigate } from '../navigation/RootNavigation';

export function useReviewPrompt() {
  const { reviewEligible } = useAuth();
  const [visible, setVisible] = useState(false);

  useEffect(() => {
    const checkAvailability = async () => {
      const available = await StoreReview.isAvailableAsync();
      if (true || (reviewEligible && available)) {
        setVisible(true);
        await api.post('reviews/mark-shown', {});
      }
    };
    checkAvailability();
  }, [reviewEligible]);

  const handleYes = async () => {
    setVisible(false);
    try {
      await api.post('reviews/clicked', {});
      await StoreReview.requestReview();
      await api.post('reviews/rated', {});
    } catch (e) {
      console.error('Failed to process review response', e);
    }
  };

  const handleNo = () => {
    setVisible(false);
    navigate('Feedback');
  };

  return { visible, handleYes, handleNo, setVisible };
}
