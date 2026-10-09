import React, { createContext, useContext, useState, useEffect, type ReactNode } from 'react';
import type { User, UserRole, LoginCredentials, RegisterCredentials } from '../types/auth';
import { storage } from '../lib/storage';

interface AuthContextType {
  user: User | null;
  token: string | null;
  role: UserRole | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  login: (credentials: LoginCredentials) => Promise<void>;
  register: (credentials: RegisterCredentials) => Promise<void>;
  logout: () => void;
  switchRole: (role: UserRole) => void;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

// Default mock users for testing & demonstration
export const MOCK_ADMIN_USER: User = {
  id: 'usr_admin_01',
  name: 'Alex Rivera (Admin)',
  email: 'admin@pulse-gpt.ai',
  role: 'ADMIN',
  company: 'PulseGPT Inc.',
  avatar: 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=120&auto=format&fit=crop&q=60',
  createdAt: '2026-01-10T00:00:00.000Z',
};

export const MOCK_CREATOR_USER: User = {
  id: 'usr_creator_01',
  name: 'Sarah Chen',
  email: 'sarah.chen@creator.io',
  role: 'USER',
  company: 'Chen Media & Tech',
  avatar: 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=120&auto=format&fit=crop&q=60',
  createdAt: '2026-03-15T00:00:00.000Z',
};

export const AuthProvider: React.FC<{ children: ReactNode }> = ({ children }) => {
  const [user, setUser] = useState<User | null>(null);
  const [token, setToken] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState<boolean>(true);

  // Initialize auth state from sessionStorage on mount
  useEffect(() => {
    try {
      const storedToken = storage.getToken();
      const storedUser  = storage.getUser();

      if (storedToken && storedUser) {
        setToken(storedToken);
        setUser(storedUser);
      } else {
        // By default initialize as Creator User in mock mode for instant frictionless browsing if no session
        const defaultUser = MOCK_CREATOR_USER;
        const defaultToken = 'mock_jwt_token_creator_2026';
        storage.setToken(defaultToken);
        storage.setUser(defaultUser);
        setToken(defaultToken);
        setUser(defaultUser);
      }
    } catch (err) {
      console.error('Failed to initialize auth state:', err);
    } finally {
      setIsLoading(false);
    }
  }, []);

  const login = async (credentials: LoginCredentials): Promise<void> => {
    setIsLoading(true);
    // Simulate network delay
    await new Promise(resolve => setTimeout(resolve, 600));

    // Admin email check or standard user
    const isAdmin = credentials.email.toLowerCase().includes('admin');
    const authUser: User = isAdmin
      ? { ...MOCK_ADMIN_USER, email: credentials.email }
      : {
          id: `usr_${Date.now()}`,
          name: credentials.email.split('@')[0] || 'Pulse Creator',
          email: credentials.email,
          role: 'USER',
          company: 'Creator Studio',
          avatar: MOCK_CREATOR_USER.avatar,
          createdAt: new Date().toISOString(),
        };

    const generatedToken = `mock_jwt_${isAdmin ? 'admin' : 'user'}_${Date.now()}`;

    storage.setToken(generatedToken);
    storage.setUser(authUser);
    setToken(generatedToken);
    setUser(authUser);
    setIsLoading(false);
  };

  const register = async (credentials: RegisterCredentials): Promise<void> => {
    setIsLoading(true);
    await new Promise(resolve => setTimeout(resolve, 700));

    const newUser: User = {
      id: `usr_${Date.now()}`,
      name: credentials.name,
      email: credentials.email,
      role: 'USER',
      company: credentials.company || 'Creator Studio',
      avatar: MOCK_CREATOR_USER.avatar,
      createdAt: new Date().toISOString(),
    };

    const generatedToken = `mock_jwt_user_${Date.now()}`;

    storage.setToken(generatedToken);
    storage.setUser(newUser);
    setToken(generatedToken);
    setUser(newUser);
    setIsLoading(false);
  };

  const logout = (): void => {
    storage.clearAuth();
    setUser(null);
    setToken(null);
  };

  const switchRole = (newRole: UserRole): void => {
    if (!user) return;
    const updatedUser: User = {
      ...user,
      role: newRole,
      name: newRole === 'ADMIN' ? 'Alex Rivera (Admin)' : 'Sarah Chen',
    };
    storage.setUser(updatedUser);
    setUser(updatedUser);
  };

  return (
    <AuthContext.Provider
      value={{
        user,
        token,
        role: user ? user.role : null,
        isAuthenticated: !!user && !!token,
        isLoading,
        login,
        register,
        logout,
        switchRole,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = (): AuthContextType => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};
