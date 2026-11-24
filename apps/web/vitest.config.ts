import { defineConfig } from 'vitest/config';

// Unit tests cover framework-free logic (route safety, error mapping,
// permission filtering); components are checked by `ng build`.
export default defineConfig({
  test: {
    include: ['src/**/*.spec.ts'],
    environment: 'node',
  },
});
