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
    private GestureDetector mGestureDetector;//�����ж���
    private VelocityTracker mVelocityTracker;//�ٶȼ�����
    private float mUpX;
    private static final float minVelocity = 200;//��С�ٶ�
    private Scroller mScroller;//����ģ����
    private float mLeftX = 0;
    private boolean isMenuShowing;
    private SlidingMenuListener listener;
    private ViewGroup menuLayout2;//�Ҳ�˵�
    private int menuWidth2;//�Ҳ�˵����
    private int contentWidth;//�м����ݵĿ��
    private boolean isMenuShowing2;//�Ҳ�˵���ʾָʾ
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
     * ����Menu���ϵ��
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
//            ���ص�һ�˵�
            menuLayout = (ViewGroup) getChildAt(0);
            RelativeLayout.LayoutParams params = (LayoutParams) menuLayout.getLayoutParams();
            menuWidth = params.width;
            params.leftMargin = -menuWidth;
            menuLayout.setLayoutParams(params);
        }
        if (menuLayout2 == null) {
            //���صڶ��˵�
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
        //��¼�³�ʼֵ��֮����¼������ᴥ��down�¼������Ա��������ﲶ��
        float x = ev.getRawX();
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            mDownX = x;
            mLastX = x;
        }
        //�ж����ƣ�����ǹȸ�ļ��ɵ��࣬�ܺ��ã��ж����ƣ������Զ�����ǰ����event������bool���������ĺ�����¼�����
        //�������
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
                //������������ִ���¼�
                //�ж���Ҫ��ʲô��Ҫ��ʾ���ݣ�����Ҫ��ʾ�˵�
                mUpX = x;
                if (getVelocity() > minVelocity || getLength() > 0) {
                    //�ٶ�����Ҫ���׼���л�,��������Ҫ��Ҳ������л�
                    if (wantToShowMenu()) {
                        showMenu();
                    } else if (wantToShowMenu2()) {
                        showMenu2();
                    } else {
                        showContent();
                    }
                } else {
                    //�ٶȲ�����Ҫ���׼���ص�
                    if (isMenuShowing) {
                        //�˵�������ʾ���ͻ��Բ˵�
                        showMenu();
                    } else if(isMenuShowing2)
                    {
                        showMenu2();
                    }
                    else {
                        //�˵�û��ʾ�ͻ���content
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
            //��ָ���ң��˵�������˵�
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
