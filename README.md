# ISPP-G10

Aplicación para conocer en tiempo real información relevante sobre eventos en vivo.

## Tecnologías

| Capa          | Tecnología                         |
| ------------- | ---------------------------------- |
| Frontend      | React Native + Expo (JavaScript)   |
| Backend       | Spring Boot 3 (Java 21)            |
| Base de datos | H2 en memoria (dev) / MySQL (prod) |
| Autenticación | JWT                                |

## ¿Cómo ejecutar?

| Modo                   | Base de datos | Cuándo usar                               |
| ---------------------- | ------------- | ----------------------------------------- |
| **Sin Docker**         | H2 (memoria)  | Desarrollo rápido, no requiere setup      |
| **Con Docker**         | H2 (memoria)  | Desarrollo con hot-reload y contenedores  |
| **Con Docker + MySQL** | MySQL         | Probar queries reales antes de producción |
| **Producción**         | MySQL (Azure) | Despliegue real                           |

> ⚠️ **IMPORTANTE**: No puedes ejecutar ambas opciones a la vez. Usan los mismos puertos (8080, 8081). Para cambiar de modo, para primero el que esté corriendo.

---

## Opción 1: Sin Docker (Desarrollo Rápido)

**Requisitos**: Java 21 LTS, Node.js LTS

Esta opción usa **H2** (base de datos en memoria). No necesitas instalar nada más.

### Backend

```bash
# Windows
.\mvnw.cmd spring-boot:run

# macOS/Linux
./mvnw spring-boot:run
```

El backend estará en `http://localhost:8080`

### Frontend

```bash
cd frontend
npm install
npm start
```

Escanea el QR con Expo Go o pulsa `w` para abrir en el navegador.

### URLs útiles

| URL                                         | Descripción                 |
| ------------------------------------------- | --------------------------- |
| http://localhost:8080/swagger-ui/index.html | Documentación de la API     |
| http://localhost:8080/h2-console            | Consola de base de datos H2 |

> **Nota**: Los datos en H2 se pierden al reiniciar. Esto es intencional para desarrollo.

---

## Opción 2: Con Docker (Recomendado para Desarrollo)

**Requisitos**: Docker Desktop

Esta opción levanta **todo** (frontend, backend) con un solo comando. Usa **H2 en memoria** por defecto para máxima velocidad.

> ### 📌 Comandos principales

#### Preparar el entorno

```bash
cp frontend/.env.dev frontend/.env
```

> **Primera vez o cambios en dependencias:**
>
> ```bash
> docker-compose up -d --build
> ```
>
> **Día a día (desarrollo normal):**
>
> ```bash
> docker-compose up -d
> ```
>
> **Parar todo:**
>
> ```bash
> docker-compose down
> ```
>
> **¿Algo roto? Reinicio completo:**
>
> ```bash
> docker-compose down -v && docker-compose up -d --build 
> ```

### URLs

| URL                                         | Servicio       |
| ------------------------------------------- | -------------- |
| http://localhost:8081                       | Frontend (Web) |
| http://localhost:8080/swagger-ui/index.html | API Docs       |
| http://localhost:8080/h2-console            | Consola H2     |
| http://localhost:8080/api/v1/\*             | Backend API    |

**Acceso a H2 Console:**

- JDBC URL: `jdbc:h2:mem:streetask`
- Usuario: `sa`
- Password: _(dejar vacío)_

### Desarrollo diario

Durante el desarrollo, los cambios se reflejan **automáticamente**:

✅ **Backend** (Java): Spring DevTools recarga al detectar cambios  
✅ **Frontend** (React): Hot reload habilitado con `CHOKIDAR_USEPOLLING`

> **⚠️ Cuándo usar `--build`:**
>
> - Primera vez que ejecutas el proyecto
> - Cambias `pom.xml` (nuevas dependencias Maven)
> - Cambias `package.json` (nuevas dependencias npm)
> - Cambias `Dockerfile.dev` o `docker-compose.yml`
>
> **Para desarrollo diario NO es necesario** — los cambios en código se reflejan automáticamente.

