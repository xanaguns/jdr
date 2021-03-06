package com.jeongdaeri.unsplash_app_tutorial.activities

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Point
import android.os.Bundle
import android.text.InputFilter
import android.util.Log
import android.view.Menu
import android.view.View
import android.widget.CompoundButton
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.jakewharton.rxbinding4.widget.textChanges
import com.jeongdaeri.unsplash_app_tutorial.R
import com.jeongdaeri.unsplash_app_tutorial.model.Photo
import com.jeongdaeri.unsplash_app_tutorial.model.SearchData
import com.jeongdaeri.unsplash_app_tutorial.recyclerview.ISearchHistoryRecyclerView
import com.jeongdaeri.unsplash_app_tutorial.recyclerview.PhotoGridRecyeclerViewAdapter
import com.jeongdaeri.unsplash_app_tutorial.recyclerview.SearchHistoryRecyclerViewAdapter
import com.jeongdaeri.unsplash_app_tutorial.retrofit.RetrofitManager
import com.jeongdaeri.unsplash_app_tutorial.utils.Constants.TAG
import com.jeongdaeri.unsplash_app_tutorial.utils.RESPONSE_STATUS
import com.jeongdaeri.unsplash_app_tutorial.utils.SharedPrefManager
import com.jeongdaeri.unsplash_app_tutorial.utils.textChangesToFlow
import com.jeongdaeri.unsplash_app_tutorial.utils.toSimpleString
import io.reactivex.rxjava3.annotations.SchedulerSupport
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_photo_collection.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList
import kotlin.coroutines.CoroutineContext

