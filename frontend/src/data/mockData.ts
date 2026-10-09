export interface Post {
  id: string;
  title: string;
  url: string;
  platform: string;
  publishedAt: string;
  views: number;
  likes: number;
  commentCount: number;
}

export interface ConnectedAccount {
  id: string;
  platform: string;
  handle: string;
  name: string;
  avatar: string;
  connectedAt: string;
  status: 'connected' | 'disconnected' | 'error';
  followerCount: number;
  postsCount: number;
  recentPosts: Post[];
}

export interface Comment {
  id: string;
  postId: string;
  postTitle: string;
  platform: string;
  author: string;
  authorAvatar: string;
  text: string;
  publishedAt: string;
  sentiment: 'positive' | 'neutral' | 'negative';
  sentimentScore: number; // -1 to 1
  priority: 'high' | 'medium' | 'low';
  tags: string[];
  replied: boolean;
  replyText?: string;
  replyAt?: string;
}

export const mockAccounts: ConnectedAccount[] = [
  {
    id: 'yt-1',
    platform: 'youtube',
    handle: '@techpulse_hub',
    name: 'TechPulse Hub',
    avatar: 'https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=150&auto=format&fit=crop&q=60&ixlib=rb-4.0.3',
    connectedAt: '2026-05-10',
    status: 'connected',
    followerCount: 142000,
    postsCount: 184,
    recentPosts: [
      { id: 'yt-p1', title: 'React Server Components: The Complete Guide', url: '#', platform: 'youtube', publishedAt: '2026-08-15', views: 45200, likes: 3200, commentCount: 18 },
      { id: 'yt-p2', title: 'Why I Switched from Tailwind to Vanilla CSS (Just Kidding)', url: '#', platform: 'youtube', publishedAt: '2026-08-10', views: 89000, likes: 6100, commentCount: 12 },
      { id: 'yt-p3', title: 'Building a RAG Chatbot from Scratch with Python', url: '#', platform: 'youtube', publishedAt: '2026-08-01', views: 28000, likes: 1950, commentCount: 15 },
    ]
  },
  {
    id: 'ig-1',
    platform: 'instagram',
    handle: '@techpulse.ai',
    name: 'TechPulse AI & Design',
    avatar: 'https://images.unsplash.com/photo-1614741118887-7a4ee193a5fa?w=150&auto=format&fit=crop&q=60&ixlib=rb-4.0.3',
    connectedAt: '2026-05-12',
    status: 'connected',
    followerCount: 89300,
    postsCount: 312,
    recentPosts: [
      { id: 'ig-p1', title: 'Top 5 UI Trends for 2026 🎨', url: '#', platform: 'instagram', publishedAt: '2026-08-18', views: 120000, likes: 14200, commentCount: 10 },
      { id: 'ig-p2', title: 'How LLMs actually work in 60 seconds 🤖', url: '#', platform: 'instagram', publishedAt: '2026-08-14', views: 245000, likes: 31800, commentCount: 14 },
    ]
  },
  {
    id: 'fb-1',
    platform: 'facebook',
    handle: 'TechPulseOfficial',
    name: 'TechPulse Official',
    avatar: 'https://images.unsplash.com/photo-1620641788421-7a1c342ea42e?w=150&auto=format&fit=crop&q=60&ixlib=rb-4.0.3',
    connectedAt: '2026-05-15',
    status: 'connected',
    followerCount: 215000,
    postsCount: 945,
    recentPosts: [
      { id: 'fb-p1', title: 'Exciting news! We are launching our new AI Dev Course next week.', url: '#', platform: 'facebook', publishedAt: '2026-08-19', views: 15000, likes: 980, commentCount: 8 },
      { id: 'fb-p2', title: 'Tech Trends Panel Discussion Live Stream', url: '#', platform: 'facebook', publishedAt: '2026-08-08', views: 8200, likes: 450, commentCount: 5 },
    ]
  }
];

