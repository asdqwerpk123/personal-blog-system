import { afterEach, beforeEach, vi } from 'vitest';

function createMemoryStorage() {
  const store = new Map();

  return {
    getItem(key) {
      return store.has(key) ? store.get(key) : null;
    },
    setItem(key, value) {
      store.set(String(key), String(value));
    },
    removeItem(key) {
      store.delete(String(key));
    },
    clear() {
      store.clear();
    }
  };
}

function installBrowserStorageGlobals() {
  const local = typeof window !== 'undefined' && window.localStorage ? window.localStorage : createMemoryStorage();
  const session = typeof window !== 'undefined' && window.sessionStorage ? window.sessionStorage : createMemoryStorage();

  Object.defineProperty(globalThis, 'localStorage', {
    configurable: true,
    value: local
  });

  Object.defineProperty(globalThis, 'sessionStorage', {
    configurable: true,
    value: session
  });
}

installBrowserStorageGlobals();

beforeEach(() => {
  installBrowserStorageGlobals();
});

afterEach(() => {
  vi.restoreAllMocks();
});
