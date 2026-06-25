import { expect, test, type Locator, type Page } from '@playwright/test';
import {
  expectFocusVisible,
  expectNoPageHorizontalOverflow,
  expectTapTargetAtLeast,
} from './fixtures/mobile-smoke';

test.describe('Phase 15 accessibility hardening', () => {
  test('representative shell navigation is keyboard reachable with active semantics', async ({ page }) => {
    await page.goto('/console', { waitUntil: 'domcontentloaded' });

    const drawerTrigger = page.locator('[data-shell-drawer-trigger]').first();
    await page.keyboard.press('Tab');
    await expectFocusVisible(page, drawerTrigger, 'shell drawer trigger');
    await expectTapTargetAtLeast(drawerTrigger, 44, 'shell drawer trigger');
    await drawerTrigger.click();

    const consoleNav = page.locator('[data-nav-item="console"]').first();
    await expectFocusableLabel(consoleNav, 'Console');
    await expectFocusVisible(page, consoleNav, 'Console nav item');
    await expect(consoleNav, 'active Console nav item should expose data-nav-active').toHaveAttribute('data-nav-active', 'true');
    const current = await consoleNav.getAttribute('aria-current');
    expect(current ?? 'page', 'aria-current may be present and should be page-like').toMatch(/^(page|true)$/);
    await expectNoPageHorizontalOverflow(page);
  });

  test('Console composer, panel switcher, run, and cancel controls retain labels and focus', async ({ page }) => {
    await page.goto('/console', { waitUntil: 'domcontentloaded' });

    const input = page.locator('[data-role="chat-input"]').first();
    await expect(input).toBeVisible();
    await expectFocusVisible(page, input, 'Console chat input');

    const panelSwitcher = page.locator('[data-action="show-console-panel"]');
    await expect(panelSwitcher.first(), 'Console panel switcher controls should be visible').toBeVisible();
    const panelCount = await panelSwitcher.count();
    expect(panelCount, 'Console should expose representative panel buttons').toBeGreaterThanOrEqual(3);
    for (let index = 0; index < Math.min(panelCount, 4); index += 1) {
      const panel = panelSwitcher.nth(index);
      await expectFocusableLabel(panel, `Console panel ${index + 1}`);
      await expect(panel, 'panel switcher should expose pressed state').toHaveAttribute('aria-pressed', /^(true|false)$/);
      await expectFocusVisible(page, panel, `Console panel ${index + 1}`);
    }

    const send = page.locator('[data-action="send-chat"]').first();
    await expectFocusableLabel(send, 'send chat');
    await expectTapTargetAtLeast(send, 44, 'send chat action');
    await expectFocusVisible(page, send, 'send chat action');

    const cancel = page.locator('[data-action="cancel-run-primary"], [data-action="cancel-run"]').first();
    if (await cancel.isVisible().catch(() => false)) {
      await expectFocusableLabel(cancel, 'cancel run');
      await expectFocusVisible(page, cancel, 'cancel run action');
    }
    await expectNoPageHorizontalOverflow(page);
  });

  test('runtime details and approval actions are not hover-only', async ({ page }) => {
    await page.goto('/console', { waitUntil: 'domcontentloaded' });
    await page.locator('[data-role="chat-input"]').first().fill('phase 13 runtime card matrix');
    await page.locator('[data-action="send-chat"]').first().click();

    const feed = page.locator('[data-role="event-feed"]').first();
    await expect.poll(
      async () => feed.locator('[data-event-category]').count(),
      { message: 'runtime cards should render for accessibility sampling' },
    ).toBeGreaterThan(0);

    const details = feed.locator('vaadin-details, [data-detail-layer="advanced"]').first();
    await expect(details, 'runtime card Details control/layer should be visible').toBeVisible();
    await expectFocusVisible(page, details, 'runtime Details control');
    await details.click().catch(() => undefined);

    const approvalAction = page.locator('[data-action="approve-tool-call"], [data-action="reject-tool-call"], [data-risk-action]').first();
    if (await approvalAction.isVisible({ timeout: 2500 }).catch(() => false)) {
      await expectFocusableLabel(approvalAction, 'approval action');
      await expectTapTargetAtLeast(approvalAction, 44, 'approval action');
      await expectFocusVisible(page, approvalAction, 'approval action');
    }
    await expectNoPageHorizontalOverflow(page);
  });

  test('Admin cards expose Details and primary controls to keyboard users', async ({ page }) => {
    await page.goto('/admin/governance/approvals', { waitUntil: 'domcontentloaded' });

    const adminSurface = page.locator('[data-route="admin-approval-queue"]').first();
    await expect(adminSurface).toBeVisible();
    const details = page.locator('vaadin-details[data-admin-details], [data-admin-details] vaadin-details, [data-expandable="true"] vaadin-details').first();
    if (await details.isVisible().catch(() => false)) {
      await expectFocusVisible(page, details, 'Admin Details control');
      await details.click().catch(() => undefined);
    }

    const primaryControl = page.locator('[data-primary-action], [data-read-only-refresh], [data-action], [data-risk-action], a, button').first();
    await expect(primaryControl, 'Admin primary/control action should be visible without hover').toBeVisible();
    await expectFocusableLabel(primaryControl, 'Admin primary/control action');
    await expectFocusVisible(page, primaryControl, 'Admin primary/control action');
    await expectNoPageHorizontalOverflow(page);
  });

  test('prefers-reduced-motion users avoid shell drawer animation contracts', async ({ page }) => {
    await page.emulateMedia({ reducedMotion: 'reduce' });
    await page.goto('/console', { waitUntil: 'domcontentloaded' });

    const drawer = page.locator('.pi-shell-drawer').first();
    await expect(drawer).toBeVisible();
    const transition = await drawer.evaluate((element) => window.getComputedStyle(element).transitionDuration);
    expect(transition, 'reduced-motion shell drawer transition duration should be disabled or near-zero').toMatch(/^(0s|0ms)(,\s*(0s|0ms))*$/);
    await expectNoPageHorizontalOverflow(page);
  });
});

async function expectFocusableLabel(locator: Locator, label: string): Promise<void> {
  const name = await locator.evaluate((element) => {
    const ariaLabel = element.getAttribute('aria-label');
    const title = element.getAttribute('title');
    const text = element.textContent?.trim();
    return ariaLabel || title || text || '';
  });
  expect(name.trim().length, `${label} should expose visible text, title, or aria-label`).toBeGreaterThan(0);
}
