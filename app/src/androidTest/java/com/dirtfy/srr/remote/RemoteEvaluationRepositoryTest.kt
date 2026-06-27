package com.dirtfy.srr.remote

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.dirtfy.srr.core.model.Evaluation
import com.dirtfy.srr.core.scoring.DefaultFeatureScoringEngine
import com.dirtfy.srr.remote.repository.RemoteEvaluationRepository
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import java.net.HttpURLConnection
import java.net.URL
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Integration tests for RemoteEvaluationRepository against the Firebase Local Emulator Suite.
 *
 * Prerequisites (run before executing these tests):
 *   1. npm install -g firebase-tools
 *   2. firebase emulators:start --only auth,firestore --project <your-project-id>
 *
 * Physical device: run `adb reverse tcp:8080 tcp:8080 && adb reverse tcp:9099 tcp:9099` first.
 * AVD: replace localhost with 10.0.2.2 (AVD routes that address to the host).
 *
 * IMPORTANT: Run on ONE device at a time. If both a physical device and an AVD are connected,
 * set ANDROID_SERIAL to the target serial before running, e.g.:
 *   $env:ANDROID_SERIAL = "R3CX50BXZVD"; ./gradlew connectedDebugAndroidTest
 * Running on multiple devices simultaneously causes account-creation collisions in the emulator.
 *
 * Run via:  ./gradlew connectedDebugAndroidTest
 */
@RunWith(AndroidJUnit4::class)
class RemoteEvaluationRepositoryTest {

    // Must be configured once before FirebaseFirestore is ever touched
    companion object {
        @BeforeClass
        @JvmStatic
        fun setUpEmulators() {
            try { Firebase.auth.useEmulator("localhost", 9099) } catch (_: Exception) {}
            try { Firebase.firestore.useEmulator("localhost", 8080) } catch (_: Exception) {}
            // Must be in its own try-catch: useEmulator() above throws when already set,
            // which would prevent setPersistenceEnabled from running inside the same block.
            try {
                Firebase.firestore.firestoreSettings =
                    com.google.firebase.firestore.FirebaseFirestoreSettings.Builder()
                        .setPersistenceEnabled(false)
                        .build()
            } catch (_: Exception) {}
        }
    }

    private lateinit var repository: RemoteEvaluationRepository

    private val p = System.currentTimeMillis().toString().takeLast(8)
    private suspend fun signUpWith(tag: String): String {
        Firebase.auth.signOut()
        Firebase.auth.createUserWithEmailAndPassword("${p}_${tag}@t.com", "password123").await()
        return Firebase.auth.currentUser!!.uid
    }

    private fun clearEmulator() {
        try { Firebase.auth.signOut() } catch (_: Exception) {}
        try {
            deleteUrl("http://localhost:8080/emulator/v1/projects/shared-relative-rank/databases/(default)/documents")
            deleteUrl("http://localhost:9099/emulator/v1/projects/shared-relative-rank/accounts")
        } catch (_: Exception) {}
    }

    @Before
    fun setUp() {
        clearEmulator()
        repository = RemoteEvaluationRepository()
    }

    @After
    fun tearDown() {
        clearEmulator()
    }

    private fun deleteUrl(urlStr: String) {
        val conn = URL(urlStr).openConnection() as HttpURLConnection
        conn.requestMethod = "DELETE"
        conn.connectTimeout = 3000
        conn.connect()
        conn.disconnect()
    }

    // -----------------------------------------------------------------------
    // Tests
    // -----------------------------------------------------------------------

    @Test
    fun submitEvaluation_writesToCorrectFirestorePath() = runBlocking {
        val featureId = "feature_durability"
        val ordered   = listOf("item_b", "item_c", "item_a")

        val userId = signUpWith("write")

        val result = repository.submitEvaluation(Evaluation(userId, featureId, ordered))
        assertTrue("submitEvaluation should succeed", result.isSuccess)

        val snapshot = Firebase.firestore
            .collection("evaluations")
            .document(featureId)
            .collection("userEvaluations")
            .document(userId)
            .get()
            .await()

        assertTrue("Document must exist at evaluations/$featureId/userEvaluations/$userId",
            snapshot.exists())

        @Suppress("UNCHECKED_CAST")
        val stored = snapshot.get("orderedItemIds") as? List<String>
        assertEquals("orderedItemIds must match submitted order", ordered, stored)
    }

