# Requirements

## Overview

JUnitility provides utilities for processing JUnit 5 test execution results.

## JSON Test Report Summarizer

### Purpose
Read Maven Surefire/Failsafe XML test reports and generate a JSON summary with class-level and method-level aggregations.

### CLI Command Structure

**Main command:** `junitility`

**Subcommands:**

#### 1. `junitility report` - Full workflow (copy + CSV + JSON)

Orchestrates the complete reporting workflow.

**Syntax:** `junitility report <input-dir> <label> [--reports-dir <dir>]`

**Parameters:**
- `<input-dir>` - Directory containing `TEST-*.xml` files (required)
- `<label>` - Label for this test run, used as subdirectory name (required)
- `--reports-dir <dir>` - Output directory (default: `reports`)

**Behavior:**
1. Create output directory structure: `<reports-dir>/<label>/`
2. Copy all files from `<input-dir>` to `<reports-dir>/<label>/`
3. Parse all `TEST-*.xml` files from the copied directory
4. Generate CSV report: `<reports-dir>/<label>/test-results.csv`
5. Generate JSON summary: `<reports-dir>/<label>/test-summary.json`
   - JSON contains relative path to CSV: `"detailedReportPath": "test-results.csv"`

**Example:**
```bash
junitility report target/surefire-reports my-test-run
# Creates:
#   reports/my-test-run/TEST-*.xml (copied)
#   reports/my-test-run/test-results.csv
#   reports/my-test-run/test-summary.json

junitility report target/failsafe-reports integration-tests --reports-dir build/test-reports
# Creates:
#   build/test-reports/integration-tests/TEST-*.xml (copied)
#   build/test-reports/integration-tests/test-results.csv
#   build/test-reports/integration-tests/test-summary.json
```

#### 2. `junitility csv` - Generate CSV only

Generate only the CSV report from XML files.

**Syntax:** `junitility csv <input-dir> <output-file>`

**Parameters:**
- `<input-dir>` - Directory containing `TEST-*.xml` files (required)
- `<output-file>` - Path to output CSV file (required)

**Behavior:**
- Parse all `TEST-*.xml` files from input directory
- Generate CSV report at specified output path

**Example:**
```bash
junitility csv target/surefire-reports test-results.csv
# Creates: test-results.csv
```

#### 3. `junitility json` - Generate JSON only

Generate only the JSON summary from XML files.

**Syntax:** `junitility json <input-dir> <output-file> [--csv-path <path>]`

**Parameters:**
- `<input-dir>` - Directory containing `TEST-*.xml` files (required)
- `<output-file>` - Path to output JSON file (required)
- `--csv-path <path>` - Path to reference in `detailedReportPath` field (optional, defaults to `test-results.csv`)

**Behavior:**
- Parse all `TEST-*.xml` files from input directory
- Generate JSON summary at specified output path
- Use `--csv-path` value for `detailedReportPath` field in JSON

**Example:**
```bash
junitility json target/surefire-reports summary.json
# Creates: summary.json (with detailedReportPath: "test-results.csv")

junitility json target/surefire-reports summary.json --csv-path ./detailed/results.csv
# Creates: summary.json (with detailedReportPath: "./detailed/results.csv")
```

#### 4. `junitility diff` - Compare test results from two directories

Compare test results from two different test runs and generate a diff report showing which tests appeared in each run and their outcomes.

**Syntax:** `junitility diff <dir1> <dir2>`

**Parameters:**
- `<dir1>` - First directory containing test results (required)
- `<dir2>` - Second directory containing test results (required)

**Behavior:**
- Read `test-results.csv` from each directory (fail fast if either is missing)
- Create a union of all tests from both CSV files
- Generate a diff CSV showing:
  - All tests that appear in either directory
  - Outcome from first directory
  - Outcome from second directory
  - Use "-" for tests that don't appear in a particular directory
- Output filename: `<dir1-name>-<dir2-name>.csv`
- Column headers use directory names: `ClassName,MethodName,<dir1-name> Outcome,<dir2-name> Outcome`

