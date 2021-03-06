package tk.exarus.kpi.security

import tk.exarus.kpi.security.occurrence.{OccurrenceStatisticsCalculator, OccurrenceStatisticsRepository}

import scala.util.Try

/**
  * Starting point of the application
  *
  * @author Ruslan Gunawardana
  */
object Main extends App {
  val cycles = Try(args(0).toInt) getOrElse (16 * 1024)

  val statsRepo = new OccurrenceStatisticsRepository
  val statsCalculator = new OccurrenceStatisticsCalculator
  val decoder = new SimpleSubstitutionDecoder(statsRepo, statsCalculator, cycles)
  val ciphertextRepository = new CiphertextRepository

  val ciphertext = ciphertextRepository.ciphertext(0)
  val plaintext = decoder decode ciphertext

  println("Encoded ciphertext:")
  println(ciphertext)
  println("Decoded plaintext:")
  println(plaintext)
}
