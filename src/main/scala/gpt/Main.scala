package gpt
import cats.effect.{ExitCode, IO, IOApp}
import org.http4s.HttpRoutes
import org.http4s.dsl.io._
import org.http4s.blaze.server.BlazeServerBuilder
import io.circe.syntax._
import org.http4s.implicits._
import io.circe.{Decoder, Encoder, HCursor, Json}
import org.http4s.EntityDecoder
import org.http4s.circe.CirceInstances
import com.redis._
import java.sql.{ DriverManager,  Statement}


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

  def flgihtNumberExists(numbers: String, number: String) =
    numbers.split(" ").contains(number)
  def flightExists(flight: Flight, redis: RedisClient,
                   postgres: Statement): Boolean = {
    val key = Json.obj(
      ("origArp", Json.fromString(flight.origArp)),
      ("destArp", Json.fromString(flight.destArp)),
      ("date", Json.fromString(flight.date))).hashCode()
    val redisResponse: Option[String] = redis.get[String](key)
    if(redisResponse.isEmpty){
      val dataFromDB: String = postgres
        .executeQuery("SELECT numbers FROM Flights where origArp = " +
          flight.origArp + " AND destArp = " +flight.destArp + " AND date = " +
          flight.date)
        .getString("numbers")
      redis.set(key,dataFromDB)
      flgihtNumberExists(dataFromDB,flight.flightNum)
    }
    else flgihtNumberExists(redisResponse.get,flight.flightNum)
  }

  // Entity decoder for Flight
  implicit val flightEntityDecoder: EntityDecoder[IO, Flight] =
    jsonOf[IO, Flight]

  def app( redis: RedisClient,
          postgres: Statement): HttpRoutes[IO] =
    HttpRoutes.of[IO] {
      case req @ POST -> Root / "fareplace" / "flightExists" =>
        req.decode[Flight] { flight =>
            Ok(flightExists(flight,redis,postgres).asJson)
        }
    }

  override def run(args: List[String]): IO[ExitCode] = {
    val redis = new RedisClient("fake-redis-host", 6379)
    classOf[org.postgresql.Driver]
    val postgres = DriverManager
      .getConnection("jdbc:postgresql://fake-postgres-host:5432/DB_NAME?user=DB_USER")
      .createStatement()
    BlazeServerBuilder[IO]
      .bindHttp(8082, "0.0.0.0")
      .withHttpApp(app(redis,postgres).orNotFound)
      .resource
      .useForever
      .as(ExitCode.Success)
  }
}
