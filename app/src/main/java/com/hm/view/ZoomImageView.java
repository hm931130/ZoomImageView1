package com.hm.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.ProviderInfo;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewConfigurationCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewTreeObserver;
import android.widget.ImageView;

/**
 * Created by Administrator on 2017/9/9/009.
 */

public class ZoomImageView extends ImageView implements ViewTreeObserver.OnGlobalLayoutListener, ScaleGestureDetector.OnScaleGestureListener, View.OnTouchListener {


    private int imgW; //控件宽度
    private int imgH; //控件高度
    private int drawableW;//图片 宽度
    private int drawableH;//图片  高度

    private Matrix mMatrix;

    private float mInitMatrixScale;
    private float mMatrixScale;
    private float mDoubleTapMatrixScale;
    private float mMaxMatrixScale;
    private boolean mOnce;

    private ScaleGestureDetector scaleGestureDetector;
    private GestureDetector gestureDetecotr;
    private boolean isAutoScale;

    public ZoomImageView(Context context) {
        this(context, null);
    }

    public ZoomImageView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ZoomImageView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mMatrix = new Matrix();
        setScaleType(ScaleType.MATRIX);
        scaleGestureDetector = new ScaleGestureDetector(context, this);
        setOnTouchListener(this);
        mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();

        gestureDetecotr = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDoubleTap(MotionEvent e) {
                float x = e.getX();
                float y = e.getY();
                if (isAutoScale) return true;
                float scale = getXscale();
                if (scale < mDoubleTapMatrixScale) {
//                    mMatrix.postScale(mDoubleTapMatrixScale / scale, mDoubleTapMatrixScale / scale, x, y);
//                    checkScaleBorderAndCenterWhenScale();
//                    setImageMatrix(mMatrix);
                    postDelayed(new AutoScaleRunnable(x, y, mDoubleTapMatrixScale), 16);
                    isAutoScale = true;

                } else {
//                    mMatrix.postScale(mInitMatrixScale / scale, mInitMatrixScale / scale, x, y);
//                    checkScaleBorderAndCenterWhenScale();
//                    setImageMatrix(mMatrix);
                    postDelayed(new AutoScaleRunnable(x, y, mInitMatrixScale), 16);
                    isAutoScale = true;
                }
                return true;
            }
        });

    }

    class AutoScaleRunnable implements Runnable {

        private float x;
        private float y;
        private float mTargetScale;

        private final static float BIGGER = 1.03f;
        private final static float SMALLER = 0.97f;

        private float scaleRatio;

        public AutoScaleRunnable(float x, float y, float mTargetScale) {
            this.x = x;
            this.y = y;
            this.mTargetScale = mTargetScale;
            if (getXscale() < mTargetScale) {
                scaleRatio = BIGGER;
            }
            if (getXscale() > mTargetScale) {
                scaleRatio = SMALLER;
            }
        }

        @Override
        public void run() {
            mMatrix.postScale(scaleRatio, scaleRatio, x, y);
            checkScaleBorderAndCenterWhenScale();
            setImageMatrix(mMatrix);
            if ((scaleRatio > 1.0f && getXscale() < mTargetScale)
                    || (scaleRatio < 1.0f && getXscale() > mTargetScale)) {
                postDelayed(this, 16);
            } else {
                float scale = mTargetScale / getXscale();
                mMatrix.postScale(scale, scale, x, y);
                checkScaleBorderAndCenterWhenScale();
                setImageMatrix(mMatrix);
                isAutoScale = false;
            }
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        getViewTreeObserver().addOnGlobalLayoutListener(this);
    }

    @SuppressLint("NewApi")
    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        getViewTreeObserver().removeOnGlobalLayoutListener(this);
    }


    /**
     * 移动图片  占满控件
     */
    @Override
    public void onGlobalLayout() {
        if (!mOnce) {
            imgW = getWidth();
            imgH = getHeight();

            Drawable drawable = getDrawable();
            if (drawable == null) return;
            drawableW = drawable.getIntrinsicWidth();
            drawableH = drawable.getIntrinsicHeight();

            float scale = 1.0f;

            /**
             * 当图片宽度大于控件宽度且高度小于控件高度
             * 缩放宽度
             */
            if (drawableW > imgW && drawableH < imgH) {
                scale = imgW * 1.0f / drawableW;
            }
            /**
             * 当图片高度大于控件高度且图片 宽度小于控件宽度
             * 只需要缩放图片的 高度
             */
            if (drawableH > imgH && drawableW < imgW) {
                scale = imgH * 1.0f / drawableH;
            }

            /**
             * 当图片宽高同时大于控件宽高 或者 图片宽高小于控件宽高
             */

            if ((drawableW > imgW && drawableH > imgH) ||
                    (drawableW < imgW && drawableH < imgH)) {
                scale = Math.min(imgW * 1.0f / drawableW, imgH * 1.0f / drawableH);
            }

            //初始化宽高形变
            mInitMatrixScale = scale;
            mDoubleTapMatrixScale = mInitMatrixScale * 2;
            mMaxMatrixScale = mInitMatrixScale * 4;
            mMatrix.postTranslate(imgW / 2 - drawableW / 2, imgH / 2 - drawableH / 2);
            mMatrix.postScale(mInitMatrixScale, mInitMatrixScale, imgW / 2, imgH / 2);
            setImageMatrix(mMatrix);

            mOnce = true;
        }
    }

    private float getXscale() {
        float[] pos = new float[9];
        mMatrix.getValues(pos);
        return pos[Matrix.MSCALE_X];
    }

    @Override
    public boolean onScale(ScaleGestureDetector detector) {

        Drawable drawable = getDrawable();
        if (drawable == null) return true;
        float mScale = getXscale();
        float mFactorScale = detector.getScaleFactor();

        if (!(mFactorScale > 1.03 || mFactorScale < 0.97)) return false;

        if (mScale < mMaxMatrixScale && mFactorScale > 1.0f
                || mScale > mInitMatrixScale && mFactorScale < 1.0f) {//没有到最大值之前 还要 继续放大 //没有缩小到最小值，还想继续 缩小

            if (mScale * mFactorScale < mInitMatrixScale) {
                mFactorScale = mInitMatrixScale / mScale;
            }
            if (mScale * mFactorScale > mMaxMatrixScale) {
                mFactorScale = mMatrixScale / mScale;
            }
            mMatrixScale = mFactorScale;

            mMatrix.postScale(mMatrixScale, mMatrixScale, detector.getFocusX(), detector.getFocusY());

            //边界检测 中心检测

            checkScaleBorderAndCenterWhenScale();
            setImageMatrix(mMatrix);
        }

        return true;
    }

    private void checkScaleBorderAndCenterWhenScale() {
        RectF rectF = getRectF();

        // 调节边界
        float delX = 1.0f;
        float delY = 1.0f;

        Drawable d = getDrawable();
        if (d == null) return;
        if (rectF.width() >= imgW) {   //图片形变后 尺寸大于控件的宽或者高
            if (rectF.left > 0) {
                delX = -rectF.left;
            }
            if (rectF.right < imgW) {
                delX = imgW - rectF.right;
            }
        }
        if (rectF.height() >= imgH) {
            if (rectF.top > 0) {
                delY = -rectF.top;
            }
            if (rectF.bottom < imgH) {
                delY = imgH - rectF.bottom;
            }
        }
        //中点检测

        if (rectF.width() < imgW) {
            delX = imgW / 2.0f - rectF.right + rectF.width() / 2.0f;
        }
        if (rectF.height() < imgH) {
            delY = imgH / 2.0f - rectF.bottom + rectF.height() / 2.0f;
        }
        mMatrix.postTranslate(delX, delY);


    }

    /**
     * 获取图片的rect
     *
     * @return
     */
    private RectF getRectF() {
        Matrix matrix = mMatrix;
        Drawable d = getDrawable();
        if (d == null) return null;
        RectF rectF = new RectF(0, 0, d.getIntrinsicWidth(), d.getIntrinsicHeight());
        matrix.mapRect(rectF);
        return rectF;
    }

    @Override
    public boolean onScaleBegin(ScaleGestureDetector detector) {
        return true;
    }

    @Override
    public void onScaleEnd(ScaleGestureDetector detector) {

    }

    private int mLastPointerCount = 0;
    private float mLastX;
    private float mLastY;
    private int mTouchSlop;
    private boolean mCanDrag;

    @Override
    public boolean onTouch(View v, MotionEvent event) {


        if (gestureDetecotr.onTouchEvent(event))
            return true;

        scaleGestureDetector.onTouchEvent(event);
        int pointerCount = event.getPointerCount();
        int x = 0;
        int y = 0;
        for (int i = 0; i < pointerCount; i++) {
            x += event.getX(i);
            y += event.getY(i);
        }
        x /= pointerCount;
        y /= pointerCount;

        if (mLastPointerCount != pointerCount) {
            mCanDrag = false;
            mLastX = x;
            mLastY = y;
        }
        mLastPointerCount = pointerCount;

        RectF rectF = getRectF();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (rectF.width() > imgW + 0.01 || rectF.height() > imgH + 0.01) {
                    getParent().requestDisallowInterceptTouchEvent(true);
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (rectF.width() > imgW + 0.01 || rectF.height() > imgH + 0.01) {
                    getParent().requestDisallowInterceptTouchEvent(true);
                }
                int delX = (int) (x - mLastX);
                int delY = (int) (y - mLastY);
                if (!mCanDrag) {
                    mCanDrag = canMoveDrag(delX, delY);
                }
                if (mCanDrag) {
                    if (rectF.width() < imgW) {
                        delX = 0;
                    }
                    if (rectF.height() < imgH) {
                        delY = 0;
                    }
                    mMatrix.postTranslate(delX, delY);

                    //进行边界检测
                    checkBorderWhenMove();

                    setImageMatrix(mMatrix);
                }
                mLastX = x;
                mLastY = y;

                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                mLastPointerCount = 0;
                break;
        }


        return true;
    }

    private void checkBorderWhenMove() {


        RectF rectF = getRectF();
        float delX, delY;
        delX = delY = 0;
        if (rectF.left > 0 && rectF.width() >= imgW) {

            delX = -rectF.left;
        }
        if (rectF.right < imgW && rectF.width() >= imgW) {

            delX = imgW - rectF.right;
        }

        if (rectF.top > 0 && rectF.height() >= imgH) {
            delY = -rectF.top;
        }
        if (rectF.bottom < imgH && rectF.height() >= imgH) {
            delY = imgH - rectF.bottom;
        }

        mMatrix.postTranslate(delX, delY);

    }

    private boolean canMoveDrag(int delX, int delY) {
        return Math.sqrt(delX * delX + delY * delY) > mTouchSlop;
    }
}
