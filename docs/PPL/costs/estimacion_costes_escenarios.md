# Estimación de costes por escenarios

## 1. Objetivo del documento

Este documento resume cuánto puede costar StreetAsk según el número de usuarios y el nivel de uso real. El objetivo es responder a cinco preguntas sencillas:

- cuánto cuesta mantener la plataforma cada mes,
- qué pasa si la usa poca gente,
- qué pasa si la usa mucha gente,
- cuántos usuarios hacen falta para cubrir gastos,
- y en qué momento el proyecto empieza a ser rentable.

La clave no es solo cuánta gente se registra, sino cuánto usa la aplicación. No cuesta lo mismo una app que se abre de vez en cuando que una app en la que la gente pregunta, responde, recibe avisos y consulta eventos todos los días.

## 2. Base metodológica

La estimación parte de una idea simple: no importa solo cuánta gente se registra, sino cuánta gente usa la aplicación de verdad.

Ejemplo sencillo:

- un usuario que entra una vez al mes casi no cambia el coste,
- un usuario que abre la app varias veces al día sí consume más recursos,
- y si además hay notificaciones instantáneas y contenido temporal, el sistema trabaja todavía más.

Por eso el análisis se divide en dos partes:

1. lo que cuesta mantener la aplicación funcionando cada mes,
2. lo que puede ingresar gracias a publicidad y suscripciones.

## 3. Variables que más afectan al coste

### 3.1 Usuarios activos reales

La primera variable importante es cuánta gente usa la app de verdad. No es lo mismo tener 1.000 usuarios registrados que 1.000 usuarios activos.

Un usuario activo puede leer preguntas, escribir respuestas, publicar contenido, mirar eventos, recibir alertas o consultar su perfil. Cada una de esas acciones hace que el servidor trabaje más.

### 3.2 Tiempo real y fan-out de notificaciones

StreetAsk no funciona como una web estática. Se parece más a un chat o a una alarma: cuando ocurre algo, la app tiene que reaccionar rápido.

Eso significa que una sola acción puede avisar a varias personas a la vez. Cuanta más gente esté conectada, más trabajo tiene el sistema. Por eso el coste no sube siempre de forma lineal.

### 3.3 Base de datos y automatizaciones

La base de datos funciona como la memoria de la aplicación. Guarda usuarios, preguntas, respuestas, eventos, votos, notificaciones, ubicaciones, cuentas business y actividad histórica.

Además, el contenido caduca. Las preguntas y respuestas no duran para siempre, así que el sistema debe revisar de forma automática qué sigue activo y qué debe cerrarse. Esto es bueno para el producto, pero añade carga constante.

### 3.4 Monetización por capas

La aplicación no gana dinero de una sola forma, sino de varias:

- publicidad en el plan gratuito,
- suscripción premium,
- suscripción business,
- y boosts extra para negocios.

Esto permite que usuarios que no pagan sigan ayudando a generar ingresos a través de la publicidad, mientras que los usuarios y negocios que sí necesitan más visibilidad aportan una parte mayor del dinero.

### 3.5 Costes externos variables

Además del servidor y la base de datos, también hay otros gastos:

- comisiones de pago,
- emails transaccionales,
- servicios de mapas,
- tráfico de salida,
- registros de actividad,
- y herramientas de monitorización.

Al principio no son muy altos, pero cuando la app crece empiezan a notarse.

## 4. Precios de suscripción y modelo de ingresos

### 4.1 Plan gratuito

El plan gratuito cuesta 0 EUR. Cualquier persona puede usar la aplicación sin pagar.

La forma de ganar dinero aquí es la publicidad. Cada vez que una persona publica una pregunta, aparece un anuncio. Ese anuncio genera, de media:

- eCPM intersticial estimado: 10 EUR.
- Ingreso medio aproximado por publicación: 0,01 EUR.
- Proyección piloto con 100 usuarios: 2 preguntas diarias por usuario, 200 impresiones diarias y 60 EUR mensuales de ingresos publicitarios.