export const mockComments: Comment[] = [
  // YouTube Comments
  {
    id: 'c-yt-1',
    postId: 'yt-p1',
    postTitle: 'React Server Components: The Complete Guide',
    platform: 'youtube',
    author: 'Amit Kumar',
    authorAvatar: 'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=80&auto=format&fit=crop&q=60',
    text: 'Great explanation of React Server Components! But can you explain how error boundaries work with RSCs? I got a bit confused there.',
    publishedAt: '2026-08-15T14:32:00Z',
    sentiment: 'neutral',
    sentimentScore: 0.1,
    priority: 'high',
    tags: ['question', 'rsc', 'error-handling'],
    replied: false
  },
  {
    id: 'c-yt-2',
    postId: 'yt-p1',
    postTitle: 'React Server Components: The Complete Guide',
    platform: 'youtube',
    author: 'Jessica Chen',
    authorAvatar: 'https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=80&auto=format&fit=crop&q=60',
    text: 'The audio is way too low in the first 5 minutes! I had to turn my speaker volume to 100%. Otherwise, the content is amazing.',
    publishedAt: '2026-08-15T15:10:00Z',
    sentiment: 'negative',
    sentimentScore: -0.65,
    priority: 'high',
    tags: ['feedback', 'audio-issue'],
    replied: false
  },
  {
    id: 'c-yt-3',
    postId: 'yt-p1',
    postTitle: 'React Server Components: The Complete Guide',
    platform: 'youtube',
    author: 'Marcus Aurelius',
    authorAvatar: 'https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=80&auto=format&fit=crop&q=60',
    text: 'Loved the visual slides you used to explain serialization! Subscribed immediately.',
    publishedAt: '2026-08-16T08:45:00Z',
    sentiment: 'positive',
    sentimentScore: 0.9,
    priority: 'low',
    tags: ['compliment', 'visuals'],
    replied: true,
    replyText: 'Thank you Marcus! Glad the slides helped clear things up. Appreciate the support!',
    replyAt: '2026-08-16T10:12:00Z'
  },
  {
    id: 'c-yt-4',
    postId: 'yt-p2',
    postTitle: 'Why I Switched from Tailwind to Vanilla CSS (Just Kidding)',
    platform: 'youtube',
    author: 'DevJohn',
    authorAvatar: 'https://images.unsplash.com/photo-1519345182560-3f2917c472ef?w=80&auto=format&fit=crop&q=60',
    text: 'Haha, got me with the title! But seriously, Tailwind makes prototyping so fast. I can never go back to writing custom classes.',
    publishedAt: '2026-08-10T18:22:00Z',
    sentiment: 'positive',
    sentimentScore: 0.75,
    priority: 'low',
    tags: ['discussion', 'tailwind'],
    replied: false
  },
  {
    id: 'c-yt-5',
    postId: 'yt-p2',
    postTitle: 'Why I Switched from Tailwind to Vanilla CSS (Just Kidding)',
    platform: 'youtube',
    author: 'Sarah Jenkins',
    authorAvatar: 'https://images.unsplash.com/photo-1438761681033-6461ffad8d80?w=80&auto=format&fit=crop&q=60',
    text: 'Do you have a Github repo with your tailwind configuration? I really liked that grid structure you built.',
    publishedAt: '2026-08-11T02:15:00Z',
    sentiment: 'neutral',
    sentimentScore: 0.2,
    priority: 'medium',
    tags: ['question', 'github', 'tailwind'],
    replied: false
  },
  {
    id: 'c-yt-6',
    postId: 'yt-p3',
    postTitle: 'Building a RAG Chatbot from Scratch with Python',
    platform: 'youtube',
    author: 'Vikram Singh',
    authorAvatar: 'https://images.unsplash.com/photo-1472099645785-5658abf4ff4e?w=80&auto=format&fit=crop&q=60',
    text: 'Wow, excellent tutorial! But you used OpenAI embeddings. Can we use free HuggingFace embeddings instead? Will the code structure remain the same?',
    publishedAt: '2026-08-01T20:11:00Z',
    sentiment: 'neutral',
    sentimentScore: 0.15,
    priority: 'high',
    tags: ['question', 'rag', 'embeddings'],
    replied: false
  },
  {
    id: 'c-yt-7',
    postId: 'yt-p3',
    postTitle: 'Building a RAG Chatbot from Scratch with Python',
    platform: 'youtube',
    author: 'Emma Watson',
    authorAvatar: 'https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=80&auto=format&fit=crop&q=60',
    text: 'This code fails at line 45 with a key error. Looks like the response schema changed in the latest langchain update.',
    publishedAt: '2026-08-02T11:40:00Z',
    sentiment: 'negative',
    sentimentScore: -0.5,
    priority: 'high',
    tags: ['bug', 'langchain', 'code-error'],
    replied: false
  },

  // Instagram Comments
  {
    id: 'c-ig-1',
    postId: 'ig-p1',
    postTitle: 'Top 5 UI Trends for 2026 🎨',
    platform: 'instagram',
    author: 'lucas_designs',
    authorAvatar: 'https://images.unsplash.com/photo-1539571696357-5a69c17a67c6?w=80&auto=format&fit=crop&q=60',
    text: 'Glassmorphism is still going strong! Really love slide #3 where you showed the card overlay. What border-radius are you using?',
    publishedAt: '2026-08-18T16:15:00Z',
    sentiment: 'positive',
    sentimentScore: 0.8,
    priority: 'medium',
    tags: ['ui-design', 'glassmorphism'],
    replied: false
  },
  {
    id: 'c-ig-2',
    postId: 'ig-p1',
    postTitle: 'Top 5 UI Trends for 2026 🎨',
    platform: 'instagram',
    author: 'sofia_creative',
    authorAvatar: 'https://images.unsplash.com/photo-1524504388940-b1c1722653e1?w=80&auto=format&fit=crop&q=60',
    text: 'Too much glow effect in these designs. It looks cool on Instagram but is terrible for real web accessibility. Keep contrast in mind guys.',
    publishedAt: '2026-08-18T17:40:00Z',
    sentiment: 'negative',
    sentimentScore: -0.4,
    priority: 'medium',
    tags: ['feedback', 'accessibility'],
    replied: false
  },
  {
    id: 'c-ig-3',
    postId: 'ig-p2',
    postTitle: 'How LLMs actually work in 60 seconds 🤖',
    platform: 'instagram',
    author: 'pixel_coder',
    authorAvatar: 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=80&auto=format&fit=crop&q=60',
    text: 'Best 60-second explanation I have seen! The attention mechanism graphic made it so easy to visualize. Please make one for RAG next!',
    publishedAt: '2026-08-14T10:05:00Z',
    sentiment: 'positive',
    sentimentScore: 0.95,
    priority: 'medium',
    tags: ['compliment', 'request'],
    replied: true,
    replyText: 'Appreciate it! A 60-second RAG reels script is actually in my planning queue for next week!',
    replyAt: '2026-08-14T12:00:00Z'
  },
  {
    id: 'c-ig-4',
    postId: 'ig-p2',
    postTitle: 'How LLMs actually work in 60 seconds 🤖',
    platform: 'instagram',
    author: 'deep_learner',
    authorAvatar: 'https://images.unsplash.com/photo-1517841905240-472988babdf9?w=80&auto=format&fit=crop&q=60',
    text: 'Wait, did you skip temperature parameter? It is crucial for understanding output variety.',
    publishedAt: '2026-08-14T11:55:00Z',
    sentiment: 'neutral',
    sentimentScore: -0.05,
    priority: 'low',
    tags: ['feedback', 'details'],
    replied: false
  },

  // Facebook Comments
  {
    id: 'c-fb-1',
    postId: 'fb-p1',
    postTitle: 'Exciting news! We are launching our new AI Dev Course next week.',
    platform: 'facebook',
    author: 'Rajesh Patel',
    authorAvatar: 'https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?w=80&auto=format&fit=crop&q=60',
    text: 'Will there be any discount for early bird registrations? I want to enroll in the full-stack AI dev module.',
    publishedAt: '2026-08-19T09:40:00Z',
    sentiment: 'positive',
    sentimentScore: 0.6,
    priority: 'high',
    tags: ['question', 'course', 'pricing'],
    replied: false
  },
  {
    id: 'c-fb-2',
    postId: 'fb-p1',
    postTitle: 'Exciting news! We are launching our new AI Dev Course next week.',
    platform: 'facebook',
    author: 'Maria Garcia',
    authorAvatar: 'https://images.unsplash.com/photo-1554151228-14d9def656e4?w=80&auto=format&fit=crop&q=60',
    text: 'Is the course beginner friendly or do we need some Python/React experience beforehand?',
    publishedAt: '2026-08-19T10:15:00Z',
    sentiment: 'neutral',
    sentimentScore: 0.0,
    priority: 'medium',
    tags: ['question', 'course', 'prerequisites'],
    replied: false
  },
  {
    id: 'c-fb-3',
    postId: 'fb-p2',
    postTitle: 'Tech Trends Panel Discussion Live Stream',
    platform: 'facebook',
    author: 'David Miller',
    authorAvatar: 'https://images.unsplash.com/photo-1560250097-0b93528c311a?w=80&auto=format&fit=crop&q=60',
    text: 'Audio is crackling at 15:24 during the panel discussion, hard to follow what the speaker on the left is saying.',
    publishedAt: '2026-08-08T15:45:00Z',
    sentiment: 'negative',
    sentimentScore: -0.7,
    priority: 'high',
    tags: ['feedback', 'audio-issue', 'live-stream'],
    replied: false
  }
];

