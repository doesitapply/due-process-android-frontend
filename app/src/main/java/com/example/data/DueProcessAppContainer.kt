package com.example.data

import android.content.Context
import com.example.data.api.AuthTokenStore
import com.example.data.api.DueProcessApiClient
import com.example.data.repository.CaseRepository

class DueProcessAppContainer(context: Context) {
    private val database = AppDatabase.getDatabase(context)
    private val tokenStore = AuthTokenStore(context)
    private val api = DueProcessApiClient.create(tokenStore)

    val repository = CaseRepository(
        caseDao = database.caseDao(),
        documentDao = database.documentDao(),
        findingDao = database.findingDao(),
        reportDao = database.reportDao(),
        api = api,
        tokenStore = tokenStore
    )
}
