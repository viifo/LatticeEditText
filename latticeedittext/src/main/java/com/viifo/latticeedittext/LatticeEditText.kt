package com.viifo.latticeedittext

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import androidx.appcompat.widget.AppCompatTextView
import java.util.ArrayList


/**
 * 分格输入框
 */
class LatticeEditText @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null)
    : AppCompatTextView(context, attrs) {

    var textChangeListener: ((String) -> Unit)? = null

    /** 输入的文本内容 */
    private var content: String? = null
    /** 回显字符 */
    private var replaceText: String? = null
    /** 可输入字符数 */
    private var size = 0
    /** 每个输入框的宽度 */
    private var wide = 0f
    /** 每个输入框的高度 */
    private var high = 0f
    /** 输入框内外边距 */
    private var marginRect = Rect()
    private var paddingRect = Rect()
    /** 边框 */
    private var borderPaint: Paint? = null
    private var borderRadius = 0f
    private val borderRects: MutableList<RectF> = ArrayList()
    /** 背景 */
    private var backgroundPaint: Paint? = null
    private val inputRects: MutableList<RectF> = ArrayList()
    /** 文字 */
    private var textPaint: Paint? = null
    private val textRect = Rect()
    private val textPoint = Point()
    /** 光标 */
    private var cursorPaint: Paint? = null
    private var isTwink = false
    private var cursorHeight = 0f
    private var cursorOffset = 0
    private var cursorOffsetValue = 0
    private var selection = 0
    private var prevRefreshTime: Long = 0

    /**
     * 初始化属性参数
     * @param context
     * @param attrs
     */
    @SuppressLint("CustomViewStyleable")
    private fun initAttrs(context: Context, attrs: AttributeSet?) {
        context.obtainStyledAttributes(attrs, R.styleable.LatticeEditText).apply {
            // background
            val backgroundColor = getColor(R.styleable.LatticeEditText_android_background, context.resources.getColor(R.color.latticeEditText_background))
            // 每个字符框占的宽度 & 高度
            size = getInteger(R.styleable.LatticeEditText_size, 4)
            wide = getDimension(R.styleable.LatticeEditText_input_width, context.resources.getDimension(R.dimen.latticeEditText_dp36))
            high = getDimension(R.styleable.LatticeEditText_input_height, context.resources.getDimension(R.dimen.latticeEditText_dp36))
            // 外边距
            val margin = getDimension(R.styleable.LatticeEditText_android_layout_margin, context.resources.getDimension(R.dimen.latticeEditText_dp4))
            marginRect = Rect(
                getDimension(R.styleable.LatticeEditText_android_layout_marginLeft, margin).toInt(),
                getDimension(R.styleable.LatticeEditText_android_layout_marginTop, margin).toInt(),
                getDimension(R.styleable.LatticeEditText_android_layout_marginRight, margin).toInt(),
                getDimension(R.styleable.LatticeEditText_android_layout_marginBottom, margin).toInt()
            )
            // 内边距
            val padding = getDimension(R.styleable.LatticeEditText_android_padding, 0f)
            paddingRect = Rect(getDimension(R.styleable.LatticeEditText_android_paddingLeft, padding).toInt(),
                getDimension(R.styleable.LatticeEditText_android_paddingTop, padding).toInt(),
                getDimension(R.styleable.LatticeEditText_android_paddingRight, padding).toInt(),
                getDimension(R.styleable.LatticeEditText_android_paddingBottom, padding).toInt()
            )
            // text
            val textSize = getDimension(R.styleable.LatticeEditText_android_textSize, context.resources.getDimension(R.dimen.latticeEditText_sp14))
            val textColor = getColor(R.styleable.LatticeEditText_android_textColor, context.resources.getColor(R.color.latticeEditText_textColor))
            val text = getString(R.styleable.LatticeEditText_android_text)
            val repText = getString(R.styleable.LatticeEditText_replace_text)
            // border
            val borderColor = getColor(R.styleable.LatticeEditText_border_color, context.resources.getColor(R.color.latticeEditText_borderColor))
            val borderWidth = getDimension(R.styleable.LatticeEditText_border_width, context.resources.getDimension(R.dimen.latticeEditText_dp1))
            borderRadius = getDimension(R.styleable.LatticeEditText_border_radius, 0f)
            val inputMode = getInteger(R.styleable.LatticeEditText_input_mode, 0)
            // cursor
            val cursorColor = getColor(R.styleable.LatticeEditText_cursor_color, context.resources.getColor(R.color.latticeEditText_cursorColor))
            val cursorWidth = getDimension(R.styleable.LatticeEditText_cursor_width, context.resources.getDimension(R.dimen.latticeEditText_dp2))
            cursorHeight = getDimension(R.styleable.LatticeEditText_cursor_height, high / 2)

            // 边框画笔
            initBorderPaint(borderWidth, borderColor)
            // 背景画笔
            initBackgroundPaint(backgroundColor)
            // 文字画笔
            initTextPaint(textSize, textColor)
            // 光标画笔
            initCursorPaint(context, cursorWidth, cursorColor)
            // 始化输入框样式
            initInputMode(inputMode)
            // 初始化文本显示
            setContent(text)
            // 设置回显字符
            setReplaceText(repText)

        }.recycle()
    }

    /**
     * 初始化设置
     */
    private fun initSetting() {
        isFocusable = true
        isFocusableInTouchMode = true
        isSingleLine = true
        imeOptions = EditorInfo.IME_ACTION_DONE
        // 不显示原光标
        isCursorVisible = false
        background = null
    }

    /**
     * 初始化边框画笔
     */
    private fun initBorderPaint(borderWidth: Float, borderColor: Int) {
        borderPaint = Paint().apply {
            isAntiAlias = true
            strokeWidth = borderWidth
            style = Paint.Style.STROKE //设置绘制轮廓
            color = borderColor
        }
    }

    /**
     * 初始化背景画笔
     */
    private fun initBackgroundPaint(backgroundColor: Int) {
        backgroundPaint = Paint().apply {
            style = Paint.Style.FILL
            color = backgroundColor
        }
    }

    /**
     * 初始化文字画笔
     */
    private fun initTextPaint(size: Float, textColor: Int) {
        textPaint = Paint().apply {
            isAntiAlias = true
            textSize = size
            style = Paint.Style.FILL
            color = textColor
            // 初始化光标偏移，避免光标遮挡文字
            getTextBounds("A", 0, 1, textRect)
            cursorOffsetValue = (textRect.width() / 2 + context.resources.getDimension(R.dimen.latticeEditText_dp2)).toInt()
        }
    }

    /**
     * 初始化光标画笔
     */
    private fun initCursorPaint(context: Context, cursorWidth: Float, cursorColor: Int) {
        cursorPaint = Paint().apply {
            isAntiAlias = true
            strokeWidth = cursorWidth
            color = cursorColor
        }
    }

    /**
     * 初始化输入框样式
     */
    private fun initInputMode(inputMode: Int) {
        // 初始化输入框
        inputRects.clear()
        for (i in 0 until size) {
            val left = i * (wide + marginRect.right) + (i + 1) * marginRect.left
            inputRects.add(RectF(left, marginRect.top.toFloat(), left + wide, high + marginRect.top))
        }
        // 初始化输入框边框
        borderRects.clear()
        val borderTop = when(inputMode) {
            // line
            1 -> high + marginRect.top - borderPaint!!.strokeWidth
            // box
            else -> marginRect.top.toFloat()
        }
        for (i in 0 until size) {
            val left = i * (wide + marginRect.right) + (i + 1) * marginRect.left
            borderRects.add(RectF(left, borderTop, left + wide, high + marginRect.top))
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val width = (size * (wide + marginRect.left + marginRect.right))
        val height = (high + marginRect.top + marginRect.bottom)
        setMeasuredDimension(width.toInt(), height.toInt())
    }

    override fun onDraw(canvas: Canvas) {
        // 绘制背景&边框
        for (i in 0 until size) {
            canvas.drawRoundRect(inputRects[i], borderRadius, borderRadius, backgroundPaint!!)
            canvas.drawRoundRect(borderRects[i], borderRadius, borderRadius, borderPaint!!)
        }
        // 绘制文字, 由于每个字符大小可能不一致，所以需要每个字符都进行测量
        val chars = content!!.toCharArray()
        for (i in chars.indices) {
            if (replaceText != null && chars[i].code != 32) {
                textPaint!!.getTextBounds(replaceText, 0, 1, textRect)
            } else {
                textPaint!!.getTextBounds(chars[i].toString(), 0, 1, textRect)
            }
            textPoint.x = (wide / 2 - textRect.width() / 2f + paddingRect.left).toInt()
            textPoint.y = (high / 2 + textRect.height() / 2f + paddingRect.top).toInt()
            canvas.drawText(
                (if (replaceText == null || chars[i].code == 32) chars[i].toString() else replaceText)!!,
                inputRects[i].left + textPoint.x,
                inputRects[i].top + textPoint.y,
                textPaint!!
            )
        }
        // 绘制光标
        if (isTwink && isFocused && selection < size) {
            val x = inputRects[selection].left + wide / 2 + cursorOffset
            canvas.drawLine(
                x,
                inputRects[selection].top + (high - cursorHeight) / 2,
                x,
                inputRects[selection].top + high - (high - cursorHeight) / 2, cursorPaint!!
            )
        }
        // 间隔刷新光标
        if (isFocused && System.currentTimeMillis() - prevRefreshTime >= defaultRefreshTime) {
            prevRefreshTime = System.currentTimeMillis()
            isTwink = !isTwink
        }
        // 间隔 500ms 重绘
        if (isFocused) {
            postInvalidateDelayed(500)
        }
    }

    override fun onTextChanged(text: CharSequence, start: Int, lengthBefore: Int, lengthAfter: Int) {
        // onTextChanged 在 TextView 的构造函数被调用，会比本view的初始化方法更早调用，因此 content 可能为空。
        if (lengthBefore < lengthAfter && content != null && content!!.length < size) {
            // 输入的字符没有达到最大长度
            // 此时要替换前面有误的字符
            if (content!!.length > selection) {
                content = replaceChat(content, getInputNewChar(text.toString(), start), selection)
                setText(content)
                textChangeListener?.invoke(content!!.replace(" ", ""))
                // 替换前面有误的字符后将光标移动要继续新增的位置
                selection = content!!.length
                cursorOffset = 0
            } else if (text.length <= size) {
                if (text.length <= 1) {
                    content = text.toString()
                } else {
                    content += getInputNewChar(text.toString(), start)
                }
                setText(content)
                textChangeListener?.invoke(content!!.replace(" ", ""))
                // 将光标移动要继续新增的位置
                selection = content!!.length
                // 设置光标偏移，光标显示在输入框中间
                cursorOffset = 0
            }
        } else if (lengthBefore < size && content != null && content!!.length == size && content!!.length > selection) {
            // 输入的字符已达到最大长度
            // 此时要替换有误的字符
            content = replaceChat(content, getInputNewChar(text.toString(), start), selection)
            setText(content)
            textChangeListener?.invoke(content!!.replace(" ", ""))
            if (selection < content!!.length - 1 && " " == getInputNewChar(content, selection + 1)) {
                // 后面暂无内容，移动光标到下一个输入框
                selection ++
                cursorOffset = 0
            } else {
                cursorOffset = cursorOffsetValue
            }
        }
        if (content != null && content!!.length == size && !content!!.contains(" ")) {
            // 输入完毕
            selection = content!!.length
            cursorOffset = 0
        }
        if (text.isEmpty()) {
            // 空内容
            selection = 0
        } else if (text.length > size) {
            // 内容超出范围
            setText(content)
        }
    }

    /**
     * 获取新输入的字符
     * @return
     */
    private fun getInputNewChar(text: String?, start: Int): String {
        return text!!.substring(start, start + 1)
    }

    /**
     * 替换字符
     * @return
     */
    private fun replaceChat(text: String?, charStr: String, selectionIndex: Int): String {
        var result = text!!.substring(0, selectionIndex)
        result += charStr
        result += text.substring(selectionIndex + 1)
        return result
    }

    /**
     * 删除已输入的文本内容
     */
    private fun deleteText() {
        if (content!!.isEmpty()) return
        if (content!!.length == selection) {
            // 删除最后一个字符
            content = content!!.substring(0, selection - 1)
            // 移动光标
            selection = content!!.length
        } else if (selection > 0 && " " == getInputNewChar(content, selection)) {
            // 当前位置已删除, 继续向前删除
            content = replaceChat(content, " ", selection - 1)
            // 移动光标
            selection--
        } else {
            // 删除其他部分的字符, 把要删除的部分替换为空格
            content = replaceChat(content, " ", selection)
        }
        // 设置光标偏移，光标显示在输入框中间
        cursorOffset = 0
        // 更新文本内容
        setText(content)
        postInvalidate()
        textChangeListener?.invoke(content!!.replace(" ", ""))
    }

    /**
     * 点击 Item
     * @param index
     */
    private fun clickItem(index: Int) {
        // 只能点击已输入|待输入的部分
        if (index <= content!!.length && index < size) {
            // 设置光标位置
            selection = index
            // 设置光标偏移，避免光标遮挡文字
            cursorOffset = if ((index == content!!.length)
                || (content!!.length > index + 1 && " " == getInputNewChar(content, index))) 0
                else cursorOffsetValue
            invalidate()
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                run {
                    val index = (event.x / (wide + marginRect.left + marginRect.right)).toInt()
                    clickItem(index)
                }
            }
            MotionEvent.ACTION_UP -> {
                performClick()
            }
        }
        return super.onTouchEvent(event)
    }

    override fun onCreateInputConnection(outAttrs: EditorInfo): InputConnection {
        // 文本删除监听
        return LatticeEditTextInputConnection(super.onCreateInputConnection(outAttrs), true).also {
            it.setTarget(super.onCreateInputConnection(outAttrs))
            it.onBeforeTextDeletedListener = {
                deleteText()
            }
        }
    }

    /**
     * 兼容部分键盘事件
     */
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return if (event != null && event.keyCode == KeyEvent.KEYCODE_DEL && event.action == KeyEvent.ACTION_DOWN) {
            deleteText()
            true
        } else {
            super.onKeyDown(keyCode, event)
        }
    }

    override fun getDefaultEditable(): Boolean {
        return true
    }

    fun setOnTextChangeListener(onTextChangeListener: ((String) -> Unit)?) {
        this.textChangeListener = onTextChangeListener
    }

    /**
     * 兼容 java
     */
    fun setOnTextChangeListener(onTextChangeListener: OnTextChangeListener?) {
        this.textChangeListener = {text ->
            onTextChangeListener?.onTextChange(text)
        }
    }

    /**
     * 设置文本内容
     * @param text - 内容字符串
     */
    fun setContent(text: String?) {
        content = text?.let {
            (if (text.length > size) text.substring(0, size) else text).also { selection = it.length }
        } ?: ""
        setText(content)
    }

    /**
     * 设置回显字符
     * @param text - 字符
     */
    fun setReplaceText(text: String?) {
        replaceText = if (text != null && text.isNotEmpty()) text.substring(0, 1) else null
    }

    companion object {
        /** 光标刷新时间  */
        private const val defaultRefreshTime = 500
    }

    init {
        initAttrs(context, attrs)
        initSetting()
    }
}
