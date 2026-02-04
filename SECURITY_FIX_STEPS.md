# Security Fix: Remove Exposed API Keys from Git History

## Current Status
- ✅ Code updated to read keys from keystore.properties
- ⚠️ Need to remove keys from Git history
- ⚠️ Need to update keystore.properties with new keys

## Steps to Remove Keys from Git History

### Option 1: Using git filter-repo (Recommended)

```bash
# Install git-filter-repo (if not installed)
pip install git-filter-repo

# Create a backup first!
cd ..
cp -r CiclismoPortugal CiclismoPortugal_backup

# Go back to repo
cd CiclismoPortugal

# Remove the exposed key from all history
git filter-repo --replace-text <(echo 'AIzaSyDtD59cVROJhqduBBti9YjYJ02FI6P_zUk==>***REMOVED***')

# Force push to GitHub (THIS REWRITES HISTORY!)
git push origin --force --all
git push origin --force --tags
```

### Option 2: Using BFG Repo-Cleaner (Alternative)

```bash
# Download BFG: https://rtyley.github.io/bfg-repo-cleaner/

# Create a file with the exposed key
echo "AIzaSyDtD59cVROJhqduBBti9YjYJ02FI6P_zUk" > keys-to-remove.txt

# Run BFG
java -jar bfg.jar --replace-text keys-to-remove.txt .

# Clean up
git reflog expire --expire=now --all && git gc --prune=now --aggressive

# Force push
git push origin --force --all
```

### Option 3: Simpler but Less Thorough (Delete and recreate repo)

If the above seems too complex:

1. Create a new private repository on GitHub
2. Copy your current code (excluding .git folder)
3. Initialize fresh git repo
4. Push to new repository
5. Delete old repository
6. Update your local remote: `git remote set-url origin <new-repo-url>`

## After Cleaning History

```bash
# Commit the security fixes
git add app/build.gradle.kts keystore.properties
git commit -m "Security: Remove hardcoded API keys, use keystore.properties

- Move YouTube API key to keystore.properties
- Remove exposed keys from code
- All API keys now properly secured
- Regenerated compromised keys

BREAKING: Requires keystore.properties with valid API keys"

# Push (after cleaning history)
git push origin main
```

## Important Notes

⚠️ **WARNING**: Force pushing rewrites history. Notify anyone else working on this repo!

✅ **Verify**: After pushing, check GitHub to ensure the old key is not visible in any commits

🔒 **Security**: The old key `AIzaSyDtD59cVROJhqduBBti9YjYJ02FI6P_zUk` is now invalid (you regenerated it)
