package com.kingfisher.kotlinbasketdemo.api

import com.commercetools.api.client.ProjectApiRoot
import com.commercetools.api.defaultconfig.ApiRootBuilder
import com.kingfisher.kotlinbasketdemo.config.CommerceToolsConfig
import io.vrap.rmf.base.client.oauth2.ClientCredentials
import org.springframework.context.annotation.Configuration

@Configuration
class ClientFactory(
    val commerceToolsConfig: CommerceToolsConfig
) {

    fun createApiClient(): ProjectApiRoot {
        val apiRoot: ProjectApiRoot = ApiRootBuilder.of()
            .defaultClient(
                ClientCredentials.of()
                .withClientId(commerceToolsConfig.clientId)
                .withClientSecret(commerceToolsConfig.clientSecret)
                .build(),
                commerceToolsConfig.region )
            .build(commerceToolsConfig.projectKey)
        return apiRoot
    }

}