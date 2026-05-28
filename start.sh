#!/bin/bash
echo "======================================"
echo "  Java Assignment Compiler - Startup"
echo "======================================"

# Check Java
if ! command -v java &>/dev/null; then
  echo "❌ Java not found. Install Java 17+ from https://adoptium.net"
  exit 1
fi
echo "✅ Java: $(java -version 2>&1 | head -1)"

# Check Maven
if ! command -v mvn &>/dev/null; then
  echo "❌ Maven not found. Install Maven from https://maven.apache.org"
  exit 1
fi
echo "✅ Maven: $(mvn -version 2>&1 | head -1)"

# Check Node (only needed if rebuilding frontend)
if command -v node &>/dev/null; then
  echo "✅ Node: $(node -v)"
fi

echo ""
echo "📦 Building backend..."
cd backend
mvn package -q -DskipTests 2>&1

if [ $? -ne 0 ]; then
  echo "❌ Build failed. Check errors above."
  exit 1
fi

echo "✅ Build successful!"
echo ""
echo "🚀 Starting server on http://localhost:8080"
echo "   Press Ctrl+C to stop"
echo ""
java -jar target/java-assignment-1.0.0.jar
