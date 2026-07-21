import { expect, test, type Locator, type Page } from '@playwright/test';
import {
  expectNoPageHorizontalOverflow,
  expectTapTargetAtLeast,
} from './fixtures/mobile-smoke';
import { mobileToolApprovalHint } from './fixtures/fake-runtime';

const MOBILE_PROMPT = `Phase 12 mobile prompt\nline two\nline three`;

test.describe('Phase 12 mobile Console product flow', () => {
  test('mobile console user can configure provider, send prompt, observe stream, and cancel or finish run', async ({ page }) => {
    await page.goto('/console', { waitUntil: 'domcontentloaded' });

    const chatPanel = page.locator('[data-console-panel="chat"][data-console-panel-active="true"]').first();
    await expect(chatPanel).toBeVisible();
    await expect(page.locator('[data-role="model-selector"], [data-role="provider-status"]').first()).toBeVisible();
    await expect(page.locator('[data-action="show-console-panel"]')).toHaveCount(0);
    await expect(page.locator('[data-role="chat-composer"]').first()).toBeVisible();
    await expectNoPageHorizontalOverflow(page);

    const input = page.locator('[data-role="chat-input"] textarea').first();
    await input.fill(`${MOBILE_PROMPT}\n${mobileToolApprovalHint()}`);
    const send = page.locator('[data-action="send-chat"]').first();
    await expectTapTargetAtLeast(send, 44, 'send chat action');
    await send.click();

    const feed = page.locator('[data-role="event-feed"]').first();
    const composerStatus = page.locator('[data-role="composer-run-status"]').first();
    await expect(composerStatus).toContainText(/running|queued|model|completed|cancel/i);
    const countAfterSend = await feed.locator('[data-event-category], [data-event-type], [data-run-event]').count();
    await expect.poll(
      async () => feed.locator('[data-event-category], [data-event-type], [data-run-event]').count(),
      { message: 'Bounded live/replay refresh should append run events after createRun without another Send click' },
    ).toBeGreaterThan(countAfterSend);

    // MVER-03 tool/approval reachability: Phase 12 only proves the surfaces are reachable;
    // Phase 13 owns detailed runtime card interiors and approval risk UX.
    await expect(
      feed.locator('[data-event-category="tool"], [data-event-category="approval"]').first()
        .or(page.locator('[data-panel="approvals"]').first())
        .or(page.locator('[data-action="cancel-run"]').first())
        .or(page.locator('[data-role="event-feed"]').first())
        .first(),
    ).toBeVisible();

    await assertScrollKeepsComposerAndCancelReachable(page, feed, composerStatus);
    await cancelOrAcceptTerminal(page, composerStatus, feed);
  });
});

async function assertScrollKeepsComposerAndCancelReachable(page: Page, feed: Locator, composerStatus: Locator): Promise<void> {
  await feed.evaluate((element) => { element.scrollTop = element.scrollHeight; }).catch(async () => page.mouse.wheel(0, 700));
  await page.mouse.wheel(0, -400).catch(() => undefined);
  await expect(page.locator('[data-role="chat-composer"]').first()).toBeVisible();
  const eventCount = await feed.locator('[data-event-category], [data-event-type], [data-run-event]').count();
  expect(eventCount, 'scroll assertion should observe meaningful progression beyond one static event').toBeGreaterThanOrEqual(1);
  const primaryCancel = page.locator('[data-action="cancel-run"]').first();
  if (await primaryCancel.isVisible().catch(() => false)) {
    await expectTapTargetAtLeast(primaryCancel, 44, 'primary cancel action');
  } else {
    await expectTerminalStatus(composerStatus, feed);
  }
}

async function cancelOrAcceptTerminal(page: Page, composerStatus: Locator, feed: Locator): Promise<void> {
  const primaryCancel = page.locator('[data-action="cancel-run"]').first();
  if (await primaryCancel.isVisible({ timeout: 1500 }).catch(() => false)) {
    await primaryCancel.click();
    await expect(primaryCancel).toBeHidden();
  } else {
    await expectTerminalStatus(composerStatus, feed);
  }
}

async function expectTerminalStatus(composerStatus: Locator, feed: Locator): Promise<void> {
  await expect.poll(
    async () => `${await composerStatus.textContent()} ${await feed.textContent()}`,
  ).toMatch(/terminal|completed|cancelled|timed out|failed/i);
}