class PhotoCollectionActivity: AppCompatActivity(),
                                SearchView.OnQueryTextListener,
                                CompoundButton.OnCheckedChangeListener,
                                View.OnClickListener,
                                ISearchHistoryRecyclerView
{


    // ?????????
    private var photoList = ArrayList<Photo>()

    // ?????? ?????? ??????
    private var searchHistoryList = ArrayList<SearchData>()

    // ?????????
    // lateinit ??? ?????? ????????? ???????????? ???????????? ??????.
    private lateinit var photoGridRecyeclerViewAdapter: PhotoGridRecyeclerViewAdapter
    private lateinit var mySearchHistoryRecyclerViewAdapter: SearchHistoryRecyclerViewAdapter


    // ?????????
    private lateinit var mySearchView: SearchView

    // ????????? ?????? ?????????
    private lateinit var mySearchViewEditText: EditText

    /* rx ????????????
    // ???????????? ?????? ????????? ?????? CompositeDisposable
    private var myCompositeDisposable = CompositeDisposable()
     */

    private var myCoroutineJob : Job = Job()
    private val myCoroutineContext: CoroutineContext
        get() = Dispatchers.IO + myCoroutineJob


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_photo_collection)

        val bundle = intent.getBundleExtra("array_bundle")

        val searchTerm = intent.getStringExtra("search_term")

        Log.d(TAG, "PhotoCollectionActivity - onCreate() called / searchTerm : $searchTerm, photoList.count() : ${photoList.count()}")






        search_history_mode_switch.setOnCheckedChangeListener(this)
        clear_search_history_buttton.setOnClickListener(this)

        search_history_mode_switch.isChecked = SharedPrefManager.checkSearchHistoryMode()

        top_app_bar.title = searchTerm

        // ?????????????????? ?????? ???????????? ???????????? ????????????.
        setSupportActionBar(top_app_bar)

        photoList = bundle?.getSerializable("photo_array_list") as ArrayList<Photo>


        // ?????? ?????????????????? ??????
        this.photoCollectionRecyclerViewSetting(this.photoList)


        // ????????? ?????? ?????? ????????????
        this.searchHistoryList = SharedPrefManager.getSearchHistoryList() as ArrayList<SearchData>

        this.searchHistoryList.forEach {
            Log.d(TAG, "????????? ?????? ?????? - it.term : ${it.term} , it.timestamp: ${it.timestamp}")
        }

        handleSearchViewUi()

        // ?????? ?????? ?????????????????? ??????
        this.searchHistoryRecyclerViewSetting(this.searchHistoryList)

        if(searchTerm.isNotEmpty()){
            val term = searchTerm?.let {
                it
            }?: ""
            this.insertSearchTermHistory(term)
        }


    } // onCreate

    override fun onDestroy() {
        Log.d(TAG, "PhotoCollectionActivity - onDestroy() called")
        /* rx ?????? ??????
        // ?????? ??????
        this.myCompositeDisposable.clear()
         */
        myCoroutineContext.cancel()

        super.onDestroy()
    }


    // ?????? ?????? ?????????????????? ??????
    private fun searchHistoryRecyclerViewSetting(searchHistoryList: ArrayList<SearchData>){
        Log.d(TAG, "PhotoCollectionActivity - searchHistoryRecyclerViewSetting() called")

        //
        this.mySearchHistoryRecyclerViewAdapter = SearchHistoryRecyclerViewAdapter(this)
        this.mySearchHistoryRecyclerViewAdapter.submitList(searchHistoryList)

        val myLinearLayoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, true)
        myLinearLayoutManager.stackFromEnd = true

        search_history_recycler_view.apply {
            layoutManager = myLinearLayoutManager
            this.scrollToPosition(mySearchHistoryRecyclerViewAdapter.itemCount - 1)
            adapter = mySearchHistoryRecyclerViewAdapter
        }

    }


    // ????????? ?????? ?????????????????? ??????
    private fun photoCollectionRecyclerViewSetting(photoList: ArrayList<Photo>){
        Log.d(TAG, "PhotoCollectionActivity - photoCollecitonRecyclerViewSetting() called")

        this.photoGridRecyeclerViewAdapter = PhotoGridRecyeclerViewAdapter()

        this.photoGridRecyeclerViewAdapter.submitList(photoList)

        my_photo_recycler_view.layoutManager = GridLayoutManager(this,
            2,
            GridLayoutManager.VERTICAL,
            false)
        my_photo_recycler_view.adapter = this.photoGridRecyeclerViewAdapter

    }

    @FlowPreview
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        Log.d(TAG, "PhotoCollectionActivity - onCreateOptionsMenu() called")

        val inflater = menuInflater

        inflater.inflate(R.menu.top_app_bar_menu, menu)

        val searchManager = getSystemService(Context.SEARCH_SERVICE) as SearchManager

        this.mySearchView = menu?.findItem(R.id.search_menu_item)?.actionView as SearchView

        this.mySearchView.apply {
            this.queryHint = "???????????? ??????????????????"

            this.setOnQueryTextListener(this@PhotoCollectionActivity)

            this.setOnQueryTextFocusChangeListener { _, hasExpaned ->
                when(hasExpaned) {
                    true -> {
                        Log.d(TAG, "????????? ??????")
//                        linear_search_history_view.visibility = View.VISIBLE

                        handleSearchViewUi()
                    }
                    false -> {
                        Log.d(TAG, "????????? ??????")
                        linear_search_history_view.visibility = View.INVISIBLE
                    }
                }
            }

            // ??????????????? ?????????????????? ????????????.
            mySearchViewEditText = this.findViewById(androidx.appcompat.R.id.search_src_text)

            /* Rx ?????? ???
            // ??????????????? ????????????
            val editTextChangeObservable = mySearchViewEditText.textChanges()

            val searchEditTextSubscription : Disposable =
                // ??????????????? ?????????????????? ??????
                editTextChangeObservable
                    // ????????? ?????? ?????? ?????? 0.8 ??? ?????? onNext ???????????? ????????? ???????????????
                    .debounce(1000, TimeUnit.MILLISECONDS)
                    // IO ??????????????? ????????????.
                    // Scheduler instance intended for IO-bound work.
                    // ???????????? ??????, ?????? ??????,??????, ???????????? ???
                    .subscribeOn(Schedulers.io())
                    // ????????? ?????? ????????? ?????? ??????
                    .subscribeBy(
                        onNext = {
                            Log.d("RX", "onNext : $it")
                            //TODO:: ??????????????? ????????? ???????????? api ??????
                            if (it.isNotEmpty()){
                                searchPhotoApiCall(it.toString())
                            }
                        },
                        onComplete = {
                            Log.d("RX", "onComplete")
                        },
                        onError = {
                            Log.d("RX", "onError : $it")
                        }
                    )
            // compositeDisposable ??? ??????
            myCompositeDisposable.add(searchEditTextSubscription)
      */

            // Rx??? ??????????????? ??????
            // IO ??????????????? ????????????
            GlobalScope.launch(context = myCoroutineContext){

                // editText ??? ??????????????????
                val editTextFlow = mySearchViewEditText.textChangesToFlow()

                editTextFlow
                    // ????????????
                    // ???????????? ?????? 2??? ?????? ?????????
                    .debounce(2000)
                    .filter {
                        it?.length!! > 0
                    }
                    .onEach {
                        Log.d(TAG, "flow??? ????????? $it")
                        // ?????? ???????????? api ??????
                        searchPhotoApiCall(it.toString())
                    }
                    .launchIn(this)
            }


        }



        this.mySearchViewEditText.apply {
            this.filters = arrayOf(InputFilter.LengthFilter(12))
            this.setTextColor(Color.WHITE)
            this.setHintTextColor(Color.WHITE)
        }


        return true
    }


    // ????????? ????????? ?????? ?????????
    // ??????????????? ??????????????????
    override fun onQueryTextSubmit(query: String?): Boolean {

        Log.d(TAG, "PhotoCollectionActivity - onQueryTextSubmit() called / query: $query")


        if(!query.isNullOrEmpty()){
            this.top_app_bar.title = query

            //TODO:: api ??????
            //TODO:: ????????? ??????
            this.insertSearchTermHistory(query)
            this.searchPhotoApiCall(query)
        }

//        this.mySearchView.setQuery("", false)
//        this.mySearchView.clearFocus()

        this.top_app_bar.collapseActionView()

        return true
    }

    override fun onQueryTextChange(newText: String?): Boolean {
        Log.d(TAG, "PhotoCollectionActivity - onQueryTextChange() called / newText: $newText")

//        val userInputText = newText ?: ""

        val userInputText = newText.let {
            it
        }?: ""

        if(userInputText.count() == 12){
            Toast.makeText(this, "???????????? 12??? ????????? ?????? ???????????????.", Toast.LENGTH_SHORT).show()
        }

//        if(userInputText.length in 1..12){
//            searchPhotoApiCall(userInputText)
//        }

        return true
    }


    override fun onCheckedChanged(switch: CompoundButton?, isChecked: Boolean) {
        when(switch){
            search_history_mode_switch ->{
                if(isChecked == true){
                    Log.d(TAG, "????????? ???????????? ???")
                    SharedPrefManager.setSearchHistoryMode(isActivated = true)
                } else {
                    Log.d(TAG, "????????? ???????????? ??????")
                    SharedPrefManager.setSearchHistoryMode(isActivated = false)
                }
            }

        }
    }

    override fun onClick(view: View?) {
        when(view){
            clear_search_history_buttton -> {
                Log.d(TAG, "?????? ?????? ?????? ????????? ?????? ?????????.")
                SharedPrefManager.clearSearchHistoryList()
                this.searchHistoryList.clear()
                // ui ??????
                handleSearchViewUi()
            }
        }
    }

    // ?????? ??????????????? ?????? ?????????
    override fun onSearchItemDeleteClicked(position: Int) {
        Log.d(TAG, "PhotoCollectionActivity - onSearchItemDeleteClicked() called / position: $position")
        // ?????? ?????? ??????
        this.searchHistoryList.removeAt(position)
        // ????????? ????????????
        SharedPrefManager.storeSearchHistoryList(this.searchHistoryList)
        // ????????? ?????? ????????? ?????????
        this.mySearchHistoryRecyclerViewAdapter.notifyDataSetChanged()

        handleSearchViewUi()
    }

    // ?????? ????????? ?????? ?????????
    override fun onSearchItemClicked(position: Int) {
        Log.d(TAG, "PhotoCollectionActivity - onSearchItemClicked() called / position: $position")
        // TODO:: ?????? ????????? ???????????? API ??????

        val queryString = this.searchHistoryList[position].term

        searchPhotoApiCall(queryString)

        top_app_bar.title = queryString

        this.insertSearchTermHistory(searchTerm = queryString)

        this.top_app_bar.collapseActionView()


    }


    // ?????? ?????? API ??????
    private fun searchPhotoApiCall(query: String){

        RetrofitManager.instance.searchPhotos(searchTerm = query, completion = { status, list ->
            when(status){
                RESPONSE_STATUS.OKAY -> {
                    Log.d(TAG, "PhotoCollectionActivity - searchPhotoApiCall() called ?????? ?????? / list.size : ${list?.size}")

                    if (list != null){
                        this.photoList.clear()
                        this.photoList = list
                        this.photoGridRecyeclerViewAdapter.submitList(this.photoList)
                        this.photoGridRecyeclerViewAdapter.notifyDataSetChanged()
                    }

                }
                RESPONSE_STATUS.NO_CONTENT -> {
                    Toast.makeText(this, "$query ??? ?????? ?????? ????????? ????????????.", Toast.LENGTH_SHORT).show()
                }
            }
        })

    }


    private fun handleSearchViewUi(){
        Log.d(TAG, "PhotoCollectionActivity - handleSearchViewUi() called / size : ${this.searchHistoryList.size}")

        if(this.searchHistoryList.size > 0){
            search_history_recycler_view.visibility = View.VISIBLE
            search_history_recycler_view_label.visibility = View.VISIBLE
            clear_search_history_buttton.visibility = View.VISIBLE
        } else {
            search_history_recycler_view.visibility = View.INVISIBLE
            search_history_recycler_view_label.visibility = View.INVISIBLE
            clear_search_history_buttton.visibility = View.INVISIBLE
        }

    }

    // ????????? ??????
    private fun insertSearchTermHistory(searchTerm: String){
        Log.d(TAG, "PhotoCollectionActivity - insertSearchTermHistory() called")

        if(SharedPrefManager.checkSearchHistoryMode() == true){
            // ?????? ????????? ??????
            var indexListToRemove = ArrayList<Int>()

            this.searchHistoryList.forEachIndexed{ index, searchDataItem ->

                if(searchDataItem.term == searchTerm){
                    Log.d(TAG, "index: $index")
                    indexListToRemove.add(index)
                }
            }

            indexListToRemove.forEach {
                this.searchHistoryList.removeAt(it)
            }

            // ??? ????????? ??????
            val newSearchData = SearchData(term = searchTerm, timestamp = Date().toSimpleString())
            this.searchHistoryList.add(newSearchData)

            // ?????? ???????????? ????????????
            SharedPrefManager.storeSearchHistoryList(this.searchHistoryList)

            this.mySearchHistoryRecyclerViewAdapter.notifyDataSetChanged()
        }

    }


}














