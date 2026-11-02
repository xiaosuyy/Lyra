package com.lyra.player.core.download

import android.content.Context
import com.kyant.taglib.PropertyMap
import com.lyra.player.core.download.catalog.buildDownloadedSongCatalogIndex as buildDownloadedSongCatalogIndexDelegate
import com.lyra.player.core.download.catalog.findDownloadedSongCatalogMatch as findDownloadedSongCatalogMatchDelegate
import com.lyra.player.core.download.catalog.matchesDownloadedSong as matchesDownloadedSongDelegate
import com.lyra.player.core.download.catalog.matchesDownloadedSongCatalogEntry as matchesDownloadedSongCatalogEntryDelegate
import com.lyra.player.core.download.catalog.resolveDownloadedSongPlaybackReference as resolveDownloadedSongPlaybackReferenceDelegate
import com.lyra.player.core.download.catalog.deserializeDownloadedSongsCatalog as deserializeDownloadedSongsCatalogDelegate
import com.lyra.player.core.download.catalog.serializeDownloadedSongsCatalog as serializeDownloadedSongsCatalogDelegate
import com.lyra.player.core.download.catalog.shouldPublishDownloadedSongCatalogUpdate as shouldPublishDownloadedSongCatalogUpdateDelegate
import com.lyra.player.core.download.catalog.upsertDownloadedSongCatalog as upsertDownloadedSongCatalogDelegate
import com.lyra.player.core.download.cleanup.ManagedDownloadArtifactPlanner as ManagedDownloadArtifactPlannerDelegate
import com.lyra.player.core.download.cleanup.groupRemainingManagedReferencesByIdentity as groupRemainingManagedReferencesByIdentityDelegate
import com.lyra.player.core.download.cleanup.mergeManagedRequestedReferences as mergeManagedRequestedReferencesDelegate
import com.lyra.player.core.download.cleanup.resolveUndeletedManagedReferences as resolveUndeletedManagedReferencesDelegate
import com.lyra.player.core.download.metadata.DownloadedAudioTagWriteOutcome
import com.lyra.player.core.download.metadata.DownloadedAudioTagWriter as DownloadedAudioTagWriterDelegate
import com.lyra.player.core.download.naming.candidateManagedDownloadBaseNames as candidateManagedDownloadBaseNamesDelegate
import com.lyra.player.core.download.naming.candidateManagedDownloadFileNameTemplates as candidateManagedDownloadFileNameTemplatesDelegate
import com.lyra.player.core.download.naming.normalizeDownloadFileNameTemplate as normalizeDownloadFileNameTemplateDelegate
import com.lyra.player.core.download.naming.parseManagedDownloadBaseName as parseManagedDownloadBaseNameDelegate
import com.lyra.player.core.download.naming.renderManagedDownloadBaseName as renderManagedDownloadBaseNameDelegate
import com.lyra.player.core.download.naming.sanitizeManagedDownloadFileName as sanitizeManagedDownloadFileNameDelegate
import com.lyra.player.core.download.policy.buildExpectedDownloadArtists as buildExpectedDownloadArtistsDelegate
import com.lyra.player.core.download.policy.buildExpectedDownloadTitles as buildExpectedDownloadTitlesDelegate
import com.lyra.player.core.download.policy.isSuspiciousEmptyDownloadScan as isSuspiciousEmptyDownloadScanDelegate
import com.lyra.player.core.download.policy.isUnfinalizedDownloadedMetadata as isUnfinalizedDownloadedMetadataDelegate
import com.lyra.player.core.download.policy.resolveCompletedDownloadFinalizationAction as resolveCompletedDownloadFinalizationActionDelegate
import com.lyra.player.core.download.policy.resolveDownloadedLyricContent as resolveDownloadedLyricContentDelegate
import com.lyra.player.core.download.policy.resolveDownloadedLyricOverride as resolveDownloadedLyricOverrideDelegate
import com.lyra.player.core.download.policy.resolveDownloadedPlaybackHydrationDelayMs as resolveDownloadedPlaybackHydrationDelayMsDelegate
import com.lyra.player.core.download.policy.resolvePreExistingDownloadedAudioAction as resolvePreExistingDownloadedAudioActionDelegate
import com.lyra.player.core.download.policy.runNonCancellableDownloadRollback as runNonCancellableDownloadRollbackDelegate
import com.lyra.player.core.download.policy.shouldRepairMetadataLessManagedDownload as shouldRepairMetadataLessManagedDownloadDelegate
import com.lyra.player.core.download.policy.shouldDeferPendingDownloadRecoveryForNetwork as shouldDeferPendingDownloadRecoveryForNetworkDelegate
import com.lyra.player.core.download.policy.shouldDeferPreparedDownloadStartForNetwork as shouldDeferPreparedDownloadStartForNetworkDelegate
import com.lyra.player.core.download.policy.shouldDeferStartupManagedCleanup as shouldDeferStartupManagedCleanupDelegate
import com.lyra.player.core.download.policy.shouldInspectDownloadedAudioDetails as shouldInspectDownloadedAudioDetailsDelegate
import com.lyra.player.core.download.policy.shouldKeepCancellationCleanup as shouldKeepCancellationCleanupDelegate
import com.lyra.player.core.download.policy.shouldProbeCompletedAudioAccessDuringPostProcessing as shouldProbeCompletedAudioAccessDuringPostProcessingDelegate
import com.lyra.player.core.download.policy.shouldRunInitialDownloadScan as shouldRunInitialDownloadScanDelegate
import com.lyra.player.core.download.policy.shouldSkipCancelledArtifactRecovery as shouldSkipCancelledArtifactRecoveryDelegate
import com.lyra.player.core.download.policy.shouldTrustFastDownloadedSongCatalogHit as shouldTrustFastDownloadedSongCatalogHitDelegate
import com.lyra.player.core.download.policy.shouldUseImmediateDownloadedPlaybackHydration as shouldUseImmediateDownloadedPlaybackHydrationDelegate
import com.lyra.player.core.download.policy.shouldUseIndexedSidecarLookup as shouldUseIndexedSidecarLookupDelegate
import com.lyra.player.core.player.download.AudioDownloadManager
import com.lyra.player.data.traffic.TrafficNetworkType
import com.lyra.player.data.model.SongItem

