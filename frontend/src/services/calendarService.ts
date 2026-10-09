import { apiClient, mockDelay } from '../lib/apiClient';
import type {
  CalendarItem,
  CalendarConflictResponse,
  CalendarSuggestionResponse,
} from '../types/models';

export interface CreateCalendarItemPayload {
  recommendationId?: string;
  topicId?: string;
  title: string;
  platform: string;
  contentType?: string;
  scheduledStart: string;
  scheduledEnd?: string;
  timezone: string;
  priority?: 'LOW' | 'MEDIUM' | 'HIGH';
  notes?: string;
}

export interface UpdateCalendarItemPayload {
  title?: string;
  platform?: string;
  contentType?: string;
  scheduledStart?: string;
  scheduledEnd?: string;
  timezone?: string;
  status?: 'PLANNED' | 'SCHEDULED' | 'CANCELLED' | 'COMPLETED' | 'DRAFT' | 'PUBLISHED' | 'FAILED';
  priority?: 'LOW' | 'MEDIUM' | 'HIGH';
  notes?: string;
}

export interface CalendarQueryParams {
  startDate?: string;
  endDate?: string;
  platform?: string;
  contentType?: string;
  status?: string;
  topicId?: string;
  view?: string;
  page?: number;
  size?: number;
  sort?: string;
  direction?: 'asc' | 'desc';
}

let inMemoryCalendarItems: CalendarItem[] = [
  {
    id: 'cal-item-1',
    userId: 'user-default-1',
    title: 'Top 5 UI Trends for 2026 🎨',
    contentType: 'CAROUSEL',
    platform: 'INSTAGRAM',
    scheduledStart: '2026-10-10T17:00:00Z',
    scheduledEnd: '2026-10-10T18:00:00Z',
    timezone: 'Asia/Kolkata',
    status: 'SCHEDULED',
    priority: 'HIGH',
    notes: 'A breakdown of modern visual trends including refined glassmorphism and subtle borders.',
    topicName: 'Design Systems & UI Aesthetics',
    validationPassed: true,
  },
  {
    id: 'cal-item-2',
    userId: 'user-default-1',
    title: 'React 19 Server Actions vs Traditional REST Endpoints',
    contentType: 'VIDEO',
    platform: 'YOUTUBE',
    scheduledStart: '2026-10-12T14:30:00Z',
    scheduledEnd: '2026-10-12T15:30:00Z',
    timezone: 'Asia/Kolkata',
    status: 'SCHEDULED',
    priority: 'HIGH',
    notes: 'Deep dive into server actions, mutation safety, and optimistic UI rollbacks.',
    topicName: 'React 19 & Next.js 15 Server Patterns',
    validationPassed: true,
  },
  {
    id: 'cal-item-3',
    userId: 'user-default-1',
    title: 'Local Ollama vs OpenAI API Cost Breakdown',
    contentType: 'VIDEO',
    platform: 'YOUTUBE',
    scheduledStart: '2026-10-15T18:00:00Z',
    scheduledEnd: '2026-10-15T19:00:00Z',
    timezone: 'Asia/Kolkata',
    status: 'PLANNED',
    priority: 'MEDIUM',
    notes: 'Pricing and privacy matrix for running local models.',
    topicName: 'AI Engineering & LLM Economics',
    validationPassed: true,
  },
];

