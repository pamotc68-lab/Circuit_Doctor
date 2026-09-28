package com.example.circuitdoctor;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

public class CircuitCanvasView extends View {

    public interface Listener {
        void onComponentSelected(Long componentId);
        void onBeforeChange();
        void onCircuitChanged();
        void onPlacementTypeConsumed();
        void onEditPropertiesRequested(long componentId, int typeId);
    }

    private static final int[] GRID_ROWS = {
            BreadboardGrid.ROW_TOP_RAIL_PLUS, BreadboardGrid.ROW_TOP_RAIL_MINUS,
            BreadboardGrid.ROW_TOP_SECTION[0], BreadboardGrid.ROW_TOP_SECTION[1], BreadboardGrid.ROW_TOP_SECTION[2],
            BreadboardGrid.ROW_TOP_SECTION[3], BreadboardGrid.ROW_TOP_SECTION[4],
            BreadboardGrid.ROW_BOTTOM_SECTION[0], BreadboardGrid.ROW_BOTTOM_SECTION[1], BreadboardGrid.ROW_BOTTOM_SECTION[2],
            BreadboardGrid.ROW_BOTTOM_SECTION[3], BreadboardGrid.ROW_BOTTOM_SECTION[4],
            BreadboardGrid.ROW_BOTTOM_RAIL_PLUS, BreadboardGrid.ROW_BOTTOM_RAIL_MINUS
    };

    private static final String[] TOP_ROW_LETTERS = {"j", "i", "h", "g", "f"};
    private static final String[] BOTTOM_ROW_LETTERS = {"e", "d", "c", "b", "a"};

    private static final float TERMINAL_HIT_RADIUS = BreadboardGrid.CELL * 1.2f;
    private static final float HOLE_HIT_RADIUS = BreadboardGrid.CELL * 0.45f;
    private static final float TAP_SLOP = 20f;
    private static final float OUTSIDE_MARGIN_RATIO = 0.28f;

    private static final float MIN_USER_SCALE = 0.6f;
    private static final float MAX_USER_SCALE = 3f;

    private static final int[] WIRE_COLOR_PALETTE = {
            0xFF2E7D32, // เขียว
            0xFFE53935, // แดง
            0xFF1E88E5, // ฟ้า
            0xFFFB8C00, // ส้ม
            0xFF212121, // ดำ
            0xFFFBC02D  // เหลือง
    };

    private CircuitDesignModel model;
    private Listener listener;

    private SimulationResult simulationResult = null;
    private boolean simulationActive = false;

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

    // ===== ซูม/เลื่อนมุมมองที่ผู้ใช้ปรับเอง (บีบนิ้ว/ลากสองนิ้ว) =====
    private float userScale = 1f;
    private float panX = 0f;
    private float panY = 0f;
    private boolean multiTouchActive = false;
    private boolean panInitialized = false;
    private float lastFocusX, lastFocusY;
    private final ScaleGestureDetector scaleDetector;

    // ===== ปุ่มเปลี่ยนสีสายไฟ (วาดลอยมุมขวาบน ไม่ผูกกับซูม/เลื่อน) =====
    private int wireColorIndex = 0;
    private int currentWireColor = WIRE_COLOR_PALETTE[0];
    private boolean pendingWireColorButtonPress = false;
    private float wireBtnCx, wireBtnCy, wireBtnR;

    // ===== ปุ่มปรับค่าอุปกรณ์ (สี LED / โอห์ม / โวลต์แบต) วาดลอยมุมขวาบน ใต้ปุ่มสีสาย =====
    private boolean pendingPropButtonPress = false;
    private float propBtnCx, propBtnCy, propBtnR;

    // ===== พื้นบอร์ด / ราง / ป้ายกำกับ =====
    private final Paint workspacePaint = new Paint();
    private final Paint boardPaint = new Paint();
    private final Paint boardBorderPaint = new Paint();
    private final Paint gridPaint = new Paint();
    private final Paint railPaint = new Paint();
    private final Paint railPlusLinePaint = new Paint();
    private final Paint railMinusLinePaint = new Paint();
    private final Paint railLabelPaint = new Paint();
    private final Paint boardLabelPaint = new Paint();

    // ===== ทั่วไป (เงา/เลือก/ป้ายชื่อสำรอง) =====
    private final Paint componentShadowPaint = new Paint();
    private final Paint componentSelectedPaint = new Paint();
    private final Paint componentSelectedGlowPaint = new Paint();
    private final Paint componentTextPaint = new Paint();
    private final Paint fallbackChipPaint = new Paint();
    private final Paint leadPaint = new Paint();

    // ===== ตัวต้านทาน =====
    private final Paint resistorBodyPaint = new Paint();
    private final Paint resistorBorderPaint = new Paint();
    private final Paint resistorBandPaint = new Paint();

    // ===== LED =====
    private final Paint ledBodyPaint = new Paint();
    private final Paint ledBorderPaint = new Paint();
    private final Paint ledHighlightPaint = new Paint();
    private final Paint ledGlowPaint = new Paint();

    // ===== ปุ่มกด =====
    private final Paint buttonCapPaint = new Paint();
    private final Paint buttonCapBorderPaint = new Paint();
    private final Paint buttonCenterPaint = new Paint();
    private final Paint buttonLegPaint = new Paint();

    // ===== แบตเตอรี่ 9V =====
    private final Paint batteryBoxPaint = new Paint();
    private final Paint batteryBoxBorderPaint = new Paint();
    private final Paint batteryClipPaint = new Paint();
    private final Paint batteryTerminalPosPaint = new Paint();
    private final Paint batteryTerminalNegPaint = new Paint();
    private final Paint batteryTextPaint = new Paint();

    // ===== สายไฟ =====
    private final Paint wireShadowPaint = new Paint();
    private final Paint wirePaint = new Paint();
    private final Paint wirePreviewPaint = new Paint();
    private final Paint terminalDotPaint = new Paint();
    private final Paint terminalHighlightPaint = new Paint();
    private final Paint holeHighlightPaint = new Paint();

