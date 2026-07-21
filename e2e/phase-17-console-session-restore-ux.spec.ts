import { expect, test, type Locator, type Page } from '@playwright/test';
import { createRestoredConversation } from './fixtures/fake-runtime';

test.describe('Phase 17 Console session restore UX', () => {
  test('no-key browser path restores recent session transcript and continues the same session', async ({ page, request }) => {
    const restored = await createRestoredConversation(request);

    await page.goto('/console', { waitUntil: 'domcontentloaded' });
    await expectChatOnlyConsole(page);
    await expect(page.locator('[data-console-panel="chat"][data-console-panel-active="true"]').first()).toBeVisible();
    await expect(page.locator('[data-role="active-session-banner"][data-active-session-state="continued"]').first()).toContainText(/Continue:|继续：/);

    const userBubble = transcriptBubble(page, 'user', restored.sessionId).filter({ hasText: restored.prompt }).first();
    await expect(userBubble, 'restored transcript should show prior user bubble with stable identity selectors').toBeVisible();
    const assistantBubble = transcriptBubble(page, 'assistant', restored.sessionId).filter({ hasText: restored.assistantPattern }).first();
    await expect(assistantBubble, 'restored transcript should show prior assistant bubble with stable identity selectors').toBeVisible();
    await expect(userBubble).toHaveAttribute('data-run-id', restored.runId);
    await expect(assistantBubble).toHaveAttribute('data-run-id', restored.runId);

    const selectedBefore = await activeSessionId(page);
    expect(selectedBefore).toBe(restored.sessionId);

    const followUp = 'Phase 17 follow-up keeps restored session identity';
    await chatInput(page).fill(followUp);
    await page.locator('[data-action="send-chat"]').first().click();
    await expect(transcriptBubble(page, 'user', restored.sessionId).filter({ hasText: followUp }).first()).toBeVisible();
    await expect.poll(async () => activeSessionId(page), {
      message: 'follow-up should keep selected session rather than switching to a fresh session',
    }).toBe(restored.sessionId);

    await assertMainChatDoesNotFlattenRuntimeNoise(page, restored.sessionId);
  });
});

function transcriptBubble(page: Page, role: 'user' | 'assistant' | 'tool' | 'error', sessionId: string): Locator {
  return page.locator(`[data-message-role="${role}"][data-session-id="${sessionId}"][data-run-id][data-message-status][data-stream-state]`);
}

function chatInput(page: Page): Locator {
  return page.locator('[data-role="chat-input"] textarea').first();
}

async function activeSessionId(page: Page): Promise<string | null> {
  const banner = page.locator('[data-role="active-session-banner"][data-session-id]').first();
  await expect(banner).toBeVisible();
  return banner.getAttribute('data-session-id');
}

async function expectChatOnlyConsole(page: Page): Promise<void> {
  await expect(page.locator('[data-layout="chat-home"]').first()).toBeVisible();
  await expect(page.locator('[data-role="model-selector"], [data-role="provider-status"]').first()).toBeVisible();
  await expect(page.locator('[data-console-panel="chat"][data-console-panel-active="true"]').first()).toBeVisible();
  await expect(page.locator('[data-action="show-console-panel"]')).toHaveCount(0);
  await expect(page.locator('[data-console-panel="agents"], [data-console-panel="sessions"], [data-console-panel="run-context"]')).toHaveCount(0);
}

async function assertMainChatDoesNotFlattenRuntimeNoise(page: Page, sessionId: string): Promise<void> {
  const primaryBubbles = page.locator(`[data-session-id="${sessionId}"][data-message-kind="primary-bubble"]`);
  const count = await primaryBubbles.count();
  expect(count, 'restored chat should render user/assistant transcript bubbles').toBeGreaterThanOrEqual(2);
  for (let index = 0; index < count; index += 1) {
    await expect(primaryBubbles.nth(index)).not.toContainText(/run\.started|model\.delta|tool\.started|tool\.completed/i);
  }

  const secondaryCards = page.locator(`[data-session-id="${sessionId}"][data-message-kind="secondary-card"]`);
  const secondaryCount = await secondaryCards.count();
  for (let index = 0; index < secondaryCount; index += 1) {
    await expect(secondaryCards.nth(index)).toHaveAttribute('data-transcript-card', /tool|error/);
    await expect(secondaryCards.nth(index).locator('[data-card-summary]').first()).toBeVisible();
  }
}
