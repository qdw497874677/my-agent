import { expect, test, type Page } from '@playwright/test';

const consoleViewports = [
  { name: 'phone', width: 375, height: 812 },
  { name: 'tablet', width: 768, height: 900 },
  { name: 'desktop', width: 1280, height: 900 },
] as const;

type ConsoleViewport = (typeof consoleViewports)[number];

type ConsoleGeometry = {
  readonly documentClientHeight: number;
  readonly documentClientWidth: number;
  readonly documentScrollHeight: number;
  readonly documentScrollWidth: number;
  readonly composerBottom: number;
  readonly composerTop: number;
  readonly feedOverflowY: string;
  readonly feedClientHeight: number;
  readonly heroBottom: number;
  readonly headingRangeBottom: number;
  readonly headingRangeTop: number;
  readonly headingBottom: number;
  readonly headingTop: number;
  readonly modelBottom: number;
  readonly overlayCount: number;
  readonly sessionBottom: number;
  readonly chatOverflowY: string;
};

test.describe('Console viewport containment', () => {
  for (const viewport of consoleViewports) {
    test(`keeps Console content contained at ${viewport.width}x${viewport.height}`, async ({ page }) => {
      await suppressFrameworkOverlays(page);
      await page.setViewportSize(viewport);
      await page.goto('/console', { waitUntil: 'domcontentloaded' });
      await page.evaluate(removeFrameworkOverlays);

      await expect(page.locator('[data-route="console"]').first()).toBeVisible();
      await expect(page.locator('[data-role="conversation-hero"]').first()).toBeVisible();
      await expect(page.locator('[data-role="model-selector"], [data-role="provider-status"]').first()).toBeVisible();
      await expect(page.locator('[data-role="active-session-banner"]').first()).toBeVisible();
      await expect(page.locator('[data-role="event-feed"]').first()).toBeVisible();
      await expect(page.locator('[data-role="chat-composer"]').first()).toBeVisible();

      const geometry = await consoleGeometry(page, viewport);

      expect(geometry.documentScrollWidth, `${viewport.name} Console page should not scroll horizontally`).toBeLessThanOrEqual(geometry.documentClientWidth + 1);
      expect(geometry.documentScrollHeight, `${viewport.name} Console page should not scroll vertically`).toBeLessThanOrEqual(geometry.documentClientHeight + 1);
      expect(geometry.heroBottom, `${viewport.name} hero should remain in the viewport`).toBeLessThanOrEqual(viewport.height + 1);
      expect(geometry.modelBottom, `${viewport.name} model UI should remain in the viewport`).toBeLessThanOrEqual(viewport.height + 1);
      expect(geometry.sessionBottom, `${viewport.name} session UI should remain in the viewport`).toBeLessThanOrEqual(viewport.height + 1);
      expect(geometry.composerTop, `${viewport.name} composer should not render above the viewport`).toBeGreaterThanOrEqual(-1);
      expect(geometry.composerBottom, `${viewport.name} composer should be fully visible without page scrolling`).toBeLessThanOrEqual(viewport.height + 1);
      expect(geometry.feedOverflowY, `${viewport.name} feed should own vertical scrolling`).toMatch(/auto|scroll/);
      if (viewport.width === 375) {
        expect(geometry.feedClientHeight, 'phone feed should retain useful restored-session space').toBeGreaterThanOrEqual(80);
      }
      expect(geometry.chatOverflowY, `${viewport.name} chat shell must not become a second scroll owner`).toMatch(/hidden|clip/);
      expect(geometry.overlayCount, `${viewport.name} test context must not retain an external or framework overlay`).toBe(0);
      expect(geometry.headingRangeTop, `${viewport.name} heading text should start inside its line box`).toBeGreaterThanOrEqual(geometry.headingTop - 1);
      expect(geometry.headingRangeBottom, `${viewport.name} heading text should fit inside its line box`).toBeLessThanOrEqual(geometry.headingBottom + 1);
    });
  }
});