internal typealias DownloadedSongCatalogIndex = com.lyra.player.core.download.catalog.DownloadedSongCatalogIndex
internal typealias DownloadedSongCatalogStore = com.lyra.player.core.download.catalog.DownloadedSongCatalogStore
internal typealias DownloadedSongBuilder = com.lyra.player.core.download.catalog.DownloadedSongBuilder
internal typealias ManagedDownloadArtifactRemovalResult =
    com.lyra.player.core.download.cleanup.ManagedDownloadArtifactRemovalResult
internal typealias ManagedDownloadSongDeletePlan = com.lyra.player.core.download.cleanup.ManagedDownloadSongDeletePlan
internal typealias ManagedDownloadDeletePlanner = com.lyra.player.core.download.cleanup.ManagedDownloadDeletePlanner
internal typealias DownloadRequestGenerationTracker =
    com.lyra.player.core.download.generation.DownloadRequestGenerationTracker
internal typealias DownloadRequestGenerationSnapshot =
    com.lyra.player.core.download.generation.DownloadRequestGenerationSnapshot
internal typealias DownloadedAudioMetadataStore =
    com.lyra.player.core.download.metadata.DownloadedAudioMetadataStore
internal typealias ParsedManagedDownloadFileName =
    com.lyra.player.core.download.naming.ParsedManagedDownloadFileName
internal typealias CompletedDownloadFinalizationAction =
    com.lyra.player.core.download.policy.CompletedDownloadFinalizationAction
internal typealias PreExistingDownloadedAudioAction =
    com.lyra.player.core.download.policy.PreExistingDownloadedAudioAction
