import { createContext, useState, useEffect } from 'react';
import { login as apiLogin, register as apiRegister, logout as apiLogout } from '../api/authApi';
import { getMyProfile } from '../api/userApi';
import { saveTokens, clearTokens, getAccessToken, getRefreshToken, isTokenExpired } from '../utils/tokenUtils';

export const AuthContext = createContext();

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(null);
  const [isAuthenticated, setIsAuthenticated] = useState(false);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    const initAuth = async () => {
      const accessToken = getAccessToken();
      if (accessToken && !isTokenExpired(accessToken)) {
        try {
          const profileData = await getMyProfile();
          setUser(profileData);
          setIsAuthenticated(true);
        } catch (error) {
          if (import.meta.env.DEV) console.error("Error hydrating profile", error);
          clearTokens();
          setUser(null);
          setIsAuthenticated(false);
        }
      } else {
        clearTokens();
        setUser(null);
        setIsAuthenticated(false);
      }
      setIsLoading(false);
    };

    initAuth();
  }, []);

  const login = async (email, password) => {
    const response = await apiLogin(email, password);
    saveTokens(response.accessToken, response.refreshToken);
    if (response.user) {
      setUser(response.user);
    } else {
      const profileData = await getMyProfile();
      setUser(profileData);
    }
    setIsAuthenticated(true);
    return response;
  };

  const register = async (fullName, email, password) => {
    const response = await apiRegister(fullName, email, password);
    saveTokens(response.accessToken, response.refreshToken);
    if (response.user) {
      setUser(response.user);
    } else {
      const profileData = await getMyProfile();
      setUser(profileData);
    }
    setIsAuthenticated(true);
    return response;
  };

  const logout = async () => {
    try {
      const refreshToken = getRefreshToken();
      if (refreshToken) {
        await apiLogout(refreshToken);
      }
    } catch (error) {
      if (import.meta.env.DEV) console.error("Error during server logout", error);
    } finally {
      clearTokens();
      setUser(null);
      setIsAuthenticated(false);
      window.location.href = '/login';
    }
  };

  const value = {
    user,
    isAuthenticated,
    isLoading,
    login,
    register,
    logout
  };

  return (
    <AuthContext.Provider value={value}>
      {children}
    </AuthContext.Provider>
  );
};

export default AuthContext;
