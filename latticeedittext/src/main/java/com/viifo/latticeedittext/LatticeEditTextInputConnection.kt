package com.viifo.latticeedittext

import android.view.KeyEvent
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputConnectionWrapper

/**
 * 删除事件监听
 * @author Viifo
 */
class LatticeEditTextInputConnection (target: InputConnection?, mutable: Boolean)

    /**
     * Initializes a wrapper.
     *
     *
     * **Caveat:** Although the system can accept `(InputConnection) null` in some
     * places, you cannot emulate such a behavior by non-null [InputConnectionWrapper] that
     * has `null` in `target`.
     *
     * @param target  the [InputConnection] to be proxied.
     * @param mutable set `true` to protect this object from being reconfigured to target
     * another [InputConnection].  Note that this is ignored while the target is `null`.
     */
    : InputConnectionWrapper(target, mutable) {

    var onBeforeTextDeletedListener: (()->Unit)? = null

    /**
     * 删除文本之前调用
     * @param beforeLength
     * @param afterLength
     * @return
     */
    override fun deleteSurroundingText(beforeLength: Int, afterLength: Int): Boolean {
        return onBeforeTextDeletedListener?.let {
                it.invoke()
                true
            } ?: super.deleteSurroundingText(beforeLength, afterLength)
    }

    /**
     * 点击软键盘按钮调用
     * @param event
     * @return
     */
    override fun sendKeyEvent(event: KeyEvent): Boolean {
        return if (event.keyCode == KeyEvent.KEYCODE_DEL && event.action == KeyEvent.ACTION_DOWN) {
            onBeforeTextDeletedListener?.invoke()
            true
        } else super.sendKeyEvent(event)
    }

}