    // ===== ปุ่มเปลี่ยนสีสายไฟ =====
    private final Paint wireColorButtonPaint = new Paint();
    private final Paint wireColorButtonBorderPaint = new Paint();
    private final Paint wireColorButtonLabelPaint = new Paint();
    private final Paint wireColorButtonOutlinePaint = new Paint();

    // ===== ปุ่มปรับค่าอุปกรณ์ =====
    private final Paint propButtonPaint = new Paint();
    private final Paint propButtonBorderPaint = new Paint();
    private final Paint propButtonLabelPaint = new Paint();
    private final Paint propButtonOutlinePaint = new Paint();

    // ===== ป้ายค่าอุปกรณ์ (โอห์ม/โวลต์) =====
    private final Paint valueLabelPaint = new Paint();
    private final Paint valueLabelOutlinePaint = new Paint();

    public CircuitCanvasView(Context context, AttributeSet attrs) {
        super(context, attrs);
        scaleDetector = new ScaleGestureDetector(context, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                float newScale = userScale * detector.getScaleFactor();
                userScale = Math.max(MIN_USER_SCALE, Math.min(MAX_USER_SCALE, newScale));
                invalidate();
                return true;
            }
        });
        init();
    }

    private void init() {
        workspacePaint.setColor(Color.parseColor("#EFEFEF"));
        workspacePaint.setStyle(Paint.Style.FILL);

        boardPaint.setColor(Color.parseColor("#FAF9F6"));
        boardPaint.setStyle(Paint.Style.FILL);
        boardPaint.setAntiAlias(true);

        boardBorderPaint.setColor(Color.parseColor("#DCDCDC"));
        boardBorderPaint.setStyle(Paint.Style.STROKE);
        boardBorderPaint.setStrokeWidth(3f);
        boardBorderPaint.setAntiAlias(true);

        gridPaint.setColor(Color.parseColor("#8D8D8D"));
        gridPaint.setStyle(Paint.Style.FILL);
        gridPaint.setAntiAlias(true);

        railPaint.setColor(Color.parseColor("#C9C9C9"));
        railPaint.setStyle(Paint.Style.FILL);
        railPaint.setAntiAlias(true);

        railPlusLinePaint.setColor(Color.parseColor("#E53935"));
        railPlusLinePaint.setStrokeWidth(3f);
        railPlusLinePaint.setAntiAlias(true);

        railMinusLinePaint.setColor(Color.parseColor("#1E88E5"));
        railMinusLinePaint.setStrokeWidth(3f);
        railMinusLinePaint.setAntiAlias(true);

        railLabelPaint.setAntiAlias(true);
        railLabelPaint.setTextSize(16f);
        railLabelPaint.setFakeBoldText(true);
        railLabelPaint.setTextAlign(Paint.Align.CENTER);

        boardLabelPaint.setColor(Color.parseColor("#9C9C9C"));
        boardLabelPaint.setAntiAlias(true);
        boardLabelPaint.setTextSize(14f);
        boardLabelPaint.setTextAlign(Paint.Align.CENTER);

        componentShadowPaint.setColor(Color.parseColor("#2A000000"));
        componentShadowPaint.setStyle(Paint.Style.FILL);
        componentShadowPaint.setAntiAlias(true);

        componentSelectedPaint.setColor(Color.parseColor("#FF6B00"));
        componentSelectedPaint.setStyle(Paint.Style.STROKE);
        componentSelectedPaint.setStrokeWidth(4f);
        componentSelectedPaint.setAntiAlias(true);
        componentSelectedPaint.setPathEffect(new DashPathEffect(new float[]{12f, 6f}, 0));

        componentSelectedGlowPaint.setColor(Color.parseColor("#33FF6B00"));
        componentSelectedGlowPaint.setStyle(Paint.Style.STROKE);
        componentSelectedGlowPaint.setStrokeWidth(14f);
        componentSelectedGlowPaint.setAntiAlias(true);

        componentTextPaint.setColor(Color.WHITE);
        componentTextPaint.setTextSize(20f);
        componentTextPaint.setAntiAlias(true);
        componentTextPaint.setTextAlign(Paint.Align.CENTER);
        componentTextPaint.setFakeBoldText(true);

        fallbackChipPaint.setStyle(Paint.Style.FILL);
        fallbackChipPaint.setAntiAlias(true);

        leadPaint.setColor(Color.parseColor("#8A8A8A"));
        leadPaint.setStyle(Paint.Style.STROKE);
        leadPaint.setStrokeCap(Paint.Cap.ROUND);
        leadPaint.setAntiAlias(true);

        resistorBodyPaint.setColor(Color.parseColor("#E0C08A"));
        resistorBodyPaint.setStyle(Paint.Style.FILL);
        resistorBodyPaint.setAntiAlias(true);

        resistorBorderPaint.setColor(Color.parseColor("#A3824F"));
        resistorBorderPaint.setStyle(Paint.Style.STROKE);
        resistorBorderPaint.setStrokeWidth(2f);
        resistorBorderPaint.setAntiAlias(true);

        resistorBandPaint.setStyle(Paint.Style.FILL);
        resistorBandPaint.setAntiAlias(true);

        ledBodyPaint.setColor(Color.parseColor("#8B1A1A"));
        ledBodyPaint.setStyle(Paint.Style.FILL);
        ledBodyPaint.setAntiAlias(true);

        ledBorderPaint.setColor(Color.parseColor("#5A0F0F"));
        ledBorderPaint.setStyle(Paint.Style.STROKE);
        ledBorderPaint.setStrokeWidth(2f);
        ledBorderPaint.setAntiAlias(true);

        ledHighlightPaint.setColor(Color.parseColor("#55FFFFFF"));
        ledHighlightPaint.setStyle(Paint.Style.FILL);
        ledHighlightPaint.setAntiAlias(true);

        ledGlowPaint.setStyle(Paint.Style.FILL);
        ledGlowPaint.setAntiAlias(true);

        buttonCapPaint.setColor(Color.parseColor("#BDBDBD"));
        buttonCapPaint.setStyle(Paint.Style.FILL);
        buttonCapPaint.setAntiAlias(true);

        buttonCapBorderPaint.setColor(Color.parseColor("#7A7A7A"));
        buttonCapBorderPaint.setStyle(Paint.Style.STROKE);
        buttonCapBorderPaint.setStrokeWidth(2f);
        buttonCapBorderPaint.setAntiAlias(true);

        buttonCenterPaint.setColor(Color.parseColor("#616161"));
        buttonCenterPaint.setStyle(Paint.Style.FILL);
        buttonCenterPaint.setAntiAlias(true);

        buttonLegPaint.setColor(Color.parseColor("#5A5A5A"));
        buttonLegPaint.setStyle(Paint.Style.FILL);
        buttonLegPaint.setAntiAlias(true);

        batteryBoxPaint.setColor(Color.parseColor("#C97B3D"));
        batteryBoxPaint.setStyle(Paint.Style.FILL);
        batteryBoxPaint.setAntiAlias(true);

        batteryBoxBorderPaint.setColor(Color.parseColor("#8A501F"));
        batteryBoxBorderPaint.setStyle(Paint.Style.STROKE);
        batteryBoxBorderPaint.setStrokeWidth(2f);
        batteryBoxBorderPaint.setAntiAlias(true);

        batteryClipPaint.setColor(Color.parseColor("#212121"));
        batteryClipPaint.setStyle(Paint.Style.FILL);
        batteryClipPaint.setAntiAlias(true);

        batteryTerminalPosPaint.setColor(Color.parseColor("#E53935"));
        batteryTerminalPosPaint.setStyle(Paint.Style.FILL);
        batteryTerminalPosPaint.setAntiAlias(true);

        batteryTerminalNegPaint.setColor(Color.parseColor("#B0B0B0"));
        batteryTerminalNegPaint.setStyle(Paint.Style.FILL);
        batteryTerminalNegPaint.setAntiAlias(true);

        batteryTextPaint.setColor(Color.WHITE);
        batteryTextPaint.setAntiAlias(true);
        batteryTextPaint.setTextAlign(Paint.Align.CENTER);
        batteryTextPaint.setFakeBoldText(true);

        wireShadowPaint.setColor(Color.parseColor("#33000000"));
        wireShadowPaint.setStrokeWidth(9f);
        wireShadowPaint.setStyle(Paint.Style.STROKE);
        wireShadowPaint.setAntiAlias(true);
        wireShadowPaint.setStrokeCap(Paint.Cap.ROUND);

        wirePaint.setStrokeWidth(6f);
        wirePaint.setStyle(Paint.Style.STROKE);
        wirePaint.setAntiAlias(true);
        wirePaint.setStrokeCap(Paint.Cap.ROUND);

        wirePreviewPaint.setColor(Color.parseColor("#FFA500"));
        wirePreviewPaint.setStrokeWidth(5f);
        wirePreviewPaint.setStyle(Paint.Style.STROKE);
        wirePreviewPaint.setAntiAlias(true);
        wirePreviewPaint.setStrokeCap(Paint.Cap.ROUND);
        wirePreviewPaint.setPathEffect(new DashPathEffect(new float[]{16f, 10f}, 0));

        terminalDotPaint.setColor(Color.parseColor("#3A3A3A"));
        terminalDotPaint.setStyle(Paint.Style.FILL);
        terminalDotPaint.setAntiAlias(true);

        terminalHighlightPaint.setColor(Color.parseColor("#FFC107"));
        terminalHighlightPaint.setStyle(Paint.Style.FILL);
        terminalHighlightPaint.setAntiAlias(true);

        holeHighlightPaint.setColor(Color.parseColor("#4CAF50"));
        holeHighlightPaint.setStyle(Paint.Style.FILL);
        holeHighlightPaint.setAntiAlias(true);

        wireColorButtonPaint.setStyle(Paint.Style.FILL);
        wireColorButtonPaint.setAntiAlias(true);

        wireColorButtonBorderPaint.setColor(Color.WHITE);
        wireColorButtonBorderPaint.setStyle(Paint.Style.STROKE);
        wireColorButtonBorderPaint.setStrokeWidth(4f);
        wireColorButtonBorderPaint.setAntiAlias(true);

        wireColorButtonLabelPaint.setColor(Color.WHITE);
        wireColorButtonLabelPaint.setTextSize(15f);
        wireColorButtonLabelPaint.setTextAlign(Paint.Align.CENTER);
        wireColorButtonLabelPaint.setFakeBoldText(true);
        wireColorButtonLabelPaint.setAntiAlias(true);

        wireColorButtonOutlinePaint.setColor(Color.parseColor("#80000000"));
        wireColorButtonOutlinePaint.setTextSize(15f);
        wireColorButtonOutlinePaint.setTextAlign(Paint.Align.CENTER);
        wireColorButtonOutlinePaint.setFakeBoldText(true);
        wireColorButtonOutlinePaint.setStyle(Paint.Style.STROKE);
        wireColorButtonOutlinePaint.setStrokeWidth(4f);
        wireColorButtonOutlinePaint.setAntiAlias(true);

        propButtonPaint.setColor(Color.parseColor("#1E88E5"));
        propButtonPaint.setStyle(Paint.Style.FILL);
        propButtonPaint.setAntiAlias(true);

        propButtonBorderPaint.setColor(Color.WHITE);
        propButtonBorderPaint.setStyle(Paint.Style.STROKE);
        propButtonBorderPaint.setStrokeWidth(4f);
        propButtonBorderPaint.setAntiAlias(true);

        propButtonLabelPaint.setColor(Color.WHITE);
        propButtonLabelPaint.setTextSize(15f);
        propButtonLabelPaint.setTextAlign(Paint.Align.CENTER);
        propButtonLabelPaint.setFakeBoldText(true);
        propButtonLabelPaint.setAntiAlias(true);

        propButtonOutlinePaint.setColor(Color.parseColor("#80000000"));
        propButtonOutlinePaint.setTextSize(15f);
        propButtonOutlinePaint.setTextAlign(Paint.Align.CENTER);
        propButtonOutlinePaint.setFakeBoldText(true);
        propButtonOutlinePaint.setStyle(Paint.Style.STROKE);
        propButtonOutlinePaint.setStrokeWidth(4f);
        propButtonOutlinePaint.setAntiAlias(true);

        valueLabelPaint.setColor(Color.parseColor("#FFECB3"));
        valueLabelPaint.setTextAlign(Paint.Align.CENTER);
        valueLabelPaint.setFakeBoldText(true);
        valueLabelPaint.setAntiAlias(true);

        valueLabelOutlinePaint.setColor(Color.parseColor("#99000000"));
        valueLabelOutlinePaint.setTextAlign(Paint.Align.CENTER);
        valueLabelOutlinePaint.setFakeBoldText(true);
        valueLabelOutlinePaint.setStyle(Paint.Style.STROKE);
        valueLabelOutlinePaint.setStrokeWidth(3f);
        valueLabelOutlinePaint.setAntiAlias(true);
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

    public void showSimulationResult(SimulationResult result) {
        this.simulationResult = result;
        this.simulationActive = true;
        invalidate();
    }

    public void clearSimulationResult() {
        this.simulationResult = null;
        this.simulationActive = false;
        invalidate();
    }

    private boolean isLedLitComponent(long componentId) {
        return simulationActive && simulationResult != null && simulationResult.isLedLit()
                && simulationResult.litLedComponentIds.contains(componentId);
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
        float availW = viewW - padding * 2;
        float availH = viewH - padding * 2;

        float sx = availW / (boardW * (1f + OUTSIDE_MARGIN_RATIO * 2f));
        float sy = availH / (boardH * (1f + OUTSIDE_MARGIN_RATIO * 2f));
        scale = Math.min(sx, sy);
        scale = Math.min(scale, 1.4f);

        offsetX = (viewW - boardW * scale) / 2f;
        offsetY = (viewH - boardH * scale) / 2f;
    }

    private float zoomedScale() {
        return scale * userScale;
    }

    private float screenX(float worldX) {
        float baseX = worldX * scale + offsetX;
        float cx = getWidth() / 2f;
        return cx + (baseX - cx) * userScale + panX;
    }

    private float screenY(float worldY) {
        float baseY = worldY * scale + offsetY;
        float cy = getHeight() / 2f;
        return cy + (baseY - cy) * userScale + panY;
    }

    private float worldX(float screenX) {
        float cx = getWidth() / 2f;
        float baseX = (screenX - panX - cx) / userScale + cx;
        return (baseX - offsetX) / scale;
    }

    private float worldY(float screenY) {
        float cy = getHeight() / 2f;
        float baseY = (screenY - panY - cy) / userScale + cy;
        return (baseY - offsetY) / scale;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (model == null) return;

        canvas.drawRect(0, 0, getWidth(), getHeight(), workspacePaint);
        drawBoardBackground(canvas);
        drawGrid(canvas);
        drawWires(canvas);
        drawComponents(canvas);
        drawDragPreview(canvas);
        drawWireColorButton(canvas);
        drawPropertiesButton(canvas);
    }

    private void drawBoardBackground(Canvas canvas) {
        float left = screenX(0);
        float top = screenY(0);
        float right = screenX(BreadboardGrid.worldWidth());
        float bottom = screenY(BreadboardGrid.worldHeight());
        RectF board = new RectF(left, top, right, bottom);
        canvas.drawRoundRect(board, 18f * zoomedScale(), 18f * zoomedScale(), boardPaint);
        canvas.drawRoundRect(board, 18f * zoomedScale(), 18f * zoomedScale(), boardBorderPaint);
    }

    private void drawGrid(Canvas canvas) {
        drawRail(canvas, BreadboardGrid.ROW_TOP_RAIL_PLUS, railPlusLinePaint, "+");
        drawRail(canvas, BreadboardGrid.ROW_TOP_RAIL_MINUS, railMinusLinePaint, "−");
        drawRail(canvas, BreadboardGrid.ROW_BOTTOM_RAIL_PLUS, railPlusLinePaint, "+");
        drawRail(canvas, BreadboardGrid.ROW_BOTTOM_RAIL_MINUS, railMinusLinePaint, "−");

        for (int row : GRID_ROWS) {
            for (int col = 0; col < BreadboardGrid.COLS; col++) {
                float cx = screenX(BreadboardGrid.xFor(col));
                float cy = screenY(BreadboardGrid.yFor(row));
                Paint p = BreadboardGrid.isRail(row) ? railPaint : gridPaint;
                canvas.drawCircle(cx, cy, 3.6f * zoomedScale(), p);
            }
        }

        drawColumnLabels(canvas, BreadboardGrid.ROW_TOP_SECTION[0], -1);
        drawColumnLabels(canvas, BreadboardGrid.ROW_BOTTOM_SECTION[4], 1);
        drawRowLabels(canvas, BreadboardGrid.ROW_TOP_SECTION, TOP_ROW_LETTERS);
        drawRowLabels(canvas, BreadboardGrid.ROW_BOTTOM_SECTION, BOTTOM_ROW_LETTERS);
    }

    private void drawRail(Canvas canvas, int row, Paint linePaint, String symbol) {
        float y = screenY(BreadboardGrid.yFor(row));
        float x1 = screenX(BreadboardGrid.xFor(0));
        float x2 = screenX(BreadboardGrid.xFor(BreadboardGrid.COLS - 1));
        canvas.drawLine(x1, y, x2, y, linePaint);

        railLabelPaint.setColor(linePaint.getColor());
        canvas.drawText(symbol, x1 - 16f * zoomedScale(), y + 5f * zoomedScale(), railLabelPaint);
        canvas.drawText(symbol, x2 + 16f * zoomedScale(), y + 5f * zoomedScale(), railLabelPaint);
    }

    private void drawColumnLabels(Canvas canvas, int nearRow, int direction) {
        float y = screenY(BreadboardGrid.yFor(nearRow)) + direction * 18f * zoomedScale();
        for (int col = 0; col < BreadboardGrid.COLS; col += 5) {
            float x = screenX(BreadboardGrid.xFor(col));
            canvas.drawText(String.valueOf(col + 1), x, y, boardLabelPaint);
        }
    }

    private void drawRowLabels(Canvas canvas, int[] rows, String[] letters) {
        float xLeft = screenX(BreadboardGrid.xFor(0)) - 16f * zoomedScale();
        float xRight = screenX(BreadboardGrid.xFor(BreadboardGrid.COLS - 1)) + 16f * zoomedScale();
        for (int i = 0; i < rows.length; i++) {
            float y = screenY(BreadboardGrid.yFor(rows[i])) + 5f * zoomedScale();
            canvas.drawText(letters[i], xLeft, y, boardLabelPaint);
            canvas.drawText(letters[i], xRight, y, boardLabelPaint);
        }
    }

    private void drawComponents(Canvas canvas) {
        for (PlacedComponent pc : model.getComponents()) {
            ComponentType type = catalog.findById(getContext(), pc.typeId);
            if (type == null) continue;

            float x1, x2, cy;
            if (pc.freePlaced) {
                float halfSpan = (BreadboardGrid.CELL * PlacedComponent.SPAN_COLUMNS) / 2f;
                x1 = screenX(pc.freeX - halfSpan);
                x2 = screenX(pc.freeX + halfSpan);
                cy = screenY(pc.freeY);
            } else {
                x1 = screenX(BreadboardGrid.xFor(pc.colA));
                x2 = screenX(BreadboardGrid.xFor(pc.colB));
                cy = screenY(BreadboardGrid.yFor(pc.row));
            }

            float left = Math.min(x1, x2) - 36f * zoomedScale();
            float right = Math.max(x1, x2) + 36f * zoomedScale();
            float top = cy - 44f * zoomedScale();
            float bottom = cy + 44f * zoomedScale();

            RectF body = new RectF(left, top, right, bottom);

            if (selectedId != null && selectedId == pc.id) {
                RectF glow = new RectF(body);
                glow.inset(-6f * zoomedScale(), -6f * zoomedScale());
                canvas.drawRoundRect(glow, 16f, 16f, componentSelectedGlowPaint);
            }

            RectF shadow = new RectF(body);
            shadow.inset(body.width() * 0.06f, body.height() * 0.18f);
            shadow.offset(0f, 4f * zoomedScale());
            canvas.drawOval(shadow, componentShadowPaint);

            leadPaint.setStrokeWidth(Math.max(2f, 3f * zoomedScale()));

            drawComponentIcon(canvas, pc, type, body);
            drawValueLabel(canvas, pc, body);

            if (selectedId != null && selectedId == pc.id) {
                canvas.drawRoundRect(body, 14f, 14f, componentSelectedPaint);
            }
        }
    }

    private void drawComponentIcon(Canvas canvas, PlacedComponent pc, ComponentType type, RectF body) {
        int typeId = pc.typeId;
        if (typeId == ComponentType.BATTERY_9V) {
            drawBatteryIcon(canvas, body, pc.customVoltage > 0 ? pc.customVoltage : 9.0);
        } else if (typeId == ComponentType.RESISTOR) {
            drawResistorIcon(canvas, body);
        } else if (typeId == ComponentType.LED) {
            drawLedIcon(canvas, body, isLedLitComponent(pc.id), pc.ledColor);
        } else if (typeId == ComponentType.PUSHBUTTON) {
            drawButtonIcon(canvas, body);
        } else if (typeId == ComponentType.BREADBOARD) {
            drawBreadboardIcon(canvas, body);
        } else {
            fallbackChipPaint.setColor(type.color);
            canvas.drawRoundRect(body, 12f, 12f, fallbackChipPaint);
            canvas.drawText(type.shortLabel, body.centerX(), body.centerY() + 7f, componentTextPaint);
        }
    }

    private void drawValueLabel(Canvas canvas, PlacedComponent pc, RectF body) {
        String text = null;
        if (pc.typeId == ComponentType.RESISTOR) {
            double ohms = pc.customOhms > 0 ? pc.customOhms : 220.0;
            text = (ohms == Math.rint(ohms) ? String.valueOf((long) ohms) : String.valueOf(ohms)) + " Ω";
        } else if (pc.typeId == ComponentType.BATTERY_9V) {
            double volts = pc.customVoltage > 0 ? pc.customVoltage : 9.0;
            text = (volts == Math.rint(volts) ? String.valueOf((long) volts) : String.valueOf(volts)) + " V";
        }
        if (text == null) return;

        float size = Math.max(11f, 12f * zoomedScale());
        valueLabelPaint.setTextSize(size);
        valueLabelOutlinePaint.setTextSize(size);
        float y = body.top - 6f * zoomedScale();
        canvas.drawText(text, body.centerX(), y, valueLabelOutlinePaint);
        canvas.drawText(text, body.centerX(), y, valueLabelPaint);
    }

    private void drawResistorIcon(Canvas canvas, RectF body) {
        float cx = body.centerX();
        float cy = body.centerY();
        float bodyW = body.width() * 0.48f;
        float bodyH = body.height() * 0.34f;
        RectF res = new RectF(cx - bodyW / 2f, cy - bodyH / 2f, cx + bodyW / 2f, cy + bodyH / 2f);
        float leadPad = body.width() * 0.06f;

        canvas.drawLine(body.left + leadPad, cy, res.left, cy, leadPaint);
        canvas.drawLine(res.right, cy, body.right - leadPad, cy, leadPaint);

        float corner = bodyH / 2f;
        canvas.drawRoundRect(res, corner, corner, resistorBodyPaint);
        canvas.drawRoundRect(res, corner, corner, resistorBorderPaint);

        int[] bandColors = {Color.parseColor("#212121"), Color.parseColor("#8B4513"), Color.parseColor("#D4AF37")};
        float bandW = res.width() * 0.07f;
        float[] fractions = {0.28f, 0.45f, 0.62f};
        for (int i = 0; i < bandColors.length; i++) {
            float bx = res.left + res.width() * fractions[i];
            resistorBandPaint.setColor(bandColors[i]);
            canvas.drawRect(bx, res.top + 2f, bx + bandW, res.bottom - 2f, resistorBandPaint);
        }
    }

    private void drawLedIcon(Canvas canvas, RectF body, boolean lit, int ledColor) {
        float cx = body.centerX();
        float cy = body.centerY() - body.height() * 0.06f;
        float r = body.height() * 0.24f;
        float leadPad = body.width() * 0.10f;

        if (lit) {
            for (int i = 3; i >= 1; i--) {
                ledGlowPaint.setColor(ledColor);
                ledGlowPaint.setAlpha(40 / i);
                canvas.drawCircle(cx, cy, r * (1f + i * 0.55f), ledGlowPaint);
            }
            ledBodyPaint.setColor(ledColor);
        } else {
            ledBodyPaint.setColor(darkenColor(ledColor, 0.45f));
        }

        canvas.drawCircle(cx, cy, r, ledBodyPaint);
        canvas.drawCircle(cx, cy, r, ledBorderPaint);
        canvas.drawCircle(cx - r * 0.35f, cy - r * 0.35f, r * 0.32f, ledHighlightPaint);

        float legY1 = cy + r * 0.7f;
        float legY2 = body.centerY() + body.height() * 0.34f;
        float legXLeft = cx - r * 0.5f;
        float legXRight = cx + r * 0.5f;

        canvas.drawLine(legXLeft, legY1, legXLeft, legY2, leadPaint);
        canvas.drawLine(legXLeft, legY2, body.left + leadPad, legY2, leadPaint);

        canvas.drawLine(legXRight, legY1, legXRight, legY2, leadPaint);
        canvas.drawLine(legXRight, legY2, body.right - leadPad, legY2, leadPaint);
    }

    private int darkenColor(int color, float factor) {
        int a = Color.alpha(color);
        int r = (int) (Color.red(color) * factor);
        int g = (int) (Color.green(color) * factor);
        int b = (int) (Color.blue(color) * factor);
        return Color.argb(a, r, g, b);
    }

    private void drawButtonIcon(Canvas canvas, RectF body) {
        float cx = body.centerX();
        float cy = body.centerY();
        float capSize = body.height() * 0.52f;
        float leadPad = body.width() * 0.10f;

        float legOffset = capSize / 2f + 3f * zoomedScale();
        float legSize = Math.max(3f, capSize * 0.14f);
        float[][] legPositions = {
                {-legOffset, -legOffset}, {legOffset, -legOffset},
                {-legOffset, legOffset}, {legOffset, legOffset}
        };
        for (float[] pos : legPositions) {
            canvas.drawRect(cx + pos[0] - legSize / 2f, cy + pos[1] - legSize / 2f,
                    cx + pos[0] + legSize / 2f, cy + pos[1] + legSize / 2f, buttonLegPaint);
        }

        canvas.drawLine(body.left + leadPad, cy + legOffset, cx - legOffset, cy + legOffset, leadPaint);
        canvas.drawLine(cx + legOffset, cy + legOffset, body.right - leadPad, cy + legOffset, leadPaint);

        RectF cap = new RectF(cx - capSize / 2f, cy - capSize / 2f, cx + capSize / 2f, cy + capSize / 2f);
        canvas.drawRoundRect(cap, 6f, 6f, buttonCapPaint);
        canvas.drawRoundRect(cap, 6f, 6f, buttonCapBorderPaint);
        canvas.drawCircle(cx, cy, capSize * 0.22f, buttonCenterPaint);
    }

    private void drawBatteryIcon(Canvas canvas, RectF body, double voltage) {
        float clipW = body.width() * 0.16f;
        RectF clip = new RectF(body.left, body.top + body.height() * 0.12f, body.left + clipW, body.bottom - body.height() * 0.12f);
        RectF box = new RectF(clip.right - 2f, body.top + body.height() * 0.06f, body.right - body.width() * 0.04f, body.bottom - body.height() * 0.06f);

        canvas.drawRoundRect(box, 6f, 6f, batteryBoxPaint);
        canvas.drawRoundRect(box, 6f, 6f, batteryBoxBorderPaint);
        canvas.drawRect(clip, batteryClipPaint);

        float termR = Math.max(2.5f, clipW * 0.22f);
        canvas.drawCircle(clip.centerX(), clip.top + clip.height() * 0.28f, termR, batteryTerminalNegPaint);
        canvas.drawCircle(clip.centerX(), clip.bottom - clip.height() * 0.28f, termR, batteryTerminalPosPaint);

        String voltageText = (voltage == Math.rint(voltage) ? String.valueOf((long) voltage) : String.valueOf(voltage)) + "V";
        batteryTextPaint.setTextSize(Math.max(12f, box.height() * 0.30f));
        canvas.drawText(voltageText, box.centerX(), box.centerY() + batteryTextPaint.getTextSize() * 0.32f, batteryTextPaint);

        float leadPad = body.width() * 0.03f;
        canvas.drawLine(body.left + leadPad, clip.top - 3f * zoomedScale(), clip.centerX(), clip.top - 3f * zoomedScale(), leadPaint);
        canvas.drawLine(clip.centerX(), clip.top - 3f * zoomedScale(), clip.centerX(), clip.top + 2f, leadPaint);

        canvas.drawLine(box.right, body.top + body.height() * 0.02f, body.right - leadPad, body.top + body.height() * 0.02f, leadPaint);
        canvas.drawLine(box.centerX(), box.top, box.centerX(), body.top + body.height() * 0.02f, leadPaint);
    }

    private void drawBreadboardIcon(Canvas canvas, RectF body) {
        float cx = body.centerX();
        float cy = body.centerY();
        float stepX = body.width() * 0.16f;
        float stepY = body.height() * 0.22f;
        float r = Math.max(2f, body.height() * 0.05f);

        fallbackChipPaint.setColor(Color.parseColor("#ECECEC"));
        canvas.drawRoundRect(body, 10f, 10f, fallbackChipPaint);

        for (int row = -1; row <= 1; row += 2) {
            for (int col = -1; col <= 1; col++) {
                canvas.drawCircle(cx + col * stepX, cy + row * stepY, r, terminalDotPaint);
            }
        }
    }

    private void drawWires(Canvas canvas) {
        for (Connection c : model.getConnections()) {
            float[] pa = terminalScreenPoint(c.getFrom());
            float[] pb = terminalScreenPoint(c.getTo());
            if (pa == null || pb == null) continue;

            wirePaint.setColor(c.color);

            Path path = buildWirePath(pa, pb);
            canvas.drawPath(path, wireShadowPaint);
            canvas.drawPath(path, wirePaint);
            canvas.drawCircle(pa[0], pa[1], 5.5f * zoomedScale(), terminalDotPaint);
            canvas.drawCircle(pb[0], pb[1], 5.5f * zoomedScale(), terminalDotPaint);
        }
    }

    private Path buildWirePath(float[] a, float[] b) {
        Path path = new Path();
        path.moveTo(a[0], a[1]);
        float midY = (a[1] + b[1]) / 2f - 26f * zoomedScale();
        path.cubicTo(a[0], midY, b[0], midY, b[0], b[1]);
        return path;
    }

    private void drawDragPreview(Canvas canvas) {
        if (downTerminal == null || !movedBeyondSlop) return;
        float[] p = terminalScreenPoint(downTerminal);
        if (p == null) return;
        Paint dot = downTerminal.isHole ? holeHighlightPaint : terminalHighlightPaint;
        canvas.drawCircle(p[0], p[1], 10f * zoomedScale(), dot);
        wirePreviewPaint.setColor(currentWireColor);
        canvas.drawLine(p[0], p[1], dragX, dragY, wirePreviewPaint);
    }

    private void drawWireColorButton(Canvas canvas) {
        float density = getResources().getDisplayMetrics().density;
        wireBtnR = 26f * density;
        wireBtnCx = getWidth() - wireBtnR - 18f * density;
        wireBtnCy = wireBtnR + 18f * density;

        wireColorButtonPaint.setColor(currentWireColor);
        canvas.drawCircle(wireBtnCx, wireBtnCy, wireBtnR, wireColorButtonPaint);
        canvas.drawCircle(wireBtnCx, wireBtnCy, wireBtnR, wireColorButtonBorderPaint);

        float textY = wireBtnCy + 5f * density;
        canvas.drawText("ปรับสีสายไฟ", wireBtnCx, textY, wireColorButtonOutlinePaint);
        canvas.drawText("ปรับสีสายไฟ", wireBtnCx, textY, wireColorButtonLabelPaint);
    }

    private boolean isInsideWireColorButton(float sx, float sy) {
        return Math.hypot(sx - wireBtnCx, sy - wireBtnCy) <= wireBtnR;
    }

    private void cycleWireColor() {
        wireColorIndex = (wireColorIndex + 1) % WIRE_COLOR_PALETTE.length;
        currentWireColor = WIRE_COLOR_PALETTE[wireColorIndex];
        invalidate();
    }

    private boolean isPropButtonVisible() {
        if (selectedId == null || model == null) return false;
        PlacedComponent pc = model.findComponentById(selectedId);
        if (pc == null) return false;
        return pc.typeId == ComponentType.LED || pc.typeId == ComponentType.RESISTOR
                || pc.typeId == ComponentType.BATTERY_9V;
    }

    private void drawPropertiesButton(Canvas canvas) {
        if (!isPropButtonVisible()) return;

        float density = getResources().getDisplayMetrics().density;
        propBtnR = 26f * density;
        propBtnCx = getWidth() - propBtnR - 18f * density;
        propBtnCy = wireBtnCy + wireBtnR + 16f * density + propBtnR;

        canvas.drawCircle(propBtnCx, propBtnCy, propBtnR, propButtonPaint);
        canvas.drawCircle(propBtnCx, propBtnCy, propBtnR, propButtonBorderPaint);

        float textY = propBtnCy + 5f * density;
        canvas.drawText("ปรับค่า", propBtnCx, textY, propButtonOutlinePaint);
        canvas.drawText("ปรับค่า", propBtnCx, textY, propButtonLabelPaint);
    }

    private boolean isInsidePropButton(float sx, float sy) {
        if (!isPropButtonVisible()) return false;
        return Math.hypot(sx - propBtnCx, sy - propBtnCy) <= propBtnR;
    }

    private void handlePropButtonTap() {
        if (selectedId == null) return;
        PlacedComponent pc = model.findComponentById(selectedId);
        if (pc == null) return;

        if (pc.typeId == ComponentType.LED) {
            listener.onBeforeChange();
            model.cycleLedColor(pc.id);
            listener.onCircuitChanged();
            invalidate();
        } else if (pc.typeId == ComponentType.RESISTOR || pc.typeId == ComponentType.BATTERY_9V) {
            listener.onEditPropertiesRequested(pc.id, pc.typeId);
        }
    }

    private float[] terminalScreenPoint(TerminalRef ref) {
        if (ref.isHole) {
            return new float[]{screenX(BreadboardGrid.xFor(ref.holeCol)), screenY(BreadboardGrid.yFor(ref.holeRow))};
        }
        PlacedComponent pc = model.findComponentById(ref.componentId);
        if (pc == null) return null;
        if (pc.freePlaced) {
            float halfSpan = (BreadboardGrid.CELL * PlacedComponent.SPAN_COLUMNS) / 2f;
            float tx = ref.isA ? pc.freeX - halfSpan : pc.freeX + halfSpan;
            return new float[]{screenX(tx), screenY(pc.freeY)};
        }
        int col = ref.isA ? pc.colA : pc.colB;
        return new float[]{screenX(BreadboardGrid.xFor(col)), screenY(BreadboardGrid.yFor(pc.row))};
    }

    private TerminalRef findComponentTerminalNear(float wx, float wy) {
        TerminalRef best = null;
        double bestDist = Double.MAX_VALUE;

        for (PlacedComponent pc : model.getComponents()) {
            float ax, ay, bx, by;
            if (pc.freePlaced) {
                float halfSpan = (BreadboardGrid.CELL * PlacedComponent.SPAN_COLUMNS) / 2f;
                ay = by = pc.freeY;
                ax = pc.freeX - halfSpan;
                bx = pc.freeX + halfSpan;
            } else {
                ay = by = BreadboardGrid.yFor(pc.row);
                ax = BreadboardGrid.xFor(pc.colA);
                bx = BreadboardGrid.xFor(pc.colB);
            }

            double da = Math.hypot(wx - ax, wy - ay);
            if (da <= TERMINAL_HIT_RADIUS && da < bestDist) {
                bestDist = da;
                best = new TerminalRef(pc.id, true);
            }

            double db = Math.hypot(wx - bx, wy - by);
            if (db <= TERMINAL_HIT_RADIUS && db < bestDist) {
                bestDist = db;
                best = new TerminalRef(pc.id, false);
            }
        }
        return best;
    }

    private TerminalRef findHoleNear(float wx, float wy) {
        if (wx < -BreadboardGrid.CELL || wy < -BreadboardGrid.CELL
                || wx > BreadboardGrid.worldWidth() + BreadboardGrid.CELL
                || wy > BreadboardGrid.worldHeight() + BreadboardGrid.CELL) {
            return null;
        }
        TerminalRef best = null;
        double bestDist = Double.MAX_VALUE;
        for (int row : GRID_ROWS) {
            float hy = BreadboardGrid.yFor(row);
            for (int col = 0; col < BreadboardGrid.COLS; col++) {
                float hx = BreadboardGrid.xFor(col);
                double d = Math.hypot(wx - hx, wy - hy);
                if (d <= HOLE_HIT_RADIUS && d < bestDist) {
                    bestDist = d;
                    best = new TerminalRef(row, col);
                }
            }
        }
        return best;
    }

    private TerminalRef findConnectPointNear(float wx, float wy) {
        TerminalRef found = findComponentTerminalNear(wx, wy);
        if (found != null) return found;
        return findHoleNear(wx, wy);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (model == null) return true;

        scaleDetector.onTouchEvent(event);
        int action = event.getActionMasked();

        if (event.getPointerCount() >= 2) {
            if (!multiTouchActive) {
                downTerminal = null;
                movedBeyondSlop = false;
                pendingWireColorButtonPress = false;
                pendingPropButtonPress = false;
            }
            multiTouchActive = true;
            handlePan(event);
            if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                multiTouchActive = false;
                panInitialized = false;
            }
            invalidate();
            return true;
        }

        float x = event.getX();
        float y = event.getY();

        if (action == MotionEvent.ACTION_DOWN) {
            multiTouchActive = false;
            panInitialized = false;

            if (isInsideWireColorButton(x, y)) {
                pendingWireColorButtonPress = true;
                return true;
            }

            if (isInsidePropButton(x, y)) {
                pendingPropButtonPress = true;
                return true;
            }
        }

        if (multiTouchActive) {
            if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                multiTouchActive = false;
            }
            return true;
        }

        if (pendingWireColorButtonPress) {
            if (action == MotionEvent.ACTION_UP) {
                if (isInsideWireColorButton(x, y)) {
                    cycleWireColor();
                }
                pendingWireColorButtonPress = false;
                invalidate();
            } else if (action == MotionEvent.ACTION_CANCEL) {
                pendingWireColorButtonPress = false;
            }
            return true;
        }

        if (pendingPropButtonPress) {
            if (action == MotionEvent.ACTION_UP) {
                if (isInsidePropButton(x, y)) {
                    handlePropButtonTap();
                }
                pendingPropButtonPress = false;
                invalidate();
            } else if (action == MotionEvent.ACTION_CANCEL) {
                pendingPropButtonPress = false;
            }
            return true;
        }

        switch (action) {
            case MotionEvent.ACTION_DOWN: {
                downX = x;
                downY = y;
                dragX = x;
                dragY = y;
                movedBeyondSlop = false;
                downTerminal = findConnectPointNear(worldX(x), worldY(y));
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

    private void handlePan(MotionEvent event) {
        if (event.getPointerCount() < 2) {
            panInitialized = false;
            return;
        }
        float focusX = (event.getX(0) + event.getX(1)) / 2f;
        float focusY = (event.getY(0) + event.getY(1)) / 2f;

        if (!panInitialized) {
            lastFocusX = focusX;
            lastFocusY = focusY;
            panInitialized = true;
            return;
        }

        panX += focusX - lastFocusX;
        panY += focusY - lastFocusY;
        lastFocusX = focusX;
        lastFocusY = focusY;
    }

    private void handleRelease(float x, float y) {
        float wx = worldX(x);
        float wy = worldY(y);

        if (downTerminal != null && movedBeyondSlop) {
            TerminalRef release = findConnectPointNear(wx, wy);
            if (release != null && !release.equals(downTerminal)) {
                listener.onBeforeChange();
                model.addConnection(downTerminal, release, currentWireColor);
                listener.onCircuitChanged();
            }
            return;
        }

        if (pendingPlacementTypeId != null) {
            if (downTerminal != null && !downTerminal.isHole) {
                pendingPlacementTypeId = null;
                listener.onPlacementTypeConsumed();
                return;
            }
            int typeId = pendingPlacementTypeId;
            listener.onBeforeChange();
            placeComponentAt(typeId, wx, wy);
            pendingPlacementTypeId = null;
            listener.onPlacementTypeConsumed();
            listener.onCircuitChanged();
            return;
        }

        if (downTerminal != null && !downTerminal.isHole) {
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
            moveSelectedTo(wx, wy);
            selectedId = null;
            listener.onComponentSelected(null);
            listener.onCircuitChanged();
        }
    }

    private boolean isInsideBoard(float wx, float wy) {
        return wx >= 0 && wx <= BreadboardGrid.worldWidth() && wy >= 0 && wy <= BreadboardGrid.worldHeight();
    }

    private void placeComponentAt(int typeId, float wx, float wy) {
        if (isInsideBoard(wx, wy)) {
            int[] hole = BreadboardGrid.nearestHole(wx, wy);
            model.addComponent(typeId, hole[0], hole[1]);
        } else {
            model.addComponentFree(typeId, wx, wy);
        }
    }

    private void moveSelectedTo(float wx, float wy) {
        if (isInsideBoard(wx, wy)) {
            int[] hole = BreadboardGrid.nearestHole(wx, wy);
            model.moveComponent(selectedId, hole[0], hole[1]);
        } else {
            model.moveComponentFree(selectedId, wx, wy);
        }
    }
}