El eCPM intersticial es una forma de decir cuánto dinero se gana por cada 1.000 anuncios mostrados en pantalla. En este caso, 10 EUR significa que, si se enseñan 1.000 anuncios de este tipo, la aplicación puede ingresar unos 10 EUR en total. Por eso, aunque parezca una cantidad pequeña, al repetirse muchas veces acaba sumando ingresos importantes.

Ejemplo: si 100 personas hacen 2 preguntas al día, se generan unas 200 impresiones diarias. Con esa actividad, la app puede llegar a unos 60 EUR al mes.

Este plan tiene dos objetivos:

1. hacer crecer la aplicación sin cobrar al usuario,
2. generar contenido para que otros usuarios encuentren valor.

### 4.2 Plan premium

El plan premium cuesta 2,99 EUR al mes.

Es un precio bajo a propósito. La idea es que pagar casi 3 EUR resulte fácil y no dé mucha sensación de gasto. No está pensado para ganar mucho con una sola persona, sino para que lo puedan pagar muchas.

Sus beneficios previstos son:

- preguntas destacadas,
- notificaciones prioritarias,
- mayor duración de los contenidos,
- más visibilidad en la plataforma,
- ausencia de anuncios,
- y ventajas de reputación o interacción.

La conversión estimada es del 4,2%. Eso significa que, de cada 100 usuarios, unos 4 podrían pagar este plan si lo ven útil.

La idea es sencilla: poco margen por usuario, pero volumen suficiente para que compense.

### 4.3 Plan business

El plan business cuesta 19,99 EUR al mes.

Este precio está pensado para negocios, locales, organizadores o instituciones que quieran más visibilidad y mejores herramientas para comunicar lo que ofrecen.

Incluye, entre otras funcionalidades:

- distintivo de cuenta verificada,
- panel de estadísticas,
- creación ilimitada de eventos,
- eventos recurrentes,
- respuestas priorizadas,
- presencia permanente en mapa,
- soporte prioritario,
- y un boost mensual incluido.

La idea es clara: si el negocio consigue clientes o visibilidad gracias a la plataforma, 19,99 EUR al mes resulta asumible.

### 4.4 Boost adicional

Además del plan business, existe un boost adicional de 14,99 EUR.

Este pago sirve para casos concretos:

- destacar una fiesta,
- dar más visibilidad a un evento durante unos días,
- o aparecer mejor posicionado en un momento especial.

Es un ingreso extra, opcional y fácil de entender: se paga solo cuando hace falta más visibilidad.

## 5. Escenario optimista

### 5.1 Definición

Este escenario describe qué ocurriría si StreetAsk funciona muy bien. La app no solo se descarga, sino que se usa de forma habitual para preguntar, responder y buscar información cercana. En ese punto ya no sería una idea, sino un producto con masa crítica.

### 5.2 Supuestos

Para este escenario se considera:

- 10.000 usuarios activos mensuales.
- Actividad recurrente de preguntas, respuestas y eventos.
- Conversión premium cercana al 4,2%.
- Adopción progresiva de cuentas business.
- Uso suficiente para que la infraestructura llegue a una fase estable de coste, en torno a 500 EUR al mes.

### 5.3 Distribución por tipo de usuario

En este escenario, la distribución estimada de usuarios por tipo sería la siguiente:

| Tipo de usuario | % del total | Por qué este número |
|---|---:|---|
| Gratuitos | 91% | La gran masa. Generan contenido y ven anuncios sin parar. |
| Premium | 6% | Superamos el 4,2% previsto. La gente valora mucho el estatus y las alertas prioritarias. |
| Business | 3% | Los comercios locales ven que StreetAsk les trae clientes de verdad y los 20 EUR les parecen una ganga. |

Resultado: ingresos muy altos y diversificados. El riesgo está en que los servidores trabajen el triple por tanta actividad.

### 5.4 Ingresos estimados

En este escenario el ingreso mensual puede situarse en la siguiente banda:

| Concepto | Estimación |
|---|---:|
| Plan gratuito con publicidad | 2.000 - 3.000 EUR |
| Plan premium | 1.800 - 2.500 EUR |
| Plan business | 2.800 - 5.000 EUR |
| Total mensual | 6.600 - 10.839 EUR |

La diferencia entre el mínimo y el máximo depende de algo muy concreto:

- cuánta gente publica preguntas,
- cuántos usuarios compran premium,
- cuántos negocios contratan business,
- y cuántos boosts extra se compran.

Si la actividad es alta y la conversión acompaña, el ingreso sube hacia el tramo superior. Si el uso es bueno pero la monetización avanza más despacio, el resultado se queda más cerca del inferior.

### 5.5 Coste estimado

| Concepto | Estimación |
|---|---:|
| Infraestructura mensual | 500 - 600 EUR |
| Comisión de pagos | Variable según volumen de suscripciones |
| Observabilidad, logs y tráfico | Incluido en el rango anterior |
| Coste operativo total estimado | 500 - 650 EUR aprox. |

Aquí hay una idea importante: aunque la aplicación crezca, el coste no sube al mismo ritmo que los ingresos si la arquitectura está bien montada. Por eso este escenario es el más interesante.

### 5.6 Lectura del escenario

Este escenario muestra que el producto puede ganar dinero de forma clara si consigue una comunidad activa y una monetización bien repartida. Más actividad no significa automáticamente el doble de coste, pero sí puede significar mucho más ingreso. Esa diferencia hace que el proyecto sea atractivo.

## 6. Escenario pesimista

### 6.1 Definición

Este escenario representa lo que pasa si la aplicación crece más despacio de lo esperado, pero no de forma catastrófica. Es el caso en el que el producto funciona y consigue una comunidad estable, pero la adopción es más lenta y la conversión a planes de pago avanza con dificultad, especialmente en cuentas business.

No es un escenario de fracaso, sino de **crecimiento contenido**: la app vive, mantiene actividad y empieza a generar ingresos, pero tarda años en recuperar la inversión total del proyecto.

### 6.2 Supuestos

En este caso se contemplan los siguientes comportamientos:

- Aproximadamente **1.500 usuarios activos mensuales** tras una fase de captación normal.
- Frecuencia de publicación y respuesta moderada, suficiente para que la app aporte valor en zonas concretas.
- Conversión premium ligeramente por debajo del 4,2 % previsto.
- Adopción business limitada: cuesta convencer a los negocios, aunque algunos sí se apuntan.
- Infraestructura controlada, todavía en un nivel intermedio de coste.

### 6.3 Distribución por tipo de usuario

En este escenario, la distribución estimada de usuarios por tipo sería la siguiente:

| Tipo de usuario | % del total | Por qué este número |
|---|---:|---|
| Gratuitos | 95 % | La gran mayoría se queda en el plan gratis. Generan contenido y vienen anuncios. |
| Premium | 4 % | Conversión moderada-baja, justo por debajo del 4,2 % objetivo. Hay valor percibido, pero pocos pagan todavía. |
| Business | 1 % | Adopción lenta entre negocios: cuesta convencer, pero algunos comercios innovadores empiezan a probar el formato. |

Resultado: la app es viable y crece poco a poco, con ingresos por publicidad como base y una pequeña parte por suscripciones.

### 6.4 Coste estimado

Con una comunidad activa de en torno a 1.500 MAU, el coste mensual operativo estimado se sitúa en una franja intermedia:

| Escenario de usuarios | Coste mensual estimado |
|---|---:|
| 1.000 usuarios activos | 80 - 190 EUR |
| 1.500 usuarios activos | ~150 EUR (estimación intermedia) |

### 6.5 Ingresos estimados

Con la distribución anterior, el ingreso medio por usuario y mes (ARPU) sería:

- Ads (95 % gratuitos): 0,95 × 0,60 = **0,570 EUR**
- Premium (4 %): 0,04 × 2,99 = **0,1196 EUR**
- Business (1 %): 0,01 × 19,99 = **0,1999 EUR**
- **ARPU total ≈ 0,8895 EUR/usuario/mes**

