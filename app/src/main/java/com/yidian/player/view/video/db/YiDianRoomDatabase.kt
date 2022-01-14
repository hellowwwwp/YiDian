package com.yidian.player.view.video.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * @author: wangpan
 * @email: p.wang@aftership.com
 * @date: 2022/1/15
 */
@Database(entities = [VideoProgressBean::class], version = 1, exportSchema = true)
abstract class YiDianRoomDatabase : RoomDatabase() {

    abstract fun getVideoProgressDao(): VideoProgressDao

    companion object {
        private const val DATABASE_NAME = "yidian_database"

        @Volatile
        private var instance: YiDianRoomDatabase? = null

        fun get(context: Context): YiDianRoomDatabase {
            return instance ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    YiDianRoomDatabase::class.java,
                    DATABASE_NAME
                )
                    .build().also {
                        instance = it
                    }
            }
        }
    }

}