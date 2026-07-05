import { expect, test, type Locator, type Page } from '@playwright/test';
import {
  cancelStreamingRun,
  cancelledStreamingHint,
  createFailedStreamingRun,
  createRestoredConversation,
  createSlowStreamingRun,
  slowStreamingHint,
} from './fixtures/fake-runtime';

const RAW_RUNTIME_EVENT_NOISE = ['run.started', 'model.delta', 'model.completed', 'tool.call', 'RunEvent'];
const RAW_RUNTIME_EVENT_NOISE_PATTERN = /run\.started|model\.delta|model\.completed|tool\.call|RunEvent/i;

test.describe('Phase 21 Console product path regression', () => {
  test('restores a recent session card and continues the same active conversation without raw runtime noise', async ({ page, request }) => {
    const restored = await createRestoredConversation(request);

    await page.goto('/console', { waitUntil: 'domcontentloaded' });

    await openVisibleConsolePanel(page, 'sessions');
    const restoredCard = page.locator(`[data-role="session-card"][data-session-id="${restored.sessionId}"]`).first();
    await expect(restoredCard, 'recent sessions should expose the fake-runtime restored conversation card').toBeVisible();
    await expect(restoredCard.locator('[data-field="session-title"]')).not.toHaveText('');
    await expect(restoredCard.locator('[data-field="session-preview"]')).toContainText(/Phase 17|recent history|runtime/i);

    await restoredCard.click();
    await openVisibleConsolePanel(page, 'chat');

    const activeBanner = page.locator('[data-role="active-session-banner"][data-active-session-state="continued"]').first();
    await expect(activeBanner, 'restored conversations must render a continued active-session banner').toBeVisible();
    await expect(activeBanner).toContainText(/Continue:/);

    await expect(primaryBubble(page, 'user', restored.sessionId, restored.runId).filter({ hasText: restored.prompt }).first()).toBeVisible();
    await expect(primaryBubble(page, 'assistant', restored.sessionId, restored.runId).filter({ hasText: restored.assistantPattern }).first()).toBeVisible();

    const selectedBeforeFollowUp = await activeSessionId(page);
    expect(selectedBeforeFollowUp).toBe(restored.sessionId);

    const followUp = 'Phase 21 follow-up keeps restored session identity and product chat focus';
    await chatInput(page).fill(followUp);
    await page.locator('[data-action="send-chat"]').first().click();

    await expect(primaryBubble(page, 'user', restored.sessionId).filter({ hasText: followUp }).first()).toBeVisible();
    await expect.poll(async () => activeSessionId(page), {
      message: 'same-session follow-up should not switch to a fresh conversation',
    }).toBe(restored.sessionId);
    await assertMainChatRejectsRawRuntimeNoise(page, restored.sessionId);
  });

  test('streams live model deltas into one assistant bubble and exposes the product cancel action', async ({ page }) => {
    await page.goto('/console', { waitUntil: 'domcontentloaded' });
    await openVisibleConsolePanel(page, 'chat');

    const assistantCountBefore = await primaryAssistantBubbles(page).count();
    await chatInput(page).fill(slowStreamingHint());
    await page.locator('[data-action="send-chat"]').first().click();

    const assistant = primaryAssistantBubbles(page).nth(assistantCountBefore);
    await expect(assistant, 'a live send should create exactly one reducer-owned assistant bubble for the run').toBeVisible();
    await expect(assistant).toHaveAttribute('data-message-role', 'assistant');
    await expect(assistant).toHaveAttribute('data-message-kind', 'primary-bubble');
    await expect(assistant).toHaveAttribute('data-run-id', /.+/);
    await expect(assistant).toHaveAttribute('data-stream-mode', /^(push|sse|polling-fallback)$/);

    const runId = await requiredAttribute(assistant, 'data-run-id');
    await expect.poll(async () => assistantBubbleForRun(page, runId).count(), {
      message: 'all deltas for one slow stream should coalesce into a single assistant bubble',
    }).toBe(1);

    await expect(page.locator('[data-action="cancel-run"]').first(), 'Console must expose a stable cancel-run action while a run is active or recently active').toBeVisible();
    await expect(assistantBubbleForRun(page, runId).first()).not.toContainText(RAW_RUNTIME_EVENT_NOISE_PATTERN);
  });

  test('preserves partial assistant output after cancellation and suppresses runtime event names', async ({ page, request }) => {
    const cancelled = await cancelStreamingRun(request);

    await page.goto('/console', { waitUntil: 'domcontentloaded' });
    await openVisibleConsolePanel(page, 'sessions');
    await page.locator(`[data-role="session-card"][data-session-id="${cancelled.sessionId}"]`).first().click();
    await openVisibleConsolePanel(page, 'chat');

    const assistant = assistantBubbleForRun(page, cancelled.runId).first();
    await expect(assistant, 'cancelled fake-runtime stream should still render the partial assistant bubble').toBeVisible();
    await expect(assistant).toHaveAttribute('data-stream-state', /^(cancelled|partial)$/);
    await expect(assistant).toContainText(cancelled.assistantPattern);

    const textAfterCancel = await assistant.innerText();
    await page.waitForTimeout(1_000);
    await expect.poll(async () => assistant.innerText(), {
      message: 'late provider deltas must not mutate the preserved partial output after cancellation',
    }).toBe(textAfterCancel);
    await assertNoRawRuntimeNoise(assistant);

    await chatInput(page).fill(cancelledStreamingHint());
    await expect(page.locator('[data-action="cancel-run"]').first()).toBeVisible();
  });

  test('renders failed provider state safely without flattening raw runtime events into chat prose', async ({ page, request }) => {
    const failed = await createFailedStreamingRun(request);
    const slow = await createSlowStreamingRun(request);

    await page.goto('/console', { waitUntil: 'domcontentloaded' });

    await openVisibleConsolePanel(page, 'sessions');
    await page.locator(`[data-role="session-card"][data-session-id="${slow.sessionId}"]`).first().click();
    await openVisibleConsolePanel(page, 'chat');
    await expect.poll(async () => assistantBubbleForRun(page, slow.runId).count(), {
      message: 'restored slow stream should replay into exactly one assistant bubble',
    }).toBe(1);
    await assertNoRawRuntimeNoise(assistantBubbleForRun(page, slow.runId).first());

    await openVisibleConsolePanel(page, 'sessions');
    await page.locator(`[data-role="session-card"][data-session-id="${failed.sessionId}"]`).first().click();
    await openVisibleConsolePanel(page, 'chat');

    const failedAssistant = assistantBubbleForRun(page, failed.runId).first();
    await expect(failedAssistant, 'provider failure should mutate the assistant bubble into a failed state').toBeVisible();
    await expect(failedAssistant).toHaveAttribute('data-stream-state', 'failed');
    await expect(failedAssistant).toContainText(failed.assistantPattern);
    await assertNoRawRuntimeNoise(failedAssistant);

    const secondaryCards = page.locator(`[data-session-id="${failed.sessionId}"][data-run-id="${failed.runId}"][data-message-kind="secondary-card"]`);
    if (await secondaryCards.count()) {
      await expect(secondaryCards.first().locator('[data-card-summary]').first()).toBeVisible();
      await assertNoRawRuntimeNoise(secondaryCards.first());
    }
  });
});

