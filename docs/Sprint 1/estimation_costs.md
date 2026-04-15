# Time and Cost Estimation Report — ISPP-G8 StreetAsk

**Date:** 02/03/2026 | **Project:** ISPP-G8 | **Duration:** 11 weeks  
**Team:** 20 people (16 Dev @€23.85/h + 4 PM @€39.48/h)

---

## HOURLY RATE METHODOLOGY

**Rates applied:**
- **Developers:** €23.85/h (full TCE included) (Based on the "Programador/a J2EE / Web - Junior" profile ).
- **Project Managers:** €39.48/h (full TCE included) (Based on the "Jefe/a de proyecto / Coordinador - Junior" profile ).

**Source & Justification:**
These rates are based on the "Preliminary Market Consultation" for "Professional IT Profiles" published by the Junta de Andalucía (Consejería de Agricultura, Pesca y Desarrollo Rural). They include:
- Official benchmark: The rates represent the "MEDIA ACOTADA TODOS" (Trimmed Mean), which is the specific reference value CAPDER uses to determine tender amounts for future software contracts.
- Statistical reliability: This value is calculated like a traditional arithmetic mean but excludes extreme outliers from the data set to ensure market accuracy.

**Market comparison:**
- Junior Development (J2EE/Web): €23.85/h (Trimmed Mean).
- Junior Project Management: €39.48/h (Trimmed Mean).
-Other junior benchmarks: Rates in the same category range from €18.57/h for user support to €38.83/h for specialized Business Intelligence consultants

**Reference:** Fully aligned with the official market database established by the Servicio de Informática to ensure that tender estimates are within realistic market ranges and serves as a verified baseline for this project

---
## PROJECT BUDGET AT A GLANCE

### Development Costs (Sprint 0 → Sprint 3)

| Phase | Development Cost | Deployment Cost (Azure) | Subtotal |
|---|---:|---:|---:|
| **Sprint 0** (Completed) | €11,726.60 | €17.50 | €11,744.10 |
| **Sprint 1** (Completed) | €10,910.04 | €17.50 | €10,927.54 |
| **Sprint 2** (Completed) | €16,365.08 | €35.00 | €16,400.08 |
| **Sprint 3** (Estimated) | €16,365.08 | €52.50 | €16,417.58 |
| **TOTAL** | **€55,366.80** | **€122.50** | **€55,489.30** |

---

## PHYSICAL INFRASTRUCTURE & SOFTWARE COSTS

### Hardware (Development Workstations)

| Concept | Units | Unit Cost | Total Cost |
|---|---:|---:|---:|
| Development laptops | 20 | €1,000 | €20,000 |

**Assumptions:**
- Mid-range development laptop (16GB RAM, SSD)
- Suitable for Java + React Native development

👉 **Total hardware cost (CAPEX): €20,000**

### Mobile Testing Devices

| Concept | Units | Unit Cost | Total |
|---|---:|---:|---:|
| Test smartphones | 4 | €300 | €1,200 |

👉 **Total testing devices cost (CAPEX): €1,200**

### Amortization of hardware

This represents the actual cost of equipment usage and depreciation during the 11 weeks of the project duration:
| Concept  | Value |
| Total Hardware Value (CAPEX) | €21,200.00 | 
|Annual Amortization Rate (AEAT - 26%)|€5,512.00 / year|
|Amortization for 11 weeks (Project charge)|€1,166.00|

According to the updated AEAT 2026 tables, "Equipment for information processing, and computer systems and programs" (Group 5) is subject to a maximum linear coefficient of 26%. This is used to ensure the budget remains realistic for a professional 11-week project lifecycle

---

### Software & Collaboration Tools

Calculated for 11 weeks(approx. 2.75 months):

| Tool | Plan | Cost | Justification |
|---|---|---:|---|
| GitHub | Team ($4/user/mo) | €220.00 | Repository, CI/CD, project management |
| Microsoft Teams | Free | €0 | Communication |
| Azure | Student credits | ~€258.00 | Est. value of 3 student accounts (~$86 each) |
| Expo (EAS) | Free tier | €0 | Mobile deployment |
| OpenStreetMap + Leaflet | Free | €0 | Maps |

**Note:** note that these costs are not actually paid in this specific case—as they are covered by university-provided academic plans—they are intentionally included in this report to simulate a real-world professional project and ensure the budget reflects a standard operational environment.

## 📋 OPENSTREETMAP & LEAFLET POLICY

**Completely free for monetized apps**
- No licensing fees or revenue commissions
- No restrictions on premium subscriptions or business models
- Only requirement: Attribution ("© OpenStreetMap contributors")
- For massive scale (>1M tile requests/month): Consider self-hosting or Mapbox

👉 **Total software licensing cost: €478.00**

---

## TOTAL INFRASTRUCTURE COST

| Category | Cost |
|---|---:|
| Azure Infrastructure | €122.50 |
| Hardware amortization | €1,166.00 |
| Software Licenses | €478.00 |
| **TOTAL INFRASTRUCTURE COST** | **€1,766.50** |

---

##  TOTAL PROJECT COST

| Category | Cost |
|---|---:|
| Labor (Dev + PM) | €55,489.30 |
| Infrastructure | €1,766.50 |
| ** TOTAL PROJECT COST** | **€57,255.80** |

---

## COST CLASSIFICATION (CAPEX vs OPEX vs TCE)

### CAPEX (Capital Expenditure)
One-time investments in assets required to start the project:

- Development laptops → €20,000  
- Mobile testing devices → €1,200  
- Initial cloud setup (Azure) → €122.50  

👉 **Total CAPEX: €21,322.50**

