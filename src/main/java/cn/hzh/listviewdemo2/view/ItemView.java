package cn.hzh.listviewdemo2.view;

import android.content.Context;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.LinearLayout;

/**
 * Created by hzh on 2015/11/2.
 */
public class ItemView extends LinearLayout
{
    private ViewGroup mContentView;
    private ViewGroup mRightView;

    //dp
    private static final int RIGHT_VIEW_WIDTH = 100;
    private int mRightViewWidth;

    public ItemView(Context context, AttributeSet attrs)
    {
        super(context, attrs);

        float density = getResources().getDisplayMetrics().density;
        mRightViewWidth = (int) (RIGHT_VIEW_WIDTH * density);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b)
    {
        mContentView = (ViewGroup) getChildAt(0);
        mRightView = (ViewGroup) getChildAt(1);
        //设置contentview宽度
        int width = getMeasuredWidth();
        mContentView.getLayoutParams().width = width;
        //设置右侧view的宽度
        MarginLayoutParams lp = (MarginLayoutParams) mRightView.getLayoutParams();
        switch (lp.width)
        {
            case ViewGroup.LayoutParams.MATCH_PARENT:
            case ViewGroup.LayoutParams.WRAP_CONTENT:
                int count = mRightView.getChildCount();
                if(count > 1)
                    mRightViewWidth = mRightViewWidth * count;
                lp.width = mRightViewWidth;
                break;
        }

        super.onLayout(changed, l, t, r, b);
    }
}
