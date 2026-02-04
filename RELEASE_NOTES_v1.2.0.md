# Ciclismo Portugal - Release v1.2.0

**Release Date:** February 4, 2026
**Version Code:** 5
**APK Size:** 8.4 MB (Release) | 33 MB (Debug)

---

## 🎉 What's New

### DS Assistant - Intelligent Cycling Assistant

The **DS (Directeur Sportif) Assistant** is now fully functional with comprehensive AI-powered help throughout the app!

#### 🤖 **Smart AI Chat with Action Buttons**
- **Chat anywhere in the app** - Tap the floating button (bottom-right) to access your personal cycling assistant
- **Actionable suggestions** - AI responses now include **clickable action buttons** that navigate you directly to relevant screens
- **Guaranteed actions** - Every AI response includes at least one helpful action you can take
- **Vibrant button design** - Action buttons use distinct indigo color to stand out from message text

#### 🎯 **Contextual Suggestions**
Smart tips appear automatically based on your context:
- **No Captain Warning** - Reminds you to set a captain when you have cyclists but no captain selected
- **Incomplete Team Alert** - Warns when race deadline is approaching and your team isn't full (< 15 cyclists)
- **Transfer Penalty Tips** - Alerts you when pending transfers exceed free transfers
- **First Visit Tutorials** - Helpful tips when visiting screens for the first time
- **Wildcard Opportunities** - Suggests using wildcards when you have many pending transfers

#### 🧠 **Intelligent Keyword Detection**
The assistant understands simple keywords and responds with relevant actions:
- `"mercado"` → Navigate to Market
- `"minha equipa"` → Navigate to Team
- `"capitao"` → Navigate to Team or show Captain help
- `"calendario"` → Navigate to Calendar
- `"comprar"` / `"vender"` → Navigate to Market
- `"wildcard"` → Show Wildcard help or navigate to Team
- And many more...

#### 📱 **Global Overlay System**
- **Always accessible** - FAB button appears on all screens (not just Fantasy)
- **Automatic screen tracking** - Assistant knows your context without manual setup
- **Smooth animations** - 300-400ms transitions for a polished experience
- **Dismissible suggestions** - Close tips you don't need with cooldown periods

---

## 🔧 Technical Improvements

### Performance & Reliability

**AI Response System:**
- Four-layer action generation safety net ensures actions always appear
- Simple keyword detection (most reliable)
- Complex pattern matching for cyclist names and specific actions
- Topic-based contextual fallbacks
- Ultimate fallback guarantee (never returns empty actions)

**Architecture:**
- Global AI coordinator manages all contextual suggestions
- Automatic trigger evaluation based on user context
- Smart cooldown system prevents suggestion spam
- Premium/trial access gating for AI features

**Code Quality:**
- Comprehensive logging for debugging AI behavior
- Hilt dependency injection for clean architecture
- StateFlow reactive state management
- Kotlin Coroutines for async operations

### Build Optimizations
- **Release APK:** 8.4 MB (ProGuard/R8 optimized)
- **Debug APK:** 33 MB (with debugging symbols)
- Crashlytics mapping for better error tracking

---

## 🐛 Bug Fixes

### Critical Fixes
1. **Action buttons not appearing in chat** - Fixed overlay chat bubble to render action buttons
2. **Contextual suggestions not triggering** - Fixed team context loading (captain and team size)
3. **First-visit logic inverted** - Fixed screen visit tracking for tutorial triggers
4. **Action execution missing context** - Fixed user/team ID passing to action executor

### UI/UX Fixes
- Action buttons now use vibrant indigo color for better visibility
- "Acoes sugeridas:" label appears above action buttons for clarity
- Dismiss button (×) uses subtle styling to not distract from main actions
- Message text properly styled with distinct color from action buttons

---

## 📋 Known Limitations

### AI Behavior
- **Gemini AI dependency:** Action generation relies on Google Gemini API, which may occasionally not generate JSON blocks as expected. The app has comprehensive fallbacks to handle this.
- **7-day trial:** DS Assistant features require premium subscription after 7-day trial period

### Planned Improvements (Not in this release)
- **Phase 3:** UI Simplification
  - Market screen dialog consolidation
  - MyTeam screen wildcard display simplification
  - Fantasy Hub action card optimization
- **Phase 4:** Advanced Triggers
  - Idle detection (30s timeout)
  - Error-state triggers
  - Tutorial spotlight components

---

## 🔐 Privacy & Security

- **Privacy Policy updated** with Fantasy, AI, Ads, and Premium sections
- **Contact email:** app.cyclingai@gmail.com
- **Account deletion:** Available at GitHub Pages for Play Store compliance
- **Data handling:** Transparent data usage for AI features and analytics

---

## 📦 Installation

**Release APK Location:**
```
app/build/outputs/apk/release/app-release.apk
```

**Requirements:**
- Android 8.0 (API 26) or higher
- Internet connection for AI features
- ~20 MB free space

---

## 🧪 Testing

### Verified Functionality
✅ Action buttons appear in AI chat responses
✅ Simple keyword detection triggers correct actions
✅ Contextual suggestions appear on relevant screens
✅ Action execution navigates to correct destinations
✅ Trigger cooldowns prevent suggestion spam
✅ First-visit tutorials work correctly
✅ Premium/trial gating functions properly

### Test Cases
See [TEST_AI_ACTIONS.md](TEST_AI_ACTIONS.md) for comprehensive test scenarios.

---

## 📚 Documentation

New documentation files included:
- `DS_ASSISTANT_DEBUG.md` - Debugging guide for AI features
- `AI_ACTION_GENERATION_GUIDE.md` - Technical guide for action generation
- `QUICK_TEST_GUIDE.md` - Quick testing procedures
- `TEST_AI_ACTIONS.md` - Guaranteed test cases with expected results

---

## 🙏 Credits

**Developed with:**
- Claude Sonnet 4.5 (AI Assistant)
- Gemini 2.0 Flash (AI Chat Backend)
- Android Jetpack Compose
- Kotlin Coroutines & Flow
- Hilt Dependency Injection
- Firebase (Auth, Firestore, Crashlytics)

---

## 📝 Changelog Summary

```
v1.2.0 (Build 5) - February 4, 2026
-----------------------------------
✨ NEW: DS Assistant with actionable AI chat
✨ NEW: Contextual suggestions (captain, team, transfers)
✨ NEW: Simple keyword detection for common queries
✨ NEW: Global AI overlay accessible from all screens
✨ NEW: Vibrant action button styling
✨ NEW: Four-layer action generation safety net
🐛 FIX: Action buttons not rendering in chat
🐛 FIX: Team context not loading properly
🐛 FIX: First-visit trigger logic inverted
🐛 FIX: Action execution missing user context
📚 NEW: Comprehensive debugging and testing documentation
🔧 IMPROVE: Build optimization (8.4 MB release APK)
```

---

## 🚀 Next Steps

**For Users:**
1. Install the release APK
2. Test DS Assistant by tapping the FAB button
3. Try keywords like "mercado", "equipa", "capitao"
4. Provide feedback on AI suggestions

**For Developers:**
- Continue with Phase 3 (UI Simplification)
- Implement Phase 4 (Advanced Triggers)
- Monitor Crashlytics for any issues
- Gather user feedback on AI behavior

---

**Thank you for using Ciclismo Portugal!** 🚴‍♂️🇵🇹
