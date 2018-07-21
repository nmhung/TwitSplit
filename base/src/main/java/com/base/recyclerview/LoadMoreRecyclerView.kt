package com.base.recyclerview

import android.content.Context
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.StaggeredGridLayoutManager
import android.util.AttributeSet
import com.base.R

/**
 * Created by vophamtuananh on 1/7/18.
 */
class LoadMoreRecyclerView : RecyclerView {

    companion object {
        private const val NUMBER_TO_LOAD_MORE_DEFAULT = 1
    }

    private var mAdapterWrapper: LoadMoreAdapter? = null

    private var mOnLoadMoreListener: OnLoadMoreListener? = null

    private var mVisibleCountToLoadMore = 1

    private var mReachedEnd: Boolean = false

    private var isLoading: Boolean = false

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(context, attrs, defStyle) {
        val a = context.obtainStyledAttributes(attrs, R.styleable.LoadMoreRecyclerView, defStyle, 0)

        mVisibleCountToLoadMore = a.getInt(R.styleable.LoadMoreRecyclerView_number_to_load_more, NUMBER_TO_LOAD_MORE_DEFAULT)

        a.recycle()
    }

    private val mEndScrollListener = object : RecyclerView.OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView?, dx: Int, dy: Int) {
            super.onScrolled(recyclerView, dx, dy)

            if (dy < 0 || mReachedEnd || isLoading)
                return

            val layoutManager = layoutManager

            val totalItemCount = layoutManager.itemCount

            val lastVisibleItemPosition: Int

            lastVisibleItemPosition = when (layoutManager) {
                is GridLayoutManager -> layoutManager.findLastVisibleItemPosition()
                is StaggeredGridLayoutManager -> {
                    val into = IntArray(layoutManager.spanCount)
                    layoutManager.findLastVisibleItemPositions(into)
                    findMax(into)
                }
                else -> (layoutManager as LinearLayoutManager).findLastVisibleItemPosition()
            }

            if (!isLoading && totalItemCount <= lastVisibleItemPosition + mVisibleCountToLoadMore) {
                mOnLoadMoreListener?.onLoadMore()
            }
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        addOnScrollListener(mEndScrollListener)
    }

    override fun onDetachedFromWindow() {
        mOnLoadMoreListener = null
        removeOnScrollListener(mEndScrollListener)
        super.onDetachedFromWindow()
    }

    override fun setLayoutManager(layout: RecyclerView.LayoutManager) {
        super.setLayoutManager(layout)
        if (layout is GridLayoutManager) {
            layout.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                override fun getSpanSize(position: Int): Int {
                    return if (mAdapterWrapper != null && mAdapterWrapper!!.isLoadMoreView(position)) {
                        layout.spanCount
                    } else 1
                }
            }
        }
    }

    fun setOnLoadMoreListener(onLoadMoreListener: OnLoadMoreListener) {
        mOnLoadMoreListener = onLoadMoreListener
    }

    private fun findMax(lastPositions: IntArray): Int {
        return lastPositions.max() ?: 0
    }

    fun <R : RecyclerAdapter<RecyclerAdapter.BaseHolder<*, Any>, Any>> setAdapter(adapter: R) {
        mAdapterWrapper = LoadMoreAdapter(adapter)
        val dataObserver = DataObserver(mAdapterWrapper!!)
        super.setAdapter(mAdapterWrapper)
        adapter.registerDataObserver(dataObserver)
    }

    fun setLoading(loading: Boolean) {
        if (isLoading == loading)
            return
        isLoading = loading
        mAdapterWrapper?.setLoading(isLoading)
    }

    fun reachedEnd(reachedEnd: Boolean) {
        if (mReachedEnd == reachedEnd)
            return
        mReachedEnd = reachedEnd
        mAdapterWrapper?.reachedEnd(mReachedEnd)
    }

    interface OnLoadMoreListener {
        fun onLoadMore()
    }
}