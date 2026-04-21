const TOKEN_KEY = 'PB_ADMIN_TOKEN';
const USER_NAME_KEY = 'PB_ADMIN_USER_NAME';
const REMEMBER_KEY = 'PB_ADMIN_REMEMBER';

function stripBearer(token) {
  return String(token || '').replace(/^Bearer\s+/i, '').trim();
}

function readFrom(storage) {
  return {
    token: storage.getItem(TOKEN_KEY) || '',
    userName: storage.getItem(USER_NAME_KEY) || ''
  };
}

function clearStorage(storage) {
  storage.removeItem(TOKEN_KEY);
  storage.removeItem(USER_NAME_KEY);
}

export function extractAccessToken(payload) {
  if (typeof payload === 'string') {
    return stripBearer(payload);
  }

  return stripBearer(
    payload?.data?.accessToken ||
      payload?.data?.token ||
      payload?.accessToken ||
      payload?.token ||
      ''
  );
}

export function persistAuth({ token, userName, remember }) {
  const targetStorage = remember ? localStorage : sessionStorage;
  const staleStorage = remember ? sessionStorage : localStorage;

  clearStorage(staleStorage);
  targetStorage.setItem(TOKEN_KEY, stripBearer(token));
  targetStorage.setItem(USER_NAME_KEY, userName || 'Admin');
  localStorage.setItem(REMEMBER_KEY, remember ? 'true' : 'false');
}

export function getStoredAuth() {
  const localAuth = readFrom(localStorage);

  if (localAuth.token) {
    return {
      ...localAuth,
      remember: true
    };
  }

  const sessionAuth = readFrom(sessionStorage);

  return {
    ...sessionAuth,
    remember: localStorage.getItem(REMEMBER_KEY) !== 'false'
  };
}

export function getStoredToken() {
  return getStoredAuth().token;
}

export function clearStoredAuth() {
  clearStorage(localStorage);
  clearStorage(sessionStorage);
}
