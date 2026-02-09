'use client';
import SuspenseLoader from '../../../../src/components/SuspenseLoader';
import { useContext, useEffect } from 'react';
import { useRouter, useSearchParams } from 'next/navigation';
import useAuth from '../../../../src/hooks/useAuth';
import { CustomSnackBarContext } from '../../../../src/contexts/CustomSnackBarContext';

export default function OauthFailure() {
  const searchParams = useSearchParams();
  const { loginInternal } = useAuth();
  const error = searchParams.get('error');
  const { showSnackBar } = useContext(CustomSnackBarContext);
  const router = useRouter();

  useEffect(() => {
    if (error) {
      showSnackBar(error, 'error');
      router.push('/account/login');
    }
  }, [error, router, showSnackBar]);
  return <SuspenseLoader />;
}
