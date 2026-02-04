# Release Backup Best Practices

## 🎯 Complete Backup Strategy

### 1. **Git Version Control** (Critical - Free)

**Why:** Allows you to rebuild any version from source

**Implementation:**
```bash
# Tag every release
git tag -a v1.2.1 -m "Release v1.2.1 - Authentication hotfix"
git push origin v1.2.1

# Create release branch (optional, for hotfixes)
git checkout -b release/1.2.1
git push origin release/1.2.1
```

**Benefits:**
- ✅ Free (GitHub/GitLab/Bitbucket)
- ✅ Full source code history
- ✅ Can rebuild any version
- ✅ Easy rollback

**Backup schedule:** After every release

---

### 2. **Save Built AAB/APK Files** (Essential - Local + Cloud)

**Why:** Exact binary that was released, for emergency rollback

**Where to save:**
- **Primary:** Cloud storage (Google Drive, Dropbox, OneDrive)
- **Secondary:** External hard drive
- **Tertiary:** Network attached storage (NAS)

**File structure:**
```
Releases/
├── v1.2.0/
│   ├── app-release.aab
│   ├── mapping.txt (ProGuard mapping)
│   ├── RELEASE_NOTES_v1.2.0.md
│   └── metadata.json (version info, upload date)
├── v1.2.1/
│   ├── app-release.aab
│   ├── mapping.txt
│   ├── RELEASE_NOTES_v1.2.1.md
│   └── metadata.json
```

**Automation script:**
```bash
# Save after each build
VERSION=1.2.1
mkdir -p ~/Releases/v$VERSION
cp app/build/outputs/bundle/release/app-release.aab ~/Releases/v$VERSION/
cp app/build/outputs/mapping/release/mapping.txt ~/Releases/v$VERSION/
cp RELEASE_NOTES_v$VERSION.md ~/Releases/v$VERSION/
```

**Backup schedule:** Immediately after building each release

---

### 3. **ProGuard Mapping Files** (Critical for Debugging)

**Why:** Decode stack traces from release crashes

**Where:**
- Uploaded automatically to Play Console (via AAB)
- **Also save locally** in release backup folder

**File location:**
```
app/build/outputs/mapping/release/mapping.txt
```

**Play Console upload:**
- Automatically included in AAB bundle
- Also upload manually: Play Console → App → Quality → Deobfuscation files

**Backup schedule:** With every release AAB

---

### 4. **Google Play Console History** (Automatic)

**What Play Console saves:**
- All uploaded AAB files
- ProGuard mapping files
- Release notes
- Rollout percentages

**How to access old versions:**
1. Play Console → Production → Releases
2. Click "See all releases"
3. Download previous AAB (if needed)

**Retention:** Google keeps all versions indefinitely

**Limitation:** You can't download source code, only AABs

---

### 5. **Signing Keystore** (MOST CRITICAL - Keep Safe!)

**Why:** Without keystore, you can NEVER update your app again

**What to backup:**
- `keystore.jks` (or `.keystore` file)
- `keystore.properties` (with passwords - encrypted!)

**Where to save:**
- **Primary:** Password manager (1Password, LastPass, Bitwarden)
- **Secondary:** Encrypted USB drive
- **Tertiary:** Bank safe deposit box (for production apps)

**NEVER:**
- ❌ Commit to Git (even private repos)
- ❌ Store in cloud unencrypted
- ❌ Email to yourself

**Backup schedule:**
- One-time (keystore never changes)
- Verify backup works annually

---

### 6. **Release Documentation** (Important for Context)

**What to save:**
- Release notes (RELEASE_NOTES_v1.2.1.md)
- Known issues
- Testing checklist results
- A/B test results (if any)
- User feedback summary

**Where:**
- Git repository (docs/ folder)
- Cloud storage (with AAB backup)

**Format:**
```markdown
# v1.2.1 Release
Date: 2026-02-04
Type: Hotfix
Rollout: Closed testing → 100%

## Changes
- Fixed Google Sign-In authentication
- Added ProGuard rules for Firebase

## Test Results
- ✅ Auth works on Android 8-14
- ✅ No crashes in 48 hours
- ✅ 10/10 testers verified fix

## User Feedback
- "Login works perfectly now!" - João S.
- "Much better" - Maria P.
```

---

### 7. **Database Backup** (If Using Backend)

**If you have Firebase/Backend:**
- **Firestore:** Export regularly
- **Realtime Database:** Automatic daily backups
- **Storage:** Backup uploaded files

**Firebase backup commands:**
```bash
# Firestore export
gcloud firestore export gs://[BUCKET_NAME]
```

**Backup schedule:** Daily for production data

---

## 📦 Complete Backup Checklist

For **every release**, save:

- [ ] Git tag created and pushed
- [ ] AAB file saved to cloud storage
- [ ] APK file saved (optional, for testing)
- [ ] ProGuard mapping.txt saved
- [ ] Release notes saved
- [ ] Commit hash recorded
- [ ] Play Console screenshots (optional)
- [ ] Keystore verified (annually)

