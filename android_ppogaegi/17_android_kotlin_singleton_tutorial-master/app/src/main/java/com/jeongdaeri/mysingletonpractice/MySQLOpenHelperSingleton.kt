package com.jeongdaeri.mysingletonpractice

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class MySQLOpenHelperSingleton private constructor(context: Context): SQLiteOpenHelper(context, "MyDB", null, 1) {

    val TAG: String = "로그"

    companion object {

        // 자기 자신 변수선언
        @Volatile private var instance: MySQLOpenHelperSingleton? = null

        // 자기 자신 가져오기
        fun getInstance(context: Context): MySQLOpenHelperSingleton =
            instance ?: synchronized(this) {
                instance ?: MySQLOpenHelperSingleton(context).also {
                    instance = it
                }
            }

    }


    override fun onCreate(p0: SQLiteDatabase?) {

    }

    override fun onUpgrade(p0: SQLiteDatabase?, p1: Int, p2: Int) {

    }

}
