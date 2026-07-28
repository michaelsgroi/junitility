SHELL := /bin/bash

.PHONY: all build test install uninstall checks spotless-check format-apply cpd-check clean generate-gold watch

# Default target: build + run tests.
all: test

# Alias for clarity
build:
	@mvn -q package -DskipTests

# Full build with all tests
test:
	@mvn -q test

# Generate gold data from goldfile-writer tests
generate-gold:
	@echo "==> Running goldfile-writer tests..."
	@cd goldfile-writer && mvn clean test -Dmaven.test.failure.ignore=true
	@echo "==> Copying XML files to src/test/resources/gold-data..."
	@mkdir -p src/test/resources/gold-data
	@cp goldfile-writer/target/surefire-reports/TEST-*.xml src/test/resources/gold-data/
	@echo "==> Gold data updated in src/test/resources/gold-data/"
	@ls -1 src/test/resources/gold-data/

# Install CLI wrapper to ~/.local/bin
install: build
	@mkdir -p ~/.local/bin
	@ln -sf $(CURDIR)/junitility ~/.local/bin/junitility
	@echo "Installed CLI: junitility -> $(CURDIR)/junitility"

# Remove CLI wrapper from ~/.local/bin
uninstall:
	@rm -f ~/.local/bin/junitility
	@echo "Removed: ~/.local/bin/junitility"

# On-demand static checks
checks: spotless-check cpd-check

spotless-check:
	@echo "==> Spotless check (ktlint)..."
	mvn -o spotless:check

# Apply formatting fixes
format-apply:
	@echo "==> Applying spotless formatting..."
	mvn -o spotless:apply

cpd-check:
	@echo "==> CPD check (copy-paste detection)..."
	mvn -o pmd:cpd-check

clean:
	mvn clean

# Watch a live surefire run using native watch(1) for clean screen refresh
# Usage: make watch REPORTS_DIR=/path/to/surefire-reports [TOTAL_TESTS=<n>]
watch: build
	@watch -n 2 java -cp $(CURDIR)/target/junitility.jar com.michaelsgroi.test.junitility.SurefireWatcherKt \
	  --reports-dir="$(REPORTS_DIR)" \
	  $(if $(TOTAL_TESTS),--total-tests=$(TOTAL_TESTS),) \
	  --once
