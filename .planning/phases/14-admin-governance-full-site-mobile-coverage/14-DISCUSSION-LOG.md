# Phase 14: Admin Governance Full-Site Mobile Coverage - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-06-24
**Phase:** 14-admin-governance-full-site-mobile-coverage
**Areas discussed:** Admin information architecture, card density, governance state presentation, Policy/Audit safety, MVER-04 verification scope

---

## Gray Area Selection

| Option | Description | Selected |
|--------|-------------|----------|
| 全选（推荐） | 覆盖 Admin 信息架构、卡片密度、MCP/Plugin/Extension 状态、Policy/Audit 细节、验证范围，能给研究/计划最完整输入。 | ✓ |
| Admin 信息架构 | Overview、Registry、Operations、MCP、Plugin、Extension、Policy、Audit 是按现有路由分别卡片化，还是在 Registry 内做更强分组？ | |
| 卡片信息密度 | 每张卡默认显示哪些字段、哪些放入 Details；沿用 Phase 13 分层细节但需要决定 Admin 默认摘要强度。 | |
| 异常/治理状态表达 | unhealthy、disconnected、disabled、quarantined、selected、load-error 等状态在手机上如何排序、强调和呈现。 | |
| Policy/Audit 安全细节 | Policy decisions 与 Audit summaries 的 key IDs、actor/action/status/timestamp、redacted context/details 展开策略。 | |
| MVER-04 验证范围 | Admin mobile E2E 应覆盖哪些路由、fixture 状态、卡片/detail/redaction/no-overflow/tap/focus 断言。 | |

**User's choice:** 全选（推荐）
**Notes:** User wanted full decision coverage for downstream research and planning.

---

## Admin 信息架构

| Question | Option | Description | Selected |
|----------|--------|-------------|----------|
| 手机端 Admin Governance 的页面组织应如何处理？ | 保留现有路由（推荐） | Overview/Registry/Operations/Policy/Audit/Approvals 路由不变；每页内部卡片化。最符合“不改能力/不改 API”，也复用 Phase 11 导航。 | ✓ |
| 手机端 Admin Governance 的页面组织应如何处理？ | Registry 内强分组 | Registry 页面内部显式分成 Registry、MCP、Plugin、Extension 多段；路由仍不变，但页面内层级更强。 | |
| 手机端 Admin Governance 的页面组织应如何处理？ | 新增子入口 | 在 Admin landing/Overview 增加更多手机入口卡，引导到各治理页；可能接近新增导航体验，需控制范围。 | |
| Overview 默认应呈现成什么形态？ | 健康状态卡网格（推荐） | runtime/provider/tool/extension/MCP/plugin 各一张 status card，显示 health/count/message/link。符合 MADM-01。 | ✓ |
| Overview 默认应呈现成什么形态？ | 单页摘要流 | 按重要性从上到下列关键状态，减少卡片数量但对比性弱。 | |
| Overview 默认应呈现成什么形态？ | Overview 仅导航 | 主要作为入口页，详细状态放 Registry/Operations；可能低于 MADM-01 明确要求。 | |
| Registry 页内 MCP、Plugin、Extension 信息应如何组织？ | 分区卡片组（推荐） | 按 Tools/Extensions/MCP/Plugins 分区，每个来源或条目一张卡；适配当前 AdminRegistryStatusView 的 show* 方法。 | ✓ |
| Registry 页内 MCP、Plugin、Extension 信息应如何组织？ | 统一条目流 | 所有 registry/status 条目混排，用类型 chip 区分；更紧凑但治理语义不如分区清晰。 | |
| Registry 页内 MCP、Plugin、Extension 信息应如何组织？ | 只做 CSS 响应式 | 尽量不改 Java 结构，靠 CSS 包装；风险是仍保留 pipe-separated 文本，移动可读性差。 | |
| Operations 页应偏向哪种阅读模型？ | 指标分组卡（推荐） | Runs/Models/Tools/Policies/MCP/Plugins/Errors/Warnings 分组卡，适合手机快速巡检。 | ✓ |
| Operations 页应偏向哪种阅读模型？ | 事件/告警优先 | 先显示 errors/warnings，再显示指标；更偏 incident triage，但可能改变现有摘要顺序。 | |
| Operations 页应偏向哪种阅读模型？ | 保留原摘要顺序 | 仅把现有 summary 转卡片，低风险但发现异常效率一般。 | |

**User's choice:** 保留现有路由；Overview 健康状态卡网格；Registry 分区卡片组；Operations 指标分组卡。
**Notes:** Keep route model stable and cardify interiors.

---

## 卡片信息密度

