import { mockComments, type Comment } from '../data/mockData';
import { mockDelay } from '../lib/apiClient';

let inMemoryComments = [...mockComments];

export const commentsService = {
  getComments: async (params?: {
    platform?: string;
    sentiment?: string;
    priority?: string;
    search?: string;
  }): Promise<Comment[]> => {
    let list = [...inMemoryComments];

    if (params?.platform && params.platform !== 'all') {
      list = list.filter(c => c.platform.toLowerCase() === params.platform?.toLowerCase());
    }

    if (params?.sentiment && params.sentiment !== 'all') {
      list = list.filter(c => c.sentiment.toLowerCase() === params.sentiment?.toLowerCase());
    }

    if (params?.priority && params.priority !== 'all') {
      list = list.filter(c => c.priority.toLowerCase() === params.priority?.toLowerCase());
    }

    if (params?.search && params.search.trim()) {
      const q = params.search.toLowerCase();
      list = list.filter(
        c => c.text.toLowerCase().includes(q) || c.author.toLowerCase().includes(q) || c.postTitle.toLowerCase().includes(q)
      );
    }

    return mockDelay(list, 200);
  },

  postReply: async (commentId: string, replyText: string): Promise<Comment> => {
    const idx = inMemoryComments.findIndex(c => c.id === commentId);
    if (idx === -1) {
      throw new Error(`Comment with id ${commentId} not found`);
    }

    const updated: Comment = {
      ...inMemoryComments[idx],
      replied: true,
      replyText,
      replyAt: new Date().toISOString(),
    };

    inMemoryComments[idx] = updated;
    return mockDelay(updated, 300);
  },

  generateSmartReply: async (commentId: string): Promise<string> => {
    const comment = inMemoryComments.find(c => c.id === commentId);
    if (!comment) throw new Error('Comment not found');

    await new Promise(r => setTimeout(r, 600));

    if (comment.sentiment === 'positive') {
      return `Thank you so much, ${comment.author}! Really appreciate your support and feedback. Stay tuned for upcoming deep-dives! 🚀`;
    } else if (comment.sentiment === 'negative') {
      return `Hi ${comment.author}, thanks for bringing this up. We take this feedback seriously and are actively resolving this in our next release!`;
    } else {
      return `Great observation, ${comment.author}! We covered more details on this in our documentation and upcoming roadmap.`;
    }
  },
};
