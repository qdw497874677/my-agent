import { expect, type Locator, type Page } from '@playwright/test';

export type MobileSmokeSelector = {
  name: string;
  selector: string;
};

export type MobileSmokeRoute = {
  path: string;
  routeName: string;
  primaryContent?: MobileSmokeSelector[];
  primaryActions?: MobileSmokeSelector[];
};

export type HorizontalOverflowSnapshot = {
  documentClientWidth: number;
  documentScrollWidth: number;
  bodyClientWidth: number;
  bodyScrollWidth: number;
};

export async function expectNoPageHorizontalOverflow(
  page: Page,
  tolerance = 1,
): Promise<void> {
  const snapshot = await page.evaluate<HorizontalOverflowSnapshot>(() => {
    const documentElement = document.documentElement;
    const body = document.body;

    return {
      documentClientWidth: documentElement.clientWidth,
      documentScrollWidth: documentElement.scrollWidth,
      bodyClientWidth: body?.clientWidth ?? 0,
      bodyScrollWidth: body?.scrollWidth ?? 0,
    };
  });

  expect(
    snapshot.documentScrollWidth,
    `document should not overflow horizontally: ${JSON.stringify(snapshot)}`,
  ).toBeLessThanOrEqual(snapshot.documentClientWidth + tolerance);
  expect(
    snapshot.bodyScrollWidth,
    `body should not overflow horizontally: ${JSON.stringify(snapshot)}`,
  ).toBeLessThanOrEqual(snapshot.bodyClientWidth + tolerance);
}

export async function expectStableSelectorVisible(
  page: Page,
  selector: string,
): Promise<Locator> {
  const locator = page.locator(selector).first();
  await expect(locator, `stable selector should be visible: ${selector}`).toBeVisible();
  return locator;
}

export async function expectTapTargetAtLeast(
  locator: Locator,
  minimum = 44,
  label = 'tap target',
): Promise<void> {
  const target = locator.first();
  await expect(target, `${label} should be visible before geometry sampling`).toBeVisible();
  const compactOptOut = await target.evaluate((element) => element.classList.contains('pi-compact-control'));
  if (compactOptOut) {
    return;
  }
  const box = await target.boundingBox();
  expect(box, `${label} should expose a bounding box`).not.toBeNull();
  expect(box!.width, `${label} width should be at least ${minimum}px: ${JSON.stringify(box)}`)
    .toBeGreaterThanOrEqual(minimum);
  expect(box!.height, `${label} height should be at least ${minimum}px: ${JSON.stringify(box)}`)
    .toBeGreaterThanOrEqual(minimum);
}

export async function expectFocusVisible(
  page: Page,
  locator: Locator,
  label = 'focused control',
): Promise<void> {
  const target = locator.first();
  await expect(target, `${label} should be visible before focus sampling`).toBeVisible();
  await target.focus();
  await expect(target, `${label} should receive focus`).toBeFocused();
  const focusSignal = await target.evaluate((element) => {
    const style = window.getComputedStyle(element);
    const outlineVisible = style.outlineStyle !== 'none'
      && style.outlineWidth !== '0px'
      && style.outlineColor !== 'rgba(0, 0, 0, 0)';
    const boxShadowVisible = style.boxShadow !== 'none';
    const borderVisible = style.borderStyle !== 'none'
      && style.borderWidth !== '0px'
      && style.borderColor !== 'rgba(0, 0, 0, 0)';
    const tokenSignal = element.matches(':focus-visible') || element.classList.contains('focus-visible');
    return { outlineVisible, boxShadowVisible, borderVisible, tokenSignal };
  });
  expect(
    focusSignal.outlineVisible || focusSignal.boxShadowVisible || focusSignal.borderVisible || focusSignal.tokenSignal,
    `${label} should expose a visible focus signal: ${JSON.stringify(focusSignal)}`,
  ).toBeTruthy();
  await page.keyboard.press('Escape').catch(() => undefined);
}

export async function expectStableSelectorsVisible(
  page: Page,
  selectors: MobileSmokeSelector[] = [],
): Promise<void> {
  for (const selector of selectors) {
    await expectStableSelectorVisible(page, selector.selector);
  }
}

export async function expectPrimaryContentVisible(
  page: Page,
  route: Pick<MobileSmokeRoute, 'routeName' | 'primaryContent'>,
): Promise<void> {
  expect(
    route.primaryContent?.length ?? 0,
    `route ${route.routeName} should define at least one primary content selector`,
  ).toBeGreaterThan(0);
  await expectStableSelectorsVisible(page, route.primaryContent);
}

export async function expectPrimaryActionsVisible(
  page: Page,
  route: Pick<MobileSmokeRoute, 'routeName' | 'primaryActions'>,
): Promise<void> {
  expect(
    route.primaryActions?.length ?? 0,
    `route ${route.routeName} should define at least one primary action selector`,
  ).toBeGreaterThan(0);
  await expectStableSelectorsVisible(page, route.primaryActions);
}

export async function expectPrimaryContentOrActionVisible(
  page: Page,
  route: Pick<MobileSmokeRoute, 'routeName' | 'primaryContent' | 'primaryActions'>,
): Promise<void> {
  const selectors = [...(route.primaryContent ?? []), ...(route.primaryActions ?? [])];
  expect(
    selectors.length,
    `route ${route.routeName} should define at least one primary content or action selector`,
  ).toBeGreaterThan(0);
  await expectStableSelectorsVisible(page, selectors);
}

export async function expectMobileRouteBaseline(
  page: Page,
  route: MobileSmokeRoute,
): Promise<void> {
  await page.goto(route.path);
  await expectStableSelectorVisible(page, `[data-route="${route.routeName}"]`);
  await expectPrimaryContentVisible(page, route);
  await expectPrimaryActionsVisible(page, route);
  await expectNoPageHorizontalOverflow(page);
}
