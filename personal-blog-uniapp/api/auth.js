import { post } from "../utils/request.js"

export function login(data) {
  return post("/user/auth/login", data)
}

export function register(data) {
  return post("/user/auth/register", data)
}
