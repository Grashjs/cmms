# CMMS Mobile-Friendly Improvement Plan

**Date:** 2026-01-29  
**Status:** Draft  
**Priority:** High (user-reported issues with overlapping tabs)

---

## Executive Summary

The Atlas CMMS frontend has limited mobile responsiveness. While the main layout (`ExtendedSidebarLayout`) has basic responsive handling, the content components—particularly `MultipleTabsLayout`—have hardcoded spacing and no mobile adaptations, causing tabs to overlap on small screens.

---

## Issues Identified

### 1. `MultipleTabsLayout.tsx` (Critical)

**File:** `src/content/own/components/MultipleTabsLayout.tsx`

| Problem | Code | Impact |
|---------|------|--------|
| Fixed large padding | `padding: 0 ${theme.spacing(8)}` (64px) | Eats horizontal space on mobile |
| Fixed max-width | `max-width: 82%` | Doesn't adapt to screen size |
| Fixed margins | `mr: 4`, `mx: 4` (32px) | Too large for mobile |
| No flex wrap | `display: flex` without wrap | Tabs and buttons compete for space |
| No responsive breakpoints | No `useMediaQuery` or `@media` queries | No mobile adaptation |

**Affected Pages (all use MultipleTabsLayout):**
- Inventory (Parts, Sets)
- Assets (7 tabs: details, work-orders, parts, files, meters, downtimes, analytics)
- Locations
- People & Teams
- Preventive Maintenance
- Categories
- Settings

### 2. Asset Details Page (7 Tabs)

**File:** `src/content/own/Assets/Show/index.tsx`

The Asset details page has **7 tabs**, which is too many to display horizontally on mobile without scrolling or overflow issues.

### 3. Limited Responsive Design Overall

Only **20 instances** of `useMediaQuery` or breakpoints in the entire frontend codebase. Most components use fixed spacing.

---

## Recommended Fixes

### Phase 1: Fix `MultipleTabsLayout` (High Priority)

```tsx
// BEFORE
const TabsContainerWrapper = styled(Box)(
  ({ theme }) => `
      padding: 0 ${theme.spacing(8)};
      max-width: 82%;
      // ...
  `
);

// AFTER
const TabsContainerWrapper = styled(Box)(
  ({ theme }) => `
      padding: 0 ${theme.spacing(2)};
      max-width: 100%;
      
      ${theme.breakpoints.up('sm')} {
        padding: 0 ${theme.spacing(4)};
      }
      
      ${theme.breakpoints.up('md')} {
        padding: 0 ${theme.spacing(8)};
        max-width: 82%;
      }
      
      .MuiTabs-root {
        height: 44px;
        min-height: 44px;
      }
      
      // Ensure scrollable tabs work on mobile
      .MuiTabs-scrollableX {
        overflow-x: auto !important;
        -webkit-overflow-scrolling: touch;
      }
      // ...
  `
);
```

**Additional Changes:**

```tsx
// Make the layout stack on mobile
<Box 
  display="flex" 
  flexDirection={{ xs: 'column', md: 'row' }}
  justifyContent="space-between"
>
  <TabsContainerWrapper sx={{ width: { xs: '100%', md: 'auto' } }}>
    <Tabs
      onChange={handleTabsChange}
      value={currentTab}
      variant="scrollable"
      scrollButtons="auto"
      allowScrollButtonsMobile  // ADD THIS
      textColor="primary"
      indicatorColor="primary"
    >
      {tabs.map((tab) => (
        <Tab key={tab.value} label={tab.label} value={tab.value} />
      ))}
    </Tabs>
  </TabsContainerWrapper>
  <Stack 
    direction="row" 
    spacing={1} 
    sx={{ 
      mr: { xs: 2, md: 4 }, 
      my: 1,
      justifyContent: { xs: 'flex-end', md: 'flex-start' }
    }}
  >
    {/* Action buttons */}
  </Stack>
</Box>

{/* Card with responsive margins */}
<Card
  variant="outlined"
  sx={{
    mx: { xs: 1, sm: 2, md: 4 }
  }}
>
  {children}
</Card>
```

### Phase 2: Add Global Mobile Utilities

Create a responsive utility hook:

```tsx
// src/hooks/useResponsive.ts
import { useTheme, useMediaQuery } from '@mui/material';

export const useResponsive = () => {
  const theme = useTheme();
  
  return {
    isMobile: useMediaQuery(theme.breakpoints.down('sm')),
    isTablet: useMediaQuery(theme.breakpoints.between('sm', 'md')),
    isDesktop: useMediaQuery(theme.breakpoints.up('md')),
  };
};
```

### Phase 3: Consider Tab Grouping for Asset Details

For pages with many tabs (like Asset Details with 7 tabs), consider:

1. **Dropdown menu on mobile** - Show current tab with dropdown to switch
2. **Tab grouping** - Group related tabs (e.g., "Data" = details+parts+files, "Activity" = work-orders+downtimes+meters)
3. **Bottom navigation** - Use MUI BottomNavigation for primary tabs on mobile

### Phase 4: Audit Other Components

Components to check and fix:
- `SettingsLayout.tsx`
- `AnalyticsLayout.tsx`
- `CategoriesLayout.tsx`
- All data tables (`@mui/x-data-grid`) - may need horizontal scroll
- Forms with side-by-side fields

---

## Implementation Order

| Priority | Task | Effort | Impact |
|----------|------|--------|--------|
| 1 | Fix `MultipleTabsLayout` responsive padding/margins | 1 hour | High |
| 2 | Add `allowScrollButtonsMobile` to all Tabs | 30 min | High |
| 3 | Make action buttons stack on mobile | 30 min | Medium |
| 4 | Create `useResponsive` hook | 30 min | Medium |
| 5 | Responsive Card margins across app | 1 hour | Medium |
| 6 | Audit and fix other layout components | 2 hours | Medium |
| 7 | Consider tab grouping for 7+ tab pages | 4 hours | Low |

---

## Testing Checklist

After fixes, test on:
- [ ] iPhone SE (375px width)
- [ ] iPhone 14 (390px width)
- [ ] iPad (768px width)
- [ ] Android phone (360px width)

Test pages:
- [ ] Inventory (Parts tab)
- [ ] Asset Details (all 7 tabs)
- [ ] Work Orders
- [ ] Locations
- [ ] Settings

---

## Quick Win: Immediate Fix

If you want a quick fix right now, here's the minimal change to `MultipleTabsLayout.tsx`:

```tsx
// Line 11-14: Change padding and max-width
const TabsContainerWrapper = styled(Box)(
  ({ theme }) => `
      padding: 0 ${theme.spacing(2)};
      margin-top: 2px;
      position: relative;
      bottom: -1px;
      width: 100%;
      overflow-x: auto;
      
      @media (min-width: ${theme.breakpoints.values.md}px) {
        padding: 0 ${theme.spacing(8)};
        max-width: 82%;
      }
      // ... rest of styles
  `
);
```

And add `allowScrollButtonsMobile` prop to the Tabs component (around line 119).

---

*Plan created by Jarvis | Ready for implementation*
