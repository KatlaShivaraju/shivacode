
import {
  createContext,
  useContext,
  useState,
  type ReactNode,
} from 'react';

import type { AuthContextType, AuthResponse } from '../types/auth';

import { loginUser, registerUser } from '../services/authService';

const AuthContext = createContext<AuthContextType | undefined>(undefined);

const STORAGE_KEY = 'shivacode_auth';

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<AuthResponse | null>(() => {
    const storedUser = localStorage.getItem(STORAGE_KEY);

    return storedUser ? JSON.parse(storedUser) as AuthResponse : null;
  });

  const login = async (email: string, password: string) => {
    const response = await loginUser({ email, password });

    localStorage.setItem(STORAGE_KEY, JSON.stringify(response));
    setUser(response);
  };

  const register = async (
    username: string,
    email: string,
    password: string
  ) => {
    const response = await registerUser({ username, email, password });

    localStorage.setItem(STORAGE_KEY, JSON.stringify(response));
    setUser(response);
  };

  const logout = () => {
    localStorage.removeItem(STORAGE_KEY);
    setUser(null);
  };

  const value: AuthContextType = {
    user,
    isAuthenticated: user !== null,
    login,
    register,
    logout,
  };

  return (
    <AuthContext.Provider value={value}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const context = useContext(AuthContext);

  if (!context) {
    throw new Error('useAuth must be used inside AuthProvider');
  }

  return context;
}
