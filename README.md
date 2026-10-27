# LEXO — Linguistic Behavior Training App

LEXO is an evidence-based Android application that trains adult Spanish-speaking professionals to achieve **unconscious competence** (competencia inconsciente: natural, automatic, error-free production without conscious translation) in English across the **A1, A2, B1, and B2** CEFR spectrum.

Instead of teaching abstract grammar topics or passive multiple-choice quizzes, LEXO collects evidence from the learner's own written production, models behavior stability across professional contexts and time, and deterministically schedules what to practice next.

---

## Core Loop
```
TASK → PRODUCTION → EVALUATION → EVIDENCE → STATE → PRIORITIZATION → SCHEDULING → NEXT TASK
```

---

## Complete A1–B2 Curriculum Matrix (32 KUs, 192 Exercises, 33 Error Signatures)

### Level A1: Syntactic Bases & Essential Morphology
1. `ku_dummy_subject_it`: Explicit subject pronoun (“it is important”, eliminating subject omission *“is important”*).
2. `ku_tps_s`: Third-person singular `-s/-es` in affirmative present simple (*she manages*).
3. `ku_present_aux_do_does`: Present auxiliary `don't / doesn't + base verb` (*he doesn't work*, eliminating *“doesn't works”*).
4. `ku_indefinite_articles_professions`: Indefinite article `a / an` before professions (*I am a manager*, eliminating *“I am manager”*).
5. `ku_prep_in_on_at`: High-frequency prepositions of time (*at 9:00, on Monday, in November*).

### Level A2: Temporal Expansion & Primary Modals
6. `ku_past_simple_regular_irregular`: High-frequency past simple affirmative forms (*went, had, saw, met, wrote*).
7. `ku_past_aux_did_base`: Past auxiliary `didn't + base verb` (eliminating double past marking *“didn't went”*).
8. `ku_future_will_vs_going_to`: `be going to` for established plans vs `will` for spontaneous decisions.
9. `ku_modals_can_could_bare`: Modals `can / could + bare infinitive` (eliminating *“I can to send”*).
10. `ku_comparatives_er_vs_more`: Comparative adjectives: `-er than` vs `more ... than` (eliminating *“more fast”*).

### Level B1: Syntactic Complexity, Compound Tenses & Patterns
11. `ku_present_perf_vs_past_simple`: Past Simple with finished time markers (*yesterday, ago, in 2021*) vs Present Perfect.
12. `ku_since_for`: "for" + duration vs "since" + starting point (eliminating *“since 3 years”*).
13. `ku_wordfamily_manage`: Correct word form in the manage family (*manage, management, manager, manageable*).
14. `ku_agree_verb`: “agree” as a direct verb (eliminating *“I am agree”*).
15. `ku_borrow_vs_lend`: Direction of "borrow (from)" [take] vs "lend (to)" [give].
16. `ku_say_vs_tell`: "tell + person" vs "say (to person)" (eliminating *“he said me”*).
17. `ku_responsible_for`: “responsible for” (eliminating *“responsible of”*).
18. `ku_depend_on`: “depend on/upon” (eliminating *“depend of”*).
19. `ku_discuss_no_about`: Direct transitive “discuss” without "about" (eliminating *“discuss about”*).
20. `ku_listen_to`: Preposition “listen to” (eliminating *“listen the client”*).
21. `ku_verb_patterns_gerund_inf`: High-frequency verb patterns (*avoid taking, decide to launch, enjoy working*).
22. `ku_first_conditional_if_present`: Present tense in real future `if`-clauses (*If he approves, we will start*).

### Level B2: Nuances, Subordination & Advanced Structures
23. `ku_second_conditional_past`: Past simple in hypothetical `if`-clauses (*If we had more budget, we would expand*).
24. `ku_third_conditional_past_hypo`: Third conditional counterfactuals (*if we had known, we would have prevented*).
25. `ku_used_to_vs_be_used_to`: "be used to + -ing" (accustomed) vs "used to + base verb" (past habit).
26. `ku_causative_make_let_bare_inf`: `make` / `let` + object + bare infinitive (no "to", no "-ing").
27. `ku_look_forward_to_ing`: "look forward to + -ing" in professional correspondence.
28. `ku_embedded_question_order`: Statement word order in indirect/embedded questions (*where the room is*, not *where is the room*).
29. `ku_present_perf_continuous`: Present Perfect Continuous for ongoing duration (*have been investigating for three hours*).
30. `ku_modal_deduction_past_pres`: Modales of deduction (*must be / must have been*, eliminating *“must was”*).
31. `ku_connectors_despite_vs_although`: Concession connectors (*despite + noun/-ing* vs *although + clause*).
32. `ku_passive_continuous_perfect`: Passive voice in continuous and perfect tenses (*is being updated, has been resolved*).

---

## Content Distribution & Contexts
Every KU contains 6 professional exercises (192 total) distributed across:
- **Business**, **Meetings**, **Leadership**, **Negotiation**, **Technology**, **Presentations**, **ProjectManagement**, **Travel**.
- Both **constrained** (fill in the blank) and **constructed** (translation, reformulation, open sentence completion) elicitation types.
- Strict CEFR level tagging (`A1`, `A2`, `B1`, `B2`) with in-app level filtering on the Patterns screen.

---

## App Screens & Features
- **Home**: Personalized focus list, transparent session rationale, and primary CTA.
- **Session**: Progressive prompt flow, collapsible "Why this task?" pedagogical rationale, adult scenarios, wrong-span highlighting in amber, "Try it corrected" rewrite, and "I think mine is fine" dispute.
- **Patterns & Pattern Detail**: Categorized pattern states (Focus, Recovering, Consolidated, Not yet explored) with **A1, A2, B1, B2 level filter chips** and student sentence timeline.
- **Settings**: Feedback language toggle (Spanish/English), Reset Demo history, and Data management.
- **Dev Diagnostics**: Real-time attempt inspector, Replay engine, Golden Set runner (24 test cases), Config viewer, and in-app **KU Manager & Validator**.

---

## Testing
Run the complete unit & Robolectric test suite:
```bash
gradle :app:testDebugUnitTest
```
All 14 tests pass deterministically across all 32 KUs and 192 exercises.
