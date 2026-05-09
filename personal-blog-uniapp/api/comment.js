import { get, post } from "../utils/request.js"

export function pageArticleComments(articleId, params) {
  return get(`/public/articles/${articleId}/comments`, params)
}

export function createUserComment(data) {
  return post("/user/comments", {
    articleId: data.articleId,
    commentContent: data.commentContent
  })
}
