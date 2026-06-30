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

export type StreamingRun = RuntimeRun & {
  prompt: string;
  expectedChunks: string[];
  assistantPattern: RegExp;
};

export type CancelledStreamingRun = StreamingRun & {
  cancelReason: string;
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

export function slowStreamingHint(): string {
  return 'phase 18 slow streaming lifecycle: emit chunks Alpha, Beta, Gamma; replay duplicate delta once; finish completed';
}

export function failedStreamingHint(): string {
  return 'phase 18 slow streaming failure: emit partial chunk then provider error with safe public summary';
}

export function cancelledStreamingHint(): string {
  return 'phase 18 slow streaming cancel: emit partial chunk slowly and suppress late delta after cancel';
}

async function createSession(request: APIRequestContext): Promise<{ sessionId: string }> {
  const sessionResponse = await request.post('/api/sessions', {
    headers: devHeaders,
    data: {
      workspaceId: 'e2e-workspace',
      metadata: { source: 'playwright' },
    },
  });
  expect(sessionResponse.ok()).toBeTruthy();
  return sessionResponse.json();
}

async function startRun(request: APIRequestContext, sessionId: string, text: string): Promise<{ runId: string }> {
  const runResponse = await request.post(`/api/sessions/${sessionId}/runs`, {
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
  return runResponse.json();
}

export async function createRun(request: APIRequestContext, text: string): Promise<RuntimeRun> {
  const session = await createSession(request);
  const run = await startRun(request, session.sessionId, text);

  const terminal = await waitForTerminal(request, session.sessionId, run.runId);
  return { sessionId: session.sessionId, runId: run.runId, status: terminal.status, events: await listEvents(request, session.sessionId, run.runId) };
}

export async function createSlowStreamingRun(request: APIRequestContext): Promise<StreamingRun> {
  const prompt = slowStreamingHint();
  const run = await createRun(request, prompt);
  return {
    ...run,
    prompt,
    expectedChunks: ['Alpha', 'Beta', 'Gamma'],
    assistantPattern: /Alpha|Beta|Gamma|model reply|completed|response/i,
  };
}

export async function createFailedStreamingRun(request: APIRequestContext): Promise<StreamingRun> {
  const prompt = failedStreamingHint();
  const run = await createRun(request, prompt);
  return {
    ...run,
    prompt,
    expectedChunks: ['partial'],
    assistantPattern: /failed|provider|unavailable|error|partial/i,
  };
}

export async function cancelStreamingRun(request: APIRequestContext): Promise<CancelledStreamingRun> {
  const prompt = cancelledStreamingHint();
  const cancelReason = 'Playwright Phase 18 cancellation branch';
  const session = await createSession(request);
  const run = await startRun(request, session.sessionId, prompt);
  const cancelResponse = await request.post(`/api/sessions/${session.sessionId}/runs/${run.runId}/cancel`, {
    headers: devHeaders,
    data: { reason: cancelReason },
  });
  expect(cancelResponse.ok()).toBeTruthy();
  const terminal = await waitForTerminal(request, session.sessionId, run.runId);
  return {
    sessionId: session.sessionId,
    runId: run.runId,
    status: terminal.status,
    events: await listEvents(request, session.sessionId, run.runId),
    prompt,
    expectedChunks: ['partial'],
    assistantPattern: /partial|cancel|stopped/i,
    cancelReason,
  };
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
  const session = await createSession(request);
  const run = await startRun(request, session.sessionId, 'cancel me slowly');
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
