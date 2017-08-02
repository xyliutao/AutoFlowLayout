package com.example.lvruheng.autoflowlayout;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import java.util.ArrayList;
import java.util.List;

/**
 *自定义LinearLayout，支持自动换行，指定行数,实现流式布局
 */
public class AutoFlowLayout <T> extends LinearLayout  {
    /**
     * 存储所有的View，按行记录
     */
    private List<List<View>> mAllViews = new ArrayList<List<View>>();
    /**
     * 记录设置单行显示的标志
     */
    private boolean mIsSingleLine;
    /**
     * 记录每一行的最大高度
     */
    private List<Integer> mLineHeight = new ArrayList<Integer>();
    /**
     * 记录设置最大行数量
     */
    private int mMaxLineNumbers;
    /**
     * 记录当前行数
     */
    private int mCount;

    /**
     * 是否还有数据没显示
     */
    private boolean mHasMoreData;
    /**
     * 子View的点击事件
     */
    private OnItemClickListener mOnItemClickListener;
    /**
     * 当前view的索引
     */
    private int  mCurrentItemIndex=-1;
    /**
     * 多选标志，默认支持单选
     */
    private boolean mIsMultiChecked;
    /**
     * 设置View的选中/未选中背景色或者单纯的背景色
     */
    private Drawable mViewBgDrawable;
    /**
     * 记录选中的View
     */
    private View mSelectedView;
    /**
     *记录选中的View
     */
    private List<View> mCheckedViews  = new ArrayList<>();
    /**
     * 记录展示的数量
     */
    private int mDisplayNumbers;
    /**
     * 数据适配器
     */
    private FlowAdapter<T> mAdapter;
    /**
     * 水平方向View之间的间距
     */
    private int  mHorizontalSpace;
    /**
     * 竖直方向View之间的间距
     */
    private int mVerticalSpace;
    /**
     * 列数
     */
    private int mColumnNumbers;
    /**
     * 行数
     */
    private int mRowNumbers;
    /**
     * 是否设置了网格布局
     */
    private boolean mIsGridMode;

    public AutoFlowLayout(Context context) {
        super(context);
    }

