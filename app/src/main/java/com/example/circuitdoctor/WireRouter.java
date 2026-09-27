package com.example.circuitdoctor;

import android.graphics.RectF;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.PriorityQueue;

/**
 * หาเส้นทางเดินสายไฟแบบ Manhattan (แนวตั้ง/แนวนอนเท่านั้น) ระหว่างสองจุด
 * โดยหลบกล่องขอบเขตของอุปกรณ์อื่นบนเบรดบอร์ด ใช้ Dijkstra บนกริดละเอียด
 * พร้อมบทลงโทษการหักมุม เพื่อให้เส้นทางออกมาเรียบร้อยคล้ายการเดินสายอัตโนมัติของ Tinkercad
 */
public final class WireRouter {

    private WireRouter() {}

    private static final float STEP = BreadboardGrid.CELL / 2f;
    private static final int DIR_NONE = -1;
    private static final int[] DX = {0, 1, 0, -1};
    private static final int[] DY = {-1, 0, 1, 0};

    public static List<float[]> findPath(float startX, float startY, float endX, float endY,
                                         List<RectF> obstacles) {
        int cols = (int) Math.ceil(BreadboardGrid.worldWidth() / STEP) + 2;
        int rows = (int) Math.ceil(BreadboardGrid.worldHeight() / STEP) + 2;

        int startGx = clamp(Math.round(startX / STEP), cols);
        int startGy = clamp(Math.round(startY / STEP), rows);
        int endGx = clamp(Math.round(endX / STEP), cols);
        int endGy = clamp(Math.round(endY / STEP), rows);

        boolean[][] blocked = new boolean[cols][rows];
        for (RectF r : obstacles) {
            int minGx = Math.max(0, (int) (r.left / STEP) - 1);
            int maxGx = Math.min(cols - 1, (int) (r.right / STEP) + 1);
            int minGy = Math.max(0, (int) (r.top / STEP) - 1);
            int maxGy = Math.min(rows - 1, (int) (r.bottom / STEP) + 1);
            for (int gx = minGx; gx <= maxGx; gx++) {
                for (int gy = minGy; gy <= maxGy; gy++) {
                    blocked[gx][gy] = true;
                }
            }
        }
        blocked[startGx][startGy] = false;
        blocked[endGx][endGy] = false;

        float[][] dist = new float[cols][rows];
        int[][] cameDir = new int[cols][rows];
        int[][] cameFromX = new int[cols][rows];
        int[][] cameFromY = new int[cols][rows];
        for (float[] row : dist) Arrays.fill(row, Float.MAX_VALUE);

        PriorityQueue<float[]> queue = new PriorityQueue<>((a, b) -> Float.compare(a[0], b[0]));
        dist[startGx][startGy] = 0;
        queue.add(new float[]{0, startGx, startGy, DIR_NONE});

        while (!queue.isEmpty()) {
            float[] current = queue.poll();
            int cx = (int) current[1];
            int cy = (int) current[2];
            int cdir = (int) current[3];
            float cdist = current[0];

            if (cx == endGx && cy == endGy) break;
            if (cdist > dist[cx][cy] + 0.001f) continue;

            for (int dir = 0; dir < 4; dir++) {
                int nx = cx + DX[dir];
                int ny = cy + DY[dir];
                if (nx < 0 || nx >= cols || ny < 0 || ny >= rows) continue;
                if (blocked[nx][ny] && !(nx == endGx && ny == endGy)) continue;

                float turnPenalty = (cdir != DIR_NONE && cdir != dir) ? 3f : 0f;
                float newDist = cdist + 1f + turnPenalty;

                if (newDist < dist[nx][ny]) {
                    dist[nx][ny] = newDist;
                    cameDir[nx][ny] = dir;
                    cameFromX[nx][ny] = cx;
                    cameFromY[nx][ny] = cy;
                    queue.add(new float[]{newDist, nx, ny, dir});
                }
            }
        }

        if (dist[endGx][endGy] == Float.MAX_VALUE) {
            // หาเส้นทางหลบไม่ได้เลย (ถูกล้อมรอบหมด) — ใช้เส้นตรงธรรมดาแทน ดีกว่าไม่วาดอะไรเลย
            List<float[]> straight = new ArrayList<>();
            straight.add(new float[]{startX, startY});
            straight.add(new float[]{endX, endY});
            return straight;
        }

        int wx = endGx;
        int wy = endGy;
        List<float[]> reversed = new ArrayList<>();
        reversed.add(new float[]{wx * STEP, wy * STEP});
        while (!(wx == startGx && wy == startGy)) {
            int px = cameFromX[wx][wy];
            int py = cameFromY[wx][wy];
            reversed.add(new float[]{px * STEP, py * STEP});
            wx = px;
            wy = py;
        }

        List<float[]> path = new ArrayList<>();
        for (int i = reversed.size() - 1; i >= 0; i--) path.add(reversed.get(i));

        path.set(0, new float[]{startX, startY});
        path.set(path.size() - 1, new float[]{endX, endY});

        return simplify(path);
    }

    /** รวมจุดที่เรียงเป็นเส้นตรงเดียวกันเข้าด้วยกัน ลดจุดหักมุมที่ไม่จำเป็น */
    private static List<float[]> simplify(List<float[]> path) {
        if (path.size() <= 2) return path;
        List<float[]> result = new ArrayList<>();
        result.add(path.get(0));
        for (int i = 1; i < path.size() - 1; i++) {
            float[] prev = result.get(result.size() - 1);
            float[] curr = path.get(i);
            float[] next = path.get(i + 1);
            if (!isCollinear(prev, curr, next)) result.add(curr);
        }
        result.add(path.get(path.size() - 1));
        return result;
    }

    private static boolean isCollinear(float[] a, float[] b, float[] c) {
        boolean sameX = Math.abs(a[0] - b[0]) < 1f && Math.abs(b[0] - c[0]) < 1f;
        boolean sameY = Math.abs(a[1] - b[1]) < 1f && Math.abs(b[1] - c[1]) < 1f;
        return sameX || sameY;
    }

    private static int clamp(int v, int max) {
        return Math.max(0, Math.min(v, max - 1));
    }
}