Aplicado a la comunidad estimada:

| Concepto | Estimación |
|---|---:|
| Publicidad | ~855 EUR/mes |
| Premium | ~180 EUR/mes |
| Business | ~300 EUR/mes |
| **Ingreso bruto mensual** | **~1.334 EUR/mes** |
| Coste operativo | ~150 EUR/mes |
| **Ingreso neto mensual** | **~1.184 EUR/mes** |

Lo importante en este escenario no es ganar muchísimo, sino mantener un flujo de ingresos estable mientras la comunidad madura y se valida si la app realmente engancha a más usuarios y negocios.

### 6.6 Riesgos asociados

Los principales riesgos en este escenario son:

1. No alcanzar densidad suficiente en las zonas con más potencial de uso.
2. Perder utilidad percibida si las preguntas tardan demasiado en recibir respuesta.
3. Mantener gastos técnicos innecesarios sin una monetización clara.
4. Invertir en funcionalidades secundarias antes de consolidar el uso básico.

En pocas palabras: si hay poca gente usando la app, sigue costando dinero pero todavía no devuelve suficiente beneficio.

## 7. Coste operativo por volumen de usuarios

Además de los escenarios generales, conviene fijar una referencia más concreta del coste mensual esperado según el tamaño de la comunidad activa.

| Usuarios activos | Coste mensual estimado | Lectura operativa |
|---|---:|---|
| 50 | 15 - 35 EUR | Fase mínima, útil para validar uso inicial |
| 250 | 30 - 70 EUR | Tracción limitada, aún controlable |
| 500 | 45 - 110 EUR | Primer punto de tensión en base de datos |
| 1.000 | 80 - 190 EUR | Posible necesidad de redimensionado |
| 10.000 | ~500 EUR | Escala estabilizada si la arquitectura responde bien |

Esta tabla muestra algo muy útil: al principio el coste es pequeño, pero no desaparece. Y cuando la comunidad crece mucho, aparecen saltos técnicos que obligan a revisar la base de datos, el backend o la forma de enviar notificaciones.

Por eso no basta con mirar cuántos usuarios hay; también hay que mirar cómo se comportan.

## 8. Plan de contingencia

Si el proyecto no alcanza los objetivos previstos, la respuesta no debería ser aumentar complejidad, sino simplificar la operación y proteger la viabilidad económica.

### 8.1 Medidas inmediatas

- Concentrar el producto en una sola ciudad y en pocas zonas de alta densidad.
- Posponer mejoras que no sean esenciales para el uso principal.
- Mantener la infraestructura en su versión más ligera posible.
- Reducir notificaciones y revisar los procesos automáticos de mayor carga.
- Evitar saltos prematuros de base de datos o de capacidad de cálculo.

Estas medidas buscan algo muy concreto: gastar menos mientras se sigue aprendiendo qué funciona y qué no.

### 8.2 Medidas de mejora

- Reforzar la captación en campus, zonas de ocio y puntos de espera.
- Activar dinámicas que aseguren las primeras conversaciones.
- Impulsar premium y business solo cuando el valor percibido esté validado.
- Seguir de forma continua la relación entre usuarios activos, coste por interacción e ingresos reales.

La idea es sencilla: primero conseguir que la gente use la app, luego mejorar la experiencia y solo después empujar con más fuerza la monetización.

### 8.3 Respuesta ante desviaciones de objetivos

Si no se llega al nivel esperado, la prioridad debe ser sostener la operación, aprender del uso real y retomar el crecimiento con más foco.

En términos prácticos, esto implica:

- mantener el MVP con el menor gasto posible,
- retrasar mejoras que solo aporten estética o prestigio,
- centrar el producto en su propuesta de valor principal,
- y no ampliar la infraestructura hasta que la demanda lo justifique.

Traducido a lenguaje simple: si la app no despega todavía, lo mejor no es meter más cosas dentro, sino dejarla más limpia, más útil y más barata de mantener.

## 9. Punto de equilibrio

