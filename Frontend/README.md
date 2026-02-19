# CloudVault Frontend

A modern, secure cloud document sharing platform built with React, TypeScript, and Tailwind CSS.

## 🚀 Features

- **Modern UI/UX**: Professional dark-themed interface with smooth animations
- **Type-Safe**: Built with TypeScript for robust development
- **Responsive Design**: Works seamlessly across all devices
- **Component Library**: Reusable UI components following best practices
- **Routing**: Client-side routing with React Router
- **State Management**: Clean component architecture with hooks

## 🎨 Design System

The application follows a premium dark theme with:
- **Primary Color**: Graphite Black (#0F0F0F)
- **Accent Color**: Neon Green (#22C55E)
- **High Contrast**: Optimized for readability
- **Professional**: Security-focused aesthetic

See [temp-colors.md](./temp-colors.md) for complete color palette.

## 📁 Project Structure

```
src/
├── components/
│   ├── ui/              # Reusable UI components
│   │   ├── Button.tsx
│   │   ├── Input.tsx
│   │   ├── Card.tsx
│   │   ├── Modal.tsx
│   │   └── Alert.tsx
│   └── layout/          # Layout components
│       ├── Header.tsx
│       ├── Sidebar.tsx
│       └── Layout.tsx
├── pages/               # Page components
│   ├── LoginPage.tsx
│   ├── RegisterPage.tsx
│   ├── DashboardPage.tsx
│   ├── FilesPage.tsx
│   ├── UploadPage.tsx
│   ├── ActivityPage.tsx
│   ├── AccessControlPage.tsx
│   └── SettingsPage.tsx
├── types/               # TypeScript types
│   └── index.ts
├── utils/               # Utility functions
│   └── helpers.ts
├── App.tsx              # Main app component
├── main.tsx             # Entry point
└── index.css            # Global styles
```

## 🛠️ Tech Stack

- **Framework**: React 19.2.0
- **Language**: TypeScript 5.9.3
- **Styling**: Tailwind CSS 4.1.18
- **Build Tool**: Vite 7.3.1
- **Routing**: React Router DOM
- **Icons**: Lucide React
- **Linting**: ESLint 9

## 🚦 Getting Started

### Prerequisites

- Node.js (v18 or higher)
- npm or yarn

### Installation

```bash
# Install dependencies
npm install

# Start development server
npm run dev

# Build for production
npm run build

# Preview production build
npm run preview

# Run linter
npm run lint
```

## 📄 Available Pages

1. **Login** (`/login`) - User authentication
2. **Register** (`/register`) - New user registration
3. **Dashboard** (`/dashboard`) - Overview and statistics
4. **My Files** (`/files`) - File management with grid/list views
5. **Upload** (`/upload`) - Drag-and-drop file upload
6. **Activity Logs** (`/activity`) - Audit trail of all actions
7. **Access Control** (`/access-control`) - Permission management
8. **Settings** (`/settings`) - User preferences and security

## 🎯 Key Components

### UI Components

- **Button**: Multi-variant button with loading states
- **Input**: Form input with validation and icons
- **Card**: Container component with header/content sections
- **Modal**: Overlay dialog with backdrop
- **Alert**: Notification component for success/error messages

### Layout Components

- **Header**: Top navigation with search and user menu
- **Sidebar**: Navigation menu with active state
- **Layout**: Main wrapper combining header and sidebar

## 🔒 Security Features

- JWT token authentication (ready to implement)
- Protected routes structure
- Input validation and sanitization
- Secure password requirements
- Activity logging for audit compliance

## 🎨 Customization

### Colors

Update colors in:
- `tailwind.config.js` - Tailwind theme
- `src/index.css` - CSS variables

### Typography

Modify font family in `tailwind.config.js` and `src/index.css`.

## 📦 Build Output

```bash
npm run build
```

Production-ready files will be in `dist/` directory.

## 🤝 Contributing

1. Follow the existing code structure
2. Use TypeScript for all new components
3. Maintain consistent styling with Tailwind
4. Write meaningful component and variable names
5. Keep components small and focused

## 📝 TODO

- [ ] Connect to backend API
- [ ] Implement actual authentication flow
- [ ] Add file upload to AWS S3
- [ ] Implement real-time notifications
- [ ] Add file preview functionality
- [ ] Implement advanced search and filters
- [ ] Add file versioning UI
- [ ] Create mobile-responsive improvements

## 📄 License

This project is part of the CloudVault secure document sharing platform.

---

Built with ❤️ using React + TypeScript + Tailwind CSS

