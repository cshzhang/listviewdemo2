package cn.hzh.listviewdemo2.view;

import android.content.Context;
import android.os.AsyncTask;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.ListView;

import cn.hzh.listviewdemo2.R;

/**
 * Created by hzh on 2015/11/2.
 */
public class QQListView extends ListView
{
    //判断用户是否想要显示右侧的view
    private boolean mWantToShowRightView;
    //判断用户是否正常scroll listview
    private boolean mWantToScroll;

    private int mTouchSlop;
    private int mDownX = -1;
    private int mDownY = -1;
    private int mLastX;

    //记录用户点击的子view
    private View mPointerView;
    //内容view
    private View mContentView;
    //右侧的view
    private View mRightView;
    //右侧view的宽度
    private int mRightViewWidth;

    //利用VelocityTracker得到用户滑动的速度
    private VelocityTracker mVelocityTracker;
    //最小速度
    private final static int MIN_VELOCITY = 600;    //dp
    private int mMinVelocity;

    //用户UP的时候，scroll的速度
    private final static int SCROLL_SPEED = 10;

    //ContentView的LayoutParams，用于改变leftMargin
    private MarginLayoutParams mContentViewLp;
    //判断右侧view是否显示状态
    private boolean mRightViewShowing;

    //判断当前是否真该scroll，用于屏蔽用户touch操作
    private boolean mScrolling;

    //点击rightview的回调接口
    private OnItemRightViewClickListener mListener;

    public QQListView(Context context, AttributeSet attrs)
    {
        super(context, attrs);

        mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        float density = getResources().getDisplayMetrics().density;
        mMinVelocity = (int) (MIN_VELOCITY * density);
    }