---

### 🆘 ¿Algo no funciona? Comando mágico

Si tienes problemas (contenedores no arrancan, errores raros, etc.), ejecuta esto:

```bash
docker-compose down -v && docker-compose up -d --build
```

Esto **para todo, limpia volúmenes y reconstruye desde cero**. Soluciona el 90% de problemas.

### Usar MySQL en lugar de H2 (opcional)

Si necesitas probar con MySQL (ej: queries específicas de producción):

1. Abre `docker-compose.yml`
2. Descomenta el servicio `db` (MySQL)
3. Descomenta las variables de entorno de MySQL en `backend`
4. Comenta `SPRING_PROFILES_ACTIVE: default` y descomenta `SPRING_PROFILES_ACTIVE: mysql`
5. Ejecuta: `docker-compose down -v && docker-compose up -d --build`

> **Nota**: Con H2, los datos se resetean al reiniciar. Esto es ideal para desarrollo limpio.

---

## Test de carga en local

Si quieres probar rendimiento usando el entorno local, tienes un script k6 en [src/test/java/com/streetask/app/load/streetask-load-test.js](src/test/java/com/streetask/app/load/streetask-load-test.js).

### Ejecución

**Requisitos:** 
- `k6` debe estar instalado y disponible en el `PATH`.
- El backend debe estar corriendo.

Si no lo tienes instalado, en Ubuntu puedes hacerlo con APT:

```bash
# 1. Asegura que gpg esta configurado
sudo gpg -k

# 2. Descarga y anade la clave oficial del repositorio de k6
sudo gpg --no-default-keyring --keyring /usr/share/keyrings/k6-archive-keyring.gpg \
	--keyserver hkp://keyserver.ubuntu.com:80 --recv-keys C5AD17C747E3415A3642D57D77C6C491D6AC1D69

# 3. Anade el repositorio oficial de k6 a tu lista de fuentes
echo "deb [signed-by=/usr/share/keyrings/k6-archive-keyring.gpg] https://dl.k6.io/deb stable main" \
	| sudo tee /etc/apt/sources.list.d/k6.list

# 4. Actualiza la lista de paquetes y descarga k6
sudo apt-get update
sudo apt-get install k6
```

En Windows puedes hacerlo con `winget`:

```powershell
winget install k6 --source winget
```
Para correr los tests:

```bash
k6 run src/test/java/com/streetask/app/load/streetask-load-test.js
```

## Variables de entorno

### Frontend (frontend/.env)

```bash
cp frontend/.env.example frontend/.env  # Windows: copy frontend\.env.example frontend\.env
```

| Variable                     | Valor por defecto       | Descripción                    |
| ---------------------------- | ----------------------- | ------------------------------ |
| `EXPO_PUBLIC_API_BASE_URL`   | `http://localhost:8080` | URL del backend                |
| `EXPO_PUBLIC_API_TIMEOUT_MS` | `10000`                 | Timeout de las peticiones (ms) |

### Backend (.env en la raíz)

```bash
cp .env.example .env  # Windows: copy .env.example .env
```

