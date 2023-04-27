package com.kingfisher.kotlinbasketdemo.data

import org.springframework.stereotype.Component

@Component
class DataStore {

    enum class AddressKey {
        HOME, STORE;
    }

    lateinit private var products: List<ProductProjection>
    lateinit private var addresses: List<Address>
    lateinit private var shippingMethods: List<ShippingMethod>

    init {
        products = listOf(
            ProductProjection("d44ec4a7-f0b2-4f80-9bc6-df70b8d9500d", 1L, "Erbauer EJS12-Li 12V Li-Ion EXT Brushless Cordless Jigsaw", 64.99 ),
            ProductProjection("daf306c1-e113-48f7-ae61-e7d383530d39", 1L, "Hard Yakka Raptor Active Trousers Desert 38\" W 32", 52.99 ),
            ProductProjection("4ddb67ac-8f08-4c16-8804-8b8775e84e14", 1L, "Smith & Locke Stainless Brass BS 5-Lever Mortice Sashlock 66mm Case - 45mm Backset", 17.99 ),
            ProductProjection("5033ea4c-7c17-4e80-aeef-6f90895db97d", 1L, "Schneider Electric Ultimate Slimline Slave Telephone Socket White", 3.96 ),
        )

        addresses = listOf(
            Address("HOME", "Andy", "Evans", null, "12", "Oak Ave", "Bath", "BA1 2QL", "GB"),
            Address("HOME", "Josie", "Newperson", null,"22", "Ash Road", "Bristol", "BR2 2XL", "GB"),
            Address("STORE", "", "", "Penryn", "1", "Penryn Road", "Penryn", "TR11 3AB", "GB"),
            Address("STORE", "", "", "Truro", "22", "Cornwall Road", "Truro", "TR2 4PP", "GB"),
        )

        shippingMethods = listOf(
            ShippingMethod("4e744f6e-b4d7-4e6b-8f2c-103332af53a4", "NextDay", "Standard method, £5 all the way")
        )

    }

    fun projects() : List<ProductProjection> = products

    fun addresses() : List<Address> = addresses

    fun addresses(key: AddressKey) : List<Address> = addresses.filter { it -> it.key == key.toString() }

    fun shippingMethods() : List<ShippingMethod> = shippingMethods

}