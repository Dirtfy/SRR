package com.dirtfy.srr.remote

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.dirtfy.srr.core.model.Evaluation
import com.dirtfy.srr.core.scoring.DefaultFeatureScoringEngine
import com.dirtfy.srr.core.usecase.LoadFeatureScoresUseCase
import com.dirtfy.srr.remote.repository.RemoteEvaluationRepository
import com.dirtfy.srr.remote.repository.RemoteFeatureRepository
import com.dirtfy.srr.remote.repository.RemoteItemRepository
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import java.net.HttpURLConnection
import java.net.URL

/**
 * End-to-end integration tests that simulate the real multi-user workflow:
 *
 *   User A (admin) creates items + features.
 *   User B creates additional items.
 *   Users A, B, C submit evaluations for the features.
 *   LoadFeatureScoresUseCase is run and its Output is verified.
 *
 * This exercises the full read path that both ViewModels ultimately call.
 *
 * Prerequisite: Firebase emulators running (auth:9099, firestore:8080).
 * Physical device: adb reverse tcp:8080 tcp:8080 && adb reverse tcp:9099 tcp:9099
 */
@RunWith(AndroidJUnit4::class)
class MultiUserFlowTest {

    companion object {
        @BeforeClass
        @JvmStatic
        fun setUpEmulators() {
            try { Firebase.auth.useEmulator("localhost", 9099) } catch (_: Exception) {}
            try { Firebase.firestore.useEmulator("localhost", 8080) } catch (_: Exception) {}
        }
    }

    private lateinit var itemRepo:       RemoteItemRepository
    private lateinit var featureRepo:    RemoteFeatureRepository
    private lateinit var evaluationRepo: RemoteEvaluationRepository

    @Before
    fun setUp() {
        itemRepo       = RemoteItemRepository()
        featureRepo    = RemoteFeatureRepository()
        evaluationRepo = RemoteEvaluationRepository()
    }

    @After
    fun tearDown() = runBlocking {
        Firebase.auth.signOut()
        try {
            val projectId = FirebaseApp.getInstance().options.projectId ?: return@runBlocking
            deleteUrl("http://localhost:8080/emulator/v1/projects/$projectId/databases/(default)/documents")
            deleteUrl("http://localhost:9099/emulator/v1/projects/$projectId/accounts")
        } catch (_: Exception) {}
    }

    private fun deleteUrl(urlStr: String) {
        val conn = URL(urlStr).openConnection() as HttpURLConnection
        conn.requestMethod  = "DELETE"
        conn.connectTimeout = 3_000
        conn.connect()
        conn.disconnect()
    }

    // -----------------------------------------------------------------------
    // Helper: create emulator account and return UID
    // -----------------------------------------------------------------------

    private suspend fun signUpAs(email: String): String {
        Firebase.auth.signOut()
        Firebase.auth.createUserWithEmailAndPassword(email, "password123").await()
        return Firebase.auth.currentUser!!.uid
    }

    // -----------------------------------------------------------------------
    // Test 1 — items and features created by multiple users are all visible
    // -----------------------------------------------------------------------

    @Test
    fun multiUser_allItemsAndFeaturesVisible() = runBlocking {
        // User A adds two items and a feature
        val uidA   = signUpAs("flow_a@test.com")
        val phone  = itemRepo.createItem("Phone",       uidA).getOrThrow()
        val laptop = itemRepo.createItem("Laptop",      uidA).getOrThrow()
        val perf   = featureRepo.createFeature("Performance", uidA).getOrThrow()

        // User B adds one more item and a feature
        val uidB   = signUpAs("flow_b@test.com")
        val tablet = itemRepo.createItem("Tablet",      uidB).getOrThrow()
        val stab   = featureRepo.createFeature("Stability",   uidB).getOrThrow()

        // User C reads — must see everything
        signUpAs("flow_c@test.com")
        val items    = itemRepo.getAllItems().getOrThrow()
        val features = featureRepo.getAllFeatures().getOrThrow()

        val itemIds    = items.map { it.id }
        val featureIds = features.map { it.id }

        assertTrue("Phone must be visible to C",       phone.id  in itemIds)
        assertTrue("Laptop must be visible to C",      laptop.id in itemIds)
        assertTrue("Tablet must be visible to C",      tablet.id in itemIds)
        assertTrue("Performance must be visible to C", perf.id   in featureIds)
        assertTrue("Stability must be visible to C",   stab.id   in featureIds)

        // createdBy must be preserved
        assertEquals(uidA, items.find { it.id == phone.id  }!!.createdBy)
        assertEquals(uidA, items.find { it.id == laptop.id }!!.createdBy)
        assertEquals(uidB, items.find { it.id == tablet.id }!!.createdBy)
        assertEquals(uidA, features.find { it.id == perf.id }!!.createdBy)
        assertEquals(uidB, features.find { it.id == stab.id }!!.createdBy)
    }

    // -----------------------------------------------------------------------
    // Test 2 — evaluatorCountByFeature reflects submitted evaluations
    // -----------------------------------------------------------------------