El punto de equilibrio puede analizarse desde dos perspectivas: la cobertura mensual de costes y el umbral prudente de sostenibilidad.

### 9.1 Equilibrio operativo mensual

Tomando como referencia un ingreso medio conservador de 0,66 EUR por usuario y mes, el número mínimo de usuarios para cubrir distintos niveles de coste sería el siguiente:

| Coste mensual | Usuarios mínimos aproximados |
|---|---:|
| 35 EUR | 53 usuarios |
| 70 EUR | 106 usuarios |
| 110 EUR | 167 usuarios |
| 190 EUR | 288 usuarios |

Si se toma el escenario más favorable, con aproximadamente 1,08 EUR por usuario y mes, la cobertura mejora de forma sensible:

| Coste mensual | Usuarios mínimos aproximados |
|---|---:|
| 35 EUR | 33 usuarios |
| 70 EUR | 65 usuarios |
| 110 EUR | 102 usuarios |
| 190 EUR | 176 usuarios |

Eso significa, por ejemplo, que si el coste del mes es de 70 EUR y cada usuario aporta una media de 0,66 EUR, hacen falta unos 106 usuarios para compensar ese gasto. No es magia: es una cuenta sencilla.

### 9.2 Equilibrio prudente

El mínimo matemático no resulta suficiente como referencia operativa, porque siempre existen comisiones, desviaciones técnicas y cambios de comportamiento. Por ello, el umbral realmente útil es una franja de seguridad más amplia.

La referencia recomendable es la siguiente:

- Mínimo técnico para sostener el servicio: 50 usuarios activos.
- Mínimo prudente para cubrir costes con margen: 180-300 usuarios activos mensuales.
- Zona de estabilidad comercial: 500 usuarios activos o más.

La razón de trabajar con una franja y no con un número exacto es muy simple: en la vida real siempre hay meses mejores y meses peores. Un mes puede haber menos actividad, otro más notificaciones o más cuentas business. La franja protege frente a esas variaciones.

### 9.3 Break-even para la inversión total del proyecto

Más allá del equilibrio operativo mensual, resulta crucial analizar el punto en el que la aplicación puede recuperar la **inversión total del proyecto** (€84.181,38 en escenario de startup real completo).

Este análisis se basa en los porcentajes de distribución por tipo de usuario que se han definido para cada escenario:

#### Escenario Optimista: 91% gratuitos, 6% premium, 3% business

Con esta distribución, el **ARPU (Average Revenue Per User) estimado es de 1,3251 EUR/usuario/mes**:
- Ingresos por ads (usuarios gratuitos): 0,91 × 0,60 = 0,546 EUR
- Ingresos por premium: 0,06 × 2,99 = 0,1794 EUR
- Ingresos por business: 0,03 × 19,99 = 0,5997 EUR

| Usuarios activos | Ingresos mensuales | Recuperación de inversión total |
|---:|---:|---:|
| 10.000 | 13.251 EUR | 6,35 meses (sin costes operativos) |
| 10.000 (con 500 EUR/mes operativos) | 12.751 EUR | 6,60 meses |
| 10.000 (con 650 EUR/mes operativos) | 12.601 EUR | 6,68 meses |

**Lectura:** En el escenario optimista, alcanzar 10.000 usuarios activos con ese mix permite recuperar la inversión total en aproximadamente **6,5 meses**, incluso asumiendo costes operativos de infraestructura. Este es el escenario más favorable para la sostenibilidad del proyecto.

#### Escenario Pesimista: 95% gratuitos, 4% premium, 1% business

Con esta distribución, el **ARPU estimado es de 0,8895 EUR/usuario/mes**:
- Ingresos por ads (usuarios gratuitos): 0,95 × 0,60 = 0,570 EUR
- Ingresos por premium: 0,04 × 2,99 = 0,1196 EUR
- Ingresos por business: 0,01 × 19,99 = 0,1999 EUR

