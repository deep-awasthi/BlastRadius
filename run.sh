#!/usr/bin/env bash
# ─────────────────────────────────────────────────────────────
#  BlastRadius — Start Script
#  Usage: ./run.sh
# ─────────────────────────────────────────────────────────────
set -euo pipefail

RED='\033[0;31m'; GREEN='\033[0;32m'; CYAN='\033[0;36m'
YELLOW='\033[1;33m'; BOLD='\033[1m'; RESET='\033[0m'

echo ""
echo -e "${BOLD}${CYAN}  ⚡  BlastRadius — Engineering Dependency Intelligence Platform${RESET}"
echo -e "${CYAN}  ───────────────────────────────────────────────────────────────${RESET}"
echo ""

# ── Step 1: Check prerequisites ────────────────────────────
echo -e "${YELLOW}[1/3]${RESET} Checking prerequisites..."

if ! command -v java &> /dev/null; then
  echo -e "${RED}ERROR: Java is not installed. Please install Java 21+.${RESET}"
  exit 1
fi

if ! command -v mvn &> /dev/null; then
  echo -e "${RED}ERROR: Maven is not installed.${RESET}"
  exit 1
fi

if ! command -v docker &> /dev/null; then
  echo -e "${RED}ERROR: Docker is not installed.${RESET}"
  exit 1
fi

echo -e "  ${GREEN}✔${RESET} Java   : $(java -version 2>&1 | head -1)"
echo -e "  ${GREEN}✔${RESET} Maven  : $(mvn -version 2>&1 | head -1)"
echo -e "  ${GREEN}✔${RESET} Docker : $(docker --version)"
echo ""

# ── Step 2: Build ──────────────────────────────────────────
echo -e "${YELLOW}[2/3]${RESET} Building application with Maven..."
mvn clean package -DskipTests -q
echo -e "  ${GREEN}✔${RESET} Build successful"
echo ""

# ── Step 3: Docker Compose ────────────────────────────────
echo -e "${YELLOW}[3/3]${RESET} Starting Docker services (app, postgres, redis)..."
docker compose build --quiet
docker compose up -d
echo -e "  ${GREEN}✔${RESET} Services started"
echo ""

# ── Wait for app readiness ────────────────────────────────
echo -e "${CYAN}  Waiting for application to become ready...${RESET}"
ATTEMPTS=0
MAX_ATTEMPTS=30

until curl -sf http://localhost:8080/actuator/health > /dev/null 2>&1; do
  ATTEMPTS=$((ATTEMPTS + 1))
  if [ "$ATTEMPTS" -ge "$MAX_ATTEMPTS" ]; then
    echo -e "${RED}  Application did not start in time. Check logs: docker compose logs blast-radius-app${RESET}"
    break
  fi
  printf "."
  sleep 3
done

echo ""
echo ""
echo -e "${GREEN}${BOLD}  ✅  BlastRadius is running!${RESET}"
echo ""
echo -e "  ${BOLD}Application:${RESET}   ${CYAN}http://localhost:8080${RESET}"
echo -e "  ${BOLD}Swagger UI:${RESET}    ${CYAN}http://localhost:8080/swagger-ui.html${RESET}"
echo -e "  ${BOLD}Actuator:${RESET}      ${CYAN}http://localhost:8080/actuator${RESET}"
echo -e "  ${BOLD}Logs:${RESET}          docker compose logs -f blast-radius-app"
echo ""
echo -e "  ${YELLOW}Default credentials:${RESET} Register a new user at /register"
echo ""
