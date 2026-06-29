import { APIRequestContext, expect, Page, test } from '@playwright/test';

export const devHeaders = {
  'X-Pi-Dev-Tenant': 'e2e-tenant',
  'X-Pi-Dev-User': 'e2e-user',
};

export function mobileToolApprovalHint(): string {
  return 'please stream and use a tool; approval workspace write; cancel me slowly';
}

export function phase13RuntimeCardMatrixHint(): string {
  return 'phase 13 runtime card matrix: emit model status policy terminal error dense detail events; use a governed tool; approval workspace write; redact sk-live-secret and raw-token-value';
}

export type RuntimeRun = {
  sessionId: string;
  runId: string;
  status: string;
  events: RuntimeEvent[];
};

export type RestoredConversation = RuntimeRun & {
  prompt: string;
  assistantPattern: RegExp;
};

export type RuntimeEvent = {
  id: string;
  sequence: number;
  type: string;
  payload: Record<string, unknown>;
  payloadSchema?: string;
};

export async function createRun(request: APIRequestContext, text: string): Promise<RuntimeRun> {
  const sessionResponse = await request.post('/api/sessions', {
    headers: devHeaders,
    data: {
      workspaceId: 'e2e-workspace',
      metadata: { source: 'playwright' },
    },
  });
  expect(sessionResponse.ok()).toBeTruthy();
  const session = await sessionResponse.json();

  const runResponse = await request.post(`/api/sessions/${session.sessionId}/runs`, {
    headers: devHeaders,
    data: {
      agentId: 'cloud-general-agent',
      inputType: 'chat',
      input: { text },
      workspaceId: 'e2e-workspace',
      metadata: { source: 'playwright' },
    },
  });
  expect(runResponse.ok()).toBeTruthy();
  const run = await runResponse.json();

  const terminal = await waitForTerminal(request, session.sessionId, run.runId);
  return { sessionId: session.sessionId, runId: run.runId, status: terminal.status, events: await listEvents(request, session.sessionId, run.runId) };
}

export async function createRestoredConversation(request: APIRequestContext): Promise<RestoredConversation> {
  const prompt = 'Phase 17 restored conversation: explain recent history without raw runtime events';
  const run = await createRun(request, prompt);
  return {
    ...run,
    prompt,
    assistantPattern: /model reply|completed|fallback|assistant|response/i,
  };
}

export async function waitForTerminal(request: APIRequestContext, sessionId: string, runId: string) {
  for (let attempt = 0; attempt < 80; attempt += 1) {
    const response = await request.get(`/api/sessions/${sessionId}/runs/${runId}/status`, { headers: devHeaders });
    expect(response.ok()).toBeTruthy();
    const status = await response.json();
    if (status.terminal) {
      return status;
    }
    await new Promise((resolve) => setTimeout(resolve, 250));
  }
  throw new Error(`Run ${runId} did not become terminal`);
}

export async function listEvents(request: APIRequestContext, sessionId: string, runId: string): Promise<RuntimeEvent[]> {
  const response = await request.get(`/api/sessions/${sessionId}/runs/${runId}/events?afterSequence=0&limit=100`, { headers: devHeaders });
  expect(response.ok()).toBeTruthy();
  const history = await response.json();
  return history.events;
}

export async function createApprovalRun(request: APIRequestContext): Promise<RuntimeRun> {
  return createRun(request, 'approval workspace write');
}

export async function decideApproval(
  request: APIRequestContext,
  sessionId: string,
  runId: string,
  approvalId: string,
  decision: 'APPROVE' | 'REJECT',
) {
  const response = await request.post(`/api/sessions/${sessionId}/runs/${runId}/approvals/${approvalId}/decision`, {
    headers: devHeaders,
    data: {
      decision,
      actorRole: 'USER',
      reason: `${decision.toLowerCase()} by Playwright`,
    },
  });
  expect(response.ok()).toBeTruthy();
  return response.json();
}

export async function cancelRun(request: APIRequestContext): Promise<RuntimeRun> {
  const sessionResponse = await request.post('/api/sessions', { headers: devHeaders, data: { workspaceId: 'e2e-workspace', metadata: {} } });
  expect(sessionResponse.ok()).toBeTruthy();
  const session = await sessionResponse.json();
  const runResponse = await request.post(`/api/sessions/${session.sessionId}/runs`, {
    headers: devHeaders,
    data: { agentId: 'cloud-general-agent', inputType: 'chat', input: { text: 'cancel me slowly' }, workspaceId: 'e2e-workspace', metadata: {} },
  });
  expect(runResponse.ok()).toBeTruthy();
  const run = await runResponse.json();
  const cancelResponse = await request.post(`/api/sessions/${session.sessionId}/runs/${run.runId}/cancel`, {
    headers: devHeaders,
    data: { reason: 'Playwright cancellation branch' },
  });
  expect(cancelResponse.ok()).toBeTruthy();
  const terminal = await waitForTerminal(request, session.sessionId, run.runId);
  return { sessionId: session.sessionId, runId: run.runId, status: terminal.status, events: await listEvents(request, session.sessionId, run.runId) };
}

export async function expectConsoleShell(page: Page) {
  await page.goto('/api/agents');
  await expect(page.locator('body')).toContainText('cloud-general-agent');
  await expect(page.locator('body')).toContainText('Cloud General Agent');
  await expect(page.locator('body')).toContainText('streaming-events');
}

export async function attachKeyScreenshot(page: Page, name: string) {
  await test.info().attach(name, {
    body: await page.screenshot({ fullPage: true }),
    contentType: 'image/png',
  });
}
