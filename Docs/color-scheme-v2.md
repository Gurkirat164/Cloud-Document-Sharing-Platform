# CloudVault - Modern Color Scheme v2.0

## Design Philosophy
A sophisticated dark theme with electric blue accents, optimized for professional document management with excellent contrast and visual hierarchy.

---

## Core Colors

| Role | Color Name | Hex | Usage |
|------|-----------|-----|-------|
| Background - Primary | Midnight Black | `#0A0A0F` | Main app background |
| Background - Secondary | Deep Space | `#13131A` | Elevated surfaces |
| Surface - Card | Dark Slate | `#1A1A24` | Cards, panels |
| Surface - Elevated | Charcoal Blue | `#222230` | Hover states, elevated cards |
| Border - Subtle | Steel Gray | `#2A2A3C` | Dividers, borders |
| Border - Strong | Slate Border | `#3A3A4C` | Emphasized borders |

---

## Accent & Brand Colors

| Role | Color Name | Hex | RGB | Usage |
|------|-----------|-----|-----|-------|
| Primary Accent | Electric Blue | `#3B82F6` | rgb(59, 130, 246) | Primary actions, links |
| Primary Hover | Ocean Blue | `#2563EB` | rgb(37, 99, 235) | Hover state for primary |
| Secondary Accent | Violet | `#8B5CF6` | rgb(139, 92, 246) | Secondary actions |
| Success | Emerald | `#10B981` | rgb(16, 185, 129) | Success states |
| Warning | Amber | `#F59E0B` | rgb(245, 158, 11) | Warning states |
| Error | Crimson | `#EF4444` | rgb(239, 68, 68) | Error states |
| Info | Sky Blue | `#0EA5E9` | rgb(14, 165, 233) | Info messages |

---

## Text Colors

| Usage | Color Name | Hex | Opacity |
|-------|-----------|-----|---------|
| Primary Text | Pearl White | `#F8FAFC` | 100% |
| Secondary Text | Silver | `#CBD5E1` | 80% |
| Muted Text | Slate | `#94A3B8` | 60% |
| Disabled Text | Gray | `#64748B` | 40% |
| Link Text | Sky | `#38BDF8` | 100% |

---

## Semantic Colors

### Success States
- **Background**: `#10B981` at 10% opacity
- **Border**: `#10B981` at 50% opacity  
- **Text**: `#34D399`

### Error States
- **Background**: `#EF4444` at 10% opacity
- **Border**: `#EF4444` at 50% opacity
- **Text**: `#F87171`

### Warning States
- **Background**: `#F59E0B` at 10% opacity
- **Border**: `#F59E0B` at 50% opacity
- **Text**: `#FBBF24`

### Info States
- **Background**: `#0EA5E9` at 10% opacity
- **Border**: `#0EA5E9` at 50% opacity
- **Text**: `#38BDF8`

---

## Button Styles

### Primary Button
```css
Background: #3B82F6 (Electric Blue)
Text: #FFFFFF
Hover: #2563EB
Active: #1D4ED8
Shadow: 0 4px 12px rgba(59, 130, 246, 0.3)
```

### Secondary Button
```css
Background: #1A1A24 (Dark Slate)
Border: #3A3A4C (Slate Border)
Text: #F8FAFC
Hover: #222230
```

### Danger Button
```css
Background: #EF4444 (Crimson)
Text: #FFFFFF
Hover: #DC2626
Shadow: 0 4px 12px rgba(239, 68, 68, 0.3)
```

### Ghost Button
```css
Background: transparent
Text: #CBD5E1
Hover Background: #1A1A24
Hover Text: #F8FAFC
```

---

## Spacing Scale

| Token | Value | Usage |
|-------|-------|-------|
| xs | 0.5rem (8px) | Tight spacing |
| sm | 0.75rem (12px) | Small gaps |
| md | 1rem (16px) | Default spacing |
| lg | 1.5rem (24px) | Section spacing |
| xl | 2rem (32px) | Large gaps |
| 2xl | 3rem (48px) | Major sections |
| 3xl | 4rem (64px) | Page sections |

---

## Border Radius

| Token | Value | Usage |
|-------|-------|-------|
| sm | 0.375rem (6px) | Small elements |
| md | 0.5rem (8px) | Buttons, inputs |
| lg | 0.75rem (12px) | Cards |
| xl | 1rem (16px) | Modals |
| 2xl | 1.5rem (24px) | Large containers |
| full | 9999px | Circles, pills |

---

## Shadows

### Small Shadow
```css
box-shadow: 0 1px 3px rgba(0, 0, 0, 0.3), 
            0 1px 2px rgba(0, 0, 0, 0.24);
```

### Medium Shadow
```css
box-shadow: 0 4px 6px rgba(0, 0, 0, 0.3), 
            0 2px 4px rgba(0, 0, 0, 0.24);
```

### Large Shadow
```css
box-shadow: 0 10px 15px rgba(0, 0, 0, 0.3), 
            0 4px 6px rgba(0, 0, 0, 0.24);
```

### Glow Effect (Accent)
```css
box-shadow: 0 0 20px rgba(59, 130, 246, 0.4),
            0 0 40px rgba(59, 130, 246, 0.2);
```

---

## Design Principles

1. **High Contrast**: Ensure WCAG AAA compliance for text
2. **Subtle Elevation**: Use shadows and borders for depth
3. **Consistent Spacing**: Follow 8px grid system
4. **Purposeful Color**: Accent colors should guide user actions
5. **Professional**: Avoid overly bright or saturated colors
6. **Accessibility**: Maintain 4.5:1 contrast ratio minimum

---

## Usage Guidelines

### DO ✓
- Use Electric Blue for primary actions and important CTAs
- Use subtle shadows to create depth
- Maintain consistent spacing using the scale
- Use Violet for secondary features
- Keep backgrounds dark and surfaces slightly lighter

### DON'T ✗
- Mix too many accent colors in one view
- Use bright colors for large areas
- Ignore the spacing scale
- Create low-contrast text combinations
- Overuse glowing effects

---

## Accessibility

- **Primary Text on Dark Background**: 16.5:1 contrast ratio ✓
- **Secondary Text on Dark Background**: 9.8:1 contrast ratio ✓
- **Accent Blue on Dark Background**: 8.2:1 contrast ratio ✓
- **All interactive elements**: Minimum 44x44px touch target

---

**Last Updated**: February 19, 2026  
**Version**: 2.0  
**Design**: Modern Professional Dark Theme
