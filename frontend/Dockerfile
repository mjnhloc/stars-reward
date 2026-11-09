FROM node:22-alpine AS build

WORKDIR /app

ARG VITE_API_URL=/api/v1

ENV VITE_API_URL=${VITE_API_URL}

COPY package*.json ./

RUN npm install

COPY . .

RUN npm run build

FROM nginx:1.29-alpine

# default for docker-compose; on Railway set BACKEND_URL to the backend private domain
ENV BACKEND_URL=http://backend:8080

COPY nginx.conf /etc/nginx/templates/default.conf.template

COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
