package com.example.trackcovid

import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import com.google.gson.Gson
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit


class MainActivity : AppCompatActivity() {

    lateinit var stateAdapter: StateAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        list.addHeaderView(LayoutInflater.from(this).inflate(R.layout.item_header, list, false))
        fetchResults()
        swipeToRefresh.setOnRefreshListener {
          //  swipeToRefresh.isRefreshing = true
            fetchResults()

        }
    }

    private fun fetchResults() {
        GlobalScope.launch{
            val response= withContext(Dispatchers.IO){ Client.api.clone().execute() }

            if(response.isSuccessful){
                swipeToRefresh.isRefreshing = false
                val data = Gson().fromJson(response.body?.string(),Response::class.java)
                launch(Dispatchers.Main){
                    bindCombinedData(data.statewise?.get(0))
                    bindStateWiseData(data.statewise?.size?.let { data.statewise.subList(0, it) } as List<StatewiseItem>)
                }
            }
            else{
                confirmedTv.text="error"
            }
        }

    }

    private fun bindStateWiseData(subList: List<StatewiseItem>) {
        stateAdapter = StateAdapter(subList)
        list.adapter = stateAdapter
    }

    @SuppressLint("SimpleDateFormat", "SetTextI18n")
    private fun bindCombinedData(data: StatewiseItem?) {

        val lastUpdatedTime = data?.lastupdatedtime
        val simpleDateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss")
        lastUpdatedTv.text = "Last Updated\n ${getTimeAgo(
            simpleDateFormat.parse(lastUpdatedTime)
        )}"

        confirmedTv.text = data?.confirmed
        activeTv.text = data?.active
        recoveredTv.text = data?.recovered
        deceasedTv.text = data?.deaths

    }

    fun getTimeAgo(past: Date): String {
        val now = Date()
        val seconds = TimeUnit.MILLISECONDS.toSeconds(now.time - past.time)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(now.time - past.time)
        val hours = TimeUnit.MILLISECONDS.toHours(now.time - past.time)

        return when {
            seconds < 60 -> {
                "Few seconds ago"
            }
            minutes < 60 -> {
                "$minutes minutes ago"
            }
            hours < 24 -> {
                "$hours hour ${minutes % 60} min ago"
            }
            else -> {
                SimpleDateFormat("dd/MM/yy, hh:mm a").format(past).toString()
            }
        }
    }
}
