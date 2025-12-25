# Auto-Refactoring Feature Guide - Pragmite v1.4.0

## Overview

Pragmite v1.4.0 introduces AI-powered auto-refactoring that automatically generates improved code suggestions using Claude API. This feature analyzes detected code smells and provides:

- Refactored code with best practices
- Detailed explanations of changes
- Before/after comparison in HTML reports
- Benefits analysis (why the refactored version is better)
- List of specific changes made

## Prerequisites

### API Key Setup

You need a Claude API key from Anthropic. Get one at: https://console.anthropic.com/

Configure the API key using one of these methods:

**Method 1: Environment Variable (Recommended)**
```bash
# Linux/Mac
export CLAUDE_API_KEY="sk-ant-..."

# Windows (PowerShell)
$env:CLAUDE_API_KEY="sk-ant-..."

# Windows (Command Prompt)
set CLAUDE_API_KEY=sk-ant-...
```

**Method 2: CLI Argument**
```bash
java -jar pragmite-core-1.4.0.jar --claude-api-key "sk-ant-..." --auto-refactor
```

**Alternative Environment Variable**
```bash
export ANTHROPIC_API_KEY="sk-ant-..."
```

## Usage

### Basic Auto-Refactoring

Run analysis with auto-refactoring enabled:

```bash
java -jar pragmite-core-1.4.0.jar <project-path> --generate-ai-prompts --auto-refactor --format html
```

### Command-Line Options

| Option | Description |
|--------|-------------|
| `--auto-refactor` | Enable automatic code refactoring using Claude API |
| `--generate-ai-prompts` | Generate AI analysis prompts (required for auto-refactor) |
| `--claude-api-key <key>` | Provide API key via CLI (alternative to env var) |
| `--format html` | Generate HTML report with before/after comparison |
| `--format json` | Generate JSON output with refactored code data |

### Example Commands

**1. Full Analysis with Auto-Refactoring (HTML Report)**
```bash
export CLAUDE_API_KEY="sk-ant-..."
java -jar pragmite-core-1.4.0.jar ./my-project --generate-ai-prompts --auto-refactor --format html
```

**2. With Custom Output Location**
```bash
java -jar pragmite-core-1.4.0.jar ./my-project \
  --generate-ai-prompts \
  --auto-refactor \
  --format html \
  --output ./reports/refactored-report.html
```

**3. Using CLI API Key**
```bash
java -jar pragmite-core-1.4.0.jar ./my-project \
  --claude-api-key "sk-ant-..." \
  --generate-ai-prompts \
  --auto-refactor \
  --format html
```

**4. JSON Output for Programmatic Access**
```bash
java -jar pragmite-core-1.4.0.jar ./my-project \
  --generate-ai-prompts \
  --auto-refactor \
  --format json \
  --output refactored-analysis.json
```

## Expected Output

### Console Output

When auto-refactoring is enabled, you'll see:

```
🤖 Auto-refactoring enabled with Claude API

? Generating AI-Powered Analysis...
   Generated 11 AI analysis reports.

Calling Claude API for code refactoring...
✓ Successfully generated refactored code
```

### HTML Report Features

The HTML report includes AI-generated refactored code sections with:

1. **Refactored Code Header** - "✨ AI-Generated Refactored Code"
2. **Explanation** - Brief explanation of what was changed
3. **Before/After Comparison** - Side-by-side view:
   - **Before** (red background): Original problematic code
   - **After** (green background): Refactored improved code
4. **Why This is Better** - Benefits and improvements explanation
5. **Changes Made** - Bulleted list of specific changes

### JSON Output Structure

When using `--format json`, refactored code is included in each AI analysis result:

```json
{
  "aiAnalysis": [
    {
      "smell": { ... },
      "rootCause": "...",
      "impact": "...",
      "recommendation": "...",
      "refactoredCode": {
        "successful": true,
        "originalCode": "if (age < 18) { ... }",
        "refactoredCode": "private static final int MINIMUM_AGE = 18;\nif (age < MINIMUM_AGE) { ... }",
        "explanation": "Extracted magic number into named constant",
        "whyBetter": "Improves code readability and maintainability",
        "changes": [
          "Added MINIMUM_AGE constant",
          "Replaced literal 18 with constant",
          "Added descriptive name"
        ]
      }
    }
  ]
}
```

## Configuration

### API Configuration Settings

The `ApiConfig` class supports:

