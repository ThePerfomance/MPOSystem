package com.example.groupprojectfirsttry

import android.util.Log
import com.example.groupprojectfirsttry.KMeans.Point
import com.example.groupprojectfirsttry.simpleClasses.StudentData
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.random.Random

object KMeans {

    private const val K = 5 // количество кластеров: S, A, B, C, D
    private val clusterLabels = listOf("S", "A", "B", "C", "D")

    data class Point(val features: List<Double>, var clusterId: Int = -1)

    internal fun euclideanDistance(p1: Point, p2: Point): Double {
        return sqrt(p1.features.zip(p2.features).sumOf { (a, b) -> (a - b).pow(2) })
    }

    fun classifyStudents(allStudents: List<StudentData>): Pair<Map<StudentData, String>, List<Point>> {
        // 1. Фильтруем студентов с достаточными данными
        val validStudents = allStudents.filter { it.testCount > 1 }

        Log.d("KMeans", "Количество студентов с достаточными данными: ${validStudents.size} из ${allStudents.size}")

        if (validStudents.isEmpty()) {
            Log.e("KMeans", "Нет студентов с попытками тестирования")
            return fallbackClassification(allStudents)
        }

        // 2. Нормализуем данные для кластеризации
        val normalizedData = normalizeData(validStudents)

        // 3. Создаём точки на основе нормализованных данных
        val points = normalizedData.map { student ->
            Point(listOf(
                student.accuracy.toDouble(),          // accuracy (уже нормализованная)
                student.attempts.toDouble(),          // attempts
                student.timeSpent.toDouble(),         // timeSpent
                student.testCount.toDouble(),         // testCount
                student.weightedDifficulty.toDouble() // difficulty
            ))
        }

        // 4. Запускаем K-Means
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

        // 5. Сортируем кластеры по средней точности
        val sortedClusters = sortClustersByAccuracy(points)
        sortedClusters.forEachIndexed { newId, cluster ->
            for (point in cluster) {
                point.clusterId = newId
            }
        }

        // 6. Привязываем оригинальные StudentData к точкам
        val rankedMap = mutableMapOf<StudentData, String>()

        // Теперь связываем оригинальные данные со спрогнозированным рангом
        for ((i, point) in points.withIndex()) {
            val student = validStudents.getOrNull(i) ?: continue
            val initialRank = clusterLabels.getOrElse(point.clusterId) { "X" }
            val finalRank = if (initialRank == "S" && student.accuracy < 8) {
                assignRankBasedOnAccuracy(student.accuracy)
            } else {
                initialRank
            }
            rankedMap[student] = finalRank
        }

        return Pair(rankedMap, points)
    }

    private fun fallbackClassification(allStudents: List<StudentData>): Pair<Map<StudentData, String>, List<Point>> {
        // Если нет студентов с двумя и более тестами — используем простое ранжирование
        val rankedMap = allStudents.associate { student ->
            val rank = assignRankBasedOnAccuracy(student.accuracy)
            student to rank
        }

        val points = allStudents.map { student ->
            Point(listOf(
                student.accuracy.toDouble(),
                student.attempts.toDouble(),
                student.timeSpent.toDouble(),
                student.testCount.toDouble(),
                student.weightedDifficulty.toDouble()
            ))
        }

        return Pair(rankedMap, points)
    }

    private fun sortClustersByAccuracy(points: List<Point>): List<List<Point>> {
        val clusters = points.groupBy { it.clusterId }.values
        return clusters.sortedByDescending { cluster ->
            cluster.map { it.features[0] }.average()
        }
    }

    // --- Нормализация ---
    fun normalizeData(students: List<StudentData>): List<StudentData> {
        if (students.isEmpty()) return emptyList()

        val accuracyValues = students.map { it.accuracy }
        val attemptsValues = students.map { it.attempts.toDouble() }
        val timeSpentValues = students.map { it.timeSpent }
        val testCountValues = students.map { it.testCount.toDouble() }
        val difficultyValues = students.map { it.weightedDifficulty }

        val maxAccuracy = accuracyValues.maxOrNull()!!
        val minAccuracy = accuracyValues.minOrNull()!!

        val maxAttempts = attemptsValues.maxOrNull()!!
        val minAttempts = attemptsValues.minOrNull()!!

        val maxTimeSpent = timeSpentValues.maxOrNull()!!
        val minTimeSpent = timeSpentValues.minOrNull()!!

        val maxTestCount = testCountValues.maxOrNull()!!
        val minTestCount = testCountValues.minOrNull()!!

        val maxDifficulty = difficultyValues.maxOrNull()!!
        val minDifficulty = difficultyValues.minOrNull()!!

        // Логируем параметры нормализации
        Log.d("KMeans", "Normalization stats:")
        Log.d("KMeans", "accuracy: min = $minAccuracy, max = $maxAccuracy")
        Log.d("KMeans", "attempts: min = $minAttempts, max = $maxAttempts")
        Log.d("KMeans", "timeSpent: min = $minTimeSpent, max = $maxTimeSpent")
        Log.d("KMeans", "testCount: min = $minTestCount, max = $maxTestCount")
        Log.d("KMeans", "difficulty: min = $minDifficulty, max = $maxDifficulty")

        return students.map { student ->
            StudentData(
                accuracy = normalize(student.accuracy, minAccuracy, maxAccuracy),
                attempts = student.attempts,
                timeSpent = normalize(student.timeSpent, minTimeSpent, maxTimeSpent),
                testCount = student.testCount,
                weightedDifficulty = normalize(student.weightedDifficulty, minDifficulty, maxDifficulty)
            )
        }
    }

    private fun normalize(value: Double, min: Double, max: Double): Double {
        if (max == min) return 0.0
        return (value - min) / (max - min)
    }

    // --- Инициализация центроидов ---
    private fun initializeCentroids(data: List<Point>): List<Point> {
        val centroids = mutableListOf<Point>()
        val random = Random(42) // Фиксированный seed для воспроизводимости

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

    // --- Ранги на основе точности ---
    private fun assignRankBasedOnAccuracy(accuracy: Double): String {
        return when {
            accuracy >= 0.8 -> "S"
            accuracy >= 0.6 -> "A"
            accuracy >= 0.4 -> "B"
            accuracy >= 0.2 -> "C"
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

        Log.d("KMeans", "Silhouette Score: ${String.format("%.2f", scores.average())}")
        return scores.takeIf { it.isNotEmpty() }?.average() ?: 0.0
    }

    private fun meanIntraClusterDistance(point: Point, clusterPoints: List<Point>): Double {
        val others = clusterPoints.filter { it !== point }
        return if (others.isEmpty()) 0.0 else others.map { euclideanDistance(it, point) }.average()
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

    // --- Вспомогательные функции ---
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

        Log.d("KMeans", "--- Silhouette Plot ---")
        for ((index, score) in scores.withIndex()) {
            val barLength = (score * 20).toInt() + 20
            val bar = "-".repeat(barLength.coerceIn(0..40))
            Log.d("KMeans", "Point $index |$bar| $score")
        }
    }
}

// Extension-функции вне объекта
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