import React, { Suspense, lazy } from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';
import { ProtectedRoute } from './ProtectedRoute';
import { RoleRoute } from './RoleRoute';
import { AppShell } from '../components/AppShell';

// Auth Pages (lazy loaded)
const LoginPage = lazy(() => import('../pages/auth/LoginPage').then(m => ({ default: m.LoginPage })));
const RegisterPage = lazy(() => import('../pages/auth/RegisterPage').then(m => ({ default: m.RegisterPage })));
const ForgotPasswordPage = lazy(() => import('../pages/auth/ForgotPasswordPage').then(m => ({ default: m.ForgotPasswordPage })));

// Error Pages
const ForbiddenPage = lazy(() => import('../pages/errors/ForbiddenPage').then(m => ({ default: m.ForbiddenPage })));
const NotFoundPage = lazy(() => import('../pages/errors/NotFoundPage').then(m => ({ default: m.NotFoundPage })));

// App Section Pages (lazy loaded)
const OverviewPage = lazy(() => import('../pages/dashboard/OverviewPage').then(m => ({ default: m.OverviewPage })));
const AudiencePage = lazy(() => import('../pages/intelligence/AudiencePage').then(m => ({ default: m.AudiencePage })));
const TrendsPage = lazy(() => import('../pages/intelligence/TrendsPage').then(m => ({ default: m.TrendsPage })));
const MemoryPage = lazy(() => import('../pages/intelligence/MemoryPage').then(m => ({ default: m.MemoryPage })));
const CommentsPage = lazy(() => import('../pages/engagement/CommentsPage').then(m => ({ default: m.CommentsPage })));
const RecommendationsPage = lazy(() => import('../pages/intelligence/RecommendationsPage').then(m => ({ default: m.RecommendationsPage })));
const ProductionPage = lazy(() => import('../pages/planning/ProductionPage').then(m => ({ default: m.ProductionPage })));
const CalendarPage = lazy(() => import('../pages/planning/CalendarPage').then(m => ({ default: m.CalendarPage })));
const PlannerPage = lazy(() => import('../pages/planning/PlannerPage').then(m => ({ default: m.PlannerPage })));
const ChatPage = lazy(() => import('../pages/engagement/ChatPage').then(m => ({ default: m.ChatPage })));
const IntegrationsPage = lazy(() => import('../pages/management/IntegrationsPage').then(m => ({ default: m.IntegrationsPage })));
const EvaluationPage = lazy(() => import('../pages/intelligence/EvaluationPage').then(m => ({ default: m.EvaluationPage })));
const ResearchPage = lazy(() => import('../pages/intelligence/ResearchPage').then(m => ({ default: m.ResearchPage })));
const RagPage = lazy(() => import('../pages/rag/RagPage').then(m => ({ default: m.RagPage })));
const SettingsPage = lazy(() => import('../pages/management/SettingsPage').then(m => ({ default: m.SettingsPage })));


// Admin Page
const AdminPage = lazy(() => import('../pages/management/AdminPage').then(m => ({ default: m.AdminPage })));

const PageLoader: React.FC = () => (
  <div className="min-h-[50vh] flex items-center justify-center">
    <div className="flex flex-col items-center gap-2">
      <div className="w-8 h-8 border-3 border-indigo-600 border-t-transparent rounded-full animate-spin" />
      <span className="text-xs font-medium text-slate-400">Loading view...</span>
    </div>
  </div>
);

export const AppRoutes: React.FC = () => {
  return (
    <Suspense fallback={<PageLoader />}>
      <Routes>
        {/* Public Auth Routes */}
        <Route path="/login" element={<LoginPage />} />
        <Route path="/register" element={<RegisterPage />} />
        <Route path="/forgot-password" element={<ForgotPasswordPage />} />

        {/* Root Redirect */}
        <Route path="/" element={<Navigate to="/app/overview" replace />} />

        {/* Protected App Routes */}
        <Route
          path="/app"
          element={
            <ProtectedRoute>
              <AppShell />
            </ProtectedRoute>
          }
        >
          <Route index element={<Navigate to="/app/overview" replace />} />
          <Route path="overview" element={<OverviewPage />} />
          <Route path="audience" element={<AudiencePage />} />
          <Route path="trends" element={<TrendsPage />} />
          <Route path="memory" element={<MemoryPage />} />
          <Route path="comments" element={<CommentsPage />} />
          <Route path="recommendations" element={<RecommendationsPage />} />
          <Route path="production" element={<ProductionPage />} />
          <Route path="calendar" element={<CalendarPage />} />
          <Route path="planner" element={<PlannerPage />} />

          <Route path="chat" element={<ChatPage />} />
          <Route path="integrations" element={<IntegrationsPage />} />
          <Route path="evaluation" element={<EvaluationPage />} />
          <Route path="research" element={<ResearchPage />} />
          <Route path="rag" element={<RagPage />} />
          <Route path="settings" element={<SettingsPage />} />
        </Route>

        {/* Role Protected Admin Route */}
        <Route
          path="/admin"
          element={
            <RoleRoute allowedRoles={['ADMIN']}>
              <AppShell />
            </RoleRoute>
          }
        >
          <Route index element={<AdminPage />} />
        </Route>

        {/* Error Pages */}
        <Route path="/403" element={<ForbiddenPage />} />
        <Route path="/404" element={<NotFoundPage />} />
        <Route path="*" element={<Navigate to="/404" replace />} />
      </Routes>
    </Suspense>
  );
};
