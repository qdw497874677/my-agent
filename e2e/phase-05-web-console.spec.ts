import { expect, test } from '@playwright/test';
import {
  attachKeyScreenshot,
  cancelRun,
  createApprovalRun,
  createRun,
  decideApproval,
  devHeaders,
  expectConsoleShell,
} from './fixtures/fake-runtime';

test.describe('Phase 5 Agent Web Console and Runtime Cockpit', () => {
  test('happy path covers catalog, chat shell, streaming events, tool card data, and terminal result', async ({ page, request }) => {
    await expectConsoleShell(page);
    await attachKeyScreenshot(page, 'console-shell');

    await page.goto('/console', { waitUntil: 'domcontentloaded' });
    await expect(page.locator('[data-layout="chat-home"]').first()).toBeVisible();
    await expect(page.locator('[data-role="model-selector"], [data-role="provider-status"]').first()).toBeVisible();
    await expect(page.locator('[data-column="chat-event-stream"]').first()).toBeVisible();
    await expect(page.locator('[data-action="show-console-panel"]')).toHaveCount(0);
    await expect(page.locator('[data-role="chat-input"]').first()).toBeVisible();
    await expect(page.locator('[data-action="send-chat"]').first()).toBeVisible();

    const catalog = await request.get('/api/agents', { headers: devHeaders });
    expect(catalog.ok()).toBeTruthy();
    const catalogJson = await catalog.json();
    expect(catalogJson.agents[0]).toMatchObject({ id: 'cloud-general-agent', name: 'Cloud General Agent' });
    expect(catalogJson.agents[0].capabilities).toContain('streaming-events');

    const outcome = await createRun(request, 'please stream and use a tool');
    expect(outcome.status).toBe('COMPLETED');
    expect(outcome.events.map((event) => event.type)).toEqual(
      expect.arrayContaining(['model.delta', 'tool.proposed', 'tool.policy_decided', 'tool.started', 'tool.completed', 'run.completed']),
    );
    expect(outcome.events.filter((event) => event.type === 'model.delta').length).toBeGreaterThanOrEqual(1);
    expect(JSON.stringify(outcome.events)).toContain('builtin.info');
    expect(JSON.stringify(outcome.events)).toContain('toolGovernance');

    const history = await request.get(`/api/sessions/${outcome.sessionId}/history`, { headers: devHeaders });
    expect(history.ok()).toBeTruthy();
    const result = await request.get(`/api/sessions/${outcome.sessionId}/runs/${outcome.runId}/result`, { headers: devHeaders });
    expect(result.ok()).toBeTruthy();
    expect(await result.json()).toMatchObject({ status: 'COMPLETED' });
  });

  test('approval approve and reject branches emit auditable approval outcomes', async ({ request }) => {
    const approvedRun = await createApprovalRun(request);
    expect(approvedRun.status).toBe('POLICY_BLOCKED');
    const approvalsResponse = await request.get(`/api/sessions/${approvedRun.sessionId}/runs/${approvedRun.runId}/approvals`, { headers: devHeaders });
    expect(approvalsResponse.ok()).toBeTruthy();
    const approvalId = (await approvalsResponse.json())[0].approvalId;

    const approved = await decideApproval(request, approvedRun.sessionId, approvedRun.runId, approvalId, 'APPROVE');
    expect(approved).toMatchObject({ decision: 'APPROVE', approvalId });

    const rejectedRun = await createApprovalRun(request);
    const rejectedApprovalsResponse = await request.get(`/api/sessions/${rejectedRun.sessionId}/runs/${rejectedRun.runId}/approvals`, { headers: devHeaders });
    expect(rejectedApprovalsResponse.ok()).toBeTruthy();
    const rejectedId = (await rejectedApprovalsResponse.json())[0].approvalId;
    const rejected = await decideApproval(request, rejectedRun.sessionId, rejectedRun.runId, rejectedId, 'REJECT');
    expect(rejected).toMatchObject({ decision: 'REJECT', approvalId: rejectedId });
  });

  test('session continuation and cancellation branches are observable from public APIs', async ({ request }) => {
    const first = await createRun(request, 'first continuation message');
    const continuation = await request.post(`/api/sessions/${first.sessionId}/runs`, {
      headers: devHeaders,
      data: {
        agentId: 'cloud-general-agent',
        inputType: 'chat',
        input: { text: 'continue the session' },
        workspaceId: 'e2e-workspace',
        metadata: { source: 'playwright-continuation' },
      },
    });
    expect(continuation.ok()).toBeTruthy();
    const continuedRun = await continuation.json();
    expect(continuedRun.sessionId).toBe(first.sessionId);

    const cancelled = await cancelRun(request);
    expect(['CANCELLED', 'COMPLETED', 'TIMED_OUT']).toContain(cancelled.status);
    expect(JSON.stringify(cancelled.events)).toContain('run.');
  });

  test('admin governance read-only overview, policy, audit, and placeholder views are reachable', async ({ page, request }) => {
    for (const path of [
      '/api/admin/governance/overview',
      '/api/admin/governance/policy-decisions',
      '/api/admin/governance/audits',
    ]) {
      await page.goto(path);
      await expect(page.locator('body')).toContainText(/runtime|policy|audit|extensions|mcp|plugins|\[/i);
    }
    await attachKeyScreenshot(page, 'admin-governance');

    const overview = await request.get('/api/admin/governance/overview', { headers: devHeaders });
    expect(overview.ok()).toBeTruthy();
    const overviewJson = await overview.json();
    expect(overviewJson.extensions.status).toMatch(/FUTURE_ENABLED|UNCONFIGURED/);
    expect(overviewJson.mcp.status).toMatch(/FUTURE_ENABLED|UNCONFIGURED/);
    expect(overviewJson.plugins.status).toMatch(/FUTURE_ENABLED|UNCONFIGURED/);
  });
});
