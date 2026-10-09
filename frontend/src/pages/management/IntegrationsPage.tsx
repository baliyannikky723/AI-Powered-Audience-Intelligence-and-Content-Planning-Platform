import React from 'react';
import { useIntegrations, useConnectAccountMutation, useDisconnectAccountMutation } from '../../hooks/useApi';
import { IntegrationsView } from '../../components/IntegrationsView';
import { useToast } from '../../components/ui/Toast';

export const IntegrationsPage: React.FC = () => {
  const { data: accounts, isLoading } = useIntegrations();
  const connectMutation = useConnectAccountMutation();
  const disconnectMutation = useDisconnectAccountMutation();
  const { addToast } = useToast();

  const handleConnect = (
    platform: string,
    details?: { name: string; handle: string; followerCount: number; postsCount: number }
  ) => {
    const handle = details?.handle || `@${platform}_creator`;
    connectMutation.mutate(
      { platform, handle },
      {
        onSuccess: () => {
          addToast({
            title: 'Account Connected',
            body: `Successfully linked ${platform} account ${handle}`,
            variant: 'success',
          });
        },
        onError: () => {
          addToast({
            title: 'Connection Failed',
            body: 'Could not complete OAuth handshake.',
            variant: 'danger',
          });
        },
      }
    );
  };

  const handleDisconnect = (platform: string) => {
    disconnectMutation.mutate(platform, {
      onSuccess: () => {
        addToast({
          title: 'Account Disconnected',
          body: `Disconnected ${platform} channel.`,
          variant: 'default',
        });
      },
    });
  };

  if (isLoading) {
    return (
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        {[1, 2, 3, 4].map(i => (
          <div key={i} className="h-48 bg-white rounded-xl border border-slate-200 animate-pulse" />
        ))}
      </div>
    );
  }

  return (
    <IntegrationsView
      accounts={accounts || []}
      onConnect={handleConnect}
      onDisconnect={handleDisconnect}
    />
  );
};
