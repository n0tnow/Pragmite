# Changelog

All notable changes to the Pragmite project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.4] - 2025-12-05

### Added
- **Dashboard UI Improvements**
  - Single-row responsive stats cards layout with horizontal scroll
  - Quality score detailed metrics display (DRY, Orthogonality, Correctness, Performance)
  - Improvement suggestions section with difficulty badges
  - File collapse/expand functionality
  - "Expand All" and "Collapse All" buttons for file groups
  - Full file path display in file groups and modals
  - Theme toggle button (Light/Dark mode) with localStorage persistence
  - Neomorphic 3D design for theme toggle

- **Modal Improvements**
  - Fixed modal popup functionality for issue details
  - Added full file path in modal location section
  - Fixed path escaping issues for Windows file paths
  - VSCode file opening integration

- **Responsive Design**
  - Mobile-friendly horizontal scroll for stats
  - Tablet and desktop optimized layouts
  - Custom scrollbar styling
  - Light theme support for all new components

### Changed
- Updated stats container to use flexbox instead of grid
- Improved file group UI with better hierarchy visualization
- Enhanced modal content structure with better information organization

### Fixed
- Modal click events not working due to path escape issues
- Stats cards wrapping on narrow screens
- File path display inconsistencies

## [1.0.3] - 2025-12-03

### Added
- Initial public release
- 31 code smell detectors
- Big-O complexity analysis
- Real-time Live Dashboard on port 3745
- Quality scoring system (DRY, Orthogonality, Correctness, Performance)
- VSCode integration
- Server-Sent Events (SSE) for real-time updates

### Features
- CK Metrics calculation
- Halstead metrics
- Cyclomatic complexity detection
- Maintainability index
- Parallel analysis support
- YAML-based configuration
- Refactoring suggestions with auto-fix capability