**Example:**
```bash
junitility diff reports/baseline reports/current
# Creates: baseline-current.csv

# Example output:
# ClassName,MethodName,baseline Outcome,current Outcome
# com.example.TestA,test1,SUCCESS,SUCCESS
# com.example.TestA,test2,SUCCESS,FAILURE
# com.example.TestB,test3,SUCCESS,-
# com.example.TestC,test4,-,SUCCESS
```

**Error Handling:**
- If `<dir1>/test-results.csv` doesn't exist → Exit with error
- If `<dir2>/test-results.csv` doesn't exist → Exit with error
- If output file already exists → Overwrite (no prompt needed for diff output)

#### 5. `junitility compare` - Generate net impact reports from test results

Generate comprehensive net impact analysis from baseline and patched test runs, including summary metrics and detailed transition tracking.

**Syntax:** `junitility compare <baseline-dir> <patched-dir> --output <output-dir>`

**Parameters:**
- `<baseline-dir>` - Directory containing baseline Surefire/Failsafe XML files (required)
- `<patched-dir>` - Directory containing patched Surefire/Failsafe XML files (required)
- `--output <dir>` - Output directory for generated reports (required)

**Behavior:**
1. Validate both directories exist and contain XML files
2. Parse XML files from baseline directory using existing `XmlParser.parseDirectory()`
3. Parse XML files from patched directory using existing `XmlParser.parseDirectory()`
4. Compare test results to determine net changes (implement as `NetImpactGenerator.kt` with `compare()` method)
5. Generate two output files:
   - `<output-dir>/pr-impact-summary.md` - Summary markdown with metrics (use `buildString { }`)
   - `<output-dir>/pr-impact-details.csv` - Detailed CSV with all test comparisons (refactor `CsvGenerator` to extract and reuse RFC 4180 quoting logic)

**Net Change Classification:**

Test transitions are classified as a Kotlin enum with hyphenated uppercase format in CSV output (e.g., "SKIPPED-FIXED", "FAILURE-ERROR"):

| NetChange | Meaning | Notes |
|-----------|---------|-------|
| `ADDED` | Test not in baseline, present in patched | |
| `REMOVED` | Test present in baseline, not in patched | |
| `FIXED` | Test transitioned from FAILURE/ERROR to SUCCESS | Does not differentiate FAILURE → SUCCESS vs ERROR → SUCCESS; original outcome preserved in BaselineOutcome column |
| `SKIPPED-FIXED` | Test transitioned from SKIPPED to SUCCESS | |
| `REGRESSED` | Test transitioned from SUCCESS to FAILURE/ERROR | Does not differentiate SUCCESS → FAILURE vs SUCCESS → ERROR; final outcome preserved in PatchedOutcome column |
| `FAILURE-ERROR` | Test transitioned from FAILURE to ERROR | |
| `ERROR-FAILURE` | Test transitioned from ERROR to FAILURE | |
| `SKIPPED` | Test transitioned from SUCCESS to SKIPPED | |
| `FAILURE-SKIPPED` | Test transitioned from FAILURE to SKIPPED | |
| `ERROR-SKIPPED` | Test transitioned from ERROR to SKIPPED | |
| `SKIPPED-FAILURE` | Test transitioned from SKIPPED to FAILURE | |
| `SKIPPED-ERROR` | Test transitioned from SKIPPED to ERROR | |
| `SUCCESS` | Test remained SUCCESS | |
| `FAILURE` | Test remained FAILURE | |
| `ERROR` | Test remained ERROR | |
| `SKIPPED` | Test remained SKIPPED | CSV output uses "SKIPPED" (not "SKIPPED-SKIPPED") for tests that remained skipped |

**Summary Metrics:**

