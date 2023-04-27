package com.kingfisher.kotlinbasketdemo.service

import com.commercetools.api.models.product.Product
import com.commercetools.api.models.product.ProductProjection

interface ProductService {

    fun getProductById(id: String): Product
    fun getProductByKey(key: String): Product

    fun listProductProjections(projections: List<ProductProjection>)

    fun findProductProjectionsWithTaxCategory(): List<ProductProjection>
    fun findProductProjections(): List<ProductProjection>

}