| Variable              | Ejemplo                   | Descripción                         |
| --------------------- | ------------------------- | ----------------------------------- |
| `MYSQL_ROOT_PASSWORD` | `rootpassword`            | Password root MySQL local (Docker)  |
| `MYSQL_DATABASE`      | `streetask`               | Nombre de base de datos             |
| `MYSQL_USER`          | `streetask`               | Usuario MySQL                       |
| `MYSQL_PASSWORD`      | `streetask`               | Password MySQL                      |
| `BREVO_SMTP_HOST`     | `smtp-relay.brevo.com`    | Host SMTP Brevo                     |
| `BREVO_SMTP_PORT`     | `587`                     | Puerto SMTP Brevo                   |
| `BREVO_SMTP_USERNAME` | `tu-login@smtp-brevo.com` | Login SMTP Brevo                    |
| `BREVO_SMTP_PASSWORD` | `tu-smtp-key`             | Clave SMTP Brevo                    |
| `BREVO_MAIL_FROM`     | `streetask0@gmail.com`    | Remitente de emails transaccionales |
| `STRIPE_SECRET_KEY`    | `sk_test_...`             | Clave privada de Stripe (backend)   |
| `STRIPE_PUBLISHABLE_KEY` | `pk_test_...`          | Clave pública de Stripe             |
| `FRONTEND_URL` | `https://ppl-streetask.expo.app` | URL base del frontend para redirecciones de Stripe |
| `STREETASK_STRIPE_SUCCESS_URL` | `https://ppl-streetask.expo.app` | Redirección tras pago Stripe exitoso |
| `STREETASK_STRIPE_CANCEL_URL` | `https://ppl-streetask.expo.app` | Redirección cuando se cancela Stripe |
| `STREETASK_STRIPE_STREETCOINS_SUCCESS_URL` | `https://ppl-streetask.expo.app` | Redirección éxito para compra de StreetCoins |
| `STREETASK_STRIPE_STREETCOINS_CANCEL_URL` | `https://ppl-streetask.expo.app` | Redirección cancelación para StreetCoins |
| `STREETASK_STRIPE_ALLOWED_RETURN_ORIGIN_PATTERNS` | `https://ppl-streetask.expo.app,https://streetask-preprod-frontend.onrender.com` | Allowlist de orígenes válidos para retorno dinámico |

> Para cloud (Render/Azure): configura al menos `FRONTEND_URL` y `STREETASK_STRIPE_ALLOWED_RETURN_ORIGIN_PATTERNS`.
> Si quieres máxima previsibilidad, define también todas las `STREETASK_STRIPE_*_URL` explícitamente.

### Matriz de configuración por entorno

| Entorno                | Frontend API URL                                                             | SMTP Brevo                                                | Dónde se configura               |
| ---------------------- | ---------------------------------------------------------------------------- | --------------------------------------------------------- | -------------------------------- |
| Local (sin Docker)     | `frontend/.env` con backend local o remoto                                   | `.env` raíz                                               | Archivos `.env` locales          |
| Local (Docker Compose) | Definido en `docker-compose.yml` (servicio frontend)                         | Variables `BREVO_*` en `.env` raíz, inyectadas al backend | `.env` + `docker-compose.yml`    |
| Expo (dev en móvil)    | `EXPO_PUBLIC_API_BASE_URL` debe ser URL accesible (no `localhost` del móvil) | No aplica en app cliente (se envía desde backend)         | `frontend/.env` o variables EAS  |
| Render / Azure (cloud) | Variable de entorno del frontend en plataforma                               | Variables `BREVO_*` en backend cloud                      | Panel de variables del proveedor |

### Estado actual del proyecto

- ✅ SMTP Brevo configurado y validado en local.
- ✅ Backend preparado para leer `BREVO_*` por variables de entorno.
- ✅ Docker Compose preparado para propagar variables SMTP al backend.
- ✅ Documentación base en `.env.example` actualizada.
- ⚠️ Cada entorno cloud (Render/Azure/EAS) necesita cargar sus propias variables en su panel.

---

## Tests E2E de Interfaz con Selenium

En este proyecto utilizamos Selenium WebDriver junto con JUnit  para realizar pruebas de la interfaz de usuario (E2E). Para agilizar la creación de estos tests, nos apoyamos en la extensión de navegador TestCase Studio.

