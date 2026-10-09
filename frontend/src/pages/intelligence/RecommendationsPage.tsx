import React, { useState } from 'react';
import { useRecommendations, useApproveRecommendation, useCreateCalendarItem, useCalendarSuggestions } from '../../hooks/useApi';
import { Button } from '../../components/ui/Button';
import { Badge } from '../../components/ui/Badge';
import { Modal } from '../../components/ui/Modal';
import { Input } from '../../components/ui/Input';
import { getPlatformIcon } from '../../lib/platform';
import {
  Lightbulb, CheckCircle2, CalendarPlus, AlertTriangle,
  Clock, ShieldCheck, Sparkles, ChevronRight, Layers, Tag
} from 'lucide-react';
import { useToast } from '../../components/ui/Toast';
import type { ContentRecommendation } from '../../types/models';

export const RecommendationsPage: React.FC = () => {
  const { data: recommendations, isLoading } = useRecommendations();
  const approveMutation = useApproveRecommendation();
  const createCalendarItemMutation = useCreateCalendarItem();
  const { addToast } = useToast();

  const [selectedRec, setSelectedRec] = useState<ContentRecommendation | null>(null);
  const [isScheduleModalOpen, setIsScheduleModalOpen] = useState(false);
  const [conflictError, setConflictError] = useState<{ message: string; conflicts: any[] } | null>(null);

  // Scheduling Form State
  const defaultTz = Intl.DateTimeFormat().resolvedOptions().timeZone || 'Asia/Kolkata';
  const [scheduleForm, setScheduleForm] = useState({
    date: '2026-10-18',
    startTime: '18:00',
    endTime: '19:00',
    platform: 'YOUTUBE',
    contentType: 'VIDEO',
    timezone: defaultTz,
    priority: 'HIGH' as 'LOW' | 'MEDIUM' | 'HIGH',
    notes: '',
  });

  const { data: slotSuggestions } = useCalendarSuggestions(
    scheduleForm.date,
    scheduleForm.platform,
    scheduleForm.timezone
  );

  const handleOpenScheduleModal = (rec: ContentRecommendation) => {
    setSelectedRec(rec);
    setConflictError(null);
    setScheduleForm({
      date: '2026-10-18',
      startTime: '18:00',
      endTime: '19:00',
      platform: (rec.targetPlatform || 'YOUTUBE').toUpperCase(),
      contentType: rec.contentType || 'VIDEO',
      timezone: defaultTz,
      priority: 'HIGH',
      notes: rec.problemAddressed ? `Addressing audience need: ${rec.problemAddressed}` : '',
    });
    setIsScheduleModalOpen(true);
  };

  const handleApprove = async (rec: ContentRecommendation) => {
    try {
      await approveMutation.mutateAsync(rec.id);
      addToast({
        title: 'Recommendation Approved',
        body: `"${rec.title}" is now approved and ready to schedule on your editorial calendar.`,
        variant: 'success',
      });
    } catch (err: any) {
      addToast({
        title: 'Approval Failed',
        body: err?.message || 'Could not approve recommendation.',
        variant: 'danger',
      });
    }
  };

  const handleScheduleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedRec) return;
    setConflictError(null);

    const startIso = `${scheduleForm.date}T${scheduleForm.startTime}:00Z`;
    const endIso = `${scheduleForm.date}T${scheduleForm.endTime}:00Z`;

    try {
      const createdItem = await createCalendarItemMutation.mutateAsync({
        recommendationId: selectedRec.id,
        topicId: selectedRec.topicId,
        title: selectedRec.title,
        platform: scheduleForm.platform,
        contentType: scheduleForm.contentType,
        scheduledStart: startIso,
        scheduledEnd: endIso,
        timezone: scheduleForm.timezone,
        priority: scheduleForm.priority,
        notes: scheduleForm.notes,
      });

      setIsScheduleModalOpen(false);
      addToast({
        title: 'Placed on Editorial Calendar',
        body: `"${selectedRec.title}" scheduled for ${scheduleForm.date} at ${scheduleForm.startTime} (${scheduleForm.timezone}).`,
        variant: 'success',
      });

      if (createdItem.warnings && createdItem.warnings.length > 0) {
        addToast({
          title: 'Topic Diversity Notice',
          body: createdItem.warnings[0].message,
          variant: 'warning',
        });
      }
    } catch (err: any) {
      if (err?.response?.status === 409) {
        const errorData = err.response.data;
        setConflictError({
          message: errorData.message || 'Scheduling conflict detected with existing content.',
          conflicts: errorData.conflicts || [],
        });
      } else {
        addToast({
          title: 'Scheduling Failed',
          body: err?.response?.data?.message || err?.message || 'Could not schedule item.',
          variant: 'danger',
        });
      }
    }
  };

  const applySlot = (slot: any) => {
    setScheduleForm(prev => ({
      ...prev,
      startTime: slot.time,
      endTime: slot.time === '09:00' ? '10:00' : slot.time === '13:00' ? '14:00' : '19:00',
    }));
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
        <div>
          <h2 className="text-xl font-bold text-slate-900 tracking-tight flex items-center gap-2">
            <Lightbulb className="w-5 h-5 text-indigo-600" />
            Evidence-Grounded Recommendations & Planning Queue
          </h2>
          <p className="text-xs text-slate-500 mt-0.5">
            AI-synthesized topic angles strictly grounded in audience comments and clustering evidence. Creator approval is required before scheduling.
          </p>
        </div>
      </div>

      {/* Recommendations Grid */}
      {isLoading ? (
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          {[1, 2, 3, 4].map(i => (
            <div key={i} className="h-56 bg-white rounded-xl border border-slate-200 animate-pulse" />
          ))}
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 gap-5">
          {recommendations?.map((rec: ContentRecommendation) => {
            const isApproved = rec.status === 'APPROVED' || rec.recommendationStatus === 'APPROVED';
            const isValidated = rec.status === 'VALIDATED' || rec.recommendationStatus === 'VALIDATED' || rec.validationPassed;
            const isRejected = rec.status === 'REJECTED' || rec.recommendationStatus === 'REJECTED';

            return (
              <div
                key={rec.id}
                className="bg-white rounded-xl border border-slate-200 p-5 space-y-4 hover:border-slate-300 transition-all flex flex-col justify-between"
              >
                <div className="space-y-3">
                  <div className="flex items-center justify-between gap-2">
                    <div className="flex items-center gap-2">
                      <span className="p-1 rounded bg-slate-100 text-slate-700">
                        {getPlatformIcon(rec.targetPlatform || 'youtube', 16)}
                      </span>
                      <Badge variant="accent" size="xs">
                        {rec.contentType || rec.format || 'VIDEO'}
                      </Badge>
                      {rec.topicName && (
                        <span className="inline-flex items-center gap-1 text-[11px] font-medium text-slate-600 bg-slate-100 px-2 py-0.5 rounded">
                          <Tag className="w-3 h-3 text-slate-400" />
                          {rec.topicName}
                        </span>
                      )}
                    </div>
                    <div>
                      {isApproved ? (
                        <Badge variant="success" size="xs">
                          <CheckCircle2 className="w-3 h-3 mr-1" />
                          Approved
                        </Badge>
                      ) : isRejected ? (
                        <Badge variant="danger" size="xs">
                          <AlertTriangle className="w-3 h-3 mr-1" />
                          Rejected
                        </Badge>
                      ) : isValidated ? (
                        <Badge variant="accent" size="xs">
                          <ShieldCheck className="w-3 h-3 mr-1" />
                          Validated
                        </Badge>
                      ) : (
                        <Badge variant="warning" size="xs">
                          Draft
                        </Badge>
                      )}
                    </div>
                  </div>

                  <h3 className="text-base font-bold text-slate-900">{rec.title}</h3>

                  {rec.hook && (
                    <div className="p-3 rounded-lg bg-indigo-50/40 border border-indigo-100">
                      <span className="text-[10px] font-bold uppercase tracking-wider text-indigo-700 block mb-0.5 flex items-center gap-1">
                        <Sparkles className="w-3 h-3" />
                        Recommended Hook / Opening Line
                      </span>
                      <p className="text-xs text-slate-800 italic">"{rec.hook}"</p>
                    </div>
                  )}

                  {rec.problemAddressed && (
                    <div className="text-xs text-slate-600">
                      <span className="font-semibold text-slate-700">Problem Addressed: </span>
                      {rec.problemAddressed}
                    </div>
                  )}

                  {rec.keyPoints && rec.keyPoints.length > 0 && (
                    <div className="text-xs text-slate-600 space-y-1">
                      <span className="font-semibold text-slate-700 flex items-center gap-1">
                        <Layers className="w-3 h-3 text-slate-400" />
                        Key Outlines:
                      </span>
                      <ul className="list-disc list-inside space-y-0.5 pl-1 text-[11px] text-slate-500">
                        {rec.keyPoints.slice(0, 3).map((point: string, idx: number) => (
                          <li key={idx} className="truncate">{point}</li>
                        ))}
                      </ul>
                    </div>
                  )}
                </div>

                {/* Actions Bar */}
                <div className="flex items-center justify-between pt-3 border-t border-slate-100 gap-2">
                  {!isApproved && !isRejected && (
                    <Button
                      variant="secondary"
                      size="sm"
                      onClick={() => handleApprove(rec)}
                      disabled={approveMutation.isPending}
                      className="text-xs text-indigo-700 hover:bg-indigo-50 border-indigo-200"
                    >
                      <CheckCircle2 className="w-3.5 h-3.5 mr-1" />
                      {approveMutation.isPending ? 'Approving...' : 'Approve Draft'}
                    </Button>
                  )}

                  {isApproved ? (
                    <Button
                      variant="primary"
                      size="sm"
                      onClick={() => handleOpenScheduleModal(rec)}
                      className="text-xs w-full sm:w-auto"
                    >
                      <CalendarPlus className="w-3.5 h-3.5 mr-1" />
                      Schedule on Calendar <ChevronRight className="w-3.5 h-3.5 ml-1" />
                    </Button>
                  ) : (
                    <span className="text-[11px] text-slate-400 italic">
                      {isRejected ? 'Draft rejected by evaluation guardrails.' : 'Creator approval required before scheduling.'}
                    </span>
                  )}
                </div>
              </div>
            );
          })}
        </div>
      )}

      {/* Scheduling Modal */}
      <Modal
        open={isScheduleModalOpen}
        onClose={() => setIsScheduleModalOpen(false)}
        title="Schedule Approved Content"
      >
        <form onSubmit={handleScheduleSubmit} className="space-y-4">
          {selectedRec && (
            <div className="p-3 bg-slate-50 border border-slate-200 rounded-lg space-y-1 text-xs">
              <span className="text-[10px] font-bold uppercase tracking-wider text-slate-500">
                Approved Recommendation
              </span>
              <p className="font-bold text-slate-900">{selectedRec.title}</p>
              {selectedRec.topicName && (
                <p className="text-[11px] text-slate-500">Topic: {selectedRec.topicName}</p>
              )}
            </div>
          )}

          {conflictError && (
            <div className="p-3.5 bg-rose-50 border border-rose-200 rounded-lg space-y-2 text-xs text-rose-800">
              <div className="flex items-center gap-1.5 font-bold text-rose-900">
                <AlertTriangle className="w-4 h-4 text-rose-600" />
                Scheduling Conflict Detected (HTTP 409)
              </div>
              <p>{conflictError.message}</p>
              {conflictError.conflicts.map((c, i) => (
                <div key={i} className="p-2 bg-white rounded border border-rose-100 text-[11px]">
                  <span className="font-semibold">{c.title}</span> ({c.platform})
                </div>
              ))}
              <p className="text-[11px] text-rose-600">
                Please pick an alternative posting time slot or platform below.
              </p>
            </div>
          )}

          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">Platform</label>
              <select
                value={scheduleForm.platform}
                onChange={e => setScheduleForm({ ...scheduleForm, platform: e.target.value })}
                className="w-full px-3 py-2 text-xs bg-white border border-slate-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500"
              >
                <option value="YOUTUBE">YouTube</option>
                <option value="INSTAGRAM">Instagram</option>
                <option value="LINKEDIN">LinkedIn</option>
                <option value="TIKTOK">TikTok</option>
                <option value="TWITTER">Twitter (X)</option>
              </select>
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">Content Format</label>
              <select
                value={scheduleForm.contentType}
                onChange={e => setScheduleForm({ ...scheduleForm, contentType: e.target.value })}
                className="w-full px-3 py-2 text-xs bg-white border border-slate-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500"
              >
                <option value="VIDEO">Video</option>
                <option value="SHORT">Short / Reel</option>
                <option value="CAROUSEL">Carousel</option>
                <option value="POST">Long Post / Article</option>
              </select>
            </div>
          </div>

          <div className="grid grid-cols-3 gap-3">
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">Date</label>
              <Input
                type="date"
                value={scheduleForm.date}
                onChange={e => setScheduleForm({ ...scheduleForm, date: e.target.value })}
                required
              />
            </div>
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">Start Time</label>
              <Input
                type="time"
                value={scheduleForm.startTime}
                onChange={e => setScheduleForm({ ...scheduleForm, startTime: e.target.value })}
                required
              />
            </div>
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">End Time</label>
              <Input
                type="time"
                value={scheduleForm.endTime}
                onChange={e => setScheduleForm({ ...scheduleForm, endTime: e.target.value })}
                required
              />
            </div>
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1">Creator Timezone (IANA)</label>
            <select
              value={scheduleForm.timezone}
              onChange={e => setScheduleForm({ ...scheduleForm, timezone: e.target.value })}
              className="w-full px-3 py-2 text-xs bg-white border border-slate-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500"
            >
              <option value="Asia/Kolkata">Asia/Kolkata (IST)</option>
              <option value="America/New_York">America/New_York (EST/EDT)</option>
              <option value="America/Los_Angeles">America/Los_Angeles (PST/PDT)</option>
              <option value="Europe/London">Europe/London (GMT/BST)</option>
              <option value="UTC">UTC</option>
            </select>
          </div>

          {/* Suggested deterministic slots */}
          {slotSuggestions && slotSuggestions.slots && (
            <div className="space-y-1.5 pt-1">
              <label className="block text-[11px] font-semibold text-slate-600 flex items-center gap-1">
                <Clock className="w-3 h-3 text-indigo-500" />
                Suggested Posting Windows ({scheduleForm.date}):
              </label>
              <div className="flex gap-2">
                {slotSuggestions.slots.map((s, idx) => (
                  <button
                    key={idx}
                    type="button"
                    onClick={() => s.available && applySlot(s)}
                    disabled={!s.available}
                    className={`flex-1 py-1 px-2 rounded border text-[11px] font-medium transition-all ${
                      scheduleForm.startTime === s.time
                        ? 'bg-indigo-600 text-white border-indigo-600'
                        : s.available
                        ? 'bg-white text-slate-700 border-slate-200 hover:bg-slate-50'
                        : 'bg-slate-100 text-slate-400 border-slate-200 cursor-not-allowed line-through'
                    }`}
                  >
                    {s.slotName} ({s.time})
                  </button>
                ))}
              </div>
            </div>
          )}

          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1">Editorial Notes / Instructions</label>
            <textarea
              rows={2}
              value={scheduleForm.notes}
              onChange={e => setScheduleForm({ ...scheduleForm, notes: e.target.value })}
              placeholder="Add key production milestones, outline notes, or resources..."
              className="w-full px-3 py-2 text-xs bg-white border border-slate-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500"
            />
          </div>

          <div className="flex justify-end gap-2 pt-2 border-t border-slate-100">
            <Button
              type="button"
              variant="secondary"
              size="sm"
              onClick={() => setIsScheduleModalOpen(false)}
            >
              Cancel
            </Button>
            <Button
              type="submit"
              variant="primary"
              size="sm"
              disabled={createCalendarItemMutation.isPending}
            >
              {createCalendarItemMutation.isPending ? 'Scheduling...' : 'Confirm Schedule'}
            </Button>
          </div>
        </form>
      </Modal>
    </div>
  );
};

