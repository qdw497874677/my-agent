import { expect, test, type Page } from '@playwright/test';
import {
  expectNoPageHorizontalOverflow,
  expectPrimaryContentOrActionVisible,
  expectStableSelectorVisible,
  type MobileSmokeRoute,
} from './fixtures/mobile-smoke';

type MobileRouteSmoke = MobileSmokeRoute & {
  category: 'console' | 'admin-root' | 'admin-overview' | 'admin-registry' | 'admin-list';
};

const routes: MobileRouteSmoke[] = [
  {
    path: '/console',
    routeName: 'console',
    category: 'console',
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
    category: 'admin-root',
    primaryContent: [
      { name: 'admin governance surface', selector: '[data-surface="admin-governance"]' },
      { name: 'mobile critical root', selector: '[data-mobile-critical="true"]' },
    ],
  },
  {
    path: '/admin/governance/overview',
    routeName: 'admin-governance-overview',
    category: 'admin-overview',
    primaryContent: [
      { name: 'inspect-only admin surface', selector: '[data-admin-surface="inspect-only"]' },
      { name: 'overview empty state', selector: '[data-state="empty-governance-overview"]' },
    ],
  },
  {
    path: '/admin/governance/registry',
    routeName: 'admin-registry-status',
    category: 'admin-registry',
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
    category: 'admin-list',
    primaryContent: [
      { name: 'operations surface', selector: '[data-admin-surface="operations-summary"]' },
      { name: 'runs operations section', selector: '[data-operations-section="runs"]' },
      { name: 'warnings operations section', selector: '[data-operations-section="warnings"]' },
    ],
  },
  {
    path: '/admin/governance/policy-decisions',
    routeName: 'admin-policy-decisions',
    category: 'admin-list',
    primaryContent: [
      { name: 'policy decisions inspect surface', selector: '[data-admin-surface="inspect-only"]' },
      { name: 'policy decisions empty state', selector: '[data-state="empty-policy-decisions"]' },
    ],
  },
  {
    path: '/admin/governance/audits',
    routeName: 'admin-audit-summaries',
    category: 'admin-list',
    primaryContent: [
      { name: 'audit summaries inspect surface', selector: '[data-admin-surface="inspect-only"]' },
      { name: 'audit summaries empty state', selector: '[data-state="empty-audit-summaries"]' },
    ],
  },
  {
    path: '/admin/governance/approvals',
    routeName: 'admin-approval-queue',
    category: 'admin-list',
    primaryContent: [
      { name: 'approval queue governance surface', selector: '[data-admin-surface="separated-governance"]' },
      { name: 'approval queue empty state', selector: '[data-state="empty-admin-approvals"]' },
    ],
  },
];

test.describe('Phase 10 mobile route smoke baseline', () => {
  for (const route of routes) {
    test(`${route.path} opens without page-level horizontal overflow`, async ({ page }) => {
      await page.goto(route.path, { waitUntil: 'domcontentloaded' });

      await expectStableSelectorVisible(page, `[data-route="${route.routeName}"]`);
      await expectPrimaryContentOrActionVisible(page, route);
      await performLightInteraction(page, route);
      await expectNoPageHorizontalOverflow(page);
    });
  }
});

async function performLightInteraction(page: Page, route: MobileRouteSmoke): Promise<void> {
  switch (route.category) {
    case 'console': {
      const chatInput = page.locator('[data-role="chat-input"]').first();
      await chatInput.focus();
      await expect(page.locator('[data-action="send-chat"]').first()).toBeVisible();
      break;
    }
    case 'admin-root':
      await expect(page.locator('body')).toContainText('Pi Admin Governance');
      break;
    case 'admin-overview':
      await expect(page.locator('body')).toContainText('Admin Governance Overview');
      break;
    case 'admin-registry':
      await expect(page.locator('[data-mutation-controls="absent"]')).toBeVisible();
      await expect(page.locator('body')).toContainText('Registry status');
      break;
    case 'admin-list':
      await expect(page.locator('[data-state]').first()).toBeVisible();
      await expect(page.locator('body')).toContainText(/not been loaded|No pending approvals|none reported/i);
      break;
    default: {
      const exhaustive: never = route.category;
      throw new Error(`Unhandled route category: ${exhaustive}`);
    }
  }
}
