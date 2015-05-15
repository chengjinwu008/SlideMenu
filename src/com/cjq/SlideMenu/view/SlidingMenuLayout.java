package com.cjq.SlideMenu.view;

import android.content.Context;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.Scroller;

/**
 * Created by android on 2015/5/15.
 */
public class SlidingMenuLayout extends RelativeLayout {

    private int menuWidth;
    private ViewGroup menuLayout;
    private ViewGroup contentLayout;
    private float mDownX;
    private float mLastX;
    private GestureDetector mGestureDetector;//手势判断器
    private VelocityTracker mVelocityTracker;//速度计算器
    private float mUpX;
    private static final float minVelocity = 200;//最小速度
    private Scroller mScroller;//滚动模拟器
    private float mLeftX = 0;
    private boolean isMenuShowing;
    private SlidingMenuListener listener;
    private ViewGroup menuLayout2;//右侧菜单
    private int menuWidth2;//右侧菜单宽度
    private int contentWidth;//中间内容的宽度
    private boolean isMenuShowing2;//右侧菜单显示指示
    private  int minLength;

    public void setListener(SlidingMenuListener listener) {
        this.listener = listener;
    }

    interface SlidingMenuListener {
        void onMenuFinishedScroll();

        void onContentFinishedScroll();

        boolean canScroll();
    }

    /**
     * 计算Menu体积系数
     */
    private void computeScaleMenu() {
        float scaleMenu = (float) (0.9f + 0.1 * Math.abs(mLeftX / menuWidth));
        scaleMenu = scaleMenu > 1 ? 1 : scaleMenu;
        menuLayout.setPivotX(menuWidth);
        menuLayout.setAlpha(scaleMenu);
//        menuLayout.setPivotY(0.5f);
        menuLayout.setScaleX(scaleMenu);
        menuLayout.setScaleY(scaleMenu);
    }

    private void computeScaleContentLeft() {
        float scaleContent = (float) (0.9f + 0.1 * (1 - Math.abs(mLeftX / menuWidth)));
        scaleContent = scaleContent > 1 ? 1 : scaleContent;
        contentLayout.setPivotX(0);
        contentLayout.setAlpha(scaleContent);
//        contentLayout.setPivotY(0.5f);
        contentLayout.setScaleX(scaleContent);
        contentLayout.setScaleY(scaleContent);
    }

    private void computeScaleContentRight() {
        float scaleContent = (float) (0.9f + 0.1 * (1 - Math.abs(mLeftX / menuWidth)));
        scaleContent = scaleContent > 1 ? 1 : scaleContent;
        contentLayout.setPivotX(contentWidth);
        contentLayout.setAlpha(scaleContent);
//        contentLayout.setPivotY(0.5f);
        contentLayout.setScaleX(scaleContent);
        contentLayout.setScaleY(scaleContent);
    }

    private void computeScaleMenu2() {
        float scaleMenu = (float) (0.9f + 0.1 * Math.abs(mLeftX / menuWidth));
        scaleMenu = scaleMenu > 1 ? 1 : scaleMenu;
        menuLayout2.setPivotX(0);
        menuLayout2.setAlpha(scaleMenu);
//        menuLayout.setPivotY(0.5f);
        menuLayout2.setScaleX(scaleMenu);
        menuLayout2.setScaleY(scaleMenu);
    }

    public SlidingMenuLayout(Context context) {
        this(context, null);
    }

