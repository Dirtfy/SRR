package com.dirtfy.srr.remote

import androidx.test.ext.junit.runners.AndroidJUnit4
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
 * Repository-level tests for item and feature CRUD against the Firebase Local Emulator Suite.
 *
 * Covers: field storage, cross-user visibility, creation ordering (createdAt),
 * and Firestore security rule enforcement for owner-only delete.
 *
 * Prerequisite: Firebase emulators running (auth:9099, firestore:8080).
 * Physical device: adb reverse tcp:8080 tcp:8080 && adb reverse tcp:9099 tcp:9099
 */
@RunWith(AndroidJUnit4::class)
class RemoteItemFeatureRepositoryTest {

    companion object {
        @BeforeClass
        @JvmStatic
        fun setUpEmulators() {
            // try-catch: second test class in the same process gets here after the Firestore
            // client is already initialized; calling useEmulator() again would throw.
            try { Firebase.auth.useEmulator("localhost", 9099) } catch (_: Exception) {}
            try { Firebase.firestore.useEmulator("localhost", 8080) } catch (_: Exception) {}
        }
    }

    private lateinit var itemRepository: RemoteItemRepository
    private lateinit var featureRepository: RemoteFeatureRepository

    @Before
    fun setUp() {
        itemRepository    = RemoteItemRepository()
        featureRepository = RemoteFeatureRepository()
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
    // Item: field storage
    // -----------------------------------------------------------------------

    @Test
    fun createItem_storesNameAndCreatedBy() = runBlocking {
        Firebase.auth.createUserWithEmailAndPassword("itemcreate@test.com", "pw123456").await()
        val uid = Firebase.auth.currentUser!!.uid

        val item = itemRepository.createItem("Laptop", uid).getOrThrow()

        assertEquals("Laptop", item.name)
        assertEquals(uid,      item.createdBy)
        assertTrue("id must be non-empty", item.id.isNotEmpty())
    }

    // -----------------------------------------------------------------------
    // Item: cross-user visibility + createdBy attribution
    // -----------------------------------------------------------------------

    @Test
    fun items_visibleToAllSignedInUsers() = runBlocking {
        // User A creates two items
        Firebase.auth.createUserWithEmailAndPassword("itemvis_a@test.com", "pw123456").await()
        val uidA   = Firebase.auth.currentUser!!.uid
        val laptop = itemRepository.createItem("Laptop", uidA).getOrThrow()
        val tablet = itemRepository.createItem("Tablet", uidA).getOrThrow()
        Firebase.auth.signOut()

        // User B reads — should see both
        Firebase.auth.createUserWithEmailAndPassword("itemvis_b@test.com", "pw123456").await()
        val items = itemRepository.getAllItems().getOrThrow()
        val ids   = items.map { it.id }

        assertTrue("User B must see Laptop", laptop.id in ids)
        assertTrue("User B must see Tablet", tablet.id in ids)
        assertEquals("createdBy for Laptop must be uidA", uidA, items.find { it.id == laptop.id }!!.createdBy)
        assertEquals("createdBy for Tablet must be uidA", uidA, items.find { it.id == tablet.id }!!.createdBy)
    }

    // -----------------------------------------------------------------------
    // Item: chronological ordering
    // -----------------------------------------------------------------------

    @Test
    fun items_returnedInCreationOrder() = runBlocking {
        Firebase.auth.createUserWithEmailAndPassword("itemorder@test.com", "pw123456").await()
        val uid = Firebase.auth.currentUser!!.uid

        itemRepository.createItem("First",  uid).getOrThrow(); Thread.sleep(150)
        itemRepository.createItem("Second", uid).getOrThrow(); Thread.sleep(150)
        itemRepository.createItem("Third",  uid).getOrThrow()

        val names     = itemRepository.getAllItems().getOrThrow().map { it.name }
        val firstIdx  = names.indexOf("First")
        val secondIdx = names.indexOf("Second")
        val thirdIdx  = names.indexOf("Third")

        assertTrue("First before Second",  firstIdx  < secondIdx)
        assertTrue("Second before Third",  secondIdx < thirdIdx)
    }

    // -----------------------------------------------------------------------
    // Item: delete — owner succeeds, non-owner is rejected
    // -----------------------------------------------------------------------

    @Test
    fun creator_canDeleteOwnItem() = runBlocking {
        Firebase.auth.createUserWithEmailAndPassword("itemdel@test.com", "pw123456").await()
        val uid  = Firebase.auth.currentUser!!.uid
        val item = itemRepository.createItem("ToDelete", uid).getOrThrow()

        itemRepository.deleteItem(item.id).getOrThrow()

        val ids = itemRepository.getAllItems().getOrThrow().map { it.id }
        assertFalse("Deleted item must not appear in getAllItems", item.id in ids)
    }

    @Test
    fun nonCreator_cannotDeleteItem() = runBlocking {
        // Owner creates the item
        Firebase.auth.createUserWithEmailAndPassword("itemprot_a@test.com", "pw123456").await()
        val uidA = Firebase.auth.currentUser!!.uid
        val item = itemRepository.createItem("Protected", uidA).getOrThrow()
        Firebase.auth.signOut()

        // Intruder attempts delete
        Firebase.auth.createUserWithEmailAndPassword("itemprot_b@test.com", "pw123456").await()
        val result = itemRepository.deleteItem(item.id)

        assertTrue("Non-creator delete must fail (PERMISSION_DENIED)", result.isFailure)
        // Item must still be there
        val ids = itemRepository.getAllItems().getOrThrow().map { it.id }
        assertTrue("Item must still exist after failed delete", item.id in ids)
    }

    // -----------------------------------------------------------------------
    // Feature: field storage
    // -----------------------------------------------------------------------

    @Test
    fun createFeature_storesNameAndCreatedBy() = runBlocking {
        Firebase.auth.createUserWithEmailAndPassword("featcreate@test.com", "pw123456").await()
        val uid = Firebase.auth.currentUser!!.uid

        val feature = featureRepository.createFeature("Performance", uid).getOrThrow()

        assertEquals("Performance", feature.name)
        assertEquals(uid,           feature.createdBy)
        assertTrue("id must be non-empty", feature.id.isNotEmpty())
    }

    // -----------------------------------------------------------------------
    // Feature: cross-user visibility + createdBy attribution
    // -----------------------------------------------------------------------

    @Test
    fun features_visibleToAllSignedInUsers() = runBlocking {
        Firebase.auth.createUserWithEmailAndPassword("featvis_a@test.com", "pw123456").await()
        val uidA = Firebase.auth.currentUser!!.uid
        val perf = featureRepository.createFeature("Performance", uidA).getOrThrow()
        val stab = featureRepository.createFeature("Stability",   uidA).getOrThrow()
        Firebase.auth.signOut()

        Firebase.auth.createUserWithEmailAndPassword("featvis_b@test.com", "pw123456").await()
        val features = featureRepository.getAllFeatures().getOrThrow()
        val ids      = features.map { it.id }

        assertTrue("User B must see Performance", perf.id in ids)
        assertTrue("User B must see Stability",   stab.id in ids)
        assertEquals("createdBy for Performance must be uidA", uidA, features.find { it.id == perf.id }!!.createdBy)
    }

    // -----------------------------------------------------------------------
    // Feature: chronological ordering
    // -----------------------------------------------------------------------

    @Test
    fun features_returnedInCreationOrder() = runBlocking {
        Firebase.auth.createUserWithEmailAndPassword("featorder@test.com", "pw123456").await()
        val uid = Firebase.auth.currentUser!!.uid

        featureRepository.createFeature("First",  uid).getOrThrow(); Thread.sleep(150)
        featureRepository.createFeature("Second", uid).getOrThrow(); Thread.sleep(150)
        featureRepository.createFeature("Third",  uid).getOrThrow()

        val names     = featureRepository.getAllFeatures().getOrThrow().map { it.name }
        val firstIdx  = names.indexOf("First")
        val secondIdx = names.indexOf("Second")
        val thirdIdx  = names.indexOf("Third")

        assertTrue("First before Second",  firstIdx  < secondIdx)
        assertTrue("Second before Third",  secondIdx < thirdIdx)
    }

    // -----------------------------------------------------------------------
    // Feature: delete — owner succeeds, non-owner is rejected
    // -----------------------------------------------------------------------

    @Test
    fun creator_canDeleteOwnFeature() = runBlocking {
        Firebase.auth.createUserWithEmailAndPassword("featdel@test.com", "pw123456").await()
        val uid     = Firebase.auth.currentUser!!.uid
        val feature = featureRepository.createFeature("ToDelete", uid).getOrThrow()

        featureRepository.deleteFeature(feature.id).getOrThrow()

        val ids = featureRepository.getAllFeatures().getOrThrow().map { it.id }
        assertFalse("Deleted feature must not appear in getAllFeatures", feature.id in ids)
    }

    @Test
    fun nonCreator_cannotDeleteFeature() = runBlocking {
        Firebase.auth.createUserWithEmailAndPassword("featprot_a@test.com", "pw123456").await()
        val uidA    = Firebase.auth.currentUser!!.uid
        val feature = featureRepository.createFeature("Protected", uidA).getOrThrow()
        Firebase.auth.signOut()

        Firebase.auth.createUserWithEmailAndPassword("featprot_b@test.com", "pw123456").await()
        val result = featureRepository.deleteFeature(feature.id)

        assertTrue("Non-creator delete must fail (PERMISSION_DENIED)", result.isFailure)
        val ids = featureRepository.getAllFeatures().getOrThrow().map { it.id }
        assertTrue("Feature must still exist after failed delete", feature.id in ids)
    }
}
