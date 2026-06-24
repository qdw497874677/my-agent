import { expect, test, type Locator, type Page } from '@playwright/test';
import {
  expectFocusVisible,
  expectNoPageHorizontalOverflow,
  expectTapTargetAtLeast,
} from './fixtures/mobile-smoke';
import { phase13RuntimeCardMatrixHint } from './fixtures/fake-runtime';

test.describe('Phase 13 mobile runtime card matrix', () => {
  test('mobile console renders representative runtime, tool, approval, and detail cards safely', async ({ page }) => {
    await page.goto('/console', { waitUntil: 'domcontentloaded' });
    await expect(page.locator('[data-role="chat-composer"]').first()).toBeVisible();
    await expectNoPageHorizontalOverflow(page);

    const input = page.locator('[data-role="chat-input"]').first();
    await input.fill(phase13RuntimeCardMatrixHint());
    const send = page.locator('[data-action="send-chat"]').first();
    await expectTapTargetAtLeast(send, 44, 'send chat action');
    await send.click();

    const feed = page.locator('[data-role="event-feed"]').first();
    await expect(feed).toBeVisible();
    await expect.poll(
      async () => feed.locator('[data-event-category]').count(),
      { message: 'Phase 13 prompt should render categorized runtime cards' },
    ).toBeGreaterThan(1);

    const toolCards = feed.locator('[data-event-category="tool"]');
    const approvalCards = feed.locator('[data-event-category="approval"]');
    await expect(toolCards.first().or(approvalCards.first()), 'tool or approval card should be visible').toBeVisible();
    await expectNonToolRuntimeCard(feed);

    await expandDetailsAndAssertRedaction(page, feed);
    await assertApprovalControls(page, feed);
    await expectNoPageHorizontalOverflow(page);
  });
});

async function expectNonToolRuntimeCard(feed: Locator): Promise<void> {
  await expect(
    feed.locator(
      '[data-event-category="model"], [data-event-category="status"], [data-event-category="policy"], [data-event-category="terminal"], [data-event-category="event"]',
    ).first(),
    'representative matrix should include at least one non-tool runtime card',
  ).toBeVisible();
}

async function expandDetailsAndAssertRedaction(page: Page, feed: Locator): Promise<void> {
  const expandable = feed.locator('[data-expandable="true"]').first();
  await expect(expandable).toBeVisible();
  const detailsControls = feed.locator('[data-expandable="true"] vaadin-details, [data-detail-layer="advanced"]');
  const count = await detailsControls.count();
  expect(count, 'runtime cards should expose Details controls or advanced detail layers').toBeGreaterThan(0);
  for (let index = 0; index < Math.min(count, 4); index += 1) {
    const control = detailsControls.nth(index);
    if (await control.isVisible().catch(() => false)) {
      await control.click().catch(() => undefined);
    }
  }
  await expect(feed.locator('[data-detail-layer="advanced"]').first()).toBeVisible();
  await expect(page.locator('body')).not.toContainText('sk-live-secret');
  await expect(page.locator('body')).not.toContainText('raw-token-value');
}

async function assertApprovalControls(page: Page, feed: Locator): Promise<void> {
  const approve = feed.locator('[data-action="approve-tool-call"], [data-risk-action="approve"]').first()
    .or(page.locator('[data-action="approve-tool-call"], [data-risk-action="approve"]').first());
  const reject = feed.locator('[data-action="reject-tool-call"], [data-risk-action="reject"]').first()
    .or(page.locator('[data-action="reject-tool-call"], [data-risk-action="reject"]').first());

  if (await approve.isVisible({ timeout: 2500 }).catch(() => false)) {
    await expectTapTargetAtLeast(approve, 44, 'approve tool call action');
    await expectFocusVisible(page, approve, 'approve tool call action');
    await approve.click();
  }
  else if (await reject.isVisible({ timeout: 2500 }).catch(() => false)) {
    await expectTapTargetAtLeast(reject, 44, 'reject tool call action');
    await expectFocusVisible(page, reject, 'reject tool call action');
    await reject.click();
  }
  await expect(approve.or(reject), 'approval approve/reject controls should be available in card or pending approval panel').toBeVisible();
  await expect(
    page.locator('[data-decision-state="succeeded"], [data-role="approval-decision-feedback"]:has-text("Decision recorded:")').first(),
    'approval click should record visible decision feedback',
  ).toBeVisible();
}
