# UX Audit & Improvement Plan - Ciclismo Portugal

## Executive Summary

This document outlines a comprehensive UX audit of the Ciclismo Portugal app, identifying usability issues and proposing improvements to enhance user engagement across all implemented features.

---

## Part 1: Critical Issues (Priority: HIGH)

### 1.1 Calendar Screen - Remove Divided Layout
**Current State:** Calendar screen has split/divided sections that fragment the user experience.

**Problem:**
- Divided layout makes navigation confusing
- Users cannot see full calendar at once
- Disrupts the flow of browsing events

**Solution:**
- Implement single unified calendar view
- Events displayed as continuous scrollable list under calendar
- Remove any tab/segment controls that divide content
- Keep month navigation simple and intuitive

**Files to modify:**
- `CalendarScreen.kt`
- `CalendarViewModel.kt`

---

### 1.2 Onboarding Flow Optimization
**Current State:** 6-step onboarding wizard that can feel lengthy.

**Problems:**
- Too many steps before seeing app content
- Users may abandon before completing
- Skip button not prominent enough

**Solutions:**
- Reduce to 3-4 essential steps (combine related choices)
- Add progress indicator showing steps remaining
- Make "Skip" more visible but not distracting
- Allow completing preferences later in Profile
- Show preview of app benefits at each step

---

### 1.3 Fantasy Game Entry Point Confusion
**Current State:** "Apostas" tab name may not be clear to all users.

**Problems:**
- "Apostas" (Bets) could be misunderstood as gambling
- New users don't immediately understand it's a fantasy game
- Hub screen has too many options for new users

**Solutions:**
- Consider renaming to "Fantasy" or "Jogo Fantasy"
- Add subtitle: "Cria a tua equipa de ciclismo"
- Simplify hub for first-time users with guided path
- Progressive disclosure: show advanced features only after team creation

---

## Part 2: Navigation & Information Architecture

### 2.1 Bottom Navigation Optimization
**Current State:** 4 tabs - News, Home (Provas), Calendar, Apostas

**Recommendations:**
| Current | Proposed | Rationale |
|---------|----------|-----------|
| News | News | Keep as entry point |
| Home (Provas) | Eventos | Clearer label |
| Calendar | Calendario | Keep |
| Apostas | Fantasy | Clearer purpose |

**Additional improvements:**
- Add badge indicators for:
  - Unread news count
  - Upcoming events this week
  - Fantasy game alerts (transfers needed, race starting)
- Consistent icon sizing and active states

### 2.2 Deep Linking & Navigation Shortcuts
**Problems:**
- No quick access to frequently used features
- AI Assistant buried in Fantasy Hub
- Profile access requires multiple taps from some screens

**Solutions:**
- Add floating action button (FAB) pattern consistently:
  - News: Quick filter FAB
  - Events: Add to calendar FAB
  - Calendar: Today shortcut FAB
  - Fantasy: AI Assistant FAB (already implemented)
- Add pull-down quick actions on main screens
- Implement swipe gestures for common actions

---

## Part 3: Feature-Specific Improvements

### 3.1 News Screen Enhancements
**Current State:** News list with video carousel and daily tip.

**Improvements:**
- [ ] Add news categories/filters (Pro cycling, Local, BTT, etc.)
- [ ] Implement "Save for later" functionality
- [ ] Add share button on news cards
- [ ] Show reading time estimate
- [ ] Add "Related events" link when news mentions specific races
- [ ] Lazy load images for better performance
- [ ] Add skeleton loading states

### 3.2 Events (Provas) Screen Enhancements
**Current State:** List of local cycling events with filters.

**Improvements:**
- [ ] Add map view option (toggle between list/map)
- [ ] Show distance from user location
- [ ] Add "Interested" button for quick save
- [ ] Group events by weekend/week
- [ ] Add event countdown for upcoming events
- [ ] Show weather forecast for event day
- [ ] Add difficulty indicators more prominently
- [ ] Enable comparing multiple events side-by-side

### 3.3 Calendar Screen Redesign (HIGH PRIORITY)
**Current State:** Divided/segmented calendar view.

