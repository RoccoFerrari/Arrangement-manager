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

/**
 * Callback interface to notify when a table is updated.
 */
interface OnTableUpdatedListener {
    /**
     * Called when a table is moved or resized.
     * @param table The [Table] object with the updated data.
     */
    fun onTableUpdated(table: Table)
}

/**
 * Callback interface to notify when a table is clicked.
 */
interface OnTableClickedListener {
    /**
     * Called when a single tap occurs on a table.
     * @param table The [Table] object that was clicked.
     */
    fun onTableClicked(table: Table)
}

/**
 * A custom view for displaying and interacting with a list of tables.
 *
 * It allows users to view, drag, resize, zoom, and pan the table arrangement area.
 * Interactions are handled based on an edit mode.
 *
 * @param context The context of the view.
 * @param attrs The XML attribute set.
 * @param defStyleAttr A default style attribute.
 */
class TableArrangementView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr){

    // ----------------------------------------
    // Variables for table management
    private var tables: List<Table> = emptyList()
    private var selectedTable: Table? = null
    private var lastX: Float = 0f
    private var lastY: Float = 0f
    private val handleRadius = 25f
    private val reusableRect = RectF()
    private val matrix = Matrix()
    private var isScaling = false
    private var inverseMatrix = Matrix()

    private var isEditMode = false

    // ----------------------------------------
    // Listener that the Fragment sets to receive changes
    var onTableUpdatedListener: OnTableUpdatedListener? = null
    var onTableClickedListener: OnTableClickedListener? = null

    // ----------------------------------------
    // Enum for touch state
    private enum class Mode {
        NONE, DRAG, RESIZE, PAN
    }
    private var mode: Mode = Mode.NONE

