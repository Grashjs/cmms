import { useContext } from 'react';
import AuthContext from 'src/contexts/JWTAuthContext';

const useAuth = () => useContext(AuthContext);

export default useAuth;
