# Imagen base de Node.js 22.14 slim
FROM node:24.5-slim AS build

WORKDIR /app

# Copiar todo el código al contenedor
COPY . .

# Instalar las dependencias
RUN npm install

# Compila Angular con SSR (sin ejecutar el script de creación de env.ts aquí)
RUN npm run build

# Servir la aplicación utilizando un servidor Node.js
FROM node:24.5-slim AS production

RUN apt update && apt upgrade -y && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Copiar la aplicación compilada desde la etapa de construcción
COPY --from=build /app/dist /app/dist

# Copiar el script de inicio al contenedor
COPY /init.sh /init.sh

# Dar permisos de ejecución al script
RUN chmod +x /init.sh

EXPOSE 4200

ENTRYPOINT ["/init.sh"]
