import React, { useState } from 'react';
import { type ConnectedAccount } from '../data/mockData';
import { getPlatformIcon, platformColor } from '../lib/platform';
import { Button }       from './ui/Button';
import { StatusBadge }  from './ui/Badge';
import { Avatar }       from './ui/Avatar';
import { Modal }        from './ui/Modal';
import { ProgressBar }  from './ui/ProgressBar';
import {
  Check, Link2, Link2Off, ArrowRight, ShieldCheck, Database, Lock, Plus,
} from 'lucide-react';

interface PlatformInfo {
  id:     string;
  name:   string;
  desc:   string;
  scopes: string[];
}

const DEFAULT_PLATFORMS: PlatformInfo[] = [
  { id: 'youtube',   name: 'YouTube',   desc: 'Sync comments from your channel videos & community posts.', scopes: ['read_comments', 'manage_replies'] },
  { id: 'instagram', name: 'Instagram', desc: 'Ingest comments and direct replies on reels & photo posts.', scopes: ['read_comments', 'manage_replies'] },
  { id: 'facebook',  name: 'Facebook',  desc: 'Monitor page post comments and video responses.', scopes: ['read_comments', 'manage_replies'] },
  { id: 'linkedin',  name: 'LinkedIn',  desc: 'Sync professional comments and engagement analytics.', scopes: ['read_comments', 'manage_replies'] },
  { id: 'twitter',   name: 'X (Twitter)', desc: 'Track mentions, thread replies and audience sentiment.', scopes: ['read_tweets', 'manage_replies'] },
  { id: 'reddit',    name: 'Reddit',    desc: 'Monitor subreddit threads and community feedback.', scopes: ['read_posts', 'submit_comments'] },
];

interface IntegrationsViewProps {
  accounts:             ConnectedAccount[];
  platformsList?:       PlatformInfo[];
  setPlatformsList?:    React.Dispatch<React.SetStateAction<PlatformInfo[]>>;
  customDetailsMap?:    Record<string, { name: string; handle: string; followerCount: number; postsCount: number }>;
  setCustomDetailsMap?: React.Dispatch<React.SetStateAction<Record<string, { name: string; handle: string; followerCount: number; postsCount: number }>>>;
  onConnect:            (platform: string, details?: { name: string; handle: string; followerCount: number; postsCount: number }) => void;
  onDisconnect:         (platform: string) => void;
}

