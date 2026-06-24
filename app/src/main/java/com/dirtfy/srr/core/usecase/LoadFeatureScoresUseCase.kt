package com.dirtfy.srr.core.usecase

import com.dirtfy.srr.core.model.Evaluation
import com.dirtfy.srr.core.model.Feature
import com.dirtfy.srr.core.model.Item
import com.dirtfy.srr.core.model.ScoreMatrix
import com.dirtfy.srr.core.repository.EvaluationRepository
import com.dirtfy.srr.core.repository.FeatureRepository
import com.dirtfy.srr.core.repository.ItemRepository
import com.dirtfy.srr.core.scoring.DefaultFeatureScoringEngine
import com.dirtfy.srr.core.scoring.FeatureScoringEngine
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

class LoadFeatureScoresUseCase(
    private val itemRepository: ItemRepository,
    private val featureRepository: FeatureRepository,
    private val evaluationRepository: EvaluationRepository,
    private val scoringEngine: FeatureScoringEngine = DefaultFeatureScoringEngine(),
    private val minVoteThreshold: Int = 3
) {
    data class Output(
        val items: List<Item>,
        val features: List<Feature>,
        val scoreMatrix: ScoreMatrix,
        // Raw evaluations kept here so ViewModels can pre-populate the evaluation editor
        // without an extra Firestore round-trip. ScoreMatrix stores only computed scores.
        val evaluationsByFeature: Map<String, List<Evaluation>>
    )

    suspend fun execute(): Result<Output> = runCatching {
        val items    = itemRepository.getAllItems().getOrThrow()
        val features = featureRepository.getAllFeatures().getOrThrow()

        // Parallel Firestore reads — one per feature (Debator fix #2)
        val evaluationsByFeature: Map<String, List<Evaluation>> = coroutineScope {
            features.associate { feature ->
                feature.id to async {
                    evaluationRepository.getEvaluationsForFeature(feature.id).getOrThrow()
                }
            }.mapValues { (_, deferred) -> deferred.await() }
        }

        val scoreMatrix = scoringEngine.computeScores(
            allItemIds           = items.map { it.id },
            allFeatureIds        = features.map { it.id },
            evaluationsByFeature = evaluationsByFeature,
            minVoteThreshold     = minVoteThreshold
        )

        Output(items, features, scoreMatrix, evaluationsByFeature)
    }
}
