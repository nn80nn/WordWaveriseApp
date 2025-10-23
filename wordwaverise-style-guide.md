# WordWaverise Design System

> Comprehensive style guide for the WordWaverise application interface

---

## 🎨 Color Palette

### Primary Colors
```css
--primary-cyan: #00BCD4;
--primary-blue: #0288D1;
--primary-bright: #2196F3;
--gradient-primary: linear-gradient(90deg, #2196F3 0%, #00BCD4 100%);
--gradient-logo: linear-gradient(180deg, #4DD0E1 0%, #0277BD 100%);
```

### Neutral Colors
```css
--text-primary: #2C3E50;
--text-secondary: #546E7A;
--text-tertiary: #9E9E9E;
--text-placeholder: #B0BEC5;

--background-primary: #F5F7FA;
--background-secondary: #FFFFFF;
--background-light: #FAFBFC;

--border-light: #E0E0E0;
--border-medium: #BDBDBD;
```

### Semantic Colors
```css
--success: #4CAF50;
--error: #F44336;
--warning: #FF9800;
--info: #2196F3;
```

### Opacity Values
```css
--shadow-color: rgba(0, 0, 0, 0.08);
--shadow-hover: rgba(0, 0, 0, 0.12);
--button-shadow: rgba(0, 188, 212, 0.3);
--overlay: rgba(0, 0, 0, 0.5);
```

---

## 📝 Typography

### Font Family
```css
--font-primary: 'Inter', 'Poppins', 'Montserrat', -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
```

### Font Sizes
```css
--text-xs: 12px;
--text-sm: 13px;
--text-base: 14px;
--text-md: 16px;
--text-lg: 18px;
--text-xl: 20px;
--text-2xl: 24px;
--text-3xl: 28px;
--text-4xl: 36px;
--text-5xl: 42px;
```

### Font Weights
```css
--font-regular: 400;
--font-medium: 500;
--font-semibold: 600;
--font-bold: 700;
```

### Line Heights
```css
--leading-tight: 1.2;
--leading-normal: 1.5;
--leading-relaxed: 1.75;
```

### Typography Scale
```css
/* Logo */
.logo-text {
  font-size: var(--text-2xl);
  font-weight: var(--font-semibold);
}

/* H1 - Main Heading */
.heading-1 {
  font-size: var(--text-4xl);
  font-weight: var(--font-bold);
  line-height: var(--leading-tight);
  color: var(--text-primary);
}

/* H2 - Section Heading */
.heading-2 {
  font-size: var(--text-2xl);
  font-weight: var(--font-semibold);
  line-height: var(--leading-tight);
  color: var(--text-primary);
}

/* H3 - Card Heading */
.heading-3 {
  font-size: var(--text-lg);
  font-weight: var(--font-semibold);
  line-height: var(--leading-normal);
  color: var(--text-primary);
}

/* Subtitle */
.subtitle {
  font-size: var(--text-md);
  font-weight: var(--font-regular);
  line-height: var(--leading-relaxed);
  color: var(--text-secondary);
}

/* Body */
.body {
  font-size: var(--text-base);
  font-weight: var(--font-regular);
  line-height: var(--leading-normal);
  color: var(--text-secondary);
}

/* Caption */
.caption {
  font-size: var(--text-sm);
  font-weight: var(--font-regular);
  line-height: var(--leading-normal);
  color: var(--text-tertiary);
}

/* Small */
.small {
  font-size: var(--text-xs);
  font-weight: var(--font-regular);
  color: var(--text-tertiary);
}
```

---

## 📐 Spacing System

### Base Unit: 8px

```css
--space-1: 4px;
--space-2: 8px;
--space-3: 12px;
--space-4: 16px;
--space-5: 20px;
--space-6: 24px;
--space-7: 28px;
--space-8: 32px;
--space-10: 40px;
--space-12: 48px;
--space-16: 64px;
--space-20: 80px;
```

### Usage Guidelines
- **Between sections**: 40-60px (`var(--space-10)` to `var(--space-16)`)
- **Between elements**: 16-24px (`var(--space-4)` to `var(--space-6)`)
- **Card padding**: 32-40px (`var(--space-8)` to `var(--space-10)`)
- **Gap between cards**: 20-24px (`var(--space-5)` to `var(--space-6)`)

---

## 🔘 Border Radius

```css
--radius-sm: 4px;
--radius-md: 8px;
--radius-lg: 12px;
--radius-xl: 16px;
--radius-2xl: 20px;
--radius-full: 9999px;
```

