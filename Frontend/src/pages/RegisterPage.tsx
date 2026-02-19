import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Mail, Lock, User, Shield } from 'lucide-react';
import { Button, Input, Alert } from '../components/ui';
import { validateEmail, validatePassword } from '../utils/helpers';
import type { RegisterData } from '../types';

export const RegisterPage: React.FC = () => {
  const navigate = useNavigate();
  const [formData, setFormData] = useState<RegisterData>({
    email: '',
    name: '',
    password: '',
    confirmPassword: '',
  });
  const [errors, setErrors] = useState<Partial<RegisterData>>({});
  const [isLoading, setIsLoading] = useState(false);
  const [apiError, setApiError] = useState<string>('');

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
    setErrors((prev) => ({ ...prev, [name]: '' }));
    setApiError('');
  };

  const validate = (): boolean => {
    const newErrors: Partial<RegisterData> = {};

    if (!formData.name) {
      newErrors.name = 'Name is required';
    } else if (formData.name.length < 2) {
      newErrors.name = 'Name must be at least 2 characters';
    }

    if (!formData.email) {
      newErrors.email = 'Email is required';
    } else if (!validateEmail(formData.email)) {
      newErrors.email = 'Invalid email format';
    }

    if (!formData.password) {
      newErrors.password = 'Password is required';
    } else {
      const passwordValidation = validatePassword(formData.password);
      if (!passwordValidation.isValid) {
        newErrors.password = passwordValidation.errors[0];
      }
    }

    if (!formData.confirmPassword) {
      newErrors.confirmPassword = 'Please confirm your password';
    } else if (formData.password !== formData.confirmPassword) {
      newErrors.confirmPassword = 'Passwords do not match';
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!validate()) return;

    setIsLoading(true);

    try {
      // TODO: Replace with actual API call
      await new Promise((resolve) => setTimeout(resolve, 1500));

      // Mock success - implement actual API call later
      console.log('Registration attempt:', formData);

      // Navigate to login or dashboard
      navigate('/login');
    } catch {
      setApiError('Registration failed. Please try again.');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-background flex items-center justify-center p-4">
      <div className="w-full max-w-md">
        {/* Logo Section */}
        <div className="text-center mb-8">
          <div className="inline-flex items-center justify-center w-16 h-16 bg-accent rounded-2xl mb-4">
            <Shield className="w-8 h-8 text-primary" />
          </div>
          <h1 className="text-3xl font-bold text-text-primary mb-2">
            Create Account
          </h1>
          <p className="text-text-secondary">
            Join CloudVault for secure document sharing
          </p>
        </div>

        {/* Register Form */}
        <div className="bg-card rounded-xl border border-border p-8 shadow-2xl">
          {apiError && (
            <Alert
              type="error"
              message={apiError}
              onClose={() => setApiError('')}
              className="mb-6"
            />
          )}

          <form onSubmit={handleSubmit} className="space-y-5">
            <Input
              label="Full Name"
              type="text"
              name="name"
              placeholder="John Doe"
              value={formData.name}
              onChange={handleChange}
              error={errors.name}
              icon={<User className="w-5 h-5" />}
              autoComplete="name"
            />

            <Input
              label="Email Address"
              type="email"
              name="email"
              placeholder="you@example.com"
              value={formData.email}
              onChange={handleChange}
              error={errors.email}
              icon={<Mail className="w-5 h-5" />}
              autoComplete="email"
            />

            <Input
              label="Password"
              type="password"
              name="password"
              placeholder="Create a strong password"
              value={formData.password}
              onChange={handleChange}
              error={errors.password}
              icon={<Lock className="w-5 h-5" />}
              autoComplete="new-password"
              helperText="At least 8 characters with uppercase, lowercase, and number"
            />

            <Input
              label="Confirm Password"
              type="password"
              name="confirmPassword"
              placeholder="Re-enter your password"
              value={formData.confirmPassword}
              onChange={handleChange}
              error={errors.confirmPassword}
              icon={<Lock className="w-5 h-5" />}
              autoComplete="new-password"
            />

            <div className="flex items-start gap-2 text-sm">
              <input
                type="checkbox"
                required
                className="w-4 h-4 mt-0.5 rounded border-border bg-secondary text-accent focus:ring-accent focus:ring-offset-0"
              />
              <span className="text-text-secondary">
                I agree to the{' '}
                <Link to="/terms" className="text-accent hover:text-highlight">
                  Terms of Service
                </Link>{' '}
                and{' '}
                <Link to="/privacy" className="text-accent hover:text-highlight">
                  Privacy Policy
                </Link>
              </span>
            </div>

            <Button
              type="submit"
              variant="primary"
              size="lg"
              isLoading={isLoading}
              className="w-full"
            >
              Create Account
            </Button>
          </form>

          {/* Divider */}
          <div className="relative my-6">
            <div className="absolute inset-0 flex items-center">
              <div className="w-full border-t border-border"></div>
            </div>
            <div className="relative flex justify-center text-sm">
              <span className="px-4 bg-card text-text-muted">
                Already have an account?
              </span>
            </div>
          </div>

          {/* Login Link */}
          <Link to="/login">
            <Button variant="secondary" size="lg" className="w-full">
              Sign In Instead
            </Button>
          </Link>
        </div>

        {/* Footer */}
        <p className="text-center text-sm text-text-muted mt-8">
          Protected by enterprise-grade encryption
        </p>
      </div>
    </div>
  );
};
