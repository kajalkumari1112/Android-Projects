#!/usr/bin/env bash
# =============================================================================
# create-release.sh
# Usage: ./scripts/create-release.sh <version>
# Example: ./scripts/create-release.sh 1.2.0
#
# What this script does:
#   1. Validates the version string (semantic versioning: MAJOR.MINOR.PATCH)
#   2. Bumps versionName and versionCode in app/build.gradle.kts
#   3. Commits the version bump
#   4. Creates and pushes a git tag → triggers the release workflow
# =============================================================================

set -euo pipefail   # Exit immediately on error, treat unset vars as errors

# ── Colour helpers ─────────────────────────────────────────────────────────────
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Colour

info()    { echo -e "${GREEN}[INFO]${NC}  $*"; }
warn()    { echo -e "${YELLOW}[WARN]${NC}  $*"; }
error()   { echo -e "${RED}[ERROR]${NC} $*" >&2; }
die()     { error "$*"; exit 1; }

# ── 1. Validate arguments ──────────────────────────────────────────────────────
if [ $# -ne 1 ]; then
  die "Usage: $0 <version>  (e.g. $0 1.2.0)"
fi

VERSION="$1"

# Validate semantic versioning format: MAJOR.MINOR.PATCH (all numeric)
if ! echo "$VERSION" | grep -qE '^[0-9]+\.[0-9]+\.[0-9]+$'; then
  die "Invalid version format '$VERSION'. Expected MAJOR.MINOR.PATCH (e.g. 1.2.0)"
fi

TAG="v${VERSION}"
GRADLE_FILE="$(dirname "$0")/../app/build.gradle.kts"
GRADLE_FILE="$(realpath "$GRADLE_FILE")"

info "Preparing release $TAG"

# ── 2. Check we are on main / master and working tree is clean ─────────────────
CURRENT_BRANCH=$(git rev-parse --abbrev-ref HEAD)
if [[ "$CURRENT_BRANCH" != "main" && "$CURRENT_BRANCH" != "master" ]]; then
  warn "You are on branch '$CURRENT_BRANCH', not main/master."
  read -r -p "Continue anyway? [y/N] " CONFIRM
  [[ "$CONFIRM" =~ ^[Yy]$ ]] || die "Aborted."
fi

if ! git diff --quiet || ! git diff --cached --quiet; then
  die "Working tree has uncommitted changes. Please commit or stash them first."
fi

# ── 3. Ensure the tag does not already exist ──────────────────────────────────
if git rev-parse "$TAG" >/dev/null 2>&1; then
  die "Tag $TAG already exists. Choose a different version."
fi

# ── 4. Calculate the new versionCode ──────────────────────────────────────────
#   Strategy: MAJOR * 10000 + MINOR * 100 + PATCH
#   e.g. 1.2.3 → 10203   |   2.0.0 → 20000
MAJOR=$(echo "$VERSION" | cut -d. -f1)
MINOR=$(echo "$VERSION" | cut -d. -f2)
PATCH=$(echo "$VERSION" | cut -d. -f3)
VERSION_CODE=$(( MAJOR * 10000 + MINOR * 100 + PATCH ))

info "New versionName : $VERSION"
info "New versionCode : $VERSION_CODE"

# ── 5. Update app/build.gradle.kts ────────────────────────────────────────────
if [ ! -f "$GRADLE_FILE" ]; then
  die "Could not find $GRADLE_FILE"
fi

# Use sed to replace the existing versionCode and versionName lines
# Works on both macOS (BSD sed) and Linux (GNU sed) via the -i '' / -i trick
if [[ "$OSTYPE" == "darwin"* ]]; then
  sed -i '' "s/versionCode = [0-9]*/versionCode = ${VERSION_CODE}/" "$GRADLE_FILE"
  sed -i '' "s/versionName = \"[^\"]*\"/versionName = \"${VERSION}\"/" "$GRADLE_FILE"
else
  sed -i "s/versionCode = [0-9]*/versionCode = ${VERSION_CODE}/" "$GRADLE_FILE"
  sed -i "s/versionName = \"[^\"]*\"/versionName = \"${VERSION}\"/" "$GRADLE_FILE"
fi

# Verify the substitution actually happened
grep -q "versionCode = ${VERSION_CODE}" "$GRADLE_FILE" \
  || die "Failed to update versionCode in $GRADLE_FILE"
grep -q "versionName = \"${VERSION}\"" "$GRADLE_FILE" \
  || die "Failed to update versionName in $GRADLE_FILE"

info "Updated $GRADLE_FILE"

# ── 6. Commit the version bump ────────────────────────────────────────────────
git add "$GRADLE_FILE"
git commit -m "chore: bump version to $VERSION (code: $VERSION_CODE)"
info "Committed version bump"

# ── 7. Create and push the tag ────────────────────────────────────────────────
git tag -a "$TAG" -m "Release $TAG"
info "Created tag $TAG"

git push origin "$CURRENT_BRANCH"
git push origin "$TAG"
info "Pushed branch and tag to origin"

echo ""
echo -e "${GREEN}✅ Release $TAG triggered successfully!${NC}"
echo "   Monitor progress at: https://github.com/$(git remote get-url origin | sed 's/.*github.com[:/]//' | sed 's/\.git$//')/actions"

