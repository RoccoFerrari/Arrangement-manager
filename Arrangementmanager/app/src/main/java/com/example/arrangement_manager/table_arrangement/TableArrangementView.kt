package com.example.arrangement_manager.table_arrangement

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
import androidx.core.graphics.withMatrix
import com.example.arrangement_manager.retrofit.Table

// Interfaccia di callback per comunicare le modifiche a un tavolo
interface OnTableUpdatedListener {
    fun onTableUpdated(table: Table)
}

interface OnTableClickedListener {
    fun onTableClicked(table: Table)
}


class TableArrangementView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr){

    // ----------------------------------------
    // Variabili per la gestione dei tavoli
    private var tables: List<Table> = emptyList()
    private var selectedTable: Table? = null
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
    var onTableClickedListener: OnTableClickedListener? = null

    // ----------------------------------------
    // Enum per lo stato del tocco
    private enum class Mode {
        NONE, DRAG, RESIZE, PAN
    }
    private var mode: Mode = Mode.NONE

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
            mode = Mode.NONE
            return true
        }
        override fun onScaleEnd(detector: ScaleGestureDetector) {
            super.onScaleEnd(detector)
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

    // Gestore per pan e click
    private val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        // Gestione del pan
        override fun onScroll(e1: MotionEvent?, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
            if(!isScaling && !isEditMode) {
                matrix.postTranslate(-distanceX, -distanceY)
                invalidate()
                return true
            }
            return false
        }

        // Gestione del tocco singolo
        override fun onSingleTapUp(e: MotionEvent): Boolean {
            // Calcola la matrice inversa per convertire le coordinate del tocco
            matrix.invert(inverseMatrix)
            val transformedPoints = floatArrayOf(e.x, e.y)
            inverseMatrix.mapPoints(transformedPoints)
            val transformedX = transformedPoints[0]
            val transformedY = transformedPoints[1]

            val tappedTable = findTableAtPosition(transformedX, transformedY)
            if (tappedTable != null) {
                if (isEditMode) {
                    selectedTable = tappedTable
                    invalidate()
                } else {
                    // Chiama il listener del click solo se non siamo in modalità di modifica
                    onTableClickedListener?.onTableClicked(tappedTable)
                }
                return true
            }
            // Deseleziona il tavolo se il tocco è su un'area vuota
            if (isEditMode) {
                selectedTable = null
                invalidate()
            }
            return false
        }
    })


    // ----------------------------------------
    // Metodi per aggiornare la View
    // ----------------------------------------

    // Metodo per aggiornare la lista dei tavoli
    fun setTables(newTables: List<Table>) {
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

    fun getSelectedTable() : Table? {
        return selectedTable
    }

    // metodo per disegnare la vista
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Applica trasformazioni di zoom e pan
        canvas.withMatrix(matrix) {
            // Itera sulla lista dei tavoli e disegna ogni rettangolo
            tables.forEach { table ->
                reusableRect.set(
                    table.xCoordinate.toFloat(),
                    table.yCoordinate.toFloat(),
                    table.xCoordinate.toFloat() + table.width.toFloat(),
                    table.yCoordinate.toFloat() + table.height.toFloat()
                )
                // Disegna il rettangolo
                drawRect(reusableRect, tablePaint)
                val textX = reusableRect.centerX()
                val textY =
                    reusableRect.centerY() - ((tableText.descent() + tableText.ascent()) / 2)
                drawText(table.name, textX, textY, tableText)
                // Disegna il bordo e il maniglione del rettangolo
                if (isEditMode && table.name == selectedTable?.name) {
                    drawRect(reusableRect, selectedTablePaint)

                    val handleX = reusableRect.right
                    val handleY = reusableRect.bottom
                    drawCircle(handleX, handleY, handleRadius, resizeHandlePaint)
                }
            }
        }
    }

    // Gestione degli eventi touch
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        scaleGestureDetector.onTouchEvent(event!!)

        // Se lo zoom è in corso, non fare null'altro.
        if (isScaling) {
            return true
        }

        // Gestisci il pan solo se NON stai facendo un drag o resize
        if (!isEditMode) {
            gestureDetector.onTouchEvent(event)
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
                            xCoordinate = selectedTable!!.xCoordinate + deltaX,
                            yCoordinate = selectedTable!!.yCoordinate + deltaY
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
                lastX = 0f
                lastY = 0f
                invalidate()
                return true
            }
        }
        return false
    }

    private fun findTableAtPosition(x: Float, y: Float): Table? {
        for(i in tables.indices.reversed()) {
            val table = tables[i]
            if(x >= table.xCoordinate && x <= table.xCoordinate + table.width &&
                y >= table.yCoordinate && y <= table.yCoordinate + table.height) {
                return table
            }
        }
        return null
    }

    private fun isNearResizeHandle(table: Table, x: Float, y: Float): Boolean {
        val hitArea = RectF(
            table.xCoordinate + table.width - handleRadius,
            table.yCoordinate + table.height - handleRadius,
            table.xCoordinate + table.width + handleRadius,
            table.yCoordinate + table.height + handleRadius
        )
        return hitArea.contains(x, y)
    }
}