| Setting | Environment Variable | Default | Description |
|---------|---------------------|---------|-------------|
| API Key | `CLAUDE_API_KEY` or `ANTHROPIC_API_KEY` | None | Your Claude API key |
| Model | `CLAUDE_MODEL` | `claude-sonnet-4-5` | Claude model to use |
| Enabled | Auto-detected | false | Auto-enabled when API key is present |
| Cache Results | - | true | Cache API responses (future feature) |
| Max Retries | - | 3 | Number of retry attempts |
| Timeout | - | 30 seconds | HTTP request timeout |

### Future: .pragmite.yaml Configuration

In future versions, you'll be able to configure AI settings in `.pragmite.yaml`:

```yaml
ai:
  enabled: true
  provider: "claude"
  model: "claude-sonnet-4-5"
  auto-refactor: true
  cache-results: true
  max-retries: 3
  timeout-seconds: 30
```

## How It Works

### Architecture

1. **Code Analysis**: Pragmite analyzes your code and detects code smells
2. **Prompt Generation**: For each smell, an AI prompt is generated
3. **API Call**: The prompt and original code are sent to Claude API
4. **Response Parsing**: Claude's markdown-formatted response is parsed
5. **Result Integration**: Refactored code is added to analysis results
6. **Report Generation**: HTML/JSON reports include before/after comparison

### Claude API Integration

**Endpoint**: `https://api.anthropic.com/v1/messages`

**Model**: `claude-sonnet-4-5-20250929`

**Request Format**:
```json
{
  "model": "claude-sonnet-4-5",
  "max_tokens": 4096,
  "messages": [
    {
      "role": "user",
      "content": "[Enhanced prompt with code context]"
    }
  ]
}
```

**Expected Response Format**:
```markdown
## Refactored Code
```java
[improved code]
```

## Explanation
[what was changed]

## Why This is Better
[benefits and improvements]

## Changes Made
- [change 1]
- [change 2]
- [change 3]
```

### Structured Prompt Engineering

Pragmite sends enhanced prompts that request specific output format:

```
[Original AI prompt for the code smell]

Please provide your response in the following format:

## Refactored Code
```java
[your refactored code here]
```

## Explanation
[brief explanation of what you changed]

## Why This is Better
[explain the benefits and improvements]

## Changes Made
- [change 1]
- [change 2]
- [change 3]
```

## Code Smell Support

Auto-refactoring works with all detected code smells:

| Code Smell | Refactoring Example |
|------------|---------------------|
| `MAGIC_NUMBER` | Extract to named constants |
| `LONG_METHOD` | Break into smaller methods |
| `DUPLICATED_CODE` | Extract common logic |
| `FEATURE_ENVY` | Move method to appropriate class |
| `LONG_PARAMETER_LIST` | Introduce parameter object |
| `STRING_CONCAT_IN_LOOP` | Use StringBuilder |
| `DEEP_NESTING` | Reduce nesting with early returns |
| `COMPLEX_CONDITIONAL` | Simplify or extract to methods |
| `DEAD_CODE` | Remove unused code |
| `UNUSED_VARIABLE` | Remove or utilize variable |
| `PRIMITIVE_OBSESSION` | Introduce value objects |

## Error Handling

### No API Key Configured

```
⚠ API configuration is invalid. Please set CLAUDE_API_KEY environment variable.
```

**Solution**: Configure API key using environment variable or CLI argument.

### API Request Failed

```
❌ API error: HTTP 401
```

**Solution**: Verify your API key is valid and has sufficient credits.

### Network Timeout

```
❌ Network error: timeout
```

**Solution**: Check internet connection, increase timeout if needed.

### Invalid API Response

```
❌ Failed to parse response: unexpected format
```

**Solution**: This is rare - the API may have changed. Report as a bug.

## Best Practices

### 1. Review Refactored Code

Always review AI-generated code before applying:
- Ensure it maintains functionality
- Verify it follows your project's conventions
- Check for any unintended side effects

### 2. Use HTML Reports

HTML reports provide the best visualization:
- Side-by-side before/after comparison
- Color coding for easy differentiation
- Full explanations and change lists

### 3. Start with Small Projects

Test auto-refactoring on small codebases first:
- Understand the quality of suggestions
- Build trust in AI recommendations
- Learn which suggestions to apply

### 4. Combine with Manual Review

Use auto-refactoring as a suggestion tool:
- Treat it as pair programming with AI
- Apply your domain knowledge
- Customize suggestions to fit your needs

### 5. Monitor API Usage

Claude API has costs and rate limits:
- Use `--format json` to track API calls
- Cache results when analyzing same code
- Consider cost vs. benefit for large projects

## Limitations

### Current Limitations (v1.4.0)

1. **No Automatic Application**: Refactored code is suggested, not automatically applied
2. **Single File Context**: Each smell is analyzed independently
3. **No Project-Wide Context**: Claude doesn't see your entire codebase
4. **Cost**: Each code smell requires an API call (costs apply)
5. **Rate Limits**: Anthropic API has rate limits

