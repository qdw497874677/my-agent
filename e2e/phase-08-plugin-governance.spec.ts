import { expect, test } from '@playwright/test';
import { devHeaders } from './fixtures/fake-runtime';

test.describe('Phase 8 Plugin Governance', () => {
  test('admin can inspect plugin status and narrow lifecycle action plans without deferred controls or leaked secrets', async ({ page, request }) => {
    const status = await request.get('/api/admin/governance/plugins', { headers: devHeaders });
    expect(status.ok()).toBeTruthy();
    const statusJson = await status.json();
    const statusText = JSON.stringify(statusJson);

    expect(statusJson.plugins.length).toBeGreaterThanOrEqual(4);
    expect(statusText).toContain('healthy-plugin');
    expect(statusText).toContain('disabled-plugin');
    expect(statusText).toContain('quarantined-plugin');
    expect(statusText).toContain('failed-plugin');
    expect(statusText).toContain('PF4J_JAR');
    expect(statusText).toContain('COMPATIBLE');
    expect(statusText).toContain('INCOMPATIBLE');
    expect(statusText).toContain('plugin.healthy.read');
    expect(statusText).toContain('[REDACTED] load failure');
    expect(statusText).not.toContain('PI_PHASE8_FAKE_SECRET_DO_NOT_LEAK');
    expect(statusText).not.toContain('/var/secret/plugins');

    const refresh = await request.post('/api/admin/governance/plugins/refresh', { headers: devHeaders });
    expect(refresh.ok()).toBeTruthy();
    await expect.poll(async () => (await refresh.json()).operation).toBe('refresh');

    const disable = await request.post('/api/admin/governance/plugins/healthy-plugin/disable', {
      headers: devHeaders,
      data: { operation: 'disable', reason: 'confirmed disable from browser smoke' },
    });
    expect(disable.ok()).toBeTruthy();
    await expect.poll(async () => (await disable.json()).resultingLifecycleStatus).toBe('DISABLED');

    const quarantine = await request.post('/api/admin/governance/plugins/healthy-plugin/quarantine', {
      headers: devHeaders,
      data: { operation: 'quarantine', reason: 'confirmed quarantine from browser smoke' },
    });
    expect(quarantine.ok()).toBeTruthy();
    await expect.poll(async () => (await quarantine.json()).resultingLifecycleStatus).toBe('QUARANTINED');

    await page.goto('/api/admin/governance/plugins');
    await expect(page.locator('body')).toContainText(/healthy-plugin|disabled-plugin|quarantined-plugin|failed-plugin/i);
    await expect(page.locator('body')).toContainText(/PF4J_JAR|STARTED|DISABLED|QUARANTINED|FAILED/i);
    await expect(page.locator('body')).toContainText(/\[REDACTED\] load failure/i);

    const bodyText = await page.locator('body').innerText();
    expect(bodyText).not.toContain('PI_PHASE8_FAKE_SECRET_DO_NOT_LEAK');
    expect(bodyText).not.toContain('/var/secret/plugins');
    expect(bodyText.toLowerCase()).not.toMatch(/upload plugin|install plugin|delete plugin|upgrade plugin|marketplace|export plugin/);
  });
});