**New Design:**
```
┌─────────────────────────────┐
│  < Janeiro 2026 >           │  Month navigation
├─────────────────────────────┤
│  S  T  Q  Q  S  S  D        │
│  ·  ·  1  2  3  4  5        │  Calendar grid
│  6  7  8  9 10 11 12        │  (dots for events)
│ 13 14 15 16 17 18 19        │
│ 20 21 22 23 24 25 26        │
│ 27 28 29 30 31  ·  ·        │
├─────────────────────────────┤
│  Eventos em Janeiro         │  Section header
├─────────────────────────────┤
│  ┌─────────────────────┐    │
│  │ 🚴 Volta ao Algarve │    │  Event cards
│  │ 15-19 Jan · Estrada │    │  (continuous scroll)
│  └─────────────────────┘    │
│  ┌─────────────────────┐    │
│  │ 🏔️ BTT Serra...     │    │
│  │ 20 Jan · BTT        │    │
│  └─────────────────────┘    │
│         ...                 │
└─────────────────────────────┘
```

**Key changes:**
- Single scrollable view (no tabs/segments)
- Calendar compact at top, events below
- Tap date to filter events for that day
- Swipe calendar left/right for months
- Remove AI Top 3 from main view (move to event detail)

### 3.4 Fantasy Game UX Improvements

#### Team Creation Wizard
**Current:** 5-step wizard

**Improvements:**
- [ ] Add "Quick Pick" option for auto-generated team
- [ ] Show budget impact in real-time as selections made
- [ ] Add cyclist comparison feature during selection
- [ ] Show form indicators (trending up/down arrows)
- [ ] Enable drag-and-drop reordering
- [ ] Add undo/redo for selections

#### Market Screen
**Improvements:**
- [ ] Add "Watchlist" feature for tracking cyclists
- [ ] Show price change history graph
- [ ] Add "Similar cyclists" suggestions
- [ ] Filter by "Affordable" (within budget)
- [ ] Sort by form, price, points, ownership %
- [ ] Add bulk selection mode

#### My Team Screen
**Improvements:**
- [ ] Add formation visualization
- [ ] Show projected points for upcoming race
- [ ] Quick captain change (tap to toggle)
- [ ] Show transfer deadline countdown
- [ ] Add "Optimize" suggestion button
- [ ] Display team value trend

#### Leagues
**Improvements:**
- [ ] Add league chat/comments
- [ ] Show head-to-head comparison with rivals
- [ ] Add league notifications (someone passed you)
- [ ] Create mini-leagues from contacts
- [ ] Add league achievements/badges

### 3.5 AI Assistant Improvements
**Current State:** Chat-based assistant with action suggestions.

**Improvements:**
- [ ] Add voice input option
- [ ] Show confidence level for suggestions
- [ ] Add "Why?" explanation for recommendations
- [ ] Remember user preferences across sessions
- [ ] Proactive notifications ("Your captain has low form")
- [ ] Quick action buttons without typing
- [ ] Context-aware suggestions based on current screen

### 3.6 Profile & Settings
**Improvements:**
- [ ] Add achievement badges display
- [ ] Show activity summary (events attended, teams created)
- [ ] Add notification preferences granularity
- [ ] Enable theme customization (colors, dark mode toggle)
- [ ] Add data export option
- [ ] Show connected accounts status (Strava, Google)

---

## Part 4: Visual Design & Consistency

### 4.1 Color System Refinement
**Current:** Portuguese flag colors with category-specific accents.

