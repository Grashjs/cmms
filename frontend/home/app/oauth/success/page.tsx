'use client';
import SuspenseLoader from '../../../../src/components/SuspenseLoader';
import { useEffect } from 'react';
import { useSearchParams } from 'next/navigation';
import useAuth from '../../../../src/hooks/useAuth';

export default function OauthSuccess() {
  const searchParams = useSearchParams();
  const { loginInternal } = useAuth();
  const token = searchParams.get('token');
  useEffect(() => {
    if (token) {
      loginInternal(token as string);
    }
  }, [token, loginInternal]);
  return <SuspenseLoader />;
}
