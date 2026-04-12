# Inventario de costes operativos actuales por servicio

## 1. Objetivo
Identificar y documentar todos los costes de servicio actuales (alojamiento, base de datos, APIs de terceros, etc.) para ofrecer una visión clara de los gastos operativos del proyecto.

## 2. Metodología de inventario
Se clasifican los servicios por categoría de coste:
1. Compute/hosting
2. Base de datos
3. Hosting web y red
4. APIs de terceros
5. Mensajería y notificaciones
6. CI/CD y registro de imágenes
7. Costes indirectos operativos

Para cada servicio se documenta:
- Función técnica
- Tipo de coste (fijo o variable)
- Principal motor de consumo
- Riesgo de aumento de factura

## 2.1 Base económica y trazabilidad del documento
Para evitar estimaciones arbitrarias, este inventario se apoya en fuentes internas del proyecto:

1. Costes históricos reportados
- docs/Sprint 1/estimation_costs.md documenta costes de despliegue en Azure por sprint (17,50 EUR; 35,00 EUR; 52,50 EUR) y un total acumulado de infraestructura de 122,50 EUR en el periodo analizado.

2. Contexto de operación en fase MVP/piloto
- docs/Sprint 0/Pricing.md indica uso de planes gratuitos y créditos en la fase inicial, y una proyección de infraestructura de ~500 EUR/mes para 10.000 usuarios.

3. Volumen de pilotos documentado
- docs/Sprint 2/deliverables/10-S2-pilot-users.md registra 65 usuarios piloto, que sirve como referencia para el tramo inicial de operación.

Conclusión de trazabilidad:
- Los costes actuales reflejan un entorno de bajo gasto por créditos y planes gratuitos.
- Las proyecciones de crecimiento se interpretan como escenarios de planificación, no como factura cerrada de proveedor.

## 3. Inventario de servicios y coste asociado

### 3.1 Compute y hosting backend
Servicio: Backend Spring Boot en contenedor
- Función: API REST, seguridad JWT, lógica de negocio y notificaciones.
- Tipo de coste: Mixto (capacidad base + variable por uso).
- Principal motor de coste:
  - capacidad de proceso (CPU) y memoria por petición
  - conexiones activas de usuarios
  - tareas automáticas y envíos de notificaciones a varios usuarios a la vez
- Riesgo de sobrecoste: medio/alto en crecimiento de concurrencia.

### 3.2 Base de datos
Servicio: MySQL gestionado (producción), H2 en desarrollo y pruebas
- Función: Persistencia transaccional de usuarios, preguntas, respuestas, votos, notificaciones, ubicaciones y suscripciones.
- Tipo de coste: Mixto (nivel de servicio base + almacenamiento, copias de seguridad y operaciones por proveedor).
- Principal motor de coste:
  - volumen de lectura y escritura
  - crecimiento de tablas históricas
  - tareas automáticas programadas y consultas de listados
- Riesgo de sobrecoste: alto al subir de nivel de servicio.

### 3.3 Hosting web
Servicio: EAS Hosting para la web de Expo
- Función: alojamiento de la app web.
- Tipo de coste: Mixto (plan base + consumo de tráfico y artefactos según plan).
- Principal motor de coste:
  - tráfico de usuarios
  - despliegues frecuentes
- Riesgo de sobrecoste: medio en periodos de pruebas intensivas.

### 3.4 APIs y servicios de terceros

#### Stripe
- Función: proceso de pago y confirmación de la suscripción de negocio.
- Tipo de coste: variable por transacción.
- Principal motor de coste: número de cobros exitosos.
- Riesgo: directamente proporcional al volumen de suscripciones.

#### Email transaccional
- Función: envío de correos (SMTP y/o SendGrid).
- Tipo de coste: generalmente variable por volumen.
- Principal motor de coste: número de correos enviados.
- Riesgo: bajo en fase piloto si no hay campañas masivas.

#### OpenStreetMap/Nominatim
- Función: mapas y búsqueda de direcciones.
- Tipo de coste: puede ser cero en uso comunitario, pero con limitaciones de política de uso y servicio.
- Principal motor de coste: número de cargas de mapa y búsquedas de dirección.
- Riesgo: medio, por posible migración a un servicio de pago con garantía de servicio.

### 3.5 Notificaciones y tiempo real

#### Notificaciones en tiempo real dentro de la aplicación
- Función: mensajes que aparecen al instante cuando ocurre una acción relevante en la app.
- Tipo de coste: indirecto (consume capacidad del backend).
- Principal motor de coste:
  - conexiones activas
  - reconexiones
  - volumen de mensajes enviados
- Riesgo: medio/alto en concurrencia elevada.

#### Notificaciones push en el navegador
- Función: avisos que llegan al navegador del usuario aunque no esté dentro de la app en ese momento.
- Tipo de coste: indirecto (servidor, red y operaciones de base de datos).
- Principal motor de coste:
  - número de dispositivos
  - envíos a varios usuarios por cada evento
