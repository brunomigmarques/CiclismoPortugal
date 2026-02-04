# Ciclismo Portugal v1.2.1 - Critical Hotfix

**Release Date:** February 4, 2026
**Version Code:** 6
**Build Type:** Emergency hotfix for closed testing

---

## 🚨 Critical Fixes

### Authentication Issues Resolved

**Problem in v1.2.0:**
- Google Sign-In failed in release builds
- Users had to re-register accounts
- AI features showed "need to sign in" despite being logged in

**Root Cause:**
ProGuard/R8 was obfuscating Firebase Auth classes and data models, breaking authentication in release builds.

**Fixed in v1.2.1:**
✅ Added comprehensive ProGuard rules to preserve:
- Firebase Auth classes (FirebaseUser, FirebaseAuth)
- User and FantasyTeam data models
- AuthService interface and implementation
- toUser() extension function for Firebase user conversion
- All Room entities and Hilt viewmodels

✅ Added diagnostic logging:
- getCurrentUser() now logs Firebase user and conversion
- refreshContext() logs user retrieval and team loading
- Better error messages for troubleshooting

---

## 📋 What Changed

### ProGuard Rules (app/proguard-rules.pro)

Added critical keep rules:
```proguard
# Keep ALL data classes and domain models
-keep class com.ciclismo.portugal.data.** { *; }
-keep class com.ciclismo.portugal.domain.** { *; }

# Keep User model (used by AuthService)
-keep class com.ciclismo.portugal.domain.model.User { *; }

# Keep FantasyTeam model (used by AiCoordinator)
-keep class com.ciclismo.portugal.domain.model.FantasyTeam { *; }

# Keep Firebase Auth classes
-keep class com.google.firebase.auth.FirebaseUser { *; }
-keep class com.google.firebase.auth.FirebaseAuth { *; }

# Keep AuthService implementations
-keep class com.ciclismo.portugal.data.remote.firebase.FirebaseAuthService { *; }

# Keep extension function (toUser conversion)
-keepclassmembers class FirebaseAuthService {
    private *** toUser(...);
}
```

### Debug Logging

Enhanced logging in:
- `FirebaseAuthService.getCurrentUser()` - Shows Firebase user → User conversion
- `AiCoordinator.refreshContext()` - Shows user and team data loading
- Better error diagnostics for production debugging

---

## 📤 Upload Instructions (Closed Testing)

### Step 1: Go to Play Console
[Google Play Console](https://play.google.com/console) → Ciclismo Portugal

### Step 2: Update Closed Testing Track
1. Navigate to **Testing** → **Closed testing**
2. Click **Create new release**
3. Upload: `app/build/outputs/bundle/release/app-release.aab`

### Step 3: Release Notes

**Portuguese:**
```
🔧 Hotfix v1.2.1 - Correção Crítica

Problemas Corrigidos:
• Login com Google agora funciona corretamente
• Autenticação reconhecida pelas funcionalidades AI
• Regras ProGuard atualizadas para preservar classes Firebase

Se tiveste problemas de login na v1.2.0:
1. Limpa os dados da app (Definições → Apps → Ciclismo Portugal → Limpar Dados)
2. Entra novamente com Google ou Email

Pedimos desculpa pelo inconveniente!
```

**English:**
```
🔧 Hotfix v1.2.1 - Critical Fix

Fixed Issues:
• Google Sign-In now works correctly
• Authentication properly recognized by AI features
• ProGuard rules updated to preserve Firebase classes

If you experienced login issues with v1.2.0:
1. Clear app data (Settings → Apps → Ciclismo Portugal → Clear Data)
2. Sign in again with Google or Email

We apologize for the inconvenience!
```

### Step 4: Promote to Testers
- Set rollout to **100%** for closed testing
- Testers will get auto-update within hours
- Monitor crash reports in Firebase Crashlytics

---

## 🧪 Testing Checklist

Before promoting to production, verify:

**Authentication:**
- ✅ Google Sign-In works
- ✅ Email/Password login works
- ✅ User data persists after app restart
- ✅ Profile shows correct email and name

**AI Features:**
- ✅ DS Assistant recognizes logged-in user
- ✅ Action buttons appear in chat
- ✅ "Need to sign in" error doesn't appear for authenticated users
- ✅ Team data loads correctly for AI context

**Data Persistence:**
- ✅ Fantasy team survives app restart
- ✅ User preferences maintained
- ✅ Previous user data not lost

---

## 🔍 Diagnostic Logs

To verify the fix is working, check logcat:

```bash
adb logcat | grep -E "FirebaseAuthService|AiCoordinator"
```

**Expected logs when user signs in:**
```
FirebaseAuthService: getCurrentUser: fbUser=abc123, email=user@example.com
FirebaseAuthService: getCurrentUser: converted user=abc123, email=user@example.com
AiCoordinator: refreshContext: currentUser=abc123, email=user@example.com
AiCoordinator: refreshContext: cachedTeam=team456, name=My Team
```

**If you see this, authentication is broken:**
```
FirebaseAuthService: getCurrentUser: fbUser=null, email=null
AiCoordinator: refreshContext: No current user!
```

---

## 📊 Version Comparison

| Version | Status | Authentication | AI Access | Release Type |
|---------|--------|---------------|-----------|--------------|
| v1.2.0 | ❌ Broken | Failed in release | Not working | Initial release |
| v1.2.1 | ✅ Fixed | Working | Working | Hotfix |

---

## 🚀 Next Steps

After testing confirms the fix:
1. **Promote to production** if all tests pass
2. **Update release notes** on Play Store listing
3. **Monitor** Firebase Crashlytics for 24-48 hours
4. **Gather feedback** from closed testers

---

## 📝 Technical Notes

**Why did this happen?**
- ProGuard/R8 aggressively optimizes release builds
- Firebase uses reflection to access user data
- Without explicit keep rules, reflection breaks
- Debug builds don't use ProGuard, so issue wasn't caught

**Prevention for future:**
- Always test critical flows (auth, payments) in release builds before publishing
- Add ProGuard rules preemptively for all reflection-based libraries
- Use Firebase Test Lab with release APKs
- Enable ProGuard mapping upload to Play Console for better crash reports

---

**File:** `app/build/outputs/bundle/release/app-release.aab` (16 MB)
**Ready to upload to Closed Testing track!**
