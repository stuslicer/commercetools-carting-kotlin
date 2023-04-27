package com.kingfisher.kotlinbasketdemo.data

data class Address(val key:String,
                   val firstName: String,
                   val lastName: String,
                   val company: String?,
                   val streetNumber: String?,
                   val streetName: String?,
                   val city: String?,
                   val postalCode: String?,
                   val country: String
    )