The summary includes counts for:
- Total tests (baseline, patched, delta)
- Success counts (baseline, patched, delta)
- Failure counts (baseline, patched, delta)
- Error counts (baseline, patched, delta)
- Skipped counts (baseline, patched, delta)
- Fixed tests (FAILURE/ERROR → SUCCESS, excludes SKIPPED-FIXED)
- Unskipped-Fixed tests (SKIPPED → SUCCESS)
- Added tests (new in patched)
- Newly unskipped tests (SKIPPED → non-SKIPPED, includes SKIPPED → SUCCESS/FAILURE/ERROR)
- Regressed tests (SUCCESS → FAILURE/ERROR, covers both transitions)
- Newly skipped tests (non-SKIPPED → SKIPPED, includes SUCCESS/FAILURE/ERROR → SKIPPED)
- Removed tests (removed from patched)

**Output Format - Summary Markdown:**

Format deltas with "+" prefix for positive numbers, "-" for negative, "0" for zero. Include single blank line after each metric section. Left-aligned text format.

```markdown
# PR Impact Summary

**Total Tests:**  
Baseline: 100 | Patched: 101 | Delta: +1

**Success:**  
Baseline: 85 | Patched: 88 | Delta: +3

**Failures:**  
Baseline: 10 | Patched: 8 | Delta: -2

**Errors:**  
Baseline: 3 | Patched: 3 | Delta: 0

**Skipped:**  
Baseline: 2 | Patched: 2 | Delta: 0

**Fixed Tests:** 3  
**Unskipped-Fixed Tests:** 1  
**Added Tests:** 1  
**Newly Unskipped Tests:** 0  
**Regressed Tests:** 0  
**Newly Skipped Tests:** 0  
**Removed Tests:** 0
```

**Output Format - Detailed CSV:**

Sort rows by ClassName then MethodName (alphabetical). Use RFC 4180 quoting (reuse logic from CsvGenerator). Use "-" for missing outcomes. NetChange values use uppercase with hyphens (e.g., "SKIPPED-FIXED").

```csv
ClassName,MethodName,BaselineOutcome,PatchedOutcome,NetChange
com.example.Test1,test1(),FAILURE,SUCCESS,FIXED
com.example.Test2,test2(),SUCCESS,FAILURE,REGRESSED
com.example.Test3,test3(),-,SUCCESS,ADDED
com.example.Test4,test4(),SUCCESS,-,REMOVED
com.example.Test5,test5(),SUCCESS,SUCCESS,SUCCESS
```

**Example:**
```bash
junitility compare target/surefire-reports baseline/surefire-reports --output reports/pr-impact
# Creates:
#   reports/pr-impact/pr-impact-summary.md
#   reports/pr-impact/pr-impact-details.csv

# Output shows:
# Generated: reports/pr-impact/pr-impact-summary.md
# Generated: reports/pr-impact/pr-impact-details.csv
```

**Error Handling:**
- If baseline directory doesn't exist or is not a directory → Exit with error
- If patched directory doesn't exist or is not a directory → Exit with error
- If baseline directory contains no XML files → Exit with error
- If patched directory contains no XML files → Exit with error
- If `--output` is not specified → Exit with error
- If output directory already exists → Delete and recreate (no prompt needed)
- If XML parsing fails → Exit with error

**Relationship to `diff` Command:**

The `compare` command is a more sophisticated version of `diff`:
- `diff` works with existing CSV files and produces a simple side-by-side comparison (4 columns)
- `compare` works with raw XML files only (no CSV fallback), produces rich NetChange analysis and summary metrics (5 columns)
- Use `diff` for quick CSV-based comparisons
- Use `compare` for comprehensive impact analysis with classifications and summaries
- Commands are not cross-compatible (different schemas)
- `compare` always generates both markdown and CSV outputs (no flags to skip)
- `compare` supports only 2 directories (baseline vs patched)

### Input
- **Input Directory** - Directory containing Surefire/Failsafe XML report files
  - Contains `TEST-*.xml` files generated by Maven Surefire/Failsafe plugins
  - Typical locations: `target/surefire-reports/` or `target/failsafe-reports/`
  - Must be specified by caller (no default)
  - All `TEST-*.xml` files in the directory are processed
  - All files are copied to the output directory before processing

