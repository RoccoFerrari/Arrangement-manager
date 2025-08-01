package com.example.arrangement_manager

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import androidx.core.graphics.toColorInt

// Interfaccia di callback per comunicare le modifiche a un tavolo
interface OnTableUpdatedListener {
    fun onTableUpdated(table: Table_)
}


class TableArrangementView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr){

    // ----------------------------------------
    // Variabili per la gestione dei tavoli
    private var tables: List<Table_> = emptyList()
    private var selectedTable: Table_? = null
    private var lastX: Float = 0f
    private var lastY: Float = 0f
    private val handleRadius = 25f
    private val reusableRect = RectF()
    private val matrix = Matrix()
    private var isScaling = false
    private var inverseMatrix = Matrix()

    // Stato per EditMode
    private var isEditMode = false

    // ----------------------------------------
    // Listener che il Fragment imposta per ricevere le modifiche
    var onTableUpdatedListener: OnTableUpdatedListener? = null

    // ----------------------------------------
    // Enum per lo stato del tocco
    private enum class Mode {
        NONE, DRAG, RESIZE, PAN
    }
    private var mode:  Mode = Mode.NONE

    // ----------------------------------------
    // Oggetti Paint per disegnare i tavoli
    private val tablePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = "#42A5F5".toColorInt()
    }
    private val tableText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 24f
        textAlign = Paint.Align.CENTER
    }

    // Oggetto Paint per disegnare il bordo dei tavoli selezionati
    private val selectedTablePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f
        color = Color.RED
    }

    private val resizeHandlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.BLUE
    }

    // ----------------------------------------
    // Variabili e metodi per gestire lo zoom
    // -----------------------------------  -----

    // Gestore dello zoom
    private val scaleGestureDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScaleBegin(detector: ScaleGestureDetector) : Boolean {
            isScaling = true
            return true
        }
        override fun onScaleEnd(detector: ScaleGestureDetector) {
            isScaling = false
        }

        override fun onScale(detector: ScaleGestureDetector): Boolean {
            val scaleFactor = detector.scaleFactor
            // scaleFactor = 1 + (scaleFactor - 1) * 0.5f
            val focusX = detector.focusX
            val focusY = detector.focusY

            // Pre-moltiplica la matrice per trasare il punto focale sull'orgine
            // applicare la scala e poi traslare nuovamente il punto focale alla sua
            // posizione originale
            matrix.postScale(scaleFactor, scaleFactor, focusX, focusY)
            invalidate()
            return true
        }
    })

    // Gestore del pan
    private val panGestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onScroll(e1: MotionEvent?, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
            // Tralazione per il pan
            matrix.postTranslate(-distanceX, -distanceY)
            invalidate()
            return true
        }
    })


    // ----------------------------------------
    // Metodi per aggiornare la View
    // ----------------------------------------

    // Metodo per aggiornare la lista dei tavoli
    fun setTables(newTables: List<Table_>) {
        this.tables = newTables
        if(isEditMode)
            selectedTable = tables.find { it.name == selectedTable?.name }
        else
            selectedTable = null
        // Invalida la vista per forzare la ridisegnazione con la nuova lista di tavoli.
        invalidate()
    }

    fun setEditMode(enable: Boolean) {
        isEditMode = enable
        if(!enable) {
            selectedTable = null
        }
        invalidate()
    }

    fun clearSelection() {
        selectedTable = null
        invalidate()
    }

    fun getSelectedTable() : Table_? {
        return selectedTable
    }

    // metodo per disegnare la vista
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Applica trasformazioni di zoom e pan
        canvas.save()
        canvas.concat(matrix)

        // Itera sulla lista dei tavoli e disegna ogni rettangolo
        tables.forEach { table ->
            reusableRect.set(
                table.x_coordinate.toFloat(),
                table.y_coordinate.toFloat(),
                table.x_coordinate.toFloat() + table.width.toFloat(),
                table.y_coordinate.toFloat() + table.height.toFloat()
            )
            // Disegna il rettangolo
            canvas.drawRect(reusableRect, tablePaint)
            val textX = reusableRect.centerX()
            val textY = reusableRect.centerY() - ((tableText.descent() + tableText.ascent()) / 2)
            canvas.drawText(table.name, textX, textY, tableText)
            // Disegna il bordo e il maniglione del rettangolo
            if(isEditMode && table.name == selectedTable?.name) {
                canvas.drawRect(reusableRect, selectedTablePaint)

                val handleX = reusableRect.right
                val handleY = reusableRect.bottom
                canvas.drawCircle(handleX, handleY, handleRadius, resizeHandlePaint)
            }
        }
        // ripristina la tela allo stato originale
        canvas.restore()
    }

    // Gestione degli eventi touch
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        scaleGestureDetector.onTouchEvent(event!!)

        // E' in corso uno zoom?
        if(isScaling)
            return true

        // Gesto multitouch?
        if((event.pointerCount ?: 0) > 1) {
            return true
        }

        val currentX = event.x
        val currentY = event.y

        // Calcola la matrice inversa per convertire le coordinate del tocco
        matrix.invert(inverseMatrix)
        val trasformedPoints = floatArrayOf(currentX, currentY)
        inverseMatrix.mapPoints(trasformedPoints)
        val transformedX = trasformedPoints[0]
        val transformedY = trasformedPoints[1]

        if(!isEditMode) {
            panGestureDetector.onTouchEvent(event)
            if(event.action == MotionEvent.ACTION_DOWN) {
                selectedTable = null
                invalidate()
            }
            return true
        }

        when(event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                selectedTable = findTableAtPosition(transformedX, transformedY)
                if(selectedTable != null) {
                    if (isNearResizeHandle(selectedTable!!, transformedX, transformedY)) {
                        mode = Mode.RESIZE
                    } else {
                        mode = Mode.DRAG
                    }
                } else {
                    mode = Mode.PAN
                }
                lastX = currentX
                lastY = currentY
                invalidate()
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val deltaX = currentX - lastX
                val deltaY = currentY - lastY

                when(mode) {
                    Mode.DRAG -> {
                        val updatedTable = selectedTable!!.copy(
                            x_coordinate = selectedTable!!.x_coordinate + deltaX,
                            y_coordinate = selectedTable!!.y_coordinate + deltaY
                        )
                        tables = tables.map { (if (it.name == updatedTable.name) updatedTable else it) }
                        selectedTable = updatedTable
                        invalidate()
                    }
                    Mode.RESIZE -> {
                        val updatedTable = selectedTable!!.copy(
                            width = (selectedTable!!.width + deltaX).coerceAtLeast(50f),
                            height = (selectedTable!!.height + deltaY).coerceAtLeast(50f)
                        )
                        tables = tables.map { (if (it.name == updatedTable?.name) updatedTable else it) }
                        selectedTable = updatedTable
                        invalidate()
                    }
                    Mode.PAN -> {
                        matrix.postTranslate(deltaX, deltaY)
                        invalidate()
                    }
                    Mode.NONE -> {}
                }
                lastX = currentX
                lastY = currentY
                return true
            }
            MotionEvent.ACTION_UP -> {
                if(selectedTable != null && onTableUpdatedListener != null) {
                    // comunica al fragment che il tavolo è stato aggiornato
                    onTableUpdatedListener?.onTableUpdated(selectedTable!!)
                }
                mode = Mode.NONE
                invalidate()
                return true
            }
        }
        return false
    }

    private fun findTableAtPosition(x: Float, y: Float): Table_? {
        for(i in tables.indices.reversed()) {
            val table = tables[i]
            if(x >= table.x_coordinate && x <= table.x_coordinate + table.width &&
                y >= table.y_coordinate && y <= table.y_coordinate + table.height) {
                return table
            }
        }
        return null
    }

    private fun isNearResizeHandle(table: Table_, x: Float, y: Float): Boolean {
        val hitArea = RectF(
            table.x_coordinate + table.width - handleRadius,
            table.y_coordinate + table.height - handleRadius,
            table.x_coordinate + table.width + handleRadius,
            table.y_coordinate + table.height + handleRadius
        )
        return hitArea.contains(x, y)
    }
}