| Question | Option | Description | Selected |
|----------|--------|-------------|----------|
| Admin 卡片默认摘要层应该显示多少信息？ | 关键字段摘要（推荐） | 默认只显示名称/状态/计数/最近时间/一句 message；IDs 与原始 metadata 放 Details。手机可扫读。 | ✓ |
| Admin 卡片默认摘要层应该显示多少信息？ | 偏详细摘要 | 默认展示更多 ID、source、reason、path；减少展开次数但卡片更长。 | |
| Admin 卡片默认摘要层应该显示多少信息？ | 极简摘要 | 只显示 name/status/count，几乎全部放 Details；最干净但治理排障效率低。 | |
| 长 JSON、错误、metadata、capabilities 应如何呈现？ | 分层 Details（推荐） | 默认摘要 + structured detail + advanced redacted detail；沿用 Phase 13 模式。 | ✓ |
| 长 JSON、错误、metadata、capabilities 应如何呈现？ | 只保留结构化详情 | 不展示 raw-like JSON，降低泄露/溢出风险，但排障能力较弱。 | |
| 长 JSON、错误、metadata、capabilities 应如何呈现？ | 默认展开详情 | 打开页面就显示多数详情；对手机过载，不建议。 | |
| 卡片字段格式应如何标准化？ | Label-value rows（推荐） | 每张卡用一致的 label/value 行、chips 和 action row，替代 pipe-separated Span 文本。 | ✓ |
| 卡片字段格式应如何标准化？ | 自然句摘要 | 用一句话描述状态，详情再结构化；更友好但测试/扫读不如 label-value 稳定。 | |
| 卡片字段格式应如何标准化？ | 保留文本拼接 | 最小改动，但和移动卡片目标冲突。 | |
| AdminApprovalQueueView 已复用 ApprovalCard，这一页怎么处理？ | 保持复用并补齐验证（推荐） | 不重做 ApprovalCard；Phase 14 只确保 Admin queue 页面纳入 full-site mobile E2E。 | ✓ |
| AdminApprovalQueueView 已复用 ApprovalCard，这一页怎么处理？ | 统一视觉再包一层 | 给 ApprovalCard 外加 Admin section/card 包装；可能重复但能统一页面密度。 | |
| AdminApprovalQueueView 已复用 ApprovalCard，这一页怎么处理？ | 重做审批卡 | 会和 Phase 13 决策冲突，不建议。 | |

**User's choice:** Key-field summaries; layered Details; label-value rows; keep ApprovalCard reuse and verify.
**Notes:** Strongly aligns with Phase 13 card/detail model.

---

## 异常/治理状态表达

| Question | Option | Description | Selected |
|----------|--------|-------------|----------|
| 异常状态在移动端列表中的优先级如何处理？ | 异常优先排序（推荐） | unhealthy/disconnected/quarantined/load-error/disabled 在各分区置顶；healthy/selected 正常项随后。手机巡检更快。 | ✓ |
| 异常状态在移动端列表中的优先级如何处理？ | 保持后端顺序 | 不改变数据顺序；低风险但异常可能被埋没。 | |
| 异常状态在移动端列表中的优先级如何处理？ | 只用视觉强调 | 顺序不变，通过 chips/color 强调异常；中等风险。 | |
| 不同治理状态如何视觉表达？ | 语义 chip + message（推荐） | 每卡用 status/risk chip 加短 message；错误详情进入 Details。沿用 Phase 13 chip/detail 思路。 | ✓ |
| 不同治理状态如何视觉表达？ | 纯文本状态 | 实现简单但不够扫读。 | |
| 不同治理状态如何视觉表达？ | 图标优先 | 视觉强，但 Vaadin/可访问性/测试成本更高。 | |
| Plugin selected/disabled/quarantined/load-error 状态默认显示哪些？ | 全部关键状态摘要（推荐） | 默认显示 lifecycle、health、selected、disabled、quarantined、load-error 摘要；metadata/capabilities 展开。符合 MADM-04。 | ✓ |
| Plugin selected/disabled/quarantined/load-error 状态默认显示哪些？ | 只显示 lifecycle/health | 更简洁，但 selected/quarantined 等治理状态不够明显。 | |
| Plugin selected/disabled/quarantined/load-error 状态默认显示哪些？ | 按异常才显示 | 正常卡更短，但状态可发现性差。 | |
| MCP disconnected/unhealthy/refresh metadata 怎么呈现？ | 连接+工具摘要（推荐） | Server card 默认显示 connection/health/tool count/last refresh；tools 作为子卡或 Details。符合 MADM-03。 | ✓ |
| MCP disconnected/unhealthy/refresh metadata 怎么呈现？ | 服务器和工具都同级卡 | 信息平铺，查找单个 tool 快，但页面可能很长。 | |
| MCP disconnected/unhealthy/refresh metadata 怎么呈现？ | 只展示 server 状态 | 工具细节不足，不满足 inspect tool status。 | |

**User's choice:** Abnormal states first; semantic chip + message; Plugin summary shows all key governance states; MCP server card shows connection/health/tool count/refresh metadata.
**Notes:** Planner may decide exact sorting and nested tool presentation.

---

## Policy/Audit 安全细节

