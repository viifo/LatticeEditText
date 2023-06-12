package com.viifo.latticeedittext

import java.lang.ref.WeakReference

/**
 * 光标绘制线程, 实现竖直闪烁光标效果
 */
internal class CursorBlink(view: LatticeEditText) : Runnable {

    private val weakView: WeakReference<LatticeEditText> = WeakReference(view)
    private var mCancelled: Boolean = false

    override fun run() {
        if (mCancelled) return
        weakView.get()?.let {
            it.removeCallbacks(this)
            if (it.shouldBlink()) {
                it.drawBlinkCursor()
                it.postDelayed(this, LatticeEditText.BLINK.toLong())
            }
        }
    }

    fun cancel() {
        if (!mCancelled) {
            weakView.get()?.removeCallbacks(this)
            mCancelled = true
        }
    }

}