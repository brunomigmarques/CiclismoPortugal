package com.ciclismo.portugal.data.remote.scraper

import com.ciclismo.portugal.data.local.entity.ProvaEntity

interface BaseScraper {
    suspend fun scrapeProvas(): Result<List<ProvaEntity>>
    val sourceName: String
}
