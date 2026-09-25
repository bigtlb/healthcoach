# Food Journaling (Phase 1 & Future Expansion)

### Status: In Discovery (Detail TBD)

---

## 1. Overview & Objectives

Dietary tracking is a fundamental pillar of personal health and weight management alongside body weight, blood pressure, and activity tracking. This initiative introduces food journaling into HealthCoach to allow users to log their daily nutritional intake with minimal friction and maximum data privacy.

---

## 2. Phase 1: Core Daily Food Logging

*Detail: TBD (Initial Design & Prototyping)*

### Key Capabilities:
* **Daily Consumption Capture**:
  * Record foods and beverages consumed throughout each day.
  * Log estimated or measured caloric intake (kcal) for each entry.
  * Categorize entries into standard meal slots: **Breakfast**, **Lunch**, **Dinner**, and **Snacks**.
* **Search & Past Entry Reuse**:
  * Search previous entries to quickly re-add frequent meals and items without re-typing descriptions or caloric values.
  * Recent and favorite item shortcuts for rapid single-click logging.
* **Daily Aggregates & Insights**:
  * Real-time calculation of total daily caloric intake against user-configured targets.
  * Historical timeline view showing calorie consumption trends overlaid with weight progression charts.

---

## 3. Longer-Tail Follow-Up Phases

Beyond initial manual logging, subsequent phases will automate input, enrich nutritional data, and support community-driven databases:

```
┌─────────────────────────────────────────────────────────┐
│                    Evolution Roadmap                    │
├───────────────────┬─────────────────────────────────────┤
│ Phase 1           │ Core Daily Logging & Entry Reuse    │
│ (Current Target)  │ (Manual entry, quick search/repeat) │
├───────────────────┼─────────────────────────────────────┤
│ Phase 2           │ Standard Nutritional DB Import      │
│                   │ (USDA FoodData Central / offline db)│
├───────────────────┼─────────────────────────────────────┤
│ Phase 3           │ Barcode Scanning                    │
│                   │ (Mobile camera scanning & lookup)   │
├───────────────────┼─────────────────────────────────────┤
│ Phase 4           │ Crowdsourced Nutrition & Search     │
│                   │ (Community contributions & sync)    │
└───────────────────┴─────────────────────────────────────┘
```

### Phase 2: Standard Food Database Integration
* Embed or support offline imports of standard open nutritional datasets (e.g., USDA FoodData Central / Food and Nutrient Database).
* Auto-complete foods with standardized serving sizes and comprehensive macronutrient profiles (protein, carbohydrates, fats, fiber, sodium).

### Phase 3: Barcode Scanning (Mobile & Camera Integration)
* Use device camera on Android (and supported desktop webcams) to scan product UPC/EAN barcodes.
* Instant nutritional lookup using open databases (e.g., Open Food Facts API / local barcode index).
* Auto-populate serving sizes and calorie counts directly from packaged food labels.

### Phase 4: Community Data, Search & Contributions
* Search expanded community nutritional repositories.
* Allow users to create, update, and contribute missing or corrected food items back to shared community datasets.
* Seamlessly sync custom food items across user devices via HealthCoach's remote sync infrastructure.
