#!/bin/sh
#
# Design-time regression harness. Compiles 'src' + 'src_harness' and runs the
# code generator over the projects listed in 'harness/projects.txt'.
#
#   harness/run.sh baseline    # before a refactoring
#   harness/run.sh current     # after it
#   diff -r build/baseline build/current
#
# Empty diff = the generator produces byte-identical output = the step is safe.

set -e
cd "$(dirname "$0")/.."

OUT_NAME=${1:-current}
CLASSES=build/harness-classes
# slf4j-api is not used by the plug-ins. sqlite-jdbc 3.44+ logs through it and
# fails with NoClassDefFoundError without it; inside an IDE it is on the classpath anyway.
CP="lib/velocity-1.7-dep.jar;lib/jaxb-2.3.3_nb_12.3/jaxb-api.jar;lib/jaxb-2.3.3_nb_12.3/jaxb-impl.jar;lib/jaxb-2.3.3_nb_12.3/activation.jar;harness/lib/slf4j-api-1.7.30.jar"

rm -rf "$CLASSES" "build/$OUT_NAME"
mkdir -p "$CLASSES"

find src src_harness -name '*.java' > build/harness-sources.txt
javac -nowarn -encoding UTF-8 -cp "$CP" -d "$CLASSES" @build/harness-sources.txt

# 'src/com/sqldalmaker/cg/**/*.vm' are loaded as resources, not compiled
( cd src && find com -name '*.vm' -exec cp --parents {} "../$CLASSES/" \; )

java -cp "$CLASSES;$CP" com.sqldalmaker.harness.HarnessMain harness/projects.txt "build/$OUT_NAME"

# a free syntax check of the generated Go, when the toolchain happens to be around.
# It is how the stray "(Object) " cast in the Go ref-cursor code was found.
if command -v gofmt >/dev/null 2>&1; then
    if find "build/$OUT_NAME" -name '*.go' -exec gofmt -e {} \; >/dev/null 2>build/gofmt-errors.txt && [ ! -s build/gofmt-errors.txt ]; then
        echo "--- gofmt: generated Go is syntactically valid"
    else
        echo "--- gofmt reported problems in the generated Go:"
        cat build/gofmt-errors.txt
    fi
fi

echo "--- output: build/$OUT_NAME"
