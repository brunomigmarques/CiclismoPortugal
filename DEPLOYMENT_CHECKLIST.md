# Deployment Checklist - v1.2.1

## 📋 Pre-Upload Checklist

### ✅ Build & Test
- [x] Version incremented (v1.2.1, Build 6)
- [x] Release AAB built successfully
- [x] ProGuard rules verified
- [ ] Tested on physical device (release build)
- [ ] Verified authentication works
- [ ] Verified AI features work
- [ ] No crashes in 5-minute smoke test

### ✅ Code & Git
- [x] Code committed to main branch
- [x] Git tag created (v1.2.1)
- [x] Tag pushed to GitHub
- [ ] Release notes updated

### ✅ Backup
- [ ] Run backup script: `scripts\backup-release.bat 1.2.1`
- [ ] AAB saved to local folder
- [ ] AAB uploaded to cloud storage
- [ ] ProGuard mapping saved
- [ ] Keystore backup verified

---

## 📤 Upload to Play Console

### Step 1: Prepare Files
**Location:** `app/build/outputs/bundle/release/app-release.aab` (16 MB)

**Verify:**
```bash
# Check file exists and size is reasonable
dir app\build\outputs\bundle\release\app-release.aab
```

### Step 2: Upload to Closed Testing
1. Go to [Google Play Console](https://play.google.com/console)
2. Select: **Ciclismo Portugal**
3. Navigate: **Testing** → **Closed testing**
4. Click: **Create new release**
5. Upload: `app-release.aab`

### Step 3: Release Notes

**Copy this (Portuguese):**
```
🔧 Hotfix v1.2.1 - Correção Crítica

Problemas Corrigidos:
• Login com Google agora funciona corretamente em builds de produção
• Autenticação reconhecida corretamente pelas funcionalidades AI
• Regras ProGuard atualizadas para preservar classes Firebase

⚠️ Se tiveste problemas de login na v1.2.0:
1. Vai a: Definições → Apps → Ciclismo Portugal → Limpar Dados
2. Abre a app novamente e entra com Google ou Email
3. Tudo deverá funcionar normalmente agora

Pedimos desculpa pelo inconveniente e agradecemos a paciência!

🔍 Logs de diagnóstico adicionados para melhor debugging em produção.
```

**And this (English):**
```
🔧 Hotfix v1.2.1 - Critical Fix

Fixed Issues:
• Google Sign-In now works correctly in production builds
• Authentication properly recognized by AI features
• ProGuard rules updated to preserve Firebase classes

⚠️ If you experienced login issues with v1.2.0:
1. Go to: Settings → Apps → Ciclismo Portugal → Clear Data
2. Open app again and sign in with Google or Email
3. Everything should work normally now

We apologize for the inconvenience and thank you for your patience!

🔍 Diagnostic logging added for better production debugging.
```

### Step 4: Configure Release
- **Release name:** v1.2.1 (Build 6)
- **Rollout:** 100% to closed testing
- **Countries:** All countries (or your configured list)

### Step 5: Review & Publish
1. Click **Review release**
2. Verify all details are correct
3. Click **Start rollout to Closed testing**

---

## ⏱️ Post-Upload Monitoring

### First Hour
- [ ] Check Play Console for upload errors
- [ ] Verify AAB processing completed
- [ ] Confirm no immediate crashes in Firebase Crashlytics

### First 24 Hours
- [ ] Monitor Firebase Crashlytics for auth-related crashes
- [ ] Check Play Console reviews (if any)
- [ ] Verify at least 3 testers updated successfully
- [ ] Ask testers to confirm:
  - [ ] Login works
  - [ ] AI features work
  - [ ] No "sign in" errors

### First 48 Hours
- [ ] Zero auth-related crashes
- [ ] All testers can sign in
- [ ] No regression bugs reported
- [ ] App size acceptable (< 12 MB optimized)

---

## 🎯 Success Criteria

**Can promote to Production if:**
- ✅ Zero crashes in 48 hours
- ✅ All testers report successful login
- ✅ AI features working without errors
- ✅ No critical bugs found
- ✅ App size remains reasonable

**Rollback to v1.2.0 if:**
- ❌ Critical crashes introduced
- ❌ Authentication still broken
- ❌ Worse than v1.2.0

---

## 📊 Testing Protocol

### For Each Tester
**Fresh Install Test:**
1. Uninstall v1.2.0 completely
2. Install v1.2.1 from Play Store
3. Sign in with Google
4. Create/view Fantasy team
5. Test DS Assistant
6. Restart app
7. Verify still logged in

**Update Test:**
1. Keep v1.2.0 installed
2. Update to v1.2.1 from Play Store
3. Open app (should still be logged in)
4. Test AI features
5. Sign out and sign in again

---

## 🚀 Promote to Production

### When Ready (After 48h+ of testing)
1. **Play Console** → **Production** → **Create new release**
2. **Promote from closed testing**: Select v1.2.1
3. **Staged rollout**: Start with 20%
   - Monitor for 24h
   - Increase to 50% if no issues
   - Increase to 100% after 48h
4. **Update store listing** (if needed):
   - Screenshots showing DS Assistant
   - Feature graphic highlighting AI features

---

## 📝 Communication Plan

### For Closed Testers
**Message template:**
```
Olá testers!

Nova versão v1.2.1 disponível em Closed Testing.

Esta é uma correção crítica para os problemas de login da v1.2.0.

Por favor:
1. Atualizar a app
2. Testar o login (Google ou Email)
3. Testar as funcionalidades AI
4. Reportar qualquer problema

Obrigado!
```

### For Production Users (if promoting)
**Update notification (optional):**
```
🔧 Atualização Importante

Corrigimos problemas críticos de autenticação.
Se tiveste dificuldades em fazer login, esta versão resolve o problema.

Atualiza agora para a melhor experiência!
```

---

## 🔄 Rollback Procedure

### If Critical Issues Found
1. **Halt rollout** in Play Console
2. **Do NOT delete v1.2.1** (can't undo)
3. **Options:**
   - **Option A:** Fix and release v1.2.2 immediately
   - **Option B:** Keep users on v1.2.0, fix in v1.2.2
4. **Communicate with testers** about the issue

### Emergency Rollback (Extreme Cases)
Cannot actually "rollback" but can:
1. Upload v1.2.0 AAB as v1.2.2 (higher version code: 7)
2. Users will "update" to the old version
3. Fix issues in v1.2.3

---

## 📞 Support Contacts

**If users report issues:**
- Email: app.cyclingai@gmail.com
- Firebase Crashlytics: Check crash reports
- Play Console: Monitor reviews

**Escalation:**
- Check logs: `adb logcat | grep FirebaseAuthService`
- Verify in Firebase Console
- Review ProGuard mapping if crashes

---

## 📈 Metrics to Track

**Play Console:**
- Crashes per user session (target: < 0.5%)
- ANRs (target: < 0.1%)
- Install success rate (target: > 99%)

**Firebase Crashlytics:**
- Auth-related crashes (target: 0)
- AI-related crashes (target: 0)
- Overall crash-free users (target: > 99.5%)

**User Feedback:**
- Play Store rating (monitor for drops)
- Reviews mentioning "login" or "authentication"
- Reviews mentioning "AI" or "assistant"

---

## ✅ Final Pre-Upload Check

**Right before clicking "Start rollout":**
- [ ] Correct AAB uploaded (16 MB, v1.2.1)
- [ ] Release notes in Portuguese AND English
- [ ] Rollout percentage set correctly (100% for closed testing)
- [ ] Team notified about new version
- [ ] Backup saved and verified
- [ ] ProGuard mapping auto-uploaded (included in AAB)

**Then click: "Start rollout to Closed testing"**

---

**Good luck! 🚀**

---

## 📚 Reference Documents

- [RELEASE_NOTES_v1.2.1_HOTFIX.md](RELEASE_NOTES_v1.2.1_HOTFIX.md) - Detailed technical notes
- [HOTFIX_SUMMARY.md](HOTFIX_SUMMARY.md) - Quick summary
- [RELEASE_BACKUP_GUIDE.md](RELEASE_BACKUP_GUIDE.md) - Backup best practices
- [TEST_AI_ACTIONS.md](TEST_AI_ACTIONS.md) - AI feature testing

---

**Status:** Ready for upload ✅
**Version:** 1.2.1 (Build 6)
**AAB:** app/build/outputs/bundle/release/app-release.aab (16 MB)
