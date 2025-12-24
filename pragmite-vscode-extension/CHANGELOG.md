# Change Log

All notable changes to the "Pragmite" extension will be documented in this file.

## [1.0.0] - 2025-12-01

### Added
- Initial release of Pragmite VSCode extension
- Real-time Java code quality analysis
- 31 code smell detectors
- Big-O complexity detection (O(1) to O(n!))
- Automatic analysis on file save
- Workspace-wide analysis command
- Interactive HTML quality reports
- In-editor diagnostics (errors, warnings, info)
- Status bar integration
- Configuration options for analysis behavior
- JavaParser 3.27.1 with Java 1-24 support
- Quality scoring based on Pragmatic Programmer principles:
  - DRY (Don't Repeat Yourself)
  - Orthogonality
  - Correctness
  - Performance

### Code Smell Detectors
- Duplicated Code
- Deeply Nested Code
- Long Methods
- God Classes
- Magic Numbers/Strings
- Unused Variables/Imports
- Empty Catch Blocks
- Data Classes
- Feature Envy
- Message Chains
- And 21 more!

### Supported Languages
- Java (Java 8 - Java 24)

---

## Future Plans

### [1.1.0] - Planned
- Quick fix suggestions for common code smells
- Code lens integration for complexity metrics
- Configurable code smell thresholds
- Export reports to PDF/HTML
- Integration with CI/CD pipelines

### [1.2.0] - Planned
- Kotlin support
- Refactoring suggestions
- Historical trend analysis
- Team dashboard

---

Check [Keep a Changelog](http://keepachangelog.com/) for recommendations on how to structure this file.
