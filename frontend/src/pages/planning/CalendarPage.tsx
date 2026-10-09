import React, { useState } from 'react';
import {
  useCalendarItems,
  useCreateCalendarItem,
  useUpdateCalendarItem,
  useCancelCalendarItem,
  useCalendarSuggestions,
} from '../../hooks/useApi';
import { Button } from '../../components/ui/Button';
import { Badge } from '../../components/ui/Badge';
import { Modal } from '../../components/ui/Modal';
import { Input } from '../../components/ui/Input';
import { getPlatformIcon } from '../../lib/platform';
import {
  Calendar as CalendarIcon,
  Plus,
  Clock,
  AlertTriangle,
  Tag,
  XCircle,
  Edit3,
  CalendarRange,
  List as ListIcon,
  Grid,
} from 'lucide-react';
import { useToast } from '../../components/ui/Toast';
import type { CalendarItem } from '../../types/models';

export const CalendarPage: React.FC = () => {
  const { addToast } = useToast();

  const [viewMode, setViewMode] = useState<'month' | 'week' | 'list'>('month');
  const [platformFilter, setPlatformFilter] = useState<string>('ALL');
  const [statusFilter, setStatusFilter] = useState<string>('ALL');

  const { data: items, isLoading } = useCalendarItems({
    platform: platformFilter !== 'ALL' ? platformFilter : undefined,
    status: statusFilter !== 'ALL' ? statusFilter : undefined,
  });

  const createItemMutation = useCreateCalendarItem();
  const updateItemMutation = useUpdateCalendarItem();
  const cancelItemMutation = useCancelCalendarItem();

  const defaultTz = Intl.DateTimeFormat().resolvedOptions().timeZone || 'Asia/Kolkata';

  // Create Modal State
  const [isCreateModalOpen, setIsCreateModalOpen] = useState(false);
  const [createForm, setCreateForm] = useState({
    title: '',
    platform: 'YOUTUBE',
    contentType: 'VIDEO',
    date: '2026-10-18',
    startTime: '18:00',
    endTime: '19:00',
    timezone: defaultTz,
    priority: 'MEDIUM' as 'LOW' | 'MEDIUM' | 'HIGH',
    notes: '',
  });

  // Edit / Reschedule Modal State
  const [editingItem, setEditingItem] = useState<CalendarItem | null>(null);
  const [editForm, setEditForm] = useState({
    title: '',
    platform: 'YOUTUBE',
    contentType: 'VIDEO',
    date: '',
    startTime: '',
    endTime: '',
    timezone: defaultTz,
    priority: 'MEDIUM' as 'LOW' | 'MEDIUM' | 'HIGH',
    notes: '',
  });

  const [conflictError, setConflictError] = useState<{ message: string; conflicts: any[] } | null>(null);

  const { data: slotSuggestions } = useCalendarSuggestions(
    isCreateModalOpen ? createForm.date : editingItem ? editForm.date : '2026-10-18',
    isCreateModalOpen ? createForm.platform : editingItem ? editForm.platform : 'YOUTUBE',
    isCreateModalOpen ? createForm.timezone : editForm.timezone
  );

  const handleOpenCreateModal = () => {
    setConflictError(null);
    setCreateForm({
      title: '',
      platform: 'YOUTUBE',
      contentType: 'VIDEO',
      date: '2026-10-18',
      startTime: '18:00',
      endTime: '19:00',
      timezone: defaultTz,
      priority: 'MEDIUM',
      notes: '',
    });
    setIsCreateModalOpen(true);
  };

  const handleOpenEditModal = (item: CalendarItem) => {
    setConflictError(null);
    setEditingItem(item);

    const start = new Date(item.scheduledStart);
    const end = item.scheduledEnd ? new Date(item.scheduledEnd) : new Date(start.getTime() + 3600000);

    const dateStr = start.toISOString().split('T')[0];
    const startTimeStr = start.toISOString().substring(11, 16);
    const endTimeStr = end.toISOString().substring(11, 16);

    setEditForm({
      title: item.title,
      platform: item.platform,
      contentType: item.contentType || 'VIDEO',
      date: dateStr,
      startTime: startTimeStr,
      endTime: endTimeStr,
      timezone: item.timezone || defaultTz,
      priority: item.priority || 'MEDIUM',
      notes: item.notes || item.description || '',
    });
  };

  const handleCreateSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setConflictError(null);

    const startIso = `${createForm.date}T${createForm.startTime}:00Z`;
    const endIso = `${createForm.date}T${createForm.endTime}:00Z`;

    try {
      const created = await createItemMutation.mutateAsync({
        title: createForm.title,
        platform: createForm.platform,
        contentType: createForm.contentType,
        scheduledStart: startIso,
        scheduledEnd: endIso,
        timezone: createForm.timezone,
        priority: createForm.priority,
        notes: createForm.notes,
      });

      setIsCreateModalOpen(false);
      addToast({
        title: 'Plan Added to Calendar',
        body: `"${createForm.title}" scheduled for ${createForm.date} at ${createForm.startTime}.`,
        variant: 'success',
      });

      if (created.warnings && created.warnings.length > 0) {
        addToast({
          title: 'Topic Diversity Notice',
          body: created.warnings[0].message,
          variant: 'warning',
        });
      }
    } catch (err: any) {
      if (err?.response?.status === 409) {
        const errorData = err.response.data;
        setConflictError({
          message: errorData.message || 'Scheduling conflict detected on this platform slot.',
          conflicts: errorData.conflicts || [],
        });
      } else {
        addToast({
          title: 'Scheduling Failed',
          body: err?.response?.data?.message || err?.message || 'Could not schedule plan.',
          variant: 'danger',
        });
      }
    }
  };

  const handleEditSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!editingItem) return;
    setConflictError(null);

    const startIso = `${editForm.date}T${editForm.startTime}:00Z`;
    const endIso = `${editForm.date}T${editForm.endTime}:00Z`;

    try {
      await updateItemMutation.mutateAsync({
        id: editingItem.id,
        payload: {
          title: editForm.title,
          platform: editForm.platform,
          contentType: editForm.contentType,
          scheduledStart: startIso,
          scheduledEnd: endIso,
          timezone: editForm.timezone,
          priority: editForm.priority,
          notes: editForm.notes,
        },
      });

      setEditingItem(null);
      addToast({
        title: 'Calendar Item Updated',
        body: `"${editForm.title}" rescheduled successfully.`,
        variant: 'success',
      });
    } catch (err: any) {
      if (err?.response?.status === 409) {
        const errorData = err.response.data;
        setConflictError({
          message: errorData.message || 'Rescheduling conflict detected on this platform slot.',
          conflicts: errorData.conflicts || [],
        });
      } else {
        addToast({
          title: 'Reschedule Failed',
          body: err?.response?.data?.message || err?.message || 'Could not update item.',
          variant: 'danger',
        });
      }
    }
  };

  const handleCancelItem = async (item: CalendarItem) => {
    if (!window.confirm(`Are you sure you want to cancel "${item.title}"?`)) return;
    try {
      await cancelItemMutation.mutateAsync({ id: item.id, reason: 'CREATOR_MANUAL_CANCEL' });
      addToast({
        title: 'Plan Cancelled',
        body: `"${item.title}" marked as CANCELLED while preserving history.`,
        variant: 'info',
      });
    } catch (err: any) {
      addToast({
        title: 'Cancellation Failed',
        body: err?.message || 'Could not cancel item.',
        variant: 'danger',
      });
    }
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
        <div>
          <h2 className="text-xl font-bold text-slate-900 tracking-tight flex items-center gap-2">
            <CalendarIcon className="w-5 h-5 text-indigo-600" />
            Editorial Content Calendar & Planning Manager
          </h2>
          <p className="text-xs text-slate-500 mt-0.5">
            Creator-controlled multi-platform editorial schedule with deterministic conflict detection and audience evidence traceability.
          </p>
        </div>

        <div className="flex items-center gap-2">
          <Button
            variant="primary"
            size="sm"
            onClick={handleOpenCreateModal}
            className="text-xs"
          >
            <Plus className="w-3.5 h-3.5 mr-1" />
            Plan New Post
          </Button>
        </div>
      </div>

      {/* Control Bar: Views & Filters */}
      <div className="bg-white p-4 rounded-xl border border-slate-200 flex flex-wrap items-center justify-between gap-4">
        <div className="flex items-center gap-2">
          <div className="inline-flex rounded-lg bg-slate-100 p-0.5 text-xs font-semibold text-slate-700">
            <button
              onClick={() => setViewMode('month')}
              className={`px-3 py-1.5 rounded-md flex items-center gap-1.5 transition-all ${
                viewMode === 'month' ? 'bg-white text-slate-900 shadow-sm' : 'text-slate-500 hover:text-slate-900'
              }`}
            >
              <Grid className="w-3.5 h-3.5" />
              Month
            </button>
            <button
              onClick={() => setViewMode('week')}
              className={`px-3 py-1.5 rounded-md flex items-center gap-1.5 transition-all ${
                viewMode === 'week' ? 'bg-white text-slate-900 shadow-sm' : 'text-slate-500 hover:text-slate-900'
              }`}
            >
              <CalendarRange className="w-3.5 h-3.5" />
              Week
            </button>
            <button
              onClick={() => setViewMode('list')}
              className={`px-3 py-1.5 rounded-md flex items-center gap-1.5 transition-all ${
                viewMode === 'list' ? 'bg-white text-slate-900 shadow-sm' : 'text-slate-500 hover:text-slate-900'
              }`}
            >
              <ListIcon className="w-3.5 h-3.5" />
              List
            </button>
          </div>

          <Badge variant="accent" size="sm">
            {items?.length || 0} Items
          </Badge>
        </div>

        {/* Platform & Status Filters */}
        <div className="flex items-center gap-3">
          <select
            value={platformFilter}
            onChange={e => setPlatformFilter(e.target.value)}
            className="px-2.5 py-1.5 text-xs bg-white border border-slate-200 rounded-lg text-slate-700 focus:outline-none focus:ring-2 focus:ring-indigo-500"
          >
            <option value="ALL">All Platforms</option>
            <option value="YOUTUBE">YouTube</option>
            <option value="INSTAGRAM">Instagram</option>
            <option value="LINKEDIN">LinkedIn</option>
            <option value="TIKTOK">TikTok</option>
            <option value="TWITTER">Twitter (X)</option>
          </select>

          <select
            value={statusFilter}
            onChange={e => setStatusFilter(e.target.value)}
            className="px-2.5 py-1.5 text-xs bg-white border border-slate-200 rounded-lg text-slate-700 focus:outline-none focus:ring-2 focus:ring-indigo-500"
          >
            <option value="ALL">All Statuses</option>
            <option value="SCHEDULED">Scheduled</option>
            <option value="PLANNED">Planned</option>
            <option value="CANCELLED">Cancelled</option>
          </select>
        </div>
      </div>

      {/* Calendar Content Items */}
      {isLoading ? (
        <div className="space-y-3">
          {[1, 2, 3].map(i => (
            <div key={i} className="h-28 bg-white rounded-xl border border-slate-200 animate-pulse" />
          ))}
        </div>
      ) : items && items.length > 0 ? (
        <div className={viewMode === 'list' ? 'space-y-3' : 'grid grid-cols-1 md:grid-cols-2 gap-4'}>
          {items.map(item => {
            const start = new Date(item.scheduledStart);
            const dateDisplay = start.toLocaleDateString('en-US', {
              month: 'short',
              day: 'numeric',
              year: 'numeric',
            });
            const timeDisplay = start.toLocaleTimeString('en-US', {
              hour: '2-digit',
              minute: '2-digit',
              hour12: false,
            });

            const isCancelled = item.status === 'CANCELLED';

            return (
              <div
                key={item.id}
                className={`bg-white rounded-xl border p-5 space-y-3 transition-all ${
                  isCancelled
                    ? 'border-slate-200 opacity-60 bg-slate-50/50'
                    : 'border-slate-200 hover:border-indigo-300 shadow-sm'
                }`}
              >
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-2">
                    <span className="p-1 rounded bg-slate-100 text-slate-700">
                      {getPlatformIcon(item.platform.toLowerCase(), 14)}
                    </span>
                    <span className="text-xs font-semibold text-slate-800 capitalize">
                      {item.platform}
                    </span>
                    <Badge variant="default" size="xs">
                      {item.contentType || 'VIDEO'}
                    </Badge>
                    {item.priority && (
                      <span
                        className={`text-[10px] font-bold px-1.5 py-0.5 rounded ${
                          item.priority === 'HIGH'
                            ? 'bg-amber-50 text-amber-700 border border-amber-200'
                            : 'bg-slate-100 text-slate-600'
                        }`}
                      >
                        {item.priority}
                      </span>
                    )}
                  </div>

                  <div className="flex items-center gap-1.5">
                    <Badge
                      variant={
                        isCancelled
                          ? 'danger'
                          : item.status === 'SCHEDULED'
                          ? 'success'
                          : 'accent'
                      }
                      size="xs"
                    >
                      {item.status}
                    </Badge>
                  </div>
                </div>

                <div>
                  <h4 className={`text-sm font-bold text-slate-900 ${isCancelled ? 'line-through text-slate-500' : ''}`}>
                    {item.title}
                  </h4>
                  {item.notes && (
                    <p className="text-xs text-slate-500 mt-1 line-clamp-2">{item.notes}</p>
                  )}
                </div>

                {item.topicName && (
                  <div className="flex items-center gap-1 text-[11px] text-slate-600">
                    <Tag className="w-3 h-3 text-indigo-500" />
                    <span className="font-semibold text-slate-700">Topic Evidence:</span>
                    <span className="truncate">{item.topicName}</span>
                  </div>
                )}

                <div className="flex items-center justify-between pt-2.5 border-t border-slate-100 text-xs text-slate-500">
                  <div className="flex items-center gap-1.5 font-medium text-slate-700">
                    <Clock className="w-3.5 h-3.5 text-indigo-600" />
                    <span>
                      {dateDisplay} at {timeDisplay} ({item.timezone || 'UTC'})
                    </span>
                  </div>

                  {!isCancelled && (
                    <div className="flex items-center gap-2">
                      <Button
                        variant="secondary"
                        size="xs"
                        onClick={() => handleOpenEditModal(item)}
                        className="text-slate-600 hover:text-slate-900"
                      >
                        <Edit3 className="w-3 h-3 mr-1" />
                        Reschedule
                      </Button>
                      <Button
                        variant="secondary"
                        size="xs"
                        onClick={() => handleCancelItem(item)}
                        className="text-rose-600 hover:bg-rose-50 border-rose-200"
                      >
                        <XCircle className="w-3 h-3 mr-1" />
                        Cancel
                      </Button>
                    </div>
                  )}
                </div>
              </div>
            );
          })}
        </div>
      ) : (
        <div className="bg-white p-12 text-center rounded-xl border border-slate-200 space-y-3">
          <CalendarIcon className="w-10 h-10 text-slate-300 mx-auto" />
          <h3 className="text-base font-bold text-slate-800">No scheduled content pieces found</h3>
          <p className="text-xs text-slate-500 max-w-sm mx-auto">
            You have no content scheduled for this filter view. Approve recommendations or plan new content to place it on the calendar.
          </p>
          <Button variant="primary" size="sm" onClick={handleOpenCreateModal} className="text-xs mt-2">
            <Plus className="w-3.5 h-3.5 mr-1" />
            Schedule New Content
          </Button>
        </div>
      )}

      {/* Plan New Post Modal */}
      <Modal
        open={isCreateModalOpen}
        onClose={() => setIsCreateModalOpen(false)}
        title="Schedule Planned Content"
      >
        <form onSubmit={handleCreateSubmit} className="space-y-4">
          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1">Post Title / Concept</label>
            <Input
              value={createForm.title}
              onChange={e => setCreateForm({ ...createForm, title: e.target.value })}
              placeholder="e.g. Next.js 15 Server Actions In-depth"
              required
            />
          </div>

          {conflictError && (
            <div className="p-3.5 bg-rose-50 border border-rose-200 rounded-lg space-y-1.5 text-xs text-rose-800">
              <div className="flex items-center gap-1.5 font-bold text-rose-900">
                <AlertTriangle className="w-4 h-4 text-rose-600" />
                Scheduling Conflict (HTTP 409)
              </div>
              <p>{conflictError.message}</p>
              {conflictError.conflicts.map((c, i) => (
                <div key={i} className="p-2 bg-white rounded border border-rose-100 text-[11px]">
                  <span className="font-semibold">{c.title}</span> ({c.platform})
                </div>
              ))}
            </div>
          )}

          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">Platform</label>
              <select
                value={createForm.platform}
                onChange={e => setCreateForm({ ...createForm, platform: e.target.value })}
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
                value={createForm.contentType}
                onChange={e => setCreateForm({ ...createForm, contentType: e.target.value })}
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
                value={createForm.date}
                onChange={e => setCreateForm({ ...createForm, date: e.target.value })}
                required
              />
            </div>
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">Start Time</label>
              <Input
                type="time"
                value={createForm.startTime}
                onChange={e => setCreateForm({ ...createForm, startTime: e.target.value })}
                required
              />
            </div>
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">End Time</label>
              <Input
                type="time"
                value={createForm.endTime}
                onChange={e => setCreateForm({ ...createForm, endTime: e.target.value })}
                required
              />
            </div>
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1">Timezone (IANA)</label>
            <select
              value={createForm.timezone}
              onChange={e => setCreateForm({ ...createForm, timezone: e.target.value })}
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
                Available Posting Windows ({createForm.date}):
              </label>
              <div className="flex gap-2">
                {slotSuggestions.slots.map((s, idx) => (
                  <button
                    key={idx}
                    type="button"
                    onClick={() => s.available && setCreateForm(p => ({ ...p, startTime: s.time, endTime: s.time === '09:00' ? '10:00' : s.time === '13:00' ? '14:00' : '19:00' }))}
                    disabled={!s.available}
                    className={`flex-1 py-1 px-2 rounded border text-[11px] font-medium transition-all ${
                      createForm.startTime === s.time
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
            <label className="block text-xs font-semibold text-slate-700 mb-1">Editorial Notes</label>
            <textarea
              rows={2}
              value={createForm.notes}
              onChange={e => setCreateForm({ ...createForm, notes: e.target.value })}
              placeholder="Production notes, outline, references..."
              className="w-full px-3 py-2 text-xs bg-white border border-slate-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500"
            />
          </div>

          <div className="flex justify-end gap-2 pt-2 border-t border-slate-100">
            <Button
              type="button"
              variant="secondary"
              size="sm"
              onClick={() => setIsCreateModalOpen(false)}
            >
              Cancel
            </Button>
            <Button
              type="submit"
              variant="primary"
              size="sm"
              disabled={createItemMutation.isPending}
            >
              {createItemMutation.isPending ? 'Saving...' : 'Save to Calendar'}
            </Button>
          </div>
        </form>
      </Modal>

      {/* Edit / Reschedule Modal */}
      <Modal
        open={!!editingItem}
        onClose={() => setEditingItem(null)}
        title="Reschedule / Edit Plan"
      >
        <form onSubmit={handleEditSubmit} className="space-y-4">
          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1">Title</label>
            <Input
              value={editForm.title}
              onChange={e => setEditForm({ ...editForm, title: e.target.value })}
              required
            />
          </div>

          {conflictError && (
            <div className="p-3.5 bg-rose-50 border border-rose-200 rounded-lg space-y-1.5 text-xs text-rose-800">
              <div className="flex items-center gap-1.5 font-bold text-rose-900">
                <AlertTriangle className="w-4 h-4 text-rose-600" />
                Rescheduling Conflict (HTTP 409)
              </div>
              <p>{conflictError.message}</p>
              {conflictError.conflicts.map((c, i) => (
                <div key={i} className="p-2 bg-white rounded border border-rose-100 text-[11px]">
                  <span className="font-semibold">{c.title}</span> ({c.platform})
                </div>
              ))}
            </div>
          )}

          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">Platform</label>
              <select
                value={editForm.platform}
                onChange={e => setEditForm({ ...editForm, platform: e.target.value })}
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
                value={editForm.contentType}
                onChange={e => setEditForm({ ...editForm, contentType: e.target.value })}
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
                value={editForm.date}
                onChange={e => setEditForm({ ...editForm, date: e.target.value })}
                required
              />
            </div>
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">Start Time</label>
              <Input
                type="time"
                value={editForm.startTime}
                onChange={e => setEditForm({ ...editForm, startTime: e.target.value })}
                required
              />
            </div>
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">End Time</label>
              <Input
                type="time"
                value={editForm.endTime}
                onChange={e => setEditForm({ ...editForm, endTime: e.target.value })}
                required
              />
            </div>
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1">Timezone (IANA)</label>
            <select
              value={editForm.timezone}
              onChange={e => setEditForm({ ...editForm, timezone: e.target.value })}
              className="w-full px-3 py-2 text-xs bg-white border border-slate-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500"
            >
              <option value="Asia/Kolkata">Asia/Kolkata (IST)</option>
              <option value="America/New_York">America/New_York (EST/EDT)</option>
              <option value="America/Los_Angeles">America/Los_Angeles (PST/PDT)</option>
              <option value="Europe/London">Europe/London (GMT/BST)</option>
              <option value="UTC">UTC</option>
            </select>
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1">Notes</label>
            <textarea
              rows={2}
              value={editForm.notes}
              onChange={e => setEditForm({ ...editForm, notes: e.target.value })}
              className="w-full px-3 py-2 text-xs bg-white border border-slate-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500"
            />
          </div>

          <div className="flex justify-end gap-2 pt-2 border-t border-slate-100">
            <Button
              type="button"
              variant="secondary"
              size="sm"
              onClick={() => setEditingItem(null)}
            >
              Cancel
            </Button>
            <Button
              type="submit"
              variant="primary"
              size="sm"
              disabled={updateItemMutation.isPending}
            >
              {updateItemMutation.isPending ? 'Updating...' : 'Update Schedule'}
            </Button>
          </div>
        </form>
      </Modal>
    </div>
  );
};
