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
