import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { AuthLayout } from '../../components/auth/AuthLayout';
import { Input } from '../../components/ui/Input';
import { Button } from '../../components/ui/Button';
import { useAuth } from '../../context/AuthContext';
import { registerSchema, type RegisterFormData } from '../../lib/validation';
import { AlertCircle } from 'lucide-react';

export const RegisterPage: React.FC = () => {
  const navigate = useNavigate();
  const { register, isLoading } = useAuth();

  const [formData, setFormData] = useState<RegisterFormData>({
    name: '',
    email: '',
    password: '',
    confirmPassword: '',
    company: '',
    termsAccepted: false,
  });

  const [errors, setErrors] = useState<Partial<Record<keyof RegisterFormData, string>>>({});
  const [formError, setFormError] = useState<string | null>(null);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setFormError(null);

    const validation = registerSchema.safeParse(formData);
    if (!validation.success) {
      const fieldErrors: Partial<Record<keyof RegisterFormData, string>> = {};
      validation.error.issues.forEach(err => {
        const path = err.path[0] as keyof RegisterFormData;
        if (path) fieldErrors[path] = err.message;
      });
      setErrors(fieldErrors);
      return;
    }

    setErrors({});
    try {
      await register(formData);
      navigate('/app/overview', { replace: true });
    } catch (err: any) {
      setFormError(err.message || 'Registration failed. Please try again.');
    }
  };

  return (
    <AuthLayout
      title="Create your account"
      subtitle="Start generating high-impact audience intelligence in under 2 minutes."
    >
      {formError && (
        <div className="p-3 rounded-lg bg-danger-soft border border-danger-border text-danger text-xs flex items-center gap-2">
          <AlertCircle className="w-4 h-4 flex-shrink-0" />
          <span>{formError}</span>
        </div>
      )}

      <form onSubmit={handleSubmit} className="space-y-3.5">
        <div>
          <label className="block text-xs font-semibold text-ink mb-1">Full Name</label>
          <Input
            type="text"
            name="name"
            placeholder="e.g. Sarah Chen"
            value={formData.name}
            onChange={e => setFormData({ ...formData, name: e.target.value })}
            error={errors.name}
            required
          />
        </div>

        <div>
          <label className="block text-xs font-semibold text-ink mb-1">Work Email</label>
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
          <label className="block text-xs font-semibold text-ink mb-1">Workspace / Channel Name (Optional)</label>
          <Input
            type="text"
            name="company"
            placeholder="e.g. TechPulse Media"
            value={formData.company}
            onChange={e => setFormData({ ...formData, company: e.target.value })}
          />
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
          <div>
            <label className="block text-xs font-semibold text-ink mb-1">Password</label>
            <Input
              type="password"
              name="password"
              placeholder="••••••••"
              value={formData.password}
              onChange={e => setFormData({ ...formData, password: e.target.value })}
              error={errors.password}
              required
            />
          </div>

          <div>
            <label className="block text-xs font-semibold text-ink mb-1">Confirm Password</label>
            <Input
              type="password"
              name="confirmPassword"
              placeholder="••••••••"
              value={formData.confirmPassword}
              onChange={e => setFormData({ ...formData, confirmPassword: e.target.value })}
              error={errors.confirmPassword}
              required
            />
          </div>
        </div>

        <div>
          <label className="flex items-start gap-2 cursor-pointer text-xs text-ink-2 mt-1">
            <input
              type="checkbox"
              checked={formData.termsAccepted}
              onChange={e => setFormData({ ...formData, termsAccepted: e.target.checked })}
              className="w-3.5 h-3.5 mt-0.5 text-accent rounded border-border focus:ring-accent"
            />
            <span>
              I agree to the{' '}
              <a href="#" className="text-accent underline">Terms of Service</a>{' '}
              and{' '}
              <a href="#" className="text-accent underline">Privacy Policy</a>.
            </span>
          </label>
          {errors.termsAccepted && (
            <p className="text-xs text-danger mt-1">{errors.termsAccepted}</p>
          )}
        </div>

        <Button
          type="submit"
          variant="primary"
          size="md"
          loading={isLoading}
          className="w-full mt-2"
        >
          Create Free Account
        </Button>
      </form>

      <div className="text-center pt-2">
        <p className="text-xs text-ink-3">
          Already have an account?{' '}
          <Link
            to="/login"
            className="font-semibold text-accent hover:underline"
          >
            Sign in
          </Link>
        </p>
      </div>
    </AuthLayout>
  );
};
