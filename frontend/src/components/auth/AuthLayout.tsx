import React from 'react';
import { Activity, Sparkles, ShieldCheck, Zap, MessageSquareQuote } from 'lucide-react';

interface AuthLayoutProps {
  children: React.ReactNode;
  title: string;
  subtitle: string;
}

export const AuthLayout: React.FC<AuthLayoutProps> = ({ children, title, subtitle }) => {
  return (
    <div className="min-h-screen flex bg-slate-50">
      {/* Left side banner & value proposition (Desktop only) */}
      <div className="hidden lg:flex lg:w-1/2 bg-slate-900 text-white p-12 flex-col justify-between relative overflow-hidden">
        {/* Subtle background glow decorative elements */}
        <div className="absolute -top-32 -left-32 w-96 h-96 bg-indigo-600/20 rounded-full blur-3xl pointer-events-none" />
        <div className="absolute -bottom-32 -right-32 w-96 h-96 bg-purple-600/20 rounded-full blur-3xl pointer-events-none" />

        {/* Logo and Brand */}
        <div className="relative z-10 flex items-center gap-3">
          <div className="w-10 h-10 rounded-xl bg-gradient-to-tr from-indigo-500 to-indigo-400 flex items-center justify-center text-white shadow-lg shadow-indigo-500/30">
            <Activity className="w-5 h-5" />
          </div>
          <div>
            <h1 className="font-bold text-xl tracking-tight text-white flex items-center gap-2">
              Pulse<span className="text-indigo-400 font-black">GPT</span>
            </h1>
            <p className="text-xs text-slate-400 font-medium tracking-wide uppercase">
              Audience Intelligence & RAG
            </p>
          </div>
        </div>

        {/* Value Prop Center Cards */}
        <div className="relative z-10 space-y-6 my-auto max-w-md">
          <div className="inline-flex items-center gap-2 px-3 py-1.5 rounded-full bg-indigo-500/10 border border-indigo-500/20 text-indigo-300 text-xs font-semibold">
            <Sparkles className="w-3.5 h-3.5 text-indigo-400" />
            Next-Gen Audience RAG & Planning
          </div>

          <h2 className="text-3xl font-extrabold tracking-tight text-white leading-tight">
            Turn social comment chaos into actionable viral content.
          </h2>

          <p className="text-slate-400 text-sm leading-relaxed">
            Unify comments across YouTube, Instagram, LinkedIn & Reddit. Query audience intent with high-precision RAG, detect breakout topics, and plan high-impact posts with guardrail evaluation.
          </p>

          <div className="grid grid-cols-2 gap-4 pt-4 border-t border-slate-800">
            <div className="flex items-start gap-3">
              <div className="p-2 rounded-lg bg-slate-800 text-indigo-400 mt-0.5">
                <Zap className="w-4 h-4" />
              </div>
              <div>
                <h4 className="text-sm font-semibold text-slate-200">Real-time Ingestion</h4>
                <p className="text-xs text-slate-400 mt-0.5">Sub-second multi-platform sentiment tagging</p>
              </div>
            </div>

            <div className="flex items-start gap-3">
              <div className="p-2 rounded-lg bg-slate-800 text-indigo-400 mt-0.5">
                <ShieldCheck className="w-4 h-4" />
              </div>
              <div>
                <h4 className="text-sm font-semibold text-slate-200">Hallucination Safe</h4>
                <p className="text-xs text-slate-400 mt-0.5">Strict source citation & confidence checks</p>
              </div>
            </div>
          </div>
        </div>

        {/* Testimonial Quote */}
        <div className="relative z-10 p-4 rounded-xl bg-slate-800/60 border border-slate-800 backdrop-blur-sm">
          <div className="flex items-center gap-2 text-indigo-400 mb-2">
            <MessageSquareQuote className="w-4 h-4" />
            <span className="text-xs font-semibold uppercase tracking-wider">Creator Feedback</span>
          </div>
          <p className="text-xs text-slate-300 italic">
            "PulseGPT allowed us to double our newsletter conversion by directly addressing recurring questions buried in thousands of YouTube comments."
          </p>
          <div className="mt-3 flex items-center justify-between text-xs text-slate-400">
            <span className="font-semibold text-slate-200">Sarah Chen</span>
            <span>Head of Growth, DevStudio</span>
          </div>
        </div>
      </div>

      {/* Right side form container */}
      <div className="w-full lg:w-1/2 flex items-center justify-center p-6 sm:p-12">
        <div className="w-full max-w-md space-y-6">
          {/* Mobile brand header */}
          <div className="lg:hidden flex items-center gap-3 mb-6">
            <div className="w-9 h-9 rounded-lg bg-indigo-600 flex items-center justify-center text-white">
              <Activity className="w-5 h-5" />
            </div>
            <span className="font-bold text-lg text-slate-900">PulseGPT</span>
          </div>

          <div>
            <h2 className="text-2xl font-bold tracking-tight text-slate-900">{title}</h2>
            <p className="text-sm text-slate-500 mt-1">{subtitle}</p>
          </div>

          {children}
        </div>
      </div>
    </div>
  );
};
