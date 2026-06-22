import { expect, test, type Page } from '@playwright/test';
import {
  expectFocusVisible,
  expectNoPageHorizontalOverflow,
  expectPrimaryContentOrActionVisible,
  expectStableSelectorVisible,
  expectTapTargetAtLeast,
  type MobileSmokeRoute,
} from './fixtures/mobile-smoke';

type ShellRoute = MobileSmokeRoute & {
  title: string;
  navItem: string;
};

const routes: ShellRoute[] = [
  {
    path: '/console',
    routeName: 'console',
    title: 'Pi Agent Console',
    navItem: 'console',
    primaryContent: [
      { name: 'three-column workbench', selector: '[data-layout="three-column-workbench"]' },
      { name: 'chat stream column', selector: '[data-column="chat-event-stream"]' },
    ],
    primaryActions: [
      { name: 'chat input', selector: '[data-role="chat-input"]' },
      { name: 'send chat action', selector: '[data-action="send-chat"]' },
    ],
  },
  {
    path: '/admin/governance',
    routeName: 'admin-governance',
    title: 'Pi Admin Governance',
    navItem: 'admin/governance',
    primaryContent: [
      { name: 'admin governance surface', selector: '[data-surface="admin-governance"]' },
      { name: 'mobile critical root', selector: '[data-mobile-critical="true"]' },
    ],
  },
  {
    path: '/admin/governance/overview',
    routeName: 'admin-governance-overview',
    title: 'Pi Admin Governance Overview',
    navItem: 'admin/governance/overview',
    primaryContent: [
      { name: 'inspect-only admin surface', selector: '[data-admin-surface="inspect-only"]' },
      { name: 'overview empty state', selector: '[data-state="empty-governance-overview"]' },
    ],
  },
  {
    path: '/admin/governance/registry',
    routeName: 'admin-registry-status',
    title: 'Pi Admin Registry Status',
    navItem: 'admin/governance/registry',
    primaryContent: [
      { name: 'inspect-only registry surface', selector: '[data-admin-surface="inspect-only"]' },
      { name: 'registry empty state', selector: '[data-state="empty-registry-status"]' },
    ],
    primaryActions: [
      { name: 'mutation controls absent marker', selector: '[data-mutation-controls="absent"]' },
    ],
  },
  {
    path: '/admin/governance/operations',
    routeName: 'admin-operations',
    title: 'Pi Admin Operations',
    navItem: 'admin/governance/operations',
    primaryContent: [
      { name: 'operations surface', selector: '[data-admin-surface="operations-summary"]' },
      { name: 'runs operations section', selector: '[data-operations-section="runs"]' },
      { name: 'warnings operations section', selector: '[data-operations-section="warnings"]' },
    ],
  },
  {
    path: '/admin/governance/policy-decisions',
    routeName: 'admin-policy-decisions',
    title: 'Pi Admin Policy Decisions',
    navItem: 'admin/governance/policy-decisions',
    primaryContent: [
      { name: 'policy decisions inspect surface', selector: '[data-admin-surface="inspect-only"]' },
      { name: 'policy decisions empty state', selector: '[data-state="empty-policy-decisions"]' },
    ],
  },
  {
    path: '/admin/governance/audits',
    routeName: 'admin-audit-summaries',
    title: 'Pi Admin Audit Summaries',
    navItem: 'admin/governance/audits',
    primaryContent: [
      { name: 'audit summaries inspect surface', selector: '[data-admin-surface="inspect-only"]' },
      { name: 'audit summaries empty state', selector: '[data-state="empty-audit-summaries"]' },
    ],
  },
  {
    path: '/admin/governance/approvals',
    routeName: 'admin-approval-queue',
    title: 'Pi Admin Approval Queue',
    navItem: 'admin/governance/approvals',
    primaryContent: [
      { name: 'approval queue governance surface', selector: '[data-admin-surface="separated-governance"]' },
      { name: 'approval queue empty state', selector: '[data-state="empty-admin-approvals"]' },
    ],
  },
];

test.describe('Phase 11 shared shell direct route contract', () => {
  for (const route of routes) {
    test(`${route.path} renders shared shell with active nav and no overflow`, async ({ page }) => {
      await page.goto(route.path, { waitUntil: 'domcontentloaded' });

      await expectStableSelectorVisible(page, '[data-shell="pi-responsive-shell"]');
      await expectStableSelectorVisible(page, `[data-route="${route.routeName}"]`);
      await expectStableSelectorVisible(page, '[data-nav="primary"]');
      await expectStableSelectorVisible(page, `[data-nav-item="${route.navItem}"][data-nav-active="true"]`);
      await expect(page.locator('[data-page-title]').first()).toContainText(route.title);
      await expectPrimaryContentOrActionVisible(page, route);
      await expectNoPageHorizontalOverflow(page);
    });
  }
});

test.describe('Phase 11 mobile drawer navigation, touch, and focus', () => {
  test('drawer opens, navigates every route, closes, and returns focus to trigger', async ({ page }) => {
    await page.goto('/console', { waitUntil: 'domcontentloaded' });

    const trigger = page.locator('[data-shell-drawer-trigger]').first();
    const close = page.locator('[data-shell-drawer-close]').first();
    await expectTapTargetAtLeast(trigger, 44, 'drawer trigger');
    await expectFocusVisible(page, trigger, 'drawer trigger');
    await trigger.click();
    await expect(page.locator('[data-shell="pi-responsive-shell"]')).toHaveAttribute('data-shell-drawer-open', 'true');
    await expectTapTargetAtLeast(close, 44, 'drawer close');

    for (const route of routes) {
      await openDrawerIfNeeded(page);
      const navItem = page.locator(`[data-nav-item="${route.navItem}"]`).first();
      await expectTapTargetAtLeast(navItem, 44, `${route.navItem} nav item`);
      await navItem.click();
      await expect(page).toHaveURL(new RegExp(`${route.path}$`));
      await expectStableSelectorVisible(page, `[data-route="${route.routeName}"]`);
      await expect(page.locator(`[data-nav-item="${route.navItem}"]`).first()).toHaveAttribute('data-nav-active', 'true');
      await expect(page.locator('[data-page-title]').first()).toContainText(route.title);
      await expectNoPageHorizontalOverflow(page);
    }

    await openDrawerIfNeeded(page);
    await close.click();
    await expect(page.locator('[data-shell="pi-responsive-shell"]')).toHaveAttribute('data-shell-drawer-open', 'false');
    await expect(trigger).toBeFocused();
  });

  test('samples nav item and page controls for focus-visible and tap sizing', async ({ page }) => {
    await page.goto('/console', { waitUntil: 'domcontentloaded' });

    await openDrawerIfNeeded(page);
    const consoleNavItem = page.locator('[data-nav-item="console"]').first();
    await expectTapTargetAtLeast(consoleNavItem, 44, 'console nav item');
    await expectFocusVisible(page, consoleNavItem, 'console nav item');

    const pageAction = page.locator('[data-action="send-chat"]').first();
    await expectTapTargetAtLeast(pageAction, 44, 'send chat action');
    await expectFocusVisible(page, pageAction, 'send chat action');
  });
});

async function openDrawerIfNeeded(page: Page): Promise<void> {
  const shell = page.locator('[data-shell="pi-responsive-shell"]').first();
  if ((await shell.getAttribute('data-shell-drawer-open')) !== 'true') {
    await page.locator('[data-shell-drawer-trigger]').first().click();
  }
}