    @Test
    fun evaluatorCount_updatesAfterEachSubmission() = runBlocking {
        val uidA    = signUpAs("evcnt_a@test.com")
        val phone   = itemRepo.createItem("Phone",   uidA).getOrThrow()
        val laptop  = itemRepo.createItem("Laptop",  uidA).getOrThrow()
        val perf    = featureRepo.createFeature("Performance", uidA).getOrThrow()

        // 0 evaluations initially
        val evsEmpty = evaluationRepo.getEvaluationsForFeature(perf.id).getOrThrow()
        assertEquals("No evaluations yet", 0, evsEmpty.size)

        // User A evaluates
        evaluationRepo.submitEvaluation(
            Evaluation(uidA, perf.id, listOf(phone.id, laptop.id))
        ).getOrThrow()
        val evsAfterA = evaluationRepo.getEvaluationsForFeature(perf.id).getOrThrow()
        assertEquals("1 evaluation after A", 1, evsAfterA.size)

        // User B evaluates
        val uidB = signUpAs("evcnt_b@test.com")
        evaluationRepo.submitEvaluation(
            Evaluation(uidB, perf.id, listOf(laptop.id, phone.id))
        ).getOrThrow()
        val evsAfterB = evaluationRepo.getEvaluationsForFeature(perf.id).getOrThrow()
        assertEquals("2 evaluations after B", 2, evsAfterB.size)
    }

    // -----------------------------------------------------------------------
    // Test 3 — scores are null below threshold, non-null once threshold is met
    // -----------------------------------------------------------------------

    @Test
    fun scores_nullBelowThresholdThenAvailableAtThreshold() = runBlocking {
        val uidA    = signUpAs("thresh_a@test.com")
        val phone   = itemRepo.createItem("Phone",   uidA).getOrThrow()
        val laptop  = itemRepo.createItem("Laptop",  uidA).getOrThrow()
        val tablet  = itemRepo.createItem("Tablet",  uidA).getOrThrow()
        val perf    = featureRepo.createFeature("Performance", uidA).getOrThrow()

        val useCase = LoadFeatureScoresUseCase(
            itemRepository       = itemRepo,
            featureRepository    = featureRepo,
            evaluationRepository = evaluationRepo,
            scoringEngine        = DefaultFeatureScoringEngine(),
            minVoteThreshold     = 3
        )

        // 0 evaluations — all scores null
        var output = useCase.execute().getOrThrow()
        assertNull("Score must be null with 0 evals",
            output.scoreMatrix.scores[phone.id]?.get(perf.id))

        // 1 evaluation (still below threshold)
        evaluationRepo.submitEvaluation(
            Evaluation(uidA, perf.id, listOf(phone.id, laptop.id, tablet.id))
        ).getOrThrow()
        output = useCase.execute().getOrThrow()
        assertNull("Score must be null with 1 eval (threshold=3)",
            output.scoreMatrix.scores[phone.id]?.get(perf.id))

        // 2nd evaluation
        val uidB = signUpAs("thresh_b@test.com")
        evaluationRepo.submitEvaluation(
            Evaluation(uidB, perf.id, listOf(phone.id, tablet.id, laptop.id))
        ).getOrThrow()
        output = useCase.execute().getOrThrow()
        assertNull("Score must be null with 2 evals (threshold=3)",
            output.scoreMatrix.scores[phone.id]?.get(perf.id))

        // 3rd evaluation — threshold met, scores must appear
        val uidC = signUpAs("thresh_c@test.com")
        evaluationRepo.submitEvaluation(
            Evaluation(uidC, perf.id, listOf(laptop.id, phone.id, tablet.id))
        ).getOrThrow()
        output = useCase.execute().getOrThrow()
        assertNotNull("Score must be non-null with 3 evals",
            output.scoreMatrix.scores[phone.id]?.get(perf.id))
    }

    // -----------------------------------------------------------------------
    // Test 4 — LoadFeatureScoresUseCase: full output shape is correct
    // -----------------------------------------------------------------------

