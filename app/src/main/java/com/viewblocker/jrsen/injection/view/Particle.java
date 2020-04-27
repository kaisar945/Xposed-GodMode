package com.viewblocker.jrsen.injection.view;

import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.Rect;

import java.util.Random;

/**
 * Created by jrsen on 17-10-13.
 */

public final class Particle {
    //默认小球宽高
    public static final int PART_WH = 8;
    //x值
    public float cx;
    //y值
    public float cy;
    //绘制圆的半径
    public float radius;
    //颜色
    public int color;
    //透明度
    public float alpha;
    //用于生成随机数
    static Random random = new Random();
    //粒子所在的矩形区域
    public Rect mBound;

    public static Particle generateParticle(int color, Rect bound, Point point) {
        int row = point.y; //行是高
        int column = point.x; //列是宽

        Particle particle = new Particle();
        particle.mBound = bound;
        particle.color = color;
        particle.alpha = 1f;

        particle.radius = PART_WH;
        particle.cx = bound.left + PART_WH * column;
        particle.cy = bound.top + PART_WH * row;

        return particle;
    }

    public void update(float factor) {
        cx = cx + factor * random.nextInt(mBound.width()) * (random.nextFloat() - 0.5f);

        cy = cy + factor * (mBound.height() / (random.nextInt(4) + 1));

        radius = radius - factor * random.nextInt(3);
        ;

        if (radius <= 0)
            radius = 0;
        alpha = 1f - factor;
    }

    public static Particle[][] generateParticles(Bitmap bitmap, Rect bound) {
        int w = bound.width();
        int h = bound.height();

        int partW_Count = w / Particle.PART_WH;
        int partH_Count = h / Particle.PART_WH;

        Particle[][] particles = new Particle[partH_Count][partW_Count];
        Point point = null;
        for (int row = 0; row < partH_Count; row++) { //行
            for (int column = 0; column < partW_Count; column++) { //列
                //取得当前粒子所在位置的颜色
                int color = bitmap.getPixel(column * Particle.PART_WH, row * Particle.PART_WH);

                point = new Point(column, row); //x是列，y是行

                particles[row][column] = Particle.generateParticle(color, bound, point);
            }
        }
        return particles;
    }
}