### JSON Output Format

Top-level fields:
- **detailedReportPath** - Path to the CSV file containing line-by-line test results
- **classes** - Array of class-level summaries

Two-level hierarchy:

#### Class Level Summary
For each test class:
- **className** - Fully qualified class name
- **total** - Total number of test methods (including all parameterized variants)
- **success** - Number of successful tests
- **failures** - Number of failed tests
- **errors** - Number of errored tests
- **skipped** - Number of skipped tests
- **methods** - Array of method-level summaries (see below)

#### Method Level Summary
For each test method (nested under class):
- **methodName** - Fully qualified method name
- **total** - Total executions (1 for regular tests, N for parameterized tests with N variants)
- **success** - Number of successful executions
- **failures** - Number of failed executions
- **errors** - Number of errored executions
- **skipped** - Number of skipped executions

**Note:** Each parameterized test variant is counted separately in the method-level totals.

#### Example JSON Structure
```json
{
  "detailedReportPath": "test-results.csv",
  "classes": [
    {
      "className": "com.example.MyTest",
      "total": 5,
      "success": 3,
      "failures": 1,
      "errors": 0,
      "skipped": 1,
      "methods": [
        {
          "methodName": "testSimple",
          "total": 1,
          "success": 1,
          "failures": 0,
          "errors": 0,
          "skipped": 0
        },
        {
          "methodName": "testParameterized",
          "total": 3,
          "success": 2,
          "failures": 1,
          "errors": 0,
          "skipped": 0
        },
        {
          "methodName": "testSkipped",
          "total": 1,
          "success": 0,
          "failures": 0,
          "errors": 0,
          "skipped": 1
        }
      ]
    }
  ]
}
```

### Output Directory Structure

```
<reports-dir>/
  <label>/
    TEST-*.xml           # Copied from input directory
    test-results.csv     # Generated CSV report
    test-summary.json    # Generated JSON summary
```

### Output Files

Generated in `<reports-dir>/<label>/`:

1. **Copied XML Reports** - All files from input directory (preserves originals)
2. **CSV Report** (`test-results.csv`) - Line-by-line test execution details
3. **JSON Summary** (`test-summary.json`) - Aggregated class/method summaries with reference to CSV
   - `detailedReportPath` field contains relative path: `"test-results.csv"`

### CSV Format Details
- **Column headers:** `ClassName,MethodName,Outcome`
- **Field quoting:** Use RFC 4180 standard (quote fields containing commas, quotes, or newlines)
- **Delimiter:** Comma (`,`)
- **Line ending:** LF (`\n`) - Unix standard
- **Example:**
  ```csv
  ClassName,MethodName,Outcome
  com.example.MyTest,testSimple(),SUCCESS
  com.example.MyTest,testParameterized(String)[1],SUCCESS
  com.example.MyTest,testParameterized(String)[2],FAILURE
  com.example.MyTest,testSkipped(),SKIPPED
  ```

### Error Handling Strategy

**Fail-fast approach:**
- Exit with non-zero error code and descriptive message
- Do not skip problematic files
- Do not write partial results

**Specific behaviors:**
- Input directory doesn't exist or is not a directory → Exit with error
- Input directory contains no `TEST-*.xml` files → Exit with error
- XML files are malformed or can't be parsed → Exit with error
- Output directory doesn't exist → Create it (including parent directories)
- Output directory already exists → Prompt user to confirm deletion and overwrite
- File copy operation fails → Exit with error

### File Copy Behavior

- **Copy scope:** All files from input directory (not just `TEST-*.xml`)
- **Subdirectories:** Preserve directory structure recursively
- **Overwrite behavior:** If `<reports-dir>/<label>/` already exists, prompt user for confirmation before deleting and recreating

### Test Method Naming Convention

