package com.kingfisher.kotlinbasketdemo.service

data class Address(val key:String,
                   val firstName: String,
                   val lastName: String,
                   val streetNumber: String?,
                   val streetName: String?,
                   val city: String?,
                   val postalCode: String?,
                   val country: String
    )