---

### OPEX (Operational Expenditure)
The actual cost required to operate the project during its 11-week lifecycle. This includes labor and the proportional depreciation of assets:

- Team Labor (Dev + PM) → €55,366.80
- Hardware Amortization (Depreciation) → €1,166.00
- Software & Cloud Usage (GitHub + Azure Fees) → €723.00

 **Total OPEX (project phase): €57,255.80**

---

### TCE (Total Cost of Employment)

These rates are based on the Junta de Andalucía market benchmarks and represent the full cost to the employer, including social security, benefits, and structural costs:
- Developers (Junior): €23.85/h
- Project Managers (Junior): €39.48/h

(Includes: Gross salary + Employer social contributions (~35%) + Indirect costs).
---


##  COST BREAKDOWN
To ensure project viability against unforeseen risks, such as technical debt, API changes in Azure, or development delays, a 10% contingency reserve has been applied to the operational base.
| Role | Hours | Cost | % |
|---|---:|---:|---:|
| Developers | 1,560 | €37,206.00 | 59.1% |
| PMs | 460 | €18,160.80 | 28.8% |
| Contingency reserve | - | €5,725.58 | 9.1% |
| Infrastructure & Deployment (OPEX) | — | €1,889.00 | 3.0% |
| **TOTAL** | — | **€62.981.38** | **100%** |

---

## 🎯 SPRINT SUMMARY

### Sprint 0 — Foundations [COMPLETED ✅]
**Dates:** Feb 5–20 (3 weeks) | **Cost:** €11,744.10
**Delivered:** Data model, tech stack, CI/CD, business plan, user stories, mockups  
**Work:** 10 Foundation User Stories + cross-cutting activities

---

### Sprint 1 — Core Q&A [COMPLETED ✅]
**Dates:** Feb 21–Mar 5 (2 weeks) | **Cost:** €10,927.54
**Delivered:** Registration, login, map, geolocated Q&A, answer threads, authentication  
**Key US:** US-01, US-03, US-08, US-11, US-09, US-13 + infrastructure

---

### Sprint 2 — Social Interaction [COMPLETED ✅]
**Dates:** Mar 6–26 (3 weeks) | **Cost:** €16,400.08
**Scope:** User profiles, ratings (like/dislike), push notifications  
**Key US:** US-06, US-10, US-12, US-04

**Breakdown:**
- User profile system (view stats, activity, editing) — 65h dev  
- Rating/reputation engine (like/dislike, trust scores) — 80h dev + 11h PM  
- Push notification service (triggers, scheduling, delivery) — 55h dev + 7h PM  
- Cross-cutting (planning, QA, documentation) — 165h dev + 78h PM  

**Infrastructure:** 2 Azure VMs (prod + staging)

---

### Sprint 3 — Events & Business [ESTIMATED]
**Dates:** Mar 27–Apr 16 (3 weeks) | **Est. Cost:** €16,417.58
**Scope:** Events, business accounts, gamification, admin panel  
**Key US:** US-15/16/17 (event map), US-28 (business reg), US-29-32 (event CRUD), US-35 (coins), US-37/39 (admin)

**Breakdown:**
- Event visualization & management (map, details, toggles, attendance) — 65h dev  
- Business account system (registration, verification, Tax ID) — 22h dev + 4h PM  
- Event CRUD operations (creation, editing, deletion) — 35h dev + 5h PM  
- Gamification engine (coin logic, reward distribution) — 30h dev + 4h PM  
- Admin panel (metrics, approvals, user management) — 32h dev + 5h PM  
- Cross-cutting (planning, QA, documentation) — 163h dev + 68h PM  

**Infrastructure:** 3 Azure VMs (prod + staging + failover for stability)

---

## 💡 MONETIZATION & BREAK-EVEN

**Revenue Model:** Ads + Freemium (€2.99/mo) + B2B events (€9.99/mo)

**Assumptions (based on MVP data at 100 users):**
- Ads: €0.60/user/month
- Premium + Business combined: €0.48/user/month
- Total ARPU ≈ €1.08/user/month

---

| Users | Monthly Revenue | Break-even |
|---:|---:|---|
| 500 | €541.95 | ~153 months |
| 2,500 | €2,709.75 | ~30.7 months |
| **10,000** | **€10,839** | **7.6 months** ✅ |
| 50,000 | €54,195 | 1.5 month |

---

**ROI target:** 100% recovery of the total operational investment (€62,981.38) within ~6 months at 10k+ MAU is achievable under 2026 market conditions.

## ⚠️ RISKS & CONTINGENCY

| Risk | Likelihood | Buffer |
|---|---|---:|
| Development delays | Medium | €3,500 |
| Infrastructure scaling | Medium | €500 |
| Additional hires | Medium | €1,725.58 |
| **Total contingency (~10%)** | — | **€5,725.58** |

---

## 📌 FINAL INTERPRETATION

Two valid cost perspectives exist:

### Full Investment Scenario (Real Startup)
- Total cost: **€84,181.38**
- Includes: Total OPEX (Labor + Amortization + Contingency) + Full Hardware/Asset Acquisition (CAPEX).

### Operational Scenario (Academic Context)
- Total cost: **€62,981.38**
- Includes:Labor, amortized infrastructure usage, software licenses, and risk buffer.

---

## 📍 BUDGET STATUS (labor)

| Metric | Value |
|---|---:|
| Completed (Sprint 0–2) | €39,071.72 (70.4%) |
| Remaining (Sprint 3) | €16,417.58 (29.6%) |
| Timeline | ~7/11 weeks (~64%) |
| Status | On track |
