package com.zak.pressmark.data.repository.v2

import com.zak.pressmark.core.util.completion.CompletionRules
import com.zak.pressmark.data.local.dao.v2.CanonicalWorkDaoV2
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ArtistCompletionRepository @Inject constructor(
    private val canonicalWorkDao: CanonicalWorkDaoV2,
) {

    data class MissingWork(
        val id: String,
        val title: String,
        val year: Int?,
        val formatType: String?,
    )

    data class ArtistCompletionSummary(
        val ownedCount: Int,
        val totalCount: Int,
        val missing: List<MissingWork>,
    ) {
        val isComplete: Boolean
            get() = totalCount > 0 && ownedCount >= totalCount
    }

    fun observeArtistCompletion(artistId: String): Flow<ArtistCompletionSummary> {
        val formats = CompletionRules.studioFormats
        val releaseType = CompletionRules.RELEASE_TYPE_STUDIO

        val totalFlow = canonicalWorkDao.observeStudioTotalCount(artistId, formats, releaseType)
        val ownedFlow = canonicalWorkDao.observeOwnedStudioCount(artistId, formats, releaseType)
        val missingFlow = canonicalWorkDao.observeMissingStudioWorksForArtist(artistId, formats, releaseType)

        return combine(totalFlow, ownedFlow, missingFlow) { total, owned, missing ->
            ArtistCompletionSummary(
                ownedCount = owned,
                totalCount = total,
                missing = missing.map { work ->
                    MissingWork(
                        id = work.id,
                        title = work.title,
                        year = work.year,
                        formatType = work.formatType,
                    )
                },
            )
        }
    }
}
