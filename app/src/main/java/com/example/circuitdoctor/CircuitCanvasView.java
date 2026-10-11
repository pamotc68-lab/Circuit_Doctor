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

import java.util.Locale;

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
            BreadboardGrid.ROW_MID_SECTION[0], BreadboardGrid.ROW_MID_SECTION[1], BreadboardGrid.ROW_MID_SECTION[2],
            BreadboardGrid.ROW_MID_SECTION[3], BreadboardGrid.ROW_MID_SECTION[4],
            BreadboardGrid.ROW_BOTTOM_SECTION[0], BreadboardGrid.ROW_BOTTOM_SECTION[1], BreadboardGrid.ROW_BOTTOM_SECTION[2],
            BreadboardGrid.ROW_BOTTOM_SECTION[3], BreadboardGrid.ROW_BOTTOM_SECTION[4],
            BreadboardGrid.ROW_BOTTOM_RAIL_PLUS, BreadboardGrid.ROW_BOTTOM_RAIL_MINUS
    };

    private static final String[] TOP_ROW_LETTERS = {"o", "n", "m", "l", "k"};
    private static final String[] MID_ROW_LETTERS = {"j", "i", "h", "g", "f"};
    private static final String[] BOTTOM_ROW_LETTERS = {"e", "d", "c", "b", "a"};

    private static final float TERMINAL_HIT_RADIUS = BreadboardGrid.CELL * 1.2f;
    private static final float HOLE_HIT_RADIUS = BreadboardGrid.CELL * 0.45f;
    private static final float TAP_SLOP = 20f;
    private static final float OUTSIDE_MARGIN_RATIO = 0.28f;

    // ===== ความง่ายในการแตะสายไฟ (ปรับตัวเลขตรงนี้ได้) =====
    // รัศมีแตะสายไฟ (dp) วัดจากเส้นกลางสาย ~ ขนาดนิ้วมือ (แถบกว้างรวม 2 เท่า) ไม่ขึ้นกับการซูม
    private static final float WIRE_TOUCH_RADIUS_DP = 14f;
    // รัศมีแตะสายตอนมีอุปกรณ์ถูกเลือกอยู่ (เล็กลง กันสายแย่งการแตะ "เลือกรูเพื่อย้ายอุปกรณ์")
    private static final float WIRE_TOUCH_RADIUS_WHILE_SELECTED_DP = 8f;
    // "รัศมีป้องกัน" (หน่วยโลก): นิ้วแตะใกล้ศูนย์กลางรู/ขาอุปกรณ์ภายในรัศมีนี้ = รู/ขาชนะสายไฟที่พาดผ่าน
    // นอกรัศมีนี้ แต่ยังอยู่ในแถบแตะสาย = สายชนะ (สายอยู่ชั้นบนสุดเหมือนที่เห็นบนจอ)
    private static final float TERMINAL_PROTECT_RADIUS = BreadboardGrid.CELL * 0.8f;      // ขาอุปกรณ์
    private static final float HOLE_PROTECT_RADIUS_WIRE_MODE = BreadboardGrid.CELL * 0.4f; // รู ตอนอยู่ในโหมดต่อสาย
    private static final float HOLE_PROTECT_RADIUS_MOVING = BreadboardGrid.CELL * 0.3f;    // รู ตอนมีอุปกรณ์ที่เลือกไว้ (แตะรูเพื่อย้าย)
    // (โหมดปกติที่ไม่ได้เลือกอะไร: แตะรูแค่แสดงคำแนะนำ จึงไม่ป้องกันรู — สายชนะทั้งหมด)

    // นิ้วขยับไม่เกินระยะนี้ (dp) ยังนับเป็น "แตะ" (ขั้นต่ำ TAP_SLOP พิกเซล) ยิ่งมากยิ่งแตะง่าย
    private static final float TAP_SLOP_DP = 12f;

    private static final float MIN_USER_SCALE = 0.6f;
    private static final float MAX_USER_SCALE = 3f;

    private static final float BODY_PAD_X = 48f;
    private static final float BODY_PAD_Y = 58f;

    // ขนาดเส้นสายไฟ (พิกเซล) ปรับตัวเลขตรงนี้ได้ถ้าอยากให้เส้นเล็กหรือใหญ่ขึ้น
    private static final float WIRE_STROKE_WIDTH = 8f;
    private static final float WIRE_SHADOW_EXTRA = 3f;
    private static final float WIRE_SELECTED_GLOW_EXTRA = 7f;

    // ขนาดตัวแบตเตอรี่เทียบกับกรอบอุปกรณ์ (1.0 = เต็มกรอบ) ปรับตรงนี้ได้ถ้าอยากให้ใหญ่หรือเล็กลง
    private static final float BATTERY_WIDTH_RATIO = 0.94f;
    private static final float BATTERY_HEIGHT_RATIO = 0.84f;

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
    private Connection downWire = null;
    private Connection selectedWire = null;
    private boolean draggingComponent = false;
    private float downX, downY;
    private float dragX, dragY;
    private boolean movedBeyondSlop = false;

    private float scale = 1f;
    private float offsetX = 0f;
    private float offsetY = 0f;

    private float userScale = 1f;
    private float panX = 0f;
    private float panY = 0f;
    private boolean multiTouchActive = false;
    private boolean panInitialized = false;
    private float lastFocusX, lastFocusY;
    private final ScaleGestureDetector scaleDetector;

    private int wireColorIndex = 0;
    private int currentWireColor = WIRE_COLOR_PALETTE[0];
    private boolean pendingWireColorButtonPress = false;
    private float wireBtnCx, wireBtnCy, wireBtnR;

    private boolean pendingPropButtonPress = false;
    private float propBtnCx, propBtnCy, propBtnR;

    private boolean pendingDeleteWireButtonPress = false;
    private float deleteBtnCx, deleteBtnCy, deleteBtnR;

    private boolean pendingFlipButtonPress = false;
    private float flipBtnCx, flipBtnCy, flipBtnR;

    private boolean pendingRotateButtonPress = false;
    private float rotateBtnCx, rotateBtnCy, rotateBtnR;

    private int buttonStackIndex;

    // โหมดต่อสายแบบแตะทีละจุด: แตะจุดเริ่มต้น แล้วแตะจุดปลายทาง
    private static final float WIRE_TAP_SLOP_DP = 20f;
    private static final long HINT_DURATION_MS = 2600L;

    private boolean wireMode = false;
    private TerminalRef pendingWireStart = null;
    private boolean pendingWireModeButtonPress = false;
    private float wireModeBtnCx, wireModeBtnCy, wireModeBtnR;
    private String transientHint = null;
    private long transientHintUntilMs = 0L;

    private final Paint workspacePaint = new Paint();
    private final Paint boardPaint = new Paint();
    private final Paint boardBorderPaint = new Paint();
    private final Paint gridPaint = new Paint();
    private final Paint railPaint = new Paint();
    private final Paint railPlusLinePaint = new Paint();
    private final Paint railMinusLinePaint = new Paint();
    private final Paint railLabelPaint = new Paint();
    private final Paint boardLabelPaint = new Paint();

    private final Paint componentShadowPaint = new Paint();
    private final Paint componentSelectedPaint = new Paint();
    private final Paint componentSelectedGlowPaint = new Paint();
    private final Paint componentMovePreviewPaint = new Paint();
    private final Paint componentTextPaint = new Paint();
    private final Paint fallbackChipPaint = new Paint();
    private final Paint leadPaint = new Paint();

    private final Paint resistorBodyPaint = new Paint();
    private final Paint resistorBorderPaint = new Paint();
    private final Paint resistorBandPaint = new Paint();

    private final Paint ledBodyPaint = new Paint();
    private final Paint ledBorderPaint = new Paint();
    private final Paint ledHighlightPaint = new Paint();
    private final Paint ledGlowPaint = new Paint();

    private final Paint buttonCapPaint = new Paint();
    private final Paint buttonCapBorderPaint = new Paint();
    private final Paint buttonCenterPaint = new Paint();
    private final Paint buttonLegPaint = new Paint();
    private final Paint buttonCapPressedPaint = new Paint();
    private final Paint buttonPressedCenterPaint = new Paint();
    private final Paint buttonPressedGlowPaint = new Paint();

    private final Paint capacitorBodyPaint = new Paint();
    private final Paint capacitorBorderPaint = new Paint();
    private final Paint capacitorHighlightPaint = new Paint();

    private final Paint slideSwitchBodyPaint = new Paint();
    private final Paint slideSwitchBorderPaint = new Paint();
    private final Paint slideSwitchKnobPaint = new Paint();
    private final Paint slideSwitchKnobBorderPaint = new Paint();

    private final Paint batteryBoxPaint = new Paint();
    private final Paint batteryBoxBorderPaint = new Paint();
    private final Paint batteryClipPaint = new Paint();
    private final Paint batteryTerminalPosPaint = new Paint();
    private final Paint batteryTerminalNegPaint = new Paint();
    private final Paint batteryTextPaint = new Paint();
    private final Paint batteryWireNegPaint = new Paint();
    private final Paint batteryWirePosPaint = new Paint();
    private final Paint batteryPolarityPaint = new Paint();

    private final Paint wireShadowPaint = new Paint();
    private final Paint wirePaint = new Paint();
    private final Paint wireSelectedGlowPaint = new Paint();
    private final Paint terminalDotPaint = new Paint();
    private final Paint terminalHighlightPaint = new Paint();
    private final Paint holeHighlightPaint = new Paint();

    private final Paint wireColorButtonPaint = new Paint();
    private final Paint wireColorButtonBorderPaint = new Paint();
    private final Paint wireColorButtonLabelPaint = new Paint();
    private final Paint wireColorButtonOutlinePaint = new Paint();

    private final Paint propButtonPaint = new Paint();
    private final Paint propButtonBorderPaint = new Paint();
    private final Paint propButtonLabelPaint = new Paint();
    private final Paint propButtonOutlinePaint = new Paint();

    private final Paint deleteWireButtonPaint = new Paint();
    private final Paint deleteWireButtonBorderPaint = new Paint();
    private final Paint deleteWireButtonLabelPaint = new Paint();
    private final Paint deleteWireButtonOutlinePaint = new Paint();

    private final Paint flipButtonPaint = new Paint();
    private final Paint flipButtonBorderPaint = new Paint();
    private final Paint flipButtonLabelPaint = new Paint();
    private final Paint flipButtonOutlinePaint = new Paint();

    private final Paint rotateButtonPaint = new Paint();
    private final Paint rotateButtonBorderPaint = new Paint();
    private final Paint rotateButtonLabelPaint = new Paint();
    private final Paint rotateButtonOutlinePaint = new Paint();

    private final Paint valueLabelPaint = new Paint();
    private final Paint valueLabelOutlinePaint = new Paint();

    // โหมดจำลองแบบสด: LED ไหม้ ป้ายกระแส และแถบสถานะ
    private final Paint ledBurnPaint = new Paint();
    private final Paint simCurrentLabelPaint = new Paint();
    private final Paint simBurntLabelPaint = new Paint();
    private final Paint simBannerBgPaint = new Paint();
    private final Paint simBannerTextPaint = new Paint();

    // โหมดต่อสาย: ปุ่ม จุดเริ่มต้นที่เลือกไว้ และข้อความช่วยบอก
    private final Paint wireModeButtonPaint = new Paint();
    private final Paint wireModeButtonActivePaint = new Paint();
    private final Paint wireModeButtonBorderPaint = new Paint();
    private final Paint wireModeRingPaint = new Paint();
    private final Paint wireModeLabelPaint = new Paint();
    private final Paint wireModeLabelOutlinePaint = new Paint();
    private final Paint wireStartRingPaint = new Paint();
    private final Paint wireHintBgPaint = new Paint();
    private final Paint wireHintTextPaint = new Paint();

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

        componentMovePreviewPaint.setColor(Color.parseColor("#8800BCD4"));
        componentMovePreviewPaint.setStyle(Paint.Style.STROKE);
        componentMovePreviewPaint.setStrokeWidth(4f);
        componentMovePreviewPaint.setAntiAlias(true);
        componentMovePreviewPaint.setPathEffect(new DashPathEffect(new float[]{10f, 8f}, 0));

        componentTextPaint.setColor(Color.WHITE);
        componentTextPaint.setTextSize(20f);
        componentTextPaint.setAntiAlias(true);
        componentTextPaint.setTextAlign(Paint.Align.CENTER);
        componentTextPaint.setFakeBoldText(true);

        fallbackChipPaint.setStyle(Paint.Style.FILL);
        fallbackChipPaint.setAntiAlias(true);

        leadPaint.setColor(Color.parseColor("#9C9C9C"));
        leadPaint.setStyle(Paint.Style.STROKE);
        leadPaint.setStrokeCap(Paint.Cap.ROUND);
        leadPaint.setAntiAlias(true);

        resistorBodyPaint.setColor(Color.parseColor("#E6C68E"));
        resistorBodyPaint.setStyle(Paint.Style.FILL);
        resistorBodyPaint.setAntiAlias(true);

        resistorBorderPaint.setColor(Color.parseColor("#A3824F"));
        resistorBorderPaint.setStyle(Paint.Style.STROKE);
        resistorBorderPaint.setStrokeWidth(2.4f);
        resistorBorderPaint.setAntiAlias(true);

        resistorBandPaint.setStyle(Paint.Style.FILL);
        resistorBandPaint.setAntiAlias(true);

        ledBodyPaint.setColor(Color.parseColor("#8B1A1A"));
        ledBodyPaint.setStyle(Paint.Style.FILL);
        ledBodyPaint.setAntiAlias(true);

        ledBorderPaint.setColor(Color.parseColor("#4A0C0C"));
        ledBorderPaint.setStyle(Paint.Style.STROKE);
        ledBorderPaint.setStrokeWidth(2.4f);
        ledBorderPaint.setAntiAlias(true);

        ledHighlightPaint.setColor(Color.parseColor("#66FFFFFF"));
        ledHighlightPaint.setStyle(Paint.Style.FILL);
        ledHighlightPaint.setAntiAlias(true);

        ledGlowPaint.setStyle(Paint.Style.FILL);
        ledGlowPaint.setAntiAlias(true);

        buttonCapPaint.setColor(Color.parseColor("#C2C2C2"));
        buttonCapPaint.setStyle(Paint.Style.FILL);
        buttonCapPaint.setAntiAlias(true);

        buttonCapBorderPaint.setColor(Color.parseColor("#7A7A7A"));
        buttonCapBorderPaint.setStyle(Paint.Style.STROKE);
        buttonCapBorderPaint.setStrokeWidth(2.4f);
        buttonCapBorderPaint.setAntiAlias(true);

        buttonCenterPaint.setColor(Color.parseColor("#5A5A5A"));
        buttonCenterPaint.setStyle(Paint.Style.FILL);
        buttonCenterPaint.setAntiAlias(true);

        buttonLegPaint.setColor(Color.parseColor("#8A8A8A"));
        buttonLegPaint.setStyle(Paint.Style.FILL);
        buttonLegPaint.setAntiAlias(true);

        buttonCapPressedPaint.setColor(Color.parseColor("#A5D6A7"));
        buttonCapPressedPaint.setStyle(Paint.Style.FILL);
        buttonCapPressedPaint.setAntiAlias(true);

        buttonPressedCenterPaint.setColor(Color.parseColor("#2E7D32"));
        buttonPressedCenterPaint.setStyle(Paint.Style.FILL);
        buttonPressedCenterPaint.setAntiAlias(true);

        buttonPressedGlowPaint.setColor(Color.parseColor("#4CAF50"));
        buttonPressedGlowPaint.setStyle(Paint.Style.FILL);
        buttonPressedGlowPaint.setAntiAlias(true);

        capacitorBodyPaint.setColor(Color.parseColor("#2979FF"));
        capacitorBodyPaint.setStyle(Paint.Style.FILL);
        capacitorBodyPaint.setAntiAlias(true);

        capacitorBorderPaint.setColor(Color.parseColor("#1559B0"));
        capacitorBorderPaint.setStyle(Paint.Style.STROKE);
        capacitorBorderPaint.setStrokeWidth(2.4f);
        capacitorBorderPaint.setAntiAlias(true);

        capacitorHighlightPaint.setColor(Color.parseColor("#66FFFFFF"));
        capacitorHighlightPaint.setStyle(Paint.Style.FILL);
        capacitorHighlightPaint.setAntiAlias(true);

        slideSwitchBodyPaint.setColor(Color.parseColor("#2B2B2B"));
        slideSwitchBodyPaint.setStyle(Paint.Style.FILL);
        slideSwitchBodyPaint.setAntiAlias(true);

        slideSwitchBorderPaint.setColor(Color.parseColor("#141414"));
        slideSwitchBorderPaint.setStyle(Paint.Style.STROKE);
        slideSwitchBorderPaint.setStrokeWidth(2.2f);
        slideSwitchBorderPaint.setAntiAlias(true);

        slideSwitchKnobPaint.setColor(Color.parseColor("#F5F5F5"));
        slideSwitchKnobPaint.setStyle(Paint.Style.FILL);
        slideSwitchKnobPaint.setAntiAlias(true);

        slideSwitchKnobBorderPaint.setColor(Color.parseColor("#9C9C9C"));
        slideSwitchKnobBorderPaint.setStyle(Paint.Style.STROKE);
        slideSwitchKnobBorderPaint.setStrokeWidth(1.8f);
        slideSwitchKnobBorderPaint.setAntiAlias(true);

        batteryBoxPaint.setColor(Color.parseColor("#2B2B2B"));
        batteryBoxPaint.setStyle(Paint.Style.FILL);
        batteryBoxPaint.setAntiAlias(true);

        batteryBoxBorderPaint.setColor(Color.parseColor("#141414"));
        batteryBoxBorderPaint.setStyle(Paint.Style.STROKE);
        batteryBoxBorderPaint.setStrokeWidth(2.4f);
        batteryBoxBorderPaint.setAntiAlias(true);

        batteryClipPaint.setColor(Color.parseColor("#C97B3D"));
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

        batteryWireNegPaint.setColor(Color.parseColor("#212121"));
        batteryWireNegPaint.setStyle(Paint.Style.STROKE);
        batteryWireNegPaint.setStrokeCap(Paint.Cap.ROUND);
        batteryWireNegPaint.setAntiAlias(true);

        batteryWirePosPaint.setColor(Color.parseColor("#E53935"));
        batteryWirePosPaint.setStyle(Paint.Style.STROKE);
        batteryWirePosPaint.setStrokeCap(Paint.Cap.ROUND);
        batteryWirePosPaint.setAntiAlias(true);

        batteryPolarityPaint.setColor(Color.WHITE);
        batteryPolarityPaint.setFakeBoldText(true);
        batteryPolarityPaint.setAntiAlias(true);

        wireShadowPaint.setColor(Color.parseColor("#33000000"));
        wireShadowPaint.setStrokeWidth(WIRE_STROKE_WIDTH + WIRE_SHADOW_EXTRA);
        wireShadowPaint.setStyle(Paint.Style.STROKE);
        wireShadowPaint.setAntiAlias(true);
        wireShadowPaint.setStrokeCap(Paint.Cap.ROUND);

        wirePaint.setStrokeWidth(WIRE_STROKE_WIDTH);
        wirePaint.setStyle(Paint.Style.STROKE);
        wirePaint.setAntiAlias(true);
        wirePaint.setStrokeCap(Paint.Cap.ROUND);

        wireSelectedGlowPaint.setColor(Color.parseColor("#FFFFFF"));
        wireSelectedGlowPaint.setStyle(Paint.Style.STROKE);
        wireSelectedGlowPaint.setStrokeWidth(WIRE_STROKE_WIDTH + WIRE_SELECTED_GLOW_EXTRA);
        wireSelectedGlowPaint.setAntiAlias(true);
        wireSelectedGlowPaint.setStrokeCap(Paint.Cap.ROUND);
        wireSelectedGlowPaint.setPathEffect(new DashPathEffect(new float[]{4f, 6f}, 0));

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
        wireColorButtonLabelPaint.setTextSize(19f);
        wireColorButtonLabelPaint.setTextAlign(Paint.Align.CENTER);
        wireColorButtonLabelPaint.setFakeBoldText(true);
        wireColorButtonLabelPaint.setAntiAlias(true);

        wireColorButtonOutlinePaint.setColor(Color.parseColor("#80000000"));
        wireColorButtonOutlinePaint.setTextSize(19f);
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
        propButtonLabelPaint.setTextSize(19f);
        propButtonLabelPaint.setTextAlign(Paint.Align.CENTER);
        propButtonLabelPaint.setFakeBoldText(true);
        propButtonLabelPaint.setAntiAlias(true);

        propButtonOutlinePaint.setColor(Color.parseColor("#80000000"));
        propButtonOutlinePaint.setTextSize(19f);
        propButtonOutlinePaint.setTextAlign(Paint.Align.CENTER);
        propButtonOutlinePaint.setFakeBoldText(true);
        propButtonOutlinePaint.setStyle(Paint.Style.STROKE);
        propButtonOutlinePaint.setStrokeWidth(4f);
        propButtonOutlinePaint.setAntiAlias(true);

        deleteWireButtonPaint.setColor(Color.parseColor("#E53935"));
        deleteWireButtonPaint.setStyle(Paint.Style.FILL);
        deleteWireButtonPaint.setAntiAlias(true);

        deleteWireButtonBorderPaint.setColor(Color.WHITE);
        deleteWireButtonBorderPaint.setStyle(Paint.Style.STROKE);
        deleteWireButtonBorderPaint.setStrokeWidth(4f);
        deleteWireButtonBorderPaint.setAntiAlias(true);

        deleteWireButtonLabelPaint.setColor(Color.WHITE);
        deleteWireButtonLabelPaint.setTextSize(19f);
        deleteWireButtonLabelPaint.setTextAlign(Paint.Align.CENTER);
        deleteWireButtonLabelPaint.setFakeBoldText(true);
        deleteWireButtonLabelPaint.setAntiAlias(true);

        deleteWireButtonOutlinePaint.setColor(Color.parseColor("#80000000"));
        deleteWireButtonOutlinePaint.setTextSize(19f);
        deleteWireButtonOutlinePaint.setTextAlign(Paint.Align.CENTER);
        deleteWireButtonOutlinePaint.setFakeBoldText(true);
        deleteWireButtonOutlinePaint.setStyle(Paint.Style.STROKE);
        deleteWireButtonOutlinePaint.setStrokeWidth(4f);
        deleteWireButtonOutlinePaint.setAntiAlias(true);

        flipButtonPaint.setColor(Color.parseColor("#00897B"));
        flipButtonPaint.setStyle(Paint.Style.FILL);
        flipButtonPaint.setAntiAlias(true);

        flipButtonBorderPaint.setColor(Color.WHITE);
        flipButtonBorderPaint.setStyle(Paint.Style.STROKE);
        flipButtonBorderPaint.setStrokeWidth(4f);
        flipButtonBorderPaint.setAntiAlias(true);

        flipButtonLabelPaint.setColor(Color.WHITE);
        flipButtonLabelPaint.setTextSize(19f);
        flipButtonLabelPaint.setTextAlign(Paint.Align.CENTER);
        flipButtonLabelPaint.setFakeBoldText(true);
        flipButtonLabelPaint.setAntiAlias(true);

        flipButtonOutlinePaint.setColor(Color.parseColor("#80000000"));
        flipButtonOutlinePaint.setTextSize(19f);
        flipButtonOutlinePaint.setTextAlign(Paint.Align.CENTER);
        flipButtonOutlinePaint.setFakeBoldText(true);
        flipButtonOutlinePaint.setStyle(Paint.Style.STROKE);
        flipButtonOutlinePaint.setStrokeWidth(4f);
        flipButtonOutlinePaint.setAntiAlias(true);

        rotateButtonPaint.setColor(Color.parseColor("#6D4C41"));
        rotateButtonPaint.setStyle(Paint.Style.FILL);
        rotateButtonPaint.setAntiAlias(true);

        rotateButtonBorderPaint.setColor(Color.WHITE);
        rotateButtonBorderPaint.setStyle(Paint.Style.STROKE);
        rotateButtonBorderPaint.setStrokeWidth(4f);
        rotateButtonBorderPaint.setAntiAlias(true);

        rotateButtonLabelPaint.setColor(Color.WHITE);
        rotateButtonLabelPaint.setTextSize(19f);
        rotateButtonLabelPaint.setTextAlign(Paint.Align.CENTER);
        rotateButtonLabelPaint.setFakeBoldText(true);
        rotateButtonLabelPaint.setAntiAlias(true);

        rotateButtonOutlinePaint.setColor(Color.parseColor("#80000000"));
        rotateButtonOutlinePaint.setTextSize(19f);
        rotateButtonOutlinePaint.setTextAlign(Paint.Align.CENTER);
        rotateButtonOutlinePaint.setFakeBoldText(true);
        rotateButtonOutlinePaint.setStyle(Paint.Style.STROKE);
        rotateButtonOutlinePaint.setStrokeWidth(4f);
        rotateButtonOutlinePaint.setAntiAlias(true);

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

        ledBurnPaint.setColor(Color.parseColor("#D50000"));
        ledBurnPaint.setStyle(Paint.Style.STROKE);
        ledBurnPaint.setStrokeCap(Paint.Cap.ROUND);
        ledBurnPaint.setAntiAlias(true);

        simCurrentLabelPaint.setColor(Color.parseColor("#B3E5FC"));
        simCurrentLabelPaint.setTextAlign(Paint.Align.CENTER);
        simCurrentLabelPaint.setFakeBoldText(true);
        simCurrentLabelPaint.setAntiAlias(true);

        simBurntLabelPaint.setColor(Color.parseColor("#FF8A80"));
        simBurntLabelPaint.setTextAlign(Paint.Align.CENTER);
        simBurntLabelPaint.setFakeBoldText(true);
        simBurntLabelPaint.setAntiAlias(true);

        simBannerBgPaint.setStyle(Paint.Style.FILL);
        simBannerBgPaint.setAntiAlias(true);

        simBannerTextPaint.setColor(Color.WHITE);
        simBannerTextPaint.setTextAlign(Paint.Align.LEFT);
        simBannerTextPaint.setFakeBoldText(true);
        simBannerTextPaint.setAntiAlias(true);

        wireModeButtonPaint.setColor(Color.parseColor("#546E7A"));
        wireModeButtonPaint.setStyle(Paint.Style.FILL);
        wireModeButtonPaint.setAntiAlias(true);

        wireModeButtonActivePaint.setColor(Color.parseColor("#E65100"));
        wireModeButtonActivePaint.setStyle(Paint.Style.FILL);
        wireModeButtonActivePaint.setAntiAlias(true);

        wireModeButtonBorderPaint.setColor(Color.WHITE);
        wireModeButtonBorderPaint.setStyle(Paint.Style.STROKE);
        wireModeButtonBorderPaint.setStrokeWidth(4f);
        wireModeButtonBorderPaint.setAntiAlias(true);

        wireModeRingPaint.setColor(Color.parseColor("#FFB300"));
        wireModeRingPaint.setStyle(Paint.Style.STROKE);
        wireModeRingPaint.setAntiAlias(true);

        wireModeLabelPaint.setColor(Color.WHITE);
        wireModeLabelPaint.setTextSize(19f);
        wireModeLabelPaint.setTextAlign(Paint.Align.CENTER);
        wireModeLabelPaint.setFakeBoldText(true);
        wireModeLabelPaint.setAntiAlias(true);

        wireModeLabelOutlinePaint.setColor(Color.parseColor("#80000000"));
        wireModeLabelOutlinePaint.setTextSize(19f);
        wireModeLabelOutlinePaint.setTextAlign(Paint.Align.CENTER);
        wireModeLabelOutlinePaint.setFakeBoldText(true);
        wireModeLabelOutlinePaint.setStyle(Paint.Style.STROKE);
        wireModeLabelOutlinePaint.setStrokeWidth(4f);
        wireModeLabelOutlinePaint.setAntiAlias(true);

        wireStartRingPaint.setStyle(Paint.Style.STROKE);
        wireStartRingPaint.setAntiAlias(true);

        wireHintBgPaint.setStyle(Paint.Style.FILL);
        wireHintBgPaint.setAntiAlias(true);

        wireHintTextPaint.setColor(Color.WHITE);
        wireHintTextPaint.setTextAlign(Paint.Align.LEFT);
        wireHintTextPaint.setFakeBoldText(true);
        wireHintTextPaint.setAntiAlias(true);
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
        this.selectedWire = null;
        if (typeId != null) {
            // เลือกอุปกรณ์จากแผงเพื่อวาง = ออกจากโหมดต่อสายอัตโนมัติ
            this.wireMode = false;
            this.pendingWireStart = null;
        }
        invalidate();
    }

    public void clearSelection() {
        selectedId = null;
        selectedWire = null;
        pendingPlacementTypeId = null;
        pendingWireStart = null;
        downTerminal = null;
        downWire = null;
        draggingComponent = false;
        movedBeyondSlop = false;
        invalidate();
    }

    public void refresh() {
        validatePendingWireStart();
        resimulateIfActive();
        invalidate();
    }

    /** เปิด/ปิดโหมดต่อสาย (แตะจุดเริ่มต้น แล้วแตะจุดปลายทาง) ใช้ได้ทั้งจากปุ่มในแคนวาสและจากโค้ดภายนอก */
    public void setWireMode(boolean enabled) {
        if (wireMode == enabled) {
            invalidate();
            return;
        }
        wireMode = enabled;
        pendingWireStart = null;
        transientHint = null; // ข้อความช่วยเดิมหมดความหมายแล้ว ให้ข้อความของโหมดแสดงแทน
        if (enabled) {
            // เข้าโหมดต่อสาย: ยกเลิกการเลือกอุปกรณ์/สาย และการรอวางอุปกรณ์ เพื่อไม่ให้การแตะชนกัน
            selectedWire = null;
            if (selectedId != null) {
                selectedId = null;
                if (listener != null) listener.onComponentSelected(null);
            }
            if (pendingPlacementTypeId != null) {
                pendingPlacementTypeId = null;
                if (listener != null) listener.onPlacementTypeConsumed();
            }
        }
        invalidate();
    }

    public boolean isWireMode() {
        return wireMode;
    }

    /** ถ้าจุดเริ่มต้นที่เลือกไว้เป็นขาของอุปกรณ์ที่ถูกลบไปแล้ว (เช่น undo/ลบ) ให้ยกเลิกการเลือก */
    private void validatePendingWireStart() {
        if (pendingWireStart == null) return;
        if (!pendingWireStart.isHole
                && (model == null || model.findComponentById(pendingWireStart.componentId) == null)) {
            pendingWireStart = null;
        }
    }

    /** แสดงข้อความช่วยบอกชั่วคราวที่มุมซ้ายล่าง */
    private void showHint(String text) {
        transientHint = text;
        transientHintUntilMs = System.currentTimeMillis() + HINT_DURATION_MS;
        invalidate();
        postInvalidateDelayed(HINT_DURATION_MS + 60L);
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

    /** ระหว่างเปิดโหมดจำลอง คำนวณวงจรใหม่ทุกครั้งที่วงจรเปลี่ยน (แตะสวิตช์ ลากสาย ย้าย ลบ undo/redo) LED จึงติด/ดับทันที */
    private void resimulateIfActive() {
        if (!simulationActive || model == null) return;
        simulationResult = model.solve();
    }

    /** แจ้งว่าวงจรเปลี่ยนแล้ว: คำนวณใหม่ (ถ้ากำลังจำลองอยู่) ก่อนบอก Listener */
    private void notifyCircuitChanged() {
        resimulateIfActive();
        if (listener != null) listener.onCircuitChanged();
    }

    /** ความสว่างของ LED ดวงนี้ตามผลจำลอง: 0 = ดับ, 1 = สว่างเต็มที่ (แยกรายดวง) */
    private float ledBrightnessOf(long componentId) {
        if (!simulationActive || simulationResult == null) return 0f;
        if (!simulationResult.litLedComponentIds.contains(componentId)) return 0f;
        Double b = simulationResult.ledBrightnessById.get(componentId);
        if (b == null) return 1f;
        return (float) Math.max(0.0, Math.min(1.0, b));
    }

    /** LED ที่มีกระแสไหลแต่ไม่อยู่ในกลุ่มที่ติด = กระแสเกินพิกัด (ไหม้) */
    private boolean isLedBurnt(long componentId) {
        if (!simulationActive || simulationResult == null) return false;
        Double amps = simulationResult.componentCurrentAmps.get(componentId);
        return amps != null && amps >= 1e-6 && !simulationResult.litLedComponentIds.contains(componentId);
    }

    private String formatAmps(double amps) {
        if (amps >= 1.0) return String.format(Locale.US, "%.2f A", amps);
        return String.format(Locale.US, "%.1f mA", amps * 1000.0);
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

    /** ครึ่งหนึ่งของระยะห่างระหว่างขาอุปกรณ์ (เมื่อวางอิสระนอกบอร์ด) — สวิตช์เลื่อนมี 3 ขาจึงกว้างกว่าอุปกรณ์ 2 ขาทั่วไป */
    private float halfSpanFor(PlacedComponent pc) {
        int span = pc.typeId == ComponentType.SLIDESWITCH ? PlacedComponent.SWITCH_SPAN_COLUMNS : PlacedComponent.SPAN_COLUMNS;
        return (BreadboardGrid.CELL * span) / 2f;
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
        drawComponentMovePreview(canvas);
        drawPendingWireStart(canvas);
        drawWireColorButton(canvas);
        buttonStackIndex = 0;
        drawPropertiesButton(canvas);
        drawFlipButton(canvas);
        drawRotateButton(canvas);
        drawDeleteWireButton(canvas);
        drawSimulationBanner(canvas);
        drawWireModeButton(canvas);
        drawWireHint(canvas);
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
        drawRowLabels(canvas, BreadboardGrid.ROW_MID_SECTION, MID_ROW_LETTERS);
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

            float p1x, p1y, p2x, p2y;
            if (pc.freePlaced) {
                float halfSpan = halfSpanFor(pc);
                p1x = screenX(pc.freeX - halfSpan);
                p1y = screenY(pc.freeY);
                p2x = screenX(pc.freeX + halfSpan);
                p2y = p1y;
            } else if (pc.vertical) {
                p1x = screenX(BreadboardGrid.xFor(pc.colA));
                p1y = screenY(BreadboardGrid.yFor(pc.row));
                p2x = p1x;
                p2y = screenY(BreadboardGrid.yFor(pc.rowB));
            } else {
                p1x = screenX(BreadboardGrid.xFor(pc.colA));
                p1y = screenY(BreadboardGrid.yFor(pc.row));
                p2x = screenX(BreadboardGrid.xFor(pc.colB));
                p2y = p1y;
            }

            boolean vertical = !pc.freePlaced && pc.vertical;
            float cx = (p1x + p2x) / 2f;
            float cy = (p1y + p2y) / 2f;
            float halfLen = Math.max(Math.abs(p2x - p1x), Math.abs(p2y - p1y)) / 2f;

            float leftHoleX = cx - halfLen;
            float rightHoleX = cx + halfLen;

            float left = leftHoleX - BODY_PAD_X * zoomedScale();
            float right = rightHoleX + BODY_PAD_X * zoomedScale();
            float top = cy - BODY_PAD_Y * zoomedScale();
            float bottom = cy + BODY_PAD_Y * zoomedScale();

            RectF body = new RectF(left, top, right, bottom);

            canvas.save();
            canvas.rotate(vertical ? 90f : 0f, cx, cy);

            if (selectedId != null && selectedId == pc.id) {
                RectF glow = new RectF(body);
                glow.inset(-6f * zoomedScale(), -6f * zoomedScale());
                canvas.drawRoundRect(glow, 16f, 16f, componentSelectedGlowPaint);
            }

            RectF shadow = new RectF(body);
            shadow.inset(body.width() * 0.06f, body.height() * 0.18f);
            shadow.offset(0f, 4f * zoomedScale());
            canvas.drawOval(shadow, componentShadowPaint);

            leadPaint.setStrokeWidth(Math.max(2.6f, 4f * zoomedScale()));

            drawComponentIcon(canvas, pc, type, body, leftHoleX, rightHoleX, cy);

            if (selectedId != null && selectedId == pc.id) {
                canvas.drawRoundRect(body, 14f, 14f, componentSelectedPaint);
            }

            canvas.restore();

            drawValueLabel(canvas, pc, cx, cy, halfLen, vertical);
            drawSimulationCurrentLabel(canvas, pc, cx, cy, halfLen, vertical);
        }
    }

    private void drawComponentIcon(Canvas canvas, PlacedComponent pc, ComponentType type, RectF body,
                                   float leftHoleX, float rightHoleX, float holeY) {
        int typeId = pc.typeId;
        boolean mirrored = !pc.freePlaced && (pc.vertical ? pc.row > pc.rowB : pc.colA > pc.colB);

        if (typeId == ComponentType.BATTERY_9V) {
            drawBatteryIcon(canvas, body, pc.customVoltage > 0 ? pc.customVoltage : 9.0, mirrored, leftHoleX, rightHoleX, holeY);
        } else if (typeId == ComponentType.RESISTOR) {
            drawResistorIcon(canvas, body, mirrored, leftHoleX, rightHoleX, holeY);
        } else if (typeId == ComponentType.LED) {
            drawLedIcon(canvas, body, ledBrightnessOf(pc.id), isLedBurnt(pc.id), pc.ledColor, mirrored, leftHoleX, rightHoleX, holeY);
        } else if (typeId == ComponentType.PUSHBUTTON) {
            drawButtonIcon(canvas, body, pc.pressed, leftHoleX, rightHoleX, holeY);
        } else if (typeId == ComponentType.CAPACITOR) {
            drawCapacitorIcon(canvas, body, leftHoleX, rightHoleX, holeY);
        } else if (typeId == ComponentType.SLIDESWITCH) {
            float midHoleX = (leftHoleX + rightHoleX) / 2f;
            drawSlideSwitchIcon(canvas, body, pc.slideToB, leftHoleX, midHoleX, rightHoleX, holeY);
        } else if (typeId == ComponentType.BREADBOARD) {
            drawBreadboardIcon(canvas, body);
        } else {
            fallbackChipPaint.setColor(type.color);
            canvas.drawRoundRect(body, 12f, 12f, fallbackChipPaint);
            canvas.drawText(type.shortLabel, body.centerX(), body.centerY() + 7f, componentTextPaint);
        }
    }

    private void drawValueLabel(Canvas canvas, PlacedComponent pc, float cx, float cy, float halfLen, boolean vertical) {
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

        float topY = vertical ? cy - (halfLen + BODY_PAD_X * zoomedScale()) : cy - BODY_PAD_Y * zoomedScale();
        float y = topY - 6f * zoomedScale();
        canvas.drawText(text, cx, y, valueLabelOutlinePaint);
        canvas.drawText(text, cx, y, valueLabelPaint);
    }

    /** ป้ายกระแสใต้อุปกรณ์ (ตัวต้านทาน LED แบตเตอรี่) ขณะเปิดโหมดจำลอง */
    private void drawSimulationCurrentLabel(Canvas canvas, PlacedComponent pc, float cx, float cy, float halfLen, boolean vertical) {
        if (!simulationActive || simulationResult == null) return;
        if (pc.typeId != ComponentType.RESISTOR && pc.typeId != ComponentType.LED
                && pc.typeId != ComponentType.BATTERY_9V) return;

        Double amps = simulationResult.componentCurrentAmps.get(pc.id);
        if (amps == null) return;

        String text;
        Paint paint;
        if (pc.typeId == ComponentType.LED && isLedBurnt(pc.id)) {
            text = "ไหม้! " + formatAmps(amps);
            paint = simBurntLabelPaint;
        } else if (amps >= 0.0001) {
            text = formatAmps(amps);
            paint = simCurrentLabelPaint;
        } else {
            return;
        }

        float size = Math.max(11f, 12f * zoomedScale());
        paint.setTextSize(size);
        valueLabelOutlinePaint.setTextSize(size);

        float bottomY = vertical ? cy + (halfLen + BODY_PAD_X * zoomedScale()) : cy + BODY_PAD_Y * zoomedScale();
        float y = bottomY + size + 2f * zoomedScale();
        canvas.drawText(text, cx, y, valueLabelOutlinePaint);
        canvas.drawText(text, cx, y, paint);
    }

    /** แถบสถานะมุมซ้ายบน แสดงผลจำลองแบบสด (แทนการดู Toast ครั้งเดียว) */
    private void drawSimulationBanner(Canvas canvas) {
        if (!simulationActive || simulationResult == null) return;
        SimulationResult r = simulationResult;

        String text;
        int bg;
        if (!r.hasBattery) {
            text = "ไม่มีแบตเตอรี่ในวงจร";
            bg = 0xE6B26A00;
        } else if (!r.pathFound) {
            text = "วงจรยังไม่ครบ";
            bg = 0xE6B26A00;
        } else if (r.overCurrentDanger) {
            text = "กระแสเกิน / ลัดวงจร!";
            bg = 0xE6C62828;
        } else if (r.hasLed && !r.isLedLit()) {
            text = "LED ไม่ติด (แรงดันไม่พอ)";
            bg = 0xE6B26A00;
        } else {
            text = "วงจรทำงาน  " + formatAmps(r.currentAmps) + "  " + Math.round(r.totalResistanceOhms) + " Ω";
            bg = 0xE62E7D32;
        }

        float density = getResources().getDisplayMetrics().density;
        simBannerTextPaint.setTextSize(14f * density);
        float padX = 12f * density;
        float padY = 8f * density;
        float margin = 12f * density;

        Paint.FontMetrics fm = simBannerTextPaint.getFontMetrics();
        float textW = simBannerTextPaint.measureText(text);
        float textH = fm.descent - fm.ascent;

        RectF box = new RectF(margin, margin, margin + textW + padX * 2f, margin + textH + padY * 2f);
        simBannerBgPaint.setColor(bg);
        canvas.drawRoundRect(box, 12f * density, 12f * density, simBannerBgPaint);
        canvas.drawText(text, box.left + padX, box.top + padY - fm.ascent, simBannerTextPaint);
    }

    private void drawResistorIcon(Canvas canvas, RectF body, boolean mirrored, float leftHoleX, float rightHoleX, float holeY) {
        float cx = body.centerX();
        float cy = body.centerY();
        float bodyW = body.width() * 0.56f;
        float bodyH = body.height() * 0.40f;
        RectF res = new RectF(cx - bodyW / 2f, cy - bodyH / 2f, cx + bodyW / 2f, cy + bodyH / 2f);

        // ขาตัวต้านทาน ยึดตำแหน่งรูบอร์ดจริงเสมอ ไม่ขึ้นกับการพลิก
        canvas.drawLine(leftHoleX, holeY, res.left, cy, leadPaint);
        canvas.drawLine(res.right, cy, rightHoleX, holeY, leadPaint);
        canvas.drawCircle(leftHoleX, holeY, leadPaint.getStrokeWidth() * 0.55f, leadPaint);
        canvas.drawCircle(rightHoleX, holeY, leadPaint.getStrokeWidth() * 0.55f, leadPaint);

        // ตัวถัง + แถบสี: พลิกรอบจุดศูนย์กลางเมื่อ mirrored ทำให้ลำดับแถบสีสลับด้านให้เห็นชัด
        canvas.save();
        if (mirrored) canvas.scale(-1f, 1f, cx, cy);

        float corner = bodyH / 2f;
        canvas.drawRoundRect(res, corner, corner, resistorBodyPaint);
        canvas.drawRoundRect(res, corner, corner, resistorBorderPaint);

        int[] bandColors = {Color.parseColor("#1A1A1A"), Color.parseColor("#C62828"), Color.parseColor("#D4AF37")};
        float bandW = res.width() * 0.085f;
        float[] fractions = {0.22f, 0.43f, 0.64f};
        for (int i = 0; i < bandColors.length; i++) {
            float bx = res.left + res.width() * fractions[i];
            resistorBandPaint.setColor(bandColors[i]);
            canvas.drawRect(bx, res.top + 2f, bx + bandW, res.bottom - 2f, resistorBandPaint);
        }

        canvas.restore();
    }

    private void drawLedIcon(Canvas canvas, RectF body, float brightness, boolean burnt, int ledColor, boolean mirrored,
                             float leftHoleX, float rightHoleX, float holeY) {
        float cx = body.centerX();
        float domeTopY = body.centerY() - body.height() * 0.30f;
        float flatY = body.centerY() + body.height() * 0.02f;
        float r = body.height() * 0.26f;

        // ขา LED ยึดตำแหน่งรูบอร์ดจริงเสมอ ไม่ขึ้นกับการพลิก
        float legXLeft = cx - r * 0.55f;
        float legXRight = cx + r * 0.55f;
        float bendY = flatY + body.height() * 0.22f;

        canvas.drawLine(legXLeft, flatY, legXLeft, bendY, leadPaint);
        canvas.drawLine(legXLeft, bendY, leftHoleX, holeY, leadPaint);
        canvas.drawCircle(leftHoleX, holeY, leadPaint.getStrokeWidth() * 0.55f, leadPaint);

        canvas.drawLine(legXRight, flatY, legXRight, bendY, leadPaint);
        canvas.drawLine(legXRight, bendY, rightHoleX, holeY, leadPaint);
        canvas.drawCircle(rightHoleX, holeY, leadPaint.getStrokeWidth() * 0.55f, leadPaint);

        // โดม LED + จุดไฮไลต์: พลิกรอบจุดศูนย์กลางเมื่อ mirrored ทำให้จุดไฮไลต์สลับด้านให้เห็นชัด
        canvas.save();
        if (mirrored) canvas.scale(-1f, 1f, cx, body.centerY());

        if (brightness > 0f) {
            // ยิ่งกระแสมาก LED ยิ่งสว่าง (0.35 = ติดริบหรี่ที่ยังมองเห็น, 1.0 = สว่างเต็มที่)
            float glow = 0.35f + 0.65f * Math.min(1f, brightness);
            float glowCy = (domeTopY + flatY) / 2f;
            for (int i = 3; i >= 1; i--) {
                ledGlowPaint.setColor(ledColor);
                ledGlowPaint.setAlpha((int) ((40f / i) * glow));
                canvas.drawCircle(cx, glowCy, r * (1f + i * 0.55f), ledGlowPaint);
            }
            ledBodyPaint.setColor(blendColors(darkenColor(ledColor, 0.45f), ledColor, glow));
        } else {
            ledBodyPaint.setColor(darkenColor(ledColor, 0.45f));
        }

        Path dome = new Path();
        RectF domeRect = new RectF(cx - r, domeTopY - r, cx + r, domeTopY + r);
        dome.addArc(domeRect, 180f, 180f);
        dome.lineTo(cx + r, flatY);
        dome.lineTo(cx - r, flatY);
        dome.close();

        canvas.drawPath(dome, ledBodyPaint);
        canvas.drawPath(dome, ledBorderPaint);
        canvas.drawCircle(cx - r * 0.35f, domeTopY - r * 0.25f, r * 0.30f, ledHighlightPaint);

        if (burnt) {
            // LED กระแสเกินพิกัด: ขีดกากบาทแดงทับโดม
            float crossCy = (domeTopY + flatY) / 2f;
            float s = r * 0.55f;
            ledBurnPaint.setStrokeWidth(Math.max(3f, r * 0.22f));
            canvas.drawLine(cx - s, crossCy - s, cx + s, crossCy + s, ledBurnPaint);
            canvas.drawLine(cx + s, crossCy - s, cx - s, crossCy + s, ledBurnPaint);
        }

        canvas.restore();
    }

    private int blendColors(int from, int to, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int a = (int) (Color.alpha(from) + (Color.alpha(to) - Color.alpha(from)) * t);
        int r = (int) (Color.red(from) + (Color.red(to) - Color.red(from)) * t);
        int g = (int) (Color.green(from) + (Color.green(to) - Color.green(from)) * t);
        int b = (int) (Color.blue(from) + (Color.blue(to) - Color.blue(from)) * t);
        return Color.argb(a, r, g, b);
    }

    private int darkenColor(int color, float factor) {
        int a = Color.alpha(color);
        int r = (int) (Color.red(color) * factor);
        int g = (int) (Color.green(color) * factor);
        int b = (int) (Color.blue(color) * factor);
        return Color.argb(a, r, g, b);
    }

    private void drawButtonIcon(Canvas canvas, RectF body, boolean pressed, float leftHoleX, float rightHoleX, float holeY) {
        float cx = body.centerX();
        float cy = body.centerY();
        float capSize = body.height() * 0.58f;

        float legOffset = capSize / 2f + 5f * zoomedScale();
        float legSize = Math.max(3.4f, capSize * 0.16f);
        float[][] legPositions = {
                {-legOffset, -legOffset}, {legOffset, -legOffset},
                {-legOffset, legOffset}, {legOffset, legOffset}
        };
        for (float[] pos : legPositions) {
            canvas.drawRect(cx + pos[0] - legSize / 2f, cy + pos[1] - legSize / 2f,
                    cx + pos[0] + legSize / 2f, cy + pos[1] + legSize / 2f, buttonLegPaint);
        }

        float bendY = cy + legOffset;
        canvas.drawLine(cx - legOffset, bendY, cx - legOffset * 0.3f, bendY, leadPaint);
        canvas.drawLine(cx - legOffset * 0.3f, bendY, leftHoleX, holeY, leadPaint);
        canvas.drawCircle(leftHoleX, holeY, leadPaint.getStrokeWidth() * 0.55f, leadPaint);

        canvas.drawLine(cx + legOffset, bendY, cx + legOffset * 0.3f, bendY, leadPaint);
        canvas.drawLine(cx + legOffset * 0.3f, bendY, rightHoleX, holeY, leadPaint);
        canvas.drawCircle(rightHoleX, holeY, leadPaint.getStrokeWidth() * 0.55f, leadPaint);

        // ปุ่มที่ "กด" อยู่: ฝาปุ่มยุบลงเล็กน้อย + มีวงแหวนเรืองแสงสีเขียวรอบปุ่ม + จุดกลางเปลี่ยนเป็นสีเขียว
        if (pressed) {
            buttonPressedGlowPaint.setAlpha(70);
            canvas.drawCircle(cx, cy, capSize * 0.85f, buttonPressedGlowPaint);
        }

        float capScale = pressed ? 0.88f : 1f;
        RectF cap = new RectF(cx - capSize * capScale / 2f, cy - capSize * capScale / 2f,
                cx + capSize * capScale / 2f, cy + capSize * capScale / 2f);
        canvas.drawRoundRect(cap, 8f, 8f, pressed ? buttonCapPressedPaint : buttonCapPaint);
        canvas.drawRoundRect(cap, 8f, 8f, buttonCapBorderPaint);
        canvas.drawCircle(cx, cy, capSize * 0.24f, pressed ? buttonPressedCenterPaint : buttonCenterPaint);
    }

    private void drawCapacitorIcon(Canvas canvas, RectF body, float leftHoleX, float rightHoleX, float holeY) {
        float cx = body.centerX();
        float cy = body.centerY();
        float r = body.height() * 0.30f;
        float domeTopY = cy - body.height() * 0.20f;
        float flatY = cy + r * 0.55f;

        // ขาตัวเก็บประจุ ยึดตำแหน่งรูบอร์ดจริงเสมอ
        canvas.drawLine(leftHoleX, holeY, cx - r * 0.5f, flatY, leadPaint);
        canvas.drawCircle(leftHoleX, holeY, leadPaint.getStrokeWidth() * 0.55f, leadPaint);

        canvas.drawLine(rightHoleX, holeY, cx + r * 0.5f, flatY, leadPaint);
        canvas.drawCircle(rightHoleX, holeY, leadPaint.getStrokeWidth() * 0.55f, leadPaint);

        // ตัวถังทรงโดมสีฟ้า (ตัวเก็บประจุชนิดอิเล็กโทรไลต์ ตามรูปอ้างอิง)
        Path dome = new Path();
        RectF domeRect = new RectF(cx - r, domeTopY - r, cx + r, domeTopY + r);
        dome.addArc(domeRect, 180f, 180f);
        dome.lineTo(cx + r, flatY);
        dome.lineTo(cx - r, flatY);
        dome.close();

        canvas.drawPath(dome, capacitorBodyPaint);
        canvas.drawPath(dome, capacitorBorderPaint);
        canvas.drawCircle(cx - r * 0.35f, domeTopY - r * 0.25f, r * 0.28f, capacitorHighlightPaint);
    }

    private void drawSlideSwitchIcon(Canvas canvas, RectF body, boolean slideToB,
                                     float legAX, float legCX, float legBX, float holeY) {
        float cx = body.centerX();
        float cy = body.centerY();
        float caseW = body.width() * 0.66f;
        float caseH = body.height() * 0.46f;
        RectF switchCase = new RectF(cx - caseW / 2f, cy - caseH / 2f, cx + caseW / 2f, cy + caseH / 2f);

        // ขาทั้ง 3 ขา (A, ขั้วร่วม, B) ยึดตำแหน่งรูบอร์ดจริงเสมอ
        canvas.drawLine(legAX, holeY, switchCase.left + caseW * 0.16f, switchCase.bottom, leadPaint);
        canvas.drawCircle(legAX, holeY, leadPaint.getStrokeWidth() * 0.55f, leadPaint);

        canvas.drawLine(legCX, holeY, legCX, switchCase.bottom, leadPaint);
        canvas.drawCircle(legCX, holeY, leadPaint.getStrokeWidth() * 0.55f, leadPaint);

        canvas.drawLine(legBX, holeY, switchCase.right - caseW * 0.16f, switchCase.bottom, leadPaint);
        canvas.drawCircle(legBX, holeY, leadPaint.getStrokeWidth() * 0.55f, leadPaint);

        // ตัวเรือนสวิตช์สีดำ
        canvas.drawRoundRect(switchCase, 6f, 6f, slideSwitchBodyPaint);
        canvas.drawRoundRect(switchCase, 6f, 6f, slideSwitchBorderPaint);

        // แกนเลื่อนสีขาว แสดงตำแหน่งปัจจุบัน (ซ้าย = เลื่อนไปทาง A, ขวา = เลื่อนไปทาง B)
        float knobW = caseW * 0.40f;
        float knobH = caseH * 0.70f;
        float knobCx = slideToB ? switchCase.right - knobW * 0.65f : switchCase.left + knobW * 0.65f;
        RectF knob = new RectF(knobCx - knobW / 2f, cy - knobH / 2f, knobCx + knobW / 2f, cy + knobH / 2f);
        canvas.drawRoundRect(knob, 3f, 3f, slideSwitchKnobPaint);
        canvas.drawRoundRect(knob, 3f, 3f, slideSwitchKnobBorderPaint);
    }

    private void drawBatteryIcon(Canvas canvas, RectF body, double voltage, boolean mirrored,
                                 float leftHoleX, float rightHoleX, float holeY) {
        float bw = body.width();
        float bh = body.height();
        float cx = body.centerX();
        float cy = body.centerY();

        // ตัวแบตทรงแคปซูลแนวนอน แบ่งครึ่งเหลือง (+) และครึ่งกรมท่า (−) ตามภาพอ้างอิง
        float capsuleW = bw * BATTERY_WIDTH_RATIO;
        float capsuleH = bh * BATTERY_HEIGHT_RATIO;
        RectF capsule = new RectF(cx - capsuleW / 2f, cy - capsuleH / 2f, cx + capsuleW / 2f, cy + capsuleH / 2f);
        float cornerR = capsuleH / 2f;

        Path capsulePath = new Path();
        capsulePath.addRoundRect(capsule, cornerR, cornerR, Path.Direction.CW);

        boolean posOnLeft = !mirrored;
        int yellow = Color.parseColor("#F2C230");
        int navy = Color.parseColor("#2B2560");

        canvas.save();
        canvas.clipPath(capsulePath);
        batteryClipPaint.setColor(posOnLeft ? yellow : navy);
        canvas.drawRect(capsule.left, capsule.top, capsule.centerX(), capsule.bottom, batteryClipPaint);
        batteryBoxPaint.setColor(posOnLeft ? navy : yellow);
        canvas.drawRect(capsule.centerX(), capsule.top, capsule.right, capsule.bottom, batteryBoxPaint);
        canvas.restore();

        batteryBoxBorderPaint.setColor(Color.parseColor("#141414"));
        canvas.drawPath(capsulePath, batteryBoxBorderPaint);
        canvas.drawLine(capsule.centerX(), capsule.top, capsule.centerX(), capsule.bottom, batteryBoxBorderPaint);

        // หมวกขั้วบวก (ปุ่มนูนทรงรีสีครีม) ที่ปลายด้านขั้วบวก เหมือนภาพอ้างอิง
        float capR = capsuleH * 0.30f;
        float capCx = posOnLeft ? capsule.left + capR * 0.9f : capsule.right - capR * 0.9f;
        RectF capOval = new RectF(capCx - capR * 0.62f, cy - capR, capCx + capR * 0.62f, cy + capR);
        batteryTerminalPosPaint.setColor(Color.parseColor("#F5F3E6"));
        canvas.drawOval(capOval, batteryTerminalPosPaint);
        canvas.drawOval(capOval, batteryBoxBorderPaint);

        // สัญลักษณ์ + / − บนตัวแบต
        float symSize = Math.max(13f, capsuleH * 0.36f);
        batteryTextPaint.setTextSize(symSize);
        batteryTextPaint.setColor(Color.parseColor("#1A1A1A"));
        float plusX = posOnLeft ? capsule.left + capsule.width() * 0.28f : capsule.right - capsule.width() * 0.28f;
        canvas.drawText("+", plusX, cy + symSize * 0.34f, batteryTextPaint);

        batteryPolarityPaint.setTextAlign(Paint.Align.CENTER);
        batteryPolarityPaint.setTextSize(symSize);
        batteryPolarityPaint.setColor(Color.parseColor("#F0F0F5"));
        float minusX = posOnLeft ? capsule.right - capsule.width() * 0.28f : capsule.left + capsule.width() * 0.28f;
        canvas.drawText("−", minusX, cy + symSize * 0.34f, batteryPolarityPaint);

        // สายไฟจากปลายแบตทั้งสองด้านไปยังรูบอร์ด (ขั้วไหนอยู่ด้านไหน อิงตาม mirrored)
        float wireW = Math.max(3f, 4.2f * zoomedScale());
        batteryWireNegPaint.setStrokeWidth(wireW);
        batteryWirePosPaint.setStrokeWidth(wireW);

        float posEndX = posOnLeft ? capsule.left : capsule.right;
        float negEndX = posOnLeft ? capsule.right : capsule.left;
        float posHoleX = posOnLeft ? leftHoleX : rightHoleX;
        float negHoleX = posOnLeft ? rightHoleX : leftHoleX;

        canvas.drawLine(posEndX, cy, posHoleX, holeY, batteryWirePosPaint);
        canvas.drawCircle(posHoleX, holeY, wireW * 0.65f, batteryWirePosPaint);

        canvas.drawLine(negEndX, cy, negHoleX, holeY, batteryWireNegPaint);
        canvas.drawCircle(negHoleX, holeY, wireW * 0.65f, batteryWireNegPaint);
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

            Path path = buildWirePath(pa, pb);

            if (selectedWire != null && selectedWire.equals(c)) {
                canvas.drawPath(path, wireSelectedGlowPaint);
            }

            wirePaint.setColor(c.color);
            canvas.drawPath(path, wireShadowPaint);
            canvas.drawPath(path, wirePaint);
            canvas.drawCircle(pa[0], pa[1], 8f * zoomedScale(), terminalDotPaint);
            canvas.drawCircle(pb[0], pb[1], 8f * zoomedScale(), terminalDotPaint);
        }
    }

    /** ความสูงจุดควบคุมโค้งของสาย ใช้ร่วมกันทั้งตอนวาดและตอนตรวจการแตะ ให้เส้นที่เห็นกับพื้นที่แตะตรงกันเสมอ */
    private float wireControlY(float[] a, float[] b) {
        return (a[1] + b[1]) / 2f - 26f * zoomedScale();
    }

    private Path buildWirePath(float[] a, float[] b) {
        Path path = new Path();
        path.moveTo(a[0], a[1]);
        float midY = wireControlY(a, b);
        path.cubicTo(a[0], midY, b[0], midY, b[0], b[1]);
        return path;
    }

    /** วงแหวนที่จุดเริ่มต้นของสายที่เลือกไว้ (รอแตะจุดปลายทาง) สีวงแหวนตรงกับสีสายที่จะต่อ */
    private void drawPendingWireStart(Canvas canvas) {
        if (pendingWireStart == null) return;
        float[] p = terminalScreenPoint(pendingWireStart);
        if (p == null) {
            pendingWireStart = null;
            return;
        }
        float z = zoomedScale();
        Paint dot = pendingWireStart.isHole ? holeHighlightPaint : terminalHighlightPaint;

        wireStartRingPaint.setColor(currentWireColor);
        wireStartRingPaint.setStrokeWidth(Math.max(3f, 4f * z));
        canvas.drawCircle(p[0], p[1], 19f * z, wireStartRingPaint);
        canvas.drawCircle(p[0], p[1], 13f * z, terminalDotPaint);
        canvas.drawCircle(p[0], p[1], 9f * z, dot);
    }

    /** ปุ่มเปิด/ปิดโหมดต่อสาย อยู่มุมขวาล่าง (แยกจากกองปุ่มด้านบนขวา กันปุ่มล้นจอ) */
    private void drawWireModeButton(Canvas canvas) {
        float density = getResources().getDisplayMetrics().density;
        wireModeBtnR = 32f * density;
        wireModeBtnCx = getWidth() - wireModeBtnR - 18f * density;
        wireModeBtnCy = getHeight() - wireModeBtnR - 18f * density;

        if (wireMode) {
            wireModeRingPaint.setStrokeWidth(5f * density);
            canvas.drawCircle(wireModeBtnCx, wireModeBtnCy, wireModeBtnR + 5f * density, wireModeRingPaint);
        }
        canvas.drawCircle(wireModeBtnCx, wireModeBtnCy, wireModeBtnR,
                wireMode ? wireModeButtonActivePaint : wireModeButtonPaint);
        canvas.drawCircle(wireModeBtnCx, wireModeBtnCy, wireModeBtnR, wireModeButtonBorderPaint);

        String label = wireMode ? "เสร็จ" : "ต่อสาย";
        float textY = wireModeBtnCy + 6f * density;
        canvas.drawText(label, wireModeBtnCx, textY, wireModeLabelOutlinePaint);
        canvas.drawText(label, wireModeBtnCx, textY, wireModeLabelPaint);
    }

    private boolean isInsideWireModeButton(float sx, float sy) {
        return Math.hypot(sx - wireModeBtnCx, sy - wireModeBtnCy) <= wireModeBtnR;
    }

    /** ข้อความบอกขั้นตอนต่อสาย (หรือข้อความช่วยชั่วคราว) เหนือปุ่มต่อสาย */
    private void drawWireHint(Canvas canvas) {
        String text = null;
        int bg = 0xE6455A64;
        long now = System.currentTimeMillis();
        if (transientHint != null && now < transientHintUntilMs) {
            text = transientHint;
            bg = 0xE6B26A00;
        } else if (wireMode) {
            if (pendingWireStart == null) {
                text = "โหมดต่อสาย: แตะจุดเริ่มต้นของสาย";
            } else {
                text = "แตะจุดปลายทางเพื่อต่อสาย (แตะจุดเดิมอีกครั้งเพื่อยกเลิก)";
                bg = 0xE6E65100;
            }
        }
        if (text == null) return;

        float density = getResources().getDisplayMetrics().density;
        float margin = 12f * density;
        float padX = 12f * density;
        float padY = 8f * density;

        float textSize = 13f * density;
        wireHintTextPaint.setTextSize(textSize);
        float textW = wireHintTextPaint.measureText(text);
        float maxTextW = getWidth() - margin * 2f - padX * 2f;
        if (textW > maxTextW && textW > 0f) {
            textSize = Math.max(9f * density, textSize * maxTextW / textW);
            wireHintTextPaint.setTextSize(textSize);
            textW = wireHintTextPaint.measureText(text);
        }

        Paint.FontMetrics fm = wireHintTextPaint.getFontMetrics();
        float textH = fm.descent - fm.ascent;
        float boxBottom = wireModeBtnCy - wireModeBtnR - 12f * density;
        RectF box = new RectF(margin, boxBottom - textH - padY * 2f, margin + textW + padX * 2f, boxBottom);

        wireHintBgPaint.setColor(bg);
        canvas.drawRoundRect(box, 12f * density, 12f * density, wireHintBgPaint);
        canvas.drawText(text, box.left + padX, box.top + padY - fm.ascent, wireHintTextPaint);
    }

    /** แตะในโหมดต่อสาย: แตะจุดแรก = เลือกจุดเริ่มต้น, แตะจุดที่สอง = ต่อสาย, แตะจุดเดิม/ที่ว่าง = ยกเลิก */
    private void handleWireModeRelease(float x, float y) {
        boolean isTap = Math.hypot(x - downX, y - downY) <= WIRE_TAP_SLOP_DP * density();
        if (!isTap) return; // ลากนิ้ว = ไม่ทำอะไร (ต่อสายด้วยการแตะเท่านั้น)

        TerminalRef tapped = downTerminal;
        if (tapped == null) {
            if (downWire != null) {
                // แตะสายที่มีอยู่ = เลือกสายนั้น (เพื่อลบ)
                selectedWire = downWire;
            } else {
                selectedWire = null;
            }
            pendingWireStart = null;
            return;
        }

        selectedWire = null;
        if (pendingWireStart == null) {
            pendingWireStart = tapped;
            return;
        }
        if (pendingWireStart.equals(tapped)) {
            pendingWireStart = null; // แตะจุดเดิมซ้ำ = ยกเลิก
            return;
        }

        TerminalRef start = pendingWireStart;
        pendingWireStart = null;
        createWire(start, tapped);
    }

    private void createWire(TerminalRef a, TerminalRef b) {
        for (Connection c : model.getConnections()) {
            TerminalRef f = c.getFrom();
            TerminalRef t = c.getTo();
            if ((f.equals(a) && t.equals(b)) || (f.equals(b) && t.equals(a))) {
                showHint("มีสายเชื่อมสองจุดนี้อยู่แล้ว");
                return;
            }
        }
        listener.onBeforeChange();
        model.addConnection(a, b, currentWireColor);
        notifyCircuitChanged();
    }

    private void drawComponentMovePreview(Canvas canvas) {
        if (!draggingComponent || !movedBeyondSlop || selectedId == null || model == null) return;
        PlacedComponent sel = model.findComponentById(selectedId);
        if (sel == null) return;

        float wx = worldX(dragX);
        float wy = worldY(dragY);

        float px, py;
        if (isInsideBoard(wx, wy)) {
            int[] hole = BreadboardGrid.nearestHole(wx, wy);
            px = screenX(BreadboardGrid.xFor(hole[1]));
            py = screenY(BreadboardGrid.yFor(hole[0]));
        } else {
            px = dragX;
            py = dragY;
        }

        boolean vertical = !sel.freePlaced && sel.vertical;
        RectF ghost;
        if (vertical) {
            ghost = new RectF(
                    px - BODY_PAD_Y * zoomedScale(),
                    py - (BODY_PAD_X + 10f) * zoomedScale(),
                    px + BODY_PAD_Y * zoomedScale(),
                    py + (BODY_PAD_X + 10f) * zoomedScale());
        } else {
            ghost = new RectF(
                    px - (BODY_PAD_X + 10f) * zoomedScale(),
                    py - BODY_PAD_Y * zoomedScale(),
                    px + (BODY_PAD_X + 10f) * zoomedScale(),
                    py + BODY_PAD_Y * zoomedScale());
        }
        canvas.drawRoundRect(ghost, 14f, 14f, componentMovePreviewPaint);
    }

    private void drawWireColorButton(Canvas canvas) {
        float density = getResources().getDisplayMetrics().density;
        wireBtnR = 32f * density;
        wireBtnCx = getWidth() - wireBtnR - 18f * density;
        wireBtnCy = wireBtnR + 18f * density;

        wireColorButtonPaint.setColor(currentWireColor);
        canvas.drawCircle(wireBtnCx, wireBtnCy, wireBtnR, wireColorButtonPaint);
        canvas.drawCircle(wireBtnCx, wireBtnCy, wireBtnR, wireColorButtonBorderPaint);

        float textY = wireBtnCy + 6f * density;
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

    private float allocateButtonSlotCenterY(float r) {
        float density = getResources().getDisplayMetrics().density;
        float cy = wireBtnCy + wireBtnR + density * 16f + r
                + buttonStackIndex * (2f * r + density * 16f);
        buttonStackIndex++;
        return cy;
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
        propBtnR = 32f * density;
        propBtnCx = getWidth() - propBtnR - 18f * density;
        propBtnCy = allocateButtonSlotCenterY(propBtnR);

        canvas.drawCircle(propBtnCx, propBtnCy, propBtnR, propButtonPaint);
        canvas.drawCircle(propBtnCx, propBtnCy, propBtnR, propButtonBorderPaint);

        float textY = propBtnCy + 6f * density;
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
            notifyCircuitChanged();
            invalidate();
        } else if (pc.typeId == ComponentType.RESISTOR || pc.typeId == ComponentType.BATTERY_9V) {
            listener.onEditPropertiesRequested(pc.id, pc.typeId);
        }
    }

    private boolean isFlipButtonVisible() {
        if (selectedId == null || model == null) return false;
        PlacedComponent pc = model.findComponentById(selectedId);
        return pc != null && !pc.freePlaced;
    }

    private void drawFlipButton(Canvas canvas) {
        if (!isFlipButtonVisible()) return;

        float density = getResources().getDisplayMetrics().density;
        flipBtnR = 32f * density;
        flipBtnCx = getWidth() - flipBtnR - 18f * density;
        flipBtnCy = allocateButtonSlotCenterY(flipBtnR);

        canvas.drawCircle(flipBtnCx, flipBtnCy, flipBtnR, flipButtonPaint);
        canvas.drawCircle(flipBtnCx, flipBtnCy, flipBtnR, flipButtonBorderPaint);

        float textY = flipBtnCy + 6f * density;
        canvas.drawText("พลิก", flipBtnCx, textY, flipButtonOutlinePaint);
        canvas.drawText("พลิก", flipBtnCx, textY, flipButtonLabelPaint);
    }

    private boolean isInsideFlipButton(float sx, float sy) {
        if (!isFlipButtonVisible()) return false;
        return Math.hypot(sx - flipBtnCx, sy - flipBtnCy) <= flipBtnR;
    }

    private void handleFlipButtonTap() {
        if (selectedId == null || model == null) return;
        PlacedComponent pc = model.findComponentById(selectedId);
        if (pc == null || pc.freePlaced) return;

        listener.onBeforeChange();
        if (pc.vertical) {
            int tmp = pc.row;
            pc.row = pc.rowB;
            pc.rowB = tmp;
        } else {
            int tmp = pc.colA;
            pc.colA = pc.colB;
            pc.colB = tmp;
        }
        notifyCircuitChanged();
        invalidate();
    }

    private boolean isRotateButtonVisible() {
        if (selectedId == null || model == null) return false;
        PlacedComponent pc = model.findComponentById(selectedId);
        return pc != null && !pc.freePlaced;
    }

    private void drawRotateButton(Canvas canvas) {
        if (!isRotateButtonVisible()) return;

        float density = getResources().getDisplayMetrics().density;
        rotateBtnR = 32f * density;
        rotateBtnCx = getWidth() - rotateBtnR - 18f * density;
        rotateBtnCy = allocateButtonSlotCenterY(rotateBtnR);

        canvas.drawCircle(rotateBtnCx, rotateBtnCy, rotateBtnR, rotateButtonPaint);
        canvas.drawCircle(rotateBtnCx, rotateBtnCy, rotateBtnR, rotateButtonBorderPaint);

        float textY = rotateBtnCy + 6f * density;
        canvas.drawText("หมุน", rotateBtnCx, textY, rotateButtonOutlinePaint);
        canvas.drawText("หมุน", rotateBtnCx, textY, rotateButtonLabelPaint);
    }

    private boolean isInsideRotateButton(float sx, float sy) {
        if (!isRotateButtonVisible()) return false;
        return Math.hypot(sx - rotateBtnCx, sy - rotateBtnCy) <= rotateBtnR;
    }

    private void handleRotateButtonTap() {
        if (selectedId == null || model == null) return;
        listener.onBeforeChange();
        model.rotateOrientation(selectedId);
        notifyCircuitChanged();
        invalidate();
    }

    private boolean isDeleteWireButtonVisible() {
        return selectedWire != null;
    }

    private void drawDeleteWireButton(Canvas canvas) {
        if (!isDeleteWireButtonVisible()) return;

        float density = getResources().getDisplayMetrics().density;
        deleteBtnR = 32f * density;
        deleteBtnCx = getWidth() - deleteBtnR - 18f * density;
        deleteBtnCy = allocateButtonSlotCenterY(deleteBtnR);

        canvas.drawCircle(deleteBtnCx, deleteBtnCy, deleteBtnR, deleteWireButtonPaint);
        canvas.drawCircle(deleteBtnCx, deleteBtnCy, deleteBtnR, deleteWireButtonBorderPaint);

        float textY = deleteBtnCy + 6f * density;
        canvas.drawText("ลบสาย", deleteBtnCx, textY, deleteWireButtonOutlinePaint);
        canvas.drawText("ลบสาย", deleteBtnCx, textY, deleteWireButtonLabelPaint);
    }

    private boolean isInsideDeleteWireButton(float sx, float sy) {
        if (!isDeleteWireButtonVisible()) return false;
        return Math.hypot(sx - deleteBtnCx, sy - deleteBtnCy) <= deleteBtnR;
    }

    private void handleDeleteWireButtonTap() {
        if (selectedWire == null) return;
        listener.onBeforeChange();
        model.getConnections().remove(selectedWire);
        notifyCircuitChanged();
        selectedWire = null;
        invalidate();
    }

    private RectF componentBodyRectScreen(PlacedComponent pc) {
        float p1x, p1y, p2x, p2y;
        if (pc.freePlaced) {
            float halfSpan = halfSpanFor(pc);
            p1x = screenX(pc.freeX - halfSpan);
            p1y = screenY(pc.freeY);
            p2x = screenX(pc.freeX + halfSpan);
            p2y = p1y;
        } else if (pc.vertical) {
            p1x = screenX(BreadboardGrid.xFor(pc.colA));
            p1y = screenY(BreadboardGrid.yFor(pc.row));
            p2x = p1x;
            p2y = screenY(BreadboardGrid.yFor(pc.rowB));
        } else {
            p1x = screenX(BreadboardGrid.xFor(pc.colA));
            p1y = screenY(BreadboardGrid.yFor(pc.row));
            p2x = screenX(BreadboardGrid.xFor(pc.colB));
            p2y = p1y;
        }

        boolean vertical = !pc.freePlaced && pc.vertical;
        float cx = (p1x + p2x) / 2f;
        float cy = (p1y + p2y) / 2f;
        float halfLen = Math.max(Math.abs(p2x - p1x), Math.abs(p2y - p1y)) / 2f;

        if (vertical) {
            float left = cx - BODY_PAD_Y * zoomedScale();
            float right = cx + BODY_PAD_Y * zoomedScale();
            float top = cy - (halfLen + BODY_PAD_X * zoomedScale());
            float bottom = cy + (halfLen + BODY_PAD_X * zoomedScale());
            return new RectF(left, top, right, bottom);
        } else {
            float left = cx - (halfLen + BODY_PAD_X * zoomedScale());
            float right = cx + (halfLen + BODY_PAD_X * zoomedScale());
            float top = cy - BODY_PAD_Y * zoomedScale();
            float bottom = cy + BODY_PAD_Y * zoomedScale();
            return new RectF(left, top, right, bottom);
        }
    }

    private float[] terminalScreenPoint(TerminalRef ref) {
        if (ref.isHole) {
            return new float[]{screenX(BreadboardGrid.xFor(ref.holeCol)), screenY(BreadboardGrid.yFor(ref.holeRow))};
        }
        PlacedComponent pc = model.findComponentById(ref.componentId);
        if (pc == null) return null;
        if (pc.freePlaced) {
            float halfSpan = halfSpanFor(pc);
            if (ref.terminalIndex == 2) {
                return new float[]{screenX(pc.freeX), screenY(pc.freeY)};
            }
            float tx = ref.isA ? pc.freeX - halfSpan : pc.freeX + halfSpan;
            return new float[]{screenX(tx), screenY(pc.freeY)};
        }
        if (pc.vertical) {
            int row;
            if (ref.terminalIndex == 2) {
                row = (pc.row + pc.rowB) / 2;
            } else {
                row = ref.isA ? pc.row : pc.rowB;
            }
            return new float[]{screenX(BreadboardGrid.xFor(pc.colA)), screenY(BreadboardGrid.yFor(row))};
        }
        int col;
        if (ref.terminalIndex == 2) {
            col = (pc.colA + pc.colB) / 2;
        } else {
            col = ref.isA ? pc.colA : pc.colB;
        }
        return new float[]{screenX(BreadboardGrid.xFor(col)), screenY(BreadboardGrid.yFor(pc.row))};
    }

    private TerminalRef findComponentTerminalNear(float wx, float wy) {
        TerminalRef best = null;
        double bestDist = Double.MAX_VALUE;

        for (PlacedComponent pc : model.getComponents()) {
            float ax, ay, bx, by;
            if (pc.freePlaced) {
                float halfSpan = halfSpanFor(pc);
                ay = by = pc.freeY;
                ax = pc.freeX - halfSpan;
                bx = pc.freeX + halfSpan;
            } else if (pc.vertical) {
                ax = bx = BreadboardGrid.xFor(pc.colA);
                ay = BreadboardGrid.yFor(pc.row);
                by = BreadboardGrid.yFor(pc.rowB);
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

            if (pc.typeId == ComponentType.SLIDESWITCH) {
                float cx, cy;
                if (pc.freePlaced) {
                    cx = pc.freeX;
                    cy = pc.freeY;
                } else if (pc.vertical) {
                    cx = BreadboardGrid.xFor(pc.colA);
                    cy = BreadboardGrid.yFor((pc.row + pc.rowB) / 2);
                } else {
                    cx = BreadboardGrid.xFor((pc.colA + pc.colB) / 2);
                    cy = BreadboardGrid.yFor(pc.row);
                }
                double dc = Math.hypot(wx - cx, wy - cy);
                if (dc <= TERMINAL_HIT_RADIUS && dc < bestDist) {
                    bestDist = dc;
                    best = new TerminalRef(pc.id, 2);
                }
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

    /** รูที่ใกล้จุดแตะที่สุดภายในรัศมีที่กำหนด (กว้างกว่าการแตะเลือกอุปกรณ์ เพื่อให้แตะต่อสายง่าย) */
    private TerminalRef findNearestHole(float wx, float wy, float radius) {
        TerminalRef best = null;
        double bestDist = Double.MAX_VALUE;
        for (int row : GRID_ROWS) {
            float hy = BreadboardGrid.yFor(row);
            if (Math.abs(wy - hy) > radius) continue;
            for (int col = 0; col < BreadboardGrid.COLS; col++) {
                float hx = BreadboardGrid.xFor(col);
                double d = Math.hypot(wx - hx, wy - hy);
                if (d <= radius && d < bestDist) {
                    bestDist = d;
                    best = new TerminalRef(row, col);
                }
            }
        }
        return best;
    }

    /**
     * จุดต่อสายที่ใกล้นิ้วที่สุดในโหมดต่อสาย: ถ้าแตะตรงขาอุปกรณ์จะได้ขาอุปกรณ์ แต่ถ้าแตะรูข้างเคียง
     * (ซึ่งอยู่ในรัศมีของขาอุปกรณ์) จะได้รูนั้น ไม่ถูกขาอุปกรณ์ดึงไป
     */
    private TerminalRef findWirePointNear(float wx, float wy) {
        TerminalRef term = findComponentTerminalNear(wx, wy);
        TerminalRef hole = findNearestHole(wx, wy, BreadboardGrid.CELL * 0.75f);
        if (term == null) return hole;
        if (hole == null) return term;

        float[] tp = terminalScreenPoint(term);
        if (tp == null) return hole;
        double dTerm = Math.hypot(wx - worldX(tp[0]), wy - worldY(tp[1]));
        double dHole = Math.hypot(wx - BreadboardGrid.xFor(hole.holeCol), wy - BreadboardGrid.yFor(hole.holeRow));
        return dHole < dTerm - 1.0 ? hole : term;
    }

    /** ผลการหาสายไฟที่อยู่ใต้นิ้ว */
    private static final class WireHit {
        final Connection connection;
        /** ระยะ (พิกเซล) จากนิ้วถึงเส้นกลางสาย */
        final double distance;
        /** จุดบนสายที่ใกล้นิ้วที่สุดคือ "ปลายสาย" (นิ้วอยู่ที่ปลายหรือเลยปลายสายออกไป) */
        final boolean atEnd;

        WireHit(Connection connection, double distance, boolean atEnd) {
            this.connection = connection;
            this.distance = distance;
            this.atEnd = atEnd;
        }
    }

    private float density() {
        return getResources().getDisplayMetrics().density;
    }

    private float wireTouchRadiusPx() {
        float dp = (selectedId != null) ? WIRE_TOUCH_RADIUS_WHILE_SELECTED_DP : WIRE_TOUCH_RADIUS_DP;
        return dp * density();
    }

    private float tapSlopPx() {
        return Math.max(TAP_SLOP, TAP_SLOP_DP * density());
    }

    /**
     * ระยะ (พิกเซล) จากจุด (px, py) ถึงเส้นโค้งของสายไฟที่วาดจาก (ax, ay) ไป (bx, by)
     * โค้ง Bezier ชุดเดียวกับที่ใช้วาด (จุดควบคุม (ax,cy) และ (bx,cy)) ตัดเป็นเส้นตรงสั้น ๆ
     * แล้ววัดระยะแบบจุดถึงเส้นตรง จึงไม่มีช่องว่างระหว่างจุดตัวอย่างแบบเดิม
     */
    static double distanceToWire(float ax, float ay, float bx, float by, float cy, float px, float py) {
        double ctrlLen = Math.abs(cy - ay) + Math.abs(bx - ax) + Math.abs(by - cy);
        int n = (int) Math.min(160.0, Math.max(16.0, Math.ceil(ctrlLen / 8.0)));

        double best = Double.MAX_VALUE;
        double prevX = ax, prevY = ay;
        for (int i = 1; i <= n; i++) {
            double t = i / (double) n;
            double u = 1.0 - t;
            double w0 = u * u * u;
            double w1 = 3.0 * u * u * t;
            double w2 = 3.0 * u * t * t;
            double w3 = t * t * t;
            double x = (w0 + w1) * ax + (w2 + w3) * bx;
            double y = w0 * ay + (w1 + w2) * cy + w3 * by;

            double segX = x - prevX, segY = y - prevY;
            double len2 = segX * segX + segY * segY;
            double k = len2 > 0 ? ((px - prevX) * segX + (py - prevY) * segY) / len2 : 0.0;
            k = Math.max(0.0, Math.min(1.0, k));
            double d = Math.hypot(px - (prevX + k * segX), py - (prevY + k * segY));
            if (d < best) best = d;

            prevX = x;
            prevY = y;
        }
        return best;
    }

    /** สายไฟที่อยู่ใกล้นิ้วที่สุดภายในรัศมีแตะ (ถ้าซ้อนกันพอดี สายที่วาดทีหลัง = อยู่บนสุด ชนะ) */
    private WireHit findWireNear(float sx, float sy) {
        float radius = wireTouchRadiusPx();
        WireHit best = null;

        for (Connection c : model.getConnections()) {
            float[] pa = terminalScreenPoint(c.getFrom());
            float[] pb = terminalScreenPoint(c.getTo());
            if (pa == null || pb == null) continue;

            double d = distanceToWire(pa[0], pa[1], pb[0], pb[1], wireControlY(pa, pb), sx, sy);
            if (d > radius) continue;
            if (best == null || d <= best.distance) {
                double dEnd = Math.min(Math.hypot(sx - pa[0], sy - pa[1]), Math.hypot(sx - pb[0], sy - pb[1]));
                best = new WireHit(c, d, d >= dEnd - 1.5);
            }
        }
        return best;
    }

    /**
     * ตัดสินว่านิ้วที่แตะลงตรงนี้ "สายไฟ" หรือ "รู/ขาอุปกรณ์" ชนะ (สายไฟอยู่ชั้นบนสุดเหมือนที่เห็นบนจอ)
     * - ไม่มีรู/ขาอยู่ใกล้ → สายชนะ
     * - นิ้วอยู่ที่/เลยปลายสาย → รู/ขาตรงนั้นชนะ (ไม่ให้ปลายสายแย่งการแตะขาอุปกรณ์หรือรูที่สายต่ออยู่)
     * - นิ้วอยู่ตรงรู/ขาพอดี (ในรัศมีป้องกัน ดูค่าคงที่ด้านบน) → รู/ขาชนะ
     * - นอกนั้น (แตะข้างๆ รู/ขา แต่ใกล้เส้นสาย) → สายชนะ
     * (โหมดปกติที่ไม่ได้เลือกอะไร: แตะรูเปล่าแค่แสดงคำแนะนำ จึงให้สายชนะเสมอ ยกเว้นที่ปลายสาย/ขาอุปกรณ์)
     */
    private boolean wireBeatsPoint(WireHit hit, TerminalRef point, float sx, float sy) {
        if (point == null) return true;
        if (hit.atEnd) return false;

        float[] pp = terminalScreenPoint(point);
        if (pp == null) return true;

        float protectWorld;
        if (!point.isHole) {
            protectWorld = TERMINAL_PROTECT_RADIUS;
        } else if (wireMode) {
            protectWorld = HOLE_PROTECT_RADIUS_WIRE_MODE;
        } else if (selectedId != null) {
            protectWorld = HOLE_PROTECT_RADIUS_MOVING;
        } else {
            return true; // แตะรูเปล่า ๆ ไม่มีหน้าที่อื่นนอกจากแสดงคำแนะนำ ให้สายชนะ
        }
        double dPoint = Math.hypot(sx - pp[0], sy - pp[1]);
        return dPoint > protectWorld * zoomedScale();
    }

    /** อุปกรณ์ที่เลือกอยู่และนิ้วแตะลงบนตัวมัน (ใช้ลากย้าย) ให้ชนะสายไฟที่พาดผ่าน */
    private boolean isOnSelectedComponentBody(float sx, float sy) {
        if (wireMode || selectedId == null) return false;
        PlacedComponent sel = model.findComponentById(selectedId);
        return sel != null && componentBodyRectScreen(sel).contains(sx, sy);
    }

    /** หาว่านิ้วที่แตะลงตรงนี้ตรงกับ "จุดต่อ" หรือ "สายไฟ" แล้วเก็บไว้ใน downTerminal / downWire */
    private void resolveDownTargets(float sx, float sy) {
        TerminalRef point = wireMode
                ? findWirePointNear(worldX(sx), worldY(sy))
                : findConnectPointNear(worldX(sx), worldY(sy));

        Connection wire = null;
        // กำลังรอวางอุปกรณ์ หรือเลือกจุดเริ่มต้นสายไว้แล้ว = งานนั้นมาก่อน ไม่ให้สายแย่งการแตะ
        boolean wiresTappable = pendingPlacementTypeId == null && pendingWireStart == null;
        if (wiresTappable) {
            WireHit hit = findWireNear(sx, sy);
            if (hit != null && wireBeatsPoint(hit, point, sx, sy) && !isOnSelectedComponentBody(sx, sy)) {
                wire = hit.connection;
            }
        }

        downWire = wire;
        downTerminal = (wire != null) ? null : point;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (model == null) return true;

        scaleDetector.onTouchEvent(event);
        int action = event.getActionMasked();

        if (event.getPointerCount() >= 2) {
            if (!multiTouchActive) {
                downTerminal = null;
                downWire = null;
                draggingComponent = false;
                movedBeyondSlop = false;
                pendingWireColorButtonPress = false;
                pendingWireModeButtonPress = false;
                pendingPropButtonPress = false;
                pendingDeleteWireButtonPress = false;
                pendingFlipButtonPress = false;
                pendingRotateButtonPress = false;
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

            if (isInsideWireModeButton(x, y)) {
                pendingWireModeButtonPress = true;
                return true;
            }

            if (isInsidePropButton(x, y)) {
                pendingPropButtonPress = true;
                return true;
            }

            if (isInsideFlipButton(x, y)) {
                pendingFlipButtonPress = true;
                return true;
            }

            if (isInsideRotateButton(x, y)) {
                pendingRotateButtonPress = true;
                return true;
            }

            if (isInsideDeleteWireButton(x, y)) {
                pendingDeleteWireButtonPress = true;
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

        if (pendingWireModeButtonPress) {
            if (action == MotionEvent.ACTION_UP) {
                if (isInsideWireModeButton(x, y)) {
                    setWireMode(!wireMode);
                }
                pendingWireModeButtonPress = false;
                invalidate();
            } else if (action == MotionEvent.ACTION_CANCEL) {
                pendingWireModeButtonPress = false;
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

        if (pendingFlipButtonPress) {
            if (action == MotionEvent.ACTION_UP) {
                if (isInsideFlipButton(x, y)) {
                    handleFlipButtonTap();
                }
                pendingFlipButtonPress = false;
                invalidate();
            } else if (action == MotionEvent.ACTION_CANCEL) {
                pendingFlipButtonPress = false;
            }
            return true;
        }

        if (pendingRotateButtonPress) {
            if (action == MotionEvent.ACTION_UP) {
                if (isInsideRotateButton(x, y)) {
                    handleRotateButtonTap();
                }
                pendingRotateButtonPress = false;
                invalidate();
            } else if (action == MotionEvent.ACTION_CANCEL) {
                pendingRotateButtonPress = false;
            }
            return true;
        }

        if (pendingDeleteWireButtonPress) {
            if (action == MotionEvent.ACTION_UP) {
                if (isInsideDeleteWireButton(x, y)) {
                    handleDeleteWireButtonTap();
                }
                pendingDeleteWireButtonPress = false;
                invalidate();
            } else if (action == MotionEvent.ACTION_CANCEL) {
                pendingDeleteWireButtonPress = false;
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
                resolveDownTargets(x, y);

                draggingComponent = false;
                if (!wireMode && downTerminal == null && downWire == null && selectedId != null) {
                    PlacedComponent sel = model.findComponentById(selectedId);
                    if (sel != null && componentBodyRectScreen(sel).contains(x, y)) {
                        draggingComponent = true;
                    }
                }

                invalidate();
                return true;
            }
            case MotionEvent.ACTION_MOVE: {
                float dx = x - downX;
                float dy = y - downY;
                if (Math.hypot(dx, dy) > tapSlopPx()) movedBeyondSlop = true;

                if (draggingComponent && movedBeyondSlop) {
                    dragX = x;
                    dragY = y;
                    invalidate();
                    return true;
                }

                return true;
            }
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL: {
                // โหมดต่อสาย: ถ้าระบบยกเลิกการสัมผัสกลางคัน ไม่นับเป็นการแตะ
                if (!(wireMode && action == MotionEvent.ACTION_CANCEL)) {
                    handleRelease(x, y);
                }
                downTerminal = null;
                downWire = null;
                draggingComponent = false;
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
        if (wireMode) {
            handleWireModeRelease(x, y);
            return;
        }

        float wx = worldX(x);
        float wy = worldY(y);

        if (draggingComponent) {
            draggingComponent = false;
            if (movedBeyondSlop) {
                listener.onBeforeChange();
                moveSelectedTo(wx, wy);
                notifyCircuitChanged();
            }
            return;
        }

        // ลากนิ้วที่เริ่มจากจุดต่อ ไม่ทำอะไรแล้ว (ต่อสายด้วยการแตะ ดูโหมดต่อสาย)
        if (downTerminal != null && movedBeyondSlop) {
            return;
        }

        if (downWire != null) {
            // แตะสาย = เลือกสาย (แล้วกดปุ่ม “ลบสาย” สีแดงเพื่อลบ) ส่วนการลากที่เริ่มจากสายไม่ทำอะไร
            if (!movedBeyondSlop) {
                selectedWire = downWire;
                if (selectedId != null) {
                    selectedId = null;
                    listener.onComponentSelected(null);
                }
            }
            return;
        }

        if (pendingPlacementTypeId != null) {
            selectedWire = null;
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
            notifyCircuitChanged();
            return;
        }

        if (downTerminal != null && !downTerminal.isHole) {
            selectedWire = null;

            PlacedComponent tapped = model.findComponentById(downTerminal.componentId);
            if (tapped != null && tapped.typeId == ComponentType.PUSHBUTTON) {
                listener.onBeforeChange();
                tapped.pressed = !tapped.pressed;
                notifyCircuitChanged();
            } else if (tapped != null && tapped.typeId == ComponentType.SLIDESWITCH) {
                listener.onBeforeChange();
                tapped.slideToB = !tapped.slideToB;
                notifyCircuitChanged();
            }

            if (selectedId != null && selectedId == downTerminal.componentId) {
                selectedId = null;
                listener.onComponentSelected(null);
            } else {
                selectedId = downTerminal.componentId;
                listener.onComponentSelected(selectedId);
            }
            return;
        }

        // แตะรูบนบอร์ดตอนยังไม่ได้เลือกอะไร: บอกวิธีต่อสาย
        if (selectedId == null && downTerminal != null && downTerminal.isHole) {
            if (selectedWire == null) {
                showHint("กดปุ่ม “ต่อสาย” ที่มุมขวาล่างก่อน แล้วแตะสองจุดเพื่อต่อสาย");
            }
            selectedWire = null;
            return;
        }

        if (selectedId != null) {
            listener.onBeforeChange();
            moveSelectedTo(wx, wy);
            selectedId = null;
            listener.onComponentSelected(null);
            notifyCircuitChanged();
            return;
        }

        selectedWire = null;
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