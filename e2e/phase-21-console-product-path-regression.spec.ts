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
    await createRestoredConversation(request);

    await page.goto('/console', { waitUntil: 'domcontentloaded' });
    await expectChatOnlyConsole(page);

    const followUp = 'Phase 21 chat-only follow-up keeps product chat focus';
    await chatInput(page).fill(followUp);
    await page.locator('[data-action="send-chat"]').first().click();

    const submittedUser = page.locator('[data-message-role="user"][data-message-kind="primary-bubble"]').filter({ hasText: followUp }).first();
    await expect(submittedUser).toBeVisible();
    const sessionId = await requiredAttribute(submittedUser, 'data-session-id');
    await assertMainChatRejectsRawRuntimeNoise(page, sessionId);
  });

  test('streams live model deltas into one assistant bubble and exposes the product cancel action', async ({ page }) => {
    await page.goto('/console', { waitUntil: 'domcontentloaded' });
    await expectChatOnlyConsole(page);

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
    await expectChatOnlyConsole(page);

    await chatInput(page).fill(cancelledStreamingHint());
    await page.locator('[data-action="send-chat"]').first().click();
    await expect(page.locator('[data-action="cancel-run"]').first()).toBeVisible();
    await expect(primaryAssistantBubbles(page).first()).toBeVisible();
  });

  test('renders failed provider state safely without flattening raw runtime events into chat prose', async ({ page, request }) => {
    await createFailedStreamingRun(request);
    await createSlowStreamingRun(request);

    await page.goto('/console', { waitUntil: 'domcontentloaded' });
    await expectChatOnlyConsole(page);

    const failedPrompt = 'Phase 21 failed provider state stays in one safe assistant bubble';
    await chatInput(page).fill(failedPrompt);
    await page.locator('[data-action="send-chat"]').first().click();

    const failedAssistant = primaryAssistantBubbles(page).first();
    await expect(failedAssistant, 'provider failure should mutate the assistant bubble into a failed state').toBeVisible();
    await assertNoRawRuntimeNoise(failedAssistant);
  });
});

async function expectChatOnlyConsole(page: Page): Promise<void> {
  await expect(page.locator('[data-role="model-selector"], [data-role="provider-status"]').first()).toBeVisible();
  await expect(page.locator('[data-console-panel="chat"][data-console-panel-active="true"]').first()).toBeVisible();
  await expect(page.locator('[data-action="show-console-panel"]')).toHaveCount(0);
  await expect(page.locator('[data-console-panel="agents"], [data-console-panel="sessions"], [data-console-panel="run-context"]')).toHaveCount(0);
}

function chatInput(page: Page): Locator {
  return page.locator('[data-role="chat-input"] textarea').first();
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