// Simulated vector-store query engine for RAG dashboard demo
export interface RAGCitation {
  id: string;
  author: string;
  platform: string;
  text: string;
  postTitle: string;
}

export interface RAGResponse {
  answer: string;
  citations: RAGCitation[];
}

export function simulateRAGQuery(query: string): Promise<RAGResponse> {
  return new Promise((resolve) => {
    setTimeout(() => {
      const q = query.toLowerCase();
      let answer = '';
      let citations: RAGCitation[] = [];

      // Look for keywords
      if (q.includes('audio') || q.includes('sound') || q.includes('hear') || q.includes('crackl')) {
        citations = mockComments
          .filter(c => c.text.toLowerCase().includes('audio') || c.text.toLowerCase().includes('volume') || c.text.toLowerCase().includes('crackl'))
          .map(c => ({ id: c.id, author: c.author, platform: c.platform, text: c.text, postTitle: c.postTitle }));

        answer = `Based on our social comments database, audience feedback reveals a significant concern regarding **audio quality** in recent contents:

1. **Volume Issue on YouTube [1]**: Multiple users have complained that the audio level on the video *"React Server Components: The Complete Guide"* is extremely low during the initial five minutes. Author Jessica Chen had to increase speaker volume to 100%.
2. **Audio Crackling on Facebook Live [2]**: A critical comment on the Live Stream recording indicates that there is noticeable audio crackling around timestamp 15:24, which disrupts the speech of the guest panelist on the left.

**Actionable Recommendations:**
* Add a compressor and limiter in your audio post-processing chain to normalize levels before publishing.
* Check the microphone/connection settings used during live streams, specifically verifying channel configurations for the secondary microphone used by left-positioned guests.`;
      } 
      else if (q.includes('tailwind') || q.includes('css')) {
        citations = mockComments
          .filter(c => c.text.toLowerCase().includes('tailwind') || c.text.toLowerCase().includes('css'))
          .map(c => ({ id: c.id, author: c.author, platform: c.platform, text: c.text, postTitle: c.postTitle }));

        answer = `Regarding questions on **Tailwind and styling methodologies**, our search retrieved 2 comments:

1. **Fast Prototyping Praise [1]**: Viewers appreciate Tailwind's ability to facilitate rapid prototype buildouts. YouTube user DevJohn commented that Tailwind speeds up workflow significantly, making it hard to return to traditional vanilla CSS.
2. **Configuration Request [2]**: Another user (Sarah Jenkins) requested the GitHub link for the specific Tailwind configuration file, highlighting that they really liked the layout grids displayed in the tutorial video.

**Actionable Recommendations:**
* Ensure a public GitHub repository link containing the configuration files and layout patterns is appended to the video description.
* Create a micro-content piece highlighting your customized Tailwind overrides and plugins used for layout creation.`;
      }
      else if (q.includes('course') || q.includes('price') || q.includes('discount') || q.includes('cost')) {
        citations = mockComments
          .filter(c => c.text.toLowerCase().includes('course') || c.text.toLowerCase().includes('discount') || c.text.toLowerCase().includes('enroll') || c.text.toLowerCase().includes('beginner'))
          .map(c => ({ id: c.id, author: c.author, platform: c.platform, text: c.text, postTitle: c.postTitle }));

        answer = `There is high audience interest regarding the **upcoming AI Developer Course**. Retrieved comments indicate two main points of concern:

1. **Pricing & Early Discounts [1]**: Users are eager to enroll (specifically full-stack AI development modules) and are asking if an early-bird discount or coupon codes will be available for registrations.
2. **Prerequisites & Skill Levels [2]**: Maria Garcia on Facebook inquired whether the course is beginner-friendly or requires prior knowledge of React/Python.

**Actionable Recommendations:**
* Send out a marketing post outlining course prerequisites and recommending introductory resources for complete beginners.
* Finalize the early bird discount structure and dispatch a promotional email or announcement post with discount codes.`;
      }
      else if (q.includes('rag') || q.includes('llm') || q.includes('embeddings')) {
        citations = mockComments
          .filter(c => c.text.toLowerCase().includes('rag') || c.text.toLowerCase().includes('huggingface') || c.text.toLowerCase().includes('embeddings') || c.text.toLowerCase().includes('llm'))
          .map(c => ({ id: c.id, author: c.author, platform: c.platform, text: c.text, postTitle: c.postTitle }));

        answer = `The audience is highly engaged with **AI, LLM, and RAG concepts**, expressing interest in both conceptual understanding and alternative code implementations:

1. **Visual Graphic Praise [1]**: The 60-second Reels/Shorts format explaining LLMs received strong praise, with users requesting a similar, quick summary for Retrieval Augmented Generation (RAG).
2. **HuggingFace Integration [2]**: On the RAG tutorial, viewers asked whether free HuggingFace embeddings could be swapped in place of OpenAI API components, inquiring if the code structure would remain compatible.

**Actionable Recommendations:**
* Publish a follow-up mini-video/reel explaining RAG mechanics in a simplified visual style.
* Create a supplementary code branch in the RAG GitHub repository demonstrating HuggingFace embeddings integration as an open-source alternative.`;
      }
      else {
        // Default generic query match
        citations = mockComments.slice(0, 2).map(c => ({ id: c.id, author: c.author, platform: c.platform, text: c.text, postTitle: c.postTitle }));
        answer = `I scanned all connected platform comments for "${query}". While I didn't find specific high-density clusters, here is a general analysis based on recent active topics:

We have retrieved 2 recent comments [1, 2] that show general engagement around your React Server Components tutorial. Overall sentiment remains positive, with users asking clarifying questions about error handling and requesting visual layouts.

**Suggestions:**
- Continue addressing questions about complex React architecture in comments.
- Keep highlighting visual layouts since they drive immediate positive reviews and subscriptions.`;
      }

      resolve({ answer, citations });
    }, 1500); // 1.5 second typing/retrieval simulation delay
  });
}

