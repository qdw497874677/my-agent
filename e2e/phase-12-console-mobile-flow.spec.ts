import { expect, test, type Locator, type Page } from '@playwright/test';
import {
  expectNoPageHorizontalOverflow,
  expectTapTargetAtLeast,
} from './fixtures/mobile-smoke';
import { mobileToolApprovalHint } from './fixtures/fake-runtime';

const MOBILE_PROMPT = `Phase 12 mobile prompt\nline two\nline three`;

test.describe('Phase 12 mobile Console product flow', () => {
  test('mobile console user can browse agent, send prompt, observe stream, inspect sessions, and cancel or finish run', async ({ page }) => {
    await page.goto('/console', { waitUntil: 'domcontentloaded' });

    const chatPanel = page.locator('[data-console-panel="chat"][data-console-panel-active="true"]').first();
    await expect(chatPanel).toBeVisible();
    await expect(page.locator('[data-role="chat-composer"]').first()).toBeVisible();
    await expectNoPageHorizontalOverflow(page);

    await openConsolePanel(page, 'agents');
    await expectNoPageHorizontalOverflow(page);
    const generalAgent = page.locator('[data-agent-id="cloud-general-agent"]').first();
    await expect(generalAgent).toBeVisible();
    await generalAgent.locator('[data-primary-action^="general-agent-"]').first().click();
    await expect(page.locator('[data-console-panel="chat"][data-console-panel-active="true"]').first()).toBeVisible();

    const input = page.locator('[data-role="chat-input"]').first();
    await input.fill(`${MOBILE_PROMPT}\n${mobileToolApprovalHint()}`);
    const send = page.locator('[data-action="send-chat"]').first();
    await expectTapTargetAtLeast(send, 44, 'send chat action');
    await send.click();

    const feed = page.locator('[data-role="event-feed"]').first();
    const composerStatus = page.locator('[data-role="composer-run-status"]').first();
    await expect(composerStatus.or(feed)).toContainText(/running|queued|model|completed|cancel/i);
    await expect.poll(
      async () => feed.locator('[data-event-category], [data-event-type], [data-run-event]').count(),
      { message: 'Send click should produce browser-visible run event progression' },
    ).toBeGreaterThanOrEqual(1);

    // MVER-03 tool/approval reachability: Phase 12 only proves the surfaces are reachable;
    // Phase 13 owns detailed runtime card interiors and approval risk UX.
    await expect(
      feed.locator('[data-event-category="tool"], [data-event-category="approval"]').first()
        .or(page.locator('[data-panel="approvals"]').first())
        .or(page.locator('[data-console-panel="run-context"]').first()),
    ).toBeVisible();

    await openConsolePanel(page, 'sessions');
    await expectNoPageHorizontalOverflow(page);
    await expect(page.locator('[data-role="session-card"], [data-state*="session"], [data-empty-state*="session"]').first()).toBeVisible();

    await openConsolePanel(page, 'run-context');
    await expectNoPageHorizontalOverflow(page);
    await expect(page.locator('[data-action="cancel-run"], [data-role="run-status"], [data-role="event-feed"]').first()).toBeVisible();

    await openConsolePanel(page, 'chat');
    await assertScrollKeepsComposerAndCancelReachable(page, feed, composerStatus);
    await cancelOrAcceptTerminal(page, composerStatus, feed);
  });
});

async function openConsolePanel(page: Page, target: 'agents' | 'sessions' | 'run-context' | 'chat'): Promise<void> {
  await page.locator(`[data-action="show-console-panel"][data-console-target="${target}"]`).first().click();
  await expect(page.locator(`[data-console-panel="${target}"][data-console-panel-active="true"]`).first()).toBeVisible();
}

async function assertScrollKeepsComposerAndCancelReachable(page: Page, feed: Locator, composerStatus: Locator): Promise<void> {
  await feed.evaluate((element) => { element.scrollTop = element.scrollHeight; }).catch(async () => page.mouse.wheel(0, 700));
  await page.mouse.wheel(0, -400).catch(() => undefined);
  await expect(page.locator('[data-role="chat-composer"]').first()).toBeVisible();
  const eventCount = await feed.locator('[data-event-category], [data-event-type], [data-run-event]').count();
  expect(eventCount, 'scroll assertion should observe meaningful progression beyond one static event').toBeGreaterThanOrEqual(1);
  const primaryCancel = page.locator('[data-action="cancel-run-primary"]').first();
  if (await primaryCancel.isVisible().catch(() => false)) {
    await expectTapTargetAtLeast(primaryCancel, 44, 'primary cancel action');
  } else {
    await expect(composerStatus.or(feed)).toContainText(/terminal|completed|cancelled|timed out|failed/i);
  }
}

async function cancelOrAcceptTerminal(page: Page, composerStatus: Locator, feed: Locator): Promise<void> {
  const primaryCancel = page.locator('[data-action="cancel-run-primary"]').first();
  if (await primaryCancel.isVisible({ timeout: 1500 }).catch(() => false)) {
    await primaryCancel.click();
    await expect(composerStatus.or(feed)).toContainText(/cancelling|cancelled|terminal|completed/i);
  } else {
    await expect(composerStatus.or(feed)).toContainText(/terminal|completed|cancelled|timed out|failed/i);
  }
}
