import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Mail, Lock, Shield } from 'lucide-react';
import { Button, Input, Alert } from '../components/ui';
import { validateEmail } from '../utils/helpers';
import type { LoginCredentials } from '../types';

export const LoginPage: React.FC = () => {
  const navigate = useNavigate();
  const [formData, setFormData] = useState<LoginCredentials>({
    email: '',
    password: '',
  });
  const [errors, setErrors] = useState<Partial<LoginCredentials>>({});
  const [isLoading, setIsLoading] = useState(false);
  const [apiError, setApiError] = useState<string>('');

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
    // Clear error for this field
    setErrors((prev) => ({ ...prev, [name]: '' }));
    setApiError('');
  };

  const validate = (): boolean => {
    const newErrors: Partial<LoginCredentials> = {};

    if (!formData.email) {
      newErrors.email = 'Email is required';
    } else if (!validateEmail(formData.email)) {
      newErrors.email = 'Invalid email format';
    }

    if (!formData.password) {
      newErrors.password = 'Password is required';
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
      console.log('Login attempt:', formData);

      // For now, just navigate to dashboard
      // In real implementation, store token and user data
      navigate('/dashboard');
    } catch {
      setApiError('Invalid credentials. Please try again.');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-background via-background-secondary to-background flex items-center justify-center p-6">
      <div className="w-full max-w-md">
        {/* Logo Section */}
        <div className="text-center mb-10">
          <div className="inline-flex items-center justify-center w-20 h-20 bg-gradient-to-br from-primary to-secondary rounded-2xl mb-6 shadow-2xl shadow-primary/30">
            <Shield className="w-10 h-10 text-white" />
          </div>
          <h1 className="text-4xl font-bold text-text-primary mb-3 tracking-tight">
            Welcome Back
          </h1>
          <p className="text-text-secondary text-lg">
            Sign in to access your secure documents
          </p>
        </div>

        {/* Login Form */}
        <div className="bg-surface rounded-2xl border-2 border-border-strong p-10 shadow-2xl backdrop-blur-xl">
          {apiError && (
            <Alert
              type="error"
              message={apiError}
              onClose={() => setApiError('')}
              className="mb-6"
            />
          )}

          <form onSubmit={handleSubmit} className="space-y-6">
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
              placeholder="Enter your password"
              value={formData.password}
              onChange={handleChange}
              error={errors.password}
              icon={<Lock className="w-5 h-5" />}
              autoComplete="current-password"
            />

            <div className="flex items-center justify-between text-sm">
              <label className="flex items-center gap-2 cursor-pointer group">
                <input
                  type="checkbox"
                  className="w-4 h-4 rounded border-2 border-border bg-surface text-primary focus:ring-2 focus:ring-primary focus:ring-offset-0 transition-all duration-200"
                />
                <span className="text-text-secondary group-hover:text-text-primary transition-colors">Remember me</span>
              </label>
              <Link
                to="/forgot-password"
                className="text-primary hover:text-primary-hover transition-colors font-semibold"
              >
                Forgot password?
              </Link>
            </div>

            <Button
              type="submit"
              variant="primary"
              size="lg"
              isLoading={isLoading}
              className="w-full"
            >
              Sign In
            </Button>
          </form>

          {/* Divider */}
          <div className="relative my-8">
            <div className="absolute inset-0 flex items-center">
              <div className="w-full border-t-2 border-border"></div>
            </div>
            <div className="relative flex justify-center text-sm">
              <span className="px-4 bg-surface text-text-muted font-medium">
                Don't have an account?
              </span>
            </div>
          </div>

          {/* Register Link */}
          <Link to="/register">
            <Button variant="secondary" size="lg" className="w-full">
              Create New Account
            </Button>
          </Link>
        </div>

        {/* Footer */}
        <p className="text-center text-sm text-text-muted mt-10 font-medium flex items-center justify-center gap-2">
          <Shield className="w-4 h-4" />
          Protected by enterprise-grade encryption
        </p>
      </div>
    </div>
  );
};
