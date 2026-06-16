import { expect, test } from '@playwright/test';
import { devHeaders } from './fixtures/fake-runtime';

test.describe('Phase 7 MCP Governance', () => {
  test('admin can inspect MCP status, discovered tools, unhealthy redacted errors, and refresh without CRUD controls', async ({ page, request }) => {
    const refresh = await request.post('/api/admin/governance/mcp/refresh', { headers: devHeaders });
    expect(refresh.ok()).toBeTruthy();
    const refreshJson = await refresh.json();
    expect(refreshJson).toMatchObject({ refreshed: true });
    expect(JSON.stringify(refreshJson).toLowerCase()).not.toContain('secret');

    const status = await request.get('/api/admin/governance/mcp', { headers: devHeaders });
    expect(status.ok()).toBeTruthy();
    const statusJson = await status.json();
    expect(statusJson.servers.length).toBeGreaterThanOrEqual(1);
    expect(JSON.stringify(statusJson)).toContain('mcp.');
    expect(JSON.stringify(statusJson).toLowerCase()).not.toContain('api_key');

    await page.goto('/api/admin/governance/mcp');
    await expect(page.locator('body')).toContainText(/serverId|connectionStatus|discoveryStatus|servers/i);
    await expect(page.locator('body')).toContainText(/mcp\./i);

    const bodyText = await page.locator('body').innerText();
    expect(bodyText).toMatch(/AVAILABLE|DISCOVERED|CONNECTED|UNCONFIGURED|UNHEALTHY|FAILED/i);
    expect(bodyText.toLowerCase()).not.toMatch(/add mcp|edit mcp|delete mcp|disable mcp|server form|api key|password/);
  });
});
