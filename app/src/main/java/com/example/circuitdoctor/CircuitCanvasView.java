package com.example.circuitdoctor;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class CircuitCanvasView extends View {

    public interface Listener {
        void onComponentSelected(Long componentId);
        void onBeforeChange();
        void onCircuitChanged();
        void onPlacementTypeConsumed();
    }

    private static final int[] GRID_ROWS = {
            BreadboardGrid.ROW_TOP_RAIL_PLUS, BreadboardGrid.ROW_TOP_RAIL_MINUS,
            BreadboardGrid.ROW_TOP_SECTION[0], BreadboardGrid.ROW_TOP_SECTION[1], BreadboardGrid.ROW_TOP_SECTION[2],
            BreadboardGrid.ROW_TOP_SECTION[3], BreadboardGrid.ROW_TOP_SECTION[4],
            BreadboardGrid.ROW_BOTTOM_SECTION[0], BreadboardGrid.ROW_BOTTOM_SECTION[1], BreadboardGrid.ROW_BOTTOM_SECTION[2],
            BreadboardGrid.ROW_BOTTOM_SECTION[3], BreadboardGrid.ROW_BOTTOM_SECTION[4],
            BreadboardGrid.ROW_BOTTOM_RAIL_PLUS, BreadboardGrid.ROW_BOTTOM_RAIL_MINUS
    };

    private static final float TERMINAL_HIT_RADIUS = BreadboardGrid.CELL * 1.2f;
    private static final float TAP_SLOP = 20f; // ระยะขยับนิ้ว (พิกเซลจอ) ก่อนถือว่าเป็นการ "ลาก" ไม่ใช่ "แตะ"

    private CircuitDesignModel model;
    private Listener listener;

    private final ComponentCatalog catalog = new ComponentCatalog();

    private Long selectedId = null;
    private Integer pendingPlacementTypeId = null;

    private TerminalRef downTerminal = null;
    private float downX, downY;
    private float dragX, dragY;
    private boolean movedBeyondSlop = false;

    private float scale = 1f;
    private float offsetX = 0f;
    private float offsetY = 0f;

    private final Paint gridPaint = new Paint();
    private final Paint railPaint = new Paint();
    private final Paint componentBodyPaint = new Paint();
    private final Paint componentSelectedPaint = new Paint();
    private final Paint componentTextPaint = new Paint();
    private final Paint wirePaint = new Paint();
    private final Paint wirePreviewPaint = new Paint();
    private final Paint terminalHighlightPaint = new Paint();

    public CircuitCanvasView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        gridPaint.setColor(Color.parseColor("#D8D8D8"));
        gridPaint.setStyle(Paint.Style.FILL);
        gridPaint.setAntiAlias(true);

        railPaint.setColor(Color.parseColor("#EAEAEA"));
        railPaint.setStyle(Paint.Style.FILL);
        railPaint.setAntiAlias(true);

        componentBodyPaint.setStyle(Paint.Style.FILL);
        componentBodyPaint.setAntiAlias(true);

        componentSelectedPaint.setColor(Color.parseColor("#FF6B00"));
        componentSelectedPaint.setStyle(Paint.Style.STROKE);
        componentSelectedPaint.setStrokeWidth(6f);
        componentSelectedPaint.setAntiAlias(true);

        componentTextPaint.setColor(Color.WHITE);
        componentTextPaint.setTextSize(20f);
        componentTextPaint.setAntiAlias(true);
        componentTextPaint.setTextAlign(Paint.Align.CENTER);

        wirePaint.setColor(Color.parseColor("#2E7D32"));
        wirePaint.setStrokeWidth(6f);
        wirePaint.setStyle(Paint.Style.STROKE);
        wirePaint.setAntiAlias(true);
        wirePaint.setStrokeCap(Paint.Cap.ROUND);

        wirePreviewPaint.setColor(Color.parseColor("#FFA500"));
        wirePreviewPaint.setStrokeWidth(5f);
        wirePreviewPaint.setStyle(Paint.Style.STROKE);
        wirePreviewPaint.setAntiAlias(true);
        wirePreviewPaint.setStrokeCap(Paint.Cap.ROUND);

        terminalHighlightPaint.setColor(Color.parseColor("#FFC107"));
        terminalHighlightPaint.setStyle(Paint.Style.FILL);
        terminalHighlightPaint.setAntiAlias(true);
    }

    public void setModel(CircuitDesignModel model) {
        this.model = model;
        invalidate();
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public void setPendingPlacementType(Integer typeId) {
        this.pendingPlacementTypeId = typeId;
        this.selectedId = null;
        invalidate();
    }

    public void clearSelection() {
        selectedId = null;
        pendingPlacementTypeId = null;
        downTerminal = null;
        movedBeyondSlop = false;
        invalidate();
    }

    public void refresh() {
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        computeFixedTransform(w, h);
    }

    private void computeFixedTransform(int viewW, int viewH) {
        float boardW = BreadboardGrid.worldWidth();
        float boardH = BreadboardGrid.worldHeight();
        if (boardW <= 0 || boardH <= 0 || viewW <= 0 || viewH <= 0) return;

        float padding = 40f;
        float sx = (viewW - padding * 2) / boardW;
        float sy = (viewH - padding * 2) / boardH;
        scale = Math.min(sx, sy);
        scale = Math.min(scale, 1.4f);

        offsetX = (viewW - boardW * scale) / 2f;
        offsetY = (viewH - boardH * scale) / 2f;
    }

    private float screenX(float worldX) {
        return worldX * scale + offsetX;
    }

    private float screenY(float worldY) {
        return worldY * scale + offsetY;
    }

    private float worldX(float screenX) {
        return (screenX - offsetX) / scale;
    }

    private float worldY(float screenY) {
        return (screenY - offsetY) / scale;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (model == null) return;

        drawGrid(canvas);
        drawComponents(canvas);
        drawWires(canvas);
        drawDragPreview(canvas);
    }

    private void drawGrid(Canvas canvas) {
        for (int row : GRID_ROWS) {
            for (int col = 0; col < BreadboardGrid.COLS; col++) {
                float cx = screenX(BreadboardGrid.xFor(col));
                float cy = screenY(BreadboardGrid.yFor(row));
                Paint p = BreadboardGrid.isRail(row) ? railPaint : gridPaint;
                canvas.drawCircle(cx, cy, 5f * scale, p);
            }
        }
    }

    private void drawComponents(Canvas canvas) {
        for (PlacedComponent pc : model.getComponents()) {
            ComponentType type = catalog.findById(getContext(), pc.typeId);
            if (type == null) continue;

            float x1 = screenX(BreadboardGrid.xFor(pc.colA));
            float x2 = screenX(BreadboardGrid.xFor(pc.colB));
            float y = screenY(BreadboardGrid.yFor(pc.row));

            float left = Math.min(x1, x2) - 24f * scale;
            float right = Math.max(x1, x2) + 24f * scale;
            float top = y - 30f * scale;
            float bottom = y + 30f * scale;

            componentBodyPaint.setColor(type.color);
            RectF body = new RectF(left, top, right, bottom);
            canvas.drawRoundRect(body, 12f, 12f, componentBodyPaint);

            if (selectedId != null && selectedId == pc.id) {
                canvas.drawRoundRect(body, 12f, 12f, componentSelectedPaint);
            }

            canvas.drawText(type.shortLabel, (left + right) / 2f, (top + bottom) / 2f + 7f, componentTextPaint);
        }
    }

    private void drawWires(Canvas canvas) {
        for (Connection c : model.getConnections()) {
            TerminalRef a = new TerminalRef(c.fromId, c.fromA);
            TerminalRef b = new TerminalRef(c.toId, c.toA);
            float[] pa = terminalScreenPoint(a);
            float[] pb = terminalScreenPoint(b);
            if (pa == null || pb == null) continue;
            canvas.drawLine(pa[0], pa[1], pb[0], pb[1], wirePaint);
        }
    }

    private void drawDragPreview(Canvas canvas) {
        if (downTerminal == null || !movedBeyondSlop) return;
        float[] p = terminalScreenPoint(downTerminal);
        if (p == null) return;
        canvas.drawCircle(p[0], p[1], 10f * scale, terminalHighlightPaint);
        canvas.drawLine(p[0], p[1], dragX, dragY, wirePreviewPaint);
    }

    private float[] terminalScreenPoint(TerminalRef ref) {
        PlacedComponent pc = model.findComponentById(ref.componentId);
        if (pc == null) return null;
        int col = ref.isA ? pc.colA : pc.colB;
        return new float[]{screenX(BreadboardGrid.xFor(col)), screenY(BreadboardGrid.yFor(pc.row))};
    }

    /** หาขาอุปกรณ์ที่ใกล้จุดที่แตะที่สุด โดยยอมรับระยะคลาดเคลื่อนกว้างพอให้แตะกลางตัวอุปกรณ์ก็ถือว่าโดน */
    private TerminalRef findTerminalNear(float wx, float wy) {
        TerminalRef best = null;
        double bestDist = Double.MAX_VALUE;

        for (PlacedComponent pc : model.getComponents()) {
            float ay = BreadboardGrid.yFor(pc.row);

            float ax = BreadboardGrid.xFor(pc.colA);
            double da = Math.hypot(wx - ax, wy - ay);
            if (da <= TERMINAL_HIT_RADIUS && da < bestDist) {
                bestDist = da;
                best = new TerminalRef(pc.id, true);
            }

            float bx = BreadboardGrid.xFor(pc.colB);
            double db = Math.hypot(wx - bx, wy - ay);
            if (db <= TERMINAL_HIT_RADIUS && db < bestDist) {
                bestDist = db;
                best = new TerminalRef(pc.id, false);
            }
        }
        return best;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (model == null) return true;

        float x = event.getX();
        float y = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN: {
                downX = x;
                downY = y;
                dragX = x;
                dragY = y;
                movedBeyondSlop = false;
                downTerminal = findTerminalNear(worldX(x), worldY(y));
                invalidate();
                return true;
            }
            case MotionEvent.ACTION_MOVE: {
                float dx = x - downX;
                float dy = y - downY;
                if (Math.hypot(dx, dy) > TAP_SLOP) movedBeyondSlop = true;

                if (downTerminal != null && movedBeyondSlop) {
                    dragX = x;
                    dragY = y;
                    invalidate();
                }
                return true;
            }
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL: {
                handleRelease(x, y);
                downTerminal = null;
                movedBeyondSlop = false;
                invalidate();
                return true;
            }
        }
        return true;
    }

    private void handleRelease(float x, float y) {
        float wx = worldX(x);
        float wy = worldY(y);
        int[] hole = BreadboardGrid.nearestHole(wx, wy);

        if (downTerminal != null && movedBeyondSlop) {
            // ลากจากขาหนึ่งไปปล่อยที่อีกขาหนึ่ง = ต่อสายไฟ
            TerminalRef release = findTerminalNear(wx, wy);
            if (release != null && !release.equals(downTerminal)) {
                listener.onBeforeChange();
                model.addConnection(downTerminal, release);
                listener.onCircuitChanged();
            }
            return;
        }

        if (pendingPlacementTypeId != null) {
            if (downTerminal != null) {
                // แตะโดนอุปกรณ์อื่นระหว่างรอวาง = ยกเลิกการวาง
                pendingPlacementTypeId = null;
                listener.onPlacementTypeConsumed();
                return;
            }
            int typeId = pendingPlacementTypeId;
            listener.onBeforeChange();
            model.addComponent(typeId, hole[0], hole[1]);
            pendingPlacementTypeId = null;
            listener.onPlacementTypeConsumed();
            listener.onCircuitChanged();
            return;
        }

        if (downTerminal != null) {
            // แตะเฉยๆ ไม่ลาก = เลือก/ยกเลิกเลือกเพื่อย้าย
            if (selectedId != null && selectedId == downTerminal.componentId) {
                selectedId = null;
                listener.onComponentSelected(null);
            } else {
                selectedId = downTerminal.componentId;
                listener.onComponentSelected(selectedId);
            }
            return;
        }

        if (selectedId != null) {
            listener.onBeforeChange();
            model.moveComponent(selectedId, hole[0], hole[1]);
            selectedId = null;
            listener.onComponentSelected(null);
            listener.onCircuitChanged();
        }
    }
}