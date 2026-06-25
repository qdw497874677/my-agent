import { expect, test, type Page } from '@playwright/test';
import {
  expectNoPageHorizontalOverflow,
  expectPhase15RouteViewportBaseline,
  expectStableSelectorVisible,
  phase15ViewportCases,
  type MobileSmokeRoute,
  type MobileSmokeSelector,
  type Phase15ViewportCase,
} from './fixtures/mobile-smoke';

type Phase15OrientationRoute = MobileSmokeRoute & {
  navItem: string;
  criticalControls: MobileSmokeSelector[];
};

const routes: Phase15OrientationRoute[] = [
  {
    path: '/console',
    routeName: 'console',
    navItem: 'console',
    primaryContent: [
      { name: 'three-column workbench', selector: '[data-layout="three-column-workbench"]' },
      { name: 'chat stream column', selector: '[data-column="chat-event-stream"]' },
    ],
    primaryActions: [
      { name: 'chat input', selector: '[data-role="chat-input"]' },
      { name: 'send chat action', selector: '[data-action="send-chat"]' },
    ],
    criticalControls: [
      { name: 'chat input', selector: '[data-role="chat-input"]' },
      { name: 'send chat action', selector: '[data-action="send-chat"]' },
    ],
  },
  {
    path: '/admin/governance',
    routeName: 'admin-governance',
    navItem: 'admin/governance',
    primaryContent: [
      { name: 'admin governance surface', selector: '[data-surface="admin-governance"]' },
      { name: 'mobile critical root', selector: '[data-mobile-critical="true"]' },
    ],
    criticalControls: [
      { name: 'mobile critical root', selector: '[data-mobile-critical="true"]' },
    ],
  },
  {
    path: '/admin/governance/overview',
    routeName: 'admin-governance-overview',
    navItem: 'admin/governance/overview',
    primaryContent: [
      { name: 'inspect-only admin surface', selector: '[data-admin-surface="inspect-only"]' },
      { name: 'overview empty state', selector: '[data-state="empty-governance-overview"]' },
    ],
    criticalControls: [
      { name: 'overview empty state', selector: '[data-state="empty-governance-overview"]' },
    ],
  },
  {
    path: '/admin/governance/registry',
    routeName: 'admin-registry-status',
    navItem: 'admin/governance/registry',
    primaryContent: [
      { name: 'inspect-only registry surface', selector: '[data-admin-surface="inspect-only"]' },
      { name: 'registry empty state', selector: '[data-state="empty-registry-status"]' },
    ],
    primaryActions: [
      { name: 'mutation controls absent marker', selector: '[data-mutation-controls="absent"]' },
    ],
    criticalControls: [
      { name: 'mutation controls absent marker', selector: '[data-mutation-controls="absent"]' },
    ],
  },
  {
    path: '/admin/governance/operations',
    routeName: 'admin-operations',
    navItem: 'admin/governance/operations',
    primaryContent: [
      { name: 'operations surface', selector: '[data-admin-surface="operations-summary"]' },
      { name: 'runs operations section', selector: '[data-operations-section="runs"]' },
      { name: 'warnings operations section', selector: '[data-operations-section="warnings"]' },
    ],
    criticalControls: [
      { name: 'runs operations section', selector: '[data-operations-section="runs"]' },
      { name: 'warnings operations section', selector: '[data-operations-section="warnings"]' },
    ],
  },
  {
    path: '/admin/governance/policy-decisions',
    routeName: 'admin-policy-decisions',
    navItem: 'admin/governance/policy-decisions',
    primaryContent: [
      { name: 'policy decisions inspect surface', selector: '[data-admin-surface="inspect-only"]' },
      { name: 'policy decisions empty state', selector: '[data-state="empty-policy-decisions"]' },
    ],
    criticalControls: [
      { name: 'policy decisions empty state', selector: '[data-state="empty-policy-decisions"]' },
    ],
  },
  {
    path: '/admin/governance/audits',
    routeName: 'admin-audit-summaries',
    navItem: 'admin/governance/audits',
    primaryContent: [
      { name: 'audit summaries inspect surface', selector: '[data-admin-surface="inspect-only"]' },
      { name: 'audit summaries empty state', selector: '[data-state="empty-audit-summaries"]' },
    ],
    criticalControls: [
      { name: 'audit summaries empty state', selector: '[data-state="empty-audit-summaries"]' },
    ],
  },
  {
    path: '/admin/governance/approvals',
    routeName: 'admin-approval-queue',
    navItem: 'admin/governance/approvals',
    primaryContent: [
      { name: 'approval queue governance surface', selector: '[data-admin-surface="separated-governance"]' },
      { name: 'approval queue empty state', selector: '[data-state="empty-admin-approvals"]' },
    ],
    criticalControls: [
      { name: 'approval queue empty state', selector: '[data-state="empty-admin-approvals"]' },
    ],
  },
];

test.describe('Phase 15 layered browser all-route orientation release smoke', () => {
  for (const route of routes) {
    for (const viewport of phase15ViewportCases) {
      test(`${route.path} keeps shell, primary content, and no overflow in ${viewport.name}`, async ({ page }) => {
        await expectPhase15RouteViewportBaseline(page, route, viewport);
        await expectActiveNavigationReachable(page, route);
        await expectCriticalControlVisible(page, route);

        if (viewport.kind === 'phone-landscape') {
          await expectLandscapeShellNavigationUsable(page, route, viewport);
        }
      });
    }
  }
});

async function expectActiveNavigationReachable(page: Page, route: Phase15OrientationRoute): Promise<void> {
  await expectStableSelectorVisible(page, '[data-nav="primary"]');
  await expect(page.locator(`[data-nav-item="${route.navItem}"]`).first()).toHaveAttribute('data-nav-active', 'true');
}

async function expectCriticalControlVisible(page: Page, route: Phase15OrientationRoute): Promise<void> {
  expect(route.criticalControls.length, `${route.routeName} should define a critical control`).toBeGreaterThan(0);
  for (const control of route.criticalControls) {
    await expectStableSelectorVisible(page, control.selector);
  }
}

async function expectLandscapeShellNavigationUsable(
  page: Page,
  route: Phase15OrientationRoute,
  viewport: Phase15ViewportCase,
): Promise<void> {
  await expectStableSelectorVisible(page, '[data-shell="pi-responsive-shell"]');
  await expectStableSelectorVisible(page, '[data-nav="primary"]');
  await expectActiveNavigationReachable(page, route);

  const drawerTrigger = page.locator('[data-shell-drawer-trigger]').first();
  if (await drawerTrigger.isVisible().catch(() => false)) {
    await drawerTrigger.click();
    await expect(page.locator('[data-shell="pi-responsive-shell"]').first()).toHaveAttribute('data-shell-drawer-open', 'true');
    await expect(page.locator(`[data-nav-item="${route.navItem}"]`).first()).toBeVisible();
  }

  await expectCriticalControlVisible(page, route);
  await expectNoPageHorizontalOverflow(page, viewport.kind === 'phone-landscape' ? 2 : 1);
}
