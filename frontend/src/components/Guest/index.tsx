import { FC, ReactNode } from 'react';
import { Navigate } from 'react-router-dom';

import PropTypes from 'prop-types';
import useAuth from 'src/hooks/useAuth';

interface GuestProps {
  children: ReactNode;
}

const Guest: FC<GuestProps> = ({ children }) => {
  const { isAuthenticated, user } = useAuth();

  if (isAuthenticated) {
    return (
      <Navigate
        to={
          user.role?.roleType === 'ROLE_SUPER_ADMIN'
            ? '/app/superadmin/companies'
            : user.role.code === 'REQUESTER'
            ? '/app/requests'
            : '/app/work-orders'
        }
      />
    );
  }

  return <>{children}</>;
};

Guest.propTypes = {
  children: PropTypes.node
};

export default Guest;
