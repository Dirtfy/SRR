package com.dirtfy.srr.remote

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.dirtfy.srr.remote.repository.RemoteFeatureRepository
import com.dirtfy.srr.remote.repository.RemoteItemRepository
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
            // SRRApplication.onCreate() already calls setPersistenceEnabled(false) + useEmulator().
            // DO NOT call firestoreSettings = Builder()...build() here — Builder() starts from
            // DEFAULT (production) host, which would overwrite the emulator URL.
            try { Firebase.auth.useEmulator("localhost", 9099) } catch (_: Exception) {}
            try { Firebase.firestore.useEmulator("localhost", 8080) } catch (_: Exception) {}
        }
    }

    private lateinit var itemRepository: RemoteItemRepository
    private lateinit var featureRepository: RemoteFeatureRepository

    // Timestamp suffix makes every email address globally unique per JVM launch,
    // so accounts from a prior test run that the emulator hasn't fully deleted yet
    // can never cause a collision in the current run.
    private val p = System.currentTimeMillis().toString().takeLast(8)
    private suspend fun signUpWith(tag: String): String {
        Firebase.auth.signOut()
        Firebase.auth.createUserWithEmailAndPassword("${p}_${tag}@t.com", "pw123456").await()
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
        clearEmulator()  // start every test with a clean slate
        itemRepository    = RemoteItemRepository()
        featureRepository = RemoteFeatureRepository()
    }

    @After
    fun tearDown() {
        clearEmulator()
    }

    private fun deleteUrl(urlStr: String) {
        val conn = URL(urlStr).openConnection() as HttpURLConnection
        conn.requestMethod  = "DELETE"
        conn.connectTimeout = 3_000
        conn.responseCode   // triggers the request to be sent and waits for response
        conn.disconnect()
    }

    // -----------------------------------------------------------------------
    // Item: field storage
    // -----------------------------------------------------------------------

    @Test
    fun createItem_storesNameAndCreatedBy() = runBlocking {
        val uid = signUpWith("itemcreate")

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
        val uidA   = signUpWith("itemvis_a")
        val laptop = itemRepository.createItem("Laptop", uidA).getOrThrow()
        val tablet = itemRepository.createItem("Tablet", uidA).getOrThrow()

        // User B reads — should see both
        signUpWith("itemvis_b")
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
        val uid = signUpWith("itemorder")

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
        val uid = signUpWith("itemdel")
        val item = itemRepository.createItem("ToDelete", uid).getOrThrow()

        itemRepository.deleteItem(item.id).getOrThrow()

        val ids = itemRepository.getAllItems().getOrThrow().map { it.id }
        assertFalse("Deleted item must not appear in getAllItems", item.id in ids)
    }

    // -----------------------------------------------------------------------
    // Item: uniqueness enforcement
    // -----------------------------------------------------------------------

    @Test
    fun createItem_duplicateName_fails() = runBlocking {
        val uid = signUpWith("itemdup")
        itemRepository.createItem("Laptop", uid).getOrThrow()

        val result = itemRepository.createItem("Laptop", uid)
        assertTrue("Duplicate item name must return failure", result.isFailure)
        assertTrue("Error message must mention 'already exists'",
            result.exceptionOrNull()?.message?.contains("already exists") == true)
    }

    @Test
    fun createItem_caseInsensitiveDuplicate_fails() = runBlocking {
        val uid = signUpWith("itemcase")
        itemRepository.createItem("Laptop", uid).getOrThrow()

        val result = itemRepository.createItem("LAPTOP", uid)
        assertTrue("Case-insensitive duplicate must return failure", result.isFailure)
    }

    @Test
    fun createItem_sameNameAsFeature_succeeds() = runBlocking {
        val uid = signUpWith("itemfeatshare")
        featureRepository.createFeature("Durability", uid).getOrThrow()

        val result = itemRepository.createItem("Durability", uid)
        assertTrue("Item can share a name with a feature", result.isSuccess)
    }

    @Test
    fun nonCreator_cannotDeleteItem() = runBlocking {
        // Owner creates the item
        val uidA = signUpWith("itemprot_a")
        val item = itemRepository.createItem("Protected", uidA).getOrThrow()

        // Intruder attempts delete
        signUpWith("itemprot_b")
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
        val uid = signUpWith("featcreate")

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
        val uidA = signUpWith("featvis_a")
        val perf = featureRepository.createFeature("Performance", uidA).getOrThrow()
        val stab = featureRepository.createFeature("Stability",   uidA).getOrThrow()

        signUpWith("featvis_b")
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
        val uid = signUpWith("featorder")

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
        val uid = signUpWith("featdel")
        val feature = featureRepository.createFeature("ToDelete", uid).getOrThrow()

        featureRepository.deleteFeature(feature.id).getOrThrow()

        val ids = featureRepository.getAllFeatures().getOrThrow().map { it.id }
        assertFalse("Deleted feature must not appear in getAllFeatures", feature.id in ids)
    }

    // -----------------------------------------------------------------------
    // Feature: uniqueness enforcement
    // -----------------------------------------------------------------------

    @Test
    fun createFeature_duplicateName_fails() = runBlocking {
        val uid = signUpWith("featdup")
        featureRepository.createFeature("Durability", uid).getOrThrow()

        val result = featureRepository.createFeature("Durability", uid)
        assertTrue("Duplicate feature name must return failure", result.isFailure)
        assertTrue("Error message must mention 'already exists'",
            result.exceptionOrNull()?.message?.contains("already exists") == true)
    }

    @Test
    fun createFeature_caseInsensitiveDuplicate_fails() = runBlocking {
        val uid = signUpWith("featcase")
        featureRepository.createFeature("Durability", uid).getOrThrow()

        val result = featureRepository.createFeature("DURABILITY", uid)
        assertTrue("Case-insensitive duplicate feature name must return failure", result.isFailure)
    }

    @Test
    fun nonCreator_cannotDeleteFeature() = runBlocking {
        val uidA    = signUpWith("featprot_a")
        val feature = featureRepository.createFeature("Protected", uidA).getOrThrow()

        signUpWith("featprot_b")
        val result = featureRepository.deleteFeature(feature.id)

        assertTrue("Non-creator delete must fail (PERMISSION_DENIED)", result.isFailure)
        val ids = featureRepository.getAllFeatures().getOrThrow().map { it.id }
        assertTrue("Feature must still exist after failed delete", feature.id in ids)
    }
}