internal typealias PendingDownloadRecoveryCandidate =
    com.lyra.player.core.download.policy.PendingDownloadRecoveryCandidate
internal typealias DownloadTaskStore = com.lyra.player.core.download.task.DownloadTaskStore

internal const val DEFAULT_DOWNLOAD_FILE_NAME_TEMPLATE =
    com.lyra.player.core.download.naming.DEFAULT_DOWNLOAD_FILE_NAME_TEMPLATE
internal const val LEGACY_DOWNLOAD_FILE_NAME_TEMPLATE =
    com.lyra.player.core.download.naming.LEGACY_DOWNLOAD_FILE_NAME_TEMPLATE

internal fun sanitizeManagedDownloadFileName(name: String): String =
    sanitizeManagedDownloadFileNameDelegate(name)

internal fun normalizeDownloadFileNameTemplate(template: String?): String? =
    normalizeDownloadFileNameTemplateDelegate(template)

internal fun candidateManagedDownloadFileNameTemplates(activeTemplate: String? = null): List<String> =
    candidateManagedDownloadFileNameTemplatesDelegate(activeTemplate)

internal fun renderManagedDownloadBaseName(
    title: String,
    artist: String,
    album: String,
    source: String = "",
    songId: String = "",
    audioId: String = "",
    subAudioId: String = "",
    template: String? = DEFAULT_DOWNLOAD_FILE_NAME_TEMPLATE
): String = renderManagedDownloadBaseNameDelegate(title, artist, album, source, songId, audioId, subAudioId, template)

internal fun renderManagedDownloadBaseName(
    song: SongItem,
    template: String? = DEFAULT_DOWNLOAD_FILE_NAME_TEMPLATE
): String = renderManagedDownloadBaseNameDelegate(song, template)

internal fun parseManagedDownloadBaseName(
    baseName: String,
    template: String? = DEFAULT_DOWNLOAD_FILE_NAME_TEMPLATE
): ParsedManagedDownloadFileName? = parseManagedDownloadBaseNameDelegate(baseName, template)

internal fun candidateManagedDownloadBaseNames(
    song: SongItem,
    activeTemplate: String? = null
): List<String> = candidateManagedDownloadBaseNamesDelegate(song, activeTemplate)

internal fun candidateManagedDownloadBaseNames(fileNameWithoutExtension: String): List<String> =
    candidateManagedDownloadBaseNamesDelegate(fileNameWithoutExtension)

internal fun shouldRunInitialDownloadScan(catalogReady: Boolean, hasRecoveredEntries: Boolean = false): Boolean =
    shouldRunInitialDownloadScanDelegate(catalogReady, hasRecoveredEntries)

internal fun isSuspiciousEmptyDownloadScan(
    scannedSongCount: Int,
    existingSongCount: Int,
    storageRootResolvable: Boolean,
    scanMatchesCatalogRoot: Boolean
): Boolean = isSuspiciousEmptyDownloadScanDelegate(
    scannedSongCount,
    existingSongCount,
    storageRootResolvable,
    scanMatchesCatalogRoot
)

internal fun shouldDeferStartupManagedCleanup(configuredDirectoryUri: String?, treeRootAvailable: Boolean): Boolean =
    shouldDeferStartupManagedCleanupDelegate(configuredDirectoryUri, treeRootAvailable)

internal fun shouldDeferPendingDownloadRecoveryForNetwork(
    networkType: TrafficNetworkType,
    mobileDataOverrideAllowed: Boolean
): Boolean = shouldDeferPendingDownloadRecoveryForNetworkDelegate(networkType, mobileDataOverrideAllowed)

internal fun shouldDeferPreparedDownloadStartForNetwork(
    networkType: TrafficNetworkType,
    mobileDataOverrideAllowed: Boolean,
    deferForNetworkPolicy: Boolean
): Boolean = shouldDeferPreparedDownloadStartForNetworkDelegate(
    networkType,
    mobileDataOverrideAllowed,
    deferForNetworkPolicy
)