- **Regular tests:** `methodName()` (e.g., `testSimple()`)
- **Parameterized tests:** `methodName(ParamType)[index]` (e.g., `testWithParams(String)[1]`)
  - Include parameter types in parentheses
  - Include zero-based index in brackets
- **Use actual method names** (not `@DisplayName` values)

## Surefire Report Parser Research Findings

Based on examination of `surefire-report-parser` 3.5.5 source code:

### Core Model Classes

**`ReportTestSuite`:**
- Represents a test class/suite
- Contains: `List<ReportTestCase> testCases`, `numberOfErrors`, `numberOfFailures`, `numberOfSkipped`, `numberOfTests`
- Properties: `name`, `fullClassName`, `packageName`, `timeElapsed`

**`ReportTestCase`:**
- Represents a single test execution
- Properties: `name`, `fullName`, `className`, `fullClassName`, `time`
- Outcome flags: `hasFailure()`, `hasError()`, `hasSkipped()`, `isSuccessful()`
- Failure details: `failureMessage`, `failureType`, `failureDetail`

### Test Outcome Mapping

| Our Outcome | ReportTestCase Method | Notes |
|-------------|----------------------|-------|
| SUCCESS | `isSuccessful()` | Returns `!hasFailure() && !hasError() && !hasSkipped()` |
| FAILURE | `hasFailure()` | Assertion failures |
| ERROR | `hasError()` | Exceptions thrown |
| SKIPPED | `hasSkipped()` | Skipped tests |

### Method Naming

The library provides:
- `getName()` - Test method name as it appears in XML (includes parameterized test suffixes)
- `getFullName()` - Fully qualified: `fullClassName + "." + name`
- `getClassName()` - Simple class name
- `getFullClassName()` - Fully qualified class name

**The library uses the test name exactly as written in the XML by Surefire/Failsafe**, which already includes JUnit's parameterized test notation.

## Non-Functional Requirements

### Implementation Language

All code written in Kotlin.

### Execution Model

- **Command name:** `junitility` (installed to `~/.local/bin/junitility`)
- Packaged as an executable JAR with wrapper script
- Uses Maven Shade plugin to create fat JAR with all dependencies
- Wrapper script (`junitility`) invokes `java -jar target/junitility.jar`
- CLI entry point in `Main.kt` with standard argument parsing
- Installable via `make install` (symlinks wrapper script to `~/.local/bin/junitility`)
- Single-module Maven project (no multi-module structure needed initially)
- **Subcommand structure:**
  - `junitility report` - Full workflow (copy + CSV + JSON)
  - `junitility csv` - Generate CSV only
  - `junitility json` - Generate JSON only
  - `junitility diff` - Compare test results from two directories
  - `junitility compare` - Generate net impact reports with summary metrics

### Code Organization

- **Shared core functions:** CSV and JSON generation logic implemented as reusable Kotlin functions/classes
- **No subprocess calls:** The `report` command calls CSV and JSON generation functions directly in-process
  - Must NOT shell out to `junitility csv` or `junitility json` as subprocesses
  - All subcommands share the same JVM process and call core library code
- **Command handlers:** Each subcommand is a thin CLI wrapper around shared core functions
- **Suggested structure:**
  - Core generators: `CsvGenerator.generate(inputDir, outputFile)`, `JsonGenerator.generate(inputDir, outputFile, csvPath)`
  - Command implementations: `ReportCommand`, `CsvCommand`, `JsonCommand`
  - `ReportCommand` calls both generators directly after copying files

### XML Parsing Library

- **Library:** `org.apache.maven.surefire:surefire-report-parser`
- **Version:** 3.5.5 (or latest compatible)
- **Purpose:** Official Maven library for parsing Surefire/Failsafe XML test reports
- **Rationale:** 
  - Official parser from Maven Surefire project
  - Handles both Surefire and Failsafe report formats
  - Abstracts XML parsing details, provides Java object model
  - Industry standard for this use case

### JSON Output Format

- **Pretty-printed:** Yes (indented for readability)
- **Field ordering:** As specified in functional requirements (detailedReportPath, then classes array)

