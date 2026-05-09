import { buildApiUrl } from "../utils/config.js"
import { buildLoginUrl, clearLoginState, getCurrentPageUrl, getToken } from "../utils/request.js"

export function uploadAvatar(filePath) {
  return new Promise((resolve, reject) => {
    const token = getToken()
    uni.uploadFile({
      url: buildApiUrl("/user/files/avatar"),
      filePath,
      name: "file",
      header: token ? {
        Authorization: `Bearer ${token}`
      } : {},
      success(response) {
        let body = {}
        try {
          body = JSON.parse(response.data || "{}")
        } catch (error) {
          uni.showToast({ title: "上传响应解析失败", icon: "none" })
          reject(error)
          return
        }

        if (response.statusCode === 401 || body.code === 401) {
          clearLoginState()
          uni.showToast({ title: body.message || "请先登录", icon: "none" })
          uni.redirectTo({ url: buildLoginUrl(getCurrentPageUrl()) })
          reject(new Error(body.message || "Unauthorized"))
          return
        }

        if (body.code === 200) {
          resolve(body.data)
          return
        }

        const message = body.message || "头像上传失败"
        uni.showToast({ title: message, icon: "none" })
        reject(new Error(message))
      },
      fail(error) {
        uni.showToast({ title: "头像上传失败", icon: "none" })
        reject(error)
      }
    })
  })
}
