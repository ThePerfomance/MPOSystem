package com.example.groupprojectfirsttry

import android.util.Log
import com.example.groupprojectfirsttry.KMeans.Point
import com.example.groupprojectfirsttry.simpleClasses.StudentData
import kotlin.math.pow
import kotlin.math.sqrt

object KMeans {

    private const val K = 5 // количество кластеров: S, A, B, C, D

    data class Point(val features: List<Double>, var clusterId: Int = -1)

    internal fun euclideanDistance(p1: Point, p2: Point): Double {
        return sqrt(p1.features.zip(p2.features).sumOf { (a, b) -> (a - b).pow(2) })
    }

    // Преобразуем данные в точки с весами
    private fun applyWeights(student: StudentData): Point {
        val weights = listOf(0.6, 0.1, 0.2, 0.05, 0.05)
        val features = listOf(
            student.accuracy * weights[0],
            student.attempts.toDouble() * weights[1],
            student.timeSpent * weights[2],
            student.testCount.toDouble() * weights[3],
            student.weightedDifficulty * weights[4]
        )
        return Point(features)
    }

    fun classifyStudents(allStudents: List<StudentData>): Pair<Map<StudentData, String>, List<Point>> {
        val normalizedData = normalizeData(allStudents)

        val points = normalizedData.map { student ->
            Point(listOf(
                student.accuracy.toDouble(),
                student.attempts.toDouble(),
                student.timeSpent.toDouble(),
                student.testCount.toDouble(),
                student.weightedDifficulty.toDouble()
            ))
        }

        var centroids = initializeCentroids(points)

        var changedAssignments = true
        while (changedAssignments) {
            for (point in points) {
                var minDist = Double.MAX_VALUE
                var bestCluster = -1
                for ((clusterId, centroid) in centroids.withIndex()) {
                    val dist = euclideanDistance(point, centroid)
                    if (dist < minDist) {
                        minDist = dist
                        bestCluster = clusterId
                    }
                }
                point.clusterId = bestCluster
            }

            val newCentroids = centroids.indices.mapNotNull { clusterId ->
                val assignedPoints = points.filter { it.clusterId == clusterId }
                if (assignedPoints.isEmpty()) null else {
                    val newFeatures = centroids[0].features.indices.map { i ->
                        assignedPoints.map { p -> p.features[i] }.average().toDouble()
                    }
                    Point(newFeatures)
                }
            }

            changedAssignments = centroids.zip(newCentroids).any { (oldP, newP) ->
                euclideanDistance(oldP, newP) > 0.001
            }

            centroids = newCentroids
        }

        val clusterLabels = listOf("S", "A", "B", "C", "D")
        val rankedMap = points.zip(allStudents).associate { (point, student) ->
            val initialRank = clusterLabels.getOrElse(point.clusterId) { "X" }
            val finalRank = if (initialRank == "S" && student.accuracy < 8) {
                assignRankBasedOnAccuracy(student.accuracy)
            } else {
                initialRank
            }
            student to finalRank
        }

        return Pair(rankedMap, points)
    }

    private fun normalizeData(students: List<StudentData>): List<StudentData> {
        val maxAccuracy = students.maxOfOrNull { it.accuracy } ?: 10.0
        val maxAttempts = students.maxOfOrNull { it.attempts.toDouble() } ?: 1.0
        val minTimeSpent = students.minOfOrNull { it.timeSpent } ?: 1.0
        val maxTestCount = students.maxOfOrNull { it.testCount.toDouble() } ?: 1.0
        val maxWeightedDifficulty = students.maxOfOrNull { it.weightedDifficulty } ?: 1.0

        return students.map { student ->
            StudentData(
                accuracy = student.accuracy / maxAccuracy,
                attempts = (student.attempts.toDouble() / maxAttempts).toInt(),
                timeSpent = minTimeSpent / student.timeSpent,
                testCount = (student.testCount.toDouble() / maxTestCount).toInt(),
                weightedDifficulty = student.weightedDifficulty / maxWeightedDifficulty
            )
        }
    }