async function openVisibleConsolePanel(page: Page, target: 'agents' | 'sessions' | 'run-context' | 'chat'): Promise<void> {
  const control = page.locator(`[data-action="show-console-panel"][data-console-target="${target}"]`).first();
  await expect(control, `${target} control must be visible before product-path navigation`).toBeVisible();
  await expect(control, `${target} control must be enabled before product-path navigation`).toBeEnabled();
  await control.click();
  await expect(page.locator(`[data-console-panel="${target}"][data-console-panel-active="true"]`).first()).toBeVisible();
}

function chatInput(page: Page): Locator {
  return page.locator('[data-role="chat-input"]').first();
}

function primaryAssistantBubbles(page: Page): Locator {
  return page.locator('[data-message-role="assistant"][data-message-kind="primary-bubble"][data-run-id][data-stream-state][data-stream-mode]');
}

function assistantBubbleForRun(page: Page, runId: string): Locator {
  return page.locator(`[data-message-role="assistant"][data-message-kind="primary-bubble"][data-run-id="${runId}"][data-stream-state][data-stream-mode]`);
}

function primaryBubble(page: Page, role: 'user' | 'assistant', sessionId: string, runId?: string): Locator {
  const runSelector = runId ? `[data-run-id="${runId}"]` : '[data-run-id]';
  return page.locator(`[data-message-role="${role}"][data-message-kind="primary-bubble"][data-session-id="${sessionId}"]${runSelector}[data-message-status][data-stream-state]`);
}

async function activeSessionId(page: Page): Promise<string | null> {
  await openVisibleConsolePanel(page, 'sessions');
  const active = page.locator('[data-role="session-card"][data-session-active="true"]').first();
  await expect(active).toBeVisible();
  const id = await active.getAttribute('data-session-id');
  await openVisibleConsolePanel(page, 'chat');
  return id;
}

async function assertMainChatRejectsRawRuntimeNoise(page: Page, sessionId: string): Promise<void> {
  const primaryBubbles = page.locator(`[data-session-id="${sessionId}"][data-message-kind="primary-bubble"]`);
  const count = await primaryBubbles.count();
  expect(count, 'main chat should render product user/assistant bubbles for the selected conversation').toBeGreaterThanOrEqual(2);
  for (let index = 0; index < count; index += 1) {
    await assertNoRawRuntimeNoise(primaryBubbles.nth(index));
  }
}

async function assertNoRawRuntimeNoise(locator: Locator): Promise<void> {
  for (const rawNoise of RAW_RUNTIME_EVENT_NOISE) {
    await expect(locator, `main chat should not expose raw runtime-event noise: ${rawNoise}`).not.toContainText(rawNoise);
  }
}

async function requiredAttribute(locator: Locator, name: string): Promise<string> {
  const value = await locator.getAttribute(name);
  expect(value, `${name} should be present`).toBeTruthy();
  return value!;
}