    public AutoFlowLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context,attrs);
    }

    public AutoFlowLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context,attrs);
    }
    private void init(Context context, AttributeSet attrs) {
        TypedArray ta = context.obtainStyledAttributes(attrs,R.styleable.AutoFlowLayout);
        mIsSingleLine = ta.getBoolean(R.styleable.AutoFlowLayout_singleLine,false);
        mMaxLineNumbers = ta.getInteger(R.styleable.AutoFlowLayout_maxLines,Integer.MAX_VALUE);
        mIsMultiChecked = ta.getBoolean(R.styleable.AutoFlowLayout_multiChecked,false);
        mHorizontalSpace = ta.getInteger(R.styleable.AutoFlowLayout_horizontalSpace,0);
        mVerticalSpace = ta.getInteger(R.styleable.AutoFlowLayout_verticalSpace,0);
        mColumnNumbers = ta.getInteger(R.styleable.AutoFlowLayout_columnNumbers,0);
        mRowNumbers = ta.getInteger(R.styleable.AutoFlowLayout_rowNumbers,0);
        if (mColumnNumbers != 0) {
            mIsGridMode = true;
        }
        ta.recycle();
        setOrientation(HORIZONTAL);
    }
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (mIsGridMode) {
            setGridMeasure(widthMeasureSpec,heightMeasureSpec);
        } else {
            setFlowMeasure(widthMeasureSpec,heightMeasureSpec);
        }


    }

    /**
     * 网格布局的测量模式 默认各个子VIEW宽高值相同
     * @param widthMeasureSpec
     * @param heightMeasureSpec
     */
    private void setGridMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // 获得它的父容器为它设置的测量模式和大小
        int sizeWidth = MeasureSpec.getSize(widthMeasureSpec);
        int sizeHeight = MeasureSpec.getSize(heightMeasureSpec);
        int modeWidth = MeasureSpec.getMode(widthMeasureSpec);
        int modeHeight = MeasureSpec.getMode(heightMeasureSpec);
        //获取viewgroup的padding
        int paddingLeft = getPaddingLeft();
        int paddingRight = getPaddingRight();
        int paddingTop = getPaddingTop();
        int paddingBottom = getPaddingBottom();
        //超宽/超高纪录的最后值
        int lastHeight;
        int lastWidth;
        //最终的宽高值
        int heightResult;
        int widthResult;
        //未设置行数 推测行数
        if (mRowNumbers == 0) {
            mRowNumbers = getChildCount()/mColumnNumbers == 0 ?
                    getChildCount()/mColumnNumbers : (getChildCount()/mColumnNumbers + 1);
        }
        int maxChildHeight = 0;
        int maxWidth = 0;
        int maxHeight = 0;
        //统计最大高度/最大宽度
        for (int i = 0; i <  mRowNumbers; i++) {
            for (int j = 0; j < mColumnNumbers; j++) {
                final View child = getChildAt(i * mColumnNumbers + j);
                if (child != null) {
                    if (child.getVisibility() != GONE) {
                        final LayoutParams lp = (LayoutParams) child.getLayoutParams();
                        final int childWidthMeasureSpec = getChildMeasureSpec(widthMeasureSpec, 0, lp.width);
                        final int childHeightMeasureSpec = getChildMeasureSpec(heightMeasureSpec, 0, lp.height);
                        child.measure(childWidthMeasureSpec, childHeightMeasureSpec);
                        maxWidth +=child.getMeasuredWidth();
                        maxChildHeight = Math.max(maxChildHeight, child.getMeasuredHeight());
                    }
                }
            }
            maxHeight += maxChildHeight;
            maxChildHeight = 0;
        }
        int tempWidth = maxWidth+mHorizontalSpace*(mColumnNumbers-1)+paddingLeft+paddingRight;
        int tempHeight = maxHeight+mVerticalSpace*(mRowNumbers-1)+paddingBottom+paddingTop;
        if (tempWidth > sizeWidth) {
            widthResult = sizeWidth;
        } else {
            widthResult = tempWidth;
        }
        //宽高超过屏幕大小，则进行压缩存放
        if (tempHeight > sizeHeight) {
            heightResult = sizeHeight;
        } else {
            heightResult = tempHeight;
        }
        setMeasuredDimension((modeWidth == MeasureSpec.EXACTLY) ? sizeWidth
                : widthResult, (modeHeight == MeasureSpec.EXACTLY) ? sizeHeight
                : heightResult);
    }

    /**
     * 流式布局的测量模式
     * @param widthMeasureSpec
     * @param heightMeasureSpec
     */
    private void setFlowMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // 获得它的父容器为它设置的测量模式和大小
        int sizeWidth = MeasureSpec.getSize(widthMeasureSpec);
        int sizeHeight = MeasureSpec.getSize(heightMeasureSpec);
        int modeWidth = MeasureSpec.getMode(widthMeasureSpec);
        int modeHeight = MeasureSpec.getMode(heightMeasureSpec);
        // 如果是warp_content情况下，记录宽和高
        int width = 0;
        int height = 0;
        /**
         * 记录每一行的宽度，width不断取最大宽度
         */
        int lineWidth = getPaddingLeft() + getPaddingRight();
        /**
         * 每一行的高度，累加至height
         */
        int lineHeight = getPaddingTop() + getPaddingBottom();

        int cCount = getChildCount();

        // 遍历每个子元素
        for (int i = 0; i < cCount; i++) {
            View child = getChildAt(i);
            // 测量每一个child的宽和高
            measureChild(child, widthMeasureSpec, heightMeasureSpec);
            // 得到child的lp
            MarginLayoutParams lp = (MarginLayoutParams) child
                    .getLayoutParams();
            // 当前子空间实际占据的宽度
            int childWidth = child.getMeasuredWidth() + lp.leftMargin
                    + lp.rightMargin;
            // 当前子空间实际占据的高度
            int childHeight = child.getMeasuredHeight() + lp.topMargin
                    + lp.bottomMargin;
            /**
             * 如果加入当前child，则超出最大宽度，则的到目前最大宽度给width，类加height 然后开启新行
             */
            if (lineWidth + childWidth > sizeWidth) {
                width = Math.max(lineWidth, childWidth);// 取最大的
                lineWidth = childWidth; // 重新开启新行，开始记录
                // 叠加当前高度，
                height += lineHeight;
                // 开启记录下一行的高度
                lineHeight = childHeight;
                mCount++;
                if (mCount>=mMaxLineNumbers) {
                    setHasMoreData(i+1,cCount);
                    break;
                }
                if (mIsSingleLine) {
                    setHasMoreData(i+1,cCount);
                    break;
                }
            } else
            // 否则累加值lineWidth,lineHeight取最大高度
            {
                lineWidth += childWidth;
                lineHeight = Math.max(lineHeight, childHeight);
            }
            // 如果是最后一个，则将当前记录的最大宽度和当前lineWidth做比较
            if (i == cCount - 1) {
                width = Math.max(width, lineWidth);
                height += lineHeight;
            }

        }
        setMeasuredDimension((modeWidth == MeasureSpec.EXACTLY) ? sizeWidth
                : width, (modeHeight == MeasureSpec.EXACTLY) ? sizeHeight
                : height);
    }


    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        if (mIsGridMode) {
           setGridLayout();
        } else {
            setFlowLayout();
        }
    }

    /**
     * 网格布局的布局模式
     */
    private void setGridLayout() {
        int sizeWidth = getWidth();
        int sizeHeight = getHeight();
        //子View的平均宽高
        int childAvWidth = (sizeWidth - getPaddingLeft() - getPaddingRight() - mHorizontalSpace * (mColumnNumbers-1))/mColumnNumbers;
        int childAvHeight = (sizeHeight - getPaddingTop() - getPaddingBottom() - mVerticalSpace * (mRowNumbers-1))/mRowNumbers;
        for (int i = 0; i < mRowNumbers; i++) {
            for (int j = 0; j < mColumnNumbers; j++) {
                final View child = getChildAt(i * mColumnNumbers + j);
                if (child != null) {
                    if (child.getVisibility() != View.GONE) {
                        int childLeft = getPaddingLeft() + j * (childAvWidth + mHorizontalSpace);
                        int childTop = getPaddingTop() + i * (childAvHeight + mVerticalSpace);
                        child.layout(childLeft, childTop, childLeft + childAvWidth, childAvHeight +childTop);
                    }
                }
            }
        }
    }

    /**
     * 流式布局的布局模式
     */
    private void setFlowLayout() {
        mAllViews.clear();
        mLineHeight.clear();

        int width = getWidth();

        int lineWidth = getPaddingLeft();
        int lineHeight = getPaddingTop();
        // 存储每一行所有的childView
        List<View> lineViews = new ArrayList<View>();
        int cCount = getChildCount();
        // 遍历所有的孩子
        for (int i = 0; i < cCount; i++) {
            View child = getChildAt(i);
            MarginLayoutParams lp = (MarginLayoutParams) child
                    .getLayoutParams();
            int childWidth = child.getMeasuredWidth();
            int childHeight = child.getMeasuredHeight();

            // 如果已经需要换行
            if (childWidth + lp.leftMargin + lp.rightMargin + getPaddingRight() + lineWidth > width) {
                // 记录这一行所有的View以及最大高度
                mLineHeight.add(lineHeight);
                // 将当前行的childView保存，然后开启新的ArrayList保存下一行的childView
                mAllViews.add(lineViews);
                lineWidth = 0;// 重置行宽
                lineViews = new ArrayList<View>();
                mCount++;
                if (mCount>=mMaxLineNumbers) {
                    setHasMoreData(i+1,cCount);
                    break;
                }
                if (mIsSingleLine) {
                    setHasMoreData(i+1,cCount);
                    break;
                }
            }
            /**
             * 如果不需要换行，则累加
             */
            lineWidth += childWidth + lp.leftMargin + lp.rightMargin;
            lineHeight = Math.max(lineHeight, childHeight + lp.topMargin
                    + lp.bottomMargin);
            lineViews.add(child);
        }
        // 记录最后一行
        mLineHeight.add(lineHeight);
        mAllViews.add(lineViews);

        int left = getPaddingLeft();
        int top = getPaddingTop();
        // 得到总行数
        int lineNums = mAllViews.size();
        if (mAllViews.get(mAllViews.size()-1).size() == 0){
            lineNums = mAllViews.size()-1;
        }
        for (int i = 0; i < lineNums; i++) {
            // 每一行的所有的views
            lineViews = mAllViews.get(i);
            // 当前行的最大高度
            lineHeight = mLineHeight.get(i);
            // 遍历当前行所有的View
            for (int j = 0; j < lineViews.size(); j++) {
                final View child = lineViews.get(j);
                mCurrentItemIndex++;
                if (child.getVisibility() == View.GONE) {
                    continue;
                }
                if (child.getTag()==null){
                    child.setTag(mCurrentItemIndex);
                }
                child.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (mIsMultiChecked) {
                            if (mCheckedViews.contains(view)) {
                                mCheckedViews.remove(view);
                                view.setSelected(false);
                            } else {
                                view.setSelected(true);
                                mCheckedViews.add(view);
                                mSelectedView = view;
                            }
                        } else {
                            if (view.isSelected()) {
                                view.setSelected(false);
                            } else {
                                if (mSelectedView != null) {
                                    mSelectedView.setSelected(false);
                                }
                                view.setSelected(true);
                                mSelectedView = view;
                            }
                        }
                        if (mOnItemClickListener != null) {
                            mOnItemClickListener.onItemClick((Integer) view.getTag(),view);
                        }
                    }
                });
                MarginLayoutParams lp = (MarginLayoutParams) child
                        .getLayoutParams();

                //计算childView的left,top,right,bottom
                int lc = left + lp.leftMargin;
                int tc = top + lp.topMargin;
                int rc = lc + child.getMeasuredWidth();
                int bc = tc + child.getMeasuredHeight();


                child.layout(lc, tc, rc, bc);

                left += child.getMeasuredWidth() + lp.rightMargin
                        + lp.leftMargin;
            }
            left = getPaddingLeft();
            top += lineHeight;
        }
    }

    /**
     * 判断是否还有跟多View未展示
     * @param i 当前展示的View
     * @param count 总共需要展示的View
     */
    private void setHasMoreData(int i, int count) {
        if (i<count){
            mHasMoreData = true;
        }
    }

    public void setAllViews(List<View> views) {
        removeAllViews();
        if (views == null || views.size() == 0) {
            return;
        }
        for (int i = 0; i < views.size(); i++) {
            View view = views.get(i);
            if (mViewBgDrawable != null) {
                view.setBackgroundDrawable(mViewBgDrawable);
            }
            addView(view);
        }
        requestLayout();
    }
    /**
     * 删除指定索引的view
     * @param index true删除成功 false删除失败
     * @return
     */
    public boolean deleteView(int index) {
        if (mCurrentItemIndex != 0) {
            mDisplayNumbers = mCurrentItemIndex/2;
            if (index > mDisplayNumbers) {
               return  false;
            } else {
                removeViewAt(index);
                return true;
            }
        }
        return false;
    }

    /**
     * 删除最后一个view
     * @return  true删除成功 false删除失败
     */
    public boolean deleteView() {
        if (mCurrentItemIndex != 0) {
            mDisplayNumbers = mCurrentItemIndex/2;
            removeViewAt(mDisplayNumbers);
            return true;
        }
        return false;
    }

    /**
     * 删除指定范围的所有view
     * @param start 开始范围
     * @param end   结束范围
     * @return
     */
    public boolean deleteView(int start, int end) {
        if (mCurrentItemIndex != 0) {
            mDisplayNumbers = mCurrentItemIndex/2;
            if (start < 0) {
                start = 0;
            }
            if (end > mDisplayNumbers) {
                end = mDisplayNumbers;
            }
            removeViews(start,end-start+1);
            return true;
        }
        return false;
    }

    /**
     * 设置最多显示的行数
     * @param number
     */
    public void setMaxLines(int number) {
        mMaxLineNumbers = number;
    }

    /**
     * 是否只显示单行
     * @param isSingle
     */
    public void setSingleLine(boolean isSingle) {
        mIsSingleLine = isSingle;
    }

    /**
     * 是否单行显示
     * @return true 单行显示 false 多行显示
     */
    public boolean isSingleLine() {
        return mIsSingleLine;
    }

    /**
     * 支持显示的最大行数
     * @return 最大行数
     */
    public int getMaxLineNumbers() {
        return mMaxLineNumbers;
    }

    /**
     * 是否还有更多数据未显示
     * @return true 还有未显示数据 false 完全显示
     */
    public boolean hasMoreData() {
        return mHasMoreData;
    }

    /**
     * 是否支持多选
     * @return
     */
    public boolean isMultiChecked() {
        return mIsMultiChecked;
    }
    public void setMultiChecked(boolean isMultiChecked) {
        mIsMultiChecked = isMultiChecked;
    }
    /**
     * 设置选择背景
     * @param drawable  selector 设置选中与未选中的背景或者单纯的背景色
     */
    public void setCheckedBackgorud(Drawable drawable) {
        mViewBgDrawable = drawable;
    }

    /**
     * 获得选中的View集合
     * @return view集合
     */
    public List<View> getCheckedViews() {
        if (mIsMultiChecked) {
            return mCheckedViews;
        } else {
            mCheckedViews.add(mSelectedView);
            return mCheckedViews;
        }
    }

    /**
     * 获取上一个被选中的View
     * @return 被选中的view
     */
    public View  getSelectedView() {
        return  mSelectedView;
    }

    /**
     * 设置数据适配器
     * @param adapter
     */
    public  void setAdapter(FlowAdapter<T> adapter) {
        mAdapter = adapter;
        if (mAdapter.getCount() != 0) {
            for (int i = 0; i < mAdapter.getCount(); i ++) {
                View view = mAdapter.getView(i);
                addView(view);
            }
            requestLayout();
        }
    }

    /**
     * 设置网格布局的水平间距
     * @param horizontalSpace 单位px
     */
    public void setHorizontalSpace(int horizontalSpace) {
        mHorizontalSpace = horizontalSpace;
        requestLayout();
    }

    /**
     * 返回网格布局的水平距离
     * @return
     */
    public int getHorizontalSpace() {
        return mHorizontalSpace;
    }

    /**
     * 设置网格布局的垂直间距
     * @param verticalSpace 单位px
     */
    public void setVerticalSpace(int verticalSpace) {
        mVerticalSpace = verticalSpace;
        requestLayout();
    }

    /**
     * 返回网格布局的垂直距离
     */
    public int getVerticalSpace() {
        return mVerticalSpace;
    }

    /**
     * 设置列数
     * @param columnNumbers
     */
    public void setColumnNumbers(int columnNumbers) {
        mColumnNumbers = columnNumbers;
    }

    /**
     * 获得列数
     * @return
     */
    public int getColumnNumbers() {
        return mColumnNumbers;
    }

    /**
     * 设置行数
     * @param rowNumbers
     */
    public void setRowNumbers(int rowNumbers) {
        mRowNumbers = rowNumbers;
    }

    /**
     * 得到行数
     * @return
     */
    public int getRowNumbers() {
        return mRowNumbers;
    }


    public interface OnItemClickListener{
        void onItemClick(int position,View view);
    }
    public void setOnItemClickListener(OnItemClickListener onItemClickListener){
        mOnItemClickListener = onItemClickListener;
    }



}