// Generate smart replies based on comment text and target tone
export function generateSmartReply(commentText: string, tone: 'professional' | 'friendly' | 'witty' | 'apologetic'): string {
  const isQuestion = commentText.includes('?') || commentText.toLowerCase().includes('how') || commentText.toLowerCase().includes('can you');
  const isNegative = commentText.toLowerCase().includes('low') || commentText.toLowerCase().includes('bad') || commentText.toLowerCase().includes('fail') || commentText.toLowerCase().includes('error') || commentText.toLowerCase().includes('issue');

  if (isNegative) {
    switch (tone) {
      case 'professional':
        return "Thank you for bringing this to our attention. We apologize for the inconvenience and are actively looking into resolving this issue. We appreciate your patience.";
      case 'friendly':
        return "Oh no! So sorry you're running into this issue. Thanks for letting us know—we're working on a fix right now. Stay tuned!";
      case 'witty':
        return "Looks like a bug slipped past our code sentinels! Thanks for the heads-up; we are deployed and sending reinforcement fixes shortly.";
      case 'apologetic':
        return "We are very sorry for the issue you're experiencing with this. We strive for high quality and clearly missed the mark here. We are fixing it immediately.";
    }
  }

  if (isQuestion) {
    switch (tone) {
      case 'professional':
        return "That is a great question. We will compile the requested files/guides and update our description links shortly. Let us know if you need further details.";
      case 'friendly':
        return "Great question! Yes, we're planning to share the complete repository link and setup guide very soon. Hope this helps you out!";
      case 'witty':
        return "A details-detective! Love it. The source files are being polished as we speak and will be available in the github link very shortly!";
      case 'apologetic':
        return "Apologies if this wasn't clear in the video. We will release a detailed written guide on this topic in our next repository commit.";
    }
  }

  // Compliments or generic
  switch (tone) {
    case 'professional':
      return "Thank you for your valuable feedback. We are glad you find our content beneficial and look forward to sharing more insights soon.";
    case 'friendly':
      return "Thanks a lot for the support! It comments like yours that keep us motivated to build and share. You rock! 🙌";
    case 'witty':
      return "Subscribe for the code, stay for the memes! Glad you liked the video, appreciate you being here!";
    case 'apologetic':
      return "Thank you for the encouragement. Apologies if some segments felt a bit rushed, we will aim to expand more in the future.";
  }
}

