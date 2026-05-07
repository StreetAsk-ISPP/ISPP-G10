# Análisis de Punto de Equilibrio (Break-even) — StreetAsk

## 1. Modelo de Ingresos

Existen tres fuentes principales de ingresos:

- **Publicidad (plan gratuito):**
  - Todos los usuarios gratis ven anuncios.
  - eCPM intersticial estimado: 10 EUR/1.000 impresiones.
  - Ingreso medio por publicación: ~0,01 EUR (proyección: 2 preguntas diarias por 100 usuarios ≈ 60 EUR/mes por 200 impresiones diarias).
- **Plan Premium:**
  - Precio: 2,99 EUR/mes.
  - Pensado para usuarios activos que desean ventajas (sin anuncios, más visibilidad, notificaciones, prioridad, etc.).
  - Conversión estimada: 4,2 % de usuarios (de cada 100, ~4 pagan Premium).
- **Plan Business:**
  - Precio: 19,99 EUR/mes.
  - Dirigido a negocios o entidades que buscan visibilidad: distintivo, eventos ilimitados, respuestas priorizadas, soporte premium, etc.
  - Conversión más baja, pero mayor importe por cliente.

## 2. Definición de Costes

Costes directos según la infraestructura y el número de usuarios activos:

| Usuarios Activos | Coste Mensual Estimado      | Nota                                                                |
|------------------|----------------------------|---------------------------------------------------------------------|
| 50               | 15 – 35 EUR                | Mínimo viable, útil para validar                                    |
| 250              | 30 – 70 EUR                | Tracción limitada                                                   |
| 500              | 45 – 110 EUR               | Primer salto en uso de base de datos                                |
| 1.000            | 80 – 190 EUR               | Inicio de escalado en infraestructura                              |
| 10.000           | ~500 EUR                   | Escala estabilizada, si la arquitectura lo permite                  |

Costes considerados:
- Backend (compute, base de datos)
- Hosting frontend
- Logs y observabilidad
- Email/push/automatización
- Pagos (Stripe fees variables, bajos)
- Soporte y contingencia

## 3. Escenarios de Ingresos y Break-even

### 3.1. Supuestos Base
- **Ingresos estimados por usuario/mes:** entre 0,66 EUR (conservador) y 1,08 EUR (escenario favorable)
  - Ads usuario medio: 0,60 EUR/mes
  - Premium + Business usuario medio: 0,48 EUR/mes

### 3.2. Break-even por tamaño de comunidad

| Coste mensual | Usuarios mínimos para cubrir gasto (0,66 €/usuario) | Usuarios (1,08 €/usuario) |
|--------------:|:--------------------------------------------------:|:-------------------------:|
| 35 EUR        |                     53                              |          33               |
| 70 EUR        |                    106                              |          65               |
| 110 EUR       |                    167                              |         102               |
| 190 EUR       |                    288                              |         176               |

> Ejemplo claro: si el gasto es 70 EUR/mes y cada usuario aporta 0,66 EUR, se requieren ~106 usuarios activos para break-even.

### 3.3. Proyección escalar

| Usuarios | Ingreso mensual estimado | Meses para ROI break-even* |
|---------:|------------------------:|-------------------------:|
|  500     |        541 EUR          |          153            |
| 2.500    |      2.709 EUR          |         30.7            |
| 10.000   |     10.839 EUR          |         7.6 ✅           |

*\*Break-even ROI: recuperación de inversión operativa total en meses.*

### 3.4. Ejemplo de mes 1 (100 usuarios activos)

- Ads: 60 EUR  
- Premium: 8,41 EUR  
- Business: 39,98 EUR  
- **Total ingreso:** 108,39 EUR  
- **Infraestructura:** 0 EUR (free tier para el MVP inicial)  
- Resultado: beneficios positivos desde el inicio con baja base de usuarios (hasta 100), pero el coste aumentará al crecer.

## 4. Explicación Clara

- El punto de equilibrio mensual se alcanza cuando los ingresos de la suma de publicidad, Premium y Business permiten cubrir los gastos operativos de infraestructura y soporte.
- En escenarios conservadores (0,66 €/usuario), con **~100–110 usuarios activos recurrentes** ya se cubren costes básicos (70–110 EUR/mes). En escenarios más optimistas, basta con 65–100 usuarios.
- A mayor crecimiento, el coste sube pero el ingreso escala más rápido (a 10.000 usuarios → coste 500 EUR vs ingreso estimado >6.600 EUR).
- El verdadero riesgo es la curva de adopción: retener y monetizar suficiente volumen de usuarios recurrentes y captar cuentas business será el factor decisivo.

## 5. Resumen Numérico

- **Umbral break-even recurrente realista:** entre **100 y 170 usuarios activos** mensuales recurrentes, según escenario.
- **Expansión:** a partir de ~2.500 usuarios, proyecto sólido y retornando inversión.
- **Escalado:** modelo rentable a medio/largo plazo si la captación y retención cumplen métricas de mercado.

---

## 6. Fuentes y Links

- [docs/PPL/costs/estimacion_costes_escenarios.md](https://github.com/StreetAsk-ISPP/ISPP-G10/blob/main/docs/PPL/costs/estimacion_costes_escenarios.md)
- [docs/Sprint 0/Pricing.md](https://github.com/StreetAsk-ISPP/ISPP-G10/blob/main/docs/Sprint%200/Pricing.md)
- [docs/Sprint 1/estimation_costs.md](https://github.com/StreetAsk-ISPP/ISPP-G10/blob/main/docs/Sprint%201/estimation_costs.md)
- [docs/Sprint 3/pilot-cost-forecast.md](https://github.com/StreetAsk-ISPP/ISPP-G10/blob/main/docs/Sprint%203/pilot-cost-forecast.md)