    public SlidingMenuLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SlidingMenuLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    private void init(Context context) {
        minLength= (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,100,context.getResources().getDisplayMetrics());

        mGestureDetector = new GestureDetector(context, new GestureDetector.OnGestureListener() {
            @Override
            public boolean onDown(MotionEvent e) {
                return false;
            }

            @Override
            public void onShowPress(MotionEvent e) {

            }

            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                return false;
            }

            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                return Math.abs(distanceX) > Math.abs(distanceY)&&Math.abs(distanceX)>minLength;
            }

            @Override
            public void onLongPress(MotionEvent e) {

            }

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                return false;
            }
        });
        mScroller = new Scroller(context);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        if (menuLayout == null) {
//            隐藏第一菜单
            menuLayout = (ViewGroup) getChildAt(0);
            RelativeLayout.LayoutParams params = (LayoutParams) menuLayout.getLayoutParams();
            menuWidth = params.width;
            params.leftMargin = -menuWidth;
            menuLayout.setLayoutParams(params);
        }
        if (menuLayout2 == null) {
            //隐藏第二菜单
            menuLayout2 = (ViewGroup) getChildAt(2);
            RelativeLayout.LayoutParams params = (LayoutParams) menuLayout2.getLayoutParams();
            menuWidth2 = params.width;
            params.rightMargin = -menuWidth2;
            params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
            menuLayout2.setLayoutParams(params);
        }
        if (contentLayout == null) {
            contentLayout = (ViewGroup) getChildAt(1);
            contentWidth = contentLayout.getMeasuredWidth();
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        //记录下初始值（之后的事件都不会触发down事件，所以必须在这里捕获）
        float x = ev.getRawX();
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            mDownX = x;
            mLastX = x;
        }
        //判断手势，这个是谷歌的集成的类，很好用，判断手势，他会自动消耗前几个event，返回bool，不会消耗后面的事件对象
        //加入监听
        boolean flag = true;
        if (listener != null)
            flag = listener.canScroll();
        return flag && mGestureDetector.onTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        createVelocityTracker(event);
        float x = event.getRawX();
        switch (event.getAction()) {
            case MotionEvent.ACTION_MOVE:
                float deltaX = (x - mLastX) / 3;
                mLastX = x;
                mLeftX -= deltaX;
                this.scrollBy((int) -deltaX, 0);
                break;
            case MotionEvent.ACTION_UP:
                //在这里才是真的执行事件
                //判断下要做什么，要显示内容？还是要显示菜单
                mUpX = x;
                if (getVelocity() > minVelocity || getLength() > 0) {
                    //速度满足要求就准备切换,长度满足要求也会进行切换
                    if (wantToShowMenu()) {
                        showMenu();
                    } else if (wantToShowMenu2()) {
                        showMenu2();
                    } else {
                        showContent();
                    }
                } else {
                    //速度不满足要求就准备回弹
                    if (isMenuShowing) {
                        //菜单还在显示，就回显菜单
                        showMenu();
                    } else if(isMenuShowing2)
                    {
                        showMenu2();
                    }
                    else {
                        //菜单没显示就回显content
                        showContent();
                    }
                }
                break;
        }
        return true;
    }

    private void showMenu2() {
        mScroller.startScroll((int) mLeftX, 0, (int) (menuWidth2 - mLeftX), 0);
        this.invalidate();
        if (!isMenuShowing2)
            isMenuShowing2 = true;
    }

    private int getLength() {
        int res;
        if(isMenuShowing){
            res = (int) (Math.abs(mUpX - mDownX)-(float)menuWidth*0.7);
        }else if(isMenuShowing2){
            res = (int) (Math.abs(mUpX - mDownX)-(float)menuWidth2*0.7);
        }else{
            if(mUpX>mDownX){
            //手指向右，菜单划出左菜单
                res = (int) (Math.abs(mUpX - mDownX)-(float)menuWidth*0.7);
            }else{
                res = (int) (Math.abs(mUpX - mDownX)-(float)menuWidth2*0.7);
            }
        }

        return res;
    }

    @Override
    public void computeScroll() {
        if (mScroller.computeScrollOffset()) {
            this.scrollTo(mScroller.getCurrX(), mScroller.getCurrY());
            mLeftX = mScroller.getCurrX();
            this.invalidate();
        } else {
            if (listener != null)
                if (isMenuShowing) {
                    listener.onMenuFinishedScroll();
                } else {
                    listener.onContentFinishedScroll();
                }
        }
        if(mLeftX<0){
            computeScaleMenu();
            computeScaleContentLeft();
        }else{
            computeScaleContentRight();
            computeScaleMenu2();
        }
        super.computeScroll();
    }

    private void showContent() {
        mScroller.startScroll((int) mLeftX, 0, (int) (-mLeftX), 0);
        this.invalidate();
        if (isMenuShowing)
            isMenuShowing = false;
        if (isMenuShowing2)
            isMenuShowing2 = false;
    }

    private void showMenu() {
        mScroller.startScroll((int) mLeftX, 0, (int) (-menuWidth - mLeftX), 0);
        this.invalidate();
        if (!isMenuShowing)
            isMenuShowing = true;
    }

    private void createVelocityTracker(MotionEvent event) {
        if (mVelocityTracker == null)
            mVelocityTracker = VelocityTracker.obtain();
        mVelocityTracker.addMovement(event);
    }

    private boolean wantToShowMenu() {
        return mUpX - mDownX > 0 && !isMenuShowing2 ;
    }

    private boolean wantToShowMenu2() {
        return mUpX - mDownX < 0 && !isMenuShowing;
    }

    private float getVelocity() {
        mVelocityTracker.computeCurrentVelocity(1000);
        return Math.abs(mVelocityTracker.getXVelocity());
    }
}
