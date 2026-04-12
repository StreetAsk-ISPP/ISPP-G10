# Protocolo de Comunicación y Gestión de Incidencias (ISPP-G10)

Este documento define el protocolo oficial para la gestión de bloqueos, comunicación interna y el ciclo de entregas del proyecto **StreetAsk**. El objetivo es garantizar que el ritmo de desarrollo no se detenga y que el **Grupo 4** disponga de material estable para las presentaciones de los jueves.

## 1. Ciclo Semanal de Trabajo ("Ciclo StreetAsk")
Nuestra semana de desarrollo se estructura de jueves a miércoles para alinearnos con el feedback de clase y las entregas:

* **Jueves (19:30+):** Tras la clase, los **4 Scrum Masters (SM)** se reúnen para analizar el feedback recibido en la presentación y planificar la estrategia de la siguiente semana.
* **Viernes:** Los SM crean las Issues en GitHub y organizan el tablero Kanban basándose en los nuevos requisitos y el feedback del jueves.
* **Sábado (Mañana):** Reunión general por **Teams**. Se asignan las tareas y el desarrollo comienza oficialmente justo después de la llamada.
* **Sábado (Tarde) - Domingo - Lunes - Martes:** Fase de desarrollo intensivo y resolución de dudas técnicas.
* **Miércoles (18:00): DEADLINE INTERNO.** Todas las tareas deben estar en Pull Request (PR) o mergeadas a `Trunk`.
* **Miércoles (Noche) y Jueves (Mañana):** El **Grupo de Presentación (G4)** consolida el material y prepara la presentación.
* **Jueves (15:30):** Entrega y Presentación en clase.

---

## 2. Estructura de Canales (Comunidad WhatsApp)
Para minimizar el ruido en un equipo de 20 personas, se establece la siguiente jerarquía:

| Canal | Audiencia | Propósito |
| :--- | :--- | :--- |
| **Subgrupos (S, Z, J, G4)** | Miembros + 4 SM | **Primera línea de defensa.** Consultas técnicas y bloqueos de tareas del grupo. |
| **Grupo General** | Todo el equipo | Avisos de impacto global (ej: servidor caído, errores críticos en `Trunk`). |
| **Grupo Avisos** | Todo el equipo | **Solo lectura.** Comunicaciones oficiales y urgentes de los SM. |
| **Administración** | Solo 4 SM | Coordinación de gestión, reasignación de tareas y decisiones estratégicas. |

---
## 3. Roles y Colaboración Total
Aunque el equipo se divide en subgrupos (S, Z, J, G4), seguimos una filosofía de **"todos hacen de todo"**:
* **Multidisciplinar:** Los miembros del grupo de presentación (G4) también programan y tocan código.
* **Apoyo Cruzado:** Cualquier miembro de un subgrupo puede (y debe) echar una mano a otros grupos si están bloqueados o si la carga de trabajo lo requiere, especialmente cuando se acerca el deadline del miércoles.

---

## 4. Gestión de Bloqueos (Escalada de Incidencias)
Si un desarrollador no puede avanzar en su tarea (Bloqueo/Blocker), debe seguir estos tiempos:

1.  **Auto-resolución (30 min):** Investigación en logs, documentación o StackOverflow.
2.  **Consulta al Subgrupo (1-3 horas):** Preguntar por el WhatsApp del subequipo. Compañeros y SM asignados intentarán ayudar.
3.  **Escalada a SM (+3 horas):** Si no hay solución, mencionar directamente a los Scrum Masters en el subgrupo.
4.  **Registro en GitHub:** Mover la Issue a la columna **"Blocked"** y añadir un comentario detallando el error técnico. Esto es vital para la revisión de los jueves.

---

## 5. Flujo de Trabajo y Pull Requests (PR)
Para mantener la integridad del código:

* **Regla de Oro:** Prohibido trabajar directamente sobre `Trunk`. Siempre rama por Issue (`feature/ID-desc`).
* **Sincronización:** Antes de reportar un error o subir una PR, es obligatorio hacer `git pull trunk` para resolver conflictos en local.
* **Revisión de PR:** Los revisores/SM deben dar feedback en un máximo de **24 horas** para evitar cuellos de botella antes del miércoles.
* **Merge:** El flujo es `Branch` ➡️ `Trunk`. El paso a `Main` se realiza cada 2 semanas previo al Sprint.

---

## 6. Compromiso con la Presentación (Grupo 4)
El éxito de la presentación depende de la estabilidad del Miércoles:

* **Disponibilidad:** El miércoles tarde/noche, los responsables de tareas deben estar atentos al WhatsApp para resolver dudas del Grupo 4 sobre el funcionamiento de las features.
* **Hotfixes:** Si el Grupo 4 detecta un bug crítico durante la preparación de la presentación, se comunicará de forma **urgente** al subgrupo responsable para un fix inmediato.
* **Transparencia:** Si una Issue no va a estar lista el miércoles a las 18:00, el responsable debe avisar con antelación a su subgrupo para que el Grupo 4 ajuste el contenido de la presentación.

---

## 7. Responsabilidades de los Scrum Masters
* **Supervisión:** Monitorizar que nadie esté bloqueado más de 4 horas sin recibir respuesta.
* **Mediación:** Resolver conflictos de merge complejos que el subgrupo no pueda manejar.
* **Control de Calidad:** Asegurar que los comentarios en las Issues bloqueadas sean claros para su revisión en la reunión post-clase.

## 8. Plan de Mejora de Fluidez y Eficacia de la Comunicación

En respuesta al feedback de los grupos de interés, este plan busca garantizar que la información fluya sin interrupciones y que existan métricas claras para evaluar nuestra coordinación.

### 8.1. Estrategias de Mejora de Fluidez
* **Sincronización Inter-Grupos:** Los **4 Scrum Masters** realizarán un resumen de los avances de sus respectivos subgrupos en el **Grupo General** cada martes a las 20:00. Esto garantiza una visión global antes del cierre del miércoles.
* **Compromiso de Disponibilidad y Entrega:** Una vez asignadas las Issues el **Sábado a mediodía**, cada miembro debe comunicar en su subgrupo de WhatsApp el plazo estimado en el que prevé terminar su tarea. Esto permite detectar cuellos de botella desde el inicio del ciclo.
* **Registro de Decisiones en GitHub:** Cualquier decisión técnica tomada por WhatsApp que afecte al desarrollo debe quedar reflejada como comentario en la Issue correspondiente. La "verdad oficial" del proyecto reside en GitHub.

### 8.2. Umbrales de Medición de Efectividad (KPIs)
Para medir si la comunicación es eficaz, evaluaremos semanalmente los siguientes indicadores:

| Indicador (Métrica) | Umbral de Éxito | Método de Medición |
| :--- | :--- | :--- |
| **Compromiso de Entrega** | 100% de los miembros | El sábado tras la reunión, todos deben haber dado una fecha estimada en su subgrupo. |
| **Consistencia GitHub/Clockify** | 100% | Todas las horas de Clockify deben coincidir con una Issue activa y con descripción clara. |
| **Resolución de Bloqueos** | < 24 horas | Tiempo desde que una Issue se marca como "Blocked" hasta que se registra una solución o avance. |
| **Alineación de Presentación** | 0 incidencias críticas | El Grupo 4 confirma los jueves que no ha habido falta de información para la demo. |

### 8.3. Acciones Correctoras
Si los umbrales de éxito bajan del 80% en cualquiera de los puntos anteriores (salvo los de cumplimiento 100% obligatorio):
1. Los Scrum Masters analizarán si el problema es de un subgrupo o una falta de herramientas.
2. Se realizará una "Retrospectiva de Emergencia" el sábado para ajustar el flujo y evitar que el problema se repita en el siguiente ciclo.