### Requisitos Previos e Instalación
Para grabar nuevos flujos de prueba, necesitas instalar la extensión TestCase Studio:
- Disponible para [Google Chrome](https://chromewebstore.google.com/detail/testcase-studio-selenium/loopjjegnlccnhgfehekecpanpmielcj?hl=es&utm_source=ext_sidebar) y navegadores basados en Chromium.
- Esta herramienta graba tus clics e interacciones en la web y genera automáticamente los selectores (`XPath` y `CSS`) que necesitamos para cada Test en Java.

### Grabar un nuevo Test 
1. Levanta el entorno local (Frontend en `8081` y Backend en `8080`).
2. Abre tu navegador y activa la extensión TestCase Studio.
3. Navega por tu aplicación realizando el flujo que quieres probar (ej. hacer login, crear una pregunta, responder una pregunta).
4. La extensión grabará cada paso. Al terminar, copia los selectores generados (preferiblemente los `XPath` relativos, ya que son más robustos a cambios en la interfaz).

### @Disabled
Todos los tests E2E están deshabilitados por defecto con `@Disabled` por dos motivos:
- **Evitar bloqueos en CI/CD o JaCoCo:** Estos entornos no levantan el frontend por defecto; el test fallaría al no encontrar la interfaz, entonces no hay aumento de cobertura de tests.
- **Evitar colisión de puertos:** Si tu backend ya está corriendo localmente, Spring Boot lanzará un `IllegalStateException` al intentar usar el puerto `8080` de nuevo.

### Ejecutar los tests manualmente
Para probar un flujo en tu máquina, sigue estos pasos exactos:

1. Apaga tu Backend (si lo tienes corriendo en el IDE).
2. Mantén tu Frontend encendido (`puerto 8081`).
3. Elimina temporalmente la anotación `@Disabled` del test que quieras probar.
4. Ejecuta el test. Spring Boot levantará su propio backend temporal y abrirá Chrome.

> **💡 Tip: Modo Silencioso (Headless)**
> Para ejecutar los tests sin que la ventana de Chrome se abra visualmente por si no está instalado, descomenta la línea `options.addArguments("--headless");` en el método `@BeforeEach`.

---

## Producción (Azure)

Backend, Frontend y MySQL están desplegados en Azure. El despliegue es automático vía CI/CD.

- **Backend**: Azure App Service
- **Frontend**: Expo (EAS Build)
- **Database**: Azure MySQL

---

## Solución de Problemas

### VS Code muestra errores de dependencias falsos

Si VS Code muestra cientos de errores de imports pero Maven compila bien (`./mvnw compile`), el caché del Java Language Server está desincronizado.

**¿Por qué pasa esto?** El Java Language Server de VS Code genera archivos de caché (`.classpath`, `bin/`, etc.) que a veces se corrompen al hacer pull, cambiar de rama, o editar el `pom.xml`. La configuración del proyecto ya está optimizada para minimizar esto, pero si ocurre:

**Opción 1 - Sin cerrar VS Code (recomendada):**

1. Presiona `Ctrl+Shift+P` (Windows/Linux) o `Cmd+Shift+P` (macOS)
2. Escribe `Java: Clean Java Language Server Workspace`
3. Selecciona "Reload and delete"
4. Espera ~30 segundos a que reinicie

**Opción 2 - Con script (si la opción 1 no funciona):**

> ⚠️ **IMPORTANTE**: Cierra VS Code completamente antes de ejecutar el script

```bash
# Windows (PowerShell) - Abre PowerShell desde el menú inicio, NO desde VS Code
cd C:\Users\TU_USUARIO\Desktop\ISPP\ISPP-G8
.\scripts\clean-java-cache.ps1

# macOS/Linux - Abre Terminal, NO el terminal de VS Code
cd ~/Desktop/ISPP/ISPP-G8
chmod +x scripts/clean-java-cache.sh
./scripts/clean-java-cache.sh
```

Después de ejecutar el script, abre VS Code de nuevo.

**Consejos para evitar que vuelva a pasar:**

- Después de hacer `git pull`, espera que el Java LS termine de indexar (barra de carga abajo)
- Si cambias de rama, haz `Ctrl+Shift+P` → `Java: Clean Java Language Server Workspace`
- Nunca commits archivos `.classpath`, `.project`, `bin/`, `.settings/` (ya están en `.gitignore`)

### El backend no conecta a MySQL en Docker

1. Verifica que Docker Desktop esté ejecutándose
2. Espera a que el healthcheck de MySQL pase (~30 segundos)
3. Revisa los logs: `docker-compose logs db`
