package com.dirtfy.srr.remote

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.dirtfy.srr.core.model.Evaluation
import com.dirtfy.srr.remote.repository.RemoteEvaluationRepository
import com.google.firebase.FirebaseApp
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
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Integration tests for RemoteEvaluationRepository against the Firebase Local Emulator Suite.
 *
 * Prerequisites (run before executing these tests):
 *   1. npm install -g firebase-tools
 *   2. firebase login
 *   3. firebase init emulators  (enable Auth on port 9099, Firestore on port 8080)
 *   4. firebase emulators:start --only auth,firestore
 *
 * Run via:  ./gradlew connectedDebugAndroidTest
 * (Requires a connected device or emulator with 10.0.2.2 routing to host machine.)
 */
@RunWith(AndroidJUnit4::class)
class RemoteEvaluationRepositoryTest {

    private val repository = RemoteEvaluationRepository()

    @Before
    fun setUp() {
        // Point SDKs at local emulators — 10.0.2.2 is host machine from Android emulator
        Firebase.auth.useEmulator("10.0.2.2", 9099)
        Firebase.firestore.useEmulator("10.0.2.2", 8080)
    }

    @After
    fun tearDown() {
        // Clear all emulator Firestore data via REST API so tests are independent
        runBlocking {
            try {
                val projectId = FirebaseApp.getInstance().options.projectId ?: return@runBlocking
                val url = URL("http://10.0.2.2:8080/emulator/v1/projects/$projectId/databases/(default)/documents")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "DELETE"
                conn.connect()
                conn.disconnect()
            } catch (_: Exception) {
                // Teardown failure is non-fatal; emulator may not be running
            }
            // Sign out after each test
            Firebase.auth.signOut()
        }
    }

    @Test
    fun submitEvaluation_writesToCorrectFirestorePath() = runBlocking {
        val featureId = "feature_durability"
        val userId    = "uid_test_write"
        val ordered   = listOf("item_b", "item_c", "item_a")

        val result = repository.submitEvaluation(
            Evaluation(userId, featureId, ordered)
        )
        assertTrue("submitEvaluation should succeed", result.isSuccess)

        // Verify the document exists at the expected path
        val snapshot = Firebase.firestore
            .collection("evaluations")
            .document(featureId)
            .collection("userEvaluations")
            .document(userId)
            .get()
            .await()

        assertTrue("Document should exist at evaluations/$featureId/userEvaluations/$userId",
            snapshot.exists())

        @Suppress("UNCHECKED_CAST")
        val storedOrder = snapshot.get("orderedItemIds") as? List<String>
        assertEquals("orderedItemIds should match submitted order", ordered, storedOrder)
    }

    @Test
    fun submitEvaluation_resubmitOverwritesPrevious() = runBlocking {
        val featureId  = "feature_portability"
        val userId     = "uid_test_overwrite"
        val firstOrder  = listOf("item_a", "item_b")
        val secondOrder = listOf("item_b", "item_a")

        repository.submitEvaluation(Evaluation(userId, featureId, firstOrder)).getOrThrow()
        repository.submitEvaluation(Evaluation(userId, featureId, secondOrder)).getOrThrow()

        val evaluations = repository.getEvaluationsForFeature(featureId).getOrThrow()

        assertEquals("Only one evaluation document should exist after re-submit", 1, evaluations.size)
        assertEquals("orderedItemIds should reflect the second submission",
            secondOrder, evaluations[0].orderedItemIds)
    }

    @Test
    fun getEvaluationsForFeature_returnsAllUsers() = runBlocking {
        val featureId = "feature_safety"

        // Sign up and submit as user A
        Firebase.auth.createUserWithEmailAndPassword("userA@test.com", "password123").await()
        val uidA = Firebase.auth.currentUser!!.uid
        repository.submitEvaluation(Evaluation(uidA, featureId, listOf("item_a", "item_b"))).getOrThrow()
        Firebase.auth.signOut()

        // Sign up and submit as user B
        Firebase.auth.createUserWithEmailAndPassword("userB@test.com", "password123").await()
        val uidB = Firebase.auth.currentUser!!.uid
        repository.submitEvaluation(Evaluation(uidB, featureId, listOf("item_b", "item_a"))).getOrThrow()

        val evaluations = repository.getEvaluationsForFeature(featureId).getOrThrow()

        assertEquals("Both users' evaluations should be returned", 2, evaluations.size)
        val returnedUids = evaluations.map { it.userId }.toSet()
        assertTrue("User A's evaluation should be present", uidA in returnedUids)
        assertTrue("User B's evaluation should be present", uidB in returnedUids)
    }

    @Test
    fun getEvaluationsForFeature_returnsEmptyListWhenNoEvaluations() = runBlocking {
        val evaluations = repository.getEvaluationsForFeature("feature_nonexistent").getOrThrow()
        assertTrue("Should return empty list when no evaluations exist", evaluations.isEmpty())
    }
}
