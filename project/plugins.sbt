resolvers += Resolver.url("co.blocke ivy resolver", url("http://dl.bintray.com/blocke/releases/"))(Resolver.ivyStylePatterns)
addSbtPlugin("co.blocke" % "gitflow-packager" % "0.1.1")
addSbtPlugin("me.lessis" % "bintray-sbt" % "0.3.0")