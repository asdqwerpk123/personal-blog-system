const TOKEN_KEY = 'PB_ADMIN_TOKEN';
const USER_NAME_KEY = 'PB_ADMIN_USER_NAME';
const USER_ID_KEY = 'PB_ADMIN_USER_ID';
const NICK_NAME_KEY = 'PB_ADMIN_NICK_NAME';
const ROLE_ID_KEY = 'PB_ADMIN_ROLE_ID';
const ROLE_CODE_KEY = 'PB_ADMIN_ROLE_CODE';
const ROLE_NAME_KEY = 'PB_ADMIN_ROLE_NAME';
const REMEMBER_KEY = 'PB_ADMIN_REMEMBER';

function stripBearer(token) {
  return String(token || '').replace(/^Bearer\s+/i, '').trim();
}

function readFrom(storage) {
  return {
    token: storage.getItem(TOKEN_KEY) || '',
    userName: storage.getItem(USER_NAME_KEY) || '',
    userId: storage.getItem(USER_ID_KEY) || '',
    nickName: storage.getItem(NICK_NAME_KEY) || '',
    roleId: storage.getItem(ROLE_ID_KEY) || '',
    roleCode: storage.getItem(ROLE_CODE_KEY) || '',
    roleName: storage.getItem(ROLE_NAME_KEY) || ''
  };
}

function clearStorage(storage) {
  storage.removeItem(TOKEN_KEY);
  storage.removeItem(USER_NAME_KEY);
  storage.removeItem(USER_ID_KEY);
  storage.removeItem(NICK_NAME_KEY);
  storage.removeItem(ROLE_ID_KEY);
  storage.removeItem(ROLE_CODE_KEY);
  storage.removeItem(ROLE_NAME_KEY);
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

function setOptional(storage, key, value) {
  storage.setItem(key, value == null ? '' : String(value));
}

export function persistAuth({ token, userName, userId, nickName, roleId, roleCode, roleName, remember }) {
  const targetStorage = remember ? localStorage : sessionStorage;
  const staleStorage = remember ? sessionStorage : localStorage;

  clearStorage(staleStorage);
  targetStorage.setItem(TOKEN_KEY, stripBearer(token));
  targetStorage.setItem(USER_NAME_KEY, userName || 'Admin');
  setOptional(targetStorage, USER_ID_KEY, userId);
  setOptional(targetStorage, NICK_NAME_KEY, nickName);
  setOptional(targetStorage, ROLE_ID_KEY, roleId);
  setOptional(targetStorage, ROLE_CODE_KEY, roleCode);
  setOptional(targetStorage, ROLE_NAME_KEY, roleName);
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
