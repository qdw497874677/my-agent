import { defineConfig, devices } from '@playwright/test';

const port = Number(process.env.PI_E2E_PORT ?? 18080);
const baseURL = process.env.PLAYWRIGHT_BASE_URL ?? `http://127.0.0.1:${port}`;

export default defineConfig({
  testDir: './e2e',
  timeout: 60_000,
  expect: {
    timeout: 10_000,
  },
  fullyParallel: false,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 1 : 0,
  reporter: [['list'], ['html', { open: 'never' }]],
  use: {
    baseURL,
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
    video: 'retain-on-failure',
    extraHTTPHeaders: {
      'X-Pi-Dev-Tenant': 'e2e-tenant',
      'X-Pi-Dev-User': 'e2e-user',
    },
  },
  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
  ],
  webServer: process.env.PLAYWRIGHT_SKIP_WEBSERVER
    ? undefined
    : {
        command:
          'JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-adapter-web -am spring-boot:run ' +
          '-Dspring-boot.run.useTestClasspath=true ' +
          '-Dspring-boot.run.profiles=test,e2e ' +
          `-Dspring-boot.run.arguments="--server.port=${port} --spring.main.allow-bean-definition-overriding=true --spring.flyway.enabled=false --spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration --pi.runtime.worker.poll-delay-ms=100"`,
        url: `${baseURL}/actuator/health`,
        reuseExistingServer: !process.env.CI,
        timeout: 120_000,
        stdout: 'pipe',
        stderr: 'pipe',
      },
});
