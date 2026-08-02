package com.baidu.mobads.demo.main.tools;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Build;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.baidu.mobads.sdk.api.IBasicCPUData;
import com.baidu.mobads.sdk.api.NativeResponse;


public class CustomProgressButton extends View {

    private static final String TAG = CustomProgressButton.class.getSimpleName();
    /** 下载按钮初始文案 */
    private static final String DOWNLOAD_IMMEDIATE = "立即下载";
    private int mProgress = -1;
    private Paint mPaint;
    /** 画边框的画笔 */
    private Paint strokePaint;
    /** 边框画笔的颜色,这里默认为蓝色 */
    private int stokeColor = Color.parseColor("#3388FF");
    /** 边框画笔的宽 */
    private int stokeWidth = 3;
    /** 是否有边框 */
    private boolean isStoke = false;
    private String mText;
    private float mTextSize = 36;
    private int mTextColor = Color.WHITE;
    private Typeface mTypeFace;
    private int mForegroundColor = Color.parseColor("#3388FF");
    private int mBackgroundColor;
    private int mMaxProgress = 100;
    private float mCorner = 12.0F; // 圆角的弧度
    private String mPackageName = "";
    private PorterDuffXfermode mPorterDuffXfermode = new PorterDuffXfermode(PorterDuff.Mode.SRC_IN);
    /** 信息流广告 */
    private NativeResponse nrAd;
    /** 内容联盟广告 */
    private IBasicCPUData mCpuAd;
    /** 是否使用长按的能力,默认不使用 */
    boolean mLongClickEnable = false;
    /** 记录button状态，0为初始状态。1为触发过长按的状态.2为本次触发长按失败的状态(距离超了) */
    int mButtonStatus = 0;
    /** 开始点击的x */
    float mStartX = 0;
    /** 开始点击的y */
    float mStartY = 0;
    /** 过多长时间算长按 */
    long mLongClickTime = 2000;
    /** 当长按触发的时候up事件是否拦截,默认是拦截的因为要处理click事件 */
    boolean mUpEventIntercept = true;

    public CustomProgressButton(Context context) {
        super(context);
        initPaint();
    }

