package com.example.circuitdoctor;

public final class BreadboardGrid {

    private BreadboardGrid() {}

    public static final int COLS = 30;
    public static final float CELL = 70f;
    public static final float PADDING = 60f;

    public static final int ROW_TOP_RAIL_PLUS = 0;
    public static final int ROW_TOP_RAIL_MINUS = 1;
    public static final int[] ROW_TOP_SECTION = {3, 4, 5, 6, 7};
    public static final int[] ROW_BOTTOM_SECTION = {9, 10, 11, 12, 13};
    public static final int ROW_BOTTOM_RAIL_PLUS = 15;
    public static final int ROW_BOTTOM_RAIL_MINUS = 16;

    public static float xFor(int col) { return PADDING + col * CELL; }
    public static float yFor(int row) { return PADDING + row * CELL; }

    public static float worldWidth() { return PADDING * 2 + (COLS - 1) * CELL; }
    public static float worldHeight() { return PADDING * 2 + 16 * CELL; }

    /** คืนรหัสกลุ่มไฟฟ้าที่รูนี้สังกัด รูในกลุ่มเดียวกันถือว่าเชื่อมถึงกันโดยอัตโนมัติ (เหมือนเบรดบอร์ดจริง) */
    public static String nodeIdFor(int row, int col) {
        if (row == ROW_TOP_RAIL_PLUS) return "TR+";
        if (row == ROW_TOP_RAIL_MINUS) return "TR-";
        if (row == ROW_BOTTOM_RAIL_PLUS) return "BR+";
        if (row == ROW_BOTTOM_RAIL_MINUS) return "BR-";
        for (int r : ROW_TOP_SECTION) if (r == row) return "T" + col;
        for (int r : ROW_BOTTOM_SECTION) if (r == row) return "B" + col;
        return null;
    }

    /** หารูที่ใกล้พิกัด (worldX, worldY) มากที่สุด ใช้สำหรับ snap ตำแหน่งอุปกรณ์ */
    public static int[] nearestHole(float worldX, float worldY) {
        int[] rows = {ROW_TOP_RAIL_PLUS, ROW_TOP_RAIL_MINUS,
                ROW_TOP_SECTION[0], ROW_TOP_SECTION[1], ROW_TOP_SECTION[2], ROW_TOP_SECTION[3], ROW_TOP_SECTION[4],
                ROW_BOTTOM_SECTION[0], ROW_BOTTOM_SECTION[1], ROW_BOTTOM_SECTION[2], ROW_BOTTOM_SECTION[3], ROW_BOTTOM_SECTION[4],
                ROW_BOTTOM_RAIL_PLUS, ROW_BOTTOM_RAIL_MINUS};

        int bestRow = rows[0];
        int bestCol = 0;
        double bestDist = Double.MAX_VALUE;

        for (int row : rows) {
            for (int col = 0; col < COLS; col++) {
                double dist = Math.hypot(worldX - xFor(col), worldY - yFor(row));
                if (dist < bestDist) {
                    bestDist = dist;
                    bestRow = row;
                    bestCol = col;
                }
            }
        }
        return new int[]{bestRow, bestCol};
    }
    public static boolean isRail(int row) {
        return row == ROW_TOP_RAIL_PLUS || row == ROW_TOP_RAIL_MINUS
                || row == ROW_BOTTOM_RAIL_PLUS || row == ROW_BOTTOM_RAIL_MINUS;
    }
}