package com.kingfisher.kotlinbasketdemo.config

import com.commercetools.api.client.ProjectApiRoot
import com.kingfisher.kotlinbasketdemo.api.ClientFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class ApiRootConfig {

    @Bean
    fun createApi(clientFactory: ClientFactory): ProjectApiRoot {
        return clientFactory.createApiClient()
    }
}