package gpt
import java.io.File
import java.nio.file.{Path, Paths}
import cats.effect.{ExitCode, IO, IOApp, Ref}
import fs2.Stream
import org.http4s.HttpRoutes
import org.http4s.dsl.io._
import org.http4s.server.blaze.BlazeServerBuilder
import io.circe.generic.auto._
import io.circe.syntax._
import org.http4s.implicits._
import io.circe.{Decoder, Encoder, HCursor, Json}
import org.http4s.EntityDecoder
import org.http4s.circe.{CirceInstances, DecodingFailures}

case class Flight(
    origArp: String,
    destArp: String,
    date: String,
    flightNum: String
)

object Flight {
  implicit val flightEncoder: Encoder[Flight] = (flight: Flight) =>
    Json.obj(
      ("origArp", Json.fromString(flight.origArp)),
      ("destArp", Json.fromString(flight.destArp)),
      ("date", Json.fromString(flight.date)),
      ("flightNum", Json.fromString(flight.flightNum))
    )

  implicit val flightDecoder: Decoder[Flight] = (c: HCursor) =>
    for {
      origArp <- c.downField("origArp").as[String]
      destArp <- c.downField("destArp").as[String]
      date <- c.downField("date").as[String]
      flightNum <- c.downField("flightNum").as[String]
    } yield Flight(origArp, destArp, date, flightNum)
}

object FlightService extends IOApp with CirceInstances {
  import cats.effect.unsafe.implicits.global
  def readFlightsFile(filePath: Path): IO[Seq[Flight]] = {
    Stream
      .eval(IO(scala.io.Source.fromFile(filePath.toFile)))
      .flatMap(source => Stream.fromIterator[IO](source.getLines(), 64))
      .map(line => line.split(",").map(_.trim))
      .filter(cols => cols.length == 4)
      .map(cols => Flight(cols(0), cols(1), cols(2), cols(3)))
      .compile
      .toList
  }

  def flightExists(flight: Flight, flights: IO[Seq[Flight]]): IO[Boolean] =
    flights.map(_.contains(flight))

  // Entity decoder for Flight
  implicit val flightEntityDecoder: EntityDecoder[IO, Flight] =
    jsonOf[IO, Flight]

  def app(flights: IO[Seq[Flight]]): HttpRoutes[IO] =
    HttpRoutes.of[IO] {
      case req @ POST -> Root / "fareplace" / "flightExists" =>
        req.decode[Flight] { flight =>
          flightExists(flight, flights).flatMap { exists =>
            Ok(exists.asJson)
          }
        }
    }

  override def run(args: List[String]): IO[ExitCode] = {
    implicit val ec = scala.concurrent.ExecutionContext.Implicits.global
    import cats.effect.unsafe.implicits.global

    val flightsFilePath = Paths.get("flights.csv")
    val flights = readFlightsFile(flightsFilePath)
    BlazeServerBuilder[IO]
      .bindHttp(8082, "0.0.0.0")
      .withHttpApp(app(flights).orNotFound)
      .resource
      .useForever
      .as(ExitCode.Success)
  }
}
