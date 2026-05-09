import { get } from "../utils/request.js"

export function listPublicFriendLinks(params) {
  return get("/public/friend-links/list", params)
}
