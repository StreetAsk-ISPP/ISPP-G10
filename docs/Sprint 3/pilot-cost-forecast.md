# Proyección de costes de infraestructura para crecimiento de usuarios piloto

## 1. Objetivo
Analizar y proyectar los costes de infraestructura a medida que aumenta el número de usuarios piloto, incluyendo escenarios de escalabilidad y patrones de uso previstos para anticipar gastos futuros.

## 2. Alcance de la proyección
Este informe estima el coste mensual de operación para fases de adopción progresiva:
- 50 usuarios activos (piloto inicial cercano al volumen actual)
- 250 usuarios activos (piloto ampliado)
- 500 usuarios activos (piloto extendido)
- 1.000 usuarios activos (escenario de tension/pre-produccion)

La estimacion cubre:
- Backend en contenedores
- Base de datos MySQL gestionada
- Hosting web del frontend
- Notificaciones en tiempo real (WebSocket)
- Push notifications web
- Integraciones externas (Stripe, email, geocodificación)
- Costes operativos asociados (logs/egress/CI-CD)

## 3. Supuestos de uso
Para estimar costes se usan supuestos conservadores de pilotaje:

1. Actividad de usuarios
- Usuarios activos diarios (DAU) aproximados: 30% de los usuarios activos mensuales
- Sesiones por usuario activo diario: 2-3 por día
- Peticiones API por usuario activo diario: 60-90

2. Uso funcional principal
- Lectura frecuente de preguntas/respuestas y ubicaciones
- Escrituras por creación de preguntas/respuestas, votos y ubicaciones
- Notificaciones por eventos (preguntas cercanas y actividad en respuestas)

3. Tiempo real
- Conexión WebSocket activa para usuarios autenticados
- Reconexión automática del cliente en caídas de red

4. Escenario tecnológico
- Producción sobre MySQL + backend en contenedor
- Frontend web desplegado en EAS
- Sin CDN dedicada (red de distribución de contenido) y sin broker externo en fase piloto

## 4. Patrones de uso que impactan el coste

### 4.1 Mucha lectura de datos
- Consulta de feed, filtros y listados de contenido.
- Consulta de ubicaciones públicas recientes.
- Este patrón aumenta el uso de proceso del servidor y de operaciones de disco en la base de datos.

### 4.2 Muchas escrituras de datos
- Publicación de ubicación del usuario.
- Registro de votos y transacciones de monedas.
- Inserciones de notificaciones y eventos de actividad.
- Este patrón incrementa las operaciones de guardado en la base de datos y el crecimiento del histórico almacenado.

### 4.3 Uso de red y envíos a múltiples usuarios
- WebSockets persistentes por usuario conectado.
- En eventos de alta actividad, una acción puede notificar a varios receptores al mismo tiempo (fan-out).
- Incrementa el consumo de memoria, red y tráfico saliente hacia internet (egress).

### 4.4 Proceso automático que se ejecuta cada minuto
- El sistema revisa cada minuto qué preguntas ya han caducado.
- Esas preguntas se marcan como inactivas automáticamente.
- Este proceso genera carga constante en la base de datos, incluso con poco tráfico de usuarios.

## 5. Proyección de costes mensuales (estimada y trazable)

## 5.1 Resumen por volumen de usuarios activos
| Escenario | Coste mensual estimado |
|---|---:|
| 50 usuarios activos | 15-35 EUR |
| 250 usuarios activos | 30-70 EUR |
| 500 usuarios activos | 45-110 EUR |
| 1.000 usuarios activos | 80-190 EUR |

## 5.2 Desglose aproximado por componente
| Componente | 50 usuarios | 250 usuarios | 500 usuarios | 1.000 usuarios |
|---|---:|---:|---:|---:|
| Compute backend | 5-12 EUR | 10-24 EUR | 15-36 EUR | 30-70 EUR |
| MySQL gestionado | 5-10 EUR | 10-20 EUR | 15-35 EUR | 25-60 EUR |
| Frontend hosting web | 2-5 EUR | 4-10 EUR | 6-15 EUR | 10-25 EUR |
| Logs, egress y observabilidad | 2-5 EUR | 3-10 EUR | 5-15 EUR | 10-25 EUR |
| Email/push/CI-CD (base) | 1-3 EUR | 3-6 EUR | 4-9 EUR | 5-10 EUR |
| **Total estimado** | **15-35 EUR** | **30-70 EUR** | **45-110 EUR** | **80-190 EUR** |

