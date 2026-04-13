# Communication and Incident Management Protocol (ISPP-G10)

This document defines the official protocol for managing blockers, internal communication, and the project delivery cycle for **StreetAsk**. The goal is to ensure that development momentum does not stop and that **Group 4** has stable material for Thursday presentations.

## 1. Weekly Work Cycle ("StreetAsk Cycle")
Our development week is structured from Thursday to Wednesday to align with class feedback and deliverables:

* **Thursday (19:30+):** After class, the **4 Scrum Masters (SM)** meet to review the feedback received during the presentation and plan the strategy for the following week.
* **Friday:** The SMs create Issues in GitHub and organize the Kanban board based on the new requirements and Thursday feedback.
* **Saturday (Morning):** General meeting on **Teams**. Tasks are assigned and development officially begins right after the call.
* **Saturday (Afternoon) - Sunday - Monday - Tuesday:** Intensive development phase and technical issue resolution.
* **Wednesday (18:00): INTERNAL DEADLINE.** All tasks must be in Pull Request (PR) or merged into `Trunk`.
* **Wednesday (Night) and Thursday (Morning):** The **Presentation Group (G4)** consolidates the material and prepares the presentation.
* **Thursday (15:30):** Delivery and presentation in class.

---

## 2. Channel Structure (WhatsApp Community)
To minimize noise in a 20-person team, the following hierarchy is established:

| Channel | Audience | Purpose |
| :--- | :--- | :--- |
| **Subgroups (S, Z, J, G4)** | Members + 4 SM | **First line of defense.** Technical questions and task blockers of the group. |
| **General Group** | Entire team | Notices with global impact (e.g., server down, critical errors in `Trunk`). |
| **Announcements Group** | Entire team | **Read-only.** Official and urgent communications from the SMs. |
| **Administration** | Only 4 SM | Management coordination, task reassignment, and strategic decisions. |

---

## 3. Roles and Full Collaboration
Although the team is divided into subgroups (S, Z, J, G4), we follow a philosophy of **"everyone does everything"**:
* **Multidisciplinary:** Members of the presentation group (G4) also code and contribute technically.
* **Cross Support:** Any member of a subgroup can (and should) help other groups if they are blocked or if the workload requires it, especially as the Wednesday deadline approaches.

---

## 4. Blocker Management (Incident Escalation)
If a developer cannot advance on their task (Blocker), they must follow these timeframes:

1. **Self-resolution (30 min):** Investigate logs, documentation, or StackOverflow.
2. **Subgroup consultation (1-3 hours):** Ask in the subgroup WhatsApp. Teammates and assigned SMs will try to help.
3. **Escalation to SM (+3 hours):** If there is no solution, mention the Scrum Masters directly in the subgroup.
4. **Record on GitHub:** Move the Issue to the **"Blocked"** column and add a detailed comment describing the technical issue. This is vital for Thursday review.

---

## 5. Workflow and Pull Requests (PR)
To maintain code integrity:

* **Golden rule:** Do not work directly on `Trunk`. Always branch for each Issue (`feature/ID-desc`).
* **Sync:** Before reporting a bug or opening a PR, it is mandatory to run `git pull trunk` and resolve conflicts locally.
* **PR review:** Reviewers/SMs must provide feedback within **24 hours** to avoid bottlenecks before Wednesday.
* **Merge:** The flow is `Branch` ➡️ `Trunk`. Promotion to `Main` occurs every 2 weeks before the Sprint.

---

## 6. Commitment to the Presentation (Group 4)
The success of the presentation depends on Wednesday’s stability:

* **Availability:** On Wednesday afternoon/evening, task owners must stay alert on WhatsApp to resolve Group 4 questions about feature behavior.
* **Hotfixes:** If Group 4 detects a critical bug while preparing the presentation, they must urgently notify the responsible subgroup for an immediate fix.
* **Transparency:** If an Issue will not be ready by Wednesday at 18:00, the owner must warn their subgroup in advance so Group 4 can adjust the presentation content.

---

## 7. Responsibilities of the Scrum Masters
* **Monitoring:** Ensure no one remains blocked for more than 4 hours without receiving a response.
* **Mediation:** Resolve complex merge conflicts that the subgroup cannot handle.
* **Quality Control:** Ensure comments on blocked Issues are clear for review in the post-class meeting.

## 8. Plan to Improve Communication Flow and Effectiveness

In response to stakeholder feedback, this plan aims to ensure information flows without interruption and that there are clear metrics to evaluate our coordination.

### 8.1. Strategies to Improve Flow
* **Inter-group synchronization:** The **4 Scrum Masters** will post a summary of the progress of their respective subgroups in the **General Group** every Tuesday at 20:00. This ensures a global view before Wednesday’s closure.
* **Commitment to availability and delivery:** Once Issues are assigned on **Saturday at noon**, each member must communicate in their subgroup WhatsApp the estimated deadline for completing their task. This allows bottlenecks to be detected from the start of the cycle.
* **Record decisions on GitHub:** Any technical decision made on WhatsApp that affects development must be recorded as a comment on the corresponding Issue. The project’s official source of truth is GitHub.

### 8.2. Effectiveness Measurement Thresholds (KPIs)
To measure whether communication is effective, we will evaluate the following indicators weekly:

| Indicator (Metric) | Success Threshold | Measurement Method |
| :--- | :--- | :--- |
| **Delivery Commitment** | 100% of members | After the Saturday meeting, everyone must have provided an estimated completion date in their subgroup. |
| **GitHub/Clockify Consistency** | 100% | All Clockify hours must match an active Issue with a clear description. |
| **Blocker Resolution** | < 24 hours | Time from when an Issue is marked "Blocked" until a solution or progress is recorded. |
| **Presentation Alignment** | 0 critical incidents | Group 4 confirms on Thursdays that there was no missing information for the demo. |

### 8.3. Corrective Actions
If success thresholds fall below 80% in any of the above points (except those with mandatory 100% compliance):
1. The Scrum Masters will analyze whether the problem is subgroup-related or caused by a lack of tools.
2. An "Emergency Retrospective" will be held on Saturday to adjust the flow and prevent the issue from repeating in the next cycle.