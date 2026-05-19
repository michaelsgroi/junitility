# JUnitility

A collection of JUnit 5 utilities and extensions.

## Build

```bash
make build
```

## Test

```bash
make test
```

## Install

```bash
make install
```

## Formatting

```bash
make format-apply
```

## Checks

```bash
make checks
```

## Contributing

### Regenerating Gold Files

The gold files used for testing are generated from the `goldfile-writer` module. To regenerate them:

```bash
make generate-gold
```

This will:
1. Run the test suite in `goldfile-writer/` (ignoring test failures, as they're intentional)
2. Copy the generated TEST-*.xml files to `src/test/resources/gold-data/`
3. Generate expected CSV and JSON output files in `src/test/resources/`
