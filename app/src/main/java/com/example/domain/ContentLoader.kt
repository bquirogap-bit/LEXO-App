package com.example.domain

import org.json.JSONArray
import org.json.JSONObject

object ContentLoader {

    fun parseContentPack(jsonString: String): ContentPack {
        val root = JSONObject(jsonString)
        val packId = root.getString("pack_id")
        val version = root.getString("version")

        val kus = mutableListOf<KnowledgeUnit>()
        val kuArray = root.getJSONArray("knowledge_units")
        for (i in 0 until kuArray.length()) {
            val obj = kuArray.getJSONObject(i)
            val l1Map = mutableMapOf<String, String>()
            if (obj.has("l1_note")) {
                val l1Obj = obj.getJSONObject("l1_note")
                l1Obj.keys().forEach { k -> l1Map[k] = l1Obj.getString(k) }
            }
            val expMap = mutableMapOf<String, String>()
            if (obj.has("explanation")) {
                val expObj = obj.getJSONObject("explanation")
                expObj.keys().forEach { k -> expMap[k] = expObj.getString(k) }
            }
            val outScope = mutableListOf<String>()
            if (obj.has("out_of_scope")) {
                val arr = obj.getJSONArray("out_of_scope")
                for (j in 0 until arr.length()) outScope.add(arr.getString(j))
            }

            kus.add(
                KnowledgeUnit(
                    id = obj.getString("id"),
                    name = obj.getString("name"),
                    kind = obj.getString("kind"),
                    area = obj.getString("area"),
                    band_introduced = obj.getString("band_introduced"),
                    behavior = obj.getString("behavior"),
                    obligatory_context = obj.getString("obligatory_context"),
                    success_criterion = obj.getString("success_criterion"),
                    out_of_scope = outScope,
                    l1_note = l1Map,
                    explanation = expMap
                )
            )
        }

        val sigs = mutableListOf<ErrorSignature>()
        val sigArray = root.getJSONArray("error_signatures")
        for (i in 0 until sigArray.length()) {
            val obj = sigArray.getJSONObject(i)
            sigs.add(
                ErrorSignature(
                    id = obj.getString("id"),
                    ku_id = obj.getString("ku_id"),
                    name = obj.getString("name"),
                    feedback_es = obj.getString("feedback_es"),
                    feedback_en = obj.getString("feedback_en")
                )
            )
        }

        val exercises = mutableListOf<Exercise>()
        val exArray = root.getJSONArray("exercises")
        for (i in 0 until exArray.length()) {
            val obj = exArray.getJSONObject(i)
            val secKus = mutableListOf<String>()
            if (obj.has("secondary_ku_ids")) {
                val arr = obj.getJSONArray("secondary_ku_ids")
                for (j in 0 until arr.length()) secKus.add(arr.getString(j))
            }
            val acceptedList = mutableListOf<String>()
            if (obj.has("accepted")) {
                val arr = obj.getJSONArray("accepted")
                for (j in 0 until arr.length()) acceptedList.add(arr.getString(j))
            }
            val kwList = mutableListOf<KnownWrong>()
            if (obj.has("known_wrong")) {
                val arr = obj.getJSONArray("known_wrong")
                for (j in 0 until arr.length()) {
                    val kwObj = arr.getJSONObject(j)
                    kwList.add(KnownWrong(text = kwObj.getString("text"), sig = kwObj.getString("sig")))
                }
            }
            val typoList = mutableListOf<KnownTypo>()
            if (obj.has("known_typos")) {
                val arr = obj.getJSONArray("known_typos")
                for (j in 0 until arr.length()) {
                    val typoObj = arr.getJSONObject(j)
                    typoList.add(KnownTypo(typo = typoObj.getString("typo"), fix = typoObj.getString("fix")))
                }
            }

            exercises.add(
                Exercise(
                    id = obj.getString("id"),
                    ku = obj.getString("ku"),
                    secondary_ku_ids = secKus,
                    type = obj.getString("type"),
                    elicitation = obj.getString("elicitation"),
                    context = obj.getString("context"),
                    difficulty = obj.optInt("difficulty", 1),
                    facet = if (obj.isNull("facet")) null else obj.getString("facet"),
                    situation = obj.getString("situation"),
                    prompt = obj.getString("prompt"),
                    prompt_lang = obj.optString("prompt_lang", "en"),
                    eval_mode = obj.optString("eval_mode", "deterministic_first"),
                    accepted = acceptedList,
                    known_wrong = kwList,
                    known_typos = typoList,
                    notes_for_evaluator = if (obj.isNull("notes_for_evaluator")) null else obj.getString("notes_for_evaluator")
                )
            )
        }

        return ContentPack(packId, version, kus, sigs, exercises)
    }

    fun parseGoldenCases(jsonString: String): List<GoldenCase> {
        val root = JSONObject(jsonString)
        val cases = mutableListOf<GoldenCase>()
        val arr = root.getJSONArray("cases")
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            cases.add(
                GoldenCase(
                    id = obj.getString("id"),
                    exercise_id = obj.getString("exercise_id"),
                    ku_id = obj.getString("ku_id"),
                    response = obj.getString("response"),
                    expected_outcome = obj.getString("expected_outcome"),
                    expected_opportunity = obj.getString("expected_opportunity"),
                    expected_signature = if (obj.isNull("expected_signature")) null else obj.getString("expected_signature"),
                    expected_counts = obj.optBoolean("expected_counts", true),
                    notes = if (obj.isNull("notes")) null else obj.getString("notes"),
                    needs_human_review = obj.optBoolean("needs_human_review", true)
                )
            )
        }
        return cases
    }

    fun parseDemoEvents(jsonString: String): List<EvidenceEvent> {
        val root = JSONObject(jsonString)
        val events = mutableListOf<EvidenceEvent>()
        val arr = root.getJSONArray("events")
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            events.add(
                EvidenceEvent(
                    id = obj.getString("id"),
                    attemptId = obj.getString("attemptId"),
                    kuId = obj.getString("kuId"),
                    role = obj.getString("role"),
                    opportunity = obj.getString("opportunity"),
                    outcome = obj.getString("outcome"),
                    errorKind = if (obj.isNull("errorKind")) null else obj.getString("errorKind"),
                    signatureId = if (obj.isNull("signatureId")) null else obj.getString("signatureId"),
                    quote = if (obj.isNull("quote")) null else obj.getString("quote"),
                    confidence = obj.getDouble("confidence"),
                    weight = obj.getDouble("weight"),
                    counts = obj.getBoolean("counts"),
                    source = obj.getString("source"),
                    context = obj.getString("context"),
                    exerciseId = obj.getString("exerciseId"),
                    facet = if (obj.isNull("facet")) null else obj.getString("facet"),
                    sessionId = obj.getString("sessionId"),
                    elicitation = obj.getString("elicitation"),
                    modality = obj.optString("modality", "typed"),
                    latencyMs = obj.getLong("latencyMs"),
                    hintsUsed = obj.getInt("hintsUsed"),
                    at = obj.getLong("at"),
                    status = obj.optString("status", "active"),
                    configVersion = obj.optString("configVersion", "cfg-1")
                )
            )
        }
        return events
    }
}
