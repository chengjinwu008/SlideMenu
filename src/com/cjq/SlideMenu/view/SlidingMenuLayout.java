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
 * Created by 陈晋强 on 2015/5/15.
 */

/**
 * 侧滑菜单栏，是一个ViewGroup布局，最多容纳3个子ViewGroup标签，最少容纳一个，否则报错！
 * 可以给子ViewGroup添加left,right.content进行定位，分别对应左侧滑菜单，右侧滑菜单，和内容主体，不加则默认按照左中右的顺序生成菜单和主体，只有一个ViewGroup子View时，只生成内容，无菜单
 * 如果是菜单，宽度请不要使用matchparent，要窗口宽度的菜单，麻烦你用ViewPager谢谢!content请尽量使用matchparent，不然有间隙比较难看
 * 可以设置listener控制滑动事件的拦截，和监听菜单打开和关闭事件
 * 可以设置 setIsAnimationEnabled 来控制是否播放缩放动画
 * 本布局默认拦截超过100dp的所有左右滑动事件，可以显示的通过listener返回false阻止所有的拦截，或者是通过setmGestureDetector方法制定拦截的手势
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
    private int minLength;
    private boolean isAnimationEnabled = false;
    private boolean handleEvent;//决定事件是否向下传递！true的话会导致子view不能接收到touch事件
    private float mDownY;
    private boolean isCertain=false;//拦截系数是否已经确定
    private int mLeftXMax;
    private int mLeftXMin;

    public void setIsAnimationEnabled(boolean isAnimationEnabled) {
        this.isAnimationEnabled = isAnimationEnabled;
    }

    public void setListener(SlidingMenuListener listener) {
        this.listener = listener;
    }

    public interface SlidingMenuListener {
        void onLeftMenuFinishedScroll();

        void onRightMenuFinishedScroll();

        void onContentFinishedScroll();

//        /**
//         * 是否向上传递触摸事件，若拦截了向下传递事件则会自动拦截向上传递
//         *
//         * @return true则不传递，由自身处理该事件
//         */
//        boolean canScroll();

        /**
         * 是否拦截向下传递
         *
         * @param distanceX x移动向量
         * @param distanceY y移动向量
         * @return true 拦截，false 不拦截
         */
        boolean canInterceptEvent(float distanceX, float distanceY);
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
        float scaleContent = (float) (0.9f + 0.1 * (1 - Math.abs(mLeftX / menuWidth2)));
        scaleContent = scaleContent > 1 ? 1 : scaleContent;
        contentLayout.setPivotX(contentWidth);
        contentLayout.setAlpha(scaleContent);
