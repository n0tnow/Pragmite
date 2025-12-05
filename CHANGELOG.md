# Changelog

All notable changes to the Pragmite project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.6] - 2025-12-05

### Fixed
- **Critical: Dashboard Loading Issue** 🎯
  - Fixed "Loading Data..." infinite loop when no analysis exists
  - Added helpful "No Analysis Data Yet" message with step-by-step instructions
  - Fixed null-safe issues with `toFixed()` calls in JFR profiling section
  - Fixed null-safe issues with benchmark score display
  - Dashboard now shows clear guidance when first opened
  - Error messages display properly when data loading fails

### Changed
- Dashboard now hides loading spinner and shows actual content or helpful message
- Improved error handling in `loadData()` function
- Better user experience for first-time users

## [1.0.5] - 2025-12-05

### Added
- **JFR Performance Profiling (Enabled by Default)** 🔥
  - Real-time CPU hotspot detection
  - Memory allocation tracking
  - Top 10 CPU-intensive methods display
  - Average and maximum CPU load metrics
  - Total memory allocations visualization
  - Performance insights dashboard section

- **CK Metrics Visualization** 📊
  - WMC (Weighted Methods per Class) metric
  - DIT (Depth of Inheritance Tree) metric
  - NOC (Number of Children) metric
  - CBO (Coupling Between Objects) metric
  - RFC (Response For a Class) metric
  - LCOM (Lack of Cohesion in Methods) metric
  - Automatic God Class detection (WMC>30 && LCOM>50 && CBO>10)
  - Warning indicators for threshold violations
  - Responsive grid layout for metrics cards
  - Metric legend with explanations

- **Refactoring Suggestions System** 💡
  - 5 automatic refactorers (Magic Number, Field Injection, Duplicate Code, God Class, Long Method)
  - Difficulty level badges (EASY/MEDIUM/HARD)
  - Step-by-step refactoring instructions
  - Before/After code examples
  - Auto-fix capability indicators
  - Modal popup with detailed suggestions
  - Integration with code smell detection

- **JMH Benchmark Support** ⚡
  - Benchmark result visualization
  - Score display with units
  - Mode information (Throughput/AverageTime)
  - Fastest/Slowest method tracking
  - Performance comparison dashboard

- **Backend Enhancements**
  - RefactoringManager integration in ProjectAnalyzer
  - CKMetricsCalculator integration in file analysis
  - Map import for better type safety
  - ProfileReport generation and serialization
  - BenchmarkResult model support

- **Frontend Models**
  - CKMetrics TypeScript interface
  - ProfileReport TypeScript interface
  - BenchmarkResult TypeScript interface
  - RefactoringSuggestion TypeScript interface
  - Extended AnalysisResult with new fields

- **UI Components**
  - CK Metrics section with grid layout
  - JFR Profiling section with stats cards
  - Hotspot list with method rankings
  - Benchmark results section
  - 260+ lines of new CSS styling
  - Light theme support for all new components

### Changed
- JFR profiling now enabled by default (`enableProfiling = true`)
- Enhanced FileAnalysis model with CKMetrics field
- Updated AnalysisResult to include suggestions, profileReport, and benchmarkResult
- Improved dashboard layout with new sections
- Better visual hierarchy for metrics

### Fixed
- **Critical: Quality Score Field Mismatch**
  - Backend uses `perfScore` but frontend expected `performanceScore`
  - Fixed TypeScript models to match backend field names
  - Quality Score now displays correctly (not 0/N/A)
- JAR version reference in pragmiteService.ts
- White theme card background opacity issues
- Typography contrast in light theme

### Performance
- Analysis time increase: ~180ms overhead with JFR (340ms → 520ms for 10 files)
- Memory usage: +5-10MB for JFR temporary recording files
- Auto-cleanup of temporary profiling files

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
