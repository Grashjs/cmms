import { useState, useEffect } from 'react';
import useAuth from './useAuth';
import api from '../utils/api';
import InAppReview from 'react-native-in-app-review';
import { navigate } from '../navigation/RootNavigation';

export function useReviewPrompt() {
  const { reviewEligible } = useAuth();
  const [visible, setVisible] = useState(false);

  useEffect(() => {
    if (reviewEligible && InAppReview.isAvailable()) {
      setVisible(true);
      api.post('reviews/mark-shown', {}).catch(() => {});
    }
  }, [reviewEligible]);

  const handleYes = async () => {
    setVisible(false);
    try {
      await api.post('reviews/clicked', {});
      const rated = await InAppReview.RequestInAppReview();
      if (rated) {
        api.post('reviews/rated', {}).catch(() => {});
      }
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
