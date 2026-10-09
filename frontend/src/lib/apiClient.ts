import { storage } from './storage';

export const IS_MOCK_MODE = import.meta.env.VITE_USE_MOCK !== 'false';

export interface RequestOptions extends RequestInit {
  params?: Record<string, string | number | boolean | undefined>;
}

class ApiClient {
  private baseUrl: string;

  constructor() {
    this.baseUrl = import.meta.env.VITE_API_URL || 'http://localhost:8000/api/v1';
  }

  private getHeaders(): HeadersInit {
    const token = storage.getToken();
    const headers: Record<string, string> = {
      'Content-Type': 'application/json',
      Accept: 'application/json',
    };
    if (token) {
      headers['Authorization'] = `Bearer ${token}`;
    }
    return headers;
  }

  async get<T>(endpoint: string, options: RequestOptions = {}): Promise<T> {
    if (IS_MOCK_MODE) {
      // Mock mode handles routes via service functions with artificial micro-delays
      throw new Error(`Real network call attempted for GET ${endpoint} in mock mode.`);
    }

    const url = new URL(`${this.baseUrl}${endpoint}`);
    if (options.params) {
      Object.entries(options.params).forEach(([key, value]) => {
        if (value !== undefined) {
          url.searchParams.append(key, String(value));
        }
      });
    }

    const res = await fetch(url.toString(), {
      ...options,
      method: 'GET',
      headers: { ...this.getHeaders(), ...options.headers },
    });

    if (!res.ok) {
      const errorBody = await res.json().catch(() => ({}));
      throw new Error(errorBody.message || `Request failed with status ${res.status}`);
    }

    return res.json();
  }

  async post<T>(endpoint: string, body?: any, options: RequestOptions = {}): Promise<T> {
    if (IS_MOCK_MODE) {
      throw new Error(`Real network call attempted for POST ${endpoint} in mock mode.`);
    }

    const res = await fetch(`${this.baseUrl}${endpoint}`, {
      ...options,
      method: 'POST',
      headers: { ...this.getHeaders(), ...options.headers },
      body: body ? JSON.stringify(body) : undefined,
    });

    if (!res.ok) {
      const errorBody = await res.json().catch(() => ({}));
      throw new Error(errorBody.message || `Request failed with status ${res.status}`);
    }

    return res.json();
  }

  async put<T>(endpoint: string, body?: any, options: RequestOptions = {}): Promise<T> {
    if (IS_MOCK_MODE) {
      throw new Error(`Real network call attempted for PUT ${endpoint} in mock mode.`);
    }

    const res = await fetch(`${this.baseUrl}${endpoint}`, {
      ...options,
      method: 'PUT',
      headers: { ...this.getHeaders(), ...options.headers },
      body: body ? JSON.stringify(body) : undefined,
    });

    if (!res.ok) {
      const errorBody = await res.json().catch(() => ({}));
      throw new Error(errorBody.message || `Request failed with status ${res.status}`);
    }

    return res.json();
  }

  async patch<T>(endpoint: string, body?: any, options: RequestOptions = {}): Promise<T> {
    if (IS_MOCK_MODE) {
      throw new Error(`Real network call attempted for PATCH ${endpoint} in mock mode.`);
    }

    const res = await fetch(`${this.baseUrl}${endpoint}`, {
      ...options,
      method: 'PATCH',
      headers: { ...this.getHeaders(), ...options.headers },
      body: body ? JSON.stringify(body) : undefined,
    });

    if (!res.ok) {
      const errorBody = await res.json().catch(() => ({}));
      throw new Error(errorBody.message || `Request failed with status ${res.status}`);
    }

    return res.json();
  }

  async delete<T>(endpoint: string, options: RequestOptions = {}): Promise<T> {
    if (IS_MOCK_MODE) {
      throw new Error(`Real network call attempted for DELETE ${endpoint} in mock mode.`);
    }

    const res = await fetch(`${this.baseUrl}${endpoint}`, {
      ...options,
      method: 'DELETE',
      headers: { ...this.getHeaders(), ...options.headers },
    });

    if (!res.ok) {
      const errorBody = await res.json().catch(() => ({}));
      throw new Error(errorBody.message || `Request failed with status ${res.status}`);
    }

    return res.json();
  }

  async download(endpoint: string, options: RequestOptions = {}): Promise<Blob> {
    if (IS_MOCK_MODE) {
      return new Blob(['Mock production export file payload for offline preview.'], { type: 'text/plain' });
    }

    const url = new URL(`${this.baseUrl}${endpoint}`);
    if (options.params) {
      Object.entries(options.params).forEach(([key, value]) => {
        if (value !== undefined) {
          url.searchParams.append(key, String(value));
        }
      });
    }

    const token = storage.getToken();
    const headers: Record<string, string> = {
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...((options.headers as Record<string, string>) || {})
    };

    const res = await fetch(url.toString(), {
      ...options,
      method: 'GET',
      headers
    });

    if (!res.ok) {
      const errorBody = await res.json().catch(() => ({}));
      throw new Error(errorBody.message || `Download failed with status ${res.status}`);
    }

    return res.blob();
  }
}

export const apiClient = new ApiClient();

// Helper to simulate mock server async latency with typed response
export async function mockDelay<T>(data: T, delayMs: number = 300): Promise<T> {
  await new Promise(resolve => setTimeout(resolve, delayMs));
  return JSON.parse(JSON.stringify(data));
}