| Question | Option | Description | Selected |
|----------|--------|-------------|----------|
| Policy decision 卡片默认应该显示哪些字段？ | 决策排障摘要（推荐） | decision、reason、tool、run/session IDs、timestamp 默认显示；context 放 redacted Details。符合 MADM-06。 | ✓ |
| Policy decision 卡片默认应该显示哪些字段？ | 只显示 decision/reason | 更短但排查关联 run/session/tool 不便。 | |
| Policy decision 卡片默认应该显示哪些字段？ | 显示完整 context | 不安全且容易溢出，不建议。 | |
| Audit summary 卡片默认应该显示哪些字段？ | 审计追踪摘要（推荐） | actor/source/action/status/resource/timestamp 默认显示；details 放 redacted Details。符合 MADM-07。 | ✓ |
| Audit summary 卡片默认应该显示哪些字段？ | 按资源聚合 | 同资源多条 audit 聚成一组；更高级但可能超出当前已有 summary。 | |
| Audit summary 卡片默认应该显示哪些字段？ | 只显示 action/status | 太少，审计价值不足。 | |
| Policy/Audit 的敏感字段策略应如何处理？ | 统一复用脱敏（推荐） | 把 Phase 13 RuntimeDetailRedactor 模式用于 Admin dense details；API keys/password/token/secrets 必须隐藏。 | ✓ |
| Policy/Audit 的敏感字段策略应如何处理？ | 各视图本地脱敏 | 低改动但规则不一致，当前已有重复 safe()/looksSensitive()。 | |
| Policy/Audit 的敏感字段策略应如何处理？ | 不显示 advanced detail | 最安全但降低治理排障价值。 | |
| Policy/Audit 的详情展开默认状态？ | 默认折叠（推荐） | 摘要可扫读，context/details 需用户主动展开；移动端安全且不拥挤。 | ✓ |
| Policy/Audit 的详情展开默认状态？ | 异常项自动展开 | 排障快但可能暴露过多信息和增加页面长度。 | |
| Policy/Audit 的详情展开默认状态？ | 全部展开 | 不适合手机。 | |

**User's choice:** Policy decision troubleshooting summary; Audit traceability summary; reuse unified redaction; details collapsed by default.
**Notes:** No full context/details in default summary.

---

## MVER-04 验证范围

| Question | Option | Description | Selected |
|----------|--------|-------------|----------|
| Phase 14 Admin mobile E2E 应覆盖哪些页面？ | 全 Admin 路由（推荐） | 覆盖 landing、overview、registry、operations、policy、audit、approvals，并在 registry 内验证 MCP/Plugin/Extension 分区。符合 MVER-04 full-site。 | ✓ |
| Phase 14 Admin mobile E2E 应覆盖哪些页面？ | 只覆盖 MVER-04 明确页 | overview/registry/operations/MCP/plugin/extension/policy/audit，不单独断言 landing/approvals。 | |
| Phase 14 Admin mobile E2E 应覆盖哪些页面？ | 代表性抽样 | 更快但不满足 full-site confidence。 | |
| E2E fixture 状态要覆盖到什么程度？ | 代表性状态矩阵（推荐） | 至少覆盖 healthy + unhealthy/disconnected/disabled/quarantined/load-error/selected 中代表项；不用穷举所有 DTO。 | ✓ |
| E2E fixture 状态要覆盖到什么程度？ | 仅 happy path | 简单但无法验证治理状态移动呈现。 | |
| E2E fixture 状态要覆盖到什么程度？ | 穷举所有状态 | 范围过大，适合 Java contract tests 而非浏览器 E2E。 | |
| 浏览器断言重点是什么？ | 卡片+详情+安全（推荐） | 断言 cards/sections 可见、Details 可展开、redaction 生效、无水平溢出、关键 controls/expanders 44px/focus。 | ✓ |
| 浏览器断言重点是什么？ | 只断言路由无溢出 | 已由 Phase 10 覆盖，不足以证明 Phase 14。 | |
| 浏览器断言重点是什么？ | 截图视觉回归 | 当前项目未建立截图基线，成本高且不稳定。 | |
| Java 测试与 Playwright 如何分工？ | Java 覆盖分支，E2E 覆盖路径（推荐） | Java/component/contract 测字段、selector、redaction、状态分支；Playwright 测代表性移动路径与真实布局。沿用 Phase 13。 | ✓ |
| Java 测试与 Playwright 如何分工？ | 全部放 E2E | 慢且易脆。 | |
| Java 测试与 Playwright 如何分工？ | 全部放 Java | 不能证明真实移动布局/溢出。 | |

**User's choice:** Full Admin route E2E; representative state matrix; card/detail/security/no-overflow/tap/focus assertions; Java branch breadth + Playwright path/layout split.
**Notes:** Real-device UAT and broad hardening remain Phase 15.

---

## the agent's Discretion

- Exact Java helper/component extraction and class names.
- Exact chip colors, typography, iconography, spacing, and breakpoint polish.
- Exact split of representative state fixtures between Java tests and Playwright.
- Whether MCP tools are nested child cards or structured Details, as long as mobile inspection is possible.

## Deferred Ideas

- New mobile-only Admin routes, deep-linkable details, incident-triage shortcuts, evidence copy/share, advanced Admin search/filter/export UX, and offline/PWA governance cache.
- Exhaustive browser permutations for every Admin DTO state.
- Screenshot-based visual regression as primary Phase 14 gate.
- Broad cross-browser/orientation/accessibility and real-device/UAT release documentation — Phase 15.
