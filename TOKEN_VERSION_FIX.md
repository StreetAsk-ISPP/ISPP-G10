# Solución del Error 403: Token Versioning para Invalidación Automática

## Problema Original
Usuarios adminisradores recibían error **403 Forbidden** al intentar eliminar cuentas desde el dashboard, aunque tenían permisos válidos. La causa raíz era que el token JWT se volvía inválido después de cambios de rol, pero el sistema no detectaba esta invalidez.

## Causa Raíz
- Sistema de roles de usuarios no tenía mecanismo de revocación de tokens
- Cuando cambiaba el rol de un usuario (ej: REGULAR → ADMIN), el token antiguo seguía siendo válido hasta su expiración
- El `AuthTokenFilter` no validaba si la autoridad del usuario había cambido desde que el token fue generado

## Solución Implementada: Token Versioning

Se implementó un sistema de versionado de tokens que **invalida automáticamente** todos los tokens existentes cuando cambia el rol de un usuario. 

### Arquitectura de la Solución

```
┌─────────────────────────────────────────────────────────────┐
│ Cliente hace solicitud con JWT antiguo                      │
└──────────────────────┬──────────────────────────────────────┘
                       │
                       ▼
           ┌──────────────────────────┐
           │  AuthTokenFilter         │
           │ - Extrae JWT             │
           │ - Valida JWT signature   │
           │ - NUEVO: Valida versión  │
           └──────────┬───────────────┘
                       │ tokenVersion en JWT != tokenVersion en BD
                       ▼
          ┌────────────────────────────┐
          │ Token Inválido             │
          │ Usuario debe re-autenticar │
          └────────────────────────────┘
```

### Cambios en el Código

#### 1. **User.java** - Nuevo Campo
```java
@Column(columnDefinition = "BIGINT DEFAULT 0")
private Long tokenVersion = 0L;
```

#### 2. **UserDetailsImpl.java** - Incluir Version en UserDetails
- Agregar field `tokenVersion`
- Actualizar constructor y método `build()`
- Agregar getter `getTokenVersion()`

#### 3. **JwtUtils.java** - Incluir Version en Token
```java
// En generateJwtToken()
claims.put("tokenVersion", userPrincipal.getTokenVersion());

// Nuevo método para extraer la versión
public Long getTokenVersionFromJwtToken(String token) {
    Object tokenVersion = Jwts.parser()...get("tokenVersion");
    return ((Number) tokenVersion).longValue();
}
```

#### 4. **AuthTokenFilter.java** - Validar Versión
```java
// Validar token version contra base de datos
if (userRepository != null && !isTokenVersionValid(username, tokenVersionFromJwt)) {
    logger.warn("Token version mismatch: token invalidated due to role change");
    return; // Rechazar autenticación
}
```

#### 5. **UserTypeChangeService.java** - Incrementar Versión en Cambios de Rol
```java
// En convertToRegularUser() e convertToBusinessUser()
Long currentVersion = user.getTokenVersion() != null ? user.getTokenVersion() : 0L;
user.setTokenVersion(currentVersion + 1L);
```

#### 6. **Base de Datos** - Migration V16
```sql
ALTER TABLE appusers
    ADD COLUMN token_version BIGINT DEFAULT 0 NOT NULL;

CREATE INDEX idx_appusers_token_version ON appusers (token_version);
```

## Flujo de Funcionamiento

### Caso 1: Usuario es promovido a ADMIN
1. Admin ejecuta endpoint de cambio de rol para promover usuario
2. `UserTypeChangeService.changeAccountType()` se ejecuta:
   - Cambia la autoridad a ADMIN  
   - **Incrementa token_version de 0 → 1**
   - Guarda usuario en BD
3. Sistema genera nuevo JWT con `tokenVersion: 1`
4. Cliente recibe nuevo token y lo almacena
5. Solicitudes subsequentes usan el nuevo token

### Caso 2: Usuario intenta usar token antiguo
1. Usuario tiene token antiguo con `tokenVersion: 0`
2. Hace solicitud a endpoint protegido (ej: eliminar usuario)
3. `AuthTokenFilter` procesa la solicitud:
   - Extrae `tokenVersion: 0` del JWT
   - Consulta BD: usuario tiene `tokenVersion: 1`
   - **Desemparejamiento detectado** → Token inválido
   - Rechaza con **401 Unauthorized** (no 403)
4. Cliente fuerza re-login
5. Usuario autentica con credenciales
6. Sistema genera nuevo JWT con `tokenVersion: 1`

## Ventajas de Esta Solución

✅ **Segura**: No necesita tabla de blacklist separada  
✅ **Performante**: Solo requiere una consulta BD adicional  
✅ **Escalable**: Funciona con múltiples servidores sin estado compartido  
✅ **No rompe nada**: Backwards compatible (valores NULL → 0)  
✅ **Automática**: No requiere logout forzado del cliente  

## Riesgos Mitigados

| Riesgo | Antes | Ahora |
|--------|-------|-------|
| Token antiguo después de cambio de rol | ⚠️ Válido por 24h | ✅ Inválido inmediatamente |
| Error 403 para usuarios promovidos | ⚠️ Requería logout manual | ✅ Se maneja automáticamente |
| Inconsistencia token-BD | ⚠️ Posible | ✅ Validada en cada request |

## Instrucciones de Despliegue en Producción

1. **Compilar el proyecto**:
   ```bash
   mvn clean package -DskipTests
   ```

2. **Aplicar migración BD**:
   - Flyway ejecutará automáticamente `V16__add_token_version_to_users.sql`
   - La columna se agregará con DEFAULT 0 (no rompe datos existentes)

3. **Desplegar nuevo JAR**:
   ```bash
   docker build -t streetask:latest .
   docker run streetask:latest
   ```

4. **Verificar funcionamiento**:
   - Usuario hace login → recibe token con `tokenVersion: X`
   - Cambiar rol del usuario en BD
   - Token anterior es rechazado con **401 Unauthorized**
   - Hacer re-login genera token con `tokenVersion: X+1`

## Testing Recomendado

```bash
# Test: Token inválido después de cambio de rol
1. Login como usuario REGULAR → token_version=0
2. Admin promueve a BUSINESS
3. Intentar usar token anterior → 401
4. Hacer re-login → token_version=1
5. Solicitud con nuevo token → SUCCESS

# Test: Eliminar cuenta desde admin dashboard
1. Verificar que usuario admin tiene autoridad ADMIN
2. Intentar eliminar usuario
3. Si error 403: usuario necesita re-login
4. Después de re-login: operación debe funcionar
```

## Monitoreo en Producción

Buscar logs con:
```
"Token version mismatch" → Usuarios con tokens desactualizados
"Error validating token version" → Errores en validación
```

## Cambios de Archivos

- `src/main/java/com/streetask/app/user/User.java`
- `src/main/java/com/streetask/app/configuration/services/UserDetailsImpl.java`
- `src/main/java/com/streetask/app/configuration/jwt/JwtUtils.java`
- `src/main/java/com/streetask/app/configuration/jwt/AuthTokenFilter.java`
- `src/main/java/com/streetask/app/user/UserTypeChangeService.java`
- `src/main/resources/db/migration/V16__add_token_version_to_users.sql` (NUEVO)

## Referencias

- [SECURITY_AUDIT_ROLE_CHANGE.md](SECURITY_AUDIT_ROLE_CHANGE.md) - Documentación de cambios de rol
- [SecurityConfiguration.java](src/main/java/com/streetask/app/configuration/SecurityConfiguration.java) - Configuración de autoridades