    public CustomProgressButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        initPaint();
    }

    public CustomProgressButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initPaint();
    }

    public void setMax(int max) {
        if (max > 0) {
            mMaxProgress = max;
        }
    }

    public void initWithCPUResponse(IBasicCPUData nrAd) {
        mCpuAd = nrAd;
        int downloadStatus = nrAd.getDownloadStatus();
        if (downloadStatus == -1 || downloadStatus == 0) {
            mText = DOWNLOAD_IMMEDIATE;
        } else if (downloadStatus > 0 && downloadStatus < 101) {
            mProgress =  downloadStatus;
            mText =  downloadStatus + "%";
        } else if (downloadStatus == 101) {
            mText = "点击安装";
        }
        invalidate();
    }


    // 必须调用以初始化数据
    public void initWithResponse(NativeResponse nrAd) {
        this.nrAd = nrAd;
        if (this.nrAd == null) {
            mText = "查看详情";
            return;
        }
        int status = nrAd.getDownloadStatus();
        if (status >= 0 && status < 101) {
            if (nrAd.getAdActionType() == NativeResponse.ACTION_TYPE_APP_DOWNLOAD) {
                mText = DOWNLOAD_IMMEDIATE;
            } else {
                mText = "查看详情";
            }

            String actButtonString = nrAd.getActButtonString();
            if (!TextUtils.isEmpty(actButtonString)) {
                mText = actButtonString;
            }
        } else {
            updateStatus(nrAd);
        }

    }

    public void setForegroundColor(int color) {
        mForegroundColor = color;
    }

    public void setBackgroundColor(int color) {
        mBackgroundColor = color;
    }

    public void setCornerRadius(int radius) {
        mCorner = radius;
    }

    public void setPackageName(String pk) {
        mPackageName = pk;
    }

    public void setText(String text) {
        mText = text;
    }

    public void setStroke(boolean isStoke) {
        this.isStoke = isStoke;
    }

    public void setTextColor(int color) {
        mTextColor = color;
    }

    public void setTextSize(int size) {
        mTextSize = size;
    }

    public void setTypeFace(Typeface typeFace) {
        mTypeFace = typeFace;
    }

    public void setProgress() {
        if (this.mCpuAd != null) {
            updateCpuStatus(this.mCpuAd);
        } else if (this.nrAd != null) {
            updateStatus(this.nrAd);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mProgress < 0 || mProgress >= mMaxProgress) {
            // 不带进度的情况
            mPaint.setColor(this.mForegroundColor);
            myDrawRoundRect(canvas, 0, 0, getWidth(), getHeight(), mCorner, mPaint);
            // 按同样的大小绘制一个边框
            if (isStoke) {
                // 如果设置了就会有边框
                myDrawRoundRect(canvas, 1, 1, getWidth() - 1,
                        getHeight() - 1, mCorner, strokePaint);
            }
            drawTextInCenter(canvas, mText, mPaint, mTextColor, mTextSize, mTypeFace);
        } else {
            // 绘制背景
            drawProgressBackground(canvas);
            // 绘制文字
            if (!TextUtils.isEmpty(mText)) {
                drawProgressText(canvas);
            }
        }
    }

    private void drawProgressBackground(Canvas canvas) {
        Bitmap bgBuffer = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
        Canvas bgCanvas = new Canvas(bgBuffer);
        // 绘制底色bitmap
        mPaint.setColor(this.mBackgroundColor);
        myDrawRoundRect(bgCanvas, 0, 0, getWidth(), getHeight(), mCorner, mPaint);
        // 绘制涂层
        drawBitmapWithXfermode(bgCanvas, mPaint, mForegroundColor);
        // 绘制背景至canvas
        canvas.drawBitmap(bgBuffer, 0, 0, null);
        // 回收数据
        if (!bgBuffer.isRecycled()) {
            bgBuffer.recycle();
        }

    }

    private void drawProgressText(Canvas canvas) {
        Bitmap textBuffer = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
        Canvas textCanvas = new Canvas(textBuffer);
        // 绘制文字bitmap
        drawTextInCenter(textCanvas, mText, mPaint, mForegroundColor, mTextSize, mTypeFace);
        // 绘制涂层
        drawBitmapWithXfermode(textCanvas, mPaint, mTextColor);
        // 绘制文字至canvas
        canvas.drawBitmap(textBuffer, 0, 0, null);
        // 回收数据
        if (!textBuffer.isRecycled()) {
            textBuffer.recycle();
        }

    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // 拦截事件
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                // 如果需要长按的能力的话需要对判断长按的逻辑进行处理
                if (mLongClickEnable) {
                    // down作为一个的起始，记录button状态
                    mButtonStatus = 0;
                    // 给初始点击的x和y进行赋值
                    mStartX = event.getX();
                    mStartY = event.getY();
                    // 给触发长按之后是否拦截up事件设置一个初始值，默认拦截
                    mUpEventIntercept = true;
                }
                break;
            case MotionEvent.ACTION_MOVE:
                // 触发长按的判断有4个：
                // 1、需要开启长按能力
                // 2、status为0，初始状态
                // 3、distanceCompliance()为true，满足距离条件
                // 4、时间大于定量的时间
                // 如果满足长按能力的话触发长按回调并且把状态至为长按已触发的状态
                if (mLongClickEnable && (mButtonStatus == 0)) {
                    // 如果距离合规的话还需要看一下时间，否则修改状态为本次不合规的状态
                    if (distanceCompliance(event)) {
                        // 如果时间合格的话触发
                        if ((event.getEventTime() - event.getDownTime()) > mLongClickTime) {
                            // 取消下载
                            downloadBtnLongClick();
                            // 触发长按的回调并且把状态置为1，说明这次的down已经触发过长按了
                            mButtonStatus = 1;
                        }
                    } else {
                        // 超距离，长按失败
                        mButtonStatus = 2;
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
                // 如果需要长按的能力的话需要对判断长按的逻辑进行处理
                if (mLongClickEnable) {
                    if (mButtonStatus == 0) {
                        // 如果mButtonStatus = 0说明这次点击没有触发过长按
                        // 防止有些手机厂商在x、y不变的时候不回调move，直接回调up导致没有长按功能，
                        // 所以在up的时候也判断一下是否符合长按的条件
                        if (distanceCompliance(event) &&
                                ((event.getEventTime() - event.getDownTime()) > mLongClickTime)) {
                            // 如果符合长按逻辑话，取消下载
                            downloadBtnLongClick();
                        } else {
                            // 走click的逻辑
                            downloadBtnShortClick();
                        }
                    } else if (mButtonStatus == 2) {
                        // 如果mButtonStatus = 2说明这次长按不合规了，触发原本的up
                        downloadBtnShortClick();
                    }
                } else {
                    downloadBtnShortClick();
                }
                break;
            default:
        }
        return true;
    }

    /**
     * 长按按钮，走下载取消逻辑
     */
    private void downloadBtnLongClick() {
        if (nrAd != null) {
            nrAd.cancelAppDownload();
            updateStatus(nrAd);
        } else if (mCpuAd != null) {
            mCpuAd.cancelAppDownload();
            updateCpuStatus(mCpuAd);
        }
    }

    /**
     * 单击按钮，走下载暂停逻辑
     */
    private void downloadBtnShortClick() {
        if (nrAd != null) {
            // 获取下载状态
            int status = nrAd.getDownloadStatus();
            if (0 < status && status < 101) {
                // 暂停下载
                nrAd.pauseAppDownload();
                updateStatus(nrAd);
            } else {
                // 调用performClick以免与OnClick冲突
                performClick();
                nrAd.resumeAppDownload();
                // 处理点击，恢复下载
                updateStatus(nrAd);
            }
        } else if (mCpuAd != null) {
            // 获取下载状态
            int status = mCpuAd.getDownloadStatus();
            if (0 < status && status < 101) {
                // 暂停下载
                mCpuAd.pauseAppDownload();
                updateCpuStatus(mCpuAd);
            } else {
                // 调用performClick以免与OnClick冲突
                performClick();
                // 处理点击，恢复下载
                mCpuAd.handleCreativeView(this);
                updateCpuStatus(mCpuAd);
            }
        }
    }


    private void initPaint() {
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setStrokeWidth(3);
        // 添加画笔绘制边框
        strokePaint = new Paint();
        strokePaint.setAntiAlias(true);
        strokePaint.setColor(stokeColor);
        strokePaint.setStrokeWidth(stokeWidth);
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeJoin(Paint.Join.ROUND);

    }

    private void drawTextInCenter(Canvas canvas, String text, Paint paint,
                                  int color, float size, Typeface typeface) {
        if (canvas != null && paint != null && !TextUtils.isEmpty(text)) {
            paint.setColor(color);
            paint.setTextSize(size);
            if (typeface != null) {
                paint.setTypeface(typeface);
            }
            Paint.FontMetrics fm = paint.getFontMetrics();
            float textCenterVerticalBaselineY = (float) (getHeight() / 2) - fm.descent + (fm.descent - fm.ascent) / 2;
            canvas.drawText(text, (getMeasuredWidth() - paint.measureText(text)) / 2,
                    textCenterVerticalBaselineY, paint);
        }
    }


    private void drawBitmapWithXfermode(Canvas bitmapCanvas, Paint paint, int color) {
        // 设置混合模式
        paint.setXfermode(mPorterDuffXfermode);
        paint.setColor(color);
        // 绘制涂层
        myDrawRoundRect(bitmapCanvas, 0, 0, getWidth() * mProgress / mMaxProgress, getHeight(), 0, paint);
        // 清除混合模式
        paint.setXfermode(null);
    }

    private void myDrawRoundRect(Canvas canvas, int left, int top, int right, int bottom,
                                 float corner, Paint paint) {
        paint.setAntiAlias(true);
        if (Build.VERSION.SDK_INT >= 21) {
            canvas.drawRoundRect(left, top, right, bottom, corner, corner, paint);
        } else {
            RectF rectF = new RectF(left, top, right, bottom);
            canvas.drawRoundRect(rectF, corner, corner, paint);
        }
    }

    public void updateStatus(NativeResponse nrAd) {
        int status = nrAd.getDownloadStatus();
        if (status < 0) {
            mProgress = mMaxProgress;
            if (nrAd.getAdActionType() == NativeResponse.ACTION_TYPE_APP_DOWNLOAD) {
                mText = DOWNLOAD_IMMEDIATE;
            } else {
                mText = "去看看";
            }
            String actButtonString = nrAd.getActButtonString();
            if (!TextUtils.isEmpty(actButtonString)) {
                mText = actButtonString;
            }
        } else if (status < 101) {
            mText = status + "%";
            mProgress = status;
        } else if (status == 101) {
            mProgress = mMaxProgress;
            if (nrAd.getAdActionType() == NativeResponse.ACTION_TYPE_APP_DOWNLOAD) {
                mText = "点击安装";
            } else {
                mText = "去看看";
            }
        } else if (status == 102) {
            mText = "继续下载";
        } else if (status == 104) {
            mText = "重新下载";
            mProgress = mMaxProgress;
        }
        postInvalidate();
    }

    private void updateCpuStatus(IBasicCPUData cpuad) {
        int status = cpuad.getDownloadStatus();
        if (status < 0) {
            mProgress = mMaxProgress;
            mText = DOWNLOAD_IMMEDIATE;
        } else if (status < 101) {
            mText = status + "%";
            mProgress = status;
        } else if (status == 101) {
            mProgress = mMaxProgress;
            mText = "点击安装";
        } else if (status == 102) {
            mText = "继续下载";
        } else if (status == 104) {
            mText = "重新下载";
            mProgress = mMaxProgress;
        }
        invalidate();
    }

    public void useLongClick(boolean longClickEnable) {
        mLongClickEnable = longClickEnable;
    }
    // 距离是否合规
    public boolean distanceCompliance (MotionEvent event) {
        if (event == null) {
            return false;
        }
        try {
            // 算一下x和初始比移动了多少
            float moveX = Math.abs(mStartX - event.getX());
            // 算一下y和初始比移动了多少
            float moveY = Math.abs(mStartY - event.getY());
            // 距离合格的条件：1、x移动不超过20px。2、y移动不超过20px。
            return  (moveX < 20) && (moveY < 20);
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
        return false;
    }
}