    // ----------------------------------------
    // Paint objects for drawing tables
    private val tablePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = "#42A5F5".toColorInt()
    }
    private val tableText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 24f
        textAlign = Paint.Align.CENTER
    }

    // Paint object to draw the border of selected tables
    private val selectedTablePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f
        color = Color.RED
    }

    // Paint object to draw the resize handle
    private val resizeHandlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.BLUE
    }

    // ----------------------------------------
    // Variables and methods to manage zoom
    // -----------------------------------  -----

    // Zoom handler
    private val scaleGestureDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        /**
         * Called at the start of a scaling gesture.
         * Sets the scaling state.
         * @param detector The scale gesture detector.
         * @return `true` to continue handling the event, `false` otherwise.
         */
        override fun onScaleBegin(detector: ScaleGestureDetector) : Boolean {
            isScaling = true
            mode = Mode.NONE
            return true
        }

        /**
         * Called at the end of a scaling gesture.
         * Resets the scaling state.
         * @param detector The scale gesture detector.
         */
        override fun onScaleEnd(detector: ScaleGestureDetector) {
            super.onScaleEnd(detector)
            isScaling = false
        }

        /**
         * Called during a scaling gesture.
         * Applies the scale factor to the transformation matrix.
         * @param detector The scale gesture detector.
         * @return `true` to continue handling the event, `false` otherwise.
         */
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            val scaleFactor = detector.scaleFactor
            // scaleFactor = 1 + (scaleFactor - 1) * 0.5f
            val focusX = detector.focusX
            val focusY = detector.focusY

            matrix.postScale(scaleFactor, scaleFactor, focusX, focusY)
            invalidate()
            return true
        }
    })

    // Pan and click handler
    private val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {

        /**
         * Called during a scrolling gesture.
         * Translates the transformation matrix to simulate panning.
         * @param e1 The initial touch event.
         * @param e2 The current touch event.
         * @param distanceX The distance scrolled on the X axis.
         * @param distanceY The distance scrolled on the Y axis.
         * @return `true` if the event was consumed, `false` otherwise.
         */
        override fun onScroll(e1: MotionEvent?, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
            if(!isScaling && !isEditMode) {
                matrix.postTranslate(-distanceX, -distanceY)
                invalidate()
                return true
            }
            return false
        }

        /**
         * Called when a single tap occurs.
         * Selects a table if the tap is on it, otherwise deselects it.
         * If not in edit mode, it invokes the click listener.
         * @param e The touch event.
         * @return `true` if the event was consumed, `false` otherwise.
         */
        override fun onSingleTapUp(e: MotionEvent): Boolean {
            // Calculate the inverse matrix to convert the touch coordinates
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
                    // Calls the click listener only if we are not in edit mode
                    onTableClickedListener?.onTableClicked(tappedTable)
                }
                return true
            }
            // Deselect the table if the touch is on an empty area
            if (isEditMode) {
                selectedTable = null
                invalidate()
            }
            return false
        }
    })


    // ----------------------------------------
    // Methods to update the View
    // ----------------------------------------

    /**
     * Sets the list of tables to be drawn.
     *
     * Updates the internal list of tables and invalidates the view to force a redraw.
     * If edit mode is active, it tries to maintain the selection of the previous table.
     * @param newTables The new list of [Table] objects.
     */
    fun setTables(newTables: List<Table>) {
        this.tables = newTables
        if(isEditMode)
            selectedTable = tables.find { it.name == selectedTable?.name }
        else
            selectedTable = null
        // Invalidate the view to force it to redraw with the new table list
        invalidate()
    }

    /**
     * Enables or disables edit mode.
     *
     * In edit mode, users can drag and resize tables.
     * When disabled, the current selection is cleared.
     * @param enable `true` to enable the mode, `false` to disable it.
     */
    fun setEditMode(enable: Boolean) {
        isEditMode = enable
        if(!enable) {
            selectedTable = null
        }
        invalidate()
    }

    /**
     * Clears the selection of the currently selected table.
     */
    fun clearSelection() {
        selectedTable = null
        invalidate()
    }

    /**
     * Returns the currently selected [Table] object.
     * @return The selected [Table] object or `null` if no table is selected.
     */
    fun getSelectedTable() : Table? {
        return selectedTable
    }

    /**
     * The drawing method for the view.
     *
     * Applies zoom and pan transformations and draws each table. If a table is
     * selected and edit mode is active, it also draws a border and a resize handle.
     * @param canvas The canvas to draw on.
     */
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Apply zoom and pan transformations
        canvas.withMatrix(matrix) {
            tables.forEach { table ->
                reusableRect.set(
                    table.xCoordinate.toFloat(),
                    table.yCoordinate.toFloat(),
                    table.xCoordinate.toFloat() + table.width.toFloat(),
                    table.yCoordinate.toFloat() + table.height.toFloat()
                )
                // Draw the table
                drawRect(reusableRect, tablePaint)
                val textX = reusableRect.centerX()
                val textY =
                    reusableRect.centerY() - ((tableText.descent() + tableText.ascent()) / 2)
                drawText(table.name, textX, textY, tableText)
                // Highlight the selected table
                if (isEditMode && table.name == selectedTable?.name) {
                    drawRect(reusableRect, selectedTablePaint)

                    val handleX = reusableRect.right
                    val handleY = reusableRect.bottom
                    drawCircle(handleX, handleY, handleRadius, resizeHandlePaint)
                }
            }
        }
    }

    /**
     * Handles touch events on the view.
     *
     * Manages zoom, pan, drag, and resize gestures based on the current mode.
     * @param event The touch event received.
     * @return `true` if the event was consumed, `false` otherwise.
     */
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        scaleGestureDetector.onTouchEvent(event!!)

        if (isScaling) {
            return true
        }

        if (!isEditMode) {
            gestureDetector.onTouchEvent(event)
            return true
        }
        val currentX = event.x
        val currentY = event.y

        // Calculate the inverse matrix to convert the touch coordinates
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
                    // Tell the fragment that the table has been updated
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

    /**
     * Finds the table located at a given position on the screen.
     *
     * The check is performed in reverse order to find the topmost table (drawn last).
     * @param x The X coordinate of the point.
     * @param y The Y coordinate of the point.
     * @return The [Table] object at that position or `null` if none is found.
     */
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

    /**
     * Checks if a given position is near the table's resize handle.
     *
     * @param table The [Table] object to check.
     * @param x The X coordinate of the point.
     * @param y The Y coordinate of the point.
     * @return `true` if the point is near the resize handle, `false` otherwise.
     */
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