package com.sma6871.cardentry

import android.animation.Animator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.os.Build
import android.text.*
import android.text.method.DigitsKeyListener
import android.util.AttributeSet
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.appcompat.widget.AppCompatEditText
import androidx.core.content.ContextCompat
import java.util.*

class CardEntry : AppCompatEditText {

    var maxLength = 16 // default length
    var partsCount = 4 // AAAA BBBB CCCC DDDD
    private var mSpace = toPxF(16)
    private var mCharSize = toPxF(16)
    private var mPartLength = maxLength / partsCount
    private var mPartSize = mCharSize * mPartLength
    private var mLineSpacing = toPxF(12)
    private var mLineSpacingAnimated = toPxF(12)

    private var textWidths = FloatArray(maxLength)

    var hasAnimation = false
    var hasLine = true
    private var isAnimating = false
    private var animatedAlpha = 255


    var linePaint: Paint = Paint().apply {
        isAntiAlias = true
        color = getColor(R.color.silverGray)
        style = Paint.Style.FILL
    }

    var textPaint: TextPaint = TextPaint().apply {
        isAntiAlias = true
        color = getColor(R.color.coal)
        textSize = spToPxF(18)
    }


    var lineColor = getColor(R.color.silverGray)
    var filledLineColor = getColor(R.color.green)