//        contentLayout.setPivotY(0.5f);
        contentLayout.setScaleX(scaleContent);
        contentLayout.setScaleY(scaleContent);
    }

    private void computeScaleMenu2() {
        float scaleMenu = (float) (0.9f + 0.1 * Math.abs(mLeftX / menuWidth2));
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
        minLength = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 100, context.getResources().getDisplayMetrics());
        mScroller = new Scroller(context);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        if (getChildCount() == 3) {
            String tag1 = (String) getChildAt(0).getTag();
            String tag2 = (String) getChildAt(1).getTag();
            String tag3 = (String) getChildAt(2).getTag();

            if (tag1 == null || tag2 == null || tag3 == null) {
                initLeftMenu(0);
                initRightMenu(2);
                initContent(1);
            } else {
                if ("content".equals(tag1)) {
                    if ("left".equals(tag2) && "right".equals(tag3)) {
                        initLeftMenu(1);
                        initRightMenu(2);
                        initContent(0);
                    } else if ("left".equals(tag3) && "right".equals(tag2)) {
                        initLeftMenu(2);
                        initRightMenu(1);
                        initContent(0);
                    } else {
                        initLeftMenu(0);
                        initRightMenu(2);
                        initContent(1);
                    }
                } else if ("content".equals(tag2)) {
                    if ("left".equals(tag1) && "right".equals(tag3)) {
                        initLeftMenu(0);
                        initRightMenu(2);
                        initContent(1);
                    } else if ("left".equals(tag3) && "right".equals(tag2)) {
                        initLeftMenu(2);
                        initRightMenu(0);
                        initContent(1);
                    } else {
                        initLeftMenu(0);
                        initRightMenu(2);
                        initContent(1);
                    }
                } else if ("content".equals(tag3)) {
                    if ("left".equals(tag1) && "right".equals(tag2)) {
                        initLeftMenu(0);
                        initRightMenu(1);
                        initContent(2);
                    } else if ("left".equals(tag2) && "right".equals(tag1)) {
                        initLeftMenu(1);
                        initRightMenu(0);
                        initContent(2);
                    } else {
                        initLeftMenu(0);
                        initRightMenu(2);
                        initContent(2);
                    }
                } else {
                    initLeftMenu(0);
                    initRightMenu(2);
                    initContent(1);
                }
            }
            //很明显有左边界和又边界都需要定
            mLeftXMax =menuWidth2 ;//右边界
            mLeftXMin = -menuWidth;//左边界
        } else if (getChildCount() == 2) {
            String tag1 = (String) getChildAt(0).getTag();
            String tag2 = (String) getChildAt(1).getTag();
            if (tag1 == null || tag2 == null) {
                initLeftMenu(0);
                initContent(1);
            } else {
                if ("content".equals(tag1)) {
                    switch (tag2) {
                        case "right":
                            initContent(0);
                            initRightMenu(1);
                            break;
                        case "left":
                            initLeftMenu(1);
                            initContent(0);
                            break;
                        default:
                            initLeftMenu(0);
                            initContent(1);
                            break;
                    }
                } else if ("content".equals(tag2)) {
                    switch (tag1) {
                        case "right":
                            initContent(1);
                            initRightMenu(0);
                            break;
                        case "left":
                            initLeftMenu(0);
                            initContent(1);
                            break;
                        default:
                            initLeftMenu(0);
                            initContent(1);
                            break;
                    }
                } else {
                    initLeftMenu(0);
                    initContent(1);
                }
            }
            mLeftXMax =menuWidth2 ;//右边界
            mLeftXMin = -menuWidth;//左边界
        } else if (getChildCount() == 1) {
            initContent(0);
            mLeftXMax =menuWidth2 ;//右边界
            mLeftXMin = -menuWidth;//左边界
        } else {
            throw new RuntimeException("Wrong Children Count!");
        }

    }

    private void initRightMenu(int i) {
        if (menuLayout2 == null) {
            //隐藏第二菜单
            menuLayout2 = (ViewGroup) getChildAt(i);
            if (menuLayout2 != null) {
                LayoutParams params = (LayoutParams) menuLayout2.getLayoutParams();
                menuWidth2 = menuLayout2.getMeasuredWidth();
                params.rightMargin = -menuWidth2;
                params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
                menuLayout2.setLayoutParams(params);
            }
        }
    }

    private void initLeftMenu(int i) {
        if (menuLayout == null) {
//            隐藏第一菜单
            menuLayout = (ViewGroup) getChildAt(i);
            if (menuLayout != null) {
                LayoutParams params = (LayoutParams) menuLayout.getLayoutParams();
                menuWidth = menuLayout.getMeasuredWidth();
                params.leftMargin = -menuWidth;
                menuLayout.setLayoutParams(params);
            }
        }
    }

    private void initContent(int i) {
        if (contentLayout == null) {
            contentLayout = (ViewGroup) getChildAt(i);
            contentWidth = contentLayout.getMeasuredWidth();
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return handleEvent;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        float x = ev.getRawX();
        float y = ev.getY();

        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            mDownX = x;
            mLastX = x;
            mDownY = y;
            handleEvent=false;
            isCertain=false;
        } else if (ev.getAction() == MotionEvent.ACTION_MOVE) {
            if(!isCertain){
                float distanceX = x - mLastX;
                float distanceY = y - mDownY;
                //确定拦截系数
                handleEvent = (listener != null && listener.canInterceptEvent(distanceX, distanceY)) || isMenuOpened();
                isCertain =true;
            }
        }
        return super.dispatchTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        createVelocityTracker(event);
        float x = event.getRawX();
        switch (event.getAction()) {
            case MotionEvent.ACTION_MOVE:
                if(isCertain){
                    float deltaX = (x - mLastX);
                    mLastX = x;
                    mLeftX -= deltaX;
                    if(mLeftX<mLeftXMin){
                        mLeftX =mLeftXMin;
                    }else if (mLeftX>mLeftXMax){
                        mLeftX = mLeftXMax;
                    }
                    this.scrollTo((int) mLeftX,0);
                }
                break;
            case MotionEvent.ACTION_UP:
                //在这里才是真的执行事件
                //判断下要做什么，要显示内容？还是要显示菜单
                mUpX = x;
                if (getVelocity() > minVelocity || getLength() > 0) {
                    //速度满足要求就准备切换,长度满足要求也会进行切换
                    if (wantToShowMenu() && menuLayout != null) {
                        showMenu();
                    } else if (wantToShowMenu2() && menuLayout2 != null) {
                        showMenu2();
                    } else {
                        showContent();
                    }
                } else {
                    //速度不满足要求就准备回弹
                    if (isMenuShowing) {
                        //菜单还在显示，就回显菜单
                        showMenu();
                    } else if (isMenuShowing2) {
                        showMenu2();
                    } else {
                        //菜单没显示就回显content
                        showContent();
                    }
                }
                break;
        }
        //加入监听
//        boolean flag = true;
//        if (listener != null)
//            flag = listener.canScroll();
//        Log.i("ev", String.valueOf(event.getAction())+"+"+this.getId());
        return true;
    }

    private void showMenu2() {
        mScroller.startScroll((int) mLeftX, 0, (int) (menuWidth2 - mLeftX), 0);
        this.invalidate();
        if (!isMenuShowing2)
            isMenuShowing2 = true;
        if (isMenuShowing)
            isMenuShowing = false;
    }

    private int getLength() {
        int res;
        if (isMenuShowing) {
            res = (int) (Math.abs(mUpX - mDownX) - (float) menuWidth * 0.7);
        } else if (isMenuShowing2) {
            res = (int) (Math.abs(mUpX - mDownX) - (float) menuWidth2 * 0.7);
        } else {
            if (mUpX > mDownX) {
                //手指向右，菜单划出左菜单
                res = (int) (Math.abs(mUpX - mDownX) - (float) menuWidth * 0.7);
            } else {
                res = (int) (Math.abs(mUpX - mDownX) - (float) menuWidth2 * 0.7);
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
                    listener.onLeftMenuFinishedScroll();
                } else if (isMenuShowing2) {
                    listener.onRightMenuFinishedScroll();
                } else {
                    listener.onContentFinishedScroll();
                }
        }
        if (isAnimationEnabled)
            if (mLeftX < 0) {
                if (menuLayout != null) {
                    computeScaleMenu();
                    computeScaleContentLeft();
                }
            } else {
                if (menuLayout2 != null) {
                    computeScaleContentRight();
                    computeScaleMenu2();
                }
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
        if (isMenuShowing2)
            isMenuShowing2 = false;
    }

    private void createVelocityTracker(MotionEvent event) {
        if (mVelocityTracker == null)
            mVelocityTracker = VelocityTracker.obtain();
        mVelocityTracker.addMovement(event);
    }

    private boolean wantToShowMenu() {
        return mUpX - mDownX > 0 && mLeftX < -menuWidth / 2;
    }

    private boolean wantToShowMenu2() {
        return mUpX - mDownX < 0 && mLeftX > menuWidth2 / 2;
    }

    private float getVelocity() {
        mVelocityTracker.computeCurrentVelocity(1000);
        return Math.abs(mVelocityTracker.getXVelocity());
    }

    public boolean isMenuOpened() {
        return isMenuShowing || isMenuShowing2;
    }
}
