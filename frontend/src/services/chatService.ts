import { simulateRAGQuery, type RAGCitation } from '../data/mockData';
import { mockDelay } from '../lib/apiClient';

export interface ChatMessage {
  id: string;
  sender: 'user' | 'ai';
  text: string;
  timestamp: string;
  citations?: RAGCitation[];
}

export const chatService = {
  sendQuery: async (
    query: string
  ): Promise<{ text: string; citations: RAGCitation[] }> => {
    // Artificial delay to simulate embedding search + LLM generation
    const result = await simulateRAGQuery(query);
    return mockDelay({ text: result.answer, citations: result.citations }, 100);
  },
};