**Recommendations:**
- Maintain primary green (#006600) for brand identity
- Use red sparingly (errors, important alerts only)
- Ensure sufficient contrast for accessibility (WCAG AA)
- Add semantic colors:
  - Success: Green
  - Warning: Amber
  - Error: Red
  - Info: Blue
- Consistent opacity levels across similar elements

### 4.2 Card Design Standardization
**Current:** Multiple card styles across screens.

**Standardize:**
- Consistent corner radius (12dp)
- Consistent elevation (4dp)
- Consistent padding (12-16dp)
- Consistent image placement (right side, 110dp width)
- Consistent color stripe (6dp, left side)
- Consistent gray background for images: `surfaceVariant.copy(alpha = 0.3f)`

### 4.3 Typography Hierarchy
**Ensure consistent usage:**
- Screen titles: titleLarge, Bold
- Section headers: titleMedium, SemiBold
- Card titles: titleMedium, Bold
- Body text: bodyMedium, Normal
- Captions: bodySmall, Normal
- Chips/badges: labelMedium, SemiBold

### 4.4 Icon Consistency
**Audit needed:**
- [ ] Use Material icons consistently
- [ ] Ensure all icons have content descriptions
- [ ] Standardize icon sizes (16dp small, 24dp normal, 32dp large)
- [ ] Use filled icons for selected states, outlined for unselected

---

## Part 5: Engagement & Retention Features

### 5.1 Gamification Elements
**Add:**
- [ ] Daily login streak tracking
- [ ] Achievement system with badges
- [ ] Points for app activity (not just fantasy)
- [ ] Leaderboard for most active users
- [ ] Seasonal challenges

### 5.2 Social Features
**Add:**
- [ ] Follow other users
- [ ] Activity feed showing friends' actions
- [ ] Share achievements to social media
- [ ] Invite friends with referral rewards
- [ ] Comment on events/news

### 5.3 Notifications Strategy
**Optimize:**
- [ ] Smart timing (not during sleep hours)
- [ ] Personalized based on preferences
- [ ] Action-oriented (tap to act, not just inform)
- [ ] Frequency caps to prevent fatigue
- [ ] Easy inline actions in notifications

### 5.4 Onboarding Progression
**Add:**
- [ ] First-week tutorial tips
- [ ] Feature discovery prompts (contextual)
- [ ] "Did you know?" tooltips
- [ ] Progress bar showing features explored
- [ ] Reward for completing setup

---

## Part 6: Performance & Polish

### 6.1 Loading States
**Implement consistently:**
- [ ] Skeleton screens for lists
- [ ] Shimmer effect for images
- [ ] Progress indicators for actions
- [ ] Optimistic updates for instant feedback

### 6.2 Error Handling
**Improve:**
- [ ] Friendly error messages in Portuguese
- [ ] Retry buttons on all error states
- [ ] Offline mode indicators
- [ ] Graceful degradation when features unavailable

### 6.3 Empty States
**Design for:**
- [ ] No events in selected filters
- [ ] No news available
- [ ] No team created yet
- [ ] No results yet
- [ ] Empty search results

### 6.4 Accessibility
**Ensure:**
- [ ] All interactive elements have min 48dp touch target
- [ ] Color is not the only indicator of state
- [ ] Screen reader compatibility
- [ ] Reduced motion option
- [ ] High contrast mode support

---

## Part 7: Implementation Priority

### Phase 1: Critical Fixes (Week 1-2)
1. **Calendar screen unification** - Remove divided layout
2. **Fix DS Assistant action errors** - Already done
3. **Consistent card backgrounds** - Already done
4. **UCI logo display** - Already done

### Phase 2: Core UX Improvements (Week 3-4)
1. Onboarding flow optimization
2. Fantasy hub simplification for new users
3. Navigation badges and indicators
4. Loading and error states

### Phase 3: Engagement Features (Week 5-6)
1. Achievement system
2. Notification optimization
3. Social sharing
4. Daily tips enhancement

### Phase 4: Polish & Delight (Week 7-8)
1. Animations and transitions
2. Micro-interactions
3. Haptic feedback
4. Sound effects (optional)

---

## Part 8: Metrics to Track

### Engagement Metrics
- Daily/Weekly/Monthly Active Users
- Session duration
- Screens per session
- Feature adoption rates
- Fantasy team creation rate
- News article read rate

### Retention Metrics
- Day 1, 7, 30 retention
- Churn rate by user segment
- Return user rate
- Premium conversion rate

### UX Quality Metrics
- Task completion rate (create team, find event)
- Error encounter rate
- Support ticket volume
- App store rating trends
- User satisfaction surveys

---

## Appendix: Screen Inventory

| Screen | Priority | Status |
|--------|----------|--------|
| CalendarScreen | HIGH | Needs redesign |
| OnboardingScreen | HIGH | Needs optimization |
| FantasyHubScreen | MEDIUM | Needs simplification |
| NewsScreen | MEDIUM | Needs categories |
| HomeScreen (Events) | MEDIUM | Needs map view |
| MyTeamScreen | LOW | Minor improvements |
| MarketScreen | LOW | Minor improvements |
| ProfileScreen | LOW | Minor improvements |

---

*Document created: February 2026*
*Last updated: February 2026*
*Version: 1.0*
