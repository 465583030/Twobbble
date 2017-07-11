package com.twobbble.view.fragment


import android.animation.Animator
import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.widget.DefaultItemAnimator
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Pair
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.twobbble.R
import com.twobbble.application.App
import com.twobbble.entity.Shot
import com.twobbble.presenter.service.ShotsPresenter
import com.twobbble.tools.*
import com.twobbble.view.activity.DetailsActivity
import com.twobbble.view.activity.UserActivity
import com.twobbble.view.adapter.ItemShotAdapter
import com.twobbble.view.api.IShotsView
import kotlinx.android.synthetic.main.error_layout.*
import kotlinx.android.synthetic.main.fragment_shots.*
import kotlinx.android.synthetic.main.item_card_head.*
import kotlinx.android.synthetic.main.item_shots.*
import kotlinx.android.synthetic.main.list.*
import org.greenrobot.eventbus.EventBus
import org.jetbrains.anko.find

/**
 * Created by liuzipeng on 2017/2/17.
 */
class ShotsFragment : BaseFragment(), IShotsView {
    private val mPresenter: ShotsPresenter by lazy {
        ShotsPresenter(this)
    }
    private var mSort: String? = null
    private var mSortList: String? = null
    private var mTimeFrame: String? = null
    private var mPage: Int = 1
    private val mShots: MutableList<Shot> by lazy {
        mutableListOf<Shot>()
    }
    private var mListAdapter: ItemShotAdapter? = null
    private var isLoading: Boolean = false

    companion object {
        val SORT = "sort"
        val RECENT = "recent"
        fun newInstance(sort: String? = null): ShotsFragment {
            val fragment = ShotsFragment()
            val args = Bundle()
            args.putString(SORT, sort)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mSort = arguments?.getString(SORT)
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return LayoutInflater.from(activity).inflate(R.layout.fragment_shots, null)
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        initView()
        getShots(false)
    }

    private fun initView() {
        mRefresh.setColorSchemeResources(R.color.google_red, R.color.google_yellow, R.color.google_green, R.color.google_blue)
        val layoutManager = LinearLayoutManager(App.instance, LinearLayoutManager.VERTICAL, false)
        mRecyclerView.layoutManager = layoutManager
        mRecyclerView.itemAnimator = DefaultItemAnimator()
    }

    fun getShots(isLoadMore: Boolean = false) {
        isLoading = true
        val token = singleData.token ?: Constant.ACCESS_TOKEN
        mPresenter.getShots(access_token = token,
                sort = mSort,
                list = mSortList,
                timeframe = mTimeFrame,
                page = mPage,
                isLoadMore = isLoadMore)
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
    }

    override fun onStart() {
        super.onStart()
        bindEvent()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mPresenter.unSubscriber()
    }

    private fun bindEvent() {
        mRefresh.setOnRefreshListener {
            mPage = 1
            getShots(false)
        }

        mErrorLayout.setOnClickListener {
            hideErrorImg(mErrorLayout)
            getShots(false)
        }

        mRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView?, dx: Int, dy: Int) {
                val linearManager = recyclerView?.layoutManager as LinearLayoutManager
                val position = linearManager.findLastVisibleItemPosition()
                if (mShots.isNotEmpty() && position == mShots.size) {
                    if (!isLoading) {
                        mPage += 1
                        getShots(true)
                    }
                }
            }
        })
    }

    override fun showProgress() {
        if (mListAdapter == null) mRefresh.isRefreshing = true
    }

    override fun hideProgress() {
        mRefresh.isRefreshing = false
    }

    override fun getShotSuccess(shots: MutableList<Shot>?, isLoadMore: Boolean) {
        isLoading = false
        if (shots != null && shots.isNotEmpty()) {
            if (!isLoadMore) {
                mountList(shots)
            } else {
                mListAdapter?.addItems(shots)
            }
        } else {
            if (!isLoadMore) {
                showErrorImg(mErrorLayout)
            } else {
                mListAdapter?.loadError {
                    getShots(true)
                }
            }
        }
    }

    private fun mountList(shots: MutableList<Shot>) {
        mShots.addAll(shots)
        mListAdapter = ItemShotAdapter(mShots, {
            EventBus.getDefault().postSticky(mShots[it])
            startDetailsActivity()
        }, {
            EventBus.getDefault().postSticky(shots[it].user)
            startActivity(Intent(activity, UserActivity::class.java))
        })
        mRecyclerView.adapter = mListAdapter
    }

    override fun getShotFailed(msg: String, isLoadMore: Boolean) {
        isLoading = false
        toast(msg)
        if (!isLoadMore) {
            if (mListAdapter == null)
                showErrorImg(mErrorLayout, msg, R.mipmap.img_network_error_2)
        } else {
            mListAdapter?.loadError {
                getShots(true)
            }
        }
    }
}