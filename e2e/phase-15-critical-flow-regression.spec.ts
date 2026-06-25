import { expect, test, type Locator, type Page } from '@playwright/test';
import {
  expectNoPageHorizontalOverflow,
  expectFocusVisible,
  expectTapTargetAtLeast,
} from './fixtures/mobile-smoke';
import {
  mobileToolApprovalHint,
  phase13RuntimeCardMatrixHint,
} from './fixtures/fake-runtime';

type AdminReleaseRouteCase = {
  name: string;
  path: string;
  routeName: string;
  selectors: string[];
};

const sensitiveMarkers = [
  'sk-test-secret',
  'PI_PHASE7_FAKE_SECRET_DO_NOT_LEAK',
  'PI_PHASE8_FAKE_SECRET_DO_NOT_LEAK',
  'rawSecret',
  'raw-token-value',
];

const adminReleaseRoutes: AdminReleaseRouteCase[] = [
  {
    name: 'landing',
    path: '/admin/governance',
    routeName: 'admin-governance',
    selectors: ['[data-surface="admin-governance"]', '[data-mobile-critical="true"]'],
  },
  {
    name: 'overview',
    path: '/admin/governance/overview',
    routeName: 'admin-governance-overview',
    selectors: ['[data-admin-overview-card]', '[data-admin-card]'],
  },
  {
    name: 'registry',
    path: '/admin/governance/registry',
    routeName: 'admin-registry-status',
    selectors: ['[data-admin-registry-section]', '[data-mcp-server-card], [data-plugin-card], [data-extension-source-card]'],
  },
  {
    name: 'operations',
    path: '/admin/governance/operations',
    routeName: 'admin-operations',
    selectors: ['[data-operations-card]', '[data-admin-card]'],
  },
  {
    name: 'policy decisions',
    path: '/admin/governance/policy-decisions',
    routeName: 'admin-policy-decisions',
    selectors: ['[data-policy-decision-card]', '[data-admin-details="policy-context"]'],
  },
  {
    name: 'audits',
    path: '/admin/governance/audits',
    routeName: 'admin-audit-summaries',
    selectors: ['[data-audit-card]', '[data-admin-details="audit-details"]'],
  },
  {
    name: 'approvals',
    path: '/admin/governance/approvals',
    routeName: 'admin-approval-queue',
    selectors: ['[data-admin-surface="separated-governance"]', '[data-event-category="approval"], [data-state="empty-admin-approvals"], .pi-approval-card'],
  },
];

test.describe('Phase 15 Console critical-flow release gate', () => {
  test('mobile console covers run, session, cancel, and runtime inspection surfaces', async ({ page }) => {
    await page.goto('/console', { waitUntil: 'domcontentloaded' });

    await expect(page.locator('[data-console-panel="chat"][data-console-panel-active="true"]').first()).toBeVisible();
    await expect(page.locator('[data-role="chat-composer"]').first()).toBeVisible();
    await expectNoPageHorizontalOverflow(page);

    await openConsolePanel(page, 'agents');
    await expectNoPageHorizontalOverflow(page);
    const generalAgent = page.locator('[data-agent-id="cloud-general-agent"]').first();
    await expect(generalAgent).toBeVisible();
    await generalAgent.locator('[data-primary-action^="general-agent-"]').first().click();

    const input = page.locator('[data-role="chat-input"]').first();
    await input.fill(`Phase 15 critical Console release gate\n${mobileToolApprovalHint()}\n${phase13RuntimeCardMatrixHint()}`);
    const send = page.locator('[data-action="send-chat"]').first();
    await expectTapTargetAtLeast(send, 44, 'Phase 15 send chat action');
    await send.click();

    const feed = page.locator('[data-role="event-feed"]').first();
    await expect(feed).toBeVisible();
    const countAfterSend = await countRunEvents(feed);
    await expect.poll(
      async () => countRunEvents(feed),
      { message: 'Phase 15 critical flow should append runtime events after Send' },
    ).toBeGreaterThan(countAfterSend);

    await expectRuntimeInspectionSurface(page, feed);
    await expandRuntimeDetails(page, feed);
    await expectNoPageHorizontalOverflow(page);

    await openConsolePanel(page, 'sessions');
    const activeSessionCard = page.locator('[data-role="session-card"][data-session-active="true"]').first();
    await expect(activeSessionCard).toBeVisible();
    await activeSessionCard.click();

    await openConsolePanel(page, 'run-context');
    await expect(page.locator('[data-action="cancel-run"], [data-role="run-status"], [data-role="event-feed"]').first()).toBeVisible();
    await openConsolePanel(page, 'chat');
    await cancelOrAcceptTerminal(page, feed);
  });
});