    @Test
    fun submitEvaluation_resubmitOverwritesPrevious() = runBlocking {
        val featureId   = "${p}_feature_portability"
        val firstOrder  = listOf("item_a", "item_b")
        val secondOrder = listOf("item_b", "item_a")

        val userId = signUpWith("overwrite")

        repository.submitEvaluation(Evaluation(userId, featureId, firstOrder)).getOrThrow()
        repository.submitEvaluation(Evaluation(userId, featureId, secondOrder)).getOrThrow()

        val evaluations = repository.getEvaluationsForFeature(featureId).getOrThrow()
        assertEquals("Exactly one doc after re-submit", 1, evaluations.size)
        assertEquals("Second order must win", secondOrder, evaluations[0].orderedItemIds)
    }

    @Test
    fun getEvaluationsForFeature_returnsAllUsers() = runBlocking {
        val featureId = "${p}_feature_safety"

        val uidA = signUpWith("userA")
        repository.submitEvaluation(Evaluation(uidA, featureId, listOf("item_a", "item_b"))).getOrThrow()

        val uidB = signUpWith("userB")
        repository.submitEvaluation(Evaluation(uidB, featureId, listOf("item_b", "item_a"))).getOrThrow()

        val evaluations = repository.getEvaluationsForFeature(featureId).getOrThrow()
        assertEquals("Both users' evaluations must be returned", 2, evaluations.size)
        val uids = evaluations.map { it.userId }.toSet()
        assertTrue("uidA must be present", uidA in uids)
        assertTrue("uidB must be present", uidB in uids)
    }

    @Test
    fun threeUsersEvaluate_scoreExceedsThreshold() = runBlocking {
        val featureId = "${p}_feature_threshold"
        val allItemIds = listOf("item_a", "item_b", "item_c")

        // User A: A > B > C
        val uidA = signUpWith("threshA")
        repository.submitEvaluation(Evaluation(uidA, featureId, listOf("item_a", "item_b", "item_c"))).getOrThrow()

        // User B: B > A > C
        val uidB = signUpWith("threshB")
        repository.submitEvaluation(Evaluation(uidB, featureId, listOf("item_b", "item_a", "item_c"))).getOrThrow()

        // User C: A > C > B
        val uidC = signUpWith("threshC")
        repository.submitEvaluation(Evaluation(uidC, featureId, listOf("item_a", "item_c", "item_b"))).getOrThrow()

        val evaluations = repository.getEvaluationsForFeature(featureId).getOrThrow()
        assertEquals("All three evaluations must be stored", 3, evaluations.size)

        // Run scoring engine inline to confirm scores are non-null (threshold=3 is met)
        val engine = DefaultFeatureScoringEngine()
        val matrix = engine.computeScores(
            allItemIds           = allItemIds,
            allFeatureIds        = listOf(featureId),
            evaluationsByFeature = mapOf(featureId to evaluations),
            minVoteThreshold     = 3
        )
        val scoreA = matrix.scores["item_a"]?.get(featureId)
        val scoreB = matrix.scores["item_b"]?.get(featureId)
        val scoreC = matrix.scores["item_c"]?.get(featureId)
        assertNotNull("item_a score must be non-null when threshold is met", scoreA)
        assertNotNull("item_b score must be non-null when threshold is met", scoreB)
        assertNotNull("item_c score must be non-null when threshold is met", scoreC)
        // item_a ranked first by 2/3 users — should have the highest score
        assertTrue("item_a should outrank item_c", scoreA!! > scoreC!!)
    }

    @Test
    fun getEvaluationsForFeature_returnsEmptyListWhenNoEvaluations() = runBlocking {
        // Must sign in: security rules require request.auth != null even for reads
        signUpWith("empty")

        val result = repository.getEvaluationsForFeature("feature_nonexistent").getOrThrow()
        assertTrue("Must return empty list when feature has no evaluations", result.isEmpty())
    }
}
