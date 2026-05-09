import { get } from "../utils/request.js"

export function pagePublicArticles(params) {
  return get("/public/articles/page", params)
}

export function getPublicArticle(id) {
  return get(`/public/articles/${id}`)
}
