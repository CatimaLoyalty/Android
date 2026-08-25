package protect.card_locker

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Looper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.LooperMode
import org.robolectric.shadows.ShadowContentResolver
import org.robolectric.shadows.ShadowToast
import java.io.FilterInputStream
import java.io.IOException
import java.io.InputStream
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

@RunWith(RobolectricTestRunner::class)
@LooperMode(LooperMode.Mode.PAUSED)
class ScanActivityTest {
    @Before
    fun setUp() {
        ShadowToast.reset()
    }

    @Test
    fun pkpassPickerReadsOffMainThreadAndReturnsResultOnMainThread() {
        val uri = Uri.parse("content://test/picker-cloud-backed.pkpass")
        val readStarted = CountDownLatch(1)
        val streamClosed = CountDownLatch(1)
        val readLooper = AtomicReference<Looper>()
        val pkpass = javaClass.getResourceAsStream(
            "pkpass/DCBLN24/DCBLN24-QLUKT-1-passbook.pkpass"
        )
        assertNotNull(pkpass)
        ShadowContentResolver().registerInputStream(
            uri,
            RecordingInputStream(pkpass, readStarted, streamClosed, readLooper)
        )
        val controller = createActivity()
        val activity = controller.get()

        handlePkpassResult(activity, Intent().setData(uri))

        waitFor { readStarted.count == 0L }
        assertTrue(readLooper.get() !== Looper.getMainLooper())
        assertTrue(streamClosed.await(5, TimeUnit.SECONDS))
        assertEquals(Activity.RESULT_CANCELED, shadowOf(activity).resultCode)

        waitFor { shadowOf(activity).resultCode == Activity.RESULT_OK }
        assertNotNull(shadowOf(activity).resultIntent)
        assertTrue(activity.isFinishing)
        controller.destroy()
    }

    @Test
    fun providerFailureShowsOneErrorAndReenablesScanner() {
        val uri = Uri.parse("content://test/unavailable-cloud-backed.pkpass")
        ShadowContentResolver().registerInputStream(uri, FailingInputStream())
        val controller = createActivity()
        val activity = controller.get()
        setScannerActive(activity, false)
        val initialToastCount = ShadowToast.shownToastCount()

        handlePkpassResult(activity, Intent().setData(uri))

        waitFor { ShadowToast.shownToastCount() > initialToastCount }
        assertEquals(initialToastCount + 1, ShadowToast.shownToastCount())
        assertEquals(activity.getString(R.string.errorReadingFile), ShadowToast.getTextOfLatestToast())
        assertTrue(isScannerActive(activity))
        assertFalse(activity.isFinishing)
        controller.destroy()
    }

    @Test
    fun destroyingActivityCancelsPendingPkpassResult() {
        val uri = Uri.parse("content://test/pending-picker-cloud-backed.pkpass")
        val readStarted = CountDownLatch(1)
        val releaseRead = CountDownLatch(1)
        val streamClosed = CountDownLatch(1)
        val pkpass = javaClass.getResourceAsStream(
            "pkpass/DCBLN24/DCBLN24-QLUKT-1-passbook.pkpass"
        )
        assertNotNull(pkpass)
        ShadowContentResolver().registerInputStream(
            uri,
            BlockingInputStream(pkpass, readStarted, releaseRead, streamClosed)
        )
        val controller = createActivity()
        val activity = controller.get()

        handlePkpassResult(activity, Intent().setData(uri))
        waitFor { readStarted.count == 0L }
        controller.destroy()
        releaseRead.countDown()
        assertTrue(streamClosed.await(5, TimeUnit.SECONDS))
        shadowOf(Looper.getMainLooper()).idle()

        assertEquals(Activity.RESULT_CANCELED, shadowOf(activity).resultCode)
        assertFalse(ShadowToast.showedToast(activity.getString(R.string.errorReadingFile)))
    }

    private fun createActivity(): ActivityController<ScanActivity> =
        Robolectric.buildActivity(ScanActivity::class.java).create().start().resume().visible()

    private fun handlePkpassResult(activity: ScanActivity, intent: Intent) {
        ScanActivity::class.java.getDeclaredMethod(
            "handleActivityResult",
            Int::class.javaPrimitiveType,
            Int::class.javaPrimitiveType,
            Intent::class.java
        ).apply { isAccessible = true }.invoke(
            activity,
            Utils.BARCODE_IMPORT_FROM_PKPASS_FILE,
            Activity.RESULT_OK,
            intent
        )
    }

    private fun setScannerActive(activity: ScanActivity, active: Boolean) {
        ScanActivity::class.java.getDeclaredMethod(
            "setScannerActive",
            Boolean::class.javaPrimitiveType
        ).apply { isAccessible = true }.invoke(activity, active)
    }

    private fun isScannerActive(activity: ScanActivity): Boolean =
        ScanActivity::class.java.getDeclaredField("mScannerActive")
            .apply { isAccessible = true }
            .getBoolean(activity)

    private fun waitFor(condition: () -> Boolean) {
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5)
        while (!condition() && System.nanoTime() < deadline) {
            shadowOf(Looper.getMainLooper()).idle()
            Thread.sleep(10)
        }
        shadowOf(Looper.getMainLooper()).idle()
        assertTrue("Timed out waiting for asynchronous activity work", condition())
    }

    private class RecordingInputStream(
        inputStream: InputStream,
        private val readStarted: CountDownLatch,
        private val streamClosed: CountDownLatch,
        private val readLooper: AtomicReference<Looper>
    ) : FilterInputStream(inputStream) {
        private fun recordRead() {
            readLooper.compareAndSet(null, Looper.myLooper())
            readStarted.countDown()
        }

        override fun read(): Int {
            recordRead()
            return super.read()
        }

        override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
            recordRead()
            return super.read(buffer, offset, length)
        }

        override fun close() {
            try {
                super.close()
            } finally {
                streamClosed.countDown()
            }
        }
    }

    private class BlockingInputStream(
        inputStream: InputStream,
        private val readStarted: CountDownLatch,
        private val releaseRead: CountDownLatch,
        private val streamClosed: CountDownLatch
    ) : FilterInputStream(inputStream) {
        private fun awaitRelease() {
            readStarted.countDown()
            try {
                if (!releaseRead.await(5, TimeUnit.SECONDS)) {
                    throw IOException("Timed out waiting to release provider stream")
                }
            } catch (exception: InterruptedException) {
                Thread.currentThread().interrupt()
                throw IOException(exception)
            }
        }

        override fun read(): Int {
            awaitRelease()
            return super.read()
        }

        override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
            awaitRelease()
            return super.read(buffer, offset, length)
        }

        override fun close() {
            try {
                super.close()
            } finally {
                streamClosed.countDown()
            }
        }
    }

    private class FailingInputStream : InputStream() {
        override fun read(): Int = throw IOException("Remote provider unavailable")
    }
}
