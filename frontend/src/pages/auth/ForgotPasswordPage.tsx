import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import { AuthLayout } from '../../components/auth/AuthLayout';
import { Input } from '../../components/ui/Input';
import { Button } from '../../components/ui/Button';
import { forgotPasswordSchema, type ForgotPasswordFormData } from '../../lib/validation';
import { CheckCircle2, ArrowLeft } from 'lucide-react';

export const ForgotPasswordPage: React.FC = () => {
  const [formData, setFormData] = useState<ForgotPasswordFormData>({ email: '' });
  const [errors, setErrors] = useState<Partial<Record<keyof ForgotPasswordFormData, string>>>({});
  const [isSubmitted, setIsSubmitted] = useState(false);
  const [isLoading, setIsLoading] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    const validation = forgotPasswordSchema.safeParse(formData);
    if (!validation.success) {
      setErrors({ email: validation.error.issues[0]?.message });
      return;
    }

    setErrors({});
    setIsLoading(true);
    // Simulate reset link dispatch
    await new Promise(resolve => setTimeout(resolve, 800));
    setIsLoading(false);
    setIsSubmitted(true);
  };

  return (
    <AuthLayout
      title="Reset your password"
      subtitle="Enter your verified email and we'll send a secure password reset link."
    >
      {isSubmitted ? (
        <div className="space-y-4 text-center py-4">
          <div className="w-12 h-12 rounded-full bg-success-soft text-success flex items-center justify-center mx-auto">
            <CheckCircle2 className="w-6 h-6" />
          </div>
          <div>
            <h3 className="text-base font-bold text-ink">Check your inbox</h3>
            <p className="text-xs text-ink-3 mt-1 max-w-xs mx-auto">
              We sent a password reset link to{' '}
              <span className="font-semibold text-ink-2">{formData.email}</span>.
            </p>
          </div>
          <div className="pt-2">
            <Link to="/login">
              <Button variant="secondary" size="sm" className="w-full">
                Back to Sign In
              </Button>
            </Link>
          </div>
        </div>
      ) : (
        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="block text-xs font-semibold text-ink mb-1.5">Account Email</label>
            <Input
              type="email"
              name="email"
              placeholder="you@company.com"
              value={formData.email}
              onChange={e => setFormData({ email: e.target.value })}
              error={errors.email}
              required
            />
          </div>

          <Button
            type="submit"
            variant="primary"
            size="md"
            loading={isLoading}
            className="w-full mt-2"
          >
            Send Reset Link
          </Button>

          <div className="text-center pt-2">
            <Link
              to="/login"
              className="inline-flex items-center gap-1.5 text-xs font-semibold text-accent hover:underline"
            >
              <ArrowLeft className="w-3.5 h-3.5" />
              Back to Sign In
            </Link>
          </div>
        </form>
      )}
    </AuthLayout>
  );
};
