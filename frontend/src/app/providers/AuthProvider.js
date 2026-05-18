import AsyncStorage from '@react-native-async-storage/async-storage';
import { Platform } from 'react-native';
import { createContext, useContext, useEffect, useMemo, useState } from 'react';
import { STORAGE_KEYS } from '../../shared/constants/storageKeys';
import apiClient from '../../shared/services/http/apiClient';
import { onAuthSessionInvalidated } from '../../shared/services/http/authSessionEvents';

const AuthContext = createContext(null);
const TOKEN_STORAGE_KEY = 'auth_token';
const USER_STORAGE_KEY = 'auth_user';

// Helper to save to storage (handles both native and web)
const saveToStorage = async (key, value) => {
  try {
    if (Platform.OS === 'web' && typeof window !== 'undefined') {
      window.localStorage.setItem(key, value);
    } else {
      await AsyncStorage.setItem(key, value);
    }
  } catch (error) {
    // Silently fail for storage errors
  }
};

// Helper to get from storage (handles both native and web)
const getFromStorage = async (key) => {
  try {
    let value;
    if (Platform.OS === 'web' && typeof window !== 'undefined') {
      value = window.localStorage.getItem(key);
    } else {
      value = await AsyncStorage.getItem(key);
    }
    return value;
  } catch (error) {
    return null;
  }
};

// Helper to remove from storage (handles both native and web)
const removeFromStorage = async (key) => {
  try {
    if (Platform.OS === 'web' && typeof window !== 'undefined') {
      window.localStorage.removeItem(key);
    } else {
      await AsyncStorage.removeItem(key);
    }
  } catch (error) {
    // Silently fail for storage errors
  }
};

const parseStoredUser = (rawUser) => {
  if (!rawUser) {
    return null;
  }

  try {
    return JSON.parse(rawUser);
  } catch (error) {
    return null;
  }
};

const clearAuthStorage = async () => {
  await removeFromStorage(TOKEN_STORAGE_KEY);
  await removeFromStorage(USER_STORAGE_KEY);
};

const clearPendingBusinessSignupDraft = () => {
  if (Platform.OS === 'web' && typeof window !== 'undefined') {
    window.localStorage.removeItem(STORAGE_KEYS.PENDING_BUSINESS_CHECKOUT);
  }
};

export function AuthProvider({ children }) {
  const [isAuthenticated, setIsAuthenticated] = useState(false);
  const [isLoadingAuth, setIsLoadingAuth] = useState(true);
  const [user, setUser] = useState(null);
  const [token, setToken] = useState(null);

  useEffect(() => {
    const bootstrapAuth = async () => {
      const storedToken = await getFromStorage(TOKEN_STORAGE_KEY);
      const userData = await getFromStorage(USER_STORAGE_KEY);
      const parsedUser = parseStoredUser(userData);

      const clearSessionState = async () => {
        await clearAuthStorage();
        setToken(null);
        setUser(null);
        setIsAuthenticated(false);
      };

      // If there's a stored token try to validate it with the backend
      if (storedToken) {
        try {
          const resp = await apiClient.get('/api/v1/users/me');
          // Prefer server-provided user data to ensure roles are up to date
          const serverUser = resp?.data;
          if (serverUser) {
            setToken(storedToken);
            setUser(serverUser);
            await saveToStorage(USER_STORAGE_KEY, JSON.stringify(serverUser));
          } else if (parsedUser) {
            setToken(storedToken);
            setUser(parsedUser);
          } else {
            await clearSessionState();
            setIsLoadingAuth(false);
            return;
          }

          setIsAuthenticated(true);
        } catch (err) {
          const status = err?.response?.status;
          // If the token is invalid or unauthorized, clear it immediately so
          // the frontend does not keep showing an obsolete authenticated state.
          if (status === 401 || status === 403) {
            await clearSessionState();
          } else {
            setToken(storedToken);
            if (parsedUser) {
              setUser(parsedUser);
            }
            setIsAuthenticated(Boolean(storedToken));
          }
        }
      } else {
        setToken(null);
        if (parsedUser) {
          setUser(parsedUser);
        }
        setIsAuthenticated(false);
      }

      setIsLoadingAuth(false);
    };

    bootstrapAuth();

    const unsubscribe = onAuthSessionInvalidated(() => {
      logout();
    });

    return () => {
      unsubscribe();
    };
  }, []);

  const login = async (newToken, userData) => {
    await saveToStorage(TOKEN_STORAGE_KEY, newToken);
    setToken(newToken);

    if (userData) {
      await saveToStorage(USER_STORAGE_KEY, JSON.stringify(userData));
      setUser(userData);
    }

    setIsAuthenticated(true);
    clearPendingBusinessSignupDraft();
  };

  const logout = async () => {
    await removeFromStorage(TOKEN_STORAGE_KEY);
    await removeFromStorage(USER_STORAGE_KEY);
    clearPendingBusinessSignupDraft();
    setToken(null);
    setUser(null);
    setIsAuthenticated(false);
  };

  /**
   * Update user roles after role change.
   * Called after successful role change from backend.
   */
  const updateUserRoles = async (newToken, newRoles) => {
    await saveToStorage(TOKEN_STORAGE_KEY, newToken);
    setToken(newToken);

    // Update user object with new roles
    setUser((prevUser) => ({
      ...prevUser,
      roles: newRoles,
    }));

    // Also update persistent storage
    const userData = await getFromStorage(USER_STORAGE_KEY);
    if (userData) {
      const userObj = JSON.parse(userData);
      userObj.roles = newRoles;
      await saveToStorage(USER_STORAGE_KEY, JSON.stringify(userObj));
    }
  };

  const value = useMemo(
    () => ({
      isAuthenticated,
      isLoadingAuth,
      user,
      token,
      login,
      logout,
      updateUserRoles,
    }),
    [isAuthenticated, isLoadingAuth, user, token]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);

  if (!context) {
    throw new Error('useAuth must be used inside AuthProvider');
  }

  return context;
}
