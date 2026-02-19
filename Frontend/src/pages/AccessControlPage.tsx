import React from 'react';
import { Shield, Users, Key, Lock } from 'lucide-react';
import { Card, CardHeader, CardTitle, CardContent, Button } from '../components/ui';

export const AccessControlPage: React.FC = () => {
  return (
    <div className="space-y-6 max-w-6xl mx-auto">
      {/* Page Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold text-text-primary mb-2">
            Access Control
          </h1>
          <p className="text-text-secondary">
            Manage file permissions and sharing settings
          </p>
        </div>
        <Button variant="primary" icon={<Key className="w-5 h-5" />}>
          Grant Access
        </Button>
      </div>

      {/* Permission Overview */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        <Card hoverable>
          <div className="flex items-center gap-4">
            <div className="p-3 bg-success/10 rounded-lg">
              <Shield className="w-6 h-6 text-success" />
            </div>
            <div>
              <p className="text-text-muted text-sm">Public Files</p>
              <h3 className="text-2xl font-bold text-text-primary">12</h3>
            </div>
          </div>
        </Card>

        <Card hoverable>
          <div className="flex items-center gap-4">
            <div className="p-3 bg-warning/10 rounded-lg">
              <Lock className="w-6 h-6 text-warning" />
            </div>
            <div>
              <p className="text-text-muted text-sm">Private Files</p>
              <h3 className="text-2xl font-bold text-text-primary">38</h3>
            </div>
          </div>
        </Card>

        <Card hoverable>
          <div className="flex items-center gap-4">
            <div className="p-3 bg-accent/10 rounded-lg">
              <Users className="w-6 h-6 text-accent" />
            </div>
            <div>
              <p className="text-text-muted text-sm">Shared With</p>
              <h3 className="text-2xl font-bold text-text-primary">24</h3>
            </div>
          </div>
        </Card>
      </div>

      {/* Shared Files */}
      <Card>
        <CardHeader>
          <CardTitle>Shared Files</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="text-center py-12 text-text-muted">
            <Shield className="w-16 h-16 mx-auto mb-4 opacity-30" />
            <p className="text-lg">Access control management coming soon</p>
            <p className="text-sm mt-2">
              This feature will allow you to manage permissions for shared files
            </p>
          </div>
        </CardContent>
      </Card>
    </div>
  );
};