---

## 🔄 Recommended Backup Schedule

| Item | Frequency | Location |
|------|-----------|----------|
| **Source code** | Every commit | GitHub/GitLab |
| **Release tags** | Every release | Git remote |
| **AAB files** | Every release | Cloud + Local |
| **ProGuard maps** | Every release | Cloud + Play Console |
| **Keystore** | One-time (verify annually) | Password manager + Encrypted USB |
| **Database** | Daily | Firebase auto-backup |
| **Documentation** | Every release | Git + Cloud |

---

## 🛠️ Automation Scripts

### Script 1: Post-Build Backup
Save this as `scripts/backup-release.sh`:

```bash
#!/bin/bash
# Usage: ./scripts/backup-release.sh 1.2.1

VERSION=$1
BACKUP_DIR=~/Releases/v$VERSION
CLOUD_DIR=~/Google\ Drive/CiclismoPortugal/Releases/v$VERSION

# Create directories
mkdir -p "$BACKUP_DIR"
mkdir -p "$CLOUD_DIR"

# Copy files
cp app/build/outputs/bundle/release/app-release.aab "$BACKUP_DIR/"
cp app/build/outputs/mapping/release/mapping.txt "$BACKUP_DIR/"
cp RELEASE_NOTES_v$VERSION*.md "$BACKUP_DIR/" 2>/dev/null || true

# Sync to cloud
cp -r "$BACKUP_DIR/"* "$CLOUD_DIR/"

# Create metadata
cat > "$BACKUP_DIR/metadata.json" <<EOF
{
  "version": "$VERSION",
  "buildDate": "$(date -u +"%Y-%m-%dT%H:%M:%SZ")",
  "commitHash": "$(git rev-parse HEAD)",
  "branch": "$(git branch --show-current)"
}
EOF

echo "✅ Backup saved to: $BACKUP_DIR"
echo "☁️  Cloud backup: $CLOUD_DIR"
```

### Script 2: Verify Keystore Backup

```bash
#!/bin/bash
# Verify keystore can sign an app

keytool -list -v -keystore /path/to/keystore.jks \
  -alias [YOUR_ALIAS] -storepass [PASSWORD]

if [ $? -eq 0 ]; then
  echo "✅ Keystore backup is valid"
else
  echo "❌ Keystore backup is BROKEN!"
fi
```

---

## 🚨 Emergency Recovery Procedures

### Scenario 1: Need to Roll Back Release

1. **From Play Console:**
   - Download previous AAB from "Releases" page
   - Create new release with higher version code
   - Upload old AAB content with new version number

2. **From Git:**
   ```bash
   git checkout v1.2.0  # Previous good version
   # Edit build.gradle.kts to set versionCode = 7
   ./gradlew bundleRelease
   # Upload to Play Console
   ```

3. **From Local Backup:**
   - Retrieve v1.2.0 AAB from cloud storage
   - Rebuild with new version code
   - Upload

---

### Scenario 2: Lost Keystore (CRITICAL)

**If keystore is lost:**
- ⚠️ **You CANNOT update your existing app**
- Only option: Publish new app with different package name
- All users must uninstall and reinstall

**Prevention:**
- Backup keystore NOW
- Store in multiple locations
- Test backup annually

---

### Scenario 3: Need ProGuard Mapping

**To decode crash:**
1. Get mapping.txt for that version
2. Upload to Play Console → Deobfuscation
3. Crashes will be decoded automatically

**If mapping.txt lost:**
- Crashes are unreadable
- Try to rebuild exact version from git tag
- Check if ProGuard output is reproducible

---

## 💡 Pro Tips

1. **Automate backups:** Create Git hooks to auto-backup on release
2. **Version everything:** Document versions of all tools (Android Studio, Gradle, Kotlin)
3. **Test restores:** Periodically verify backups actually work
4. **Document process:** Write down your backup procedure
5. **Use CI/CD:** GitHub Actions can auto-backup to cloud storage

---

## 📱 My Recommended Setup for Ciclismo Portugal

**Primary:**
- Git tags for all releases (GitHub)
- AAB files in Google Drive: `CiclismoPortugal/Releases/`
- ProGuard mappings auto-uploaded via AAB

**Secondary:**
- External HDD with all AAB files (weekly sync)
- Keystore in password manager (1Password/Bitwarden)

**Automation:**
- GitHub Actions workflow to:
  1. Build release AAB
  2. Upload to Google Drive
  3. Create GitHub release with AAB attached
  4. Tag commit

---

**Bottom Line:**
- **Keystore** = Most critical (can't replace)
- **Git tags** = Second most critical (can rebuild)
- **AAB backups** = Nice to have (faster rollback)

Save your keystore NOW if you haven't already! 🔑
