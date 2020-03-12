resolvers += Resolver.file(
  "Local",
  file(sys.env.getOrElse("WORKSPACE", sys.props("user.home")) + "/.ivy2/local")
)(Resolver.ivyStylePatterns)

sys.props.get("plugin.version") match {
  case Some(v) =>
    addSbtPlugin("org.kalergic.floppyears" %% "floppy-ears-plugin" % v)
  case _ =>
    sys.error(
      """|The system property 'plugin.version' is not defined.
                         |Specify this property using the scriptedLaunchOpts -D.""".stripMargin
    )
}
