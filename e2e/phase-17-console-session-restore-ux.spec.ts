import { expect, test, type Locator, type Page } from '@playwright/test';
import { createRestoredConversation } from './fixtures/fake-runtime';

test.describe('Phase 17 Console session restore UX', () => {
  test('no-key browser path restores recent session transcript and continues the same session', async ({ page, request }) => {
    const restored = await createRestoredConversation(request);

    await page.goto('/console', { waitUntil: 'domcontentloaded' });

    await openConsolePanel(page, 'sessions');
    const restoredCard = page.locator(`[data-role="session-card"][data-session-id="${restored.sessionId}"]`).first();
    await expect(restoredCard, 'recent history should include API-created fake-runtime session').toBeVisible();
    await expect(restoredCard.locator('[data-field="session-title"]')).not.toHaveText('');
    await expect(restoredCard.locator('[data-field="session-preview"]')).toContainText(/Phase 17|recent history|runtime/i);
    await expect(restoredCard.locator('[data-field="session-status"]')).toContainText(/completed|running|queued|ready|failed|cancel/i);

    await restoredCard.click();
    await expect(page.locator('[data-console-panel="chat"][data-console-panel-active="true"]').first()).toBeVisible();
    await expect(page.locator('[data-role="active-session-banner"][data-active-session-state="continued"]').first()).toContainText(/Continue:/);

    const userBubble = transcriptBubble(page, 'user', restored.sessionId).filter({ hasText: restored.prompt }).first();
    await expect(userBubble, 'restored transcript should show prior user bubble with stable identity selectors').toBeVisible();
    const assistantBubble = transcriptBubble(page, 'assistant', restored.sessionId).filter({ hasText: restored.assistantPattern }).first();
    await expect(assistantBubble, 'restored transcript should show prior assistant bubble with stable identity selectors').toBeVisible();
    await expect(userBubble).toHaveAttribute('data-run-id', restored.runId);
    await expect(assistantBubble).toHaveAttribute('data-run-id', restored.runId);

    const selectedBefore = await activeSessionId(page);
    expect(selectedBefore).toBe(restored.sessionId);

    const followUp = 'Phase 17 follow-up keeps restored session identity';
    await page.locator('[data-role="chat-input"]').first().fill(followUp);
    await page.locator('[data-action="send-chat"]').first().click();
    await expect(transcriptBubble(page, 'user', restored.sessionId).filter({ hasText: followUp }).first()).toBeVisible();
    await expect.poll(async () => activeSessionId(page), {
      message: 'follow-up should keep selected session rather than switching to a fresh session',
    }).toBe(restored.sessionId);

    const activeCard = page.locator(`[data-role="session-card"][data-session-active="true"][data-session-id="${restored.sessionId}"]`).first();
    await expect(activeCard).toBeVisible();
    await assertMainChatDoesNotFlattenRuntimeNoise(page, restored.sessionId);
  });
});

async function openConsolePanel(page: Page, target: 'agents' | 'sessions' | 'run-context' | 'chat'): Promise<void> {
  await page.locator(`[data-action="show-console-panel"][data-console-target="${target}"]`).first().click();
  await expect(page.locator(`[data-console-panel="${target}"][data-console-panel-active="true"]`).first()).toBeVisible();
}

function transcriptBubble(page: Page, role: 'user' | 'assistant' | 'tool' | 'error', sessionId: string): Locator {
  return page.locator(`[data-message-role="${role}"][data-session-id="${sessionId}"][data-run-id][data-message-status][data-stream-state]`);
}

async function activeSessionId(page: Page): Promise<string | null> {
  await openConsolePanel(page, 'sessions');
  const active = page.locator('[data-role="session-card"][data-session-active="true"]').first();
  await expect(active).toBeVisible();
  const id = await active.getAttribute('data-session-id');
  await openConsolePanel(page, 'chat');
  return id;
}

async function assertMainChatDoesNotFlattenRuntimeNoise(page: Page, sessionId: string): Promise<void> {
  const primaryBubbles = page.locator(`[data-session-id="${sessionId}"][data-message-kind="primary-bubble"]`);
  const count = await primaryBubbles.count();
  expect(count, 'restored chat should render user/assistant transcript bubbles').toBeGreaterThanOrEqual(2);
  for (let index = 0; index < count; index += 1) {
    await expect(primaryBubbles.nth(index)).not.toContainText(/run\.started|model\.delta|tool\.started|tool\.completed|runtime event/i);
  }

  const secondaryCards = page.locator(`[data-session-id="${sessionId}"][data-message-kind="secondary-card"]`);
  const secondaryCount = await secondaryCards.count();
  for (let index = 0; index < secondaryCount; index += 1) {
    await expect(secondaryCards.nth(index)).toHaveAttribute('data-transcript-card', /tool|error/);
    await expect(secondaryCards.nth(index).locator('[data-card-summary]').first()).toBeVisible();
  }
}