internal fun shouldKeepCancellationCleanup(
    currentGeneration: Long?,
    cancellationGeneration: Long?,
    cancelled: Boolean
): Boolean = shouldKeepCancellationCleanupDelegate(currentGeneration, cancellationGeneration, cancelled)

internal fun resolveCompletedDownloadFinalizationAction(
    hasStoredAudio: Boolean,
    cancelled: Boolean
): CompletedDownloadFinalizationAction =
    resolveCompletedDownloadFinalizationActionDelegate(hasStoredAudio, cancelled)

internal fun resolvePreExistingDownloadedAudioAction(hasExistingAudio: Boolean): PreExistingDownloadedAudioAction =
    resolvePreExistingDownloadedAudioActionDelegate(hasExistingAudio)

internal fun shouldUseImmediateDownloadedPlaybackHydration(
    originalSong: SongItem,
    hydratedSong: SongItem
): Boolean = shouldUseImmediateDownloadedPlaybackHydrationDelegate(originalSong, hydratedSong)

internal fun resolveDownloadedPlaybackHydrationDelayMs(
    originalSong: SongItem,
    hydratedSong: SongItem
): Long = resolveDownloadedPlaybackHydrationDelayMsDelegate(originalSong, hydratedSong)

internal suspend fun <T> runNonCancellableDownloadRollback(block: suspend () -> T): T =
    runNonCancellableDownloadRollbackDelegate(block)

internal fun shouldInspectDownloadedAudioDetails(
    allowSlowLocalInspection: Boolean,
    metadata: ManagedDownloadStorage.DownloadedAudioMetadata?,
    coverReference: String?,
    needsLocalLyricFallback: Boolean
): Boolean = shouldInspectDownloadedAudioDetailsDelegate(
    allowSlowLocalInspection,
    metadata,
    coverReference,
    needsLocalLyricFallback
)

internal fun isUnfinalizedDownloadedMetadata(metadata: ManagedDownloadStorage.DownloadedAudioMetadata?): Boolean =
    isUnfinalizedDownloadedMetadataDelegate(metadata)

internal fun resolveDownloadedLyricOverride(
    fileLyric: String?,
    embeddedMatchedLyric: String?,
    embeddedOriginalLyric: String?,
    localLyricContent: String?,
    indexedLyricContent: String?
): String? = resolveDownloadedLyricOverrideDelegate(
    fileLyric,
    embeddedMatchedLyric,
    embeddedOriginalLyric,
    localLyricContent,
    indexedLyricContent
)

internal fun resolveDownloadedLyricContent(
    fileLyric: String?,
    embeddedMatchedLyric: String?,
    embeddedOriginalLyric: String?,
    localLyricContent: String?,
    indexedLyricContent: String?
): String? = resolveDownloadedLyricContentDelegate(
    fileLyric,
    embeddedMatchedLyric,
    embeddedOriginalLyric,
    localLyricContent,
    indexedLyricContent
)

internal fun shouldTrustFastDownloadedSongCatalogHit(reference: String?, cachedKnownReferences: Set<String>?): Boolean =
    shouldTrustFastDownloadedSongCatalogHitDelegate(reference, cachedKnownReferences)

internal fun shouldProbeCompletedAudioAccessDuringPostProcessing(
    reference: String?,
    fastPathTrusted: Boolean
): Boolean = shouldProbeCompletedAudioAccessDuringPostProcessingDelegate(reference, fastPathTrusted)

internal fun shouldUseIndexedSidecarLookup(
    usesDocumentTree: Boolean,
    allowSlowLookup: Boolean
): Boolean = shouldUseIndexedSidecarLookupDelegate(usesDocumentTree, allowSlowLookup)

internal fun shouldSkipCancelledArtifactRecovery(
    downloadActive: Boolean,
    taskStatus: DownloadStatus?
): Boolean = shouldSkipCancelledArtifactRecoveryDelegate(downloadActive, taskStatus)

