package info.nightscout.androidaps.database.daos.delegated

import info.nightscout.androidaps.database.interfaces.DBEntry

abstract class DelegatedDao(val changes: MutableList<DBEntry>)