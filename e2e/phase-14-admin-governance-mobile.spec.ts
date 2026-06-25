import { expect, test, type Locator, type Page } from '@playwright/test';
import {
  expectFocusVisible,
  expectNoPageHorizontalOverflow,
  expectStableSelectorVisible,
  expectTapTargetAtLeast,
} from './fixtures/mobile-smoke';

type AdminMobileRouteCase = {
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

const adminRoutes: AdminMobileRouteCase[] = [
  {
    name: 'landing',
    path: '/admin/governance',
    routeName: 'admin-governance',
    selectors: [
      '[data-surface="admin-governance"]',
      '[data-mobile-critical="true"]',
    ],
  },
  {
    name: 'overview',
    path: '/admin/governance/overview',
    routeName: 'admin-governance-overview',
    selectors: [
      '[data-admin-overview-card]',
      '[data-admin-card]',
    ],
  },
  {
    name: 'registry',
    path: '/admin/governance/registry',
    routeName: 'admin-registry-status',
    selectors: [
      '[data-admin-registry-section]',
      '[data-mcp-server-card]',
      '[data-mcp-tool-card]',
      '[data-plugin-card]',
      '[data-extension-source-card]',
    ],
  },
  {
    name: 'operations',
    path: '/admin/governance/operations',
    routeName: 'admin-operations',
    selectors: [
      '[data-operations-card]',
      '[data-admin-card]',
    ],
  },
  {
    name: 'policy decisions',
    path: '/admin/governance/policy-decisions',
    routeName: 'admin-policy-decisions',
    selectors: [
      '[data-policy-decision-card]',
      '[data-admin-details="policy-context"]',
    ],
  },
  {
    name: 'audits',
    path: '/admin/governance/audits',
    routeName: 'admin-audit-summaries',
    selectors: [
      '[data-audit-card]',
      '[data-admin-details="audit-details"]',
    ],
  },
  {
    name: 'approvals',
    path: '/admin/governance/approvals',
    routeName: 'admin-approval-queue',
    selectors: [
      '[data-admin-surface="separated-governance"]',
      '[data-event-category="approval"], [data-state="empty-admin-approvals"], .pi-approval-card',
    ],
  },
];

test.describe('Phase 14 Admin Governance mobile coverage', () => {
  for (const route of adminRoutes) {
    test(`${route.name} route exposes mobile Admin cards/details safely`, async ({ page }) => {
      await page.goto(route.path, { waitUntil: 'domcontentloaded' });

      await expectStableSelectorVisible(page, `[data-route="${route.routeName}"]`);
      for (const selector of route.selectors) {
        await expectStableSelectorVisible(page, selector);
      }
      await expectNoPageHorizontalOverflow(page);

      await expandFirstVisibleAdminDetails(page);
      await expectNoPageHorizontalOverflow(page);
      await expectSensitiveMarkersRedacted(page);

      const control = await firstVisibleAdminTouchControl(page);
      await expectTapTargetAtLeast(control, 44, `${route.name} Admin details/control/action`);
      await expectFocusVisible(page, control, `${route.name} Admin details/control/action`);
    });
  }
});

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
    await expect(body, `${marker} should stay redacted from Admin mobile details`).not.toContainText(marker);
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
  throw new Error('Expected at least one visible Admin Details/control/action for mobile tap and focus sampling.');
}