internal fun shouldRepairMetadataLessManagedDownload(
    expectedTitles: Collection<String>,
    expectedArtists: Collection<String>,
    expectedDurationMs: Long,
    actualTitle: String?,
    actualArtist: String?,
    actualDurationMs: Long
): Boolean = shouldRepairMetadataLessManagedDownloadDelegate(
    expectedTitles,
    expectedArtists,
    expectedDurationMs,
    actualTitle,
    actualArtist,
    actualDurationMs
)

internal fun buildExpectedDownloadTitles(song: SongItem): Set<String> = buildExpectedDownloadTitlesDelegate(song)

internal fun buildExpectedDownloadArtists(song: SongItem): Set<String> = buildExpectedDownloadArtistsDelegate(song)

internal fun buildDownloadedSongCatalogIndex(songs: List<DownloadedSong>): DownloadedSongCatalogIndex =
    buildDownloadedSongCatalogIndexDelegate(songs)

internal fun matchesDownloadedSongCatalogEntry(existing: DownloadedSong, target: DownloadedSong): Boolean =
    matchesDownloadedSongCatalogEntryDelegate(existing, target)

internal fun matchesDownloadedSong(song: SongItem, downloadedSong: DownloadedSong): Boolean =
    matchesDownloadedSongDelegate(song, downloadedSong)

internal fun findDownloadedSongCatalogMatch(
    song: SongItem,
    downloadedSongs: List<DownloadedSong>
): DownloadedSong? = findDownloadedSongCatalogMatchDelegate(song, downloadedSongs)

internal fun resolveDownloadedSongPlaybackReference(song: DownloadedSong): String? =
    resolveDownloadedSongPlaybackReferenceDelegate(song)

internal fun shouldPublishDownloadedSongCatalogUpdate(
    currentSong: DownloadedSong,
    updatedSong: DownloadedSong
): Boolean = shouldPublishDownloadedSongCatalogUpdateDelegate(currentSong, updatedSong)

internal fun serializeDownloadedSongsCatalog(
    cacheKey: String,
    songs: List<DownloadedSong>
): String = serializeDownloadedSongsCatalogDelegate(cacheKey, songs)

internal fun deserializeDownloadedSongsCatalog(
    raw: String,
    expectedCacheKey: String
): List<DownloadedSong>? = deserializeDownloadedSongsCatalogDelegate(raw, expectedCacheKey)

fun upsertDownloadedSongCatalog(currentSongs: List<DownloadedSong>, updatedSong: DownloadedSong): List<DownloadedSong> =
    upsertDownloadedSongCatalogDelegate(currentSongs, updatedSong)

internal fun mergePendingDownloadRecoveryCandidates(
    queuedDownloads: List<ManagedDownloadStorage.PendingDownloadQueueEntry>,
    resumableDownloads: List<ManagedDownloadStorage.PendingResumableDownload>,
    cancelledKeys: Set<String> = emptySet()
): List<PendingDownloadRecoveryCandidate> =
    com.lyra.player.core.download.policy.mergePendingDownloadRecoveryCandidates(
        queuedDownloads,
        resumableDownloads,
        cancelledKeys
    )

internal fun mergeManagedRequestedReferences(
    requestedReferenceGroups: Collection<Set<String>>
): Set<String> = mergeManagedRequestedReferencesDelegate(requestedReferenceGroups)

internal fun groupRemainingManagedReferencesByIdentity(
    requestedReferencesByIdentity: Map<String, Set<String>>,
    remainingReferences: Set<String>
): Map<String, Set<String>> =
    groupRemainingManagedReferencesByIdentityDelegate(requestedReferencesByIdentity, remainingReferences)

internal suspend fun resolveUndeletedManagedReferences(
    requestedReferences: Set<String>,
    deletedReferences: Set<String>,
    exists: suspend (String) -> Boolean
): Set<String> = resolveUndeletedManagedReferencesDelegate(requestedReferences, deletedReferences, exists)

