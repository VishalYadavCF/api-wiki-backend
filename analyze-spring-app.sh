#!/bin/bash
#
# Script to analyze a Spring Boot project and generate a call graph
#

# Show usage info if no arguments provided
if [ $# -lt 1 ]; then
  echo "Usage: ./analyze-spring-app.sh /path/to/spring-boot-project [options]"
  echo ""
  echo "Options:"
  echo "  --no-build    Skip building the target project"
  echo "  --output=DIR  Specify output directory for generated artifacts (default: ./output)"
  echo ""
  exit 1
fi

# Parse arguments
TARGET_PROJECT=$1
shift

# Default values
SHOULD_BUILD=true
OUTPUT_DIR="./output"

# Parse options
while [ "$#" -gt 0 ]; do
  case "$1" in
    --no-build)
      SHOULD_BUILD=false
      ;;
    --output=*)
      OUTPUT_DIR="${1#*=}"
      ;;
    *)
      echo "Unknown option: $1"
      exit 1
      ;;
  esac
  shift
done

# Check if the target project exists
if [ ! -d "$TARGET_PROJECT" ]; then
  echo "Error: Target project directory does not exist: $TARGET_PROJECT"
  exit 1
fi

# Determine the classes directory based on project structure
CLASSES_DIR="$TARGET_PROJECT/target/classes"

# Build the project if needed
if [ "$SHOULD_BUILD" = true ]; then
  echo "Building target Spring Boot project..."
  (cd "$TARGET_PROJECT" && mvn clean compile)

  # Check if build was successful
  if [ $? -ne 0 ]; then
    echo "Error: Failed to build the target project"
    exit 1
  fi
fi

# Make sure classes directory exists
if [ ! -d "$CLASSES_DIR" ]; then
  echo "Error: Classes directory not found: $CLASSES_DIR"
  echo "       Did the build fail, or is this not a Maven project?"
  exit 1
fi

# Create output directory if it doesn't exist
mkdir -p "$OUTPUT_DIR"

echo "======================================================"
echo "Analyzing Spring Boot application"
echo "======================================================"
echo "Source:  $TARGET_PROJECT"
echo "Classes: $CLASSES_DIR"
echo "Output:  $OUTPUT_DIR"
echo "======================================================"

# Get the current directory (where this script is located)
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Build the call graph generator project if needed
echo "Ensuring call graph generator is built..."
(cd "$SCRIPT_DIR" && mvn clean package)

# Check if build was successful
if [ $? -ne 0 ]; then
  echo "Error: Failed to build the call graph generator"
  exit 1
fi

# Get dependencies classpath using maven
CLASSPATH=$(cd "$SCRIPT_DIR" && mvn dependency:build-classpath -DincludeTypes=jar -Dmdep.outputFile=/dev/stdout -q)
CLASSPATH="$SCRIPT_DIR/target/spring-api-callgraph-1.0-SNAPSHOT.jar:$CLASSPATH"

# Run the call graph generator with full classpath
echo "Generating call graph..."
java -cp "$CLASSPATH" com.redcat.tutorials.JCallGraph "$CLASSES_DIR" "$OUTPUT_DIR"

echo "Analysis complete!"
echo "Results can be found in: $OUTPUT_DIR"
