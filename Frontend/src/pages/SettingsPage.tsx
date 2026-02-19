import React from 'react';
import { User, Shield, Bell, Database } from 'lucide-react';
import { Card, CardHeader, CardTitle, CardContent, Button, Input } from '../components/ui';

export const SettingsPage: React.FC = () => {
  return (
    <div className="space-y-6 max-w-4xl mx-auto">
      {/* Page Header */}
      <div>
        <h1 className="text-3xl font-bold text-text-primary mb-2">Settings</h1>
        <p className="text-text-secondary">
          Manage your account and preferences
        </p>
      </div>

      {/* Profile Settings */}
      <Card>
        <CardHeader>
          <div className="flex items-center gap-2">
            <User className="w-5 h-5 text-accent" />
            <CardTitle>Profile Settings</CardTitle>
          </div>
        </CardHeader>
        <CardContent className="space-y-4">
          <Input
            label="Full Name"
            type="text"
            placeholder="John Doe"
            defaultValue="John Doe"
          />
          <Input
            label="Email Address"
            type="email"
            placeholder="john@example.com"
            defaultValue="john@example.com"
          />
          <Button variant="primary">Save Changes</Button>
        </CardContent>
      </Card>

      {/* Security Settings */}
      <Card>
        <CardHeader>
          <div className="flex items-center gap-2">
            <Shield className="w-5 h-5 text-accent" />
            <CardTitle>Security Settings</CardTitle>
          </div>
        </CardHeader>
        <CardContent className="space-y-4">
          <Input
            label="Current Password"
            type="password"
            placeholder="Enter current password"
          />
          <Input
            label="New Password"
            type="password"
            placeholder="Enter new password"
          />
          <Input
            label="Confirm New Password"
            type="password"
            placeholder="Confirm new password"
          />
          <div className="flex items-center justify-between p-4 bg-secondary rounded-lg">
            <div>
              <h4 className="text-text-primary font-medium mb-1">
                Two-Factor Authentication
              </h4>
              <p className="text-text-muted text-sm">
                Add an extra layer of security
              </p>
            </div>
            <Button variant="secondary" size="sm">
              Enable
            </Button>
          </div>
          <Button variant="primary">Update Password</Button>
        </CardContent>
      </Card>

      {/* Notification Settings */}
      <Card>
        <CardHeader>
          <div className="flex items-center gap-2">
            <Bell className="w-5 h-5 text-accent" />
            <CardTitle>Notification Preferences</CardTitle>
          </div>
        </CardHeader>
        <CardContent className="space-y-3">
          {[
            'File uploads',
            'File shares',
            'Permission changes',
            'Security alerts',
          ].map((notification) => (
            <div
              key={notification}
              className="flex items-center justify-between p-3 bg-secondary rounded-lg"
            >
              <span className="text-text-primary">{notification}</span>
              <input
                type="checkbox"
                defaultChecked
                className="w-4 h-4 rounded border-border bg-card text-accent focus:ring-accent focus:ring-offset-0"
              />
            </div>
          ))}
        </CardContent>
      </Card>

      {/* Storage Management */}
      <Card>
        <CardHeader>
          <div className="flex items-center gap-2">
            <Database className="w-5 h-5 text-accent" />
            <CardTitle>Storage Management</CardTitle>
          </div>
        </CardHeader>
        <CardContent>
          <div className="mb-4">
            <div className="flex items-center justify-between mb-2">
              <span className="text-text-secondary">Storage Used</span>
              <span className="text-text-primary font-medium">
                2.4 GB / 10 GB
              </span>
            </div>
            <div className="w-full h-3 bg-background rounded-full overflow-hidden">
              <div
                className="h-full bg-accent rounded-full transition-all"
                style={{ width: '24%' }}
              />
            </div>
          </div>
          <Button variant="primary">Upgrade Storage</Button>
        </CardContent>
      </Card>

      {/* Danger Zone */}
      <Card className="border-error/50">
        <CardHeader>
          <CardTitle className="text-error">Danger Zone</CardTitle>
        </CardHeader>
        <CardContent className="space-y-3">
          <div className="flex items-center justify-between p-4 bg-secondary rounded-lg">
            <div>
              <h4 className="text-text-primary font-medium mb-1">
                Delete Account
              </h4>
              <p className="text-text-muted text-sm">
                Permanently delete your account and all data
              </p>
            </div>
            <Button variant="danger" size="sm">
              Delete
            </Button>
          </div>
        </CardContent>
      </Card>
    </div>
  );
};
