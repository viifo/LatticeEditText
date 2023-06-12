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
import androidx.core.content.ContextCompat


/**
 * 分格输入框
 */
class LatticeEditText @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : AppCompatTextView(context, attrs) {

    var textChangeListener: ((String?) -> Unit)? = null

    /** 输入的文本内容 */
    internal var mContent: String = ""
    /** 回显字符 */
    private var mReplaceText: String? = null
    /** 可输入字符数 */
    internal var mSize = 0
    /** 每个输入框的宽度 */
    private var mWide = 0f
    /** 每个输入框的高度 */
    private var mHigh = 0f
    /** 输入框外边距 */
    private var mMarginRect = Rect()
    /** 边框 */
    private val mBorderPaint: Paint = Paint()
    private var mBorderRadius = 0f
    private var mBorderWidth = 0f
    private var mBorderColor = 0
    internal var mInputMode = 0
    private val mBorderRectList: MutableList<RectF> = ArrayList()
    /** 背景 */
    private val mBackgroundPaint: Paint = Paint()
    private val mInputRectList: MutableList<RectF> = ArrayList()
    /** 文字 */
    private val mTextPaint: Paint = Paint()
    private val mTextRect = Rect()
    private val mTextPoint = Point()
    /** 光标 */
    private val mCursorPaint: Paint by lazy { Paint() }
    private val mCursorBackgroundPaint: Paint by lazy { Paint() }
    internal var mCursorMode = 1
    private var mCursorColor = 0
    private var mCursorBackground = 0
    internal var mShowCursor = true
    private var mCursorWidth = 0f
    private var mCursorHeight = 0f
    internal var mCursorOrientation = 0
    private var mCursorOffset = 0
    private var mCursorOffsetValue = 0
    internal var mSelection = 0
    private var mIsTwink = false
    private var mCursorBlink: CursorBlink? = null

    /** kotlin 相关参数获取 */
    val content: String get() = mContent