    private fun initializeCentroids(data: List<Point>): List<Point> {
        val centroids = mutableListOf<Point>()
        val random = kotlin.random.Random.Default
        centroids.add(data[random.nextInt(data.size)])
        for (i in 1 until K) {
            val distances = data.map { p ->
                centroids.minOfOrNull { c -> euclideanDistance(p, c) } ?: 0.0
            }
            val sum = distances.sum()
            val probabilities = distances.map { it / sum }
            val threshold = random.nextDouble()
            var cumulative = 0.0
            var selectedIndex = -1
            for ((index, prob) in probabilities.withIndex()) {
                cumulative += prob
                if (cumulative >= threshold) {
                    selectedIndex = index
                    break
                }
            }
            if (selectedIndex != -1) {
                centroids.add(data[selectedIndex])
            } else {
                centroids.add(data.getOrNull(random.nextInt(data.size)) ?: data.first())
            }
        }
        return centroids
    }

    private fun assignRankBasedOnAccuracy(accuracy: Double): String {
       return when {
            accuracy >= 8 -> "S"
            accuracy >= 6 -> "A"
            accuracy >= 4 -> "B"
            accuracy >= 2 -> "C"
            else -> "D"
        }
    }

    // --- Silhouette Score ---

    fun calculateSilhouetteScore(points: List<Point>): Double {
        if (points.size < 2) {
            Log.d("KMeans", "Недостаточно точек для расчёта Silhouette Score")
            return 0.0
        }

        if (points.all { it.clusterId == points.first().clusterId }) {
            Log.d("KMeans", "Все точки в одном кластере → Silhouette Score = 0")
            return 0.0
        }

        val clusters = points.groupBy { it.clusterId }

        val scores = points.map { point ->
            val clusterPoints = clusters[point.clusterId] ?: return@map 0.0
            val a = meanIntraClusterDistance(point, clusterPoints)
            val b = minInterClusterDistance(point, clusters)
            when {
                a == 0.0 && b == 0.0 -> 0.0
                a >= b -> -((a - b) / a)
                else -> (b - a) / b
            }
        }
        Log.d("KMeans", "Всего точек: ${points.size}")
        Log.d("KMeans", "Количество кластеров: ${points.map { it.clusterId }.distinct().size}")

        points.forEachIndexed { i, point ->
            val clusterPoints = clusters[point.clusterId] ?: emptyList()
            val a = meanIntraClusterDistance(point, clusterPoints)
            val b = minInterClusterDistance(point, clusters)

            Log.d("KMeans", "Point $i | a = $a | b = $b | s = ${(b - a) / maxOf(a, b)}")
        }
        return scores.takeIf { it.isNotEmpty() }?.average() ?: 0.0
    }

    private fun meanIntraClusterDistance(point: Point, clusterPoints: List<Point>): Double {
        val others = clusterPoints.filter { it !== point }
        return if (others.isEmpty()) 0.0 else others.map { euclideanDistance(point, it) }.average()
    }

    private fun minInterClusterDistance(point: Point, clusters: Map<Int, List<Point>>): Double {
        val currentClusterId = point.clusterId
        val distances = clusters.entries.filter { it.key != currentClusterId }
            .map { (_, clusterPoints) ->
                if (clusterPoints.isEmpty()) Double.POSITIVE_INFINITY
                else point.getCentroidDistanceToCluster(clusterPoints)
            }
        return distances.minOrNull() ?: Double.POSITIVE_INFINITY
    }

    fun printSilhouettePlot(points: List<Point>) {
        val clusters = points.groupBy { it.clusterId }
        val scores = points.map { point ->
            val clusterPoints = clusters[point.clusterId] ?: return@map 0.0
            val a = meanIntraClusterDistance(point, clusterPoints)
            val b = minInterClusterDistance(point, clusters)
            when {
                a == 0.0 && b == 0.0 -> 0.0
                a >= b -> -((a - b) / a)
                else -> (b - a) / b
            }
        }

        Log.d("KMeans", "\n--- Silhouette Plot ---")
        for ((index, score) in scores.withIndex()) {
            val barLength = (score * 20).toInt() + 20
            val bar = "-".repeat(barLength.coerceIn(0..40))
            Log.d("KMeans", "Point $index |$bar| $score")
        }
    }
}

// Extension-функции
fun Point.getCentroidDistanceToCluster(cluster: List<Point>): Double {
    val centroid = cluster.getCentroid()
    return KMeans.euclideanDistance(this, centroid)
}

fun List<KMeans.Point>.getCentroid(): KMeans.Point {
    val featureCount = first().features.size
    val centroidFeatures = (0 until featureCount).map { i ->
        this.map { it.features[i] }.average()
    }
    return KMeans.Point(centroidFeatures)
}