package com.ciclismo.portugal.data.repository

import com.ciclismo.portugal.data.local.dao.ProvaDao
import com.ciclismo.portugal.data.local.entity.toDomain
import com.ciclismo.portugal.data.remote.scraper.BaseScraper
import com.ciclismo.portugal.domain.model.Prova
import com.ciclismo.portugal.domain.repository.ProvaRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProvaRepositoryImpl @Inject constructor(
    private val provaDao: ProvaDao,
    private val scrapers: Set<@JvmSuppressWildcards BaseScraper>
) : ProvaRepository {

    override fun getProvas(): Flow<List<Prova>> {
        val today = getTodayTimestamp()
        return provaDao.getAll(today).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getCalendarProvas(): Flow<List<Prova>> {
        val today = getTodayTimestamp()
        return provaDao.getCalendarProvas(today).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun searchProvas(query: String): Flow<List<Prova>> {
        val today = getTodayTimestamp()
        return provaDao.searchByName(query, today).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getProvaById(id: Long): Prova? =
        provaDao.getById(id)?.toDomain()

    override suspend fun addToCalendar(provaId: Long, add: Boolean) {
        provaDao.getById(provaId)?.let { prova ->
            provaDao.update(prova.copy(inCalendar = add))
        }
    }

    override suspend fun toggleNotifications(provaId: Long, enabled: Boolean) {
        provaDao.getById(provaId)?.let { prova ->
            provaDao.update(prova.copy(notificationsEnabled = enabled))
        }
    }

    override suspend fun syncProvas(): Result<Unit> {
        return try {
            val allProvas = mutableListOf<com.ciclismo.portugal.data.local.entity.ProvaEntity>()

            // Scrape from all sources
            scrapers.forEach { scraper ->
                val result = scraper.scrapeProvas()
                result.getOrNull()?.let { provas ->
                    allProvas.addAll(provas)
                }
            }

            // Deduplica eventos (mesmo nome + mesma data = duplicado)
            val dedupedProvas = allProvas.distinctBy {
                "${it.nome.trim().lowercase()}_${it.data}"
            }

            // Insert all new provas
            if (dedupedProvas.isNotEmpty()) {
                provaDao.insertAll(dedupedProvas)
            }

            // Limpa eventos passados (exceto os que estão no calendário)
            deleteOldProvas()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteOldProvas() {
        val today = getTodayTimestamp()
        // Remove apenas provas passadas que não estão no calendário
        provaDao.deleteOldProvas(today)
    }

    override fun getAvailableTipos(): Flow<List<String>> {
        val today = getTodayTimestamp()
        return provaDao.getDistinctTipos(today)
    }

    override fun getAvailableLocais(): Flow<List<String>> {
        val today = getTodayTimestamp()
        return provaDao.getDistinctLocais(today)
    }

    override fun getAllProvasAdmin(): Flow<List<Prova>> {
        val today = getTodayTimestamp()
        return provaDao.getAllAdmin(today).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun hideProva(provaId: Long) {
        provaDao.setHidden(provaId, true)
    }

    override suspend fun showProva(provaId: Long) {
        provaDao.setHidden(provaId, false)
    }

    override suspend fun deleteProva(provaId: Long) {
        provaDao.deleteById(provaId)
    }

    override suspend fun clearAllProvas() {
        provaDao.deleteAll()
    }

    private fun getTodayTimestamp(): Long {
        val calendar = Calendar.getInstance(java.util.TimeZone.getTimeZone("Europe/Lisbon"))
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }
}
