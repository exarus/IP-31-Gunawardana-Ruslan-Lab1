package ua.kpi.ip31.gunawardana

import com.typesafe.scalalogging.LazyLogging
import ua.kpi.ip31.gunawardana.repository.TextStatisticsRepository

import scala.collection.mutable
import scala.util.Random

/**
  * Thread-safe. Immutable.
  *
  * @author Ruslan Gunawardana
  */
class SimpleSubstitutionDecoder(statsRepo: TextStatisticsRepository,
                                statsCalculator: StatisticsCalculator,
                                cycles: Int)
  extends Decoder with LazyLogging {
  private[this] val random = new Random

  override def decode(ciphertext: String): String = {
    val lowerDecipheringKey = decipheringKeyFor(ciphertext.toLowerCase)
    val upperDecipheringKey = lowerDecipheringKey map { case (k, v) => k.toUpper -> v.toUpper }
    val decipheringKey = lowerDecipheringKey ++ upperDecipheringKey
    ciphertext map decipheringKey
  }

  import SimpleSubstitutionDecoder.swap

  private def decipheringKeyFor(ciphertext: String): Map[Char, Char] = {
    val expectedStats = statsRepo.thirdOrderStatistics
    val ciphertextStats = statsCalculator.orderStatistics(ciphertext, 3)
    val plaintextAlphabet = alphabetSortedByStats(statsRepo.firstOrderStatistics)
    val ciphertextAlphabet = alphabetSortedByStats(statsCalculator firstOrderStatistics ciphertext)

    // least test result is the best
    val decipheringKey = mutable.Map(ciphertextAlphabet zip plaintextAlphabet: _*)
    var minTestResult = testResult(expectedStats, ciphertextStats, decipheringKey)
    var iterationsLeft = cycles
    while (iterationsLeft > 0) {
      val (c1, c2) = (randomLetter, randomLetter)
      swap(decipheringKey, c1, c2)

      val newTestResult = testResult(expectedStats, ciphertextStats, decipheringKey)
      if (newTestResult < minTestResult) {
        minTestResult = newTestResult
        iterationsLeft = cycles
        logger.debug(s"key = $decipheringKey, testResult = $minTestResult")
      } else {
        swap(decipheringKey, c1, c2)
        iterationsLeft -= 1
      }
    }
    decipheringKey.toMap
  }

  private def alphabetSortedByStats(stats: Map[Char, Double]): String = {
    val letters = (statsRepo.alphabet
      map { c => c -> stats.getOrElse(c, 0.0) }
      sortBy (_._2)
      map (_._1))
    letters.mkString
  }

  private def randomLetter: Char = {
    val alphabet = statsRepo.alphabet
    alphabet(random nextInt alphabet.length)
  }

  private def testResult(expectedStats: Map[String, Double],
                         ciphertextStats: Map[String, Double],
                         decipheringKey: collection.Map[Char, Char]): Double = {
    val actualStats = ciphertextStats map { case (k, v) => (k map decipheringKey) -> v }
    statsCalculator.chiSquareTest(expectedStats, actualStats)
  }
}

private object SimpleSubstitutionDecoder {
  private def swap(map: mutable.Map[Char, Char], c1: Char, c2: Char) {
    val tmp = map(c1)
    map(c1) = map(c2)
    map(c2) = tmp
  }
}