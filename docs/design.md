# Design

## Architecture

Single-module Maven project with three CLI subcommands sharing common core libraries.

```
junitility/
├── Main.kt                    # CLI entry point, subcommand routing
├── commands/
│   ├── ReportCommand.kt       # report subcommand
│   ├── CsvCommand.kt          # csv subcommand
│   └── JsonCommand.kt         # json subcommand
├── core/
│   ├── XmlParser.kt           # Wraps surefire-report-parser
│   ├── CsvGenerator.kt        # CSV generation
│   ├── JsonGenerator.kt       # JSON generation
│   └── FileUtils.kt           # Directory copy operations
└── model/
    ├── TestResult.kt          # Internal test result model
    ├── TestSummary.kt         # Aggregated summary model
    └── Outcome.kt             # Enum: SUCCESS, FAILURE, ERROR, SKIPPED
```

## Data Flow

### report subcommand
1. Copy input directory → output directory
2. Parse XML files → List<TestResult>
3. Generate CSV from List<TestResult>
4. Generate JSON from List<TestResult>

### csv subcommand
1. Parse XML files → List<TestResult>
2. Generate CSV from List<TestResult>

### json subcommand
1. Parse XML files → List<TestResult>
2. Generate JSON from List<TestResult>

## Core Components

### XmlParser
```kotlin
object XmlParser {
    fun parseDirectory(dir: File): List<TestResult>
}
```
- Walks directory tree to find all `TEST-*.xml` files (case-sensitive)
- Uses `TestSuiteXmlParser.parse()` from surefire-report-parser on each XML file
- Converts `ReportTestSuite` → `List<TestResult>`
- Fail fast if no `TEST-*.xml` files found

### TestResult (Internal Model)
```kotlin
data class TestResult(
    val className: String,
    val methodName: String,
    val outcome: Outcome
)
```

### Outcome
```kotlin
enum class Outcome {
    SUCCESS, FAILURE, ERROR, SKIPPED
}
```

Mapping from `ReportTestCase`:
- SUCCESS: `isSuccessful() == true`
- FAILURE: `hasFailure() == true`
- ERROR: `hasError() == true`
- SKIPPED: `hasSkipped() == true`

### CsvGenerator
```kotlin
object CsvGenerator {
    fun generate(results: List<TestResult>, outputFile: File)
}
```
- Header: `ClassName,MethodName,Outcome`
- Hand-rolled RFC 4180 quoting:
  - Quote fields containing: comma, quote, or newline
  - Escape quotes by doubling them
- LF line endings

### JsonGenerator
```kotlin
object JsonGenerator {
    fun generate(
        results: List<TestResult>, 
        outputFile: File, 
        csvPath: String
    )
}
```
- Aggregates `List<TestResult>` by class → methods
- Uses Jackson `ObjectMapper` with Kotlin module
- Pretty-printed JSON with 2-space indent
- Top-level fields: `detailedReportPath`, `classes`

### TestSummary (JSON Model)
```kotlin
data class TestSummary(
    val detailedReportPath: String,
    val classes: List<ClassSummary>
)

data class ClassSummary(
    val className: String,
    val total: Int,
    val success: Int,
    val failures: Int,
    val errors: Int,
    val skipped: Int,
    val methods: List<MethodSummary>
)

data class MethodSummary(
    val methodName: String,
    val total: Int,
    val success: Int,
    val failures: Int,
    val errors: Int,
    val skipped: Int
)
```

### FileUtils
```kotlin
object FileUtils {
    fun copyDirectory(source: File, target: File)
    fun promptOverwrite(dir: File): Boolean
}
```
- Recursive directory copy (preserves structure)
- Console prompt for overwrite confirmation
  - Message: `"Directory {path} already exists. Delete and overwrite? (y/n): "`
  - Accepts: `y` or `n` (case-insensitive)
  - Uses `readLine()` from stdin
  - Returns: `true` if user enters `y`, `false` otherwise

