# Informe de Uso de IA — WPL

## 0. Resumen ejecutivo

Durante la fase de **World Project Launch (WPL)**, el uso de herramientas de Inteligencia Artificial Generativa siguió siendo un complemento relevante del trabajo del equipo, aunque con un enfoque distinto al de sprints anteriores. En etapas previas del proyecto, la IA se utilizó principalmente como apoyo al desarrollo intensivo de funcionalidades, pruebas y arquitectura. En WPL, en cambio, el peso del trabajo se desplazó hacia la **estabilización del producto, la preparación del lanzamiento y la generación de materiales de comunicación**, por lo que el uso de IA también cambió de naturaleza: menos generación masiva de código y más apoyo a documentación, validación, redacción, contenidos promocionales y revisión.

Las herramientas utilizadas con mayor frecuencia han sido **ChatGPT, Gemini, GitHub Copilot y Claude Code**, cada una con un papel diferenciado dentro del flujo de trabajo. En todos los casos la IA se usó como **herramienta de asistencia**, nunca como sustituto de la revisión humana ni de la responsabilidad técnica del equipo. Cada salida producida por una herramienta de IA fue validada manualmente antes de incorporarse al repositorio, a la presentación o a los materiales públicos del proyecto.

El equipo considera que el principal beneficio aportado por la IA durante WPL ha sido **reducir el tiempo invertido en tareas repetitivas o de baja complejidad** (formato, redacción, revisión, generación de variantes de texto, esquemas iniciales de slides, ideas de copy para anuncios, etc.), permitiendo que las personas se concentren en las decisiones de producto, la coordinación entre áreas y la calidad final del entregable.

---

## 1. Herramientas utilizadas y rol asignado

Aunque las herramientas de IA generativa son intercambiables en muchos casos, el equipo ha ido especializando su uso según el tipo de tarea. La siguiente tabla resume el reparto observado durante WPL:

| Herramienta | Uso principal en WPL | Ventaja diferencial percibida |
|---|---|---|
| **ChatGPT** | Redacción y revisión de documentación, generación de variantes de copy publicitario, resúmenes de feedback, soporte a la elaboración de la knowledge base. | Buena calidad lingüística en español y flexibilidad para iterar tono. |
| **Gemini** | Búsqueda complementaria de referencias, contraste de cifras de mercado, exploración de fuentes alternativas para datos económicos y de marketing. | Acceso a información más reciente y verificación cruzada de datos. |
| **GitHub Copilot** | Autocompletado puntual en código durante tareas de mantenimiento y pequeños refactores. | Integración directa en el editor; agiliza tareas repetitivas. |
| **Claude Code** | Generación, refinamiento y depuración de los anuncios audiovisuales y de la landing, soporte a scripts de grabación y automatización, edición de documentación técnica. | Manejo cómodo de archivos largos y trabajo multi-archivo con contexto amplio. |

Esta especialización no es estricta: en muchos casos varias herramientas se usan en paralelo para contrastar respuestas, especialmente cuando el contenido se publica fuera del repositorio (anuncios, landing, redes sociales, presentación).

---

## 2. Soporte en mantenimiento y pequeñas mejoras de código

Dado que en WPL el peso del desarrollo se redujo respecto a sprints anteriores, el papel de la IA en código se limitó a tareas **acotadas y de bajo riesgo**:

- **Refactorización ligera:** sugerencias para simplificar componentes ya existentes, renombrar variables, extraer pequeños helpers o eliminar duplicación menor en archivos del frontend y del backend.
- **Soporte en corrección de bugs:** ante errores pequeños o reportes de comportamientos extraños, se usó la IA para proponer hipótesis sobre la causa probable y validar posibles soluciones antes de aplicarlas.
- **Mejora de funcionalidades existentes:** ajustes en mensajes de error, validaciones de formularios, comportamientos de navegación, animaciones o pequeñas correcciones de UI.
- **Asistencia en revisión de código:** revisión secundaria de PRs para detectar inconsistencias, faltas de validación o posibles efectos colaterales antes de la integración final.

En todos los casos, las propuestas de la IA se han tratado como **borradores**: nunca se han fusionado cambios automáticos sin lectura humana, ejecución de pruebas y validación manual.

---

## 3. Actualización y mejora de la documentación

Aunque el desarrollo activo fue menor, mantener la documentación alineada con el estado real del proyecto siguió siendo crítico para el cierre del WPL.

