package com.example.demo;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

public class PullToRefreshLayout extends LinearLayout {

    private Context context;

    public PullToRefreshLayout(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        setOrientation(VERTICAL);
    }

    //header

    private FrameLayout headerContainer;
    private View header;

    public View setHeader(int headerId) {

        //header container

        headerContainer = new FrameLayout(context);
        LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 500);
        headerContainer.setLayoutParams(llp);
        addView(headerContainer, 0);

        //header

        header = ((Activity)context).getLayoutInflater().inflate(headerId, headerContainer, false);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, header.getLayoutParams().height);
        lp.gravity = Gravity.BOTTOM;
        header.setLayoutParams(lp);
        headerContainer.addView(header);

        //header container set the same bg like header

        headerContainer.setBackground(header.getBackground());

        return header;
    }

    //state

    private static final int BASE = 1;

    private static final int NONE = BASE;

    private static final int PULL_LESS_THAN_CHANGE_HEIGHT = BASE << 1;
    private static final int PULL_MORE_THAN_CHANGE_HEIGHT = BASE << 2;
    private static final int RECOVER_LESS_THAN_CHANGE_HEIGHT = BASE << 3;
    private static final int RECOVER_MORE_THAN_CHANGE_HEIGHT = BASE << 4;
    private static final int RECOVER_REFRESHING = BASE << 5;

    private int state = NONE;

    //listener

    private Listener listener;

    public interface Listener {
        void onChange();//下拉刷新->释放刷新
        void onRecover();//释放刷新->下拉刷新

        void onRefresh();//释放刷新->刷新中
        void onFinish();//刷新中->下拉刷新
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public void refreshFinished() {//网络请求完后 进行调用

        //从刷新中状态恢复到初始状态

        if (state == RECOVER_REFRESHING) {
            int start = -containerHeight + headerHeight;
            int end = - containerHeight;

            ValueAnimator animator = ValueAnimator.ofInt(start, end);
            animator.setDuration(500).start();
            animator.addUpdateListener(animation -> {
                int i = (int) animation.getAnimatedValue();

                LayoutParams params = (LayoutParams) headerContainer.getLayoutParams();
                params.topMargin = i;
                headerContainer.setLayoutParams(params);

                //回调finish

                if (i == end) {
                    state = NONE;
                    listener.onFinish();
                }
            });
        }
    }

    //params

    private boolean isInitialized = false;

    private int offset;

    private int containerHeight;
    private int headerHeight;

    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        super.onWindowFocusChanged(hasWindowFocus);

        if (!isInitialized) {
            isInitialized = true;

            //header

            containerHeight = headerContainer.getHeight();
            headerHeight = header.getHeight();

            LinearLayout.LayoutParams params = (LayoutParams) headerContainer.getLayoutParams();
            params.topMargin = -containerHeight;
            headerContainer.setLayoutParams(params);

            //拿到rv的下滑距离

            RecyclerView rv = (RecyclerView) getChildAt(1);
            rv.setOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                    super.onScrolled(recyclerView, dx, dy);
                    offset = rv.computeVerticalScrollOffset();
                }
            });
        }
    }

    //intercept

    private int downY;

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {//只需要做入口状态判断即可
        int y = (int) ev.getY();
        boolean intercept = false;

        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:

                //手指落点

                downY = y;
                intercept = false;
                break;
            case MotionEvent.ACTION_MOVE:

                //如果rv在顶端，且正处于下拉状态，我们进入下拉刷新的逻辑

                if (state == NONE) {
                    if (offset == 0 && y - downY > 0) {
                        state = PULL_LESS_THAN_CHANGE_HEIGHT;
                    }
                }

                //只要不是NONE状态，一律拦截事件

                intercept = state != NONE;
                break;
        }

        return intercept;
    }

    //touch

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int y = (int) event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                downY = y;
                break;
            case MotionEvent.ACTION_MOVE:

                //到达header顶部时的切换

                int pullOffset = y - downY;
                int downOffset = (int) (pullOffset * 0.4);

                if (state == PULL_LESS_THAN_CHANGE_HEIGHT) {
                    if (downOffset > headerHeight) {
                        state = PULL_MORE_THAN_CHANGE_HEIGHT;
                        listener.onChange();
                    }
                }

                if (state == PULL_MORE_THAN_CHANGE_HEIGHT) {
                    if (downOffset <= headerHeight) {
                        state = PULL_LESS_THAN_CHANGE_HEIGHT;
                        listener.onRecover();
                    }
                }

                //执行下拉

                if (state == PULL_LESS_THAN_CHANGE_HEIGHT || state == PULL_MORE_THAN_CHANGE_HEIGHT) {
                    LinearLayout.LayoutParams params = (LayoutParams) headerContainer.getLayoutParams();
                    params.topMargin = - containerHeight + downOffset;
                    headerContainer.setLayoutParams(params);
                }
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:

                //下拉距离

                int pullOffset2 = y - downY;
                int downOffset2 = (int) (pullOffset2 * 0.4);

                //如果没有超过header 正常恢复

                if (state == PULL_LESS_THAN_CHANGE_HEIGHT) {

                    state = RECOVER_LESS_THAN_CHANGE_HEIGHT;

                    int start = -containerHeight+ downOffset2;
                    int end = -containerHeight;

                    ValueAnimator animator = ValueAnimator.ofInt(start, end);
                    animator.setDuration(500).start();
                    animator.addUpdateListener(animation -> {
                        int i = (int) animation.getAnimatedValue();

                        LayoutParams params = (LayoutParams) headerContainer.getLayoutParams();
                        params.topMargin = i;
                        headerContainer.setLayoutParams(params);

                        //state back to NONE

                        if (i == end) {
                            state = NONE;
                        }
                    });
                }

                //如果超过了header 进入刷新状态 刷新完成后 再执行恢复

                if (state == PULL_MORE_THAN_CHANGE_HEIGHT) {

                    state = RECOVER_MORE_THAN_CHANGE_HEIGHT;

                    int start = -containerHeight + downOffset2;
                    int end = -containerHeight + headerHeight;

                    ValueAnimator animator = ValueAnimator.ofInt(start, end);
                    animator.setDuration(500).start();
                    animator.addUpdateListener(animation -> {
                        int i = (int) animation.getAnimatedValue();

                        LayoutParams params = (LayoutParams) headerContainer.getLayoutParams();
                        params.topMargin = i;
                        headerContainer.setLayoutParams(params);

                        //fresh

                        if (i == end) {
                            state = RECOVER_REFRESHING;
                            listener.onRefresh();
                        }
                    });
                }
                break;
        }

        return true;
    }
}