### Component Usage
- **Cards**: `var(--radius-xl)` or `var(--radius-2xl)`
- **Buttons**: `var(--radius-md)` or `var(--radius-lg)`
- **Input fields**: `var(--radius-md)`
- **Icons/Avatars**: `var(--radius-full)` for circular

---

## 💫 Shadows

```css
--shadow-sm: 0 1px 2px rgba(0, 0, 0, 0.05);
--shadow-base: 0 2px 8px rgba(0, 0, 0, 0.08);
--shadow-md: 0 4px 20px rgba(0, 0, 0, 0.08);
--shadow-lg: 0 8px 30px rgba(0, 0, 0, 0.12);
--shadow-xl: 0 12px 40px rgba(0, 0, 0, 0.15);

--shadow-button: 0 2px 8px rgba(0, 188, 212, 0.3);
--shadow-button-hover: 0 4px 12px rgba(0, 188, 212, 0.4);
```

### Component Usage
- **Cards (rest)**: `var(--shadow-md)`
- **Cards (hover)**: `var(--shadow-lg)`
- **Buttons**: `var(--shadow-button)`
- **Inputs (focus)**: `var(--shadow-base)`

---

## 🎯 Components

### Buttons

#### Primary Button
```css
.btn-primary {
  background: linear-gradient(90deg, #2196F3 0%, #00BCD4 100%);
  color: white;
  font-size: var(--text-md);
  font-weight: var(--font-medium);
  padding: 14px 32px;
  border-radius: var(--radius-lg);
  border: none;
  box-shadow: var(--shadow-button);
  transition: all 0.3s ease;
  cursor: pointer;
  height: 48px;
  min-height: 48px;
}

.btn-primary:hover {
  box-shadow: var(--shadow-button-hover);
  transform: translateY(-1px);
  filter: brightness(1.05);
}

.btn-primary:active {
  transform: translateY(0);
}
```

#### OAuth Button
```css
.btn-oauth {
  background: white;
  border: 1px solid var(--border-light);
  color: var(--text-primary);
  font-size: var(--text-base);
  font-weight: var(--font-medium);
  padding: 12px 24px;
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-sm);
  transition: all 0.2s ease;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  height: 48px;
}

.btn-oauth:hover {
  border-color: var(--border-medium);
  box-shadow: var(--shadow-base);
}
```

#### Button Full Width
```css
.btn-full {
  width: 100%;
}
```

### Cards

#### Base Card
```css
.card {
  background: var(--background-secondary);
  border-radius: var(--radius-xl);
  padding: var(--space-8);
  box-shadow: var(--shadow-md);
  transition: all 0.3s ease;
}

.card:hover {
  box-shadow: var(--shadow-lg);
  transform: translateY(-4px);
}
```

#### Feature Card
```css
.card-feature {
  background: white;
  border-radius: var(--radius-xl);
  padding: 32px 24px;
  box-shadow: var(--shadow-md);
  text-align: center;
  transition: all 0.3s ease;
  cursor: pointer;
}

.card-feature:hover {
  box-shadow: var(--shadow-lg);
  transform: translateY(-4px);
}

.card-feature-icon {
  width: 40px;
  height: 40px;
  color: var(--primary-cyan);
  margin: 0 auto var(--space-4);
}

.card-feature-title {
  font-size: var(--text-lg);
  font-weight: var(--font-semibold);
  color: var(--text-primary);
  margin-bottom: var(--space-2);
}

.card-feature-description {
  font-size: var(--text-sm);
  color: var(--text-tertiary);
  line-height: var(--leading-relaxed);
}
```

### Input Fields

```css
.input {
  width: 100%;
  height: 48px;
  padding: 12px 16px;
  font-size: var(--text-base);
  color: var(--text-primary);
  background: white;
  border: 1px solid var(--border-light);
  border-radius: var(--radius-md);
  transition: all 0.2s ease;
  outline: none;
}

.input::placeholder {
  color: var(--text-placeholder);
}

.input:focus {
  border-color: var(--primary-cyan);
  box-shadow: 0 0 0 3px rgba(0, 188, 212, 0.1);
}

.input:hover {
  border-color: var(--border-medium);
}
```

### Navigation

