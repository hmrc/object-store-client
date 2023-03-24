# object-store-client

![](https://img.shields.io/github/v/release/hmrc/object-store-client)

object-store-client is a Scala client to interact with the [object-store](https://github.com/hmrc/object-store) service.
The methods available on the client encapsulate the HTTP interaction with object-store service, making it easier for the consuming service to interact with the object-store service.

The operations available via object-store-client are
- Put a new object
- Download a object
- Delete a object
- List objects under a path
- Zip objects under a path
- Upload a new object from a url

Read more [here](https://github.com/hmrc/object-store/blob/master/README.md) about object-store.

We also have a working example [hello-world-object-store](https://github.com/hmrc/hello-world-object-store) which demonstrates using object-store-client.

## Using object-store-client

### Add object-store-client dependency

In the SBT build add:

```sbt
resolvers += MavenRepository("HMRC-open-artefacts-maven2", "https://open.artefacts.tax.service.gov.uk/maven2")

libraryDependencies += "uk.gov.hmrc.objectstore" %% "object-store-client-play-xx" % "x.x.x"
```

### Configuration

- Ensure `appName` is configured in `application.conf`


- Enable `uk.gov.hmrc.objectstore.client.play.modules.ObjectStoreModule` in the `application.conf` by adding
```hocon
play.modules.enabled += "uk.gov.hmrc.objectstore.client.play.modules.ObjectStoreModule"
```

- Configure object-store HTTP configuration under `microservice.services` block in `application.conf`
```hocon
object-store {
  host = localhost
  port = 8464
}
```

Also, override this configuration in app-config-base so this service can interact with object-store service in the deployed environments.

- Configure the Authorization token in `application.conf` which grants permissions to this service to interact with object-store via object-store-client.
```hocon
internal-auth.token = "<INSERT-VALUE>"
```
For the deployed environments, please request Build & Deployment team to generate a token for the service and configure this property with the encrypted value of the token.

- Configure `object-store.default-retention-period` in `application.conf`. This property represents the default retention period of the uploaded object in object-store.
  This value can be overriden on the individual upload requests.

```hocon
object-store.default-retention-period = "1-week"
```

The valid values for this configuration are
- 1-day
- 1-week
- 1-month
- 6-months
- 1-year
- 7-years
- 10-years

Any other configured value or absence of configuration will result in the failure of the service start up.

### Available operations

Use DI (Guice or your preferred mechanism) to inject an instance of `uk.gov.hmrc.objectstore.client.play.PlayObjectStoreClient` in the controllers or services
and use its methods to interact with object-store. Here onwards, we'll call that instance as `client` in the code excerpts.


#### **Put object**

```scala
import uk.gov.hmrc.objectstore.client.play.Implicits._

client.putObject(
  path            = Path.Directory("accounts/2020").file("summary.txt"),
  content         = "this is the content",
  retentionPeriod = RetentionPeriod.OneMonth, // defaults to 'object-store.default-retention-period' configuration
  contentType     = Some("plain/text"), // defaults to None
  contentMd5      = Some(Md5Hash("4033ff85a6fdc6a2f51e60d89236a244")), // defaults to None, and will be calculated
  owner           = "my-service" // defaults to 'appName' configuration
) // returns Future[ObjectSummaryWithMd5]
```

The above code will put the argument of the `content` parameter in an object at the path `/my-service/accounts/2020/summary.text` with a retention period of 1 month.
Bear in mind, that this operation will overwrite the content that already exists at that path.

The method `putObject` is polymorphic in its parameter `content`. At the moment, we support following types -

- `akka.stream.scaladsl.Source[ByteString, _]`
- `java.io.File`
- `String`
- `Array[Byte]`
- `uk.gov.hmrc.objectstore.client.http.Payload`

We recommend using streamed content (`akka.stream.scaladsl.Source[ByteString, _]`), as it'll avoid loading the whole content in the memory.

#### **Get object**

```scala
import uk.gov.hmrc.objectstore.client.play.Implicits._

client.getObject[Source[ByteString, NotUsed]](
  path = Path.Directory("accounts/2020").file("summary.txt"),
  owner = "my-service" // defaults to 'appName' configuration
) // returns Future[Option[Object[Source[ByteString, NotUsed]]]]
```

The above code will try to download the content of the object at the path `/my-service/accounts/2020/summary.text`.
If the path doesn't exist, the method will return `Future[None]`.

The method `getObject` is polymorphic in its return type. At the moment, we support following types -

- `akka.stream.scaladsl.Source[ByteString, _]`
- `String`
- `play.api.libs.json.JsValue`
- Any type `A` for which an implicit instance of type `play.api.libs.Reads[A]` is also available in the scope

#### **Delete object**

```scala
client.deleteObject(
  path = Path.Directory("accounts/2020").file("summary.txt"),
  owner = "my-service" // defaults to 'appName' configuration
) // returns Future[Unit]
```

The above code will delete the object at the path `/my-service/accounts/2020/summary.text`.

#### **List objects**

```scala
client.listObjects(
  path = Path.Directory("accounts/2020"),
  owner = "my-service" // defaults to 'appName' configuration
) // returns Future[ObjectListing]
```

#### **Zip**

```scala
osClient.zip(
  from            = Path.Directory("envelope1"),
  to              = Path.File(Path.Directory("zips"), "zip1.zip"),
  retentionPeriod = RetentionPeriod.OneWeek, // defaults to 'object-store.default-retention-period' configuration
  fromOwner       = "my-service",            // defaults to 'appName' configuration
  toOwner         = "my-service"             // defaults to 'appName' configuration
) // returns Future[ObjectSummaryWithMd5]
```

The above code will zip all objects in the directory `/my-service/envelope1/` to a zipped object at `/my-service/zips/zip1.zip`

#### **Upload object from url**

```scala
osClient.uploadFromUrl(
  from            = new URL("https://fus-outbound.s3.eu-west-2.amazonaws.com/81fb03f5-195d-422a-91ab-460939045846"),
  to              = Path.File(Path.Directory("my-folder"), "sample.pdf"),
  retentionPeriod = RetentionPeriod.OneWeek, // defaults to 'object-store.default-retention-period' configuration
  contentType     = Some("text/csv"), // defaults to None which results in the contentType 'application/octet-stream'
  contentMd5      = Some(Md5Hash("a3c2f1e38701bd2c7b54ebd7b1cd0dbc")), // defaults to None
  owner           = "my-service" // defaults to 'appName' configuration
) // returns Future[ObjectSummaryWithMd5]
```

The above code will download a file from `https://fus-outbound.s3.eu-west-2.amazonaws.com/81fb03f5-195d-422a-91ab-460939045846` to
`/my-service/my-folder/sample.pdf`

### Error handling
Exceptions like `uk.gov.hmrc.http.GatewayTimeoutException` or response parsing exception will be returned wrapped inside the failed Future.
If you prefer explicit error encoded in the return type of object-store methods, then use `uk.gov.hmrc.objectstore.client.play.PlayObjectStoreClientEither` instead of `uk.gov.hmrc.objectstore.client.play.PlayObjectStoreClient`.
The object-store methods will now return a `Future[Either[Exception, A]]`.


### Stubbing object-store-client in tests
In the unit or integration tests, we'd like to stub out the interaction with real object-store. For this, we provide in-memory stubs `StubPlayObjectStoreClient` and `StubPlayObjectStoreClientEither` (stub counterparts for `PlayObjectStoreClient` and `PlayObjectStoreClientEither` correspondingly).

For unit tests, we can inject the stub instance into the subject under test and also make assertions on the stub instance.

For integration tests, we can override the default binding to use the stub instance like below

```scala
class HelloWorldObjectStoreIntegrationSpec extends PlaySpec with GuiceOneAppPerSuite {

  val baseUrl    = s"baseUrl-${randomUUID().toString}"
  val owner      = s"owner-${randomUUID().toString}"
  val token      = s"token-${randomUUID().toString}"
  val config     = ObjectStoreClientConfig(baseUrl, owner, token, OneWeek)

  lazy val objectStoreStub = new stub.StubPlayObjectStoreClient(config)

  override def fakeApplication(): Application =
    GuiceApplicationBuilder()
            .bindings(bind(classOf[PlayObjectStoreClient]).to(objectStoreStub))
            .build()
}
```

### Stubbing object-store for acceptance tests
Please see [object-store stubs](https://github.com/hmrc/object-store#object-store-stubs)

### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").
