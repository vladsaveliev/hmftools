package com.hartwig.hmftools.knowledgebaseimporter

import com.google.common.annotations.VisibleForTesting
import com.hartwig.hmftools.knowledgebaseimporter.output.FusionEvent
import com.hartwig.hmftools.knowledgebaseimporter.output.FusionPair
import com.hartwig.hmftools.knowledgebaseimporter.output.PromiscuousGene

private const val GENE_PATTERN = "[A-Za-z0-9-]"
private const val GENE_GROUP = "($GENE_PATTERN+)"

data class FusionReader(private val separators: Set<String> = setOf(), private val filterSet: Set<FusionEvent> = setOf(),
                        private val flipSet: Set<FusionPair> = setOf()) {

    companion object {
        @VisibleForTesting
        fun extractFusion(gene: String, fusionString: String, separators: Set<String>): FusionEvent {
            return separators.map { extractFusion(gene, fusionString, it) }
                    .sortedBy {
                        when (it) {
                            is FusionPair      -> 0
                            is PromiscuousGene -> 1
                        }
                    }
                    .first()
        }

        @VisibleForTesting
        fun extractFusion(gene: String, fusionString: String, separator: String): FusionEvent {
            val fiveGene = fiveGene(gene, fusionString, separator)
            val threeGene = threeGene(gene, fusionString, separator)
            return if (fiveGene == null || threeGene == null) {
                PromiscuousGene(gene)
            } else {
                FusionPair(fiveGene, threeGene)
            }
        }

        @VisibleForTesting
        fun fiveGene(gene: String, fusion: String, separator: String): String? {
            return if (isFiveGene(gene, fusion, separator)) gene else extractFiveGene(gene, fusion, separator)
        }

        @VisibleForTesting
        fun threeGene(gene: String, fusion: String, separator: String): String? {
            return if (isThreeGene(gene, fusion, separator)) gene else extractThreeGene(gene, fusion, separator)
        }

        @VisibleForTesting
        fun extractFiveGene(gene: String, fusion: String, separator: String): String? {
            return extractGene(fusion, "$GENE_GROUP${Regex.escape(separator)}${geneStartLetters(gene)}")
        }

        @VisibleForTesting
        fun extractThreeGene(gene: String, fusion: String, separator: String): String? {
            return extractGene(fusion, "${geneStartLetters(gene)}$GENE_PATTERN*${Regex.escape(separator)}$GENE_GROUP")
        }

        private fun extractGene(fusion: String, patternString: String): String? {
            val matchResult = patternString.toRegex().find(fusion)
            return matchResult?.groupValues?.get(1)
        }

        @VisibleForTesting
        fun isFiveGene(gene: String, fusion: String, separator: String): Boolean {
            return !isThreeGene(gene, fusion, separator) && fusion.contains(geneStartLetters(gene))
        }

        @VisibleForTesting
        fun isThreeGene(gene: String, fusion: String, separator: String): Boolean {
            return fusion.contains("$separator${geneStartLetters(gene)}")
        }

        private fun geneStartLetters(gene: String) = if (gene.length < 3) gene else gene.substring(0, 3)

        private fun flipFusion(fusion: FusionEvent, pairsToFlip: Set<FusionPair>): FusionEvent {
            return if (fusion is FusionPair && pairsToFlip.contains(fusion)) {
                FusionPair(fusion.threeGene, fusion.fiveGene)
            } else {
                fusion
            }
        }
    }

    fun read(gene: String, fusionString: String): FusionEvent? {
        val fusion = extractFusion(gene.trim(), fusionString.trim(), separators)
        return if (filterSet.contains(fusion)) null else flipFusion(fusion, flipSet)
    }
}
