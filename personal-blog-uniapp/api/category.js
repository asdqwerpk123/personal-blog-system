import { get } from "../utils/request.js"

export function listPublicCategories() {
  return get("/public/categories/list")
}