### Future Improvements

Planned for future versions:
- Automatic code application with rollback
- Batch processing to reduce API calls
- Project-wide context awareness
- Multiple AI provider support (GPT-4, Gemini)
- Result caching to reduce costs
- Interactive mode for selective application

## Troubleshooting

### API Key Not Recognized

**Symptom**: "API configuration is invalid" even with key set

**Solutions**:
1. Check environment variable is exported: `echo $CLAUDE_API_KEY`
2. Verify no extra spaces or quotes in the key
3. Try CLI argument method instead
4. Restart terminal/IDE after setting env var

### No Refactored Code in Report

**Symptom**: HTML report generated but no refactored code section

**Solutions**:
1. Ensure `--auto-refactor` flag is used
2. Verify `--generate-ai-prompts` is also included
3. Check API key is configured correctly
4. Look for error messages in console output

### Timeout Errors

**Symptom**: "Network error: timeout" for each smell

**Solutions**:
1. Check internet connection
2. Verify Claude API endpoint is accessible
3. Try with fewer code smells first
4. Contact Anthropic support if persistent

## Examples

### Example 1: Magic Number Refactoring

**Before** (Original Code):
```java
if (age < 18) {
    System.out.println("User too young");
}
```

**After** (Refactored by AI):
```java
private static final int MINIMUM_AGE = 18;

if (age < MINIMUM_AGE) {
    System.out.println("User too young");
}
```

**Explanation**: Extracted magic number into named constant

**Why Better**:
- Improves code readability
- Makes the business rule explicit
- Easier to modify threshold in one place

**Changes Made**:
- Added MINIMUM_AGE constant with value 18
- Replaced literal 18 with constant reference
- Used descriptive name that explains the purpose

### Example 2: Long Method Refactoring

**Before**:
```java
public void processUser(String name, String email, int age) {
    // 50 lines of validation and processing
}
```

**After**:
```java
public void processUser(String name, String email, int age) {
    validateUserData(name, email, age);
    processValidatedUser(name, email, age);
}

private void validateUserData(String name, String email, int age) {
    // Validation logic
}

private void processValidatedUser(String name, String email, int age) {
    // Processing logic
}
```

## Performance Considerations

### API Call Overhead

Each code smell triggers one API call:
- **Small project** (10 smells): ~10 seconds, ~$0.10
- **Medium project** (100 smells): ~100 seconds, ~$1.00
- **Large project** (1000 smells): ~1000 seconds, ~$10.00

*Note: Costs are approximate and depend on Claude API pricing*

### Optimization Strategies

1. **Filter Critical Issues Only**: Use severity filters (planned feature)
2. **Batch Processing**: Process files incrementally
3. **Incremental Analysis**: Only analyze changed files
4. **Result Caching**: Reuse previous refactorings (planned feature)

## Security Considerations

### API Key Security

⚠ **Important**: Never commit API keys to version control!

**Safe practices**:
- Use environment variables
- Use secret management tools (AWS Secrets Manager, etc.)
- Rotate keys regularly
- Use separate keys for dev/prod

### Code Privacy

Your code is sent to Anthropic's API:
- Review Anthropic's privacy policy
- Consider for open-source projects only
- Use on-premise AI for sensitive code (future feature)

## Support and Feedback

### Getting Help

1. Check this guide first
2. Review [AI_FEATURE_ROADMAP.md](AI_FEATURE_ROADMAP.md)
3. Check GitHub issues: https://github.com/your-repo/pragmite
4. Contact: your-email@example.com

### Reporting Issues

When reporting auto-refactoring issues, include:
- Pragmite version: `java -jar pragmite-core-1.4.0.jar --version`
- Code smell type that failed
- Error message from console
- Sample code (if not sensitive)

### Feature Requests

We're planning:
- GPT-4 and Gemini support
- Automatic code application
- Project-wide refactoring
- Custom refactoring rules

Submit ideas: GitHub Issues > Enhancement

## Version History

### v1.4.0 (Current)
- ✅ Claude API integration
- ✅ Auto-refactoring for all code smells
- ✅ HTML report with before/after comparison
- ✅ JSON output with refactored code
- ✅ Environment variable configuration
- ✅ CLI argument support

### Planned v1.5.0
- 🔄 Multi-provider support (GPT-4, Gemini)
- 🔄 Automatic code application
- 🔄 Result caching
- 🔄 .pragmite.yaml AI configuration

## License

Pragmite Auto-Refactoring Feature
Copyright (c) 2025
Licensed under MIT License
