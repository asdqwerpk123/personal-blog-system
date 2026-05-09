import { buildApiUrl, storageKeys } from "./config.js"

const SUCCESS_CODE = 200
const UNAUTHORIZED_CODE = 401
const LOGIN_PAGE = "/pages/auth/login"

function normalizePath(url) {
  if (!url) {
    return ""
  }
  if (url.startsWith("http://") || url.startsWith("https://")) {
    try {
      return new URL(url).pathname
    } catch (error) {
      return url
    }
  }
  return url.startsWith("/") ? url : `/${url}`
}

function shouldAttachAuthorization(url, method) {
  const normalizedMethod = (method || "GET").toUpperCase()
  const path = normalizePath(url)

  if (path.startsWith("/public/")) {
    return false
  }
  if (normalizedMethod === "POST" && path === "/user/auth/login") {
    return false
  }
  if (normalizedMethod === "POST" && path === "/user/auth/register") {
    return false
  }
  return path.startsWith("/user/")
}

export function requiresUserAuthorization(url, method) {
  return shouldAttachAuthorization(url, method)
}

export function getToken() {
  return uni.getStorageSync(storageKeys.token) || ""
}

export function getRoleCode() {
  return uni.getStorageSync(storageKeys.roleCode) || ""
}

export function getStoredUserInfo() {
  return uni.getStorageSync(storageKeys.userInfo) || {}
}

export function isUserLoggedIn() {
  return Boolean(getToken() && getRoleCode() === "USER")
}

export function clearLoginState() {
  uni.removeStorageSync(storageKeys.token)
  uni.removeStorageSync(storageKeys.userInfo)
  uni.removeStorageSync(storageKeys.roleCode)
}

export function saveLoginState(loginData) {
  if (!loginData || loginData.roleCode !== "USER" || !loginData.accessToken) {
    clearLoginState()
    throw new Error("Only USER accounts can sign in to this client")
  }
  uni.setStorageSync(storageKeys.token, loginData.accessToken)
  uni.setStorageSync(storageKeys.userInfo, loginData)
  uni.setStorageSync(storageKeys.roleCode, loginData.roleCode)
}

export function saveUserInfo(userInfo) {
  const previous = getStoredUserInfo()
  const merged = {
    ...previous,
    ...userInfo,
    roleCode: userInfo && userInfo.roleCode ? userInfo.roleCode : getRoleCode()
  }
  uni.setStorageSync(storageKeys.userInfo, merged)
  if (merged.roleCode) {
    uni.setStorageSync(storageKeys.roleCode, merged.roleCode)
  }
  return merged
}

export function getCurrentPageUrl() {
  const pages = getCurrentPages()
  if (!pages.length) {
    return ""
  }
  const currentPage = pages[pages.length - 1]
  const route = `/${currentPage.route}`
  const options = currentPage.options || {}
  const query = Object.keys(options)
    .map((key) => `${encodeURIComponent(key)}=${encodeURIComponent(options[key])}`)
    .join("&")
  return query ? `${route}?${query}` : route
}

export function buildLoginUrl(redirect) {
  const target = redirect || getCurrentPageUrl()
  return target ? `${LOGIN_PAGE}?redirect=${encodeURIComponent(target)}` : LOGIN_PAGE
}

function redirectToLogin(redirect) {
  const currentRoute = getCurrentPageUrl().split("?")[0]
  if (currentRoute !== LOGIN_PAGE) {
    uni.redirectTo({ url: buildLoginUrl(redirect) })
  }
}

function handleUnauthorized(reject, message, redirect) {
  clearLoginState()
  uni.showToast({
    title: message || "请先登录",
    icon: "none"
  })
  redirectToLogin(redirect)
  reject(new Error(message || "Unauthorized"))
}

function showError(message) {
  uni.showToast({
    title: message || "请求失败",
    icon: "none"
  })
}

export function request(options) {
  const method = (options.method || "GET").toUpperCase()
  const requiresAuth = shouldAttachAuthorization(options.url, method)
  const headers = {
    ...(options.header || {})
  }

  if (requiresAuth) {
    const token = getToken()
    if (token) {
      headers.Authorization = `Bearer ${token}`
    }
  }

  return new Promise((resolve, reject) => {
    uni.request({
      url: buildApiUrl(options.url),
      method,
      data: options.data || {},
      header: headers,
      success(response) {
        const body = response.data || {}
        if (response.statusCode === UNAUTHORIZED_CODE || body.code === UNAUTHORIZED_CODE) {
          if (requiresAuth) {
            handleUnauthorized(reject, body.message, options.redirect)
          } else {
            const message = body.message || "请求未授权"
            showError(message)
            reject(new Error(message))
          }
          return
        }
        if (response.statusCode < 200 || response.statusCode >= 300) {
          const message = body.message || `HTTP ${response.statusCode}`
          showError(message)
          reject(new Error(message))
          return
        }
        if (body.code === SUCCESS_CODE) {
          resolve(body.data)
          return
        }

        const message = body.message || "请求失败"
        showError(message)
        reject(new Error(message))
      },
      fail(error) {
        const message = error && error.errMsg ? error.errMsg : "网络请求失败"
        showError(message)
        reject(error)
      }
    })
  })
}

export function get(url, params) {
  return request({
    url,
    method: "GET",
    data: params
  })
}

export function post(url, data) {
  return request({
    url,
    method: "POST",
    data
  })
}

export function put(url, data) {
  return request({
    url,
    method: "PUT",
    data
  })
}

export function del(url, data) {
  return request({
    url,
    method: "DELETE",
    data
  })
}
