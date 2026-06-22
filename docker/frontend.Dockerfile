FROM node:22-alpine AS builder

WORKDIR /workspace/personal-blog-admin-web

COPY personal-blog-admin-web/package.json personal-blog-admin-web/package-lock.json ./
RUN npm ci

COPY personal-blog-admin-web ./

RUN npm run build

FROM nginx:1.27-alpine

COPY docker/nginx/default.conf /etc/nginx/conf.d/default.conf
COPY --from=builder /workspace/personal-blog-admin-web/dist /usr/share/nginx/html

EXPOSE 80
