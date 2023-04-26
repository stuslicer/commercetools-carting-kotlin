package com.kingfisher.kotlinbasketdemo.config

import com.commercetools.api.defaultconfig.ServiceRegion
import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("commercetools")
data class CommerceToolsConfig(
    val clientId: String,
    val clientSecret: String,
    val projectKey: String,
    val region: ServiceRegion
) {
}