- **Sincronización con el estado del proyecto:** la IA ayudó a revisar documentos existentes para reflejar la estructura actual del repositorio, las funcionalidades realmente implementadas y los ajustes realizados durante el sprint.
- **Mejora de claridad:** se usaron herramientas de IA para reorganizar, resumir y simplificar explicaciones técnicas largas, especialmente las dirigidas a perfiles no técnicos (presentación, knowledge base, materiales para el público objetivo).
- **Formato y consistencia:** la IA contribuyó a mantener una terminología, estructura y tono uniformes a lo largo de los documentos de WPL, evitando contradicciones entre los entregables económicos, técnicos y de marketing.
- **Traducción y adaptación:** en algunos documentos generados originalmente con apuntes en inglés o con citas de fuentes externas, la IA se usó para producir versiones en español consistentes con el resto del proyecto.

El objetivo no fue *generar documentación de forma automática*, sino **acelerar el trabajo de redacción y revisión** sin perder control sobre el contenido.

---

## 4. Soporte al flujo de trabajo y al equipo

Las herramientas de IA siguieron apoyando los procesos de colaboración del equipo durante WPL.

- **Estructuración de pull requests:** redacción de descripciones de PR más claras, listas de cambios y resúmenes de impacto que facilitan la revisión por parte de otros miembros del equipo.
- **Reducción del trabajo repetitivo:** automatización ligera o asistida de tareas como reformatear listados, generar checklists de validación, organizar notas de reuniones o preparar versiones limpias de documentos colaborativos.
- **Asistencia en validación previa al cierre de tareas:** antes de marcar tareas como completadas, la IA se usó para sugerir validaciones faltantes, posibles casos límite o pequeñas comprobaciones manuales.
- **Organización de tareas y priorización:** apoyo puntual para resumir el trabajo pendiente, agrupar tareas por área (desarrollo, marketing, costes, presentación) y aclarar dependencias entre subtareas.

En este apartado la IA actúa más como un **asistente operativo** que como herramienta técnica: ayuda al equipo a coordinarse, pero las decisiones de planificación se toman entre las personas del grupo.

---

## 5. Creación de anuncios, landing y contenido promocional

Esta ha sido, sin duda, la categoría de uso más visible durante WPL. La preparación del lanzamiento exigió generar **varias piezas de comunicación** alineadas entre sí: anuncios audiovisuales, copy de redes sociales, banner para las pantallas de la ETSII, landing page y materiales de presentación.

- **Generación y refinamiento con Claude Code:** Claude Code se utilizó intensamente para apoyar la creación de los anuncios audiovisuales para los dos públicos definidos (inversores y clientes), iterando sobre escenas, transiciones, textos en pantalla y narración. La herramienta ayudó tanto en la generación inicial como en la depuración de los scripts de grabación y procesamiento.
- **Iteración rápida de mensajes:** la IA permitió generar y comparar rápidamente diferentes versiones de eslóganes, copys cortos para redes, llamadas a la acción y descripciones de la propuesta de valor, adaptadas a los distintos canales y audiencias seleccionadas en el plan de marketing.
- **Coherencia entre piezas:** se usó la IA para revisar que el tono, las palabras clave y la propuesta de valor presentada en anuncios, banner, landing y presentación fueran coherentes entre sí, evitando contradicciones entre materiales que el público objetivo podría ver en momentos distintos.
- **Apoyo a la accesibilidad y legibilidad:** sugerencias para evitar bloques de texto demasiado densos, mejorar contrastes, simplificar el lenguaje y dejar mensajes más entendibles a primera vista, en línea con el feedback recibido sobre presentaciones anteriores.
- **Mayor eficiencia en la creación de contenido:** el uso combinado de varias herramientas redujo de forma significativa el tiempo necesario para preparar contenido promocional. Aun así, **toda pieza pública pasó por revisión y personalización final por parte del equipo** antes de ser publicada.

Es importante destacar que la IA no se usó para **reemplazar la voz** del equipo, sino para **acelerar la fase de borrador** y permitir más iteraciones de calidad en menos tiempo.

---

## 6. Apoyo en testing, QA y revisión funcional

La IA también funcionó como apoyo en tareas de pruebas y validación del sistema:

- **Identificación de escenarios de error:** ayuda a enumerar posibles flujos problemáticos, entradas inesperadas y combinaciones poco habituales que podrían dejar al producto en mal estado durante una demo.
- **Sugerencia de casos límite:** generación de listas de casos límite para revisión manual, especialmente en flujos críticos de cara al lanzamiento (registro, login, navegación principal, formularios).
- **Verificación rápida de fragmentos de código:** revisión asistida para confirmar que ciertos cambios no afectan funcionalidades implementadas previamente, sin necesidad de ejecutar siempre la suite completa.
- **Cross-checking de la documentación con el comportamiento real:** comparación entre lo descrito en la documentación y lo observado al usar la app, para detectar incoherencias antes de mostrar el producto al público objetivo.

La validación final, sin embargo, siempre se realizó **manualmente y en el producto real**, ejecutando los flujos en dispositivos y entornos representativos del usuario final.

---

## 7. Apoyo a documentos de negocio, costes y marketing

Durante WPL, una parte importante del esfuerzo del equipo se ha dedicado a los entregables económicos y estratégicos (estimación de costes por escenarios, break-even, segmentación de mercado, canales de marketing, propuesta de valor, etc.). En todos ellos la IA aportó valor de tres formas:

- **Estructuración inicial:** generación de borradores con la estructura habitual de cada tipo de documento (introducción, objetivo, supuestos, escenarios, conclusiones), sobre los que el equipo trabajó con datos reales.
- **Coherencia entre documentos:** revisión cruzada para que las cifras y las hipótesis manejadas en un documento (por ejemplo, número de usuarios activos) coincidan con las usadas en otro (por ejemplo, ingresos esperados o coste mensual).
- **Mejora de la accesibilidad para perfiles no técnicos:** reescritura de párrafos con vocabulario más sencillo en los documentos pensados para audiencias no especializadas (presentación, knowledge base, comunicación externa).

En estos casos, la IA actúa como un **redactor técnico junior**: produce mucho material útil, pero requiere supervisión cercana porque tiende a inventar datos si no se le proporcionan fuentes.

---

## 8. Limitaciones detectadas y supervisión humana

A pesar del apoyo proporcionado por las herramientas de IA, el equipo ha identificado limitaciones claras que justifican una revisión humana sistemática:

- **Datos inventados o desactualizados:** las herramientas pueden generar cifras, fuentes o referencias que parecen sólidas pero no están verificadas. Por eso, todos los datos económicos y de mercado utilizados en el entregable han sido contrastados con las fuentes documentales acordadas por el equipo.
- **Sesgo hacia respuestas plausibles, no necesariamente correctas:** la IA tiende a producir respuestas coherentes incluso cuando carece de la información real. Esto exige un trabajo activo de verificación, sobre todo en código, comandos y configuraciones.
- **Pérdida de contexto en sesiones largas:** en interacciones extensas, las herramientas pueden olvidar detalles importantes del proyecto. Para mitigarlo, el equipo ha tendido a **dividir tareas largas en bloques** y a aportar resúmenes explícitos del contexto al inicio de cada sesión.
- **Riesgo de homogeneización del tono:** cuando se usa IA para redactar muchos documentos seguidos, el tono puede volverse demasiado uniforme. El equipo ha cuidado de **introducir matices propios**, especialmente en los documentos visibles para el público objetivo y en la presentación.
- **Privacidad y propiedad intelectual:** ningún dato sensible de personas reales, credenciales, claves o información confidencial de terceros se ha compartido con herramientas externas. Las pruebas con datos reales se realizaron en entornos controlados y no en chats de IA.

Como consecuencia, **todas las sugerencias y contenidos generados por IA fueron revisados manualmente antes de incorporarse al proyecto**. La IA se utilizó únicamente como herramienta de asistencia y no como sustituto de decisiones técnicas, económicas o comunicativas.

---

## 9. Investigación y resolución de dudas técnicas

En ocasiones puntuales, las herramientas de IA se utilizaron para:

- consultar conceptos técnicos específicos (frameworks, APIs, formatos de vídeo, configuración de despliegues),
- resolver dudas concretas sobre tecnologías utilizadas en el proyecto,
- explorar alternativas de implementación o de configuración,
- y validar buenas prácticas frente a la documentación oficial de cada herramienta.

En estos casos, el equipo asume que las respuestas de la IA son un **punto de partida**, no una fuente autoritativa. Cuando la respuesta afecta a partes críticas del sistema, la decisión final se contrasta con la documentación oficial.

---

## 10. Diferencias con el uso de IA en sprints anteriores

Comparado con sprints previos, el uso de IA en WPL ha cambiado en varios aspectos:

- **Menos generación de código nuevo:** en sprints anteriores la IA se usaba más para crear funcionalidades desde cero. En WPL, su papel se ha concentrado en mantenimiento, pequeños refactores y depuración.
- **Más generación de contenido y comunicación:** la IA ha tenido un papel mayor en redacción, copy, anuncios, landing, knowledge base y revisión de presentación.
- **Más trabajo de coordinación entre documentos:** en lugar de pedir a la IA que produzca un único documento aislado, se ha utilizado para mantener la coherencia entre varios entregables (técnicos, económicos y de marketing).
- **Mayor exigencia en verificación:** dado que los materiales de WPL son más visibles externamente (público objetivo, evaluación final, posibles inversores), el equipo ha sido más estricto en revisar manualmente cada salida.

Este cambio refleja la propia evolución del proyecto: ya no se trata de construir el producto, sino de **prepararlo para presentarlo al mundo**, lo cual requiere un tipo de apoyo distinto.

---

## 11. Optimización de tiempo y productividad

Aunque el uso de IA fue más moderado durante este sprint, su impacto en productividad sigue siendo significativo:

- **Ahorro de tiempo en tareas repetitivas:** redacción de textos cortos, formateo de tablas, generación de variantes de copy y resúmenes de feedback.
- **Reducción del coste de revisión inicial:** los borradores iniciales se producen más rápido, lo que deja más margen para la revisión y la mejora.
- **Mejor reparto del esfuerzo del equipo:** al delegar el trabajo más mecánico a la IA, el grupo pudo dedicar más atención a tareas que exigen criterio humano: coordinación, validación manual, decisiones de producto y estabilización del proyecto.
- **Más iteraciones de calidad por unidad de tiempo:** especialmente notable en materiales de marketing, donde se pudo comparar varias versiones de un mismo mensaje antes de elegir la final.

Sin embargo, el equipo es consciente de que la IA **no reduce el coste real de un entregable de calidad**, sino que **redistribuye** el esfuerzo: menos tiempo en redacción inicial, más tiempo en verificación, ajuste y personalización.

---

## 12. Consideraciones éticas y de responsabilidad

El uso de IA generativa plantea cuestiones éticas que el equipo ha tenido en cuenta:

- **Atribución:** las herramientas de IA son **apoyo**, no autoría. Los entregables siguen siendo trabajo del equipo, que asume la responsabilidad final del contenido publicado.
- **Veracidad:** se ha evitado publicar datos o afirmaciones generadas por IA sin verificar, especialmente cifras económicas, estadísticas de mercado o citas atribuidas a terceros.
- **Privacidad:** no se han compartido con herramientas externas datos personales sensibles, credenciales, configuraciones privadas ni información confidencial de evaluadores, profesores u otros grupos.
- **Uso razonable:** las herramientas se han utilizado dentro de los términos de uso permitidos y de forma compatible con la finalidad académica del proyecto.

---

## 13. Lecciones aprendidas durante WPL

Como cierre, el equipo destaca varias lecciones aprendidas sobre el uso de IA generativa en esta fase del proyecto:

1. **La IA acelera, pero no decide.** Funciona mejor cuando el equipo aporta criterio, contexto y restricciones claras; sin eso, las respuestas tienden a ser genéricas.
2. **Iterar en bloques pequeños es más útil que pedir un entregable completo.** Cuanto más acotada es la petición, mejor es la salida.
3. **Cruzar fuentes mejora la calidad.** Usar más de una herramienta para contrastar respuestas reduce el riesgo de incorporar información incorrecta.
4. **Documentar el uso de IA es útil incluso para el propio equipo.** Permite identificar qué tareas merece la pena delegar en IA y cuáles no.
5. **La revisión humana sigue siendo el cuello de botella real.** El tiempo ahorrado en redacción se reinvierte en revisión; el equipo ha aprendido a planificar pensando en eso.
6. **Para materiales públicos, el último 20 % siempre lo pone una persona.** Tono, matiz, contexto, ejemplos locales y referencias específicas del proyecto son aportaciones humanas, no de la IA.

En resumen, durante WPL la IA generativa ha sido una herramienta valiosa, especialmente en documentación, contenido promocional y mantenimiento. Su uso ha estado supeditado en todo momento a la revisión humana y al criterio del equipo, lo que ha permitido aprovechar sus ventajas sin comprometer la calidad ni la responsabilidad final sobre el proyecto.
