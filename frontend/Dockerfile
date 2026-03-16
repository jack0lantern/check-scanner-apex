# Build stage
FROM node:20-alpine AS build
WORKDIR /app

COPY package.json package-lock.json ./
RUN npm ci

COPY . .
# VITE_API_BASE from Railway variables is used at build time for production API URL
RUN npm run build

# Serve stage
FROM node:20-alpine
WORKDIR /app
RUN npm install -g serve
COPY --from=build /app/dist ./dist
EXPOSE 3000
# Railway sets PORT; serve -l uses it
CMD ["sh", "-c", "serve -s dist -l ${PORT:-3000}"]
