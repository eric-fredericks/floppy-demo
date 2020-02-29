package org.kalergic.floppyears.wiretap

import play.api.mvc.{Request, WrappedRequest}

class MyAppRequest[A](request: Request[A]) extends WrappedRequest[A](request)
