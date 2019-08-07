package info.nightscout.androidaps.database.transactions

import info.nightscout.androidaps.database.DelegatedAppDatabase

abstract class Transaction<T> {

    internal abstract fun run(): T

    internal lateinit var database: DelegatedAppDatabase

}