    @Test
    fun loadFeatureScoresUseCase_fullOutputShape() = runBlocking {
        // Setup: 2 users, 3 items, 2 features
        val uidA    = signUpAs("shape_a@test.com")
        val phone   = itemRepo.createItem("Phone",       uidA).getOrThrow()
        val laptop  = itemRepo.createItem("Laptop",      uidA).getOrThrow()
        val tablet  = itemRepo.createItem("Tablet",      uidA).getOrThrow()
        val perf    = featureRepo.createFeature("Performance", uidA).getOrThrow()
        val stab    = featureRepo.createFeature("Stability",   uidA).getOrThrow()

        val uidB = signUpAs("shape_b@test.com")
        val uidC = signUpAs("shape_c@test.com")

        // 3 users evaluate Performance: Phone > Laptop > Tablet
        for ((uid, email) in listOf(uidA to "shape_a@test.com", uidB to "shape_b@test.com", uidC to "shape_c@test.com")) {
            Firebase.auth.signOut()
            Firebase.auth.signInWithEmailAndPassword(email, "password123").await()
            evaluationRepo.submitEvaluation(
                Evaluation(uid, perf.id, listOf(phone.id, laptop.id, tablet.id))
            ).getOrThrow()
        }

        // Only 2 users evaluate Stability (below threshold, so Stability scores → null)
        Firebase.auth.signOut()
        Firebase.auth.signInWithEmailAndPassword("shape_a@test.com", "password123").await()
        evaluationRepo.submitEvaluation(
            Evaluation(uidA, stab.id, listOf(tablet.id, phone.id, laptop.id))
        ).getOrThrow()
        Firebase.auth.signOut()
        Firebase.auth.signInWithEmailAndPassword("shape_b@test.com", "password123").await()
        evaluationRepo.submitEvaluation(
            Evaluation(uidB, stab.id, listOf(tablet.id, laptop.id, phone.id))
        ).getOrThrow()

        val useCase = LoadFeatureScoresUseCase(
            itemRepository       = itemRepo,
            featureRepository    = featureRepo,
            evaluationRepository = evaluationRepo,
            scoringEngine        = DefaultFeatureScoringEngine(),
            minVoteThreshold     = 3
        )
        val output = useCase.execute().getOrThrow()

        // --- items ---
        assertEquals("3 items returned", 3, output.items.size)
        val returnedItemIds = output.items.map { it.id }
        assertTrue(phone.id  in returnedItemIds)
        assertTrue(laptop.id in returnedItemIds)
        assertTrue(tablet.id in returnedItemIds)

        // --- features ---
        assertEquals("2 features returned", 2, output.features.size)

        // --- evaluationsByFeature ---
        assertEquals("3 evaluations for Performance", 3,
            output.evaluationsByFeature[perf.id]?.size)
        assertEquals("2 evaluations for Stability", 2,
            output.evaluationsByFeature[stab.id]?.size)

        // --- scoreMatrix: Performance has scores (threshold met) ---
        val phonePerf  = output.scoreMatrix.scores[phone.id]?.get(perf.id)
        val laptopPerf = output.scoreMatrix.scores[laptop.id]?.get(perf.id)
        val tabletPerf = output.scoreMatrix.scores[tablet.id]?.get(perf.id)
        assertNotNull("Phone-Performance score must be non-null",  phonePerf)
        assertNotNull("Laptop-Performance score must be non-null", laptopPerf)
        assertNotNull("Tablet-Performance score must be non-null", tabletPerf)

        // All agreed: Phone > Laptop > Tablet
        assertTrue("Phone must outscore Laptop for Performance",  phonePerf!!  > laptopPerf!!)
        assertTrue("Laptop must outscore Tablet for Performance", laptopPerf!! > tabletPerf!!)

        // All scores must be in [0, 10]
        for (score in listOf(phonePerf, laptopPerf, tabletPerf)) {
            assertTrue("Score must be >= 0",  score!! >= 0.0)
            assertTrue("Score must be <= 10", score!! <= 10.0)
        }

        // --- scoreMatrix: Stability scores are null (only 2 evals, threshold=3) ---
        assertNull("Phone-Stability score must be null (below threshold)",
            output.scoreMatrix.scores[phone.id]?.get(stab.id))
        assertNull("Laptop-Stability score must be null (below threshold)",
            output.scoreMatrix.scores[laptop.id]?.get(stab.id))
    }

    // -----------------------------------------------------------------------
    // Test 5 — delete removes item/feature from subsequent LoadFeatureScores output
    // -----------------------------------------------------------------------

    @Test
    fun deleteItem_disappearsFromUseCase() = runBlocking {
        val uidA   = signUpAs("del_flow_a@test.com")
        val phone  = itemRepo.createItem("Phone",  uidA).getOrThrow()
        val laptop = itemRepo.createItem("Laptop", uidA).getOrThrow()

        itemRepo.deleteItem(phone.id).getOrThrow()

        val useCase = LoadFeatureScoresUseCase(
            itemRepository       = itemRepo,
            featureRepository    = featureRepo,
            evaluationRepository = evaluationRepo,
            scoringEngine        = DefaultFeatureScoringEngine()
        )
        val output = useCase.execute().getOrThrow()
        val ids    = output.items.map { it.id }

        assertFalse("Deleted phone must not appear in use-case output", phone.id  in ids)
        assertTrue("Laptop must still appear",                          laptop.id in ids)
    }

    @Test
    fun deleteFeature_disappearsFromUseCase() = runBlocking {
        val uidA  = signUpAs("del_feat_a@test.com")
        val perf  = featureRepo.createFeature("Performance", uidA).getOrThrow()
        val stab  = featureRepo.createFeature("Stability",   uidA).getOrThrow()

        featureRepo.deleteFeature(perf.id).getOrThrow()

        val useCase = LoadFeatureScoresUseCase(
            itemRepository       = itemRepo,
            featureRepository    = featureRepo,
            evaluationRepository = evaluationRepo,
            scoringEngine        = DefaultFeatureScoringEngine()
        )
        val output  = useCase.execute().getOrThrow()
        val featIds = output.features.map { it.id }

        assertFalse("Deleted Performance must not appear in use-case output", perf.id in featIds)
        assertTrue("Stability must still appear",                             stab.id in featIds)
    }
}
