# Releasing CryptoPriceTracker

This document describes the full release process for the team.

---

## Versioning Conventions

We follow [Semantic Versioning](https://semver.org/): **MAJOR.MINOR.PATCH**

| Increment | When to use |
|-----------|-------------|
| `MAJOR`   | Breaking changes or major redesign (e.g. `2.0.0`) |
| `MINOR`   | New features, backwards-compatible (e.g. `1.3.0`) |
| `PATCH`   | Bug fixes only (e.g. `1.2.1`) |

**versionCode** is derived automatically by the release script:
```
versionCode = MAJOR × 10000 + MINOR × 100 + PATCH
# e.g. 1.2.3 → 10203
```

**Pre-release suffixes** (workflow marks these as pre-release on GitHub):
- Alpha: `v1.0.0-alpha`
- Beta:  `v1.0.0-beta`
- RC:    `v1.0.0-rc`

---

## Required GitHub Secrets

Configure these in **Settings → Secrets and variables → Actions** on GitHub:

| Secret name       | Description                                    | How to generate |
|-------------------|------------------------------------------------|-----------------|
| `KEYSTORE_BASE64` | Base64-encoded keystore file                   | See command below |
| `STORE_PASSWORD`  | Password for the `.jks` keystore file          | Chosen when creating the keystore |
| `KEY_ALIAS`       | Alias of the signing key inside the keystore   | Chosen when creating the keystore |
| `KEY_PASSWORD`    | Password for the signing key                   | Chosen when creating the keystore |

### Generate a new keystore (first time only)

```bash
keytool -genkey -v \
  -keystore keystore.jks \
  -alias crypto-tracker \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000 \
  -storepass YOUR_STORE_PASSWORD \
  -keypass YOUR_KEY_PASSWORD \
  -dname "CN=CryptoPriceTracker, OU=Mobile, O=YourOrg, L=City, S=State, C=IN"
```

> ⚠️  **Never commit `keystore.jks` to the repository.**  
> Add it to `.gitignore` immediately after creation.

### Encode the keystore for GitHub Secrets

```bash
base64 -i keystore.jks | pbcopy   # macOS — copies to clipboard
base64 -i keystore.jks            # Linux  — print to stdout, then paste
```

Paste the output as the value of the `KEYSTORE_BASE64` secret.

---

## Step-by-Step Release Process

### Prerequisites

- You are on the `main` branch with a clean working tree.
- All PRs for this release have been merged.
- The app builds locally: `./gradlew assembleRelease`
- GitHub Secrets are configured (see above).

### 1. Make the release script executable (one-time setup)

```bash
chmod +x scripts/create-release.sh
```

### 2. Run the release script

```bash
./scripts/create-release.sh <MAJOR.MINOR.PATCH>

# Examples:
./scripts/create-release.sh 1.0.0   # First stable release
./scripts/create-release.sh 1.1.0   # New feature release
./scripts/create-release.sh 1.1.1   # Bug fix release
```

The script will:
1. Validate the version format
2. Update `versionName` and `versionCode` in `app/build.gradle.kts`
3. Commit the version bump: `chore: bump version to X.Y.Z`
4. Create an annotated git tag `vX.Y.Z`
5. Push the branch and tag to `origin`

### 3. Monitor the workflow

The push of the tag triggers the **Release** workflow automatically.

```
GitHub → Actions → Release → <your run>
```

Expected steps:
- ✅ Checkout source
- ✅ Set up JDK 17
- ✅ Make gradlew executable
- ✅ Decode keystore
- ✅ Build signed release APK
- ✅ Locate APK
- ✅ Generate release notes
- ✅ Create GitHub Release
- ✅ Remove keystore

### 4. Verify the release

Go to **GitHub → Releases** and confirm:
- The tag is correct (e.g. `v1.1.0`)
- The signed APK is attached
- Release notes list the expected commits
- Pre-release flag is **not** set (unless it's an alpha/beta/RC)

---

## Hotfix Process

A hotfix is a `PATCH` bump applied to a released tag, **not** branched from `main`
(in case `main` has unreleased features).

```bash
# 1. Create a hotfix branch from the tag you are patching
git checkout -b hotfix/1.1.1 v1.1.0

# 2. Apply your fix(es) and commit
git add .
git commit -m "fix: <description of the fix>"

# 3. Release from this branch (script works on any branch)
./scripts/create-release.sh 1.1.1

# 4. Merge the hotfix back to main so the fix is not lost
git checkout main
git merge --no-ff hotfix/1.1.1 -m "chore: merge hotfix/1.1.1 into main"
git push origin main

# 5. Clean up
git branch -d hotfix/1.1.1
```

---

## Rolling Back a Release

GitHub releases can be deleted but **tags cannot be re-used**.

```bash
# Delete the remote tag (use with extreme caution)
git push origin --delete vX.Y.Z
git tag -d vX.Y.Z
```

Then fix the issue, bump to the next PATCH version, and re-release.

---

## Local Signed Build (without CI)

Set the environment variables in your terminal, then run Gradle:

```bash
export KEYSTORE_PATH="/path/to/keystore.jks"
export STORE_PASSWORD="your_store_password"
export KEY_ALIAS="your_key_alias"
export KEY_PASSWORD="your_key_password"

./gradlew assembleRelease

# APK will be at:
# app/build/outputs/apk/release/app-release.apk
```

---

## Branch & Tag Naming Conventions

| Type            | Pattern                  | Example              |
|-----------------|--------------------------|----------------------|
| Feature branch  | `feature/<description>`  | `feature/dark-mode`  |
| Bug fix branch  | `fix/<description>`      | `fix/crash-on-open`  |
| Hotfix branch   | `hotfix/<version>`       | `hotfix/1.1.1`       |
| Release tag     | `v<MAJOR.MINOR.PATCH>`   | `v1.2.0`             |
| Pre-release tag | `v<version>-<suffix>`    | `v2.0.0-beta`        |

---

## Commit Message Conventions

We follow [Conventional Commits](https://www.conventionalcommits.org/):

```
<type>: <short description>

Types: feat | fix | chore | refactor | test | docs | style | perf | ci
```

These commit messages are used verbatim in the auto-generated release notes.

---

## FAQ

**Q: The workflow failed at "Build signed release APK". What do I check?**  
A: Verify all four secrets (`KEYSTORE_BASE64`, `STORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`) are set correctly in GitHub → Settings → Secrets.

**Q: Can I trigger a release without the script?**  
A: Yes — manually push a tag: `git tag -a v1.0.0 -m "Release v1.0.0" && git push origin v1.0.0`. But you must update `versionName`/`versionCode` in `app/build.gradle.kts` manually first.

**Q: How do I create a pre-release (beta)?**  
A: Use a suffix in the version tag: `git tag -a v1.0.0-beta -m "Beta release"`. The workflow will automatically mark it as a pre-release on GitHub.

