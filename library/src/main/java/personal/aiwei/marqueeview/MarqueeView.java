package personal.aiwei.marqueeview;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.database.DataSetObserver;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;

import java.util.ArrayList;
import java.util.List;

/**
 * 滚动轮播控件
 * <p>
 * Created by Ai Wei on 2019/4/14
 */
public class MarqueeView extends ViewGroup {
    private static final int INDEX_INIT = -1;

    private static final int DEFAULT_ANIM_DURATION = 1000;
    private static final int DEFAULT_STILL_DURATION = 3000;

    private int mWidthMeasureSpec;
    private int mHeightMeasureSpec;

    private Adapter mAdapter;
    private AdapterDataSetObserver mDataSetObserver;

    private SparseArray<List<View>> mCacheViewTypeToViews = new SparseArray<>();
    private int mIndex = INDEX_INIT;
    private float mLayoutOffsetPercent = .0f; // 0.0-1.0f

    private ValueAnimator mAnimator;
    private long mAnimDuration;
    private long mStillDuration;

    public MarqueeView(Context context) {
        this(context, null);
    }

    public MarqueeView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MarqueeView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.MarqueeView, defStyleAttr, 0);
        setAnimDuration(typedArray.getInt(R.styleable.MarqueeView_animDuration, DEFAULT_ANIM_DURATION));
        setStillDuration(typedArray.getInt(R.styleable.MarqueeView_stillDuration, DEFAULT_STILL_DURATION));
        typedArray.recycle();

        init(context);
    }

    public void setAnimDuration(long animDuration) {
        mAnimDuration = animDuration;
        if (mAnimDuration <= 0) {
            mAnimDuration = DEFAULT_ANIM_DURATION;
        }
    }

    public void setStillDuration(long stillDuration) {
        mStillDuration = stillDuration;
        if (mStillDuration <= 0) {
            mStillDuration = DEFAULT_STILL_DURATION;
        }
    }

    private void init(Context context) {
    }

    public void setAdapter(Adapter adapter) {
        if (mAdapter != null && mDataSetObserver != null) {
            mAdapter.unregisterDataSetObserver(mDataSetObserver);
        }

        resetList();

        mAdapter = adapter;

        if (mAdapter != null) {
            mDataSetObserver = new AdapterDataSetObserver();
            mAdapter.registerDataSetObserver(mDataSetObserver);
        }

        requestLayout();
    }

    public Adapter getAdapter() {
        return mAdapter;
    }

    private void resetList() {
        removeAllViewsInLayout();

        mCacheViewTypeToViews.clear();
        mIndex = INDEX_INIT;
        mLayoutOffsetPercent = .0f;

        stopPlay();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        mWidthMeasureSpec = widthMeasureSpec;
        mHeightMeasureSpec = heightMeasureSpec;

        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        resetPosition(-getChildCount());

        for (int i = 0; i < getChildCount(); i++) {
            recycleView(getChildAt(i));
        }
        removeAllViewsInLayout();

        while (needAddView()) {
            addNextView();
        }
    }

    private void resetPosition(int offset) {
        if (mAdapter == null || mAdapter.getCount() <= 0 || mIndex < 0) {
            mIndex = INDEX_INIT;
        } else {
            mIndex += offset;
            while (mIndex < 0) {
                mIndex += mAdapter.getCount();
            }
        }
    }

    private void recycleView(View view) {
        LayoutParams layoutParams = (LayoutParams) view.getLayoutParams();
        List<View> views = mCacheViewTypeToViews.get(layoutParams.viewType);
        if (views == null) {
            views = new ArrayList<>();
            mCacheViewTypeToViews.put(layoutParams.viewType, views);
        }
        views.add(view);
    }

    private View getRecycledView(int viewType) {
        List<View> views = mCacheViewTypeToViews.get(viewType);
        if (views != null && views.size() > 0) {
            return views.remove(views.size() - 1);
        }
        return null;
    }

    private boolean needAddView() {
        if (mAdapter == null || mAdapter.getCount() <= 0) {
            return false;
        }
        if (getChildCount() <= 1) {
            return true;
        }

        return (childrenHeight() - childHeight(getChildAt(0))) < getMeasuredHeight() - getPaddingTop() - getPaddingBottom();
    }

    private int childrenHeight() {
        int childrenHeight = 0;
        for (int i = 0; i < getChildCount(); i++) {
            childrenHeight += childHeight(getChildAt(i));
        }
        return childrenHeight;
    }

    private int childHeight(View child) {
        final LayoutParams lp = (LayoutParams) child.getLayoutParams();

        final int width = child.getMeasuredWidth();
        final int height = child.getMeasuredHeight();

        return lp.topMargin + height + lp.bottomMargin;
    }

    private void addNextView() {
        int position = (++mIndex % mAdapter.getCount());
        View view = mAdapter.getView(position, getRecycledView(mAdapter.getItemViewType(position)), this);
        setItemViewLayoutParams(view, position);
        measureChildWithMargins(view, mWidthMeasureSpec, 0, mHeightMeasureSpec, 0);

        final LayoutParams layoutParams = (LayoutParams) view.getLayoutParams();
        final int left = getPaddingLeft() + layoutParams.leftMargin;
        final int offset = (int) (childHeight(getChildCount() <= 0 ? view : getChildAt(0)) * mLayoutOffsetPercent);
        final int top = getPaddingTop() + childrenHeight() + layoutParams.topMargin - offset;
        view.layout(left, top, left + view.getMeasuredWidth(), top + view.getMeasuredHeight());
        addViewInLayout(view, -1, view.getLayoutParams());
    }

    private void setItemViewLayoutParams(View child, int position) {
        final ViewGroup.LayoutParams vlp = child.getLayoutParams();
        MarqueeView.LayoutParams lp;
        if (vlp == null) {
            lp = (MarqueeView.LayoutParams) generateDefaultLayoutParams();
        } else if (!checkLayoutParams(vlp)) {
            lp = (MarqueeView.LayoutParams) generateLayoutParams(vlp);
        } else {
            lp = (MarqueeView.LayoutParams) vlp;
        }

        lp.viewType = mAdapter.getItemViewType(position);
        if (lp != vlp) {
            child.setLayoutParams(lp);
        }
    }

    private void relayoutOffset() {
        if (getChildCount() <= 0) {
            return;
        }

        int offsetTop = (int) (childHeight(getChildAt(0)) * mLayoutOffsetPercent);

        int layoutTop = getPaddingTop() - offsetTop;
        for (int i = 0; i < getChildCount(); i++) {
            final View child = getChildAt(i);

            final LayoutParams layoutParams = (LayoutParams) child.getLayoutParams();
            final int left = getPaddingLeft() + layoutParams.leftMargin;
            final int top = layoutTop + layoutParams.topMargin;
            child.layout(left, top, left + child.getMeasuredWidth(), top + child.getMeasuredHeight());

            layoutTop = layoutTop + layoutParams.topMargin + child.getMeasuredHeight() + layoutParams.bottomMargin;
        }
        invalidate();
    }

    public void startPlay() {
        if (isContextFinishing()) {
            return;
        }

        stopPlay();
        mAnimator = ValueAnimator.ofFloat(0f, 1f);
        mAnimator.setDuration(mAnimDuration);
        mAnimator.setStartDelay(mStillDuration);
        mAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                mLayoutOffsetPercent = (float) animation.getAnimatedValue();
                relayoutOffset();
            }
        });
        mAnimator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                mLayoutOffsetPercent = 0f;

                if (getChildCount() > 0) {
                    final View view = getChildAt(0);
                    removeViewInLayout(view);
                    recycleView(view);
                    while (needAddView()) {
                        addNextView();
                    }
                    invalidate();
                }

                startPlay();
            }

            @Override
            public void onAnimationCancel(Animator animation) {
            }

            @Override
            public void onAnimationRepeat(Animator animation) {
            }
        });
        mAnimator.start();
    }

    private void stopPlay() {
        if (mAnimator != null) {
            mAnimator.cancel();
        }
    }

    private boolean isContextFinishing() {
        final Context context = getContext();
        if (context instanceof Activity) {
            return ((Activity) context).isFinishing();
        }
        return false;
    }

    @Override
    public void addView(View child) {
        throw new UnsupportedOperationException("addView(View) is not supported in MarqueeView");
    }

    @Override
    public void addView(View child, int index) {
        throw new UnsupportedOperationException("addView(View, int) is not supported in MarqueeView");
    }

    @Override
    public void addView(View child, ViewGroup.LayoutParams params) {
        throw new UnsupportedOperationException("addView(View, LayoutParams) " + "is not supported in MarqueeView");
    }

    @Override
    public void addView(View child, int index, ViewGroup.LayoutParams params) {
        throw new UnsupportedOperationException("addView(View, int, LayoutParams) " + "is not supported in MarqueeView");
    }

    @Override
    public void removeView(View child) {
        throw new UnsupportedOperationException("removeView(View) is not supported in MarqueeView");
    }

    @Override
    public void removeViewAt(int index) {
        throw new UnsupportedOperationException("removeViewAt(int) is not supported in MarqueeView");
    }

    @Override
    public void removeAllViews() {
        throw new UnsupportedOperationException("removeAllViews() is not supported in MarqueeView");
    }

    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
        return p instanceof MarqueeView.LayoutParams;
    }

    @Override
    protected ViewGroup.LayoutParams generateDefaultLayoutParams() {
        return new MarqueeView.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
    }

    @Override
    protected ViewGroup.LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
        return new MarqueeView.LayoutParams(p);
    }

    @Override
    public ViewGroup.LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new MarqueeView.LayoutParams(getContext(), attrs);
    }

    public static class LayoutParams extends MarginLayoutParams {
        int viewType;

        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);
        }

        public LayoutParams(int width, int height) {
            super(width, height);
        }

        public LayoutParams(int w, int h, int viewType) {
            super(w, h);
            this.viewType = viewType;
        }

        public LayoutParams(MarginLayoutParams source) {
            super(source);
        }

        public LayoutParams(ViewGroup.LayoutParams source) {
            super(source);
        }
    }

    private class AdapterDataSetObserver extends DataSetObserver {
        @Override
        public void onChanged() {
            super.onChanged();
            requestLayout();
        }

        @Override
        public void onInvalidated() {
            super.onInvalidated();
            requestLayout();
        }
    }
}