test('Admin provider settings retain the light shared shell and normal document scrolling', async ({ page }) => {
  await suppressFrameworkOverlays(page);
  await page.setViewportSize({ width: 1280, height: 900 });
  await page.goto('/admin/governance/providers', { waitUntil: 'domcontentloaded' });
  await page.evaluate(removeFrameworkOverlays);

  await expect(page.locator('[data-route="admin-providers"]').first()).toBeVisible();

  const styles = await page.evaluate(() => {
    const required = <T extends Element>(selector: string): T => {
      const element = document.querySelector<T>(selector);
      if (element === null) {
        throw new Error(`Missing Admin shell element: ${selector}`);
      }
      return element;
    };
    const documentElement = document.documentElement;
    const shell = required<HTMLElement>('.pi-shell');
    const header = required<HTMLElement>('.pi-shell-header');
    const title = required<HTMLElement>('.pi-page-title');

    return {
      documentClientHeight: documentElement.clientHeight,
      documentScrollHeight: documentElement.scrollHeight,
      headerBackground: window.getComputedStyle(header).backgroundColor,
      shellBackground: window.getComputedStyle(shell).backgroundColor,
      titleFontFamily: window.getComputedStyle(title).fontFamily,
    };
  });

  expect(styles.shellBackground).toBe('rgb(246, 248, 251)');
  expect(styles.headerBackground).toBe('rgb(255, 255, 255)');
  expect(styles.titleFontFamily).not.toContain('SagittaireAsset');
  expect(styles.documentScrollHeight).toBeGreaterThanOrEqual(styles.documentClientHeight);
});

async function suppressFrameworkOverlays(page: Page): Promise<void> {
  await page.addInitScript(removeFrameworkOverlays);
}

function removeFrameworkOverlays(): void {
  document.querySelectorAll('copilot-main, vaadin-dev-tools, vaadin-dev-mode-gizmo').forEach((overlay) => overlay.remove());
  document.querySelectorAll<HTMLElement>('[role="dialog"], vaadin-dialog-overlay, vaadin-notification-card').forEach((overlay) => {
    const text = overlay.textContent?.toLowerCase() ?? '';
    if (text.includes('development mode') || text.includes('dev mode')) {
      overlay.remove();
    }
  });
}

async function consoleGeometry(page: Page, viewport: ConsoleViewport): Promise<ConsoleGeometry> {
  return page.evaluate<ConsoleGeometry>((targetViewport) => {
    const required = <T extends Element>(selector: string): T => {
      const element = document.querySelector<T>(selector);
      if (element === null) {
        throw new Error(`Missing Console element: ${selector}`);
      }
      return element;
    };
    const heading = required<HTMLElement>('.pi-console-hero h1');
    const headingRange = document.createRange();
    headingRange.selectNodeContents(heading);
    const headingRangeRect = headingRange.getBoundingClientRect();
    const documentElement = document.documentElement;
    const hero = required<HTMLElement>('[data-role="conversation-hero"]');
    const model = required<HTMLElement>('[data-role="model-selector"], [data-role="provider-status"]');
    const session = required<HTMLElement>('[data-role="active-session-banner"]');
    const composer = required<HTMLElement>('[data-role="chat-composer"]');
    const feed = required<HTMLElement>('[data-role="event-feed"]');
    const chat = required<HTMLElement>('.pi-console-chat');

    return {
      documentClientHeight: documentElement.clientHeight,
      documentClientWidth: documentElement.clientWidth,
      documentScrollHeight: documentElement.scrollHeight,
      documentScrollWidth: documentElement.scrollWidth,
      composerBottom: composer.getBoundingClientRect().bottom,
      composerTop: composer.getBoundingClientRect().top,
      feedOverflowY: window.getComputedStyle(feed).overflowY,
      feedClientHeight: feed.clientHeight,
      heroBottom: hero.getBoundingClientRect().bottom,
      headingRangeBottom: headingRangeRect.bottom,
      headingRangeTop: headingRangeRect.top,
      headingBottom: heading.getBoundingClientRect().bottom,
      headingTop: heading.getBoundingClientRect().top,
      modelBottom: model.getBoundingClientRect().bottom,
      overlayCount: document.querySelectorAll('copilot-main, vaadin-dev-tools, vaadin-dev-mode-gizmo').length
          + [...document.querySelectorAll<HTMLElement>('[role="dialog"], vaadin-dialog-overlay, vaadin-notification-card')]
              .filter((overlay) => (overlay.textContent?.toLowerCase() ?? '').includes('development mode') || (overlay.textContent?.toLowerCase() ?? '').includes('dev mode'))
              .length,
      sessionBottom: session.getBoundingClientRect().bottom,
      chatOverflowY: window.getComputedStyle(chat).overflowY,
    };
  }, viewport);
}