Nota: Stripe se considera aparte como coste variable por transacción.
Nota 2: en contexto académico con créditos o planes gratuitos, el coste efectivo puede ser inferior en fases tempranas.
Nota 3: estos importes son costes mensuales totales de la infraestructura para cada escenario, no coste por usuario individual.

## 5.3 Impacto de Stripe (variable)
Fórmula aproximada por pago:
- 2,9% + 0,30 EUR por transaccion

Ejemplo con suscripción de 19,99 EUR:
- Comisión aproximada por cobro: 0,88 EUR
- 50 cobros/mes: ~44 EUR en comisiones Stripe
- 200 cobros/mes: ~176 EUR en comisiones Stripe

## 6. Escenarios de escalabilidad y puntos no lineales

## 6.1 Escenario A: 50 usuarios activos (piloto inicial)
- Infraestructura mínima viable.
- Escalado generalmente lineal.
- Riesgo bajo de cuellos de botella.

## 6.2 Escenario B: 250 usuarios activos (piloto ampliado)
- Empieza a tensionarse la base de datos en picos.
- WebSocket y notificaciones elevan el consumo de memoria y red.
- Primeros saltos de nivel de servicio posibles en base de datos o compute.

## 6.3 Escenario C: 500 usuarios activos (piloto extendido)
- Mayor probabilidad de coste no lineal por:
  - salto de nivel de servicio de MySQL
  - necesidad de mayor capacidad en contenedor backend
  - mayor coste de egress y logs
- Conviene preparar optimizaciones estructurales.

## 6.4 Escenario D: 1.000 usuarios activos (preproducción)
- Incremento notable de concurrencia en tiempo real.
- Mayor probabilidad de necesitar redimensionado conjunto de compute y base de datos.
- Recomendable validar límites de arquitectura antes de este umbral.

## 6.5 Dónde deja de ser lineal el coste
1. Base de datos:
- El paso de un nivel de servicio básico a uno superior suele implicar un aumento de precio significativo.

2. Tiempo real:
- Con muchas conexiones WebSocket concurrentes, una sola replica puede no ser suficiente.
- Escalar horizontalmente con broker en memoria complica la consistencia y la operación.

3. Integraciones externas:
- Geocodificación y pagos crecen por evento, no solo por usuario.
- Campañas o picos de uso pueden aumentar los costes por encima de lo esperado.

## 7. Riesgos de desviación presupuestaria
1. DAU mayor al previsto.
2. Más acciones por usuario (votos, respuestas, notificaciones) de las modeladas.
3. Incremento de retención de datos sin políticas de limpieza.
4. Picos de tráfico con reconexiones WebSocket más frecuentes.

## 8. Recomendaciones de control
1. Definir presupuesto mensual tope por entorno y alertas de gasto.
2. Medir de forma continua:
- coste total mensual
- coste por usuario activo
- coste por 1.000 requests
3. Revisar periódicamente tareas programadas y consultas más costosas.
4. Planificar umbrales de escalado (DB/compute) antes de alcanzarlos.
5. Revisar trimestralmente el proveedor y el nivel de servicio para optimización de coste-rendimiento.

## 9. Conclusiones
- El coste inicial del piloto es contenido, pero crece rápidamente al aumentar la concurrencia y el tiempo real.
- El principal factor de gasto en crecimiento es la base de datos MySQL junto con el backend.
- A partir de 250-500 usuarios activos pueden aparecer primeros saltos de coste no lineales.
- La visibilidad temprana de métricas financieras y técnicas es clave para evitar sobrecostes.

## Anexo A. Glosario
- DAU: usuarios activos diarios.
- CPU: capacidad de proceso del servidor.
- CDN: red de servidores que acelera la entrega de contenido (imágenes, JS, CSS).
- IOPS: número de operaciones de lectura/escritura por segundo en disco o base de datos.
- Egress: tráfico de salida desde tu infraestructura hacia internet.
- Nivel de servicio: plan o nivel contratado con un proveedor cloud.
- Fee: comisión o tarifa aplicada a un cobro.
- Lectura intensiva: predominan consultas de lectura.
- Escritura frecuente: predominan operaciones de guardado o actualización.
- Fan-out: una acción genera envíos a varios usuarios a la vez.
- Concurrencia: número de usuarios o procesos activos al mismo tiempo.
