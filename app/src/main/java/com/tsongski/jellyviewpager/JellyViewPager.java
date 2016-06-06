package com.tsongski.jellyviewpager;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.support.v4.app.Fragment;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;

import com.facebook.rebound.BaseSpringSystem;
import com.facebook.rebound.SimpleSpringListener;
import com.facebook.rebound.Spring;
import com.facebook.rebound.SpringSystem;
import com.nineoldandroids.view.ViewHelper;

public class JellyViewPager extends ViewPager {
    private float mMinScale = 0.2f;

    private float mMaxScale = 0.9f;
    /**
     * 滑动的最低速度
     */
    private static float FLING_VELOCITY = 500;
    private static final int UNIT = 1000; // 计算速率的单位（毫秒）

    /**
     * 手指滑动的距离，大于此距离时，移出屏幕
     */
    private static float OUT_DISTANCE_BOUNDARY;

    /**
     * view 倾斜角度
     */
    private float mDegree = 15;

    private boolean mEnableScroll = true;

    private float mTouchSlop;

    private VelocityTracker mVelocityTracker;

    private PagerAdapter mAdapter;
    private Spring mScaleSpring;
    private Spring mTransSpring;
    private Spring mRotateSpring;

    private final BaseSpringSystem mSpringSystem = SpringSystem.create();
    private final MySpringListener mSpringListener = new MySpringListener();

    private View mCurrentView;
    private Rect mCurrentViewRect = new Rect();

    private int mCurrentItem = 0;

    private int mScreenHeight, mScreenWidth;

    private OnPageChangeListener mPageChangeListener;

    private SparseArray<Object> mData = new SparseArray<Object>();

    public JellyViewPager(Context context) {
        super(context);
        init(context, null);
    }