#### Bottom Navigation
```css
.bottom-nav {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  background: white;
  border-top: 1px solid var(--border-light);
  padding: 8px 0;
  display: flex;
  justify-content: space-around;
  align-items: center;
  box-shadow: 0 -2px 10px rgba(0, 0, 0, 0.05);
}

.nav-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  padding: 8px 16px;
  color: var(--text-tertiary);
  text-decoration: none;
  transition: color 0.2s ease;
  cursor: pointer;
}

.nav-item-icon {
  width: 24px;
  height: 24px;
}

.nav-item-label {
  font-size: var(--text-xs);
  font-weight: var(--font-medium);
}

.nav-item.active {
  color: var(--primary-cyan);
}

.nav-item:hover {
  color: var(--primary-blue);
}
```

### Logo

```css
.logo {
  display: flex;
  align-items: center;
  gap: 12px;
  text-decoration: none;
}

.logo-icon {
  width: 40px;
  height: 40px;
  background: linear-gradient(180deg, #4DD0E1 0%, #0277BD 100%);
  /* Wave shape - use SVG or custom element */
}

.logo-text {
  font-size: var(--text-2xl);
  font-weight: var(--font-semibold);
  background: linear-gradient(90deg, #2196F3 0%, #00BCD4 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}
```

---

## 🎭 Effects & Animations

### Transitions
```css
--transition-fast: 0.15s ease;
--transition-base: 0.2s ease;
--transition-slow: 0.3s ease;
--transition-slower: 0.5s ease;
```

### Hover Effects
```css
.hover-lift {
  transition: transform var(--transition-slow);
}

.hover-lift:hover {
  transform: translateY(-4px);
}

.hover-scale {
  transition: transform var(--transition-base);
}

.hover-scale:hover {
  transform: scale(1.05);
}
```

### Loading States
```css
@keyframes pulse {
  0%, 100% {
    opacity: 1;
  }
  50% {
    opacity: 0.5;
  }
}

.loading {
  animation: pulse 2s cubic-bezier(0.4, 0, 0.6, 1) infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

.spinner {
  animation: spin 1s linear infinite;
}
```

---

## 📱 Responsive Breakpoints

```css
--breakpoint-sm: 640px;
--breakpoint-md: 768px;
--breakpoint-lg: 1024px;
--breakpoint-xl: 1280px;
--breakpoint-2xl: 1536px;
```

### Media Queries
```css
/* Mobile First Approach */

/* Small devices (landscape phones) */
@media (min-width: 640px) {
  /* ... */
}

/* Medium devices (tablets) */
@media (min-width: 768px) {
  /* ... */
}

/* Large devices (desktops) */
@media (min-width: 1024px) {
  /* ... */
}

/* Extra large devices */
@media (min-width: 1280px) {
  /* ... */
}
```

---

## 🏗️ Layout

### Container
```css
.container {
  width: 100%;
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 var(--space-4);
}

@media (min-width: 768px) {
  .container {
    padding: 0 var(--space-8);
  }
}
```

### Grid System
```css
.grid {
  display: grid;
  gap: var(--space-6);
}

/* Mobile: 1 column */
.grid-cols-1 {
  grid-template-columns: repeat(1, 1fr);
}

/* Tablet: 2 columns */
@media (min-width: 768px) {
  .grid-cols-md-2 {
    grid-template-columns: repeat(2, 1fr);
  }
}

/* Desktop: 3 columns */
@media (min-width: 1024px) {
  .grid-cols-lg-3 {
    grid-template-columns: repeat(3, 1fr);
  }
}
```

### Flexbox Utilities
```css
.flex {
  display: flex;
}

.flex-col {
  flex-direction: column;
}

.items-center {
  align-items: center;
}

.justify-center {
  justify-content: center;
}

.justify-between {
  justify-content: space-between;
}

.gap-2 { gap: var(--space-2); }
.gap-4 { gap: var(--space-4); }
.gap-6 { gap: var(--space-6); }
.gap-8 { gap: var(--space-8); }
```

---

## 🎨 Decorative Elements

### Wave Pattern
```css
.wave-decoration {
  position: absolute;
  top: 0;
  right: 0;
  width: 50%;
  height: 100%;
  opacity: 0.05;
  pointer-events: none;
  /* Use SVG with curved, intersecting lines */
}
```

### Accent Highlight
```css
.text-accent {
  color: var(--primary-cyan);
}

.text-gradient {
  background: linear-gradient(90deg, #2196F3 0%, #00BCD4 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}
```

