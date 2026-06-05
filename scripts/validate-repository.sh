#!/usr/bin/env bash
set -euo pipefail

required_files=(
  "README.md"
  "AGENTS.md"
  "AGENTS.local.md"
  "CLAUDE.md"
  "CLAUDE.local.md"
  "WORKFLOW.md"
  ".github/pull_request_template.md"
  ".claude/agents/pm.md"
  ".claude/agents/explorer.md"
  ".claude/agents/builder.md"
  ".claude/agents/tester.md"
  ".claude/agents/reporter.md"
  ".claude/skills/harness-local-server/SKILL.md"
  ".claude/skills/harness-playwright-evidence/SKILL.md"
  ".claude/skills/harness-linear-loop/SKILL.md"
  ".claude/skills/debug/SKILL.md"
  ".claude/skills/commit/SKILL.md"
  ".claude/skills/pull/SKILL.md"
  ".claude/skills/push/SKILL.md"
  ".claude/skills/land/SKILL.md"
  ".claude/skills/linear/SKILL.md"
  "docs/README.md"
  "docs/TEMPLATE.md"
  "docs/prd/README.md"
  "docs/plans/README.md"
  "docs/design/README.md"
  "docs/acceptance/README.md"
  "docs/acceptance/001-im-e2e-rag-acceptance.md"
  "docs/operations/README.md"
)

for file in "${required_files[@]}"; do
  test -f "$file"
done

require_text() {
  local file="$1"
  local pattern="$2"
  local message="$3"

  if ! grep -q "$pattern" "$file"; then
    echo "$message" >&2
    exit 1
  fi
}

require_absent_text() {
  local file="$1"
  local pattern="$2"
  local message="$3"

  if grep -qi "$pattern" "$file"; then
    echo "$message" >&2
    exit 1
  fi
}

legacy_spec_token="open""spec"
legacy_spec_dir="open""spec"

require_text AGENTS.md "## Test-First PR 提交规范" "AGENTS.md 缺少 Test-First PR 提交规范"
require_text AGENTS.md "test: add failing tests for xxx" "AGENTS.md 缺少 test 提交示例"
require_text AGENTS.md "impl: make xxx tests pass" "AGENTS.md 缺少 impl 提交示例"
require_text AGENTS.md "中间产物" "AGENTS.md 缺少中间产物提交规则"
require_text AGENTS.md "DTO Request / VO Response" "AGENTS.md 缺少 DTO Request / VO Response 契约规则"
require_text AGENTS.md "工程化守护门禁" "AGENTS.md 缺少工程化守护门禁规则"
require_text AGENTS.md "ContractConsistencyTest" "AGENTS.md 缺少契约守护测试命名规则"

require_text .github/pull_request_template.md "Test-first Evidence" "PR 模板缺少 Test-first Evidence"
require_text .github/pull_request_template.md "Focused contract tests" "PR 模板缺少契约守护测试记录"
require_text .github/pull_request_template.md "DTO Request / VO Response" "PR 模板缺少 DTO Request / VO Response 影响说明"
require_text .github/pull_request_template.md "Compatibility:" "PR 模板缺少兼容性说明"
require_text .github/pull_request_template.md "Compatibility exceptions" "PR 模板缺少兼容性例外说明"
require_text .github/pull_request_template.md "Code Review" "PR 模板缺少 Code Review 记录"
require_text .github/pull_request_template.md "Readonly review result" "PR 模板缺少只读审查结果"

require_text WORKFLOW.md "tracker:" "WORKFLOW.md 缺少 tracker 配置"
require_text WORKFLOW.md "kind: linear" "WORKFLOW.md 缺少 Linear tracker 配置"
require_text WORKFLOW.md "## Claude Workpad" "WORKFLOW.md 缺少 Claude Workpad 约定"
require_text WORKFLOW.md "command: claude" "WORKFLOW.md 缺少 Claude 命令配置"

require_absent_text AGENTS.md "$legacy_spec_token" "AGENTS.md 不应继续依赖旧规格框架"
require_absent_text AGENTS.local.md "$legacy_spec_token" "AGENTS.local.md 不应继续依赖旧规格框架"
require_absent_text CLAUDE.md "$legacy_spec_token" "CLAUDE.md 不应继续依赖旧规格框架"
require_absent_text CLAUDE.local.md "$legacy_spec_token" "CLAUDE.local.md 不应继续依赖旧规格框架"
require_absent_text WORKFLOW.md "$legacy_spec_token" "WORKFLOW.md 不应继续依赖旧规格框架"
require_absent_text .github/pull_request_template.md "$legacy_spec_token" "PR 模板不应继续依赖旧规格框架"
require_absent_text .github/workflows/ci.yml "$legacy_spec_token" "CI 不应继续依赖旧规格框架"
require_absent_text docs/README.md "$legacy_spec_token" "docs/README.md 不应继续依赖旧规格框架"
require_absent_text docs/plans/README.md "$legacy_spec_token" "docs/plans/README.md 不应继续依赖旧规格框架"
require_absent_text docs/acceptance/README.md "$legacy_spec_token" "docs/acceptance/README.md 不应继续依赖旧规格框架"

if [ -d "$legacy_spec_dir" ]; then
  echo "旧规格目录应移除，避免继续作为项目依赖" >&2
  exit 1
fi

require_text .github/workflows/ci.yml "Run engineering contract guards" "CI 缺少工程化契约守护测试入口"
require_text .github/workflows/ci.yml "ChatApiContractConsistencyTest" "CI 缺少 chat 契约守护测试"
require_text .github/workflows/ci.yml "LogApiContractConsistencyTest" "CI 缺少 log 契约守护测试"
require_text .github/workflows/ci.yml "FileUploadContractConsistencyTest" "CI 缺少 file 契约守护测试"
require_text .github/workflows/ci.yml "NotificationApiContractConsistencyTest" "CI 缺少 notification 契约守护测试"
require_text .github/workflows/ci.yml "UserApiContractConsistencyTest" "CI 缺少 user 契约守护测试"
require_text .github/workflows/ci.yml "AiApiContractConsistencyTest" "CI 缺少 ai 契约守护测试"
require_text .github/workflows/ci.yml "GatewayAuthWhitelistConfigTest" "CI 缺少 gateway 白名单守护测试"
require_text .github/workflows/ci.yml "RateLimitConfigTest" "CI 缺少 gateway 限流守护测试"
require_text .github/workflows/ci.yml "RabbitMqSenderTest" "CI 缺少 RabbitMQ 公共组件守护测试"

git diff --check