test.describe('Phase 15 Admin critical inspection release gate', () => {
  for (const route of adminReleaseRoutes) {
    test(`${route.name} admin route supports card/detail inspection safely`, async ({ page }) => {
      await page.goto(route.path, { waitUntil: 'domcontentloaded' });
      await expect(page.locator(`[data-route="${route.routeName}"]`).first()).toBeVisible();
      for (const selector of route.selectors) {
        await expect(page.locator(selector).first(), `${route.name} selector ${selector}`).toBeVisible();
      }
      await expectNoPageHorizontalOverflow(page);

      await expandFirstVisibleAdminDetails(page);
      await expectNoPageHorizontalOverflow(page);
      await expectSensitiveMarkersRedacted(page);

      const control = await firstVisibleAdminTouchControl(page);
      await expectTapTargetAtLeast(control, 44, `Phase 15 ${route.name} Admin control`);
      await expectFocusVisible(page, control, `Phase 15 ${route.name} Admin control`);
    });
  }
});

async function openConsolePanel(page: Page, target: 'agents' | 'sessions' | 'run-context' | 'chat'): Promise<void> {
  await page.locator(`[data-action="show-console-panel"][data-console-target="${target}"]`).first().click();
  await expect(page.locator(`[data-console-panel="${target}"][data-console-panel-active="true"]`).first()).toBeVisible();
}

async function countRunEvents(feed: Locator): Promise<number> {
  return feed.locator('[data-event-category], [data-event-type], [data-run-event]').count();
}

async function expectRuntimeInspectionSurface(page: Page, feed: Locator): Promise<void> {
  await expect(
    feed.locator('[data-event-category="tool"], [data-event-category="approval"], [data-event-category="model"], [data-event-category="policy"]').first()
      .or(page.locator('[data-panel="approvals"]').first())
      .or(page.locator('[data-console-panel="run-context"]').first()),
    'Phase 15 critical flow should expose runtime/tool/approval inspection surfaces',
  ).toBeVisible();
}

async function expandRuntimeDetails(page: Page, feed: Locator): Promise<void> {
  const details = feed.locator('[data-expandable="true"] vaadin-details, [data-detail-layer="advanced"]');
  const count = await details.count();
  for (let index = 0; index < Math.min(count, 3); index += 1) {
    const candidate = details.nth(index);
    if (await candidate.isVisible().catch(() => false)) {
      await candidate.click().catch(() => undefined);
    }
  }
  await expect(page.locator('body')).not.toContainText('sk-live-secret');
  await expect(page.locator('body')).not.toContainText('raw-token-value');
}

async function cancelOrAcceptTerminal(page: Page, feed: Locator): Promise<void> {
  const composerStatus = page.locator('[data-role="composer-run-status"]').first();
  const primaryCancel = page.locator('[data-action="cancel-run-primary"]').first();
  if (await primaryCancel.isVisible({ timeout: 1500 }).catch(() => false)) {
    await expectTapTargetAtLeast(primaryCancel, 44, 'Phase 15 primary cancel action');
    await primaryCancel.click();
    await expect(composerStatus.or(feed)).toContainText(/cancelling|cancelled|terminal|completed/i);
  } else {
    await expect(composerStatus.or(feed)).toContainText(/terminal|completed|cancelled|timed out|failed/i);
  }
}

async function expandFirstVisibleAdminDetails(page: Page): Promise<void> {
  const details = page.locator('vaadin-details[data-admin-details], [data-admin-details] vaadin-details, [data-expandable="true"] vaadin-details');
  const count = await details.count();
  for (let index = 0; index < count; index += 1) {
    const candidate = details.nth(index);
    if (await candidate.isVisible().catch(() => false)) {
      await candidate.click().catch(() => undefined);
      return;
    }
  }
}

async function expectSensitiveMarkersRedacted(page: Page): Promise<void> {
  const body = page.locator('body');
  for (const marker of sensitiveMarkers) {
    await expect(body, `${marker} should stay redacted from Phase 15 Admin release details`).not.toContainText(marker);
  }
}

async function firstVisibleAdminTouchControl(page: Page): Promise<Locator> {
  const controls = page.locator([
    'vaadin-details[data-admin-details]',
    '[data-admin-details] vaadin-details',
    '[data-expandable="true"] vaadin-details',
    '[data-action]',
    '[data-risk-action]',
    '[data-primary-action]',
    '[data-read-only-refresh]',
    'button',
    'a',
  ].join(', '));
  const count = await controls.count();
  for (let index = 0; index < count; index += 1) {
    const candidate = controls.nth(index);
    if (await candidate.isVisible().catch(() => false)) {
      return candidate;
    }
  }
  throw new Error('Expected at least one visible Phase 15 Admin Details/control/action.');
}