export const IntegrationsView: React.FC<IntegrationsViewProps> = ({
  accounts,
  platformsList: externalPlatforms,
  setPlatformsList: externalSetPlatforms,
  customDetailsMap: externalDetails,
  setCustomDetailsMap: externalSetDetails,
  onConnect,
  onDisconnect,
}) => {
  const [internalPlatforms, setInternalPlatforms] = useState<PlatformInfo[]>(DEFAULT_PLATFORMS);
  const [internalDetails, setInternalDetails] = useState<Record<string, { name: string; handle: string; followerCount: number; postsCount: number }>>({});

  const platformsList = externalPlatforms || internalPlatforms;
  const setPlatformsList = externalSetPlatforms || setInternalPlatforms;
  const customDetailsMap = externalDetails || internalDetails;
  const setCustomDetailsMap = externalSetDetails || setInternalDetails;
  const [oauthOpen,      setOauthOpen]      = useState(false);
  const [modalPlatform,  setModalPlatform]  = useState<string | null>(null);
  const [oauthStep,      setOauthStep]      = useState<1|2|3|4>(1);
  const [syncProgress,   setSyncProgress]   = useState(0);
  const [addCustomOpen,  setAddCustomOpen]  = useState(false);

  // Custom platform form
  const [customName,      setCustomName]      = useState('');
  const [customDesc,      setCustomDesc]      = useState('');
  const [customHandle,    setCustomHandle]    = useState('');
  const [customCreator,   setCustomCreator]   = useState('');
  const [customFollowers, setCustomFollowers] = useState('12,500');
  const [customPosts,     setCustomPosts]     = useState('42');

  const getPlatformName = (id: string) => platformsList.find(p => p.id === id)?.name ?? id;

  const startOauth = (platform: string) => {
    setModalPlatform(platform);
    setOauthStep(1);
    setSyncProgress(0);
    setOauthOpen(true);
  };

  const handlePermissionsAccept = () => {
    setOauthStep(2);
    setTimeout(() => {
      setOauthStep(3);
      const interval = setInterval(() => {
        setSyncProgress(prev => {
          if (prev >= 100) {
            clearInterval(interval);
            setTimeout(() => {
              setOauthStep(4);
              if (modalPlatform) onConnect(modalPlatform, customDetailsMap[modalPlatform]);
            }, 600);
            return 100;
          }
          return prev + 10;
        });
      }, 150);
    }, 1500);
  };

  const closeOauth = () => {
    setOauthOpen(false);
    setModalPlatform(null);
    setOauthStep(1);
    setSyncProgress(0);
  };

  const handleCustomSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!customName.trim()) return;
    const id = customName.toLowerCase().replace(/[^a-z0-9]/g, '-') || `platform-${Date.now()}`;
    setPlatformsList(prev => [...prev, {
      id, name: customName,
      desc: customDesc || `Access your ${customName} comments and analytics.`,
      scopes: ['read_comments', 'manage_replies'],
    }]);
    setCustomDetailsMap(prev => ({ ...prev, [id]: {
      name: customCreator || `${customName} Creator`,
      handle: customHandle.startsWith('@') ? customHandle : `@${customHandle}`,
      followerCount: parseInt(customFollowers.replace(/,/g, '')) || 0,
      postsCount:    parseInt(customPosts.replace(/,/g, ''))     || 0,
    }}));
    setCustomName(''); setCustomDesc(''); setCustomHandle('');
    setCustomCreator(''); setCustomFollowers('12,500'); setCustomPosts('42');
    setAddCustomOpen(false);
  };

  return (
    <div className="space-y-6 animate-slide-up">

      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
        <div>
          <h2 className="text-[22px] font-semibold text-ink">Platform integrations</h2>
          <p className="text-sm text-ink-3 mt-0.5">Connect your social accounts to pull comments and trigger AI analysis.</p>
        </div>
        <Button variant="secondary" size="sm" icon={<Plus size={14} />} onClick={() => setAddCustomOpen(true)}>
          Add custom platform
        </Button>
      </div>

      {/* Platform grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-4">
        {platformsList.map(platform => {
          const account     = accounts.find(a => a.platform === platform.id);
          const isConnected = !!account && account.status === 'connected';
          return (
            <div key={platform.id} className={`panel p-5 flex flex-col gap-4 transition-all ${
              isConnected ? 'border-success-border' : ''
            }`}>
              {/* Platform identity */}
              <div className="flex items-start justify-between">
                <div className="h-10 w-10 rounded-xl bg-surface-2 border border-border flex items-center justify-center">
                  {getPlatformIcon(platform.id, 22)}
                </div>
                <StatusBadge status={isConnected ? 'connected' : 'disconnected'} />
              </div>

              <div>
                <h3 className="text-base font-semibold text-ink">{platform.name}</h3>
                <p className="text-sm text-ink-3 mt-0.5 leading-relaxed">{platform.desc}</p>
              </div>

              {/* Connected account details */}
              {isConnected && account ? (
                <div className="bg-surface-2 rounded-lg border border-border p-3 space-y-2.5 animate-slide-up">
                  <div className="flex items-center gap-2.5">
                    <Avatar src={account.avatar} name={account.name} size="sm" />
                    <div>
                      <p className="text-sm font-semibold text-ink">{account.name}</p>
                      <p className="text-xs text-ink-3">{account.handle}</p>
                    </div>
                  </div>
                  <div className="grid grid-cols-2 gap-2 pt-2 border-t border-border text-center">
                    <div>
                      <span className="block text-[10px] text-ink-3 font-medium uppercase tracking-wide">Followers</span>
                      <span className="text-sm font-semibold text-ink">{account.followerCount.toLocaleString()}</span>
                    </div>
                    <div>
                      <span className="block text-[10px] text-ink-3 font-medium uppercase tracking-wide">Posts</span>
                      <span className="text-sm font-semibold text-ink">{account.postsCount}</span>
                    </div>
                  </div>
                </div>
              ) : (
                <div className="flex flex-col items-center justify-center py-6 rounded-lg border border-dashed border-border text-ink-3 gap-1">
                  <Lock size={18} />
                  <span className="text-xs">No connection</span>
                </div>
              )}

              {/* Action button */}
              {isConnected ? (
                <Button
                  variant="ghost"
                  size="sm"
                  icon={<Link2Off size={13} />}
                  className="w-full text-danger hover:bg-danger-soft hover:text-danger border-danger-border"
                  onClick={() => onDisconnect(platform.id)}
                >
                  Disconnect
                </Button>
              ) : (
                <button
                  onClick={() => startOauth(platform.id)}
                  className="w-full h-9 px-4 rounded-md text-sm font-semibold text-white flex items-center justify-center gap-2 transition-all hover:opacity-90 active:scale-[0.98]"
                  style={{ background: platformColor(platform.id) }}
                >
                  <Link2 size={13} /> Connect {platform.name}
                </button>
              )}
            </div>
          );
        })}

        {/* Add custom card */}
        <button
          onClick={() => setAddCustomOpen(true)}
          className="panel flex flex-col items-center justify-center p-8 border-dashed border-border-strong hover:border-accent hover:bg-accent-soft/30 transition-all cursor-pointer group min-h-[280px]"
        >
          <div className="h-12 w-12 rounded-full bg-surface-2 border border-border group-hover:border-accent group-hover:bg-accent-soft flex items-center justify-center text-ink-3 group-hover:text-accent transition-all mb-3">
            <Plus size={22} />
          </div>
          <h3 className="text-sm font-semibold text-ink mb-1">Add custom platform</h3>
          <p className="text-xs text-ink-3 text-center max-w-[160px]">
            Connect LinkedIn, TikTok, Twitter/X, or any other platform
          </p>
        </button>
      </div>

      {/* Security notice */}
      <div className="panel p-4 flex gap-3">
        <div className="h-9 w-9 rounded-lg bg-success-soft border border-success-border flex items-center justify-center flex-shrink-0">
          <ShieldCheck size={18} className="text-success" />
        </div>
        <div>
          <h4 className="text-sm font-semibold text-ink">OAuth security &amp; rate management</h4>
          <p className="text-sm text-ink-3 mt-0.5 leading-relaxed">
            PulseGPT uses secure standard API handshakes. All social media API keys and tokens are stored
            using client-side encrypted environments. We request minimal scopes for comment indexing and reply publishing.
          </p>
        </div>
      </div>

      {/* OAuth simulation modal */}
      <Modal
        open={oauthOpen && !!modalPlatform}
        onClose={closeOauth}
        size="sm"
        title={`Connect ${getPlatformName(modalPlatform ?? '')}`}
      >
        {/* Step 1: Permissions */}
        {oauthStep === 1 && (
          <div className="space-y-4">
            <p className="text-sm text-ink-2">
              <strong className="text-ink">PulseGPT</strong> is requesting permission to access your{' '}
              {getPlatformName(modalPlatform ?? '')} account:
            </p>
            <ul className="space-y-2 text-sm text-ink-2">
              {[
                'Read comments and reactions from your posts.',
                'Publish replies on your behalf when triggered.',
                'Access public profile and demographic fields.',
              ].map((item, i) => (
                <li key={i} className="flex items-start gap-2">
                  <span className="h-1.5 w-1.5 rounded-full bg-accent mt-2 flex-shrink-0" />
                  {item}
                </li>
              ))}
            </ul>
            <div className="flex gap-2 pt-2">
              <Button variant="secondary" size="sm" className="flex-1" onClick={closeOauth}>Cancel</Button>
              <Button variant="primary" size="sm" className="flex-1" iconRight={<ArrowRight size={13} />} onClick={handlePermissionsAccept}>
                Grant access
              </Button>
            </div>
          </div>
        )}

        {/* Step 2: Authenticating */}
        {oauthStep === 2 && (
          <div className="flex flex-col items-center justify-center py-8 space-y-3 text-center">
            <svg className="animate-spin h-10 w-10 text-accent" viewBox="0 0 24 24" fill="none">
              <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="2.5"/>
              <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"/>
            </svg>
            <div>
              <h4 className="text-sm font-semibold text-ink">Authenticating…</h4>
              <p className="text-xs text-ink-3 mt-0.5">Exchanging security tokens with {getPlatformName(modalPlatform ?? '')}…</p>
            </div>
          </div>
        )}

        {/* Step 3: Syncing */}
        {oauthStep === 3 && (
          <div className="py-6 space-y-4">
            <div className="flex items-center justify-between text-xs text-ink-3 mb-1">
              <div className="flex items-center gap-1.5"><Database size={12} className="text-accent" /> Indexing comments</div>
              <span className="font-semibold text-accent">{syncProgress}%</span>
            </div>
            <ProgressBar value={syncProgress} color="accent" size="md" />
            <p className="text-xs text-ink-3 text-center animate-pulse">
              Pulling posts, extracting profiles, and classifying sentiments…
            </p>
          </div>
        )}

        {/* Step 4: Success */}
        {oauthStep === 4 && (
          <div className="flex flex-col items-center justify-center py-8 space-y-4 text-center animate-slide-up">
            <div className="h-14 w-14 rounded-full bg-success-soft border border-success-border flex items-center justify-center">
              <Check size={26} className="text-success" />
            </div>
            <div>
              <h4 className="text-base font-semibold text-ink">Account linked!</h4>
              <p className="text-sm text-ink-3 mt-0.5">
                Your {getPlatformName(modalPlatform ?? '')} data is now synced and analysed by PulseGPT.
              </p>
            </div>
            <Button variant="secondary" size="sm" className="w-full" onClick={closeOauth}>Done</Button>
          </div>
        )}
      </Modal>

      {/* Add custom platform modal */}
      <Modal
        open={addCustomOpen}
        onClose={() => setAddCustomOpen(false)}
        title="Add custom integration"
        size="md"
        footer={
          <>
            <Button variant="secondary" size="sm" onClick={() => setAddCustomOpen(false)}>Cancel</Button>
            <Button variant="primary" size="sm" form="custom-form" type="submit">Create integration</Button>
          </>
        }
      >
        <form id="custom-form" onSubmit={handleCustomSubmit} className="space-y-4">
          <div className="space-y-1.5">
            <label className="text-xs font-semibold text-ink-3 uppercase tracking-wide">
              Platform name <span className="text-danger">*</span>
            </label>
            <input required className="form-input w-full px-3 py-2 text-sm rounded-md"
              placeholder="e.g. LinkedIn, TikTok, Twitter" value={customName}
              onChange={e => setCustomName(e.target.value)} />
          </div>
          <div className="grid grid-cols-2 gap-3">
            <div className="space-y-1.5">
              <label className="text-xs font-semibold text-ink-3 uppercase tracking-wide">Creator / page name <span className="text-danger">*</span></label>
              <input required className="form-input w-full px-3 py-2 text-sm rounded-md"
                placeholder="e.g. TechPulse Official" value={customCreator}
                onChange={e => setCustomCreator(e.target.value)} />
            </div>
            <div className="space-y-1.5">
              <label className="text-xs font-semibold text-ink-3 uppercase tracking-wide">Account handle <span className="text-danger">*</span></label>
              <input required className="form-input w-full px-3 py-2 text-sm rounded-md"
                placeholder="@techpulse" value={customHandle}
                onChange={e => setCustomHandle(e.target.value)} />
            </div>
          </div>
          <div className="grid grid-cols-2 gap-3">
            <div className="space-y-1.5">
              <label className="text-xs font-semibold text-ink-3 uppercase tracking-wide">Followers <span className="text-danger">*</span></label>
              <input required className="form-input w-full px-3 py-2 text-sm rounded-md"
                placeholder="5,000" value={customFollowers}
                onChange={e => setCustomFollowers(e.target.value)} />
            </div>
            <div className="space-y-1.5">
              <label className="text-xs font-semibold text-ink-3 uppercase tracking-wide">Posts <span className="text-danger">*</span></label>
              <input required className="form-input w-full px-3 py-2 text-sm rounded-md"
                placeholder="42" value={customPosts}
                onChange={e => setCustomPosts(e.target.value)} />
            </div>
          </div>
          <div className="space-y-1.5">
            <label className="text-xs font-semibold text-ink-3 uppercase tracking-wide">Description</label>
            <textarea rows={2} className="form-input w-full px-3 py-2 text-sm rounded-md resize-none"
              placeholder="Access your corporate articles, followers, and engagement metrics."
              value={customDesc} onChange={e => setCustomDesc(e.target.value)} />
          </div>
        </form>
      </Modal>
    </div>
  );
};
