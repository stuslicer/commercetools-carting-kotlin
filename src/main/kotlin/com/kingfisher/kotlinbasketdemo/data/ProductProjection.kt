package com.kingfisher.kotlinbasketdemo.data

data class ProductProjection(
    val id: String,
    val variant: Long,
    val name: String,
    val price: Double
)
