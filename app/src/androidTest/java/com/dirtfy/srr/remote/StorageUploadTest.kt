package com.dirtfy.srr.remote

import android.graphics.Bitmap
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.dirtfy.srr.remote.repository.RemoteStorageRepository
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * Integration tests for Firebase Storage upload against the Storage emulator (port 9199).
 *
 * Prerequisite: Firebase emulators running (auth:9099, storage:9199).
 * Physical device: adb reverse tcp:9099 tcp:9099 && adb reverse tcp:9199 tcp:9199
 */
@RunWith(AndroidJUnit4::class)
class StorageUploadTest {

    companion object {
        @BeforeClass
        @JvmStatic
        fun setUpEmulators() {
            try { Firebase.auth.useEmulator("localhost", 9099) } catch (_: Exception) {}
            try { Firebase.storage.useEmulator("localhost", 9199) } catch (_: Exception) {}
        }
    }

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val p = System.currentTimeMillis().toString().takeLast(8)

    @Before
    fun setUp() = runBlocking {
        Firebase.auth.signOut()
        Firebase.auth.createUserWithEmailAndPassword("${p}_stor@t.com", "pw123456").await()
    }

    @After
    fun tearDown() {
        try { Firebase.auth.signOut() } catch (_: Exception) {}
        try {
            val conn = URL("http://localhost:9099/emulator/v1/projects/shared-relative-rank/accounts")
                .openConnection() as HttpURLConnection
            conn.requestMethod  = "DELETE"
            conn.connectTimeout = 3_000
            conn.responseCode
            conn.disconnect()
        } catch (_: Exception) {}
    }

    private fun createTestImageFile(name: String): File {
        val bitmap = Bitmap.createBitmap(50, 50, Bitmap.Config.ARGB_8888)
        val file = File(context.cacheDir, name)
        FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 80, it) }
        return file
    }

    @Test
    fun uploadItemImage_successReturnsHttpUrl() = runBlocking {
        val file = createTestImageFile("upload_success_${System.currentTimeMillis()}.jpg")
        val uri  = Uri.fromFile(file)

        val result = RemoteStorageRepository(context).uploadItemImage(uri)

        assertTrue("upload should succeed: ${result.exceptionOrNull()?.message}", result.isSuccess)
        val url = result.getOrThrow()
        assertTrue("URL must start with http", url.startsWith("http"))

        file.delete()
    }

    @Test
    fun uploadItemImage_urlContainsItemsPath() = runBlocking {
        val file = createTestImageFile("upload_path_${System.currentTimeMillis()}.jpg")
        val result = RemoteStorageRepository(context).uploadItemImage(Uri.fromFile(file))

        assertTrue(result.isSuccess)
        // Storage path is items/<uuid>.jpg — encoded as items%2F in download URL
        assertTrue("URL should reference items/ path", result.getOrThrow().contains("items"))

        file.delete()
    }

    @Test
    fun uploadItemImage_unauthenticated_fails() = runBlocking {
        Firebase.auth.signOut()
        val file = createTestImageFile("upload_unauth_${System.currentTimeMillis()}.jpg")

        val result = RemoteStorageRepository(context).uploadItemImage(Uri.fromFile(file))

        assertTrue("unauthenticated upload must fail", result.isFailure)

        file.delete()
    }

    @Test
    fun uploadTwice_producesTwoDistinctUrls() = runBlocking {
        val file1 = createTestImageFile("upload_dup1_${System.currentTimeMillis()}.jpg")
        val file2 = createTestImageFile("upload_dup2_${System.currentTimeMillis()}.jpg")
        val repo  = RemoteStorageRepository(context)

        val url1 = repo.uploadItemImage(Uri.fromFile(file1)).getOrThrow()
        val url2 = repo.uploadItemImage(Uri.fromFile(file2)).getOrThrow()

        assertTrue("each upload must produce a unique URL", url1 != url2)

        file1.delete()
        file2.delete()
    }
}