export const calendarService = {
  getCalendarItems: async (params?: CalendarQueryParams): Promise<CalendarItem[]> => {
    try {
      const queryStr = params
        ? '?' +
          new URLSearchParams(
            Object.entries(params)
              .filter(([_, v]) => v !== undefined && v !== null && v !== '')
              .map(([k, v]) => [k, String(v)])
          ).toString()
        : '';
      const res = await apiClient.get<any>(`/calendar${queryStr}`);
      if (res?.data?.content) {
        return res.data.content;
      }
      if (Array.isArray(res?.data)) {
        return res.data;
      }
    } catch (err) {
      // fallback to in-memory store
    }
    let items = [...inMemoryCalendarItems];
    if (params?.platform) {
      items = items.filter(i => i.platform.toLowerCase() === params.platform?.toLowerCase());
    }
    if (params?.status) {
      items = items.filter(i => i.status.toLowerCase() === params.status?.toLowerCase());
    }
    return mockDelay(items, 150);
  },

  getCalendarItemById: async (id: string): Promise<CalendarItem> => {
    try {
      const res = await apiClient.get<any>(`/calendar/${id}`);
      if (res?.data) return res.data;
    } catch (err) {
      // fallback
    }
    const item = inMemoryCalendarItems.find(i => i.id === id);
    if (!item) throw new Error('Calendar item not found');
    return mockDelay(item, 150);
  },

  createCalendarItem: async (payload: CreateCalendarItemPayload): Promise<CalendarItem> => {
    // Conflict check in mock mode if backend unreachable
    const overlapping = inMemoryCalendarItems.find(
      i =>
        i.platform.toUpperCase() === payload.platform.toUpperCase() &&
        i.status !== 'CANCELLED' &&
        new Date(i.scheduledStart) < new Date(payload.scheduledEnd || new Date(new Date(payload.scheduledStart).getTime() + 3600000).toISOString()) &&
        new Date(i.scheduledEnd) > new Date(payload.scheduledStart)
    );

    try {
      const res = await apiClient.post<any>('/calendar', payload);
      if (res?.data) return res.data;
    } catch (err: any) {
      if (err?.response?.status === 409) {
        throw err;
      }
    }

    if (overlapping) {
      const conflictError: any = new Error('Scheduling conflict detected on ' + payload.platform);
      conflictError.response = {
        status: 409,
        data: {
          conflict: true,
          message: 'Scheduling conflict detected with existing items on ' + payload.platform,
          conflicts: [
            {
              calendarItemId: overlapping.id,
              title: overlapping.title,
              platform: overlapping.platform,
              scheduledStart: overlapping.scheduledStart,
              scheduledEnd: overlapping.scheduledEnd,
            },
          ],
        },
      };
      throw conflictError;
    }

    const newItem: CalendarItem = {
      id: `cal-${Date.now()}`,
      userId: 'user-default-1',
      recommendationId: payload.recommendationId,
      topicId: payload.topicId,
      title: payload.title,
      contentType: payload.contentType || 'VIDEO',
      platform: payload.platform.toUpperCase(),
      scheduledStart: payload.scheduledStart,
      scheduledEnd: payload.scheduledEnd || new Date(new Date(payload.scheduledStart).getTime() + 3600000).toISOString(),
      timezone: payload.timezone,
      priority: payload.priority || 'MEDIUM',
      notes: payload.notes,
      status: 'SCHEDULED',
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
      validationPassed: true,
    };

    inMemoryCalendarItems = [newItem, ...inMemoryCalendarItems];
    return mockDelay(newItem, 200);
  },

  updateCalendarItem: async (id: string, payload: UpdateCalendarItemPayload): Promise<CalendarItem> => {
    try {
      const res = await apiClient.patch<any>(`/calendar/${id}`, payload);
      if (res?.data) return res.data;
    } catch (err) {
      // fallback
    }

    const index = inMemoryCalendarItems.findIndex(i => i.id === id);
    if (index === -1) throw new Error('Calendar item not found');

    const updated: CalendarItem = {
      ...inMemoryCalendarItems[index],
      ...payload,
      updatedAt: new Date().toISOString(),
    };
    inMemoryCalendarItems[index] = updated;
    return mockDelay(updated, 150);
  },

  cancelCalendarItem: async (id: string, reason?: string): Promise<CalendarItem> => {
    try {
      const res = await apiClient.post<any>(`/calendar/${id}/cancel`, null, {
        params: { reason },
      });
      if (res?.data) return res.data;
    } catch (err) {
      // fallback
    }

    const index = inMemoryCalendarItems.findIndex(i => i.id === id);
    if (index === -1) throw new Error('Calendar item not found');

    inMemoryCalendarItems[index].status = 'CANCELLED';
    inMemoryCalendarItems[index].cancelledAt = new Date().toISOString();
    return mockDelay(inMemoryCalendarItems[index], 150);
  },

  deleteCalendarItem: async (id: string): Promise<void> => {
    try {
      await apiClient.delete(`/calendar/${id}`);
      return;
    } catch (err) {
      // fallback
    }
    inMemoryCalendarItems = inMemoryCalendarItems.filter(i => i.id !== id);
    return mockDelay(undefined, 150);
  },

  checkConflicts: async (
    platform: string,
    start: string,
    end: string,
    excludeId?: string
  ): Promise<CalendarConflictResponse> => {
    try {
      const res = await apiClient.get<any>('/calendar/conflicts', {
        params: { platform, start, end, excludeId },
      });
      if (res?.data) return res.data;
    } catch (err) {
      // fallback
    }

    const conflicts = inMemoryCalendarItems.filter(
      i =>
        i.platform.toUpperCase() === platform.toUpperCase() &&
        i.status !== 'CANCELLED' &&
        (!excludeId || i.id !== excludeId) &&
        new Date(i.scheduledStart) < new Date(end) &&
        new Date(i.scheduledEnd) > new Date(start)
    );

    return mockDelay(
      {
        conflict: conflicts.length > 0,
        message: conflicts.length > 0 ? `Detected ${conflicts.length} conflict(s)` : 'No conflicts',
        conflicts: conflicts.map(c => ({
          calendarItemId: c.id,
          title: c.title,
          platform: c.platform,
          scheduledStart: c.scheduledStart,
          scheduledEnd: c.scheduledEnd,
        })),
      },
      100
    );
  },

  getSuggestions: async (
    date?: string,
    platform?: string,
    timezone?: string
  ): Promise<CalendarSuggestionResponse> => {
    try {
      const res = await apiClient.get<any>('/calendar/suggestions', {
        params: { date, platform, timezone },
      });
      if (res?.data) return res.data;
    } catch (err) {
      // fallback
    }

    const targetDate = date || '2026-10-15';
    const targetPlatform = platform || 'YOUTUBE';
    const targetTz = timezone || 'Asia/Kolkata';

    return mockDelay(
      {
        date: targetDate,
        platform: targetPlatform,
        timezone: targetTz,
        slots: [
          {
            slotName: 'Morning',
            time: '09:00',
            scheduledStart: `${targetDate}T09:00:00Z`,
            scheduledEnd: `${targetDate}T10:00:00Z`,
            available: true,
          },
          {
            slotName: 'Afternoon',
            time: '13:00',
            scheduledStart: `${targetDate}T13:00:00Z`,
            scheduledEnd: `${targetDate}T14:00:00Z`,
            available: true,
          },
          {
            slotName: 'Evening',
            time: '18:00',
            scheduledStart: `${targetDate}T18:00:00Z`,
            scheduledEnd: `${targetDate}T19:00:00Z`,
            available: true,
          },
        ],
      },
      150
    );
  },
};
