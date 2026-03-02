@echo off
REM ===================================
REM Order Manager - Local Setup Script
REM ===================================

echo 🚀 Setting up Order Manager for local development...

REM Check if .env exists
if not exist .env (
    echo 📝 Creating .env file from template...
    copy .env.example .env
    echo ⚠️  Please edit .env file with your configuration before continuing
    exit /b 1
)

REM Check if Java is installed
java -version >nul 2>&1
if errorlevel 1 (
    echo ❌ Java is not installed. Please install Java 17 or higher.
    exit /b 1
)

REM Check if Maven is installed
mvn -version >nul 2>&1
if errorlevel 1 (
    echo ❌ Maven is not installed. Please install Maven 3.6 or higher.
    exit /b 1
)

echo ✅ Prerequisites check passed

REM Build the project
echo 🔨 Building project...
call mvn clean install -DskipTests

if errorlevel 1 (
    echo ❌ Build failed. Please check the errors above.
    exit /b 1
)

REM Run the application
echo 🎯 Starting application in DEV mode...
call mvn spring-boot:run -Dspring-boot.run.profiles=dev

if errorlevel 1 (
    echo ❌ Application failed to start. Check the logs above.
    exit /b 1
)

