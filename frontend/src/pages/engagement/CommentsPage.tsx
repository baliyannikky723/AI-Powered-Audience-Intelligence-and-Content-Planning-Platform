import React from 'react';
import { useComments, usePostReplyMutation, useIntegrations } from '../../hooks/useApi';
import { CommentsView } from '../../components/CommentsView';
import { AlertCircle } from 'lucide-react';
import { useToast } from '../../components/ui/Toast';

export const CommentsPage: React.FC = () => {
  const { data: comments, isLoading, error } = useComments();
  const { data: accounts } = useIntegrations();
  const postReplyMutation = usePostReplyMutation();
  const { addToast } = useToast();

  const connectedPlatforms = (accounts || [])
    .filter(a => a.status === 'connected')
    .map(a => a.platform);

  const handlePostReply = (commentId: string, replyText: string) => {
    postReplyMutation.mutate(
      { commentId, text: replyText },
      {
        onSuccess: () => {
          addToast({
            title: 'Reply published',
            body: 'Your response has been dispatched to the platform.',
            variant: 'success',
          });
        },
        onError: () => {
          addToast({
            title: 'Failed to post reply',
            body: 'Please check your connection and try again.',
            variant: 'danger',
          });
        },
      }
    );
  };

  if (isLoading) {
    return (
      <div className="space-y-4">
        <div className="h-14 bg-white rounded-xl border border-slate-200 animate-pulse" />
        <div className="space-y-3">
          {[1, 2, 3, 4].map(i => (
            <div key={i} className="h-28 bg-white rounded-xl border border-slate-200 animate-pulse" />
          ))}
        </div>
      </div>
    );
  }

  if (error || !comments) {
    return (
      <div className="p-8 text-center bg-white rounded-xl border border-slate-200">
        <AlertCircle className="w-8 h-8 text-danger mx-auto mb-2" />
        <h3 className="font-bold text-ink">Failed to load comments</h3>
      </div>
    );
  }

  return (
    <CommentsView
      comments={comments}
      connectedPlatforms={connectedPlatforms.length > 0 ? connectedPlatforms : ['youtube', 'instagram', 'facebook']}
      onPostReply={handlePostReply}
    />
  );
};