    public JellyViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        super.addOnPageChangeListener(new MyPageChangeListener());

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.JellyViewPager);
        mDegree = a.getFloat(R.styleable.JellyViewPager_degree, 15);
        mEnableScroll = a.getBoolean(R.styleable.JellyViewPager_enableScroll, true);
        mMinScale = a.getFloat(R.styleable.JellyViewPager_minScale, mMinScale);
        mMaxScale = a.getFloat(R.styleable.JellyViewPager_maxScale, mMaxScale);

        mScaleSpring = mSpringSystem.createSpring();
        mTransSpring = mSpringSystem.createSpring();
        mRotateSpring = mSpringSystem.createSpring();
        mScaleSpring.addListener(mSpringListener);
        mTransSpring.addListener(mSpringListener);
        mRotateSpring.addListener(mSpringListener);

        a.recycle();

        ViewConfiguration configuration = ViewConfiguration.get(context);
        mTouchSlop = configuration.getScaledTouchSlop();
    }


    @Override
    public void setAdapter(PagerAdapter adapter) {
        mAdapter = adapter;
        super.setAdapter(new ViewPagerAdapter());
    }

    @Override
    public void addOnPageChangeListener(OnPageChangeListener listener) {
        mPageChangeListener = listener;
    }

    private void resetSpring() {
        if (mTransSpring.isAtRest()) {
            mTransSpring.removeAllListeners();
            mTransSpring.setCurrentValue(0);
            mTransSpring.setEndValue(0);
            mTransSpring.addListener(mSpringListener);
        }
        if (mRotateSpring.isAtRest()) {
            mRotateSpring.removeAllListeners();
            mRotateSpring.setCurrentValue(0);
            mRotateSpring.setEndValue(0);
            mRotateSpring.addListener(mSpringListener);
        }
    }

    public void showNext() {
        resetSpring();
        animOut(mScreenWidth, 0);
    }

    public void showPre() {
        resetSpring();
        animOut(-mScreenWidth, 0);
    }

    private void nextPage() {
        super.setCurrentItem(mCurrentItem - 1, false);
    }

    private void prePage() {
        super.setCurrentItem(mCurrentItem + 1, false);
    }

    public boolean hasNext() {
        return getCurrentItem() < mAdapter.getCount() - 1;
    }

    public boolean hasPre() {
        return getCurrentItem() > 0;
    }

    private View getCurrentView() {
        View view = findViewFromObject(getCurrentItem());
        if (view == null) {
            return new View(getContext());
        }
        return view;
    }

    private class ViewPagerAdapter extends PagerAdapter {

        @Override
        public int getCount() {
            return mAdapter.getCount();
        }

        @Override
        public boolean isViewFromObject(View view, Object obj) {
            return mAdapter.isViewFromObject(view, obj);
        }

        @Override
        public void destroyItem(ViewGroup view, int position, Object object) {
            mData.remove(position);
            mAdapter.destroyItem(view, position, object);
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            Object obj = mAdapter.instantiateItem(container, position);
            mData.put(position, obj);

            return obj;
        }

        @Override
        public void setPrimaryItem(ViewGroup container, int position,
                                   Object object) {
            mAdapter.setPrimaryItem(container, position, object);
        }

        @Override
        public void finishUpdate(ViewGroup container) {
            mAdapter.finishUpdate(container);
        }

    }


    @Override
    protected void onLayout(boolean arg0, int arg1, int arg2, int arg3, int arg4) {
        super.onLayout(arg0, arg1, arg2, arg3, arg4);

        mCurrentView = getCurrentView();
        mCurrentView.getHitRect(mCurrentViewRect);
        setFirstEnteredViewScale();
        mScreenHeight = getHeight();
        mScreenWidth = getWidth();
        OUT_DISTANCE_BOUNDARY = mMaxScale * mScreenWidth / 3;
    }

    private boolean hasSetFirstScale = false;

    private void setFirstEnteredViewScale() {
        if (mCurrentView != null && !hasSetFirstScale) {
            mScaleSpring.setCurrentValue(mMinScale);
            mScaleSpring.setEndValue(mMaxScale);
            hasSetFirstScale = true;
        }
    }

    /**
     * 移出视图动画
     *
     * @param scrollDis 滑动距离
     * @param velocityX 滑动速度
     */
    private void animOut(float scrollDis, float velocityX) {
        float tranX = 0;
        mTransSpring.setOvershootClampingEnabled(true);
        // 右移
        if (velocityX > FLING_VELOCITY || (scrollDis > OUT_DISTANCE_BOUNDARY)) {
            if (hasPre()) {
                tranX = mScreenWidth;
                mRotateSpring.setAtRest();
            } else {
                // 角度回正
                mRotateSpring.setEndValue(0);
            }
        } else if (velocityX < -FLING_VELOCITY
                || (scrollDis < -OUT_DISTANCE_BOUNDARY)) {
            // 左移
            if (hasNext()) {
                tranX = -mScreenWidth;
                mRotateSpring.setAtRest();
            } else {
                mRotateSpring.setEndValue(0);
            }
        } else {
            // 不移动
            mTransSpring.setOvershootClampingEnabled(false);
            mRotateSpring.setEndValue(0);
        }
        mTransSpring.setEndValue(tranX);
    }

    private void ensureCorrectView() {
        if (mCurrentItem != getCurrentItem()) {
            mCurrentItem = getCurrentItem();
            mCurrentView = getCurrentView();
        }
    }

    private class MySpringListener extends SimpleSpringListener {
        @Override
        public void onSpringUpdate(Spring spring) {
            ensureCorrectView();

            float value = (float) spring.getCurrentValue();
            String springId = spring.getId();
            if (springId.equals(mTransSpring.getId())) {
                ViewHelper.setTranslationX(mCurrentView, value);
                if (spring.isAtRest()) {
                    if (value >= mScreenWidth) {
                        nextPage();
                    } else if (value <= -mScreenWidth) {
                        prePage();
                    }
                }
            } else if (springId.equals(mScaleSpring.getId())) {
                ViewHelper.setScaleX(mCurrentView, value);
                ViewHelper.setScaleY(mCurrentView, value);
            } else if (springId.equals(mRotateSpring.getId())) {
                ViewHelper.setRotation(mCurrentView, value);
            }
        }
    }

    private class MyPageChangeListener implements OnPageChangeListener {

        @Override
        public void onPageScrollStateChanged(int arg0) {
        }

        @Override
        public void onPageScrolled(int arg0, float arg1, int arg2) {
        }

        @Override
        public void onPageSelected(int position) {
            if (mPageChangeListener != null) {
                mPageChangeListener.onPageSelected(position);
            }
            if (mCurrentView != null) {
                mScaleSpring.setCurrentValue(mMinScale);
                mScaleSpring.setEndValue(mMaxScale);
                ViewHelper.setTranslationX(mCurrentView, 0);
                ViewHelper.setRotation(mCurrentView, 0);
            }
        }
    }

    public View findViewFromObject(int position) {
        Object o = mData.get(position);
        if (o == null) {
            return null;
        }
        if (o instanceof View) {
            return (View) o;
        } else if (o instanceof Fragment) {
            return ((Fragment) o).getView();
        }

        return new View(getContext());
    }

    private float mDownY, mDownX, mDistanceX;

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        if (!mEnableScroll) {
            return false;
        }

        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
        mVelocityTracker.addMovement(event);
        float currentX = event.getX();
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mDownY = event.getY();
                mDownX = event.getX();
                if (!mCurrentViewRect.contains((int) mDownX, (int) mDownY)) {
                    return false;
                }
                break;
            case MotionEvent.ACTION_MOVE:
                mDistanceX = currentX - mDownX;
                if (Math.abs(mDistanceX) > mTouchSlop) {
                    if (mPageChangeListener != null) {
                        mPageChangeListener.onPageScrolled(mCurrentItem, Math.abs(mDistanceX) / getWidth(), (int) mDistanceX);
                        mPageChangeListener.onPageScrollStateChanged(SCROLL_STATE_DRAGGING);
                    }
                    mTransSpring.setEndValue(mDistanceX);
                    float degree = mDegree * mDistanceX / mScreenWidth;
                    if (mDownY > mScreenHeight / 2) {
                        degree = -degree;
                    }
                    mRotateSpring.setEndValue(degree);
                }
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                if (Math.abs(mDistanceX) > mTouchSlop) {
                    if (mPageChangeListener != null) {
                        mPageChangeListener.onPageScrollStateChanged(SCROLL_STATE_SETTLING);
                    }
                    final VelocityTracker tracker = mVelocityTracker;
                    tracker.computeCurrentVelocity(UNIT);
                    float velocityX = tracker.getXVelocity();
                    animOut(currentX - mDownX, velocityX);
                    if (mVelocityTracker != null) {
                        mVelocityTracker.recycle();
                        mVelocityTracker = null;
                    }
                }
                break;
        }
        return true;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {

        if (!mEnableScroll) {
            return false;
        }

        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
        mVelocityTracker.addMovement(event);
        float currentX = event.getX();
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mDownY = event.getY();
                mDownX = event.getX();
                resetSpring();
                break;
            case MotionEvent.ACTION_MOVE:
                float distance = Math.abs(currentX - mDownX);
                if (distance > mTouchSlop) {
                    return true;
                }
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                final VelocityTracker tracker = mVelocityTracker;
                tracker.computeCurrentVelocity(UNIT);
                float velocityX = tracker.getXVelocity();
                if (mVelocityTracker != null) {
                    mVelocityTracker.recycle();
                    mVelocityTracker = null;
                }
                if (velocityX > FLING_VELOCITY) {
                    return true;
                }
                break;
        }
        return false;
    }

    public void setEnableScroll(boolean enableScroll) {
        mEnableScroll = enableScroll;
    }

    @Override
    public void setCurrentItem(int item) {
        super.setCurrentItem(item);
        mCurrentItem = item;
    }
}
