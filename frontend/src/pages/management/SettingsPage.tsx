import React, { useState } from 'react';
import { Button } from '../../components/ui/Button';
import { Input } from '../../components/ui/Input';
import { useAuth } from '../../context/AuthContext';
import { useToast } from '../../components/ui/Toast';
import {
  Settings, Key, Bell, User, Save,
} from 'lucide-react';

export const SettingsPage: React.FC = () => {
  const { user } = useAuth();
  const { addToast } = useToast();

  const [activeTab, setActiveTab] = useState<'general' | 'api' | 'notifications'>('general');
  const [formData, setFormData] = useState({
    name: user?.name || 'Sarah Chen',
    email: user?.email || 'sarah.chen@creator.io',
    company: user?.company || 'Chen Media & Tech',
    openAiKey: 'sk-proj-••••••••••••••••••••••••••••••••',
    geminiKey: 'AIzaSy••••••••••••••••••••••••••••••',
    emailAlerts: true,
    slackWebhook: 'https://hooks.slack.com/services/T00/B00/XXXX',
  });

  const handleSave = (e: React.FormEvent) => {
    e.preventDefault();
    addToast({
      title: 'Settings Saved',
      body: 'Your workspace preferences have been updated successfully.',
      variant: 'success',
    });
  };

  return (
    <div className="space-y-6 max-w-4xl">
      {/* Header */}
      <div>
        <h2 className="text-xl font-bold text-slate-900 tracking-tight flex items-center gap-2">
          <Settings className="w-5 h-5 text-indigo-600" />
          Workspace & Platform Settings
        </h2>
        <p className="text-xs text-slate-500 mt-0.5">
          Manage creator profile, LLM API keys, and notification triggers.
        </p>
      </div>

      {/* Settings Navigation Tabs */}
      <div className="flex items-center gap-2 border-b border-slate-200 pb-2">
        <button
          onClick={() => setActiveTab('general')}
          className={`px-3 py-1.5 rounded-lg text-xs font-semibold transition-colors flex items-center gap-1.5 ${
            activeTab === 'general'
              ? 'bg-indigo-50 text-indigo-700'
              : 'text-slate-600 hover:text-slate-900 hover:bg-slate-100'
          }`}
        >
          <User className="w-3.5 h-3.5" />
          Profile & Account
        </button>
        <button
          onClick={() => setActiveTab('api')}
          className={`px-3 py-1.5 rounded-lg text-xs font-semibold transition-colors flex items-center gap-1.5 ${
            activeTab === 'api'
              ? 'bg-indigo-50 text-indigo-700'
              : 'text-slate-600 hover:text-slate-900 hover:bg-slate-100'
          }`}
        >
          <Key className="w-3.5 h-3.5" />
          LLM & API Keys
        </button>
        <button
          onClick={() => setActiveTab('notifications')}
          className={`px-3 py-1.5 rounded-lg text-xs font-semibold transition-colors flex items-center gap-1.5 ${
            activeTab === 'notifications'
              ? 'bg-indigo-50 text-indigo-700'
              : 'text-slate-600 hover:text-slate-900 hover:bg-slate-100'
          }`}
        >
          <Bell className="w-3.5 h-3.5" />
          Alerts & Webhooks
        </button>
      </div>

      {/* Settings Form Container */}
      <div className="bg-white rounded-xl border border-slate-200 p-6 shadow-sm">
        <form onSubmit={handleSave} className="space-y-5">
          {activeTab === 'general' && (
            <div className="space-y-4">
              <h3 className="text-sm font-bold text-slate-900">Personal Information</h3>
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div>
                  <label className="block text-xs font-semibold text-ink mb-1.5">Display Name</label>
                  <Input
                    value={formData.name}
                    onChange={e => setFormData({ ...formData, name: e.target.value })}
                    required
                  />
                </div>
                <div>
                  <label className="block text-xs font-semibold text-ink mb-1.5">Email Address</label>
                  <Input
                    type="email"
                    value={formData.email}
                    onChange={e => setFormData({ ...formData, email: e.target.value })}
                    required
                  />
                </div>
              </div>
              <div>
                <label className="block text-xs font-semibold text-ink mb-1.5">Studio / Brand Name</label>
                <Input
                  value={formData.company}
                  onChange={e => setFormData({ ...formData, company: e.target.value })}
                />
              </div>
            </div>
          )}

          {activeTab === 'api' && (
            <div className="space-y-4">
              <h3 className="text-sm font-bold text-slate-900">External Model API Credentials</h3>
              <p className="text-xs text-slate-500">
                PulseGPT uses your custom API keys for real-time vector embeddings and semantic search.
              </p>
              <div>
                <label className="block text-xs font-semibold text-ink mb-1">OpenAI API Key</label>
                <Input
                  type="password"
                  value={formData.openAiKey}
                  onChange={e => setFormData({ ...formData, openAiKey: e.target.value })}
                />
                <span className="text-[11px] text-ink-3 mt-1 block">Used for text-embedding-3-small & GPT-4o synthesis</span>
              </div>
              <div>
                <label className="block text-xs font-semibold text-ink mb-1">Google Gemini API Key</label>
                <Input
                  type="password"
                  value={formData.geminiKey}
                  onChange={e => setFormData({ ...formData, geminiKey: e.target.value })}
                />
                <span className="text-[11px] text-ink-3 mt-1 block">Used for multimodal video comment analysis</span>
              </div>
            </div>
          )}

          {activeTab === 'notifications' && (
            <div className="space-y-4">
              <h3 className="text-sm font-bold text-slate-900">Notification Preferences</h3>
              <label className="flex items-center gap-3 cursor-pointer p-3 rounded-lg border border-slate-200">
                <input
                  type="checkbox"
                  checked={formData.emailAlerts}
                  onChange={e => setFormData({ ...formData, emailAlerts: e.target.checked })}
                  className="w-4 h-4 text-indigo-600 rounded"
                />
                <div>
                  <span className="text-xs font-bold text-slate-800 block">
                    High Priority Incident Email Alerts
                  </span>
                  <span className="text-xs text-slate-500">
                    Receive immediate notifications when negative sentiment spikes over 20%.
                  </span>
                </div>
              </label>

              <div>
                <label className="block text-xs font-semibold text-ink mb-1.5">Slack Incoming Webhook URL</label>
                <Input
                  value={formData.slackWebhook}
                  onChange={e => setFormData({ ...formData, slackWebhook: e.target.value })}
                  placeholder="https://hooks.slack.com/services/..."
                />
              </div>
            </div>
          )}

          <div className="pt-4 border-t border-slate-100 flex justify-end">
            <Button type="submit" variant="primary" size="md">
              <Save className="w-4 h-4 mr-2" />
              Save Changes
            </Button>
          </div>
        </form>
      </div>
    </div>
  );
};
