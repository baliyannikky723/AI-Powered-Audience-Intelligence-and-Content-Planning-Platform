import React, { useState } from 'react';
import { Link, useNavigate, useLocation } from 'react-router-dom';
import { AuthLayout } from '../../components/auth/AuthLayout';
import { Input } from '../../components/ui/Input';
import { Button } from '../../components/ui/Button';
import { useAuth } from '../../context/AuthContext';
import { loginSchema, type LoginFormData } from '../../lib/validation';
import { Shield, Sparkles, AlertCircle } from 'lucide-react';

export const LoginPage: React.FC = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const { login, isLoading } = useAuth();

  const [formData, setFormData] = useState<LoginFormData>({
    email: '',
    password: '',
    rememberMe: false,
  });

  const [errors, setErrors] = useState<Partial<Record<keyof LoginFormData, string>>>({});
  const [formError, setFormError] = useState<string | null>(null);

  const from = (location.state as any)?.from?.pathname || '/app/overview';

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setFormError(null);

    const validation = loginSchema.safeParse(formData);
    if (!validation.success) {
      const fieldErrors: Partial<Record<keyof LoginFormData, string>> = {};
      validation.error.issues.forEach(err => {
        const path = err.path[0] as keyof LoginFormData;
        if (path) fieldErrors[path] = err.message;
      });
      setErrors(fieldErrors);
      return;
    }

    setErrors({});
    try {
      await login(formData);
      navigate(from, { replace: true });
    } catch (err: any) {
      setFormError(err.message || 'Invalid credentials. Please try again.');
    }
  };

  const handleDemoLogin = async (role: 'creator' | 'admin') => {
    setFormError(null);
    setErrors({});
    if (role === 'admin') {
      await login({ email: 'admin@pulse-gpt.ai', password: 'password123' });
      navigate('/admin', { replace: true });
    } else {
      await login({ email: 'sarah.chen@creator.io', password: 'password123' });
      navigate('/app/overview', { replace: true });
    }
  };

  return (
    <AuthLayout
      title="Welcome back"
      subtitle="Enter your account credentials to access your intelligence workspace."
    >
      {formError && (
        <div className="p-3 rounded-lg bg-danger-soft border border-danger-border text-danger text-xs flex items-center gap-2">
          <AlertCircle className="w-4 h-4 flex-shrink-0" />
          <span>{formError}</span>
        </div>
      )}

      {/* Quick Demo Login Preset Buttons */}
      <div className="p-3.5 rounded-xl bg-accent-soft border border-accent/20 space-y-2">
        <div className="flex items-center justify-between text-xs text-ink font-semibold">
          <span className="flex items-center gap-1.5">
            <Sparkles className="w-3.5 h-3.5 text-accent" />
            Instant Demo Sign-in
          </span>
          <span className="text-[10px] uppercase font-bold text-accent bg-white px-1.5 py-0.5 rounded shadow-xs">
            Mock Mode
          </span>
        </div>
        <div className="grid grid-cols-2 gap-2">
          <Button
            type="button"
            variant="secondary"
            size="sm"
            onClick={() => handleDemoLogin('creator')}
            disabled={isLoading}
            className="w-full text-xs font-medium"
          >
            Demo as Creator
          </Button>
          <Button
            type="button"
            variant="secondary"
            size="sm"
            onClick={() => handleDemoLogin('admin')}
            disabled={isLoading}
            className="w-full text-xs font-semibold text-accent"
          >
            <Shield className="w-3.5 h-3.5 mr-1" />
            Demo as Admin
          </Button>
        </div>
      </div>

      <form onSubmit={handleSubmit} className="space-y-4">
        <div>
          <label className="block text-xs font-semibold text-ink mb-1.5">Email Address</label>
          <Input
            type="email"
            name="email"
            placeholder="you@company.com"
            value={formData.email}
            onChange={e => setFormData({ ...formData, email: e.target.value })}
            error={errors.email}
            required
          />
        </div>

        <div>
          <label className="block text-xs font-semibold text-ink mb-1.5">Password</label>
          <Input
            type="password"
            name="password"
            placeholder="••••••••"
            value={formData.password}
            onChange={e => setFormData({ ...formData, password: e.target.value })}
            error={errors.password}
            required
          />
          <div className="flex items-center justify-between mt-1.5">
            <label className="flex items-center gap-2 cursor-pointer text-xs text-ink-2">
              <input
                type="checkbox"
                checked={formData.rememberMe}
                onChange={e => setFormData({ ...formData, rememberMe: e.target.checked })}
                className="w-3.5 h-3.5 text-accent rounded border-border focus:ring-accent"
              />
              <span>Remember me</span>
            </label>
            <Link
              to="/forgot-password"
              className="text-xs font-medium text-accent hover:underline"
            >
              Forgot password?
            </Link>
          </div>
        </div>

        <Button
          type="submit"
          variant="primary"
          size="md"
          loading={isLoading}
          className="w-full mt-2"
        >
          Sign In
        </Button>
      </form>

      <div className="text-center pt-2">
        <p className="text-xs text-ink-3">
          Don't have an account?{' '}
          <Link
            to="/register"
            className="font-semibold text-accent hover:underline"
          >
            Create an account
          </Link>
        </p>
      </div>
    </AuthLayout>
  );
};
