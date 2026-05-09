import { get, put } from "../utils/request.js"

export function getProfile() {
  return get("/user/profile/me")
}

export function updateProfile(data) {
  return put("/user/profile/me", data)
}

export function changePassword(data) {
  return put("/user/profile/password", data)
}
