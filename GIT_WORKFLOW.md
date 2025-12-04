# Git Workflow Guide

This document outlines the Git workflow and versioning strategy for the Pragmite project.

## Semantic Versioning

We follow [Semantic Versioning](https://semver.org/) (MAJOR.MINOR.PATCH):

- **MAJOR** version: Incompatible API changes
- **MINOR** version: New functionality (backwards-compatible)
- **PATCH** version: Bug fixes (backwards-compatible)

## Version Components to Update

When releasing a new version, update these files:

1. **pragmite-core/pom.xml** - Line 9: `<version>X.Y.Z</version>`
2. **pragmite-vscode-extension/package.json** - Line 5: `"version": "X.Y.Z"`
3. **CHANGELOG.md** - Add new version section with changes

## Release Process

### 1. Make Changes and Test

```bash
# Work on features/fixes
# Test thoroughly
```

### 2. Update Version Numbers

```bash
# Manually update version in:
# - pragmite-core/pom.xml
# - pragmite-vscode-extension/package.json
# - CHANGELOG.md (add new section)
```

### 3. Build and Package

```bash
# Build Java core
cd pragmite-core
mvn clean package

# Compile TypeScript extension
cd ../pragmite-vscode-extension
npm run compile

# Package extension
npx vsce package
```

### 4. Commit and Tag

```bash
# Stage changes
git add .

# Commit with conventional commit message
git commit -m "chore: release version X.Y.Z"

# Create annotated tag
git tag -a vX.Y.Z -m "Release version X.Y.Z"
```

### 5. Push to GitHub

```bash
# Push commits
git push origin main

# Push tags
git push origin --tags
```

## Commit Message Convention

We use [Conventional Commits](https://www.conventionalcommits.org/):

- `feat:` New feature
- `fix:` Bug fix
- `docs:` Documentation changes
- `style:` Code style changes (formatting)
- `refactor:` Code refactoring
- `test:` Adding tests
- `chore:` Maintenance tasks

Examples:
```
feat: add theme toggle to dashboard
fix: resolve modal popup path escaping issues
docs: update installation instructions
chore: release version 1.0.4
```

## Branch Strategy

- **main**: Production-ready code
- **feature/**: Feature development branches
- **fix/**: Bug fix branches

## Tagging Releases

Always create annotated tags for releases:

```bash
git tag -a v1.0.4 -m "Release version 1.0.4 - Dashboard improvements"
git push origin v1.0.4
```

## Quick Commands Reference

```bash
# Check status
git status

# Stage all changes
git add .

# Commit
git commit -m "type: description"

# Create tag
git tag -a vX.Y.Z -m "Release message"

# Push everything
git push origin main --tags

# View commit history
git log --oneline --graph

# View tags
git tag -l
```

## GitHub Actions

We use GitHub Actions for CI/CD:

- **Maven Build**: Automatically builds Java core on push/PR
- Future: Automated testing, extension publishing

## Notes

- Always test builds locally before committing
- Update CHANGELOG.md for every release
- Keep version numbers synchronized across all files
- Use descriptive commit messages
- Tag every release for easy rollback