    fun onPinChange(onChange: (isComplete: Boolean, length: Int) -> Unit) {
        addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) {
                onChange(s.length == maxLength, s.length)

            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

            }

        })
    }

    constructor(context: Context) : super(context) {
        init(context, null)
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init(context, attrs)
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init(context, attrs)
    }

    private fun init(context: Context, attrs: AttributeSet?) {

        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.CardEntry, 0, 0)

        if (typedArray.hasValue(R.styleable.CardEntry_ce_number_count)) {
            maxLength = typedArray.getInt(R.styleable.CardEntry_ce_number_count, 4)
            textWidths = FloatArray(maxLength)
        }

        if (typedArray.hasValue(R.styleable.CardEntry_ce_line_color)) {
            lineColor = typedArray.getColor(
                    R.styleable.CardEntry_ce_line_color,
                    ContextCompat.getColor(context, R.color.silverGray)
            )
        }

        if (typedArray.hasValue(R.styleable.CardEntry_ce_text_color))
            textPaint.color = typedArray.getInt(
                    R.styleable.CardEntry_ce_text_color,
                    ContextCompat.getColor(context, R.color.coal)
            )

        if (typedArray.hasValue(R.styleable.CardEntry_ce_filled_line_color))
            filledLineColor = typedArray.getInt(
                    R.styleable.CardEntry_ce_filled_line_color,
                    ContextCompat.getColor(context, R.color.coal)
            )

        if (typedArray.hasValue(R.styleable.CardEntry_ce_has_animation))
            hasAnimation = typedArray.getBoolean(R.styleable.CardEntry_ce_has_animation, false)

        if (typedArray.hasValue(R.styleable.CardEntry_ce_has_line))
            hasLine = typedArray.getBoolean(R.styleable.CardEntry_ce_has_line, true)

        if (typedArray.hasValue(R.styleable.CardEntry_ce_digit_size))
            textPaint.textSize = typedArray.getDimension(R.styleable.CardEntry_ce_digit_size, textPaint.textSize)

        if (typedArray.hasValue(R.styleable.CardEntry_ce_digit_space))
            mSpace = typedArray.getDimension(R.styleable.CardEntry_ce_digit_space, mSpace)

        if (typedArray.hasValue(R.styleable.CardEntry_ce_digit_width))
            mCharSize = typedArray.getDimension(R.styleable.CardEntry_ce_digit_width, mCharSize)

        if (typedArray.hasValue(R.styleable.CardEntry_ce_digit_line_spacing)) {
            mLineSpacingAnimated = typedArray.getDimension(R.styleable.CardEntry_ce_digit_line_spacing, toPxF(12))
            mLineSpacing = typedArray.getDimension(R.styleable.CardEntry_ce_digit_line_spacing, toPxF(12))
        }


        typedArray.recycle()

        setBackgroundResource(0)
        setTextIsSelectable(false)
        isCursorVisible = false
        inputType = InputType.TYPE_CLASS_NUMBER
        keyListener = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            DigitsKeyListener.getInstance(Locale.US)
        } else {
            DigitsKeyListener.getInstance()
        }

        val lengthFilter = InputFilter.LengthFilter(maxLength)
        filters = arrayOf<InputFilter>(lengthFilter)

        //Disable copy paste
        super.setCustomSelectionActionModeCallback(object : ActionMode.Callback {
            override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
                return false
            }

            override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
                return false
            }

            override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
                return false
            }

            override fun onDestroyActionMode(mode: ActionMode?) {
            }

        })

        addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {

            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                if (hasAnimation) {
                    if (start == s!!.length && !isAnimating) {
                        animate1()
                    }
                }
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

        })

        mLineSpacingAnimated = if (hasAnimation) 0f else mLineSpacing

    }

    override fun onDraw(canvas: Canvas) {
        //super.onDraw(canvas)

        setWillNotDraw(false)
        var startX = paddingLeft
        val top = height - paddingBottom

        val charSequence = text as CharSequence
        val textLength = charSequence.length
        textPaint.getTextWidths(charSequence, 0, textLength, textWidths)

        //draw lines
        var i = 0
        if (hasLine) {
            while (i < mPartLength) {
                linePaint.color = when {
                    i < textLength / mPartLength -> filledLineColor
                    else -> lineColor
                }
                canvas.drawRect(
                        startX.toFloat(),
                        top.toFloat() + 0,
                        startX + mPartSize,
                        (top + toPxF(2)),
                        linePaint
                )

                startX += (mPartSize + mSpace).toInt()
                i++
            }
        }

        //draw characters
        startX = paddingLeft
        i = 0
        if (!hasAnimation) {
            while (i < textLength) {
                val middle = startX + mCharSize / 2
                drawNumber(canvas, charSequence, i, middle, top, false)

                startX += if (i % mPartLength == mPartLength - 1)
                    (mCharSize + mSpace).toInt()
                else (mCharSize).toInt()
                i++
            }
        } else {//last character must be animate

            for (k in 0 until textLength) {
                val middle = startX + mCharSize / 2
                if ((k < textLength - 1)) {
                    drawNumber(canvas, charSequence, k, middle, top, false)
                    startX += if (k % mPartLength == mPartLength - 1)
                        (mCharSize + mSpace).toInt()
                    else (mCharSize).toInt()
                } else {
                    drawNumber(canvas, charSequence, k, middle, top, true)
                }

            }

        }
    }

    private fun drawNumber(canvas: Canvas, text: CharSequence, i: Int, middle: Float, top: Int, animated: Boolean) {
        if (animated) {
            textPaint.alpha = animatedAlpha
        } else {
            textPaint.alpha = 255
        }
        canvas.drawText(
                text,
                i,
                i + 1,
                middle - textWidths[i] / 2,
                top - if (animated) mLineSpacingAnimated else mLineSpacing,
                textPaint
        )
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        setMeasuredDimension(
                (maxLength * mCharSize).toInt() + ((partsCount - 1) * mSpace).toInt() + paddingLeft + paddingRight,
                measuredHeight
        )

    }

    private fun animate1() {
        val valueAnimator = ValueAnimator.ofFloat(0F, mLineSpacing)
        valueAnimator.duration = 200
        valueAnimator.interpolator = AccelerateDecelerateInterpolator()
        valueAnimator.addUpdateListener {
            mLineSpacingAnimated = it.animatedValue as Float
            animatedAlpha = ((it.animatedValue as Float) / mLineSpacing * 255).toInt()
            postInvalidate()
        }
        valueAnimator.addListener(object : Animator.AnimatorListener {
            override fun onAnimationRepeat(animation: Animator?) {

            }

            override fun onAnimationEnd(animation: Animator?) {
                // mLineSpacingAnimated = 0F
                isAnimating = false
            }

            override fun onAnimationCancel(animation: Animator?) {
            }

            override fun onAnimationStart(animation: Animator?) {
                isAnimating = true
            }

        })
        valueAnimator.start()
    }


}