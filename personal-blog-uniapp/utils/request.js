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

export function getToken() {
  return uni.getStorageSync(storageKeys.token) || ""
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

function redirectToLogin() {
  const pages = getCurrentPages()
  const currentRoute = pages.length ? `/${pages[pages.length - 1].route}` : ""
  if (currentRoute !== LOGIN_PAGE) {
    uni.redirectTo({ url: LOGIN_PAGE })
  }
}

function handleUnauthorized(reject, message) {
  clearLoginState()
  uni.showToast({
    title: message || "请先登录",
    icon: "none"
  })
  redirectToLogin()
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
  const headers = {
    ...(options.header || {})
  }

  if (shouldAttachAuthorization(options.url, method)) {
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
          handleUnauthorized(reject, body.message)
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
