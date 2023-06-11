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

    /** 软键盘删除按键按下监听 */
    var onKeyDeletedDownListener: (()->Unit)? = null

    /** 软键盘字母和数字按键抬起监听，除删除按键外的其他软键盘按键抬起时触发 */
    var onKeyEventUpListener: ((CharSequence?)->Unit)? = null

    /**
     * 删除文本之前调用
     * @param beforeLength
     * @param afterLength
     * @return
     */
    override fun deleteSurroundingText(beforeLength: Int, afterLength: Int): Boolean {
        return onKeyDeletedDownListener?.let {
                it.invoke()
                true
            } ?: super.deleteSurroundingText(beforeLength, afterLength)
    }

    /**
     * 点击软键盘按钮调用，如删除键
     * @param event
     * @return
     */
    override fun sendKeyEvent(event: KeyEvent): Boolean {
        return if (event.keyCode == KeyEvent.KEYCODE_DEL && event.action == KeyEvent.ACTION_DOWN) {
            onKeyDeletedDownListener?.invoke()
            true
        } else super.sendKeyEvent(event)
    }

    /**
     * 输入法提交输入文本内容到文本框时调用
     * @param text - 提交的文本内容
     * @param newCursorPosition - 新的光标位置
     */
    override fun commitText(text: CharSequence?, newCursorPosition: Int): Boolean {
        // return super.commitText(text, newCursorPosition)
        onKeyEventUpListener?.invoke(text)
        // 返回 true 表示已处理，此时绑定的控件不会在回调 onTextChanged 等方法
        return true
    }

}