- Riesgo: medio por carga de envío y filtrado por distancia.

### 3.6 CI/CD, compilación y registro de imágenes
Servicios: GitHub Actions, Docker Hub y despliegues automatizados
- Función: validación de calidad, pruebas, compilación y despliegue.
- Tipo de coste:
  - GitHub Actions: bajo o gratuito según límites o plan
  - Docker Hub: plan base o profesional según uso
- Principal motor de coste:
  - frecuencia de pipelines
  - compilaciones pesadas
  - almacenamiento de imágenes
- Riesgo: bajo/medio, normalmente controlable con buenas políticas de pipeline.

### 3.7 Costes indirectos operativos
1. Logs y observabilidad
- Aumentan con el tráfico y la verbosidad.

2. Tráfico de salida a internet
- Sube con los datos que la plataforma envía a usuarios y dispositivos.

3. Almacenamiento de histórico
- Las tablas de notificaciones, ubicaciones, actividad y auditoría crecen con el uso.

## 4. Matriz resumida de costes actuales
| Categoría | Servicio | Estado actual | Tipo de coste | Principal motor |
|---|---|---|---|---|
| Hosting backend | Contenedor Spring Boot | Activo | Mixto | CPU, memoria, conexiones activas |
| Base de datos | MySQL (prod) / H2 (dev) | Activo | Mixto | Operaciones de lectura/escritura |
| Hosting web | EAS web | Activo | Mixto | Tráfico y despliegues |
| Pagos | Stripe | Activo | Variable | Transacciones |
| Correo | Brevo SMTP / SendGrid | Activo | Variable | Correos enviados |
| Mapas y búsquedas | OSM + Nominatim | Activo | Variable indirecto | Cargas de mapa y búsquedas |
| Tiempo real | Notificaciones en la app + push en el navegador | Activo | Indirecto | Concurrencia y envíos a varios usuarios |
| CI/CD | GitHub Actions + Docker Hub | Activo | Bajo/mixto | Número de pipelines |

## 4.1 Costes actuales documentados en el proyecto
| Concepto | Valor documentado | Fuente interna |
|---|---:|---|
| Coste de despliegue Azure Sprint 0 | 17,50 EUR | docs/Sprint 1/estimation_costs.md |
| Coste de despliegue Azure Sprint 1 | 17,50 EUR | docs/Sprint 1/estimation_costs.md |
| Coste de despliegue Azure Sprint 2 | 35,00 EUR | docs/Sprint 1/estimation_costs.md |
| Coste de despliegue Azure Sprint 3 (estimado) | 52,50 EUR | docs/Sprint 1/estimation_costs.md |
| Total infraestructura periodo analizado | 122,50 EUR | docs/Sprint 1/estimation_costs.md |
| Coste actual en MVP con planes gratuitos/créditos | 0 EUR (operativo) | docs/Sprint 0/Pricing.md |
| Referencia de infraestructura a escala (10k usuarios) | ~500 EUR/mes | docs/Sprint 0/Pricing.md |

## 5. Principales generadores de gasto hoy
1. Base de datos MySQL (lectura/escritura y nivel de servicio).
2. Compute backend por uso de API y notificaciones en tiempo real.
3. Costes variables de Stripe (si hay conversión de pagos).
4. Tráfico de salida y registros al aumentar notificaciones y tráfico.

## 6. Recomendaciones para gestión de gasto operativo
1. Crear una hoja mensual de costes por categoría.
2. Medir coste por usuario activo y coste por 1.000 requests.
3. Definir alertas de presupuesto por servicio.
4. Revisar retención de datos para contener almacenamiento.
5. Evaluar cambios de nivel de servicio y optimizaciones antes de picos de uso.

## 7. Conclusiones
- Este inventario ofrece una visión clara de qué servicios generan gasto y cuál es su peso dentro del proyecto.
- A partir de él se pueden priorizar las áreas que conviene vigilar primero: base de datos, backend y servicios de terceros.
- El documento sirve como base para fijar presupuestos, definir alertas de gasto y planificar futuras mejoras de infraestructura.

## Anexo A. Glosario
- API: interfaz que permite que una parte del sistema se comunique con otra.
- Backend: parte del sistema que procesa la lógica y guarda los datos.
- CDN: red de servidores que acelera la entrega de contenido (imágenes, JS, CSS).
- CPU: capacidad de proceso del servidor.
- Créditos: saldo promocional o gratuito que ofrece un proveedor cloud.
- DAU: usuarios activos diarios.
- Egress: tráfico de salida desde la infraestructura hacia internet.
- Fan-out: una acción genera envíos a varios usuarios al mismo tiempo.
- IOPS: número de operaciones de lectura/escritura por segundo en disco o base de datos.
- Nivel de servicio: plan o nivel contratado con un proveedor cloud.
- Tarea programada: acción que se ejecuta automáticamente cada cierto tiempo sin que una persona la lance.
- Tráfico de salida: datos que salen del servidor hacia los usuarios.