    /**
     * 初始化属性参数
     * @param context
     * @param attrs
     */
    @SuppressLint("CustomViewStyleable")
    private fun initAttrs(context: Context, attrs: AttributeSet?) {
        context.obtainStyledAttributes(attrs, R.styleable.LatticeEditText).apply {
            // background
            val backgroundColor = getColor(R.styleable.LatticeEditText_android_background, ContextCompat.getColor(context, R.color.latticeEditText_background))
            // 每个字符框占的宽度 & 高度
            mSize = getInteger(R.styleable.LatticeEditText_size, 4)
            mWide = getDimension(R.styleable.LatticeEditText_input_width, context.resources.getDimension(R.dimen.latticeEditText_dp36))
            mHigh = getDimension(R.styleable.LatticeEditText_input_height, context.resources.getDimension(R.dimen.latticeEditText_dp36))
            // 外边距
            val margin = getDimension(R.styleable.LatticeEditText_android_layout_margin, context.resources.getDimension(R.dimen.latticeEditText_dp4))
            mMarginRect = Rect(
                getDimension(R.styleable.LatticeEditText_android_layout_marginLeft, margin).toInt(),
                getDimension(R.styleable.LatticeEditText_android_layout_marginTop, margin).toInt(),
                getDimension(R.styleable.LatticeEditText_android_layout_marginRight, margin).toInt(),
                getDimension(R.styleable.LatticeEditText_android_layout_marginBottom, margin).toInt()
            )
            // text
            val textSize = getDimension(R.styleable.LatticeEditText_text_size, context.resources.getDimension(R.dimen.latticeEditText_sp14))
            val textColor = getColor(R.styleable.LatticeEditText_text_color, ContextCompat.getColor(context, R.color.latticeEditText_textColor))
            val text = getString(R.styleable.LatticeEditText_android_content) ?: ""
            val repText = getString(R.styleable.LatticeEditText_replace_text)
            // border
            mBorderColor = getColor(R.styleable.LatticeEditText_border_color, ContextCompat.getColor(context, R.color.latticeEditText_borderColor))
            mBorderWidth = getDimension(R.styleable.LatticeEditText_border_width, context.resources.getDimension(R.dimen.latticeEditText_dp1))
            mBorderRadius = getDimension(R.styleable.LatticeEditText_border_radius, 0f)
            mInputMode = getInteger(R.styleable.LatticeEditText_input_mode, 0)
            // cursor
            mCursorMode = getInteger(R.styleable.LatticeEditText_cursor_mode, 1)
            mCursorColor = getColor(R.styleable.LatticeEditText_cursor_color, ContextCompat.getColor(context, R.color.latticeEditText_cursorColor))
            mCursorBackground = getColor(R.styleable.LatticeEditText_cursor_background, backgroundColor)
            mShowCursor = getBoolean(R.styleable.LatticeEditText_show_cursor, true)
            mCursorWidth = getDimension(R.styleable.LatticeEditText_cursor_width, resources.getDimension(R.dimen.latticeEditText_dp1))
            mCursorHeight = getDimension(R.styleable.LatticeEditText_cursor_height, mHigh / 2)
            mCursorOrientation = getInteger(R.styleable.LatticeEditText_cursor_orientation, ORIENTATION_VERTICAL)

            // 边框画笔
            initBorderPaint()
            // 背景画笔
            initBackgroundPaint(backgroundColor)
            // 文字画笔
            initTextPaint(textSize, textColor)
            // 光标画笔
            initCursorPaint()
            // 始化输入框样式
            initInputMode()
            // 初始化文本显示
            setContent(text)
            // 设置回显字符
            setEchoChar(repText)
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
    private fun initBorderPaint() {
        mBorderPaint.isAntiAlias = true
        mBorderPaint.strokeWidth = mBorderWidth
        mBorderPaint.style = Paint.Style.STROKE //设置绘制轮廓
        mBorderPaint.color = mBorderColor
    }

    /**
     * 初始化背景画笔
     */
    private fun initBackgroundPaint(backgroundColor: Int) {
        mBackgroundPaint.style = Paint.Style.FILL
        mBackgroundPaint.color = backgroundColor
    }

    /**
     * 初始化文字画笔
     */
    private fun initTextPaint(size: Float, textColor: Int) {
        mTextPaint.isAntiAlias = true
        mTextPaint.textSize = size
        mTextPaint.style = Paint.Style.FILL
        mTextPaint.color = textColor
        mTextPaint.textAlign = Paint.Align.CENTER
        // 初始化光标偏移，避免光标遮挡文字
        mTextPaint.getTextBounds("A", 0, 1, mTextRect)
        mCursorOffsetValue = (mTextRect.width() / 2 + resources.getDimension(R.dimen.latticeEditText_dp2)).toInt()

        // 初始化文字绘制坐标
        // 配合 Paint.Align.CENTER 实现水平居中
        mTextPoint.x = (mWide / 2f).toInt()
        // 垂直居中
        val fontMetrics = mTextPaint.fontMetrics
        // 计算文字高度
        val fontHeight = fontMetrics.bottom - fontMetrics.top
        // 计算文字基线
        val baseline = mHigh - (mHigh - fontHeight) / 2f - fontMetrics.bottom
        mTextPoint.y = baseline.toInt()
    }

    /**
     * 初始化光标画笔
     */
    private fun initCursorPaint() {
        if (!mShowCursor) return
        mCursorPaint.isAntiAlias = true
        mCursorPaint.color = mCursorColor
        mCursorPaint.strokeWidth = mCursorWidth
        if (mCursorMode == MODE_CURSOR_LINE) {
            if (mCursorOrientation == ORIENTATION_HORIZONTAL) {
                mCursorPaint.style = Paint.Style.STROKE
                mCursorPaint.strokeWidth = mBorderWidth
            }else {
                mCursorPaint.style = Paint.Style.FILL
            }
        } else {
            mCursorPaint.style = Paint.Style.STROKE
            mCursorBackgroundPaint.isAntiAlias = true
            mCursorBackgroundPaint.style = Paint.Style.FILL
            mCursorBackgroundPaint.color = mCursorBackground
        }
    }

    /**
     * 初始化输入框样式
     */
    private fun initInputMode() {
        // 初始化输入框
        mInputRectList.clear()
        for (i in 0 until mSize) {
            val left = i * (mWide + mMarginRect.right) + (i + 1) * mMarginRect.left
            mInputRectList.add(RectF(left, mMarginRect.top.toFloat(), left + mWide, mHigh + mMarginRect.top))
        }
        // 初始化输入框边框
        mBorderRectList.clear()
        val borderTop = when(mInputMode) {
            // line
            MODE_INPUT_LINE -> mHigh + mMarginRect.top - mBorderPaint.strokeWidth
            // box
            else -> mMarginRect.top.toFloat()
        }
        for (i in 0 until mSize) {
            val left = i * (mWide + mMarginRect.right) + (i + 1) * mMarginRect.left
            mBorderRectList.add(RectF(left, borderTop, left + mWide, mHigh + mMarginRect.top))
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val width = (mSize * (mWide + mMarginRect.left + mMarginRect.right))
        val height = (mHigh + mMarginRect.top + mMarginRect.bottom)
        setMeasuredDimension(width.toInt(), height.toInt())
    }

    override fun onDraw(canvas: Canvas) {
        // 绘制背景&边框
        for (i in 0 until mSize) {
            canvas.drawRoundRect(mInputRectList[i], mBorderRadius, mBorderRadius, mBackgroundPaint)
            canvas.drawRoundRect(mBorderRectList[i], mBorderRadius, mBorderRadius, mBorderPaint)
        }
        // 绘制 box 光标
        drawBoxCursor(canvas)

        // 绘制文字
        // mSize.coerceAtMost(mContent.length) 保险措施，避免 mInputRectList 数组越界
        for (i in 0 until mSize.coerceAtMost(mContent.length)) {
            canvas.drawText(
                mReplaceText ?: mContent[i].toString(),
                mInputRectList[i].left + mTextPoint.x,
                mInputRectList[i].top + mTextPoint.y,
                mTextPaint
            )
        }

        // 绘制 line 光标
        drawLineCursor(canvas)
    }

    /**
     * 绘制 box 模式光标, 即盒子背景及边框，需在绘制文字前绘制
     * @param canvas
     */
    private fun drawBoxCursor(canvas: Canvas) {
        if (mCursorMode == MODE_CURSOR_BOX && shouldDrawCursor()) {
            canvas.drawRoundRect(mInputRectList[mSelection], mBorderRadius, mBorderRadius, mCursorBackgroundPaint)
            canvas.drawRoundRect(mBorderRectList[mSelection], mBorderRadius, mBorderRadius, mCursorPaint)
        }
    }

    /**
     * 绘制 line 模式光标
     * @param canvas
     */
    private fun drawLineCursor(canvas: Canvas) {
        if (mCursorMode == MODE_CURSOR_LINE && shouldDrawCursor()) {
            if (mCursorOrientation == ORIENTATION_HORIZONTAL && mInputMode == MODE_INPUT_LINE) {
                // 绘制水平下划线光标
                canvas.drawRoundRect(mBorderRectList[mSelection], mBorderRadius, mBorderRadius, mCursorPaint)
            } else {
                // 绘制竖直光标。 mIsTwink = true 时绘制光标，mIsTwink = false 时擦除光标 (使用背景色覆盖)
                val x = mInputRectList[mSelection].left + mWide / 2f + mCursorOffset
                canvas.drawLine(
                    x,
                    mInputRectList[mSelection].top + (mHigh - mCursorHeight) / 2f,
                    x,
                    mInputRectList[mSelection].top + mHigh - (mHigh - mCursorHeight) / 2f,
                    if (mIsTwink) mCursorPaint else mBackgroundPaint
                )
                mIsTwink = !mIsTwink
            }
        }
    }

    /**
     * 绘制竖直闪烁光标
     */
    internal fun drawBlinkCursor() {
        if (mSelection < mSize) {
            val left = mInputRectList[mSelection].left + mWide / 2f + mCursorOffset
            val top = mInputRectList[mSelection].top + (mHigh - mCursorHeight) / 2f
            val right = left + mCursorWidth
            val bottom = mInputRectList[mSelection].top + mHigh - (mHigh - mCursorHeight) / 2f
            // 更新光标区域
            postInvalidate(left.toInt(), top.toInt(),right.toInt(),bottom.toInt())
        }
    }

    private fun makeBlink() {
        if (shouldBlink()) {
            mCursorBlink?.let {
                it.cancel()
                removeCallbacks(it)
            }
            mCursorBlink = CursorBlink(this)
            postDelayed(mCursorBlink, BLINK.toLong())
        } else {
            mCursorBlink?.let {
                it.cancel()
                removeCallbacks(it)
            }
        }
    }

    /**
     * 是否应该绘制光标
     * @return Boolean
     */
    private fun shouldDrawCursor(): Boolean {
        return (mShowCursor && isFocused && mSelection < mSize)
    }

    /**
     * 是否应该绘制闪烁光标
     * @return Boolean
     */
    internal fun shouldBlink(): Boolean {
        return (mShowCursor
                && isFocused
                && mCursorMode == MODE_CURSOR_LINE
                && (mCursorOrientation == ORIENTATION_VERTICAL
                || mInputMode != MODE_INPUT_LINE))
    }

    /**
     * 点击 Item
     * @param index
     */
    private fun clickItem(index: Int) {
        // 选中输入位 | 选中最后一位
        if (index < mSize) {
            val length = mContent.length
            // 设置光标位置
            mSelection = if (length == mSize) length -1 else length
            // 设置光标偏移，避免光标遮挡文字
            mCursorOffset = if (length != mSize) 0 else mCursorOffsetValue
            invalidate()
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                run {
                    val index = (event.x / (mWide + mMarginRect.left + mMarginRect.right)).toInt()
                    clickItem(index)
                }
            }
            MotionEvent.ACTION_UP -> {
                performClick()
            }
        }
        return super.onTouchEvent(event)
    }

    override fun onFocusChanged(focused: Boolean, direction: Int, previouslyFocusedRect: Rect?) {
        super.onFocusChanged(focused, direction, previouslyFocusedRect)
        makeBlink()
    }

    override fun onCreateInputConnection(outAttrs: EditorInfo): InputConnection {
        // 文本删除监听
        return LatticeEditTextInputConnection(super.onCreateInputConnection(outAttrs), true).also {
            it.setTarget(super.onCreateInputConnection(outAttrs))
            it.onKeyDeletedDownListener = { removeChar() }
            it.onKeyEventUpListener = { text -> appendChar(text) }
        }
    }

    /**
     * 兼容部分键盘事件
     */
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return if (event != null && event.keyCode == KeyEvent.KEYCODE_DEL && event.action == KeyEvent.ACTION_DOWN) {
            removeChar()
            true
        } else {
            super.onKeyDown(keyCode, event)
        }
    }

    /**
     * 是否可编辑
     */
    override fun getDefaultEditable(): Boolean {
        return true
    }

    /**
     * 文本内容改变监听
     */
    fun setOnTextChangeListener(onTextChangeListener: ((String?) -> Unit)?) {
        this.textChangeListener = onTextChangeListener
    }

    /**
     * 文本内容改变监听
     * 兼容 java
     */
    fun setOnTextChangeListener(onTextChangeListener: OnTextChangeListener?) {
        this.textChangeListener = { text ->
            onTextChangeListener?.onTextChange(text)
        }
    }

    /**
     * 删除文本末尾字符
     */
    fun removeChar() {
        mContent.takeIf { it.isNotEmpty() }?.let {
            // 依次删除最后一位字符
            mContent = it.substring(0, it.length - 1)
            // 更新光标位置
            mSelection = mContent.length
            // 设置光标偏移，光标显示在输入框中间
            mCursorOffset = 0
            postInvalidate()
            textChangeListener?.invoke(mContent)
        }
    }

    /**
     * 添加一个字符到输入框
     * @param text - CharSequence
     */
    fun appendChar(text: CharSequence?) {
        text?.takeIf { it.isNotEmpty() }?.let {
            val length = mContent.length
            if (length + it.length <= mSize) {
                mContent += it
                if (mSelection + it.length == mSize) {
                    mSelection = mSize
                    // 设置光标偏移，避免光标遮挡文字
                    mCursorOffset = mCursorOffsetValue
                } else {
                    mSelection += it.length
                    // 设置光标偏移，光标显示在输入框中间
                    mCursorOffset = 0
                }
                postInvalidate()
                textChangeListener?.invoke(mContent)
            } else if (length < mSize) {
                mContent += it.substring(0, mSize - length)
                mSelection = mSize
                // 设置光标偏移，避免光标遮挡文字
                mCursorOffset = mCursorOffsetValue
                postInvalidate()
                textChangeListener?.invoke(mContent)
            } else {
                // do nothing
            }
        }
    }

    /**
     * 设置文本内容
     * @param text - 内容字符串
     */
    fun setContent(text: String?) {
        mContent = text?.takeIf { it.isNotEmpty() }?.let {
            if (it.length > mSize) it.substring(0, mSize) else it
        } ?: ""
    }

    /**
     * 设置回显字符
     * @param text - 字符
     */
    fun setEchoChar(text: String?) {
        mReplaceText = text?.takeIf { it.isNotEmpty() }?.substring(0, 1)
    }

    /**
     * 是否显示焦点
     */
    fun isCursorVisible(visible: Boolean) {
        mShowCursor = visible
        postInvalidate()
        makeBlink()
    }

    companion object {
        /** 光标刷新时间  */
        const val BLINK = 500
        /** 输入模式 */
        const val MODE_INPUT_BOX = 0
        const val MODE_INPUT_LINE = 1
        /** 光标模式 */
        const val MODE_CURSOR_BOX = 0
        const val MODE_CURSOR_LINE = 1
        const val ORIENTATION_HORIZONTAL = 0
        const val ORIENTATION_VERTICAL = 1
    }

    init {
        initAttrs(context, attrs)
        initSetting()
    }
}
