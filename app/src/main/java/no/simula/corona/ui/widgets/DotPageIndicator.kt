package no.simula.corona.ui.widgets

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import no.simula.corona.R

class DotPageIndicator : View {

    private var selected = 0
    var totalDots = 0
    var selectedColor = 0
    var unSelectedColor = 0

    var selectedPaint: Paint? = null
    var unSelectedPaint: Paint? = null


    constructor(context: Context?) : super(context) {
        initialize(null, 0)
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(
        context,
        attrs
    ) {
        initialize(attrs, 0)
    }

    constructor(
        context: Context?, attrs: AttributeSet?,
        defStyleAttr: Int
    ) : super(context, attrs, defStyleAttr) {
        initialize(attrs, defStyleAttr)
    }

    fun initialize(attrs: AttributeSet?, defStyle: Int) {

        val a = context.obtainStyledAttributes(
            attrs, R.styleable.DotPageIndicator, defStyle, 0
        )
        selectedColor = a.getColor(
            R.styleable.DotPageIndicator_selected_color,
            ContextCompat.getColor(context, R.color.selected_color)
        )
        unSelectedColor = a.getColor(
            R.styleable.DotPageIndicator_unselected_color,
            ContextCompat.getColor(context, R.color.unselected_color)
        )

        totalDots = a.getColor(R.styleable.DotPageIndicator_totalDots, 3)

        a.recycle()

        selectedPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        selectedPaint!!.color = selectedColor

        unSelectedPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        unSelectedPaint!!.color = unSelectedColor
    }


    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val height = height
        val cy = height / 2
        val radius = height / 4

        val dotWidth = width / totalDots
        for (i in 0 until totalDots) {
            val cx = dotWidth / 2 + dotWidth * i
            if (i == selected) {
                canvas.drawCircle(cx.toFloat(), cy.toFloat(), radius.toFloat(), selectedPaint!!)
            } else {
                canvas.drawCircle(cx.toFloat(), cy.toFloat(), radius.toFloat(), unSelectedPaint!!)
            }
        }
    }

    fun getSelected() = selected

    fun setSelected(selected: Int) {
        this.selected = selected
        invalidate()
    }
}