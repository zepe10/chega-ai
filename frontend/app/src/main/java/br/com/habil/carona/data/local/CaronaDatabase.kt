package br.com.habil.carona.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import br.com.habil.carona.data.local.dao.ContactDao
import br.com.habil.carona.data.local.dao.MessageDao
import br.com.habil.carona.data.local.dao.RideDao
import br.com.habil.carona.data.local.dao.RideRequestDao
import br.com.habil.carona.data.local.dao.UserDao
import br.com.habil.carona.data.local.entity.ContactEntity
import br.com.habil.carona.data.local.entity.MessageEntity
import br.com.habil.carona.data.local.entity.RideEntity
import br.com.habil.carona.data.local.entity.RideRequestEntity
import br.com.habil.carona.data.local.entity.UserEntity

@Database(
    entities = [
        UserEntity::class,
        RideEntity::class,
        RideRequestEntity::class,
        MessageEntity::class,
        ContactEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class CaronaDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun rideDao(): RideDao
    abstract fun rideRequestDao(): RideRequestDao
    abstract fun messageDao(): MessageDao
    abstract fun contactDao(): ContactDao

    companion object {
        fun create(context: Context): CaronaDatabase =
            Room.databaseBuilder(context.applicationContext, CaronaDatabase::class.java, "carona.db")
                .fallbackToDestructiveMigration()
                .fallbackToDestructiveMigrationOnDowngrade()
                .build()
    }
}
