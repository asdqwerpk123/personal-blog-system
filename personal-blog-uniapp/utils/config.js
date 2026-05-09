export const baseURL = "http://localhost:8081"

export const storageKeys = {
  token: "token",
  userInfo: "userInfo",
  roleCode: "roleCode"
}

export const defaultImages = {
  cover: "/static/default-cover.svg",
  avatar: "/static/default-avatar.svg",
  empty: "/static/empty.svg"
}

export function resolveAssetUrl(url, fallback = defaultImages.cover) {
  const value = typeof url === "string" ? url.trim() : ""
  if (!value) {
    return fallback
  }
  if (value.startsWith("http://") || value.startsWith("https://")) {
    return value
  }

  const normalizedBase = baseURL.endsWith("/") ? baseURL.slice(0, -1) : baseURL
  if (value.startsWith("/")) {
    return `${normalizedBase}${value}`
  }
  return `${normalizedBase}/${value}`
}

export function buildApiUrl(url) {
  if (url.startsWith("http://") || url.startsWith("https://")) {
    return url
  }

  const normalizedBase = baseURL.endsWith("/") ? baseURL.slice(0, -1) : baseURL
  const normalizedPath = url.startsWith("/") ? url : `/${url}`
  return `${normalizedBase}${normalizedPath}`
}
