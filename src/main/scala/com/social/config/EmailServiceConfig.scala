package com.social.config

import pureconfig.ConfigReader
import pureconfig.generic.derivation.default.*

final case class EmailServiceConfig (
    host: String,
    port: Int,
    user: String,
    pass: String,
    frontEndUrl: String
                                    ) derives ConfigReader
