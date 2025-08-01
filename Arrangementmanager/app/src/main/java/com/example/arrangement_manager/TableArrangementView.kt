package com.example.arrangement_manager

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.MotionEvent
import android.widget.Toast
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

    // Stati per la gestione del tocco
    private var tables: List<Table_> = emptyList()
    private var selectedTable: Table_? = null
    private var lastX: Float = 0f
    private var lastY: Float = 0f

    // Listener che il Fragment imposta per ricevere le modifiche
    var onTableUpdatedListener: OnTableUpdatedListener? = null

    // Enum per lo stato del tocco
    private enum class Mode {
        NONE, DRAG, RESIZE
    }
    private var mode:  Mode = Mode.NONE

    // Oggetto Paint per disegnare i rettangoli dei tavoli
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

    private val reusableRect = RectF()


    fun setTables(newTables: List<Table_>) {
        this.tables = newTables
        selectedTable = tables.find { it.name == selectedTable?.name }
        // Invalida la vista per forzare la ridisegnazione con la nuova lista di tavoli.
        invalidate()
    }

    // metodo per disegnare la vista
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

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
            // Disegna il bordo del rettangolo
            if(table.name == selectedTable?.name) {
                canvas.drawRect(reusableRect, selectedTablePaint)
            }
        }
    }

    // Gestione degli eventi touch
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        when(event?.action) {
            MotionEvent.ACTION_DOWN -> {
                mode = Mode.NONE
                selectedTable = findTableAtPosition(event.x, event.y)
                if(selectedTable != null) {
                    if(isNearResizeHandle(selectedTable!!, event.x, event.y)) {
                        mode = Mode.RESIZE
                    } else {
                        mode = Mode.DRAG
                    }
                }
                lastX = event.x
                lastY = event.y
                invalidate()
                return selectedTable != null
            }
            MotionEvent.ACTION_MOVE -> {
                if(selectedTable != null && mode != Mode.NONE) {
                    val deltaX = event.x - lastX
                    val deltaY = event.y - lastY
                    val updatedTable = when(mode) {
                        Mode.DRAG -> selectedTable!!.copy(
                            x_coordinate = selectedTable!!.x_coordinate + deltaX,
                            y_coordinate = selectedTable!!.y_coordinate + deltaY
                        )
                        Mode.RESIZE -> selectedTable!!.copy(
                            width = selectedTable!!.width + deltaX,
                            height = selectedTable!!.height + deltaY
                        )
                        else -> selectedTable
                    }
                    tables = tables.map { (if (it.name == updatedTable?.name) updatedTable else it) }
                    selectedTable = updatedTable
                    lastX = event.x
                    lastY = event.y
                    invalidate()
                    return true
                }
            }
            MotionEvent.ACTION_UP -> {
                if(selectedTable != null && onTableUpdatedListener != null) {
                    // comunica al fragment che il tavolo è stato aggiornato
                    onTableUpdatedListener?.onTableUpdated(selectedTable!!)
                    Toast.makeText(context, "Tavolo aggiornato", Toast.LENGTH_SHORT).show()
                }
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
        val handleSize = 50f
        val right = table.x_coordinate + table.width
        val bottom = table.y_coordinate + table.height
        return x >= right - handleSize && y >= bottom - handleSize
    }
}