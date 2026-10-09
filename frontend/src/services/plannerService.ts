import { mockAiSuggestions, type AIContentSuggestion } from '../data/mockData';
import type { CalendarEvent } from '../types/models';
import { mockDelay } from '../lib/apiClient';

export interface PlannerCard {
  id: string;
  title: string;
  platform: string;
  description: string;
  status: 'ideas' | 'scripting' | 'production' | 'scheduled';
  audienceImpact?: number;
  outline: string[];
  scheduledDate?: string;
}

let inMemoryCards: PlannerCard[] = [
  {
    id: 'k-1',
    title: '5 Costly Mistakes In Production RAG Applications',
    platform: 'youtube',
    description: 'Based on recurring audience feedback around hallucinations and high token costs.',
    status: 'ideas',
    audienceImpact: 94,
    outline: [
      'Hook: Real-world latency demo',
      'Mistake 1: Fixed chunk sizes with no semantic boundaries',
      'Mistake 2: Missing dense-sparse hybrid re-ranking',
      'Mistake 3: Zero guardrail evaluation before response streaming',
      'Solution & open-source starter code',
    ],
  },
  {
    id: 'k-2',
    title: 'React 19 Server Actions vs Traditional REST Endpoints',
    platform: 'linkedin',
    description: 'Breakdown post explaining mutation safety, cache revalidation, and optimistic UI patterns.',
    status: 'scripting',
    audienceImpact: 88,
    outline: [
      'The Shift from useEffect to useActionState',
      'Optimistic state rollbacks made simple',
      'Security pitfalls: CSRF and parameter leakage',
    ],
  },
  {
    id: 'k-3',
    title: 'Building a Real-time Multi-platform Dashboard in React & Tailwind',
    platform: 'youtube',
    description: 'Comprehensive 40-minute build tutorial focusing on clean SaaS design systems.',
    status: 'production',
    audienceImpact: 82,
    outline: [
      'Design tokens and CSS custom properties',
      'Reusable accessible components with zero bloat',
      'Mock-first architecture with TanStack Query',
    ],
  },
  {
    id: 'k-4',
    title: 'Top 5 UI Trends for 2026 🎨',
    platform: 'instagram',
    description: 'High engagement carousel breakdown.',
    status: 'scheduled',
    audienceImpact: 75,
    outline: ['Glassmorphism 2.0', 'Micro-interactions', 'Subtle border glows'],
    scheduledDate: '2026-10-10',
  },
];

let inMemoryCalendarEvents: CalendarEvent[] = [
  {
    id: 'cal-1',
    title: 'Top 5 UI Trends for 2026 🎨',
    platform: 'instagram',
    scheduledDate: '2026-10-10',
    scheduledTime: '17:00',
    status: 'scheduled',
    format: 'Carousel',
    authorName: 'Sarah Chen',
    previewText: 'A breakdown of modern visual trends including refined glassmorphism and subtle borders.',
  },
  {
    id: 'cal-2',
    title: 'React 19 Server Actions deep dive',
    platform: 'linkedin',
    scheduledDate: '2026-10-12',
    scheduledTime: '14:30',
    status: 'scheduled',
    format: 'Long-form Post',
    authorName: 'Alex Rivera',
    previewText: 'Why architectural mindset shift matters when moving away from pure client-side queries.',
  },
  {
    id: 'cal-3',
    title: 'Local Ollama vs OpenAI API Cost Breakdown',
    platform: 'youtube',
    scheduledDate: '2026-10-15',
    scheduledTime: '18:00',
    status: 'draft',
    format: 'Video (15 min)',
    authorName: 'Sarah Chen',
  },
  {
    id: 'cal-4',
    title: 'Live Q&A on RAG Architecture',
    platform: 'youtube',
    scheduledDate: '2026-10-20',
    scheduledTime: '19:30',
    status: 'draft',
    format: 'Live Stream',
    authorName: 'Sarah Chen',
  },
];

export const plannerService = {
  getCards: async (): Promise<PlannerCard[]> => {
    return mockDelay(inMemoryCards, 200);
  },

  createCard: async (card: Omit<PlannerCard, 'id'>): Promise<PlannerCard> => {
    const newCard: PlannerCard = {
      ...card,
      id: `k-${Date.now()}`,
    };
    inMemoryCards = [newCard, ...inMemoryCards];
    return mockDelay(newCard, 200);
  },

  updateCardStatus: async (
    id: string,
    status: PlannerCard['status']
  ): Promise<PlannerCard> => {
    const card = inMemoryCards.find(c => c.id === id);
    if (!card) throw new Error('Card not found');
    card.status = status;
    return mockDelay(card, 150);
  },

  deleteCard: async (id: string): Promise<{ success: boolean }> => {
    inMemoryCards = inMemoryCards.filter(c => c.id !== id);
    return mockDelay({ success: true }, 150);
  },

  getAiSuggestions: async (): Promise<AIContentSuggestion[]> => {
    return mockDelay(mockAiSuggestions, 200);
  },

  getCalendarEvents: async (): Promise<CalendarEvent[]> => {
    return mockDelay(inMemoryCalendarEvents, 200);
  },

  addCalendarEvent: async (event: Omit<CalendarEvent, 'id'>): Promise<CalendarEvent> => {
    const newEvent: CalendarEvent = {
      ...event,
      id: `cal-${Date.now()}`,
    };
    inMemoryCalendarEvents = [...inMemoryCalendarEvents, newEvent];
    return mockDelay(newEvent, 200);
  },
};
