package com.kingfisher.kotlinbasketdemo.service

import com.commercetools.api.client.ProjectApiRoot
import com.commercetools.api.models.product.Product
import com.commercetools.api.models.product.ProductProjection
import com.kingfisher.kotlinbasketdemo.exception.InvalidProductId
import io.vrap.rmf.base.client.error.NotFoundException
import org.springframework.stereotype.Component

@Component
class CtProductService(val apiRoot: ProjectApiRoot) : ProductService {

    override fun getProductById(id: String): Product {
        return try {
            // if product not found then throws a 404 exception - NotFoundException - so return null
            apiRoot.products()
                .withId(id)
                .get()
                .executeBlocking()
                .body
        } catch (e: NotFoundException) {
            throw InvalidProductId("Product id ${id} is invalid.")
        }
    }

    override fun getProductByKey(key: String): Product {
        return try {
            apiRoot.products()
                .withKey(key)
                .get()
                .executeBlocking()
                .body
        } catch (e: NotFoundException) {
            throw InvalidProductId("Product key ${key} is invalid.")
        }
    }

    override fun listProductProjections(projections: List<ProductProjection>) {
        TODO("Not yet implemented")
    }

    override fun findProductProjectionsWithTaxCategory(): List<ProductProjection> {
        val response = apiRoot.productProjections()
            .search()
            .get()
            .withLimit(200)
            .addFilterQuery("taxCategory.id:exists")
            .executeBlocking()
            .body
        println(response.total)
        return response.results
    }

    override fun findProductProjections(): List<ProductProjection> {
        TODO("Not yet implemented")
    }
}