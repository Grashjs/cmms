import SuspenseLoader from '../../../components/SuspenseLoader';
import { useEffect } from 'react';
import { useSearchParams } from 'react-router-dom';
import useAuth from '../../../hooks/useAuth';

export default function OauthSuccess() {
  const [searchParams, setSearchParams] = useSearchParams();
  const { loginInternal } = useAuth();
  const token = searchParams.get('token');
  const refreshToken = searchParams.get('refreshToken');
  useEffect(() => {
    if (token) {
      loginInternal(token, refreshToken);
    }
  }, [token, refreshToken]);
  return <SuspenseLoader />;
}