internal object ManagedDownloadArtifactPlanner {
    fun collectArtifactReferences(
        snapshot: ManagedDownloadStorage.DownloadLibrarySnapshot,
        storedAudio: ManagedDownloadStorage.StoredEntry?,
        songId: Long,
        candidateBaseNames: List<String>,
        explicitReferences: List<String> = emptyList(),
        deletingAudioNames: Set<String> = emptySet()
    ): Set<String> = ManagedDownloadArtifactPlannerDelegate.collectArtifactReferences(
        snapshot,
        storedAudio,
        songId,
        candidateBaseNames,
        explicitReferences,
        deletingAudioNames
    )

    fun trustedMetadataReference(
        reference: String?,
        snapshot: ManagedDownloadStorage.DownloadLibrarySnapshot
    ): String? = ManagedDownloadArtifactPlannerDelegate.trustedMetadataReference(reference, snapshot)

    fun indexedLyricReference(
        audio: ManagedDownloadStorage.StoredEntry,
        songId: Long?,
        translated: Boolean,
        snapshot: ManagedDownloadStorage.DownloadLibrarySnapshot
    ): String? = ManagedDownloadArtifactPlannerDelegate.indexedLyricReference(audio, songId, translated, snapshot)

    suspend fun indexedLyricText(
        context: Context,
        audio: ManagedDownloadStorage.StoredEntry,
        songId: Long?,
        translated: Boolean,
        snapshot: ManagedDownloadStorage.DownloadLibrarySnapshot
    ): String? = ManagedDownloadArtifactPlannerDelegate.indexedLyricText(context, audio, songId, translated, snapshot)

    fun indexedRomanizedLyricReference(
        audio: ManagedDownloadStorage.StoredEntry,
        songId: Long?,
        snapshot: ManagedDownloadStorage.DownloadLibrarySnapshot
    ): String? = ManagedDownloadArtifactPlannerDelegate.indexedRomanizedLyricReference(
        audio = audio,
        songId = songId,
        snapshot = snapshot
    )

    fun indexedCoverReference(
        audio: ManagedDownloadStorage.StoredEntry,
        snapshot: ManagedDownloadStorage.DownloadLibrarySnapshot
    ): String? = ManagedDownloadArtifactPlannerDelegate.indexedCoverReference(audio, snapshot)

    fun indexedCoverReference(
        candidateBaseNames: List<String>,
        snapshot: ManagedDownloadStorage.DownloadLibrarySnapshot
    ): String? = ManagedDownloadArtifactPlannerDelegate.indexedCoverReference(candidateBaseNames, snapshot)
}

internal object DownloadedAudioTagWriter {
    suspend fun write(
        context: Context,
        audio: ManagedDownloadStorage.StoredEntry,
        song: SongItem,
        sidecarReferences: AudioDownloadManager.DownloadedSidecarReferences?,
        standardizedLyricEmbeddingEnabled: Boolean
    ): DownloadedAudioTagWriteOutcome = DownloadedAudioTagWriterDelegate.write(
        context,
        audio,
        song,
        sidecarReferences,
        standardizedLyricEmbeddingEnabled
    )

    fun supportsEmbeddedTags(fileName: String): Boolean =
        DownloadedAudioTagWriterDelegate.supportsEmbeddedTags(fileName)

    fun normalizeEmbeddedAlbumName(album: String): String? =
        DownloadedAudioTagWriterDelegate.normalizeEmbeddedAlbumName(album)

    fun normalizeLyricForEmbedding(lyric: String?, enabled: Boolean): String? =
        DownloadedAudioTagWriterDelegate.normalizeLyricForEmbedding(lyric, enabled)

    fun hasRequiredEmbeddedMetadata(propertyMap: PropertyMap, song: SongItem): Boolean =
        DownloadedAudioTagWriterDelegate.hasRequiredEmbeddedMetadata(propertyMap, song)
}