| Usuarios activos | Ingresos mensuales | Recuperación de inversión total |
|---:|---:|---:|
| 1.500 | 1.334,25 EUR | 63 meses (sin costes operativos) |
| 1.500 (con 110 EUR/mes operativos) | 1.224,25 EUR | 69 meses |
| 1.500 (con 150 EUR/mes operativos) | 1.184,25 EUR | **~71 meses (≈ 5,9 años)** |
| 1.500 (con 190 EUR/mes operativos) | 1.144,25 EUR | 74 meses |

**Lectura:** En el escenario pesimista revisado, con una comunidad estable de 1.500 usuarios activos y una conversión moderada, la inversión total se recupera en torno a **70 meses (unos 6 años)**. Es claramente peor que el escenario esperado (~32 meses) y mucho peor que el optimista (~6,7 meses), pero deja de ser un horizonte irrealista de varias décadas. Este escenario refleja un modelo de **crecimiento lento pero sostenible**: el producto funciona, mantiene actividad y monetiza, aunque el ritmo de retorno obliga a una gestión muy cuidadosa del gasto y a buscar palancas de crecimiento que acerquen al escenario esperado.

#### Conclusión

El contraste entre escenarios evidencia que **la monetización está directamente ligada a la distribución del tipo de usuario**. El éxito del proyecto depende de:

1. **Alcanzar masa crítica en usuarios gratuitos** (que generan ingresos por publicidad y crean densidad de contenido).
2. **Convertir un porcentaje significativo a premium** (usuarios activos que perciben valor en la experiencia mejorada).
3. **Captar cuentas business desde fases tempranas** (negocios que ven ROI tangible en la plataforma).

Sin una distribución favorable hacia premium y business, incluso un crecimiento de usuarios genera ingresos insuficientes para justificar la inversión inicial. Por eso, la estrategia de monetización no debe ser "activar pagos tarde", sino **validar desde el piloto que hay voluntad de pago** en ambas dimensiones.

## 11. Número mínimo de usuarios para cubrir costes

En términos prácticos, la lectura más útil es la siguiente:

- Con alrededor de 50 usuarios activos ya puede sostenerse una versión muy ligera del servicio.
- Entre 180 y 300 usuarios activos mensuales se entra en una zona razonable de cobertura de costes.
- A partir de 500 usuarios activos, el proyecto empieza a contar con una base estable para crecer.

Esta franja no debe interpretarse como una cifra exacta, sino como una estimación prudente que contempla variaciones de infraestructura, pagos y monetización.

Si se quiere decir de forma muy simple, la idea es esta:

- 50 usuarios: el proyecto respira, pero va muy justo.
- 180-300 usuarios: el proyecto empieza a cubrirse de forma razonable.
- 500 usuarios o más: el proyecto empieza a parecer estable.

## 12. Lectura global de la viabilidad

StreetAsk puede ser viable, pero su sostenibilidad depende más de la densidad de uso que del número bruto de usuarios.

El escenario optimista muestra que el modelo puede generar margen si la comunidad crece y participa. El escenario pesimista, en cambio, exige una gestión más prudente del gasto y una concentración clara del producto en los contextos donde aporta más valor.

La clave no es llegar pronto a muchas cuentas registradas, sino reunir suficientes usuarios activos en pocos puntos concretos para que la información sea realmente útil.

## 13. Conclusión

La estimación presenta una estructura económicamente razonable siempre que el crecimiento técnico se mantenga controlado, especialmente en base de datos, tiempo real y notificaciones. El principal riesgo no es el coste fijo inicial, sino la posible falta de densidad de uso durante la fase temprana.

La estrategia más sólida consiste en priorizar la liquidez informativa: asegurar suficientes usuarios activos en zonas concretas para que las preguntas reciban respuesta, el producto tenga utilidad real y el modelo económico pueda consolidarse.

En resumen: la aplicación necesita usuarios, pero sobre todo necesita actividad útil. Si hay preguntas y respuestas rápidas, si los eventos tienen visibilidad y si los negocios ven valor, entonces los ingresos pueden cubrir el coste y el proyecto gana sentido económico.
