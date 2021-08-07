# payments

dao and models for payments

### Usage

requires: 2.13

To include this as a sub-project

1. add the project in build.sbt:

  ```
  lazy val payments = RootProject(uri(s"https://github.com/j-chimienti/payments/"))
  
  // tip: to add specify a commit or a tag append to the url
  lazy val commit = "9ba3ae817789448f67df140ea9663136d90a6dee"
  lazy val payments = RootProject(uri(s"https://github.com/j-chimienti/payments.git#$commit"))
  ```

and then aggregate in your root project
  ```
  lazy val root = (project in file("."))
      .aggregate(payments)
      .dependsOn(payments)
  ```
## Test

`sbt test`

## Integration Tests

copy .env.template .env

`sbt it:test`
