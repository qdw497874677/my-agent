import { expect, test } from '@playwright/test';
import { devHeaders } from './fixtures/fake-runtime';

const fakeSecret = 'PI_PHASE9_FAKE_SECRET_DO_NOT_LEAK';

test.describe('Phase 9 Operations Governance', () => {
  test('admin can inspect no-key operations summary without exposing secrets or APM controls', async ({ page, request }) => {
    const response = await request.get('/api/admin/governance/operations', { headers: devHeaders });
    expect(response.ok()).toBeTruthy();
    const responseText = JSON.stringify(await response.json());
    expect(responseText).toContain('runs');
    expect(responseText).toContain('models');
    expect(responseText).toContain('tools');
    expect(responseText).toContain('policies');
    expect(responseText).toContain('mcp');
    expect(responseText).toContain('plugins');
    expect(responseText).not.toContain(fakeSecret);

    await page.goto('/admin/governance/operations');
    await expect(page.locator('[data-route="admin-operations"]')).toBeVisible();
    await expect(page.locator('[data-admin-surface="operations-summary"]')).toBeVisible();
    await expect(page.locator('body')).toContainText('Operations metrics');
    await expect(page.locator('body')).toContainText('Runs');
    await expect(page.locator('body')).toContainText('Models');
    await expect(page.locator('body')).toContainText('Tools');
    await expect(page.locator('body')).toContainText('Policies');
    await expect(page.locator('body')).toContainText('MCP');
    await expect(page.locator('body')).toContainText('Plugins');

    const bodyText = await page.locator('body').innerText();
    expect(bodyText).not.toContain(fakeSecret);
    expect(bodyText.toLowerCase()).not.toMatch(/export|query builder|chart editor|prometheus|otlp collector|docker/);
  });
});