### Divider
```css
.divider {
  display: flex;
  align-items: center;
  text-align: center;
  color: var(--text-tertiary);
  font-size: var(--text-sm);
  margin: var(--space-6) 0;
}

.divider::before,
.divider::after {
  content: '';
  flex: 1;
  border-bottom: 1px solid var(--border-light);
}

.divider::before {
  margin-right: var(--space-4);
}

.divider::after {
  margin-left: var(--space-4);
}
```

---

## ♿ Accessibility

### Focus States
```css
*:focus-visible {
  outline: 2px solid var(--primary-cyan);
  outline-offset: 2px;
}

button:focus-visible,
a:focus-visible {
  outline: 2px solid var(--primary-cyan);
  outline-offset: 2px;
}
```

### Color Contrast
- Ensure minimum contrast ratio of 4.5:1 for normal text
- Ensure minimum contrast ratio of 3:1 for large text (18px+)
- All interactive elements must have visible focus states

### Touch Targets
- Minimum touch target size: 48x48px
- Adequate spacing between interactive elements (at least 8px)

---

## 📋 Design Principles

1. **Airiness**: Use generous white space between elements
2. **Hierarchy**: Clear visual distinction by importance
3. **Consistency**: Unified design language across all screens
4. **Accessibility**: High contrast text, proper touch target sizes
5. **Minimalism**: Only necessary elements, no visual noise
6. **Flow**: Rounded corners, gradients, wave-like forms
7. **Professionalism**: Clean, modern, trustworthy appearance

---

## 🔗 Icon Guidelines

### Icon Style
- **Type**: Outlined (line icons)
- **Stroke width**: 2px
- **Size**: 24px standard, 32-40px for feature cards
- **Color**: Use theme colors (primary cyan/blue)

### Icon Libraries
Recommended: Lucide Icons, Heroicons, Feather Icons

---

## 📝 Usage Examples

### Login Screen
```html
<div class="container">
  <div class="card" style="max-width: 400px; margin: 0 auto;">
    <div class="logo" style="justify-content: center; margin-bottom: 32px;">
      <!-- Logo SVG -->
      <span class="logo-text">WordWaverise</span>
    </div>
    
    <h1 class="heading-2" style="text-align: center;">Welcome Back</h1>
    <p class="body" style="text-align: center; margin-bottom: 32px;">Login to your account</p>
    
    <input type="email" class="input" placeholder="Email Address" style="margin-bottom: 16px;">
    <input type="password" class="input" placeholder="Password" style="margin-bottom: 8px;">
    
    <a href="#" class="caption" style="display: block; text-align: right; margin-bottom: 24px;">Forgot Password?</a>
    
    <button class="btn-primary btn-full">Login</button>
    
    <div class="divider">OR</div>
    
    <button class="btn-oauth btn-full">
      <!-- Google Icon -->
      Continue with Google
    </button>
  </div>
</div>
```

### Feature Cards Grid
```html
<div class="container">
  <h1 class="heading-1" style="text-align: center;">Unlock Your Story's Full Potential</h1>
  <p class="subtitle" style="text-align: center; margin-bottom: 48px;">
    Crafting narratives with clarity and <span class="text-accent">flow</span>.
  </p>
  
  <div class="grid grid-cols-1 grid-cols-md-2 grid-cols-lg-3">
    <div class="card-feature">
      <div class="card-feature-icon"><!-- Icon SVG --></div>
      <h3 class="card-feature-title">Seamless Writing</h3>
      <p class="card-feature-description">Create ipsum writing sit conkey saind domey eitois</p>
    </div>
    
    <div class="card-feature">
      <div class="card-feature-icon"><!-- Icon SVG --></div>
      <h3 class="card-feature-title">Creative Flow</h3>
      <p class="card-feature-description">Cre ew ipsum ailing sit simkey saind demoy ditois</p>
    </div>
    
    <div class="card-feature">
      <div class="card-feature-icon"><!-- Icon SVG --></div>
      <h3 class="card-feature-title">Elevate Your Words</h3>
      <p class="card-feature-description">Litere polun wille sit commu ni rivini domoy eftolis</p>
    </div>
  </div>
</div>
```

---

## 🎯 Version

**Style Guide Version**: 1.0.0  
**Last Updated**: October 24, 2025  
**Design System**: WordWaverise

---

## 📚 Resources

- **Figma**: [Design File Link]
- **Component Library**: [Storybook Link]
- **Brand Guidelines**: [Brand Guide Link]

---

*This style guide should be used as the single source of truth for all WordWaverise interface development.*
