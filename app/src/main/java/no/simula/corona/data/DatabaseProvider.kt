package no.simula.corona.data

import android.content.Context
import no.simula.corona.data.greendao.GreenDaoDatabaseImpl

class DatabaseProvider {

    companion object {
        fun open(context: Context): DatabaseContract {
            return GreenDaoDatabaseImpl(context)
        }


        fun deleteDatabase(context: Context): Boolean {
            return DatabaseUtils.deleteNewDatabase(context)
        }

    }
}