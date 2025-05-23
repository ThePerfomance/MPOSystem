package com.example.groupprojectfirsttry.MathMethods

import android.util.Log
import com.example.groupprojectfirsttry.simpleClasses.StudentData
import org.apache.commons.math3.linear.EigenDecomposition
import org.apache.commons.math3.linear.MatrixUtils
import org.apache.commons.math3.linear.RealMatrix
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
        // 1. Фильтруем студентов с достаточным количеством тестов
        val validStudents = allStudents.filter { it.testCount > 1 }
        Log.d("KMeans", "Количество студентов с достаточными данными: ${validStudents.size} из ${allStudents.size}")

        if (validStudents.isEmpty()) return Pair(emptyMap(), emptyList())

        // 2. Нормализуем данные после фильтрации
        val normalizedData = normalizeData(validStudents)
        Log.d("KMeans", "Нормализованные данные:")
        normalizedData.forEachIndexed { index, student ->
            Log.d("KMeans", "Студент $index: accuracy = ${student.accuracy}, attempts = ${student.attempts}, timeSpent = ${student.timeSpent}, testCount = ${student.testCount}, weightedDifficulty = ${student.weightedDifficulty}")
        }

        // 3. Создаем точки с весами
        val weights = listOf(0.6, 0.1, 0.2, 0.05, 0.05) // можно менять или подбирать автоматически

        val points = normalizedData.map { student ->
            Point(listOf(
                student.accuracy * weights[0],
                student.attempts.toDouble() * weights[1],
                student.timeSpent * weights[2],
                student.testCount.toDouble() * weights[3],
                student.weightedDifficulty * weights[4]
            ))
        }

        Log.d("KMeans", "Точки для KMeans:")
        points.forEachIndexed { index, point ->
            Log.d("KMeans", "Точка $index: features = ${point.features}, clusterId = ${point.clusterId}")
        }

        // 4. Применяем PCA
        val reducedPoints = applyPCA(points, targetDim = 2)
        Log.d("KMeans", "Данные после PCA:")
        reducedPoints.forEachIndexed { index, point ->
            Log.d("KMeans", "Точка $index (PCA): features = ${point.features}, clusterId = ${point.clusterId}")
        }

        // 5. Запускаем KMeans несколько раз и выбираем лучший результат
        var bestPoints = reducedPoints
        var bestScore = -1.0

        repeat(10) {
            var centroids = initializeCentroids(reducedPoints)
            var changedAssignments = true

            while (changedAssignments) {
                for (point in reducedPoints) {
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
                    val assignedPoints = reducedPoints.filter { it.clusterId == clusterId }
                    if (assignedPoints.isNotEmpty()) {
                        val newFeatures = centroids[0].features.indices.map { i ->
                            assignedPoints.map { p -> p.features[i] }.average()
                        }
                        Point(newFeatures)
                    } else null
                }

                changedAssignments = centroids.zip(newCentroids).any { (oldP, newP) ->
                    euclideanDistance(oldP, newP) > 0.001
                }

                centroids = newCentroids
            }

            val currentSilhouette = calculateSilhouetteScore(reducedPoints)
            if (currentSilhouette > bestScore) {
                bestScore = currentSilhouette
                bestPoints = reducedPoints.map { it.copy(clusterId = it.clusterId) }
            }
        }

        Log.d("KMeans", "Результаты кластеризации:")
        bestPoints.forEachIndexed { index, point ->
            Log.d("KMeans", "Точка $index: features = ${point.features}, clusterId = ${point.clusterId}")
        }

        // 6. Пересортировка кластеров по средней точности
        val sortedClusters = sortClustersByPCA(bestPoints)
        sortedClusters.forEachIndexed { newId, cluster ->
            for (point in cluster) {
                point.clusterId = newId
            }
        }

        // 7. Вычисляем силуэтные коэффициенты
        val silhouetteScores = calculateSilhouetteForAllPoints(bestPoints)
        Log.d("KMeans", "Силуэтные коэффициенты:")
        silhouetteScores.forEachIndexed { index, score ->
            Log.d("KMeans", "Точка $index: Silhouette Score = $score")
        }

        // 8. Привязываем оригинальные данные и назначаем ранг по кластеру
        val rankedMap = assignRanksByClusterId(bestPoints, validStudents)

        return Pair(rankedMap, bestPoints)
    }

    private fun sortClustersByPCA(points: List<Point>): List<List<Point>> {
        val clusters = points.groupBy { it.clusterId }.values
        return clusters.sortedByDescending { cluster ->
            cluster.map { it.features[0] }.average()
        }
    }

    private fun assignRanksByClusterId(points: List<Point>, students: List<StudentData>): Map<StudentData, String> {
        val clusterToRank = mapOf(
            0 to "S",
            1 to "A",
            2 to "B",
            3 to "C",
            4 to "D"
        )

        val studentPointMap = students.zip(points).toMap()

        return studentPointMap.mapValues { (_, point) ->
            clusterToRank[point.clusterId] ?: "X"
        }
    }

    fun normalizeData(students: List<StudentData>): List<StudentData> {
        if (students.isEmpty()) return emptyList()

        val accuracyValues = students.map { it.accuracy }
        val attemptsValues = students.map { it.attempts.toDouble() }
        val timeSpentValues = students.map { it.timeSpent }
        val testCountValues = students.map { it.testCount.toDouble() }
        val difficultyValues = students.map { it.weightedDifficulty }

        val minAccuracy = accuracyValues.minOrNull()!!
        val maxAccuracy = accuracyValues.maxOrNull()!!
        val minAttempts = attemptsValues.minOrNull()!!
        val maxAttempts = attemptsValues.maxOrNull()!!
        val minTimeSpent = timeSpentValues.minOrNull()!!
        val maxTimeSpent = timeSpentValues.maxOrNull()!!
        val minTestCount = testCountValues.minOrNull()!!
        val maxTestCount = testCountValues.maxOrNull()!!
        val minDifficulty = difficultyValues.minOrNull()!!
        val maxDifficulty = difficultyValues.maxOrNull()!!

        Log.d("KMeans", "Normalization stats:")
        Log.d("KMeans", "accuracy: [$minAccuracy, $maxAccuracy]")
        Log.d("KMeans", "attempts: [$minAttempts, $maxAttempts]")
        Log.d("KMeans", "timeSpent: [$minTimeSpent, $maxTimeSpent]")
        Log.d("KMeans", "testCount: [$minTestCount, $maxTestCount]")
        Log.d("KMeans", "difficulty: [$minDifficulty, $maxDifficulty]")

        return students.map { student ->
            StudentData(
                accuracy = normalize(student.accuracy, minAccuracy, maxAccuracy),
                attempts = normalize(student.attempts.toDouble(), minAttempts, maxAttempts),
                timeSpent = normalize(student.timeSpent, minTimeSpent, maxTimeSpent),
                testCount = normalize(student.testCount.toDouble(), minTestCount, maxTestCount),
                weightedDifficulty = normalize(
                    student.weightedDifficulty,
                    minDifficulty,
                    maxDifficulty
                )
            )
        }
    }

    private fun normalize(value: Double, min: Double, max: Double): Double {
        if (max == min) return 0.0
        return (value - min) / (max - min)
    }

    fun applyPCA(points: List<Point>, targetDim: Int = 2): List<Point> {
        // Преобразуем точки в матрицу
        val matrix = points.map { it.features }.map { it.toDoubleArray() }.toTypedArray()
        val realMatrix = MatrixUtils.createRealMatrix(matrix)

        // Центрируем данные
        val n = realMatrix.rowDimension
        val d = realMatrix.columnDimension

        val meanVector = DoubleArray(d)
        for (i in 0 until d) {
            meanVector[i] = realMatrix.getColumn(i).average()
        }

        val centered = MatrixUtils.createRealMatrix(n, d)
        for (i in 0 until n) {
            for (j in 0 until d) {
                centered.setEntry(i, j, realMatrix.getEntry(i, j) - meanVector[j])
            }
        }

        // Ковариационная матрица
        val covariance = centered.transpose().multiply(centered).scalarMultiply(1.0 / (n - 1))

        // Вычисляем собственные значения и векторы
        val eigen = EigenDecomposition(covariance)
        val eigenVectors = (0 until targetDim).map { eigen.getEigenvector(it).toArray() }

        // Проекция точек на первые targetDim собственных векторов
        val projectedPoints = mutableListOf<Point>()
        for (i in 0 until n) {
            val point = Point(eigenVectors.map { vec ->
                centered.getRow(i).dot(vec)
            })
            projectedPoints.add(point)
        }

        return projectedPoints
    }
    private fun DoubleArray.dot(other: DoubleArray): Double {
        if (this.size != other.size) throw IllegalArgumentException("Массивы разного размера")
        var sum = 0.0
        for (i in indices) {
            sum += this[i] * other[i]
        }
        return sum
    }
    private fun computeCovariance(data: List<List<Double>>): RealMatrix {
        val n = data.size
        val d = data.first().size
        val matrix = MatrixUtils.createRealMatrix(data.map { it.toDoubleArray() }.toTypedArray())
        return matrix.transpose().multiply(matrix).scalarMultiply(1.0 / (n - 1))
    }

    private fun List<Double>.dot(other: DoubleArray): Double {
        if (this.size != other.size) throw IllegalArgumentException("Размерности не совпадают")
        return this.mapIndexed { i, value -> value * other[i] }.sum()
    }

    private fun List<Double>.toDoubleArray(): DoubleArray {
        return DoubleArray(this.size) { this[it] }
    }

    private fun transpose(matrix: List<List<Double>>): List<List<Double>> {
        if (matrix.isEmpty()) return emptyList()
        val transposed = mutableListOf<List<Double>>()
        for (i in 0 until matrix[0].size) {
            val row = matrix.mapNotNull { col ->
                col.getOrNull(i)
            }
            transposed.add(row)
        }
        return transposed
    }

    private fun initializeCentroids(data: List<Point>): List<Point> {
        val random = kotlin.random.Random(Random.Default.nextLong())
        val centroids = mutableListOf<Point>()
        centroids.add(data[random.nextInt(data.size)])

        for (i in 1 until K) {
            val distances = data.map { p ->
                centroids.minOfOrNull { c -> euclideanDistance(p, c).pow(2) } ?: 0.0
            }
            val total = distances.sum()
            val probabilities = distances.map { it / total }
            val r = random.nextDouble()
            var cumulative = 0.0
            var selectedIndex = -1

            for ((index, prob) in probabilities.withIndex()) {
                cumulative += prob
                if (cumulative >= r) {
                    selectedIndex = index
                    break
                }
            }

            if (selectedIndex != -1 && selectedIndex < data.size) {
                centroids.add(data[selectedIndex])
            } else {
                centroids.add(data[random.nextInt(data.size)])
            }
        }

        return centroids
    }

    fun calculateSilhouetteScore(points: List<Point>): Double {
        val clusters = points.groupBy { it.clusterId }
        if (clusters.size < 2) return 0.0

        val scores = points.map { point ->
            val clusterPoints = clusters[point.clusterId] ?: return@map 0.0
            val intra = meanIntraClusterDistance(point, clusterPoints)
            val inter = minInterClusterDistance(point, clusters)
            when {
                intra == 0.0 && inter == 0.0 -> 0.0
                intra < inter -> (inter - intra) / inter
                else -> -(intra - inter) / intra
            }
        }

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
                euclideanDistance(point, clusterPoints.getCentroid())
            }
        return distances.minOrNull() ?: Double.POSITIVE_INFINITY
    }

    fun assignRanksBySilhouette(scores: List<Double>, students: List<StudentData>): Map<StudentData, String> {
        if (scores.size != students.size) throw IllegalArgumentException("Кол-во оценок не совпадает с кол-вом студентов")
        if (students.isEmpty()) return emptyMap()

        val studentWithScore = students.zip(scores).sortedByDescending { it.second }
        val groupSize = (studentWithScore.size / 5) + 1
        val grouped = studentWithScore.chunked(groupSize)
        val rankMap = mutableMapOf<StudentData, String>()

        val ranks = listOf("S", "A", "B", "C", "D")
        for ((rankIndex, group) in grouped.withIndex()) {
            val rank = ranks.getOrNull(rankIndex) ?: "X"
            for ((student, _) in group) {
                rankMap[student] = rank
            }
        }

        return rankMap
    }

    fun calculateSilhouetteForAllPoints(points: List<Point>): List<Double> {
        val clusters = points.groupBy { it.clusterId }
        return points.map { point ->
            val clusterPoints = clusters[point.clusterId] ?: return@map 0.0
            val a = meanIntraClusterDistance(point, clusterPoints)
            val b = minInterClusterDistance(point, clusters)
            when {
                a == 0.0 && b == 0.0 -> 0.0
                a < b -> (b - a) / b
                else -> -((a - b) / a)
            }
        }
    }

    // --- Вспомогательные функции ---
    fun List<Point>.getCentroid(): Point {
        val featureCount = first().features.size
        val features = (0 until featureCount).map { i ->
            map { it.features[i] }.average()
        }
        return Point(features)
    }

    // --- Тест подбора весов ---
    fun findBestWeights(students: List<StudentData>) {
        val weightOptions = listOf(
            listOf(0.6, 0.1, 0.2, 0.05, 0.05),
            listOf(0.2, 0.2, 0.2, 0.2, 0.2),
            listOf(0.5, 0.1, 0.1, 0.1, 0.2),
            listOf(0.3, 0.1, 0.3, 0.1, 0.2)
        )

        weightOptions.forEach { weights ->
            val points = students.map { student ->
                Point(listOf(
                    student.accuracy * weights[0],
                    student.attempts.toDouble() * weights[1],
                    student.timeSpent * weights[2],
                    student.testCount.toDouble() * weights[3],
                    student.weightedDifficulty * weights[4]
                ))
            }

            val reducedPoints = applyPCA(points, targetDim = 2)
            val clusteredPoints = runKMeans(reducedPoints)
            val score = calculateSilhouetteScore(clusteredPoints)
            Log.d("KMeans", "Веса: $weights → Silhouette = ${String.format("%.3f", score)}")
        }
    }

    private fun runKMeans(points: List<Point>, k: Int = 5): List<Point> {
        var bestPoints = points.toList()
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
                if (assignedPoints.isNotEmpty()) {
                    val newFeatures = centroids[0].features.indices.map { i ->
                        assignedPoints.map { p -> p.features[i] }.average()
                    }
                    Point(newFeatures)
                } else null
            }

            changedAssignments = centroids.zip(newCentroids).any { (oldP, newP) ->
                euclideanDistance(oldP, newP) > 0.001
            }

            centroids = newCentroids
        }

        return points.map { it.copy(clusterId = it.clusterId) }
    }
    fun calculateInertia(points: List<Point>): Double {
        val clusters = points.groupBy { it.clusterId }
        return clusters.values.sumOf { cluster ->
            val centroid = cluster.getCentroid()
            cluster.sumOf { point -> euclideanDistance(point, centroid).pow(2) }
        }
    }
    fun daviesBouldinIndex(points: List<Point>): Double {
        val clusters = points.groupBy { it.clusterId }.values
        var score = 0.0

        for (clusterA in clusters) {
            val centroidA = clusterA.getCentroid()
            var maxSimilarity = 0.0

            for (clusterB in clusters) {
                if (clusterA == clusterB) continue
                val centroidB = clusterB.getCentroid()

                val distanceBetweenCentroids = euclideanDistance(centroidA, centroidB)
                val scatterA = clusterA.map { euclideanDistance(it, centroidA) }.average()
                val scatterB = clusterB.map { euclideanDistance(it, centroidB) }.average()

                val similarity = (scatterA + scatterB) / distanceBetweenCentroids
                if (similarity > maxSimilarity) maxSimilarity = similarity
            }

            score += maxSimilarity
        }

        return score / clusters.size
    }
}