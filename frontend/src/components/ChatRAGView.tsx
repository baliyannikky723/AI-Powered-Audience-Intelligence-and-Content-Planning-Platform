import React, { useState, useRef, useEffect } from 'react';
import { simulateRAGQuery, type RAGCitation } from '../data/mockData';
import { getPlatformIcon }  from '../lib/platform';
import { Button }           from './ui/Button';
import { Modal }            from './ui/Modal';
import {
  Send, BrainCircuit, BookOpen, User, ArrowRight, Info,
} from 'lucide-react';

interface Message {
  id:         string;
  sender:     'user' | 'ai';
  text:       string;
  timestamp:  Date;
  citations?: RAGCitation[];
}

const SUGGESTIONS = [
  { title: 'Find audio complaints',  query: 'Are there any issues with the video audio or sound quality?' },
  { title: 'Check course pricing',   query: 'What are users asking about the course pricing or early bird discounts?' },
  { title: 'RSC questions',          query: 'Show me questions about React Server Components and error handling' },
  { title: 'Viewer requests',        query: 'What topics are viewers requesting for next videos?' },
];

export const ChatRAGView: React.FC = () => {
  const [messages,         setMessages]         = useState<Message[]>([{
    id: 'welcome', sender: 'ai', timestamp: new Date(),
    text: "Hi! I'm **PulseGPT**, your audience intelligence assistant. I can run semantic searches over all connected social media comments.\n\nAsk me about viewer requests, bug reports, pricing feedback, or audio quality.",
  }]);
  const [inputVal,         setInputVal]         = useState('');
  const [isTyping,         setIsTyping]         = useState(false);
  const [selectedCitation, setSelectedCitation] = useState<RAGCitation | null>(null);
  const messagesEndRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages, isTyping]);

  const handleSend = async (text: string) => {
    if (!text.trim() || isTyping) return;
    const userMsg: Message = { id: `u-${Date.now()}`, sender: 'user', text, timestamp: new Date() };
    setMessages(p => [...p, userMsg]);
    setInputVal('');
    setIsTyping(true);
    try {
      const res = await simulateRAGQuery(text);
      setMessages(p => [...p, {
        id: `ai-${Date.now()}`, sender: 'ai', timestamp: new Date(),
        text: res.answer, citations: res.citations,
      }]);
    } catch {/* ignore */} finally {
      setIsTyping(false);
    }
  };

  const renderText = (text: string, citations?: RAGCitation[]) =>
    text.split('\n').map((line, li) => {
      const boldRegex = /\*\*(.*?)\*\*/g;
      const citRegex  = /\[(\d+)\]/g;
      const parts: React.ReactNode[] = [];
      let boldMatch; let last = 0;

      const parseCitations = (part: string, key: string) => {
        const sub: React.ReactNode[] = [];
        let cm; let cl = 0;
        while ((cm = citRegex.exec(part)) !== null) {
          const idx = parseInt(cm[1]);
          if (cm.index > cl) sub.push(part.slice(cl, cm.index));
          if (citations && idx <= citations.length) {
            const cit = citations[idx - 1];
            sub.push(
              <button key={`${key}-c${idx}`} onClick={() => setSelectedCitation(cit)}
                className="mx-0.5 px-1.5 py-0.5 bg-accent-soft border border-accent/20 text-accent-ink rounded text-[10px] font-bold hover:bg-accent-soft/80 transition-colors">
                [{idx}]
              </button>
            );
          } else sub.push(`[${idx}]`);
          cl = citRegex.lastIndex;
        }
        sub.push(part.slice(cl));
        return sub;
      };

      while ((boldMatch = boldRegex.exec(line)) !== null) {
        if (boldMatch.index > last) parts.push(...parseCitations(line.slice(last, boldMatch.index), `${li}-b${last}`));
        parts.push(<strong key={`${li}-b${boldMatch.index}`} className="font-semibold text-ink">{boldMatch[1]}</strong>);
        last = boldRegex.lastIndex;
      }
      parts.push(...parseCitations(line.slice(last), `${li}-end`));

      return <p key={li} className={line.trim() === '' ? 'h-3' : 'mb-1.5 leading-relaxed'}>{parts}</p>;
    });

  return (
    <div className="flex flex-col h-[calc(100vh-112px)] space-y-4 animate-slide-up">

      {/* Page header */}
      <div>
        <h2 className="text-[22px] font-semibold text-ink">Ask PulseGPT</h2>
        <p className="text-sm text-ink-3 mt-0.5">Search your comment knowledge base using natural language.</p>
      </div>

      {/* Main layout */}
      <div className="flex-1 grid grid-cols-1 lg:grid-cols-4 gap-4 min-h-0 overflow-hidden">

        {/* Suggestions sidebar */}
        <div className="panel p-4 hidden lg:flex flex-col justify-between overflow-hidden">
          <div className="space-y-4">
            <div className="flex items-center gap-1.5 text-xs font-semibold text-accent uppercase tracking-wide">
              <BookOpen size={13} /> Quick exploration
            </div>
            <p className="text-xs text-ink-3 leading-relaxed">
              These queries search your simulated comment vector index and return contextually matched citations.
            </p>
            <div className="space-y-2">
              {SUGGESTIONS.map(s => (
                <button
                  key={s.title}
                  onClick={() => handleSend(s.query)}
                  className="w-full text-left p-3 rounded-lg border border-border hover:bg-surface-2 hover:border-border-strong transition-colors flex items-start justify-between gap-2 group"
                >
                  <span className="text-sm text-ink-2 group-hover:text-ink leading-snug">{s.title}</span>
                  <ArrowRight size={13} className="text-ink-3 group-hover:text-accent transition-colors flex-shrink-0 mt-0.5" />
                </button>
              ))}
            </div>
          </div>

          {/* RAG info box */}
          <div className="mt-4 p-3 rounded-lg bg-accent-soft border border-accent/15 flex gap-2">
            <Info size={14} className="text-accent mt-0.5 flex-shrink-0" />
            <div>
              <p className="text-xs font-semibold text-accent-ink uppercase tracking-wide mb-0.5">RAG pipeline active</p>
              <p className="text-[11px] text-ink-3 leading-normal">
                Query → Vector matching → Context extraction → Synthesized report
              </p>
            </div>
          </div>
        </div>

        {/* Chat console */}
        <div className="panel flex flex-col overflow-hidden lg:col-span-3">

          {/* Messages */}
          <div className="flex-1 overflow-y-auto p-4 space-y-4 bg-surface-2/30">
            {messages.map(msg => {
              const isAi = msg.sender === 'ai';
              return (
                <div key={msg.id} className={`flex gap-3 ${isAi ? 'justify-start' : 'justify-end'}`}>
                  {isAi && (
                    <div className="h-8 w-8 rounded-lg bg-accent-soft border border-accent/20 text-accent flex items-center justify-center flex-shrink-0">
                      <BrainCircuit size={16} />
                    </div>
                  )}
                  <div className={`max-w-[80%] rounded-xl p-4 text-sm leading-relaxed border ${
                    isAi
                      ? 'bg-surface border-border text-ink-2 rounded-tl-sm'
                      : 'bg-accent text-white border-accent rounded-tr-sm shadow-sm'
                  }`}>
                    <div>{isAi ? renderText(msg.text, msg.citations) : msg.text}</div>

                    {/* Citations */}
                    {isAi && msg.citations && msg.citations.length > 0 && (
                      <div className="mt-3 pt-3 border-t border-border flex flex-wrap gap-1.5 items-center">
                        <span className="text-[10px] font-semibold text-ink-3 uppercase tracking-wide mr-1">Sources:</span>
                        {msg.citations.map((cit, i) => (
                          <button
                            key={cit.id}
                            onClick={() => setSelectedCitation(cit)}
                            className="flex items-center gap-1 px-2 py-0.5 bg-surface-2 hover:bg-surface border border-border text-xs text-ink-2 font-medium rounded-md transition-colors"
                          >
                            <span className="text-accent font-bold">[{i + 1}]</span>
                            {getPlatformIcon(cit.platform, 11)}
                            <span>{cit.author}</span>
                          </button>
                        ))}
                      </div>
                    )}
                  </div>
                  {!isAi && (
                    <div className="h-8 w-8 rounded-lg bg-surface-2 border border-border text-ink-3 flex items-center justify-center flex-shrink-0">
                      <User size={15} />
                    </div>
                  )}
                </div>
              );
            })}

            {/* Typing indicator */}
            {isTyping && (
              <div className="flex gap-3 justify-start">
                <div className="h-8 w-8 rounded-lg bg-accent-soft border border-accent/20 text-accent flex items-center justify-center flex-shrink-0">
                  <BrainCircuit size={16} />
                </div>
                <div className="bg-surface border border-border rounded-xl rounded-tl-sm px-4 py-3 flex items-center gap-2 text-sm text-ink-3">
                  Searching comment index…
                  <div className="flex gap-1">
                    {[0, 150, 300].map(delay => (
                      <span key={delay} className="h-1.5 w-1.5 bg-accent rounded-full animate-bounce" style={{ animationDelay: `${delay}ms` }} />
                    ))}
                  </div>
                </div>
              </div>
            )}
            <div ref={messagesEndRef} />
          </div>

          {/* Input */}
          <div className="p-3 border-t border-border flex-shrink-0">
            <form
              onSubmit={e => { e.preventDefault(); handleSend(inputVal); }}
              className="flex items-center gap-2 bg-surface-2 border border-border rounded-lg px-3 py-2 focus-within:border-accent focus-within:ring-2 focus-within:ring-accent/10 transition-colors"
            >
              <input
                disabled={isTyping}
                type="text"
                placeholder="Ask PulseGPT about your audience…"
                value={inputVal}
                onChange={e => setInputVal(e.target.value)}
                className="flex-1 bg-transparent border-0 outline-none text-sm text-ink placeholder-ink-3"
              />
              <Button type="submit" variant="primary" size="sm" disabled={!inputVal.trim() || isTyping} icon={<Send size={13} />}>
                Send
              </Button>
            </form>
          </div>
        </div>
      </div>

      {/* Citation modal */}
      <Modal
        open={!!selectedCitation}
        onClose={() => setSelectedCitation(null)}
        title="Source citation"
        size="md"
      >
        {selectedCitation && (
          <div className="space-y-4">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                <span className="text-sm font-semibold text-ink">{selectedCitation.author}</span>
                {getPlatformIcon(selectedCitation.platform, 14)}
              </div>
              <span className="text-xs text-ink-3 truncate max-w-[200px]">
                "{selectedCitation.postTitle}"
              </span>
            </div>
            <div className="bg-surface-2 border border-border rounded-lg p-4 text-sm text-ink-2 italic leading-relaxed">
              "{selectedCitation.text}"
            </div>
            <div className="flex gap-2 p-3 rounded-lg bg-accent-soft border border-accent/15 text-xs text-ink-3">
              <Info size={13} className="text-accent flex-shrink-0 mt-0.5" />
              <span>Retrieved via vector similarity matching from the indexed comments database.</span>
            </div>
          </div>
        )}
      </Modal>
    </div>
  );
};
