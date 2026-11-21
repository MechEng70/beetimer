package io.github.mecheng70.beetimer.icon

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.core.content.ContextCompat
import androidx.test.core.app.ApplicationProvider
import io.github.mecheng70.beetimer.R
import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

/**
 * Renders the launcher icon's VectorDrawable layers with Android's own graphics (Robolectric
 * native mode) to app/build/icon-render/, for scripts/icon/compare.py to diff against the SVGs.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class IconRenderTest {
    @Test
    fun renderLayers() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val out = File("build/icon-render").apply { mkdirs() }
        mapOf(
            "background" to R.drawable.ic_launcher_background,
            "foreground" to R.drawable.ic_launcher_foreground,
            "monochrome" to R.drawable.ic_launcher_monochrome,
        ).forEach { (name, id) ->
            val bitmap = Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888)
            val drawable = ContextCompat.getDrawable(context, id)!!
            drawable.setBounds(0, 0, SIZE, SIZE)
            drawable.draw(Canvas(bitmap))
            File(out, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            var opaque = 0
            for (y in 0 until SIZE step 8) for (x in 0 until SIZE step 8) if (bitmap.getPixel(x, y) ushr 24 != 0) opaque++
            assertTrue("$name rendered nothing", opaque > 0)
        }
    }

    private companion object {
        /** 108 dp at 4x. */
        const val SIZE = 432
    }
}