// Mock AI content ideas suggestions
export interface AIContentSuggestion {
  id: string;
  title: string;
  platform: string;
  sourceIssue: string;
  audienceCount: number;
  outline: string[];
}

export const mockAiSuggestions: AIContentSuggestion[] = [
  {
    id: 'sug-1',
    title: 'RAG Architecture Explained for Beginners',
    platform: 'instagram',
    sourceIssue: "Audience asking for simplified RAG explanation after the LLM 60-second reel.",
    audienceCount: 142,
    outline: [
      "What does Retrieval Augmented Generation actually mean?",
      "3 main steps: Retrieve (Search), Augment (Add Context), Generate (LLM answer)",
      "Analogy: Open-book exam vs closed-book exam",
      "Tools needed: Vector DB (Pinecone/Chroma) and LLM API"
    ]
  },
  {
    id: 'sug-2',
    title: 'React Server Components Error Boundaries & Loading States',
    platform: 'youtube',
    sourceIssue: "Multiple comments asking how to handle boundary errors and loading layouts in RSC.",
    audienceCount: 89,
    outline: [
      "Why standard React error boundaries work differently on server components",
      "Using error.tsx and loading.tsx in Next.js App Router",
      "Creating custom fallback skeletons for asynchronous server components",
      "Best practices for server-to-client boundaries"
    ]
  },
  {
    id: 'sug-3',
    title: 'Connecting HuggingFace Free Embeddings in Python RAG Pipelines',
    platform: 'youtube',
    sourceIssue: "Users seeking open-source embedding models instead of paying for OpenAI APIs.",
    audienceCount: 54,
    outline: [
      "Introduction to HuggingFace Inference API vs local sentence-transformers",
      "Step-by-step code adjustments in LangChain/LlamaIndex",
      "Comparing retrieval accuracy and speed between OpenAI and SentenceTransformers",
      "Hosting a free Python RAG app on HuggingFace Spaces"
    ]
  }
];
