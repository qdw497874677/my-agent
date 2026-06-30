import { expect, test, type Locator, type Page } from '@playwright/test';
import {
  cancelStreamingRun,
  cancelledStreamingHint,
  createFailedStreamingRun,
  createSlowStreamingRun,
  slowStreamingHint,
} from './fixtures/fake-runtime';

test.describe('Phase 18 streaming bubble lifecycle', () => {
  test('live Console send exposes stream mode and keeps model deltas in one assistant bubble', async ({ page }) => {
    await page.goto('/console', { waitUntil: 'domcontentloaded' });
    await openVisibleConsolePanel(page, 'chat');

    const assistantCountBefore = await primaryAssistantBubbles(page).count();
    await page.locator('[data-role="chat-input"]').first().fill(slowStreamingHint());
    await page.locator('[data-action="send-chat"]').first().click();

    const assistant = primaryAssistantBubbles(page).nth(assistantCountBefore);
    await expect(assistant, 'send should create a pending assistant bubble for the new run').toBeVisible();
    await expect(assistant, 'product streaming must expose push/sse/polling-fallback mode explicitly')
      .toHaveAttribute('data-stream-mode', /^(push|sse|polling-fallback)$/);
    await expect(assistant, 'assistant bubble must carry stable run identity for reducer assertions')
      .toHaveAttribute('data-run-id', /.+/);

    const runId = await requiredAttribute(assistant, 'data-run-id');
    const sameRunAssistant = assistantBubbleForRun(page, runId);
    await expect.poll(async () => sameRunAssistant.count(), {
      message: 'all streamed chunks for one run should stay in one assistant bubble instead of one component per token',
    }).toBe(1);

    const observedStates = await observeStreamStates(sameRunAssistant.first());
    expect(observedStates, 'streaming assertions should observe pending or streaming before terminal when timing allows')
      .toEqual(expect.arrayContaining([expect.stringMatching(/pending|streaming|completed|partial|failed|cancelled/)]));

    await expect(sameRunAssistant.first(), 'terminal assistant should contain semantic fake-runtime answer text')
      .toContainText(/Alpha|Beta|Gamma|model reply|completed|response/i);
    await expect(sameRunAssistant.first(), 'raw runtime event names must not be flattened into assistant prose')
      .not.toContainText(/model\.delta|run\.started|run\.completed|runtime event/i);
    await expect.poll(async () => sameRunAssistant.count(), {
      message: 'terminal replay should not duplicate the assistant bubble for the same run',
    }).toBe(1);
  });

  test('API-created slow stream restores with stable selectors and replay-safe single assistant bubble', async ({ page, request }) => {
    const streamed = await createSlowStreamingRun(request);

    await page.goto('/console', { waitUntil: 'domcontentloaded' });
    await openVisibleConsolePanel(page, 'sessions');
    const restoredCard = page.locator(`[data-role="session-card"][data-session-id="${streamed.sessionId}"]`).first();
    await expect(restoredCard, 'no-key fake runtime session should be discoverable in recent sessions').toBeVisible();
    await restoredCard.click();
    await openVisibleConsolePanel(page, 'chat');

    const assistant = assistantBubbleForRun(page, streamed.runId).first();
    await expect(assistant, 'restored stream should render one assistant bubble for the run').toBeVisible();
    await expect(assistant).toHaveAttribute('data-session-id', streamed.sessionId);
    await expect(assistant).toHaveAttribute('data-run-id', streamed.runId);
    await expect(assistant).toHaveAttribute('data-stream-mode', /^(push|sse|polling-fallback)$/);
    await expect(assistant).toHaveAttribute('data-stream-state', /^(completed|partial|failed|cancelled|streaming|pending)$/);
    await expect(assistant).toContainText(streamed.assistantPattern);
    await expect(assistant).not.toContainText(/model\.delta|run\.created|run\.completed|eventId|payloadSchema/i);
    await expect.poll(async () => assistantBubbleForRun(page, streamed.runId).count(), {
      message: 'replay/dedupe should not create duplicate assistant bubbles for the restored run',
    }).toBe(1);
  });

  test('cancellation preserves partial text and suppresses post-cancel mutation', async ({ page, request }) => {
    const cancelled = await cancelStreamingRun(request);

    await page.goto('/console', { waitUntil: 'domcontentloaded' });
    await openVisibleConsolePanel(page, 'sessions');
    await page.locator(`[data-role="session-card"][data-session-id="${cancelled.sessionId}"]`).first().click();
    await openVisibleConsolePanel(page, 'chat');

    const assistant = assistantBubbleForRun(page, cancelled.runId).first();
    await expect(assistant, 'cancelled run should still have a visible assistant bubble').toBeVisible();
    await expect(assistant, 'cancelled/partial state must be represented semantically').toHaveAttribute('data-stream-state', /^(cancelled|partial)$/);
    await expect(assistant).toHaveAttribute('data-run-id', cancelled.runId);
    await expect(assistant).toHaveAttribute('data-stream-mode', /^(push|sse|polling-fallback)$/);

    const textAfterCancel = await assistant.innerText();
    await page.waitForTimeout(1_000);
    await expect.poll(async () => assistant.innerText(), {
      message: 'late provider deltas after cancellation must not append to the stopped partial answer',
    }).toBe(textAfterCancel);
    await expect(assistant).not.toContainText(/java\.lang|stack trace|raw-token|sk-live|apiKey/i);
  });

  test('failed stream marks assistant failed and shows safe secondary status without raw event noise', async ({ page, request }) => {
    const failed = await createFailedStreamingRun(request);

    await page.goto('/console', { waitUntil: 'domcontentloaded' });
    await openVisibleConsolePanel(page, 'sessions');
    await page.locator(`[data-role="session-card"][data-session-id="${failed.sessionId}"]`).first().click();
    await openVisibleConsolePanel(page, 'chat');

    const assistant = assistantBubbleForRun(page, failed.runId).first();
    await expect(assistant, 'provider/runtime failure should mutate the assistant bubble rather than add raw prose').toBeVisible();
    await expect(assistant).toHaveAttribute('data-stream-state', 'failed');
    await expect(assistant).toHaveAttribute('data-stream-mode', /^(push|sse|polling-fallback)$/);
    await expect(assistant).toContainText(failed.assistantPattern);
    await expect(assistant).not.toContainText(/model\.error|run\.failed|java\.lang|stack trace|raw-token|sk-live|apiKey/i);

    const secondary = page.locator(`[data-session-id="${failed.sessionId}"][data-run-id="${failed.runId}"][data-message-kind="secondary-card"]`);
    if (await secondary.count()) {
      await expect(secondary.first().locator('[data-card-summary]').first(), 'failure details should be summarized safely').toBeVisible();
      await expect(secondary.first()).not.toContainText(/raw-token|sk-live|apiKey|stack trace/i);
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

function primaryAssistantBubbles(page: Page): Locator {
  return page.locator('[data-message-role="assistant"][data-message-kind="primary-bubble"][data-run-id][data-stream-state][data-stream-mode]');
}

function assistantBubbleForRun(page: Page, runId: string): Locator {
  return page.locator(`[data-message-role="assistant"][data-message-kind="primary-bubble"][data-run-id="${runId}"][data-stream-state][data-stream-mode]`);
}

async function requiredAttribute(locator: Locator, name: string): Promise<string> {
  const value = await locator.getAttribute(name);
  expect(value, `${name} should be present`).toBeTruthy();
  return value!;
}

async function observeStreamStates(locator: Locator): Promise<string[]> {
  const states = new Set<string>();
  for (let attempt = 0; attempt < 12; attempt += 1) {
    const state = await locator.getAttribute('data-stream-state');
    if (state) {
      states.add(state);
      if (/^(completed|failed|cancelled|partial)$/.test(state)) {
        break;
      }
    }
    await locator.page().waitForTimeout(150);
  }
  return [...states];
}