    /**
     * 主要负责检测用户意图. 是想操作rightview，还是scroll listview.
     * @param ev
     * @return
     */
    @Override
    public boolean dispatchTouchEvent(MotionEvent ev)
    {
        //如果在scrolling，屏蔽用户操作
        if(mScrolling)
            return false;

        createVelocityTracker(ev);
        if (mDownX == -1)
        {
            mDownX = (int) ev.getX();
            mDownY = (int) ev.getY();
        }
        switch (ev.getAction())
        {
            case MotionEvent.ACTION_DOWN:
                mDownX = (int) ev.getX();
                mDownY = (int) ev.getY();
                mLastX = (int) ev.getX();

                //用户down的时候的子view
                View view = getPointerView(mDownX, mDownY);
                if (view == null)
                    return false;

                //如果右侧的view是显示的，则直接关闭，然后return false；屏蔽后续MotionEvent
                if (mRightViewShowing &&
                        view.findViewById(R.id.id_content_to_delete) != mContentView)
                {
                    closeItem(10);
                    return false;
                }
                break;
            case MotionEvent.ACTION_MOVE:
                final int x = (int) ev.getX();
                final int y = (int) ev.getY();
                int deltaX = x - mDownX;
                int deltaY = y - mDownY;

                //如果已经明确用户意图，直接返回。进入onTouchEvent()执行逻辑处理
                if (mWantToShowRightView || mWantToScroll)
                {
                    return super.dispatchTouchEvent(ev);
                }

                //1.判断用户这次滑动是什么类型，scroll listview or delete item
                if (Math.abs(deltaY) > mTouchSlop || Math.abs(deltaX) > mTouchSlop)
                {
                    if (Math.abs(deltaY) >= Math.abs(deltaX))
                    {
                        //2.如果是scroll listview，按系统默认处理
                        mWantToScroll = true;
                    } else if (deltaX < 0 || mRightViewShowing)   //从右向左滑动
                    {
                        mWantToShowRightView = true;
                    }

                    //3.如果是delete item，则交给ViewDragHelper
                    if (mWantToShowRightView)
                    {
                        mPointerView = getPointerView(x, y);
                        if (mPointerView == null)
                            return false;

                        mContentView = mPointerView.
                                findViewById(R.id.id_content_to_delete);
                        mRightView = mPointerView.
                                findViewById(R.id.id_delete);
                        mRightViewWidth = mRightView.getWidth();

                        //设置监听器回调
                        mRightView.setOnClickListener(new OnClickListener()
                        {
                            @Override
                            public void onClick(View v)
                            {
                                if (mListener != null)
                                {
                                    mListener.onItemRightViewClick(pointToPosition(x, y),
                                            mPointerView);
                                }
                                closeItem(20);
                            }
                        });
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                mWantToScroll = false;
                break;
        }

        return super.dispatchTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev)
    {
        if (mWantToShowRightView)
        {
            switch (ev.getAction())
            {
                case MotionEvent.ACTION_MOVE:
                    int x = (int) ev.getX();
                    int deltaX = x - mLastX;
                    mLastX = x;

                    if (mPointerView != null)
                    {
                        MarginLayoutParams lp = (MarginLayoutParams) mContentView.getLayoutParams();
                        lp.leftMargin += deltaX;
                        //边界检查
                        lp.leftMargin = lp.leftMargin > 0 ? 0 : lp.leftMargin;
                        lp.leftMargin = lp.leftMargin < -mRightViewWidth ? -mRightViewWidth : lp.leftMargin;

                        mContentView.setLayoutParams(lp);
                    }
                    break;
                case MotionEvent.ACTION_UP:
                    mContentViewLp = (MarginLayoutParams) mContentView.getLayoutParams();
                    int leftMargin = mContentViewLp.leftMargin;
                    //offset = 1 --> 隐藏
                    //offset = 0 --> 显示
                    float offset = (mRightViewWidth + leftMargin) * 1.0f / mRightViewWidth;
                    mVelocityTracker.computeCurrentVelocity(1000);

                    int speed;

                    if (mRightViewShowing)
                    {
                        speed = mVelocityTracker.getXVelocity() > mMinVelocity ||
                                offset > 0.5f ? SCROLL_SPEED : -SCROLL_SPEED;
                    } else
                    {
                        speed = mVelocityTracker.getXVelocity() < -mMinVelocity ||
                                offset < 0.5f ? -SCROLL_SPEED : SCROLL_SPEED;
                    }

                    Log.d("TAG", "ACTION_UP-->speed: " + speed);
                    scrollItem(speed);

                    mWantToShowRightView = false;
                    mWantToScroll = false;
                    recycleVelocityTracker();
                    break;
            }
            return true;
        } else if (mRightViewShowing) //如果右侧view的显示的，屏蔽用户scroll listview操作
        {
            return true;
        } else
        {
            return super.onTouchEvent(ev);
        }
    }

    private void createVelocityTracker(MotionEvent ev)
    {
        if (mVelocityTracker == null)
        {
            mVelocityTracker = VelocityTracker.obtain();
        }
        mVelocityTracker.addMovement(ev);
    }

    private void recycleVelocityTracker()
    {
        if (mVelocityTracker != null)
        {
            mVelocityTracker.recycle();
            mVelocityTracker = null;
        }
    }

    /**
     * 异步任务，用于自动scroll
     */
    public class ScrollTask extends AsyncTask<Integer, Integer, Integer>
    {
        @Override
        protected Integer doInBackground(Integer... params)
        {
            mScrolling = true;
            int speed = params[0];
            int leftMargin = mContentViewLp.leftMargin;

            while (true)
            {
                //如果接近border就break
                if (isCloseToBorder(leftMargin, speed))
                {
                    if (speed > 0)
                    {
                        leftMargin = 0;
                        mRightViewShowing = false;
                    } else
                    {
                        leftMargin = -mRightViewWidth;
                        mRightViewShowing = true;
                    }
                    break;
                }

                leftMargin += speed;
                sleep(10);
                publishProgress(leftMargin);
            }

            Log.d("TAG", "doInBackground exit");

            return leftMargin;
        }

        @Override
        protected void onProgressUpdate(Integer... leftMargin)
        {
            mContentViewLp.leftMargin = leftMargin[0];
            mContentView.setLayoutParams(mContentViewLp);
        }

        @Override
        protected void onPostExecute(Integer leftMargin)
        {
            mContentViewLp.leftMargin = leftMargin;
            mContentView.setLayoutParams(mContentViewLp);

            mScrolling = false;
        }
    }

    private void sleep(long millis)
    {
        try
        {
            Thread.sleep(millis);
        } catch (InterruptedException e)
        {
            e.printStackTrace();
        }
    }

    /**
     * 用于判断，是否已经接近border
     * 0 and -mRightViewWidth
     *
     * @param leftMargin
     * @param speed
     * @return
     */
    private boolean isCloseToBorder(int leftMargin, int speed)
    {
        if (speed > 0)
        {
            if (leftMargin <= 0 && leftMargin + speed >= 0)
            {
                return true;
            }
        } else
        {
            if (leftMargin >= -mRightViewWidth &&
                    leftMargin + speed <= -mRightViewWidth)
            {
                return true;
            }
        }

        return false;
    }

    private View getPointerView(int x, int y)
    {
        int CurPosition = pointToPosition(x, y);
        return getChildAt(CurPosition - getFirstVisiblePosition());
    }

    private void scrollItem(int speed)
    {
        new ScrollTask().execute(speed);
    }

    private void closeItem(int speed)
    {
        scrollItem(Math.abs(speed));
    }

    private void openItem(int speed)
    {
        scrollItem(-Math.abs(speed));
    }

    public void setOnItemRightViewClickListener(OnItemRightViewClickListener listener)
    {
        this.mListener = listener;
    }

    /**
     * 点击rightView对外接口
     */
    public interface OnItemRightViewClickListener
    {
        public void onItemRightViewClick(int position, View view);
    }
}
