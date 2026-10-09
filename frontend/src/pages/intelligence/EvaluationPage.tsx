import React from 'react';
import { useEvaluationMetrics } from '../../hooks/useApi';
import { StatCard } from '../../components/ui/Card';
import { Badge } from '../../components/ui/Badge';
import { Button } from '../../components/ui/Button';
import {
  ShieldCheck, AlertTriangle, Zap, CheckCircle2,
  FileSearch, RefreshCw,
} from 'lucide-react';

export const EvaluationPage: React.FC = () => {
  const { data: metrics, isLoading, refetch, isFetching } = useEvaluationMetrics();

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
        <div>
          <h2 className="text-xl font-bold text-slate-900 tracking-tight flex items-center gap-2">
            <ShieldCheck className="w-5 h-5 text-indigo-600" />
            LLM Guardrails & RAG Evaluation Metrics
          </h2>
          <p className="text-xs text-slate-500 mt-0.5">
            Continuous verification of grounded responses, hallucination suppression, and inference latency.
          </p>
        </div>

        <Button
          variant="secondary"
          size="sm"
          onClick={() => refetch()}
          disabled={isFetching}
          className="text-xs"
        >
          <RefreshCw className={`w-3.5 h-3.5 mr-1.5 ${isFetching ? 'animate-spin' : ''}`} />
          Run Benchmark
        </Button>
      </div>

      {/* KPI Stats */}
      {isLoading || !metrics ? (
        <div className="grid grid-cols-1 sm:grid-cols-4 gap-4">
          {[1, 2, 3, 4].map(i => (
            <div key={i} className="h-24 bg-white rounded-xl border border-slate-200 animate-pulse" />
          ))}
        </div>
      ) : (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
          <StatCard
            label="RAG Retrieval Accuracy"
            value={`${metrics.ragAccuracy}%`}
            change={1.4}
            caption="Faithfulness benchmark"
            icon={<CheckCircle2 className="w-4 h-4 text-success" />}
          />
          <StatCard
            label="Hallucination Rate"
            value={`${metrics.hallucinationRate}%`}
            caption="Target SLA: < 1.0%"
            icon={<AlertTriangle className="w-4 h-4 text-accent" />}
          />
          <StatCard
            label="Avg Generation Latency"
            value={`${metrics.avgLatencyMs}ms`}
            caption="P95 latency: 510ms"
            icon={<Zap className="w-4 h-4 text-warning" />}
          />
          <StatCard
            label="Guardrail Interventions"
            value={metrics.guardrailTriggers}
            caption="Past 7 days triggered"
            icon={<ShieldCheck className="w-4 h-4 text-accent" />}
          />
        </div>
      )}

      {/* Real-time Query Verification Logs */}
      <div className="bg-white rounded-xl border border-slate-200 p-5 space-y-4">
        <div className="flex items-center justify-between border-b border-slate-100 pb-3">
          <div>
            <h3 className="text-sm font-bold text-slate-900 flex items-center gap-2">
              <FileSearch className="w-4 h-4 text-indigo-600" />
              Live Evaluation & Factuality Audit Log
            </h3>
            <p className="text-xs text-slate-500">Every Ask PulseGPT query evaluated against source comment ground truth</p>
          </div>
        </div>

        <div className="divide-y divide-slate-100">
          {metrics?.recentLogs.map(log => (
            <div key={log.id} className="py-3 flex flex-col sm:flex-row sm:items-center justify-between gap-3 text-xs">
              <div className="space-y-1">
                <p className="font-semibold text-slate-800">"{log.query}"</p>
                <div className="flex items-center gap-3 text-slate-500 text-[11px]">
                  <span>Confidence: {(log.confidence * 100).toFixed(0)}%</span>
                  <span>•</span>
                  <span>Latency: {log.latencyMs}ms</span>
                  <span>•</span>
                  <span>{log.timestamp}</span>
                </div>
              </div>

              <div className="flex items-center gap-2 flex-shrink-0">
                <Badge
                  variant={log.hallucinationCheck === 'PASSED' ? 'success' : 'danger'}
                  size="xs"
                >
                  {log.hallucinationCheck}
                </Badge>
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
};
