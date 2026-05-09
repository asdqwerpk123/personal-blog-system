import { get } from "../utils/request.js"

export function listPublicTags() {
  return get("/public/tags/list")
}
