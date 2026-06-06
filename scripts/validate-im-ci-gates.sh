#!/usr/bin/env bash
# ==============================================================================
# IM Focused Tests & E2E Smoke CI Gates 验证脚本
# 验证 CI 包含 IM focused tests 入口和 E2E smoke 入口
# ==============================================================================
set -euo pipefail

CI_FILE=".github/workflows/ci.yml"
ERRORS=()

# ------------------------------------------------------------------------------
# 验证 CI 包含 IM focused tests
# ------------------------------------------------------------------------------
echo "=== 检查 CI 包含 IM focused tests ==="

IM_FOCUSED_TESTS=(
  "ChatMessageServiceImplTest"
  "ChatRoomServiceImplTest"
  "ChatSessionServiceImplTest"
  "ChatMomentServiceImplTest"
  "ChatMessagePushHandlerTest"
  "ChatMqProducerTest"
  "ChatSessionListenerTest"
  "UserFriendServiceImplTest"
)

for test_name in "${IM_FOCUSED_TESTS[@]}"; do
  if grep -q "$test_name" "$CI_FILE"; then
    echo "  ✓ CI 包含 $test_name"
  else
    echo "  ✗ CI 缺少 IM focused test: $test_name" >&2
    ERRORS+=("CI 缺少 IM focused test: $test_name")
  fi
done

# ------------------------------------------------------------------------------
# 验证 CI 或 PR 包含 E2E smoke 入口
# ------------------------------------------------------------------------------
echo ""
echo "=== 检查 CI/PR 包含 E2E smoke 入口 ==="

E2E_SMOKE_CHECKS=(
  "im.*smoke"
  "e2e.*im"
  "im.*e2e"
)

HAS_E2E=false
for pattern in "${E2E_SMOKE_CHECKS[@]}"; do
  if grep -qiE "$pattern" "$CI_FILE"; then
    HAS_E2E=true
    echo "  ✓ 找到 E2E smoke 入口: $pattern"
    break
  fi
done

if [ "$HAS_E2E" = false ]; then
  echo "  ✗ CI 缺少 E2E smoke 入口" >&2
  ERRORS+=("CI 缺少 E2E smoke 入口")
fi

# ------------------------------------------------------------------------------
# 验证 validate-repository.sh 包含 IM focused tests 守护
# ------------------------------------------------------------------------------
echo ""
echo "=== 检查 validate-repository.sh 包含 IM focused tests 守护 ==="

VALIDATE_SCRIPT="scripts/validate-repository.sh"

IM_VALIDATE_CHECKS=(
  "ChatMessageServiceImplTest"
  "ChatRoomServiceImplTest"
  "ChatSessionServiceImplTest"
  "ChatMomentServiceImplTest"
  "ChatMqProducerTest"
  "ChatSessionListenerTest"
  "Run IM E2E smoke tests"
  "IM focused tests"
)

for check in "${IM_VALIDATE_CHECKS[@]}"; do
  if grep -q "$check" "$VALIDATE_SCRIPT"; then
    echo "  ✓ $VALIDATE_SCRIPT 包含: $check"
  else
    echo "  ✗ $VALIDATE_SCRIPT 缺少 IM focused tests 守护: $check" >&2
    ERRORS+=("$VALIDATE_SCRIPT 缺少 IM focused tests 守护: $check")
  fi
done

# ------------------------------------------------------------------------------
# 输出结果
# ------------------------------------------------------------------------------
echo ""
if [ ${#ERRORS[@]} -eq 0 ]; then
  echo "✓ 所有 IM CI 门禁检查通过"
  exit 0
else
  echo "✗ 发现 ${#ERRORS[@]} 个错误:"
  for err in "${ERRORS[@]}"; do
    echo "  - $err" >&2
  done
  exit 1
fi