## Command Implementations

### Main.kt
```kotlin
fun main(args: Array<String>) {
    // Uses Clikt for subcommand routing and argument parsing
    JunitilityCommand()
        .subcommands(ReportCommand(), CsvCommand(), JsonCommand())
        .main(args)
}
```

### ReportCommand
```kotlin
class ReportCommand : CliktCommand() {
    val inputDir by argument()
    val label by argument()
    val reportsDir by option("--reports-dir").default("reports")
    
    override fun run() {
        val input = File(inputDir)
        
        // Validate before copying (fail fast)
        validateInputDirectory(input)
        
        println("Parsing...")
        val outputDir = File(reportsDir, label)
        if (outputDir.exists()) {
            if (!FileUtils.promptOverwrite(outputDir)) {
                exitProcess(1)
            }
            outputDir.deleteRecursively()
        }
        
        FileUtils.copyDirectory(input, outputDir)
        
        val results = XmlParser.parseDirectory(outputDir)
        
        println("Generating CSV...")
        CsvGenerator.generate(results, File(outputDir, "test-results.csv"))
        
        println("Generating JSON...")
        JsonGenerator.generate(results, File(outputDir, "test-summary.json"), "test-results.csv")
        
        println("Done.")
    }
}
```

### CsvCommand
```kotlin
class CsvCommand : CliktCommand() {
    val inputDir by argument()
    val outputFile by argument()
    
    override fun run() {
        println("Parsing...")
        val results = XmlParser.parseDirectory(File(inputDir))
        
        println("Generating CSV...")
        CsvGenerator.generate(results, File(outputFile))
        
        println("Done.")
    }
}
```

### JsonCommand
```kotlin
class JsonCommand : CliktCommand() {
    val inputDir by argument()
    val outputFile by argument()
    val csvPath by option("--csv-path").default("test-results.csv")
    
    override fun run() {
        println("Parsing...")
        val results = XmlParser.parseDirectory(File(inputDir))
        
        println("Generating JSON...")
        JsonGenerator.generate(results, File(outputFile), csvPath)
        
        println("Done.")
    }
}
```

## Error Handling

All commands:
- Validate input directory exists and is a directory
- Validate input directory contains at least one `TEST-*.xml` file
- Catch XML parsing errors and exit with error code
- Catch I/O errors and exit with error code

Exit codes:
- 0: Success
- 1: User cancelled (overwrite prompt)
- 2: Invalid arguments / usage error
- 3: I/O error
- 4: Parse error

## Dependencies

```xml
<dependency>
    <groupId>org.apache.maven.surefire</groupId>
    <artifactId>surefire-report-parser</artifactId>
    <version>3.5.5</version>
</dependency>

<!-- Jackson for JSON serialization -->
<dependency>
    <groupId>com.fasterxml.jackson.core</groupId>
    <artifactId>jackson-databind</artifactId>
    <version>2.21.3</version>
</dependency>
<dependency>
    <groupId>com.fasterxml.jackson.module</groupId>
    <artifactId>jackson-module-kotlin</artifactId>
    <version>2.21.3</version>
</dependency>

<!-- Clikt for CLI argument parsing -->
<dependency>
    <groupId>com.github.ajalt.clikt</groupId>
    <artifactId>clikt</artifactId>
    <version>2.8.0</version>
</dependency>
```

## Test Method Name Extraction

Use `ReportTestCase.getName()` directly - includes JUnit's parameterized test notation as-is from XML.


## Progress Output

All commands output minimal progress messages to stdout using `println`:
- "Parsing..."
- "Generating CSV..." (if applicable)
- "Generating JSON..." (if applicable)
- "Done."

## Validation

- Input directory validation happens before any processing (fail fast)
- Check: directory exists, is a directory, contains at least one `TEST-*.xml` file
- For `report` command: validate before copying directory
