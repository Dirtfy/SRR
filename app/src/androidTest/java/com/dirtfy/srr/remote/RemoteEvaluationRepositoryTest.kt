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
            Firebase.auth.useEmulator("localhost", 9099)
            Firebase.firestore.useEmulator("localhost", 8080)
        }
    }

    private lateinit var repository: RemoteEvaluationRepository

    @Before
    fun setUp() {
        repository = RemoteEvaluationRepository()
    }

    @After
    fun tearDown() = runBlocking {
        Firebase.auth.signOut()
        // Clear Firestore emulator data so tests are independent
        try {
            val projectId = FirebaseApp.getInstance().options.projectId ?: return@runBlocking
            deleteUrl("http://localhost:8080/emulator/v1/projects/$projectId/databases/(default)/documents")
            deleteUrl("http://localhost:9099/emulator/v1/projects/$projectId/accounts")
        } catch (_: Exception) {
            // Non-fatal: emulator may not be running in all CI environments
        }
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

        // Sign in so security rules (request.auth != null && uid == docId) pass
        Firebase.auth.createUserWithEmailAndPassword("write@test.com", "password123").await()
        val userId = Firebase.auth.currentUser!!.uid

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
        val featureId   = "feature_portability"
        val firstOrder  = listOf("item_a", "item_b")
        val secondOrder = listOf("item_b", "item_a")

        Firebase.auth.createUserWithEmailAndPassword("overwrite@test.com", "password123").await()
        val userId = Firebase.auth.currentUser!!.uid

        repository.submitEvaluation(Evaluation(userId, featureId, firstOrder)).getOrThrow()
        repository.submitEvaluation(Evaluation(userId, featureId, secondOrder)).getOrThrow()

        val evaluations = repository.getEvaluationsForFeature(featureId).getOrThrow()
        assertEquals("Exactly one doc after re-submit", 1, evaluations.size)
        assertEquals("Second order must win", secondOrder, evaluations[0].orderedItemIds)
    }

    @Test
    fun getEvaluationsForFeature_returnsAllUsers() = runBlocking {
        val featureId = "feature_safety"

        Firebase.auth.createUserWithEmailAndPassword("userA@test.com", "password123").await()
        val uidA = Firebase.auth.currentUser!!.uid
        repository.submitEvaluation(Evaluation(uidA, featureId, listOf("item_a", "item_b"))).getOrThrow()
        Firebase.auth.signOut()

        Firebase.auth.createUserWithEmailAndPassword("userB@test.com", "password123").await()
        val uidB = Firebase.auth.currentUser!!.uid
        repository.submitEvaluation(Evaluation(uidB, featureId, listOf("item_b", "item_a"))).getOrThrow()

        val evaluations = repository.getEvaluationsForFeature(featureId).getOrThrow()
        assertEquals("Both users' evaluations must be returned", 2, evaluations.size)
        val uids = evaluations.map { it.userId }.toSet()
        assertTrue("uidA must be present", uidA in uids)
        assertTrue("uidB must be present", uidB in uids)
    }

    @Test
    fun getEvaluationsForFeature_returnsEmptyListWhenNoEvaluations() = runBlocking {
        // Must sign in: security rules require request.auth != null even for reads
        Firebase.auth.createUserWithEmailAndPassword("empty@test.com", "password123").await()

        val result = repository.getEvaluationsForFeature("feature_nonexistent").getOrThrow()
        assertTrue("Must return empty list when feature has no evaluations", result.isEmpty())
    }
}
