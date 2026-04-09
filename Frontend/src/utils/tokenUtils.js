export const saveTokens = (accessToken, refreshToken) => {
  localStorage.setItem('accessToken', accessToken);
  localStorage.setItem('refreshToken', refreshToken);
};

export const getAccessToken = () => {
  return localStorage.getItem('accessToken');
};

export const getRefreshToken = () => {
  return localStorage.getItem('refreshToken');
};

export const clearTokens = () => {
  localStorage.removeItem('accessToken');
  localStorage.removeItem('refreshToken');
};

export const isTokenExpired = (token) => {
  if (!token) return true;
  try {
    const payload = JSON.parse(atob(token.split('.')[1]));
    return (Date.now() / 1000) > payload.exp;
  } catch (e) {
    if (import.meta.env.DEV) console.error("Error checking token expiration", e);
    return true;
  }
};

export const getUserFromToken = (token) => {
  if (!token) return null;
  try {
    const payload = JSON.parse(atob(token.split('.')[1]));
    return {
      uuid: payload.uuid,
      email: payload.email,
      role: payload.role
    };
  } catch (e) {
    if (import.meta.env.DEV) console.error("Error extracting user from token", e);
    return null;
  }
};
