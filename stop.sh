#!/usr/bin/env bash
# ─────────────────────────────────────────────────────────────
#  BlastRadius — Stop Script
#  Usage: ./stop.sh
# ─────────────────────────────────────────────────────────────
set -euo pipefail

CYAN='\033[0;36m'; GREEN='\033[0;32m'; BOLD='\033[1m'; RESET='\033[0m'

echo ""
echo -e "${BOLD}${CYAN}  ⚡  BlastRadius — Stopping services...${RESET}"
echo ""

docker compose down --remove-orphans

echo ""
echo -e "${GREEN}${BOLD}  ✅  All BlastRadius services stopped.${RESET}"
echo ""
