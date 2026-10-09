import type { User } from '../types/auth';

const TOKEN_KEY = 'pulse_gpt_auth_token';
const USER_KEY  = 'pulse_gpt_auth_user';

export const storage = {
  getToken: (): string | null => {
    try {
      return sessionStorage.getItem(TOKEN_KEY);
    } catch {
      return null;
    }
  },

  setToken: (token: string): void => {
    try {
      sessionStorage.setItem(TOKEN_KEY, token);
    } catch (e) {
      console.error('Failed to save auth token to sessionStorage', e);
    }
  },

  removeToken: (): void => {
    try {
      sessionStorage.removeItem(TOKEN_KEY);
    } catch (e) {
      console.error('Failed to remove auth token from sessionStorage', e);
    }
  },

  getUser: (): User | null => {
    try {
      const raw = sessionStorage.getItem(USER_KEY);
      return raw ? JSON.parse(raw) : null;
    } catch {
      return null;
    }
  },

  setUser: (user: User): void => {
    try {
      sessionStorage.setItem(USER_KEY, JSON.stringify(user));
    } catch (e) {
      console.error('Failed to save auth user to sessionStorage', e);
    }
  },

  removeUser: (): void => {
    try {
      sessionStorage.removeItem(USER_KEY);
    } catch (e) {
      console.error('Failed to remove auth user from sessionStorage', e);
    }
  },

  clearAuth: (): void => {
    try {
      sessionStorage.removeItem(TOKEN_KEY);
      sessionStorage.removeItem(USER_KEY);
    } catch (e) {
      console.error('Failed to clear auth from sessionStorage', e);
    }
  },
};
